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

import io.agilehandy.commons.api.events.database.DBRequest;
import io.agilehandy.commons.api.events.database.DBResponseValues;
import io.agilehandy.commons.api.events.database.DBTxnResponse;

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
public class DBHandler {

	@Value("db.max")
	private int max;

	@Value("db.higherBound")
	private int higherBound;

	@Value("db.lowerBound")
	private int lowerBound;

	@Value("db.delay")
	private int delay;

	private final EventChannels eventChannels;

	public DBHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.DB_REQUEST
			, condition = "headers['event_task']=='DB_SUBMIT'")
	@SendTo(EventChannels.TXN_RESPONSE)
	public DBTxnResponse handleSubmitDB(@Valid DBRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, delay);
		return (result)?
				createDBResponse(request, DBResponseValues.DB_TXN_COMPLETED) :
				createDBResponse(request, DBResponseValues.DB_TXN_ABORTED);
	}

	@StreamListener(target = EventChannels.DB_REQUEST
			, condition = "headers['event_task']=='DB_CANCEL'")
	@SendTo(EventChannels.TXN_RESPONSE)
	public DBTxnResponse handleCancelDB(@Valid DBRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, 1);
		return (result)?
				createDBResponse(request, DBResponseValues.DB_TXN_COMPLETED) :
				createDBResponse(request, DBResponseValues.DB_TXN_ABORTED);
	}

	private DBTxnResponse createDBResponse(DBRequest request, DBResponseValues result) {
		DBTxnResponse response = new DBTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setJobId(request.getJobId());
		response.setRecordId(request.getRecordId());
		return response;
	}

}
