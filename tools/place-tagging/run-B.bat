@echo off
chcp 65001 >nul
set PYTHONUTF8=1
setlocal
cd /d "%~dp0"
title Soomgil 태깅 파트 B
if not "%~1"=="" set "TRIP_DUMP=%~1"
where py >nul 2>nul
if %errorlevel%==0 (
  py -3 place_tagging.py run B
) else (
  python place_tagging.py run B
)
echo.
echo 창을 닫아도 됩니다. 중간에 끊겼으면 이 파일을 다시 실행하세요.
pause
