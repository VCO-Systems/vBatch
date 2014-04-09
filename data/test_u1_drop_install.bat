call vbatch_drop.bat
call vbatch_install.bat 
pause
sqlplus vbatch/vbatch @ddl/vbatch.prod_trkg_tran.ddl
pause
REM sqlplus vbatch/vbatch @sample_data/prod_trkg_tran-ohl-export-after-07-Feb-14-12.58.00.sql
REM sqlplus vbatch/vbatch @sample_data/test_u1_wmt_dir_putwy_plt.sql
sqlplus vbatch/vbatch @sample_data/test_u2_simulated_vas_gm.sql
sqlplus vbatch/vbatch @sample_data/unit_test_data.sql