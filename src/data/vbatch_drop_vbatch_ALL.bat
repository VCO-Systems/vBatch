sqlplus vbatch/vbatch @DELETE_ALL_vbatch_triggers.sql
PAUSE
sqlplus vbatch/vbatch @DELETE_ALL_vbatch_sequences.sql
PAUSE
sqlplus vbatch/vbatch @DELETE_ALL_vbatch_tables.sql

