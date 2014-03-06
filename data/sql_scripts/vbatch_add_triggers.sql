-- Since SQL Power architect doesn't generate triggers,
-- after re-creating db from SPQ schema, apply this sql
-- to add triggers for the auto-incrementing ids.

-- job_definition.id 
create trigger job_definition_id_trg
before insert on job_definition
for each row
when (new.id IS NULL)
begin
select JOB_DEFINITION_ID_SEQ.nextval into :new.id from dual;
end;
/
-- steps.id
create trigger steps_id_trg
before insert on steps
for each row
when (new.id IS NULL)
begin
select STEPS_ID_SEQ.nextval into :new.id from dual;
end;
/
-- job_steps_xref.id
create trigger job_steps_xref_id_trg
before insert on job_steps_xref
for each row
when (new.id IS NULL)
begin
select JOB_STEPS_XREF_ID_SEQ.nextval into :new.id from dual;
end;
/
-- batch_log.id
create trigger batch_log_id_trg
before insert on batch_log
for each row
when (new.id IS NULL)
begin
select BATCH_LOG_ID_SEQ.nextval into :new.id from dual;
end;
/
-- batch_log_dtl.id
create trigger batch_log_dtl_id_trg
before insert on batch_log_dtl
for each row
when (new.id IS NULL)
begin
select BATCH_LOG_DTL_ID_SEQ.nextval into :new.id from dual;
end;
/
-- batch_log_file_output.id
create trigger batch_log_file_output_id_trg
before insert on batch_log_file_output
for each row
when (new.id IS NULL)
begin
select BATCH_LOG_FILE_OUTPUT_ID_SEQ.nextval into :new.id from dual;
end;
/
-- batch_log_ok_dtl.id
create trigger batch_log_ok_dtl_id_trg
before insert on batch_log_ok_dtl
for each row
when (new.id IS NULL)
begin
select BATCH_LOG_OK_DTL_ID_SEQ.nextval into :new.id from dual;
end;
/
EXIT;