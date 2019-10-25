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
package io.agilehandy.saga.statemachine;

import java.util.EnumSet;

import io.agilehandy.commons.api.events.JobState;
import io.agilehandy.commons.api.events.JobEvent;
import lombok.extern.log4j.Log4j2;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

/**
 * @author Haytham Mohamed
 **/

@EnableStateMachineFactory(contextEvents = false)
@Log4j2
public class TxnManagerStateMachine
		extends EnumStateMachineConfigurerAdapter<JobState, JobEvent> {

	@Override
	public void configure(StateMachineStateConfigurer<JobState, JobEvent> states) throws Exception {
		states.withStates()
				.initial(JobState.JOB_START)
				.end(JobState.JOB_COMPLETE)
				.end(JobState.JOB_FAIL)
				.states(EnumSet.allOf(JobState.class))
		;
	}

	// look a typical demonstrating picture of a state machine at
	// https://dzone.com/articles/distributed-sagas-for-microservices
	@Override
	public void configure(StateMachineTransitionConfigurer<JobState, JobEvent> transitions)
			throws Exception {
		transitions
				.withExternal()
					.source(JobState.JOB_START)
					.target(JobState.FILE_SUBMIT)
					.event(JobEvent.JOB_TXN_START)
				.and().withExternal()
					.source(JobState.FILE_SUBMIT)
					.target(JobState.DB_SUBMIT)
					.event(JobEvent.FILE_SUBMIT_COMPLETE)
				.and().withExternal()
					.source(JobState.DB_SUBMIT)
					.target(JobState.BC_SUBMIT)
					.event(JobEvent.DB_SUBMIT_COMPLETE)
				.and().withExternal()
					.source(JobState.BC_SUBMIT)
					.target(JobState.JOB_COMPLETE)
					.event(JobEvent.BC_SUBMIT_COMPLETE)  // end of happy path

				.and().withExternal()     // if file submit fails
					.source(JobState.FILE_SUBMIT)
					.target(JobState.FILE_CANCEL)
					.event(JobEvent.FILE_SUBMIT_FAIL)
				.and().withExternal()
					.source(JobState.FILE_CANCEL)
					.target(JobState.JOB_FAIL)
					.event(JobEvent.FILE_CANCEL_COMPLETE)

				.and().withExternal()   // if DB submit fails
					.source(JobState.DB_SUBMIT)
					.source(JobState.DB_CANCEL)
					.event(JobEvent.DB_SUBMIT_FAIL)
				.and().withExternal()
					.source(JobState.DB_CANCEL)
					.source(JobState.FILE_CANCEL)
					.event(JobEvent.DB_CANCEL_COMPLETE)

				.and().withExternal()  // if BC submit fails
					.source(JobState.BC_SUBMIT)
					.source(JobState.BC_CANCEL)
					.event(JobEvent.BC_SUBMIT_FAIL)
				.and().withExternal()
					.source(JobState.BC_CANCEL)
					.target(JobState.DB_CANCEL)
					.event(JobEvent.BC_CANCEL_COMPLETE)
				.and().withExternal()
					.source(JobState.DB_CANCEL)
					.target(JobState.FILE_CANCEL)
					.event(JobEvent.DB_CANCEL_COMPLETE)
				;
	}

	@Override
	public void configure(StateMachineConfigurationConfigurer<JobState, JobEvent> config) throws Exception {
		StateMachineListener<JobState, JobEvent> listener = new StateMachineListenerAdapter() {
			@Override
			public void stateMachineStarted(StateMachine stateMachine) {
				log.info("State machine started");
			}

			@Override
			public void stateExited(State state) {
				log.info("State machine exited");
			}
		};

		config.withConfiguration()
				.autoStartup(false)
				.machineId("saga-machine")
				.listener(listener);
	}


}
