cd /d %~dp0
set LIB=../lib
set CONF=../conf/client-api-server.yml
echo CONF=%CONF%
java -Djava.net.preferIPv4Stack=true -cp %LIB%/jans-client-api-server.jar;%LIB%/* io.jans.ca.server.RpServerApplication server %CONF%