set -e
sqlplus $1/$2 @sql_scripts/DELETE_vbatch_triggers.sql