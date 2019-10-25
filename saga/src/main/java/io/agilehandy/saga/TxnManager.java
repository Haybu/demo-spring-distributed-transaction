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
package io.agilehandy.saga;

import io.agilehandy.commons.api.events.JobEvent;
import io.agilehandy.commons.api.events.JobState;
import lombok.extern.log4j.Log4j2;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.annotation.OnStateMachineStart;
import org.springframework.statemachine.annotation.WithStateMachine;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
@WithStateMachine( id = "saga-machine")
@EnableBinding(Source.class)
public class TxnManager {

	private final Source source;

	public TxnManager(Source source) {
		this.source = source;
	}


	@OnStateMachineStart
	public void start(StateContext<JobState, JobEvent> stateContext) {

	}

}
