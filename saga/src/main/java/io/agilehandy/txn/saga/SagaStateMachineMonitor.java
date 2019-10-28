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

import java.util.function.Function;

import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.monitor.AbstractStateMachineMonitor;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
@Log4j2
public class SagaStateMachineMonitor extends AbstractStateMachineMonitor<JobState, JobEvent> {

	@Override
	public void transition(StateMachine<JobState, JobEvent> sm, Transition<JobState, JobEvent> trans, long duration) {
		if (trans.getSource() == null || trans.getTarget() == null) {
			return;
		}
		String source = trans.getSource().getId().name();
		String target = trans.getTarget().getId().name();
		String txnId = sm.getUuid().toString();

		log.info("[Monitor] Saga machine with txnId " + txnId
				+ " takes " + duration + " milliseconds to move from "
				+ source + " to " + target);

	}

	@Override
	public void action(StateMachine<JobState, JobEvent> sm
			, Function<StateContext<JobState, JobEvent>, Mono<Void>> action, long duration) {
		Transition<JobState, JobEvent> trans = sm.getTransitions().stream().findFirst().get();
		String source = trans.getSource().getId().name();
		String target = trans.getTarget().getId().name();
		String txnId = sm.getUuid().toString();

		log.info("[Monitor] Saga machine with txnId " + txnId
				+ " takes " + duration + " milliseconds to perform action when moving from "
				+ source + " to " + target);
	}
}
