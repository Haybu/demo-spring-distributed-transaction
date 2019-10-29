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

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.database.DBCancelRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.commons.api.storage.FileCancelRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.txn.saga.job.JobRepository;
import lombok.extern.log4j.Log4j2;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@EnableStateMachineFactory(contextEvents = false)
@Component
@Log4j2
public class SagaStateMachine extends EnumStateMachineConfigurerAdapter<JobState, JobEvent> {

	private final SagaChannels channels;
	private final JobRepository repository;
	private final SagaStateMachineMonitor monitor;

	public SagaStateMachine(SagaChannels channels, JobRepository repository, SagaStateMachineMonitor monitor) {
		this.channels = channels;
		this.repository = repository;
		this.monitor = monitor;
	}

	// build manually
	/**
	public StateMachine<JobState, JobEvent> buildStateMachine() throws Exception {
		StateMachineBuilder.Builder<JobState, JobEvent> builder = StateMachineBuilder.builder();
		configure(builder.configureConfiguration());
		configure(builder.configureStates());
		configure(builder.configureTransitions());
		return builder.build();
	}
	 */

	@Override
	public void configure(StateMachineConfigurationConfigurer<JobState, JobEvent> config) throws Exception {
		StateMachineListener<JobState, JobEvent> listener = new StateMachineListenerAdapter() {
			@Override
			public void stateMachineStarted(StateMachine stateMachine) {
				log.info("[listener] State machine started");
			}

			@Override
			public void stateExited(State state) {
				log.info("[listener] State machine exited");
			}

			@Override
			public void stateMachineStopped(StateMachine stateMachine) {
				log.info("[listener] State machine stopped");
			}

		};

		config
			.withConfiguration()
				.autoStartup(false)
				//.machineId("saga-machine")
				.listener(listener)
			.and().withMonitoring().monitor(monitor)
				;
	}

	@Override
	public void configure(StateMachineStateConfigurer<JobState, JobEvent> states) throws Exception {
		states.withStates()
				.initial(JobState.JOB_START)
				.end(JobState.JOB_COMPLETE)
				.end(JobState.JOB_FAIL)
				.state(JobState.FILE_SUBMIT, context -> handleFileSubmitAction(context), null)
				.state(JobState.FILE_CANCEL, context -> handleFileCancelAction(context), null)
				.state(JobState.DB_SUBMIT, context -> handleDbSubmitAction(context), null)
				.state(JobState.DB_CANCEL, context -> handleDbCancelAction(context), null)
				.state(JobState.BC_SUBMIT, context -> handleBcSubmitAction(context), null)
				.state(JobState.BC_CANCEL, context -> handleBcCancelAction(context), null)
				;
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
					//.action(context -> handleFileSubmitAction(context))
				.and().withExternal()
					.source(JobState.FILE_SUBMIT)
					.target(JobState.DB_SUBMIT)
					.event(JobEvent.FILE_SUBMIT_COMPLETE)
					//.action(context -> handleDbSubmitAction(context))
				.and().withExternal()
					.source(JobState.DB_SUBMIT)
					.target(JobState.BC_SUBMIT)
					.event(JobEvent.DB_SUBMIT_COMPLETE)
					//.action(context -> handleBcSubmitAction(context))
				.and().withExternal()
					.source(JobState.BC_SUBMIT)
					.target(JobState.JOB_COMPLETE)
					.event(JobEvent.BC_SUBMIT_COMPLETE)  // end of happy path

				.and().withExternal()     // if file submit fails
					.source(JobState.FILE_SUBMIT)
					.target(JobState.FILE_CANCEL)
					.event(JobEvent.FILE_SUBMIT_FAIL)
					//.action(context -> handleFileCancelAction(context) )
				.and().withExternal()
					.source(JobState.FILE_CANCEL)
					.target(JobState.JOB_FAIL)
					.event(JobEvent.FILE_CANCEL_COMPLETE)

				.and().withExternal()   // if DB submit fails
					.source(JobState.DB_SUBMIT)
					.target(JobState.DB_CANCEL)
					.event(JobEvent.DB_SUBMIT_FAIL)
					//.action(context -> handleDbCancelAction(context))
				.and().withExternal()
					.source(JobState.DB_CANCEL)
					.target(JobState.FILE_CANCEL)
					.event(JobEvent.DB_CANCEL_COMPLETE)
					//.action(context -> handleFileCancelAction(context) )

				.and().withExternal()  // if BC submit fails
					.source(JobState.BC_SUBMIT)
					.target(JobState.BC_CANCEL)
					.event(JobEvent.BC_SUBMIT_FAIL)
					//.action(context -> handleBcCancelAction(context))
				.and().withExternal()
					.source(JobState.BC_CANCEL)
					.target(JobState.DB_CANCEL)
					.event(JobEvent.BC_CANCEL_COMPLETE)
				.and().withExternal()
					.source(JobState.DB_CANCEL)
					.target(JobState.FILE_CANCEL)
					.event(JobEvent.DB_CANCEL_COMPLETE)

				.and().withInternal()
					.source(JobState.FILE_CANCEL)
					.action(context -> handleFileCancelAction(context))
					.event(JobEvent.FILE_CANCEL_FAIL)

				.and().withInternal()
					.source(JobState.DB_CANCEL)
					.action(context -> handleDbCancelAction(context))
					.event(JobEvent.DB_CANCEL_FAIL)

				.and().withInternal()
					.source(JobState.BC_CANCEL)
					.action(context -> handleBcCancelAction(context))
					.event(JobEvent.BC_CANCEL_FAIL)
				;
	}


	/*
	Machine Actions
	*/

	// submit a file txn
	//@StatesOnTransition(source = JobState.JOB_START, target = JobState.FILE_SUBMIT)
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
	//@StatesOnTransition (source = {JobState.FILE_SUBMIT, JobState.DB_CANCEL}, target = JobState.FILE_CANCEL)
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
	//@StatesOnTransition (source = JobState.FILE_SUBMIT, target = JobState.DB_SUBMIT)
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
	//@StatesOnTransition (source = {JobState.DB_SUBMIT, JobState.BC_CANCEL}, target = JobState.DB_CANCEL)
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
	//@StatesOnTransition (source = JobState.DB_SUBMIT, target = JobState.BC_SUBMIT)
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
	//@StatesOnTransition (source = JobState.BC_SUBMIT, target = JobState.BC_CANCEL)
	public void handleBcCancelAction(StateContext<JobState, JobEvent> stateContext) {
		log.info("state machine transits from BC_SUBMIT to BC_CANCEL");
		BCCancelRequest request =
				(BCCancelRequest) stateContext.getExtendedState().getVariables().get("request");
		Message message = MessageBuilder.withPayload(request)
				.setHeader("saga_request", "BC_CANCEL")
				.build();
		channels.bcRequestOut().send(message);
	}

}
