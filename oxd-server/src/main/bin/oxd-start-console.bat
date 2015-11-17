"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-conf.json
echo CONF=%CONF%
java -Doxd.server.config=%CONF% -cp %LIB%/bcprov-jdk16-1.46.jar;%LIB%/resteasy-jaxrs-2.3.4.Final.jar;%LIB%/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher