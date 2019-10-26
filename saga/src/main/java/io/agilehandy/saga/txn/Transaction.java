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
package io.agilehandy.saga.txn;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * @author Haytham Mohamed
 **/

@Entity (name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

	@Id
	private Long jobId;

	private String txnId;

	private String jobTxnStatus;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date jobStartTS;

	@Temporal(TemporalType.TIMESTAMP)
	private Date jobEndTS;

	private String fileId;
	private String fileName;
	private String fileTxnStatus;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fileStatusTS;

	private String dbRecordId;
	private String dbTxnStatus;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dbStatusTS;

	private String bcRecordId;
	private String bcTxnStatus;

	@Temporal(TemporalType.TIMESTAMP)
	private Date bcStatusTS;
}


