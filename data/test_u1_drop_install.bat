call vbatch_drop.bat
call vbatch_install.bat 
sqlplus vbatch/vbatch @ddl/vbatch.prod_trkg_tran.ddl
REM sqlplus vbatch/vbatch @sample_data/prod_trkg_tran-ohl-export-after-07-Feb-14-12.58.00.sql
sqlplus vbatch/vbatch @sample_data/test_u1_wmt_dir_putwy_plt.sql
sqlplus vbatch/vbatch @sample_data/unit_test_data.sql