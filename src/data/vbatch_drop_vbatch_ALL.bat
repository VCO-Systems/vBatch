sqlplus vbatch/vbatch @DELETE_vbatch_triggers.sql
PAUSE
sqlplus vbatch/vbatch @DELETE_vbatch_sequences.sql
PAUSE
sqlplus vbatch/vbatch @DELETE_vbatch_tables.sql

