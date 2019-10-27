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
package io.agilehandy.txn.saga;

import java.sql.Timestamp;
import java.util.UUID;

import javax.validation.Valid;

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.blockchain.BCTxnResponse;
import io.agilehandy.commons.api.database.DBCancelRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.database.DBTxnResponse;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobRequest;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.commons.api.storage.FileCancelRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.commons.api.storage.FileTxnResponse;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;

/**
 * @author Haytham Mohamed
 **/

@Data
@Log4j2
public class Saga {

	private final JobRepository jobRepository;
	private final StateMachineFactory factory;

	private FileSubmitRequest fileSubmitRequest;
	private DBSubmitRequest dbSubmitRequest;
	private BCSubmitRequest bcSubmitRequest;

	private Saga() { this (null, null);}

	public Saga(JobRepository jobRepository, StateMachineFactory factory) {
		log.info("A new saga is instantiated.");
		this.jobRepository = jobRepository;
		this.factory = factory;
	}

	public void orchestrate(FileSubmitRequest fsr, DBSubmitRequest dbr, BCSubmitRequest bcr) {
		fileSubmitRequest = fsr;
		dbSubmitRequest = dbr;
		bcSubmitRequest = bcr;
		orchestrate();
	}

	public void orchestrate() {
		log.info("saga orchestration starts.");
		UUID txnId = UUID.randomUUID();
		fileSubmitRequest.setGlobalTxnId(txnId);
		dbSubmitRequest.setGlobalTxnId(txnId);
		bcSubmitRequest.setGlobalTxnId(txnId);
		Job job = createJob(fileSubmitRequest, dbSubmitRequest, bcSubmitRequest);
		start(job.getJobId(), txnId.toString());
	}

	// start by submitting the file
	private void start(String jobId, String txnId) {
		signalStateMachine(jobId, txnId.toString(), fileSubmitRequest, JobEvent.JOB_TXN_START);
	}

	private Job createJob(FileSubmitRequest fs, DBSubmitRequest db, BCSubmitRequest bc) {
		Job txn = new Job();
		txn.setJobId(fs.getJobId().toString());
		txn.setTxnId(fs.getGlobalTxnId().toString());
		txn.setJobState(JobState.JOB_START.name());
		txn.setFileId(fs.getFileId().toString());
		txn.setDbRecordId(db.getRecordId().toString());
		txn.setBcRecordId(bc.getContentId().toString());
		txn.setStartTS(new Timestamp(System.currentTimeMillis()));
		return jobRepository.save(txn);
	}

	public StateMachine<JobState,JobEvent> signalStateMachine(String jobId, String txnId
			, JobRequest request, JobEvent signal) {
		log.info("machine signal sent: " + signal);
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload(signal)
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='FILE_SUBMIT_COMPLETE'")
	public void handleFileSubmitComplete(@Valid FileTxnResponse response) {
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,dbSubmitRequest, JobEvent.FILE_SUBMIT_COMPLETE);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='FILE_SUBMIT_FAIL'")
	public void handleFileSubmitFail(@Valid FileTxnResponse response) {
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(response.getFilename());
		fileCancelRequest.setFileId(response.getFileId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_SUBMIT_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='FILE_CANCEL_COMPLETE'")
	public void handleFileCancelComplete(@Valid FileTxnResponse response) {
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,null, JobEvent.FILE_CANCEL_COMPLETE);
	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='FILE_CANCEL_FAIL'")
	public void handleFileCancelFail(@Valid FileTxnResponse response) {
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(response.getFilename());
		fileCancelRequest.setFileId(response.getFileId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_CANCEL_FAIL);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='DB_SUBMIT_COMPLETE'")
	public void handleDBSubmitComplete(@Valid DBTxnResponse response) {
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,bcSubmitRequest, JobEvent.DB_SUBMIT_COMPLETE);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='DB_SUBMIT_FAIL'")
	public void handleDBSubmitFail(@Valid DBTxnResponse response) {
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(response.getRecordId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.DB_SUBMIT_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='DB_CANCEL_COMPLETE'")
	public void handleDBCancelComplete(@Valid DBTxnResponse response) {
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(fileSubmitRequest.getFilename());
		fileCancelRequest.setFileId(fileSubmitRequest.getFileId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.DB_CANCEL_COMPLETE);

	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='DB_CANCEL_FAIL'")
	public void handleDBCancelFail(@Valid DBTxnResponse response) {
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(response.getRecordId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.DB_CANCEL_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='BC_SUBMIT_COMPLETE'")
	public void handleBCSubmitComplete(@Valid BCTxnResponse response) {
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,null, JobEvent.BC_SUBMIT_COMPLETE);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='BC_SUBMIT_FAIL'")
	public void handleBCSubmitFail(@Valid BCTxnResponse response) {
		BCCancelRequest bcCancelRequest = new BCCancelRequest();
		bcCancelRequest.setJobId(response.getJobId());
		bcCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		bcCancelRequest.setContentId(response.getContentId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,bcCancelRequest, JobEvent.BC_SUBMIT_FAIL);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='BC_CANCEL_COMPLETE'")
	public void handleBCCancelComplete(@Valid BCTxnResponse response) {
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(dbSubmitRequest.getRecordId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.BC_CANCEL_COMPLETE);
	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE
			, condition = "headers['saga_response']=='BC_CANCEL_FAIL'")
	public void handleBCCancelFail(@Valid BCTxnResponse response) {
		BCCancelRequest bcCancelRequest = new BCCancelRequest();
		bcCancelRequest.setJobId(response.getJobId());
		bcCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		bcCancelRequest.setContentId(response.getContentId());
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,bcCancelRequest, JobEvent.BC_CANCEL_FAIL);
	}

	private StateMachine<JobState, JobEvent> build(String jobId, String txnId) {
		Job job = jobRepository.findTransactionByJobIdAndTxnId(jobId, txnId);
		StateMachine<JobState,JobEvent> machine = factory.getStateMachine(UUID.fromString(txnId));
		machine.stopReactively().subscribe();
		machine.getStateMachineAccessor()
				.doWithAllRegions(sma -> {
					sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<JobState,JobEvent>(){

						@Override
						public void preStateChange(State<JobState, JobEvent> state, Message<JobEvent> message, Transition<JobState, JobEvent> transition, StateMachine<JobState, JobEvent> stateMachine) {
							String tempJobId = String.class.cast(message.getHeaders().getOrDefault("jobId", ""));
							String tempTxnId = String.class.cast(message.getHeaders().getOrDefault("txnId", ""));
							log.info("State machine interceptor accessing Job with jobId = "+tempJobId+" and txnId = "+tempTxnId);
							Job tempJob = jobRepository.findTransactionByJobIdAndTxnId(tempJobId, tempTxnId);
							tempJob.setJobState(state.getId().name());
							jobRepository.save(tempJob);
						}
					});

					sma.resetStateMachine(new DefaultStateMachineContext(job.getJobState(), null, null,null));
				});
		machine.startReactively().subscribe();
		log.info("state machine built is at state: " + machine.getState().getId().name());
		return machine;
	}

}
