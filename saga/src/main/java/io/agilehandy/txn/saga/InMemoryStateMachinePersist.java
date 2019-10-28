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

import java.util.HashMap;

import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import lombok.extern.log4j.Log4j2;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
public class InMemoryStateMachinePersist implements StateMachinePersist<JobState, JobEvent,String> {

	private final HashMap<String, StateMachineContext<JobState, JobEvent>> contexts = new HashMap<>();

	@Override
	public void write(StateMachineContext<JobState, JobEvent> context, String key) throws Exception {
		log.info("[persister] persisting machine with key: " + key + " and state: " + context.getState().name());
		contexts.put(key, context);
	}

	@Override
	public StateMachineContext<JobState, JobEvent> read(String key) throws Exception {
		StateMachineContext<JobState, JobEvent> sm = contexts.get(key);
		log.info("[persister] restoring machine with key: " + key + " and state: " + sm.getState().name());
		return sm;
	}
}
