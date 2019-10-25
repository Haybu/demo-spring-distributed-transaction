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

import io.agilehandy.commons.api.events.JobResponseValues;
import io.agilehandy.commons.api.events.blockchain.BCRequest;
import io.agilehandy.commons.api.events.blockchain.BCTxnResponse;

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
public class BCHandler {

	@Value("${bc.max}")
	private int max;

	@Value("${bc.higherBound}")
	private int higherBound;

	@Value("${bc.lowerBound}")
	private int lowerBound;

	@Value("${bc.delay}")
	private int delay;

	private final EventChannels eventChannels;

	public BCHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.BC_REQUEST
			, condition = "headers['event_task']=='BC_SUBMIT'")
	public void  handleSubmitBC(@Valid BCRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, delay);
		BCTxnResponse response = (result)?
				createBCResponse(request, JobResponseValues.BC_TXN_COMPLETED) :
				createBCResponse(request, JobResponseValues.BC_TXN_FAIL);

		Message<BCTxnResponse> message = MessageBuilder.withPayload(response)
				.setHeader("event_task", "BC_SUBMIT")
				.build();
		eventChannels.txnResponse().send(message);
	}

	@StreamListener(target = EventChannels.BC_REQUEST
			, condition = "headers['event_task']=='BC_CANCEL'")
	public void handleCancelBC(@Valid BCRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, 1);
		BCTxnResponse response = (result)?
				createBCResponse(request, JobResponseValues.BC_TXN_COMPLETED) :
				createBCResponse(request, JobResponseValues.BC_TXN_FAIL);

		Message<BCTxnResponse> message = MessageBuilder.withPayload(response)
				.setHeader("event_task", "BC_CANCEL")
				.build();
		eventChannels.txnResponse().send(message);
	}

	private BCTxnResponse createBCResponse(BCRequest request, JobResponseValues result) {
		BCTxnResponse response = new BCTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setJobId(request.getJobId());
		return response;
	}

}
