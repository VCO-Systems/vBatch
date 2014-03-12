set -e
sqlplus $1/$2 @ddl/vbatch.ddl
sqlplus $1/$2 @sql_scripts/vbatch_triggers.sql
sqlplus $1/$2 @sample_data/config_sample.sql