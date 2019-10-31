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
package io.agilehandy.txn;

import java.util.UUID;

import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.database.DBSubmitRequest;
import io.agilehandy.commons.api.storage.FileSubmitRequest;
import io.agilehandy.txn.saga.Saga;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class TxnClient implements ApplicationRunner {

	private final Saga saga;

	public TxnClient(Saga saga) {
		this.saga = saga;
	}


	@Override
	public void run(ApplicationArguments args) throws Exception {

		Long jobId = 123L;

		FileSubmitRequest request1 = new FileSubmitRequest();
		request1.setFileId(UUID.randomUUID());
		request1.setFilename("shipping_file.txt");
		request1.setContent("some content".getBytes());

		DBSubmitRequest request2 = new DBSubmitRequest();
		request2.setField1("f1");
		request2.setField2("f2");
		request2.setRecordId(UUID.randomUUID());

		BCSubmitRequest request3 = new BCSubmitRequest();
		request3.setContent("some bc content".getBytes());
		request3.setContentId(UUID.randomUUID());

		saga.orchestrate(jobId, request1, request2, request3);

	}
}
