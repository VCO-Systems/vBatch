-- with empty vbatch database, insert all the job_master, steps, and job_steps_xref entries

-- job_master
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (1,1,'PUTAWAY_CS','Putaway Case');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (2,2,'PUTAWAY_PLT','Putaway PLT');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (100,100,'Replenishment (Test)','replenishment transactions');
Insert into VBATCH.JOB_DEFINITION (ID,ORDER_NUM,SHORT_DESC,LONG_DESC) values (101,101,'Putaway (Test)','Putaway transactions');


-- Steps
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (1,'Extract','Replenishment','Extract replenishment.','com.vco.ExtractDBStep',null,null,null,500,null,1000,'select  ptt.tran_nbr PK1,ptt.seq_nbr PK2, ptt.create_date_time ok1,  ptt.tran_type, ptt.tran_nbr, ptt.seq_nbr, ptt.create_date_time, ptt.whse, ptt.sku_id, ptt.cntr_nbr, ptt.wave_nbr, ptt.pkt_ctrl_nbr, ptt.pkt_seq_nbr from prod_trkg_tran ptt   /* where */  order by ptt.create_date_time',null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (2,'CSV','Replenishment','Generate CSV','com.vco.GenerateCSVStep',null,null,null,null,250,null,null,'csv','REPL_b{batch_num}_{dt}_W001_{seq}.csv',null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,PARAM1,PARAM2,PARAM3,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX) values (3,'TRG','Replenishment','Generate TRG','com.vco.GenerateTRGStep',null,null,null,null,null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (4,'Extract','PUTAWAY_CS','Putaway Case','com.vco.ExtractDBStep',250,null,1000,'select ptt.create_date_time as OK1, ptt.tran_nbr as PK1, ptt.seq_nbr as PK2, ''CWMS0104'', ''A'',ptt.seq_nbr, '''', ptt.tran_nbr, to_char(ptt.create_date_time, ''yyyymmdd'') plan_date, '''',ptt.plt_id, ''17532'', '''', '''', '''', ''O'', ''PUTAWAY CASE'', '''' , ''4443'', ptt.from_locn, ''1'', '''', '''', '''', '''', '''', '''','''', '''', '''', '''','''', '''', '''', '''','''', '''', '''', '''', '''', '''', ptt.user_id, to_char(ptt.begin_date, ''yyyymmddhh24miss'') sign_on_time, '''', '''', '''', '''', ''''  from prod_trkg_tran ptt, item_master im, locn_hdr lh  where ptt.create_date_time > to_date(''20140101'', ''YYYYMMDD'') and ptt.sku_id = im.sku_id  and ptt.from_locn = lh.locn_id  and ptt.menu_optn_name = ''WMT Dir Ptwy Cs'' AND ptt.create_date_time > to_date(''20140101'', ''YYYYMMDD'') /* where */ order by ptt.create_date_time asc;',null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (5,'CSV','PUTAWAY_CS','Putaway Case','com.vco.GenerateCSVStep',null,100,null,null,'csv','PTWY_CS_b{batch_num}_{dt}_W001_{seq}.csv',null,null,null,null);

Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (6,'Extract','PUTAWAY_PLT','Putaway PLT','com.vco.ExtractDBStep',250,null,1000,'',null,null,null,null,null,null);
Insert into VBATCH.STEPS (ID,TYPE,SHORT_DESC,LONG_DESC,CLASS_PATH,EXTRACT_MAX_REC,EXTRACT_MAX_REC_PER_FILE,EXTRACT_COMMIT_FREQ,EXTRACT_SQL,OUTPUT_FILE_FORMAT,OUTPUT_FILENAME_PREFIX,OUTPUT_FILENAME_POSTFIX,PARAM1,PARAM2,PARAM3) values (7,'CSV','PUTAWAY_PLT','Putaway PLT','com.vco.GenerateCSVStep',null,100,null,null,'csv','PTWY_PLT_b{batch_num}_{dt}_W001_{seq}.csv',null,null,null,null);

-- job_steps_xref
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (1,1,4,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (2,1,5,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (3,2,6,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (4,2,7,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (100,100,1,1,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (101,100,2,2,null);
Insert into VBATCH.JOB_STEPS_XREF (ID,JOB_DEFINITION_ID,STEP_ID,JOB_STEP_SEQ,SPECIAL_MODE) values (102,100,3,3,null);

COMMIT;
EXIT;