"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/jans-client-api.yml
echo CONF=%CONF%
start /b javaw -Djava.net.preferIPv4Stack=true -cp %LIB%/jans-client-api-server.jar;%LIB%/* io.jans.ca.server.RpServerApplication server %CONF%
