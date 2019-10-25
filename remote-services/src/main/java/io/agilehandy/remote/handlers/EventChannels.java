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


package io.agilehandy.remote.handlers;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Haytham Mohamed
 **/

public interface EventChannels {

	String FILE_REQUEST = "file_request";
	String DB_REQUEST = "db_request";
	String BC_REQUEST = "bc_request";

	String TXN_RESPONSE = "txn_response";

	@Input(FILE_REQUEST)
	SubscribableChannel fileRequest();

	@Input(DB_REQUEST)
	SubscribableChannel dbRequest();

	@Input(BC_REQUEST)
	SubscribableChannel bcRequest();

	@Output(TXN_RESPONSE)
	MessageChannel txnResponse();
}
