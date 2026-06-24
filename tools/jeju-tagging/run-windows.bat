@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" (
  set "INPUT=%USERPROFILE%\Downloads\SSAFY_HOME_TRIP_202604\SSAFY_TRIP_Dump.sql"
) else (
  set "INPUT=%~1"
)
py -3 jeju_tagging.py run --input "%INPUT%"
if errorlevel 1 pause

