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
import io.agilehandy.commons.api.storage.FileRequest;
import io.agilehandy.commons.api.storage.FileTxnResponse;

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
public class FileHandler {

	@Value("${file.max}")
	private int max;

	@Value("${file.higherBound}")
	private int higherBound;

	@Value("${file.lowerBound}")
	private int lowerBound;

	@Value("${file.delay}")
	private int delay;

	private final EventChannels eventChannels;

	public FileHandler(EventChannels eventChannels) {
		this.eventChannels = eventChannels;
	}

	@StreamListener(target = EventChannels.FILE_REQUEST
			, condition = "headers['saga_request']=='FILE_SUBMIT'")
	public void handleSubmitFile(@Valid FileRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, delay);
		FileTxnResponse response = (result)?
				createFileResponse(request, JobEvent.FILE_SUBMIT_COMPLETE) :
				createFileResponse(request, JobEvent.FILE_SUBMIT_FAIL);
		sendResponse(response);
	}

	@StreamListener(target = EventChannels.FILE_REQUEST
			, condition = "headers['saga_request']=='FILE_CANCEL'")
	public void handleCancelFile(@Valid FileRequest request) {
		boolean result = Utilities.simulateTxn(max, lowerBound, higherBound, 1);
		FileTxnResponse response = (result)?
				createFileResponse(request, JobEvent.FILE_CANCEL_COMPLETE) :
				createFileResponse(request, JobEvent.FILE_CANCEL_FAIL);
		sendResponse(response);
	}

	private void sendResponse(FileTxnResponse response) {
		Message message = MessageBuilder.withPayload(response)
				.setHeader("saga_response", response.getResponse())
				.build();
		eventChannels.txnResponse().send(message);
	}

	private FileTxnResponse createFileResponse(FileRequest request, JobEvent result) {
		FileTxnResponse response = new FileTxnResponse();
		response.setResponse(result);
		response.setGlobalTxnId(request.getGlobalTxnId());
		response.setFileId(request.getFileId());
		response.setFilename(request.getFilename());
		response.setJobId(request.getJobId());
		return response;
	}

}
