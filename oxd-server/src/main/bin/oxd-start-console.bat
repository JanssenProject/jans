"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-conf.json
echo CONF=%CONF%
java -Doxd.server.config=%CONF% -cp %LIB%/bcprov-jdk15on-1.54.jar;%LIB%/resteasy-jaxrs-2.3.7.Final.jar;%LIB%/oxd-local-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher