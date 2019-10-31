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
package io.agilehandy.txn.saga.job;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
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
@IdClass(JobKey.class)
@Data
@NoArgsConstructor
public class Job {

	@Id
	@Column(name = "id")
	//@GeneratedValue(strategy= GenerationType.AUTO)
	private Long jobId;

	@Id
	private String txnId;

	private String jobState;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTs;

	@Temporal(TemporalType.TIMESTAMP)
	private Date endTs;

	@UpdateTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedTs;

	private String fileId;
	private String fileTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date fileTxnStatusTs;

	private String dbRecordId;
	private String dbTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date dbTxnStatusTs;

	private String bcRecordId;
	private String bcTxnStatus;
	@Temporal(TemporalType.TIMESTAMP)
	private Date bcTxnStatusTs;
}


