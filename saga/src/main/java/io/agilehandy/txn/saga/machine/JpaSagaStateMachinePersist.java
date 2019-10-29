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

/**
 * @author Haytham Mohamed
 **/

//@Log4j2
//@Profile("jpa")
//@Component
public class JpaSagaStateMachinePersist {
		//implements StateMachinePersist<JobState, JobEvent,String> {

	/**
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

	private byte[] convertToBytes(Object object) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 ObjectOutput out = new ObjectOutputStream(bos)) {
			out.writeObject(object);
			return bos.toByteArray();
		}
	}

	private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			 ObjectInput in = new ObjectInputStream(bis)) {
			return in.readObject();
		}
	}
	*/

}
