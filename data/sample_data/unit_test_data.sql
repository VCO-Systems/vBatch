-- Data for unit tests

-- job_definition
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1000,1000,'u1','Unit Test 1');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1001,1001,'u2','Unit Test 2');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1002,1002,'u3','Unit Test 3');

-- Steps

Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1000,'Extract','u1','Extract test data','com.vco.ExtractDBStep',null,null,null,40,null,1000,'select  ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.create_date_time, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt WHERE 1=1 /* where */  order by ptt.create_date_time',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1001,'CSV','u1','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,40,null,null,'csv','u1_b{batch_num}_{dt}_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1002,'TRG','u1','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (1003,'Extract','PUTAWAY_CS_T','Putaway Case - Testing','com.vco.ExtractDBStep',250,null,1000,'',null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (1004,'CSV','PUTAWAY_CS_T','Putaway Case - Testing','com.vco.GenerateCSVStep',null,100,null,null,'csv','PTWY_CS_T_b{batch_num}_{dt}_{seq}.csv',null,null,null,null);


-- job_steps_xref
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1000,1000,1000,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1001,1000,1001,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1002,1000,1002,3,null);

COMMIT;
EXIT;