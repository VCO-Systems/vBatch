-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1,1,'Replenishment (Test)','replenishment transactions');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (2,2,'Putaway (Test)','Putaway transactions');

-- Steps
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1,'Extract','Replenishment','Extract replenishment.','com.vco.ExtractDBStep',null,null,null,15,null,1000,'select  ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_nbr ,ptt.seq_nbr, ptt.create_date_time ,ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt   /* where */  order by ptt.create_date_time',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (2,'Transform','Replenishment','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,500,null,null,'csv','repl_{batch_num}_{dt}_W001_{seq}.csv',null);
    
-- job_steps_xref
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1,1,1,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (2,1,2,2,null);

COMMIT;
EXIT;