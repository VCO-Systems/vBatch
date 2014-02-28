-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
INSERT INTO JOB_DEFINITION (short_desc,long_desc,order_num) VALUES ('Replenishment', 'Gather all replenishment data.',1);
INSERT INTO JOB_DEFINITION (short_desc,long_desc,order_num) VALUES ('Putaway', 'Gather all Putaway data',2);

-- Steps
INSERT INTO STEPS (type,  extract_commit_freq,extract_sql, short_desc,long_desc,class_path)
    VALUES ('Extract',  1000, 
    'select tran_nbr  pk1, seq_nbr pk2, CREATE_DATE_TIME as ok1,sku_id, cntr_nbr,wave_nbr,pkt_ctrl_nbr,pkt_seq_nbr,work_type,NBR_OF_CASES, nbr_Units, nbr_of_piks,nbr_scan,from_locn,to_locn from prod_trkg_tran ORDER BY ok1',
    'Replenishment','Extract replenishment.', 'com.vco.ExtractDBStep');
INSERT INTO STEPS (type, short_desc, long_desc, output_file_format, extract_max_rec_per_file, class_path, output_filename_prefix)
    VALUES ('Transform', 'Replenishment','Generate CSV','csv', 2500, 'com.vco.GenerateCSVStep','REPL_b{batch_num}_{dt}_{seq}.csv');
    
-- job_steps_xref
INSERT INTO JOB_STEPS_XREF (job_definition_id, step_id, job_step_seq)
VALUES (1,1,1);
INSERT INTO JOB_STEPS_XREF (job_definition_id, step_id, job_step_seq)
VALUES (1,2,2);
COMMIT;