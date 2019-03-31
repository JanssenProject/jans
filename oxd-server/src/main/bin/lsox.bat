cd /d %~dp0
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
start /b java -cp %LIB%/oxd-server.jar org.gluu.oxd.server.Cli -c %CONF% %*
