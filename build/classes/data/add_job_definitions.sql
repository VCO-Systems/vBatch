-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
INSERT INTO JOB_DEFINITION (short_desc,long_desc,order_num) VALUES ('Replenishment', 'Gather all replenishment data.',1);
INSERT INTO JOB_DEFINITION (short_desc,long_desc,order_num) VALUES ('Putaway', 'Gather all Putaway data',2);

-- Steps
INSERT INTO STEPS (type,  extract_commit_freq,extract_sql, short_desc,long_desc)
    VALUES ('Extract',  1000, 'SELECT * FROM PROD_TRKG_TRAN;', ,'Replenishment','Extract replenishment.');
INSERT INTO STEPS (type, short_desc, long_desc, output_file_format, extract_max_rec_per_file)
    VALUES ('Transform', 'Replenishment','Generate CSV','csv', 2500);
    
-- job_steps_xref
INSERT INTO JOB_STEPS_XREF (job_definition_id, step_id, job_step_seq)
VALUES (1,1,1);
INSERT INTO JOB_STEPS_XREF (job_definition_id, step_id, job_step_seq)
VALUES (1,2,2);
COMMIT;