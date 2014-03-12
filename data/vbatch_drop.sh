set -e
sqlplus $1/$2 @sql_scripts/DELETE_vbatch_triggers.sql
sqlplus $1/$2 @sql_scripts/DELETE_vbatch_sequences.sql
sqlplus $1/$2 @sql_scripts/DELETE_vbatch_tables.sql