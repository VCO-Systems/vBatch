-- drop previous unit test data

-- delete steps
delete from job_steps_xref where job_definition_id>=1000;
delete from steps where id in ( select step_id from job_steps_xref where job_definition_id >=1000);

(select order_num from job_definition where order_num>=1000);

-- Data for unit tests

-- job_definition
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1000,1000,'t1','Simple Job');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1001,1001,'t2','Simple Job 2');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1002,1002,'t3','OK1 Overlap');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1003,1003,'t4','Union Duplication');

-- Steps


Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1000,'Extract','t1','Simple Job 1','com.vco.ExtractDBStep',null,null,null,10,null,100,'select ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.create_date_time, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt WHERE 1=1 /* where */  order by ptt.create_date_time,ptt.tran_nbr,ptt.seq_nbr;',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1001,'CSV','t1','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,10,null,null,'csv','t1_b{batch_num}_{dt}_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1002,'TRG','t1','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1003,'Extract','t2','Simple Job 2','com.vco.ExtractDBStep',null,null,null,10,null,100,'select ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.create_date_time, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt WHERE 1=1 /* where */  order by ptt.create_date_time,ptt.tran_nbr,ptt.seq_nbr;',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1004,'CSV','t2','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,10,null,null,'csv','t2_b{batch_num}_{dt}_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1005,'TRG','t2','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1006,'Extract','t3','OK1 Overlap','com.vco.ExtractDBStep',null,null,null,10,null,100,'select ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.create_date_time, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt WHERE 1=1 /* where */  order by ptt.create_date_time,ptt.tran_nbr,ptt.seq_nbr;',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1007'CSV','t3','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,10,null,null,'csv','t3_b{batch_num}_{dt}_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1008,'TRG','t3','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1009,'Extract','t4','OK1 Overlap','com.vco.ExtractDBStep',null,null,null,20,null,100,'select ptt.create_date_time OK1, ptt.tran_nbr PK1, ptt.* from (select * from prod_trkg_tran ptt where create_date_time=to_date('02-07-2014 13:01:23','mm/dd/yyyy hh24:mi:ss')  AND ptt.create_date_time >= to_date('02/07/2014 13:01:23', 'mm/dd/yyyy hh24:mi:ss')   UNION all select * from prod_trkg_tran ptt where create_date_time=to_date(''02-07-2014 13:01:23'',''mm/dd/yyyy hh24:mi:ss'')  AND ptt.create_date_time >= to_date(''02/07/2014 13:01:23'', ''mm/dd/yyyy hh24:mi:ss''))    ptt order by create_date_time,tran_nbr,seq_nbr',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1010,'CSV','t4','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,10,null,null,'csv','t4_b{batch_num}_{dt}_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1011,'TRG','t4','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);

-- job_steps_xref
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1000,1000,1000,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1001,1000,1001,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1002,1000,1002,3,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1003,1001,1003,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1004,1001,1004,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1005,1001,1005,3,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1006,1002,1006,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1007,1002,1007,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1008,1002,1008,3,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1009,1003,1009,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1010,1003,1010,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1011,1003,1011,3,null);

COMMIT;
EXIT;