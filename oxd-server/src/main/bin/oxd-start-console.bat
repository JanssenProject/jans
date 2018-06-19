"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-conf.json
echo CONF=%CONF%
java -jar %LIB%/oxd-server.jar org.xdi.oxd.server.OxdServerApplication %CONF%