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

import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.txn.saga.job.JobRepository;
import lombok.extern.log4j.Log4j2;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/
@Log4j2
@Component
public class SagaStateMachineJpaBuilder implements SagaStateMachineBuilder {

	private final StateMachineService<JobState, JobEvent> stateMachineService;
	private final JobRepository jobRepository;

	public SagaStateMachineJpaBuilder(StateMachineService<JobState, JobEvent> stateMachineService
			, JobRepository jobRepository) {
		this.stateMachineService = stateMachineService;
		this.jobRepository = jobRepository;
	}

	@Override
	public StateMachine<JobState, JobEvent> build(String jobId, String txnId, boolean isFirstEvent) {
		log.info("Building a machine");
		StateMachine<JobState,JobEvent> machine = stateMachineService.acquireStateMachine(txnId);
		if (isFirstEvent) {
			machine.getStateMachineAccessor()
					.doWithAllRegions(sma ->
						sma.addStateMachineInterceptor(new SagaStateMachineInterceptor(stateMachineService, jobRepository)));
		}
		machine.startReactively().block();
		log.info("machine is now ready with state " + machine.getState().getId().name());
		return machine;
	}

}
