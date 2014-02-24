-- Since SQL Power architect doesn't support triggers,
-- after re-creating db from SPQ schema, apply this sql
-- to add triggers for the auto-incrementing ids.

-- job_master.id 
create trigger job_master_id
before insert on job_master
for each row
begin
select JOB_MASTER_ID_SEQ.nextval into :new.id from dual;
end;

-- vbatch_log.id
create trigger VBATCH_LOG_ID_SEQ
before insert on vbatch_log
for each row
begin
select VBATCH_LOG_ID_SEQ.nextval into :new.id from dual;
end;


-- steps.id
create trigger STEPS_ID_SEQ
before insert on steps
for each row
begin
select STEPS_ID_SEQ.nextval into :new.id from dual;
end;

-- job_steps_xref.id
create trigger JOB_STEPS_XREF_ID_SEQ
before insert on job_steps_xref
for each row
begin
select JOB_STEPS_XREF_ID_SEQ.nextval into :new.id from dual;
end;
