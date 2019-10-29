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
import io.agilehandy.txn.saga.job.Job;
import io.agilehandy.txn.saga.job.JobRepository;
import io.agilehandy.txn.saga.machine.SagaStateMachineBuilder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

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

	private Saga() {
		this (null, null);
	}

	public Saga(JobRepository repository, SagaStateMachineBuilder builder) {
		log.info("A new saga is instantiated.");
		this.jobRepository = repository;
		this.builder = builder;
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
		boolean isFirstCall = (signal == JobEvent.JOB_TXN_START);
		StateMachine<JobState,JobEvent> sm = builder.build(jobId, txnId, isFirstCall);
		log.info("machine signal to send is " + signal
				+ " to machine at state " + sm.getState().getId().name());
		if (request != null) {
			sm.getExtendedState().getVariables().put("request", request);
		}
		Message message = MessageBuilder.withPayload(signal)
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(message);
		return sm;
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_SUBMIT_COMPLETE'")
	public void handleFileSubmitComplete(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_SUBMIT_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='FILE_CANCEL_COMPLETE'")
	public void handleFileCancelComplete(@Payload FileTxnResponse response) {
		log.info("Saga receives response from remote file service with signal FILE_CANCEL_COMPLETE");
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,fileCancelRequest, JobEvent.FILE_CANCEL_FAIL);
	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='DB_SUBMIT_COMPLETE'")
	public void handleDBSubmitComplete(@Payload DBTxnResponse response) {
		log.info("Saga receives response from remote database service with signal DB_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,dbCancelRequest, JobEvent.DB_CANCEL_FAIL);

	}

	@StreamListener(target = SagaChannels.TXN_RESPONSE_IN
			, condition = "headers['saga_response']=='BC_SUBMIT_COMPLETE'")
	public void handleBCSubmitComplete(@Payload BCTxnResponse response) {
		log.info("Saga receives response from remote blockchain service with signal BC_SUBMIT_COMPLETE");
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
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
		signalStateMachine(response.getJobId().toString()
				, response.getGlobalTxnId().toString()
				,bcCancelRequest, JobEvent.BC_CANCEL_FAIL);
	}

}
