-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
INSERT INTO JOB_MASTER (name,description,order_num) VALUES ('Replenishment', 'Gather all replenishment data.',1);
INSERT INTO JOB_MASTER (name,description,order_num) VALUES ('Putaway', 'Gather all Putaway data',2);

-- Steps
INSERT INTO STEPS (type, extract_max_rec_per_file, extract_commit_freq,extract_sql, description)
    VALUES ('Extract', 1000, 1000, 'SELECT * FROM PROD_TRKG_TRAN;', 'Extract replenishment.');
INSERT INTO STEPS (type, description, output_file_format)
    VALUES ('Transform', 'Generate CSV','csv');
    
-- job_steps_xref
INSERT INTO JOB_STEPS_XREF (job_id_id, step_id, job_step_seq, description)
VALUES (1,1,1, 'Replenishment extract.');
INSERT INTO JOB_STEPS_XREF (job_id_id, step_id, job_step_seq, description)
VALUES (1,2,2, 'Replenishment extract.');
