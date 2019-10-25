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

import io.agilehandy.commons.api.events.blockchain.BCRequest;
import io.agilehandy.commons.api.events.blockchain.BCResponseValues;
import io.agilehandy.commons.api.events.blockchain.BCTxnResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.SendTo;

/**
 * @author Haytham Mohamed
 **/

@Configuration
@EnableBinding(EventChannels.class)
public class BCHandler {

	@Value("bc.max")
	private int max;

	@Value("bc.higherBound")
	private int higherBound;

	@Value("bc.lowerBound")
	private int lowerBound;

	@Value("bc.delay")
	private int delay;

	private final EventChannels eventChannels;

	public BCHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.BC_REQUEST
			, condition = "headers['event_task']=='BC_SUBMIT'")
	@SendTo(EventChannels.TXN_RESPONSE)
	public BCTxnResponse handleSubmitBC(@Valid BCRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, delay);
		return (result)?
				createBCResponse(request, BCResponseValues.BC_TXN_COMPLETED) :
				createBCResponse(request, BCResponseValues.BC_TXN_ABORTED);
	}

	@StreamListener(target = EventChannels.BC_REQUEST
			, condition = "headers['event_task']=='BC_CANCEL'")
	@SendTo(EventChannels.TXN_RESPONSE)
	public BCTxnResponse handleCancelBC(@Valid BCRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, 1);
		return (result)?
				createBCResponse(request, BCResponseValues.BC_TXN_COMPLETED) :
				createBCResponse(request, BCResponseValues.BC_TXN_ABORTED);
	}

	private BCTxnResponse createBCResponse(BCRequest request, BCResponseValues result) {
		BCTxnResponse response = new BCTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setJobId(request.getJobId());
		return response;
	}

}
