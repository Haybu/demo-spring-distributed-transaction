/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agilehandy.saga.txn;

import java.sql.Timestamp;
import java.util.UUID;

import javax.validation.Valid;

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.database.DBCancelRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobRequest;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.commons.api.storage.FileCancelRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.commons.api.storage.FileTxnResponse;
import io.agilehandy.saga.annotations.StatesOnTransition;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.OnStateMachineStart;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
@WithStateMachine( id = "saga-machine")
@EnableBinding(EventChannels.class)
//@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TxnManager {

	private final EventChannels channels;
	private final TxnRepository repository;
	private final StateMachineFactory factory;

	private JobRequest jobRequest;
	private UUID globalTxnId;

	public TxnManager(EventChannels channels, TxnRepository repository
			, StateMachineFactory factory) {
		this.channels = channels;
		this.repository = repository;
		this.factory = factory;
	}

	public void setJobRequest(JobRequest jobRequest) {
		this.jobRequest = jobRequest;
	}
	public UUID getGlobalTxnId() { return globalTxnId; }
	public void setGlobalTxnId(UUID id) { this.globalTxnId = id; }

	// Start the txn machine
	@OnStateMachineStart
	public void start() {
		setGlobalTxnId(UUID.randomUUID());
		jobRequest.setGlobalTxnId(getGlobalTxnId());
		persistNewTransaction();
		// create a new state machine
		StateMachine<JobState,JobEvent> machine = factory.getStateMachine(getGlobalTxnId());
		machine.startReactively().subscribe();
		Message message = MessageBuilder.withPayload(JobEvent.JOB_TXN_START).build();
		machine.sendEvent(Mono.just(message)).subscribe();
	}

	// submit a file txn
	@StatesOnTransition (source = JobState.JOB_START, target = JobState.FILE_SUBMIT)
	public void handleFileSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		FileSubmitRequest request = jobRequest.getFileRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "FILE_SUBMIT")
				.build();
		channels.fileRequest().send(message);
	}

	// cancel a file txn
	@StatesOnTransition (source = {JobState.FILE_SUBMIT, JobState.DB_CANCEL}, target = JobState.FILE_CANCEL)
	public void handleFileCancelAction(StateContext<JobState, JobEvent> stateContext) {
		FileCancelRequest request = new FileCancelRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		request.setFileId(jobRequest.getFileRequest().getFileId());
		request.setFilename(jobRequest.getFileRequest().getFilename());
		request.setJobId(jobRequest.getJobId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "FILE_CANCEL")
				.build();
		channels.fileRequest().send(message);
	}

	// submit a db record: send a message request
	@StatesOnTransition (source = JobState.FILE_SUBMIT, target = JobState.DB_SUBMIT)
	public void handleDbSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		DBSubmitRequest request = jobRequest.getDbRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "DB_SUBMIT")
				.build();
		channels.dbRequest().send(message);
	}

	// cancel a db txn
	@StatesOnTransition (source = {JobState.DB_SUBMIT, JobState.BC_CANCEL}, target = JobState.DB_CANCEL)
	public void handleDbCancelAction(StateContext<JobState, JobEvent> stateContext) {
		DBCancelRequest request = new DBCancelRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		request.setJobId(jobRequest.getJobId());
		request.setRecordId(jobRequest.getDbRequest().getRecordId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "DB_CANCEL")
				.build();
		channels.dbRequest().send(message);
	}

	// submit a bc record: send a message request
	@StatesOnTransition (source = JobState.DB_SUBMIT, target = JobState.BC_SUBMIT)
	public void handleBcSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		BCSubmitRequest request = jobRequest.getBcRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "BC_SUBMIT")
				.build();
		channels.bcRequest().send(message);
	}

	// cancel a bc txn
	@StatesOnTransition (source = JobState.BC_SUBMIT, target = JobState.BC_CANCEL)
	public void handleBcCancelAction(StateContext<JobState, JobEvent> stateContext) {
		BCCancelRequest request = new BCCancelRequest();
		request.setGlobalTxnId(getGlobalTxnId());
		request.setContentId(jobRequest.getBcRequest().getContentId());
		request.setJobId(jobRequest.getJobId());
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "BC_CANCEL")
				.build();
		channels.bcRequest().send(message);
	}

	// end job successfully
	@StatesOnTransition (source = JobState.BC_SUBMIT, target = JobState.JOB_COMPLETE)
	public void handleCompleteJob(StateContext<JobState, JobEvent> stateContext) {
		Transaction transaction = repository
				.findTransactionByJobIdAndTxnId(Long.valueOf(jobRequest.getJobId().toString())
						, jobRequest.getGlobalTxnId().toString());
		transaction.setJobTxnStatus(JobState.JOB_COMPLETE.toString());
		transaction.setJobEndTS(new Timestamp(System.currentTimeMillis()));
		repository.save(transaction);
	}

	// fail job
	@StatesOnTransition (source = JobState.FILE_CANCEL, target = JobState.JOB_FAIL)
	public void handleFailJob(StateContext<JobState, JobEvent> stateContext) {
		Transaction transaction = repository
				.findTransactionByJobIdAndTxnId(Long.valueOf(jobRequest.getJobId().toString())
						, jobRequest.getGlobalTxnId().toString());
		transaction.setJobTxnStatus(JobState.JOB_FAIL.toString());
		transaction.setJobEndTS(new Timestamp(System.currentTimeMillis()));
		repository.save(transaction);
	}

	//------------------------------------------------
	// handle txn responses
	//------------------------------------------------

	@StreamListener(target = EventChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='FILE_SUBMIT_COMPLETE'")
	public void handleSubmitFile(@Valid FileTxnResponse response) {
		// todo: you can use the resposne payload for any purpose

	}

	//------------------------------------------------
	// auxiliaries
	//------------------------------------------------
	private void persistNewTransaction() {
		Transaction txn = new Transaction();
		txn.setJobId(Long.valueOf(jobRequest.getJobId().toString()));
		txn.setTxnId(getGlobalTxnId().toString());
		txn.setJobTxnStatus(JobState.JOB_START.name());
		txn.setFileId(jobRequest.getFileRequest().getFileId().toString());
		txn.setFileName(jobRequest.getFileRequest().getFilename());
		txn.setDbRecordId(jobRequest.getDbRequest().getRecordId().toString());
		txn.setBcRecordId(jobRequest.getBcRequest().getContentId().toString());
		txn.setJobStartTS(new Timestamp(System.currentTimeMillis()));
		repository.save(txn);
	}

}
