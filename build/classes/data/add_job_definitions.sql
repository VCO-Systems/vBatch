-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1,1,'Replenishment','Gather all replenishment data.');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (2,2,'Putaway','Gather all Putaway data');

-- Steps
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1,'Extract','Replenishment','Extract replenishment.','com.vco.ExtractDBStep',null,null,null,1000,null,1000,'select p.tran_type, p.tran_code, p.tran_nbr, p.seq_nbr, p.whse, p.sku_id, p.cntr_nbr, p.wave_nbr, p.pkt_ctrl_nbr, p.pkt_seq_nbr, p.create_date_time ok1 from prod_trkg_tran p',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (2,'Transform','Replenishment','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,500,null,null,'csv','repl_{dt}_W001_{seq}.csv',null);
    
-- job_steps_xref
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1,1,1,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (2,1,2,2,null);
/
COMMIT;