
create table txn.jobs (
    id INT NOT NULL,
    txn_id VARCHAR(100) NOT NULL,
    job_state VARCHAR(50) NOT NULL,
    start_ts DATE,
    end_ts DATE,
    updated_ts DATE,
    file_id VARCHAR (100),
    file_txn_status VARCHAR(100),
    file_txn_status_ts DATE,
	db_record_id VARCHAR(100),
	db_txn_status VARCHAR(100),
	db_txn_status_ts DATE,
	bc_record_id VARCHAR(100),
	bc_txn_status VARCHAR(100),
	bc_txn_status_ts DATE,
    PRIMARY KEY ( id )
);

create table txn.state_machine (
    machine_id VARCHAR (100),
    state VARCHAR(100),
    state_machine_context TINYBLOB,
    PRIMARY KEY ( machine_id )
);