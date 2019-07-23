"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-conf.json
echo CONF=%CONF%
start /b java -Doxd.server.config=%CONF% -cp %LIB%/bcprov-jdk15on-1.54.jar;%LIB%/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.Cli %*
