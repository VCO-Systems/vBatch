sqlplus vbatch/vbatch @ddl/vbatch.prod_trkg_tran.ddl
PAUSE
sqlplus vbatch/vbatch @sample_data/prod_trkg_tran-ohl-export-after-07-Feb-14-12.58.00.sql
REM sqlplus vbatch/vbatch @sample_data/testdata-prod_trkg_tran-2013-10-01_2013-10-9.sql