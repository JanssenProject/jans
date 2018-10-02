"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
java -jar %LIB%/oxd-server.jar server %CONF%