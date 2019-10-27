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
package io.agilehandy.txn.job;

/**
 * @author Haytham Mohamed
 **/

//@Service
public class JobService {

	/**
	private final JobRepository repository;
	private final StateMachineFactory factory;

	public JobService(JobRepository repository, StateMachineFactory factory) {
		this.repository = repository;
		this.factory = factory;
	}

	public Job create(FileSubmitRequest fs, DBSubmitRequest db, BCSubmitRequest bc) {
		Job txn = new Job();
		txn.setJobId(Long.valueOf(fs.getJobId().toString()));
		txn.setTxnId(fs.getGlobalTxnId().toString());
		txn.setJobState(JobState.JOB_START.name());
		txn.setFileId(fs.getFileId().toString());
		txn.setDbRecordId(db.getRecordId().toString());
		txn.setBcRecordId(bc.getContentId().toString());
		txn.setStartTS(new Timestamp(System.currentTimeMillis()));
		return repository.save(txn);
	}

	public StateMachine<JobState,JobEvent> submitFile(Long jobId, String txnId, FileSubmitRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("JOB_TXN_START")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	public StateMachine<JobState,JobEvent> cancelFile(Long jobId, String txnId, FileCancelRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("FILE_CANCEL")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	public StateMachine<JobState,JobEvent> submitDB(Long jobId, String txnId, DBSubmitRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("FILE_SUBMIT_COMPLETE")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	public StateMachine<JobState,JobEvent> cancelDB(Long jobId, String txnId, DBCancelRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("DB_CANCEL")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	public StateMachine<JobState,JobEvent> submitBC(Long jobId, String txnId, BCSubmitRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("BC_SUBMIT")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	public StateMachine<JobState,JobEvent> cancelBC(Long jobId, String txnId, BCCancelRequest request) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		sm.getExtendedState().getVariables().put("request", request);
		Message message = MessageBuilder.withPayload("BC_CANCEL")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	// not in use
	public StateMachine<JobState,JobEvent> completeJob(Long jobId, String txnId) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		Message message = MessageBuilder.withPayload("JOB_COMPLETE")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	// not in use
	public StateMachine<JobState,JobEvent> failJob(Long jobId, String txnId) {
		StateMachine<JobState,JobEvent> sm = build(jobId, txnId);
		Message message = MessageBuilder.withPayload("JOB_FAIL")
				.setHeader("jobId", jobId)
				.setHeader("txnId", txnId)
				.build();
		sm.sendEvent(Mono.just(message)).subscribe();
		return sm;
	}

	private StateMachine<JobState, JobEvent> build(Long jobId, String txnId) {
		Job txn = repository.findTransactionByJobIdAndTxnId(jobId, txnId);
		StateMachine<JobState,JobEvent> machine = factory.getStateMachine(UUID.fromString(txnId));
		// TODO: reset to current txn state and add a listener to update state in db in pre state change
		return machine;
	}
	 */
}
