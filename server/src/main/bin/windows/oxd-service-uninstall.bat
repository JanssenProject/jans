@echo off

oxdServer //DS//oxd-server-4.2

@RD /S /Q "%CD%\..\log"
@RD /S /Q "%CD%\..\data"