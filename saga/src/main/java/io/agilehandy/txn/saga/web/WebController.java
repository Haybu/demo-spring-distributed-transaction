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
package io.agilehandy.txn.saga.web;

import java.util.UUID;

import io.agilehandy.commons.api.jobs.JobState;
import io.agilehandy.txn.saga.Saga;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Haytham Mohamed
 **/

@RestController
public class WebController {

	private final Saga saga;

	public WebController(Saga saga) {
		this.saga = saga;
	}

	@PostMapping
	public TxnResponse runTransaction(@RequestBody TxnRequest request) {
		UUID txnId = UUID.randomUUID();
		Long jobId = request.getId();
		request.getBlockchain().setGlobalTxnId(txnId);
		request.getStorage().setGlobalTxnId(txnId);
		request.getMetadata().setGlobalTxnId(txnId);
		JobState state = saga.orchestrate(jobId, request.getStorage()
				, request.getMetadata(), request.getBlockchain());
		TxnResponse response = new TxnResponse();
		response.setJobId(jobId);
		response.setTxnId(txnId.toString());
		response.setState(state);
		return response;
	}
}
