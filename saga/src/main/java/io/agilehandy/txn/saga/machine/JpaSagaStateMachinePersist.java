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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.jobs.JobState;
import lombok.extern.log4j.Log4j2;

import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachine;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
@Profile("jpa")
@Component
public class JpaSagaStateMachinePersist
		implements StateMachinePersist<JobState, JobEvent,String> {


	private final JpaStateMachineRepository stateMachineRepository;

	public JpaSagaStateMachinePersist(JpaStateMachineRepository repository) {
		this.stateMachineRepository = repository;
	}

	@Override
	public void write(StateMachineContext<JobState, JobEvent> context, String key)
			throws Exception {
		log.info("[persister] persisting machine with key: " + key + " and state: "
				+ context.getState().name());
		JpaRepositoryStateMachine entity = new JpaRepositoryStateMachine();
		entity.setMachineId(key);
		entity.setState(context.getState().name());
		entity.setStateMachineContext(convertToBytes(context));
		stateMachineRepository.save(entity);
	}

	@Override
	public StateMachineContext<JobState, JobEvent> read(String key) throws Exception {
		JpaRepositoryStateMachine entity = stateMachineRepository.findById(key).get();
		StateMachineContext<JobState, JobEvent> context =
				(StateMachineContext)convertFromBytes(entity.getStateMachineContext());
		log.info("[persister] restoring machine with key: " + key + " and state: "
		 + context.getState().name());
		return context;
	}

	private byte[] convertToBytes(StateMachineContext<JobState, JobEvent> context) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Output output = new Output(bos);
			Kryo kryo = new Kryo();
			kryo.writeClassAndObject(output, context);
			byte[] bytes = output.toBytes();
			output.close();
			return bytes;
		}
	}

	private StateMachineContext<JobState, JobEvent> convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
			Kryo kryo = new Kryo();
			Input input = new Input(bis);
			StateMachineContext<JobState, JobEvent> context =
					(StateMachineContext)kryo.readClassAndObject(input);
			input.close();
			return context;
		}
	}

}
