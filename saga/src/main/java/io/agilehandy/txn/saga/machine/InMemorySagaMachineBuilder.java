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
package io.agilehandy.txn.saga.machine;

import java.util.UUID;

import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.txn.saga.job.Job;
import io.agilehandy.txn.saga.job.JobRepository;
import lombok.extern.log4j.Log4j2;

import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
@Component
public class InMemorySagaMachineBuilder implements SagaStateMachineBuilder {

	private final StateMachineFactory factory;
	private final JobRepository jobRepository;

	InMemoryStateMachinePersist stateMachinePersist = new InMemoryStateMachinePersist();
	StateMachinePersister<JobState, JobEvent, String> persister = new DefaultStateMachinePersister<>(stateMachinePersist);

	public InMemorySagaMachineBuilder(StateMachineFactory factory, JobRepository jobRepository) {
		this.factory = factory;
		this.jobRepository = jobRepository;
	}

	@Override
	public StateMachine<JobState, JobEvent> build(String jobId, String txnId, boolean isFirstEvent) {
		log.info("Building a machine");

		StateMachine<JobState,JobEvent> machine = factory.getStateMachine(UUID.fromString(txnId));
		machine.stop();

		if (!isFirstEvent) {
			try {
				log.info("Restoring a machine ");
				persister.restore(machine, txnId);
			}
			catch (Exception e) {
				log.error("Error restoring a state machine " + e);
			}
		}

		machine.getStateMachineAccessor()
				.doWithAllRegions(sma -> {
					sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<JobState,JobEvent>(){
						@Override
						public void preStateChange(State<JobState, JobEvent> state, Message<JobEvent> message, Transition<JobState, JobEvent> transition, StateMachine<JobState, JobEvent> stateMachine) {
							//log.info("[interceptor] state machine built is at state: " + stateMachine.getState().getId().name());
							String tempJobId = String.class.cast(message.getHeaders().getOrDefault("jobId", ""));
							String tempTxnId = String.class.cast(message.getHeaders().getOrDefault("txnId", ""));
							log.info("[interceptor] state machine interceptor accessing Job with jobId = "+tempJobId+" and txnId = "+tempTxnId);
							Job job = jobRepository.findTransactionByJobIdAndTxnId(tempJobId, tempTxnId);
							log.info("[interceptor] setting job to state: " + state.getId().name());
							job.setJobState(state.getId().name());
							jobRepository.save(job);
						}

						@Override
						public void postStateChange(State<JobState, JobEvent> state, Message<JobEvent> message, Transition<JobState, JobEvent> transition, StateMachine<JobState, JobEvent> stateMachine) {
							try {
								persister.persist(stateMachine, txnId);
							}
							catch (Exception e) {
								log.error("cannot persist a state machine for txnId " + txnId);
							}
						}
					});
				});

		machine.start();

		log.info("machine is now ready with state " + machine.getState().getId().name());
		return machine;
	}
}
