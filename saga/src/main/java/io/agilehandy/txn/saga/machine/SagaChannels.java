/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.agilehandy.txn.saga.machine;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Haytham Mohamed
 **/

public interface SagaChannels {

	String FILE_REQUEST_OUT = "fileRequestOut";
	String DB_REQUEST_OUT = "dbRequestOut";
	String BC_REQUEST_OUT = "bcRequestOut";
	String TXN_RESPONSE_IN = "txnResponseIn";

	@Output(FILE_REQUEST_OUT)
	MessageChannel fileRequestOut();

	@Output(DB_REQUEST_OUT)
	MessageChannel dbRequestOut();

	@Output(BC_REQUEST_OUT)
	MessageChannel bcRequestOut();

	@Input(TXN_RESPONSE_IN)
	SubscribableChannel txnResponseIn();


}
