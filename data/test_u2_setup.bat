REM sqlplus vbatch/vbatch @sample_data/prod_trkg_tran-ohl-export-after-07-Feb-14-12.58.00.sql
REM sqlplus vbatch/vbatch @sample_data/test_u1_wmt_dir_putwy_plt.sql

REM delete existing PROD_TRKG_TRAN rows
@echo delete from prod_trkg_tran; | sqlplus vbatch/vbatch@xe
REM insert test data
sqlplus vbatch/vbatch @../test/data/test_u2_data.sql
REM sqlplus vbatch/vbatch @sample_data/unit_test_data.sql