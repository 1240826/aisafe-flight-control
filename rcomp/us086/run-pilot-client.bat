@echo off
REM US086 — Pilot Client App build & run script
REM Usage: run-pilot-client.bat [host] [port]
REM Default: host=localhost port=1086

set HOST=%1
if "%HOST%"=="" set HOST=localhost

set PORT=%2
if "%PORT%"=="" set PORT=1086

echo Building Pilot Client App...
mkdir out 2>nul
javac -d out src\rcomp\client\PilotClientApp.java src\rcomp\client\TcpClient.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed.
    exit /b %ERRORLEVEL%
)

echo Starting client connecting to %HOST%:%PORT% ...
java -cp out rcomp.client.PilotClientApp %HOST% %PORT%
