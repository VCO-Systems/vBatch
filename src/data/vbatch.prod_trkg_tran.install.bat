sqlplus vbatch/vbatch @sql_scripts/vbatch.prod_trkg_tran.ddl
PAUSE
sqlplus vbatch/vbatch @sample_data/prod_trkg_tran__sample_data_1.sql