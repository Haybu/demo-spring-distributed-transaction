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

import io.agilehandy.commons.api.blockchain.BCCancelRequest;
import io.agilehandy.commons.api.blockchain.BCRequest;
import io.agilehandy.commons.api.blockchain.BCSubmitRequest;
import io.agilehandy.commons.api.blockchain.BCTxnResponse;
import io.agilehandy.commons.api.jobs.JobEvent;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Haytham Mohamed
 **/

@Log4j2
@Configuration
@EnableBinding(EventChannels.class)
public class BCHandler {

	@Value("${bc.max}")
	private int max;

	@Value("${bc.submit_higher_bound}")
	private int submitHigherBound;

	@Value("${bc.submit_lower_bound}")
	private int submitLowerBound;

	@Value("${bc.cancel_higher_bound}")
	private int cancelHigherBound;

	@Value("${bc.cancel_lower_bound}")
	private int cancelLowerBound;

	@Value("${bc.delay}")
	private int delay;

	private final EventChannels eventChannels;

	public BCHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.BC_REQUEST_IN
			, condition = "headers['saga_request']=='BC_SUBMIT'")
	public void  handleSubmitBC(@Payload BCSubmitRequest request) {
		log.info("remote blockchain service receives submit message to process");
		boolean result = Utilities.simulateTxn(max, submitLowerBound, submitHigherBound, delay);
		BCTxnResponse response = (result)?
				createBCResponse(request, JobEvent.BC_SUBMIT_COMPLETE) :
				createBCResponse(request, JobEvent.BC_SUBMIT_FAIL);
		sendResponse(response);
	}

	@StreamListener(target = EventChannels.BC_REQUEST_IN
			, condition = "headers['saga_request']=='BC_CANCEL'")
	public void handleCancelBC(@Payload BCCancelRequest request) {
		log.info("remote blockchain service receives cancel message to process");
		boolean result = Utilities.simulateTxn(max, cancelLowerBound, cancelHigherBound, 1);
		BCTxnResponse response = (result)?
				createBCResponse(request, JobEvent.BC_CANCEL_COMPLETE) :
				createBCResponse(request, JobEvent.BC_CANCEL_FAIL);
		sendResponse(response);
	}

	private void sendResponse(BCTxnResponse response) {
		Message message = MessageBuilder.withPayload(response)
				.setHeader("saga_response", response.getResponse())
				.build();
		eventChannels.txnResponseOut().send(message);
	}

	private BCTxnResponse createBCResponse(BCRequest request, JobEvent result) {
		BCTxnResponse response = new BCTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setJobId(request.getJobId());
		return response;
	}

}
