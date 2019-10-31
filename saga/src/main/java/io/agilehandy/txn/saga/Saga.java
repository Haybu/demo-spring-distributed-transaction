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
import java.util.Collection;
import java.util.UUID;

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.blockchain.BCTxnResponse;
import io.agilehandy.commons.api.database.DBCancelRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.database.DBTxnResponse;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobExchange;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.commons.api.storage.FileCancelRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.commons.api.storage.FileTxnResponse;
import io.agilehandy.txn.saga.job.Job;
import io.agilehandy.txn.saga.job.JobRepository;
import io.agilehandy.txn.saga.machine.SagaStateMachineBuilder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

/**
 * @author Haytham Mohamed
 **/

@Data
@Log4j2
@EnableBinding(SagaChannels.class)
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Saga {

	private final JobRepository jobRepository;
	private final SagaStateMachineBuilder builder;

	private FileSubmitRequest fileSubmitRequest;
	private DBSubmitRequest dbSubmitRequest;
	private BCSubmitRequest bcSubmitRequest;

	StateMachine<JobState,JobEvent> currentStateMachine;

	@Value("${saga.timeout}")
	private long timeout;

	private Saga() {
		this (null, null);
	}

	public Saga(JobRepository repository, SagaStateMachineBuilder builder) {
		log.info("A new saga is instantiated.");
		this.jobRepository = repository;
		this.builder = builder;
	}

	public JobState orchestrate(Long jobId, FileSubmitRequest fsr
			, DBSubmitRequest dbr, BCSubmitRequest bcr) {
		UUID txnId = UUID.randomUUID();
		fileSubmitRequest = fsr;
		dbSubmitRequest = dbr;
		bcSubmitRequest = bcr;
		return orchestrate(jobId, txnId);
	}

	private JobState orchestrate(Long jobId, UUID txnId) {
		if (!canOrchestrate(jobId)) {
			log.info("Cannot run this transaction as same Job with ID " + jobId + " is in progress.");
			return JobState.JOB_FAIL;
		}

		log.info("saga orchestration starts.");
		fileSubmitRequest.setGlobalTxnId(txnId);
		fileSubmitRequest.setJobId(jobId);
		dbSubmitRequest.setGlobalTxnId(txnId);
		dbSubmitRequest.setJobId(jobId);
		bcSubmitRequest.setGlobalTxnId(txnId);
		bcSubmitRequest.setJobId(jobId);
		Job job = createJob(fileSubmitRequest, dbSubmitRequest, bcSubmitRequest);
		return start(job.getJobId(), txnId.toString());
	}

	// checks job transaction DB to make sure there is no inflight txn
	private boolean canOrchestrate(Long jobId) {
		Collection<Job> jobs = jobRepository.findInProgressInstances(jobId);
		return jobs.isEmpty()? true : false;
	}

	// start by submitting the file
	private JobState start(Long jobId, String txnId) {
		signalStateMachine(jobId, txnId, fileSubmitRequest, JobEvent.JOB_TXN_START);
		long startTimestamp = System.currentTimeMillis();
		long currentTimestamp = startTimestamp;

		while(!currentStateMachine.isComplete()) {
			if (currentTimestamp - startTimestamp > timeout) {
				log.info("Distributed transaction for job Id " + jobId + " and txn Id " + txnId + " is timed out");
				signalStateMachine(jobId, txnId, null, JobEvent.JOB_TXN_TIMEOUT);
			}
			currentTimestamp = System.currentTimeMillis();
		}
		JobState finalState = currentStateMachine.getState().getId();
		log.info("Final Transaction state is " + finalState + ". Job completes in "
				+ (currentTimestamp - startTimestamp) + " milliseconds");
		return finalState;
	}

	private Job createJob(FileSubmitRequest fs, DBSubmitRequest db, BCSubmitRequest bc) {
		Job job = new Job();
		job.setJobId(fs.getJobId());
		job.setTxnId(fs.getGlobalTxnId().toString());
		job.setJobState(JobState.JOB_START.name());
		job.setFileId(fs.getFileId().toString());
		job.setDbRecordId(db.getRecordId().toString());
		job.setBcRecordId(bc.getContentId().toString());
		job.setStartTs(new Timestamp(System.currentTimeMillis()));
		return jobRepository.save(job);
	}

	public void signalStateMachine(Long jobId, String txnId
			, JobExchange request, JobEvent signal) {
		boolean isFirstCall = (signal == JobEvent.JOB_TXN_START);
		currentStateMachine = builder.getStateMachine(txnId, isFirstCall);

		if (currentStateMachine.isComplete()) {
			log.info("Transaction already existed. No action to take.");
			return;
		}

		log.info("machine signal to send is " + signal
				+ " to machine with state " + currentStateMachine.getState().getId().name());
		if (request != null) {
			currentStateMachine.getExtendedState().getVariables().put("request", request);
		}
		Message message = MessageBuilder.withPayload(signal)
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		currentStateMachine.sendEvent(Mono.just(message)).blockLast();
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_SUBMIT_COMPLETE'")
	public void handleFileSubmitComplete(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,dbSubmitRequest, JobEvent.FILE_SUBMIT_COMPLETE);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_SUBMIT_FAIL'")
	public void handleFileSubmitFail(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_SUBMIT_FAIL");
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(response.getFilename());
		fileCancelRequest.setFileId(response.getFileId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_SUBMIT_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_CANCEL_COMPLETE'")
	public void handleFileCancelComplete(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_CANCEL_COMPLETE");
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,null, JobEvent.FILE_CANCEL_COMPLETE);
	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_CANCEL_FAIL'")
	public void handleFileCancelFail(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_CANCEL_FAIL");
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(response.getFilename());
		fileCancelRequest.setFileId(response.getFileId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_CANCEL_FAIL);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='DB_SUBMIT_COMPLETE'")
	public void handleDBSubmitComplete(@Payload DBTxnResponse response) {
		log.info("Saga receives response from remote database service with signal DB_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,bcSubmitRequest, JobEvent.DB_SUBMIT_COMPLETE);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='DB_SUBMIT_FAIL'")
	public void handleDBSubmitFail(@Payload DBTxnResponse response) {
		log.info("Saga receives response from remote database service with signal DB_SUBMIT_FAIL");
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(response.getRecordId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.DB_SUBMIT_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='DB_CANCEL_COMPLETE'")
	public void handleDBCancelComplete(@Payload DBTxnResponse response) {
		log.info("Saga receives response from remote database service with signal DB_CANCEL_COMPLETE");
		FileCancelRequest fileCancelRequest = new FileCancelRequest();
		fileCancelRequest.setJobId(response.getJobId());
		fileCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		fileCancelRequest.setFilename(fileSubmitRequest.getFilename());
		fileCancelRequest.setFileId(fileSubmitRequest.getFileId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.DB_CANCEL_COMPLETE);

	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='DB_CANCEL_FAIL'")
	public void handleDBCancelFail(@Payload DBTxnResponse response) {
		log.info("Saga receives response from remote database service with signal DB_CANCEL_FAIL");
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(response.getRecordId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.DB_CANCEL_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='BC_SUBMIT_COMPLETE'")
	public void handleBCSubmitComplete(@Payload BCTxnResponse response) {
		log.info("Saga receives response from remote blockchain service with signal BC_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,null, JobEvent.BC_SUBMIT_COMPLETE);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='BC_SUBMIT_FAIL'")
	public void handleBCSubmitFail(@Payload BCTxnResponse response) {
		log.info("Saga receives response from remote blockchain service with signal BC_SUBMIT_FAIL");
		BCCancelRequest bcCancelRequest = new BCCancelRequest();
		bcCancelRequest.setJobId(response.getJobId());
		bcCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		bcCancelRequest.setContentId(response.getContentId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,bcCancelRequest, JobEvent.BC_SUBMIT_FAIL);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='BC_CANCEL_COMPLETE'")
	public void handleBCCancelComplete(@Payload BCTxnResponse response) {
		log.info("Saga receives response from remote blockchain service with signal BC_CANCEL_COMPLETE");
		DBCancelRequest dbCancelRequest = new DBCancelRequest();
		dbCancelRequest.setJobId(response.getJobId());
		dbCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		dbCancelRequest.setRecordId(dbSubmitRequest.getRecordId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.BC_CANCEL_COMPLETE);
	}

	// TODO: needs a continuous timer action in the state machine transition
	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='BC_CANCEL_FAIL'")
	public void handleBCCancelFail(@Payload BCTxnResponse response) {
		log.info("Saga receives response from remote blockchain service with signal BC_CANCEL_FAIL");
		BCCancelRequest bcCancelRequest = new BCCancelRequest();
		bcCancelRequest.setJobId(response.getJobId());
		bcCancelRequest.setGlobalTxnId(response.getGlobalTxnId());
		bcCancelRequest.setContentId(response.getContentId());
		signalStateMachine(response.getJobId()
				, response.getGlobalTxnId().toString()
				,bcCancelRequest, JobEvent.BC_CANCEL_FAIL);
	}

}
