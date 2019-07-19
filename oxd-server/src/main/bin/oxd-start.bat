"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
start /b javaw -Djava.net.preferIPv4Stack=true -cp %LIB%/bcprov-jdk15on-1.54.jar;%LIB%/oxd-server.jar org.gluu.oxd.server.OxdServerApplication server %CONF%
