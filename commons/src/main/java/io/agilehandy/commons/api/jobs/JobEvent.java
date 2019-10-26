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
package io.agilehandy.commons.api.jobs;

/**
 * @author Haytham Mohamed
 **/
public enum JobEvent {

	JOB_TXN_START,
	JOB_TXN_COMPLETE,
	JOB_TXN_FAIL,

	FILE_SUBMIT_COMPLETE,
	FILE_SUBMIT_FAIL,
	FILE_CANCEL_COMPLETE,
	FILE_CANCEL_FAIL,

	DB_SUBMIT_COMPLETE,
	DB_SUBMIT_FAIL,
	DB_CANCEL_COMPLETE,
	DB_CANCEL_FAIL,

	BC_SUBMIT_COMPLETE,
	BC_SUBMIT_FAIL,
	BC_CANCEL_COMPLETE,
	BC_CANCEL_FAIL
	;
}
