cd /d %~dp0
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
java -cp %LIB%/bcprov-jdk15on-1.54.jar;%LIB%/oxd-server.jar org.xdi.oxd.server.OxdServerApplication server %CONF%