CREATE TABLE "VBATCH"."PROD_TRKG_TRAN" 
   (	"TRAN_TYPE" VARCHAR2(3 CHAR), 
	"TRAN_CODE" VARCHAR2(3 CHAR), 
	"TRAN_NBR" NUMBER(9,0) DEFAULT 0, 
	"SEQ_NBR" NUMBER(5,0) DEFAULT 0, 
	"WHSE" VARCHAR2(3 CHAR), 
	"SKU_ID" VARCHAR2(10 CHAR), 
	"CNTR_NBR" VARCHAR2(20 CHAR), 
	"WAVE_NBR" VARCHAR2(12 CHAR), 
	"PKT_CTRL_NBR" VARCHAR2(10 CHAR), 
	"PKT_SEQ_NBR" NUMBER(9,0) DEFAULT 0, 
	"WORK_TYPE" VARCHAR2(3 CHAR), 
	"NBR_OF_CASES" NUMBER(7,0) DEFAULT 0, 
	"NBR_UNITS" NUMBER(9,2) DEFAULT 0, 
	"NBR_OF_PIKS" NUMBER(7,0) DEFAULT 0, 
	"NBR_SCAN" NUMBER(7,0) DEFAULT 0, 
	"FROM_LOCN" VARCHAR2(10 CHAR), 
	"TO_LOCN" VARCHAR2(10 CHAR), 
	"WKSTN_ID" VARCHAR2(10 CHAR), 
	"RSN_CODE" VARCHAR2(2 CHAR), 
	"REF_CODE_ID_1" VARCHAR2(3 CHAR), 
	"REF_FIELD_1" VARCHAR2(20 CHAR), 
	"REF_CODE_ID_2" VARCHAR2(3 CHAR), 
	"REF_FIELD_2" VARCHAR2(20 CHAR), 
	"REF_CODE_ID_3" VARCHAR2(3 CHAR), 
	"REF_FIELD_3" VARCHAR2(20 CHAR), 
	"REF_CODE_ID_4" VARCHAR2(3 CHAR), 
	"REF_FIELD_4" VARCHAR2(20 CHAR), 
	"REF_CODE_ID_5" VARCHAR2(3 CHAR), 
	"REF_FIELD_5" VARCHAR2(20 CHAR), 
	"OLD_STAT_CODE" NUMBER(2,0) DEFAULT 0, 
	"NEW_STAT_CODE" NUMBER(2,0) DEFAULT 0, 
	"SAM" NUMBER(9,4) DEFAULT 0, 
	"BEGIN_DATE" DATE, 
	"END_DATE" DATE, 
	"STAT_CODE" NUMBER(2,0) DEFAULT 0, 
	"MODULE_NAME" VARCHAR2(10 CHAR), 
	"MENU_OPTN_NAME" VARCHAR2(40 CHAR), 
	"CREATE_DATE_TIME" DATE, 
	"MOD_DATE_TIME" DATE, 
	"USER_ID" VARCHAR2(15 CHAR), 
	"PLT_ID" VARCHAR2(20 CHAR), 
	"TASK_ID" NUMBER(9,0), 
	"CD_MASTER_ID" NUMBER(9,0)
   ) PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT);
 
  