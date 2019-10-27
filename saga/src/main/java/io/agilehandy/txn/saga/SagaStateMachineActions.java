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

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.database.DBCancelRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.commons.api.storage.FileCancelRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.txn.annotations.StatesOnTransition;
import lombok.extern.log4j.Log4j2;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
//@WithStateMachine( id = "saga-machine")
public class SagaStateMachineActions {

	private final SagaChannels channels;
	private final JobRepository repository;

	public SagaStateMachineActions(SagaChannels channels, JobRepository repository) {
		this.channels = channels;
		this.repository = repository;
	}

	// submit a file txn
	@StatesOnTransition(source = JobState.JOB_START, target = JobState.FILE_SUBMIT)
	public void handleFileSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from JOB_START to FILE_SUBMIT");
		FileSubmitRequest request =
				(FileSubmitRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "FILE_SUBMIT")
				.build();
		channels.fileRequestOut().send(message);
	}

	// cancel a file txn
	@StatesOnTransition (source = {JobState.FILE_SUBMIT, JobState.DB_CANCEL}, target = JobState.FILE_CANCEL)
	public void handleFileCancelAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from FILE_SUBMIT or DB_CANCEL to FILE_CANCEL");
		FileCancelRequest request =
				(FileCancelRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "FILE_CANCEL")
				.build();
		channels.fileRequestOut().send(message);
	}

	// submit a db record: send a message request
	@StatesOnTransition (source = JobState.FILE_SUBMIT, target = JobState.DB_SUBMIT)
	public void handleDbSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from FILE_SUBMIT to DB_SUBMIT");
		DBSubmitRequest request =
				(DBSubmitRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "DB_SUBMIT")
				.build();
		channels.dbRequestOut().send(message);
	}

	// cancel a db txn
	@StatesOnTransition (source = {JobState.DB_SUBMIT, JobState.BC_CANCEL}, target = JobState.DB_CANCEL)
	public void handleDbCancelAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from DB_SUBMIT or BC_CANCEL to DB_CANCEL");
		DBCancelRequest request =
				(DBCancelRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "DB_CANCEL")
				.build();
		channels.dbRequestOut().send(message);
	}

	// submit a bc record: send a message request
	@StatesOnTransition (source = JobState.DB_SUBMIT, target = JobState.BC_SUBMIT)
	public void handleBcSubmitAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from DB_SUBMIT to BC_SUBMIT");
		BCSubmitRequest request =
				(BCSubmitRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "BC_SUBMIT")
				.build();
		channels.bcRequestOut().send(message);
	}

	// cancel a bc txn
	@StatesOnTransition (source = JobState.BC_SUBMIT, target = JobState.BC_CANCEL)
	public void handleBcCancelAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from BC_SUBMIT to BC_CANCEL");
		BCCancelRequest request =
				(BCCancelRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "BC_CANCEL")
				.build();
		channels.bcRequestOut().send(message);
	}

	/**
	// end job successfully
	//@StatesOnTransition (source = JobState.BC_SUBMIT, target = JobState.JOB_COMPLETE)
	public void handleCompleteJob(StateContext<JobState, JobEvent> stateContext) {
		String jobId =
				(String) stateContext.getExtendedState().getVariables().get("jobId");
		String txnId =
				(String) stateContext.getExtendedState().getVariables().get("txnId");
		persistJobStatus(Long.valueOf(jobId), txnId, JobState.JOB_COMPLETE.toString());
	}

	// fail job
	//@StatesOnTransition (source = JobState.FILE_CANCEL, target = JobState.JOB_FAIL)
	public void handleFailJob(StateContext<JobState, JobEvent> stateContext) {
		String jobId =
				(String) stateContext.getExtendedState().getVariables().get("jobId");
		String txnId =
				(String) stateContext.getExtendedState().getVariables().get("txnId");
		persistJobStatus(Long.valueOf(jobId), txnId, JobState.JOB_FAIL.toString());
	}

	private void persistJobStatus(Long jobId, String txnId, String state) {
		Job transaction = repository.findTransactionByJobIdAndTxnId(jobId, txnId);
		transaction.setJobState(state);
		transaction.setEndTS(new Timestamp(System.currentTimeMillis()));
		repository.save(transaction);
	}
	 */

}
