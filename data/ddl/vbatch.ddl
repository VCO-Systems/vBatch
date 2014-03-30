
CREATE SEQUENCE JOB_DEFINITION_ID_SEQ;

CREATE TABLE vbatch.job_definition (
                id NUMBER NOT NULL,
                order_num NUMBER NOT NULL,
                short_desc VARCHAR2(20),
                long_desc VARCHAR2(150),
                CONSTRAINT JOB_DEFINITION_PK PRIMARY KEY (id)
);

CREATE SEQUENCE STEPS_ID_SEQ;

CREATE TABLE vbatch.steps (
                id NUMBER NOT NULL,
                type VARCHAR2(50) NOT NULL,
                short_desc VARCHAR2(20),
                long_desc VARCHAR2(150),
                class_path VARCHAR2(150),
                extract_max_rec NUMBER,
                extract_max_rec_per_file NUMBER,
                extract_commit_freq NUMBER,
                output_file_format VARCHAR2(10),
                output_filename_prefix VARCHAR2(100),
                output_filename_postfix VARCHAR2(100),
                extract_sql VARCHAR2(4000),
                param1 VARCHAR2(100),
                param2 VARCHAR2(50),
                param3 VARCHAR2(50),
                CONSTRAINT ID PRIMARY KEY (id)
);


CREATE SEQUENCE JOB_STEPS_XREF_ID_SEQ;

CREATE TABLE vbatch.job_steps_xref (
                id NUMBER NOT NULL,
                job_definition_id NUMBER NOT NULL,
                step_id NUMBER NOT NULL,
                job_step_seq NUMBER NOT NULL,
                special_mode VARCHAR2(25),
                CONSTRAINT JOB_STEPS_XREF_PK PRIMARY KEY (id)
);

CREATE SEQUENCE BATCH_LOG_ID_SEQ;

CREATE TABLE vbatch.batch_log (
                id NUMBER NOT NULL,
                job_definition_id NUMBER NOT NULL,
                order_num NUMBER,
                batch_num NUMBER NOT NULL,
                batch_seq_nbr NUMBER NOT NULL,
                short_desc VARCHAR2(20),
                long_desc VARCHAR2(150),
                status VARCHAR2(50),
                error_msg VARCHAR2(4000),
                start_dt TIMESTAMP,
                end_dt TIMESTAMP,
                CONSTRAINT BATCH_LOG_PK PRIMARY KEY (id)
);

CREATE SEQUENCE BATCH_LOG_DTL_ID_SEQ;

CREATE TABLE vbatch.batch_log_dtl (
                id NUMBER NOT NULL,
                batch_log_id NUMBER NOT NULL,
                start_dt TIMESTAMP,
                end_dt TIMESTAMP,
                status VARCHAR2(50),
                long_desc VARCHAR2(150),
                error_msg VARCHAR2(150),
                num_records NUMBER ,
                min_ok1 VARCHAR2(150),
                max_ok1 VARCHAR2(150),
                extract_max_recs NUMBER,
                extract_max_recs_per_file NUMBER,
                extract_commit_freq NUMBER,
                output_file_format VARCHAR2(15),
                output_filename_prefix VARCHAR2(150),
                output_filename_suffix VARCHAR2(150),
                class_path VARCHAR2(150),
                job_steps_xref_job_step_seq NUMBER,
                steps_id NUMBER,
                steps_short_desc VARCHAR2(20),
                step_type VARCHAR2(150),
                extract_sql VARCHAR2(4000),
                param1 VARCHAR2(150),
                param2 VARCHAR2(150),
                param3 VARCHAR2(150),
                CONSTRAINT BATCH_LOG_DTL_PK PRIMARY KEY (id)
);

CREATE SEQUENCE BATCH_LOG_OK_DTL_ID_SEQ;

CREATE TABLE vbatch.batch_log_ok_dtl (
                id NUMBER NOT NULL,
                batch_log_id NUMBER NOT NULL,
                pk1 NUMBER,
                pk2 NUMBER,
                pk3 NUMBER,
                ok1 VARCHAR2(150) NOT NULL,
                CONSTRAINT BATCH_LOG_OK_DTL_PK PRIMARY KEY (id)
);


CREATE SEQUENCE BATCH_LOG_FILE_OUTPUT_ID_SEQ;

CREATE TABLE vbatch.batch_log_file_output (
                id NUMBER NOT NULL,
                batch_log_id NUMBER NOT NULL,
                create_dt TIMESTAMP NOT NULL,
                filename VARCHAR2(150),
                num_records NUMBER,
                CONSTRAINT BATCH_LOG_FILE_OUTPUT_PK PRIMARY KEY (id)
);


ALTER TABLE vbatch.job_steps_xref ADD CONSTRAINT JOB_STEPS_XREF_JOB_DEF_FK
FOREIGN KEY (job_definition_id)
REFERENCES vbatch.job_definition (id)
NOT DEFERRABLE;

ALTER TABLE vbatch.batch_log ADD CONSTRAINT JOB_DEFINITION_VBATCH_LOG_FK
FOREIGN KEY (job_definition_id)
REFERENCES vbatch.job_definition (id)
NOT DEFERRABLE;

ALTER TABLE vbatch.batch_log_dtl ADD CONSTRAINT VBATCH_LOG_VBATCH_LOG_DTL_FK
FOREIGN KEY (batch_log_id)
REFERENCES vbatch.batch_log (id)
NOT DEFERRABLE;

ALTER TABLE vbatch.batch_log_file_output ADD CONSTRAINT VBATCH_LOG_VBATCH_LOG_FILE_936
FOREIGN KEY (batch_log_id)
REFERENCES vbatch.batch_log (id)
NOT DEFERRABLE;

ALTER TABLE vbatch.batch_log_ok_dtl ADD CONSTRAINT VBATCH_LOG_VBATCH_LOG_OK_DT859
FOREIGN KEY (batch_log_id)
REFERENCES vbatch.batch_log (id)
NOT DEFERRABLE;

ALTER TABLE vbatch.job_steps_xref ADD CONSTRAINT JOB_STEPS_XREF_STEPS_FK
FOREIGN KEY (step_id)
REFERENCES vbatch.steps (id)
NOT DEFERRABLE;
EXIT;