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
package io.agilehandy.txn.job;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Haytham Mohamed
 **/

@Entity (name = "jobs")
@Data
@NoArgsConstructor
public class Job {

	@Id
	private Long jobId;

	private String txnId;

	private String jobState;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTS;

	@Temporal(TemporalType.TIMESTAMP)
	private Date endTS;

	@UpdateTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedTS;

	private String fileId;
	private String fileTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date fileTxnStatusTS;

	private String dbRecordId;
	private String dbTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date dbTxnStatusTS;

	private String bcRecordId;
	private String bcTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date bcTxnStatusTS;
}


