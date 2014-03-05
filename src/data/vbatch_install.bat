call sqlplus vbatch/vbatch @sql_scripts/vbatch_all.ddl
if %ERRORLEVEL% == 0 goto :triggers
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:triggers
call sqlplus vbatch/vbatch @sql_scripts/vbatch_triggers.sql
if %ERRORLEVEL% == 0 goto :jobs
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:jobs
call sqlplus vbatch/vbatch @sample_data/config_sample.sql
if %ERRORLEVEL% == 0 goto :prod_trkg
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:prod_trkg
if "%1" == "prod" ( call sqlplus vbatch/vbatch @sample_data/vbatch.prod_trkg_tran__create_and_insert.sql )
if %ERRORLEVEL% == 0 goto :endofscript
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:endofscript
echo "Script complete"
