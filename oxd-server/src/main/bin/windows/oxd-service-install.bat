@echo off

oxdServer //IS//oxd-server-4.2 --DisplayName="OXD Service" --Install="%CD%\..\bin\oxdServer.exe" --Jvm="%CD%\..\jre\bin\server\jvm.dll" --StartMode=jvm --StopMode=jvm --Startup=auto --StartClass=org.gluu.oxd.server.OxdServerApplication --StartParams=server#"%CD%\..\conf\oxd-server.yml" --StartMethod=main --StopClass=org.gluu.oxd.server.OxdServerApplication --StopParams=stop --StopMethod=main --Classpath="%CD%\..\lib\oxd-server.jar";"%CD%\..\lib\bcprov-jdk15on-1.55.jar" --LogLevel=DEBUG --LogPath="%CD%\..\log" --LogPrefix=oxd-installer --StdOutput="%CD%\..\log\stdout.log" --StdError="%CD%\..\log\stderr.log"

oxdServer //ES//oxd-server-4.2