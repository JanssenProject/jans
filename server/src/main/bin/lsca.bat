cd /d %~dp0
set LIB=../lib
set CONF=../conf/jans-client-api.yml
echo CONF=%CONF%
start /b java -cp %LIB%/jans-client-api-server.jar;%LIB%/* Cli -c %CONF% %*
