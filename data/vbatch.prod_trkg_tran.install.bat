sqlplus vbatch/vbatch @ddl/vbatch.prod_trkg_tran.ddl
PAUSE
sqlplus vbatch/vbatch @sample_data/prod_trkg_tran-ohl-export-after-07-Feb-14-12.58.00.sql