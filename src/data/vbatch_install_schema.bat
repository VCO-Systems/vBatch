sqlplus vbatch/vbatch @create_vbatch_tables.sql
PAUSE
sqlplus vbatch/vbatch @create_vbatch_triggers.sql
PAUSE
sqlplus vbatch/vbatch @insert_job_definitions.sql
