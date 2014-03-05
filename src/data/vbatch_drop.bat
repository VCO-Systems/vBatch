call sqlplus vbatch/vbatch @sql_scripts/DELETE_vbatch_triggers.sql
if %ERRORLEVEL% == 0 goto :seq
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:seq
call sqlplus vbatch/vbatch @sql_scripts/DELETE_vbatch_sequences.sql
if %ERRORLEVEL% == 0 goto :tables
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:tables
call sqlplus vbatch/vbatch @sql_scripts/DELETE_vbatch_tables.sql
if %ERRORLEVEL% == 0 goto :endofscript
echo "Errors encountered during execution.  Exited with status: %errorlevel%"
goto :endofscript

:endofscript
echo "Script complete"
