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
package io.agilehandy.remote.handlers;

import javax.validation.Valid;

import io.agilehandy.commons.api.jobs.JobEvent;
import io.agilehandy.commons.api.database.DBRequest;
import io.agilehandy.commons.api.database.DBTxnResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Haytham Mohamed
 **/

@Configuration
@EnableBinding(EventChannels.class)
public class DBHandler {

	@Value("${db.max}")
	private int max;

	@Value("${db.higherBound}")
	private int higherBound;

	@Value("${db.lowerBound}")
	private int lowerBound;

	@Value("${db.delay}")
	private int delay;

	private final EventChannels eventChannels;

	public DBHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.DB_REQUEST
			, condition = "headers['saga_request']=='DB_SUBMIT'")
	public void handleSubmitDB(@Valid DBRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, delay);
		DBTxnResponse response = (result)?
				createDBResponse(request, JobEvent.DB_SUBMIT_COMPLETE) :
				createDBResponse(request, JobEvent.DB_SUBMIT_FAIL);
		sendResponse(response);
	}

	@StreamListener(target = EventChannels.DB_REQUEST
			, condition = "headers['saga_request']=='DB_CANCEL'")
	public void handleCancelDB(@Valid DBRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, 1);
		DBTxnResponse response = (result)?
				createDBResponse(request, JobEvent.DB_CANCEL_COMPLETE) :
				createDBResponse(request, JobEvent.DB_CANCEL_FAIL);
		sendResponse(response);
	}

	private void sendResponse(DBTxnResponse response) {
		Message message = MessageBuilder.withPayload(response)
				.setHeader("saga_response", response.getResponse())
				.build();
		eventChannels.txnResponse().send(message);
	}

	private DBTxnResponse createDBResponse(DBRequest request, JobEvent result) {
		DBTxnResponse response = new DBTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setJobId(request.getJobId());
		response.setRecordId(request.getRecordId());
		return response;
	}

}
