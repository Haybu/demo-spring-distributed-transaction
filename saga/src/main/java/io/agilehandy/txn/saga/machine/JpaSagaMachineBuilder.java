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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;

/**
 * @author Haytham Mohamed
 **/
@Profile("jpa")
public class JpaSagaMachineBuilder implements SagaStateMachineBuilder {

	@Override
	public StateMachine<JobState, JobEvent> build(String jobId, String txnId, boolean isFirstEvent) {
		return null;
	}


	@Configuration
	@Profile("jpa")
	public static class JpaPersisterConfig {

		@Bean
		public StateMachineRuntimePersister<JobState, JobEvent, String> stateMachineRuntimePersister(
				JpaStateMachineRepository jpaStateMachineRepository) {
			return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
		}
	}
}
