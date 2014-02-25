sqlplus vbatch/vbatch @create_vbatch_tables.sql
PAUSE
sqlplus vbatch/vbatch @vbatch_add_triggers.sql
