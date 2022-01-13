@echo off 
rem This batch file generate windows executable file (jans-client-api.exe) for installation of jans-client-api on windows OS.
echo ============================
echo Execution Started..
echo ============================

rem Checking JRE_HOME and INNOSETUP_HOME environment variables on system
if "%JRE_HOME%"=="" (call :EnvNotSetError "JRE_HOME") else (
if "%INNOSETUP_HOME%"=="" (call :EnvNotSetError "INNOSETUP_HOME") else (

rem Section 1: Generate windows executable file (jans-client-api.exe) using INNOSETUP
"%INNOSETUP_HOME%/ISCC.exe" /O../ /Fjans-client-api ../conf/generate-exe-using-bat.iss
)
)
goto :eof



:EnvNotSetError
 rem Display error message if environment variables not set.
 echo "%~1 is NOT defined in environment variables. The program will terminate."
 echo ==================================================================================
 goto :eof


:eof
echo ============================
echo Execution Ended..
echo ============================


