"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
start /b java -cp %LIB%/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.Cli -c %CONF% %*
