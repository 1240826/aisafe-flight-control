@echo off
REM US078 — ATCC Client App build & run script
REM Usage: run-atcc-client.bat [host] [port]
REM Default: host=localhost port=1078

set HOST=%1
if "%HOST%"=="" set HOST=localhost

set PORT=%2
if "%PORT%"=="" set PORT=1078

echo Building ATCC Client App...
mkdir out 2>nul
javac -d out src\rcomp\client\AtcClientApp.java src\rcomp\client\TcpClient.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed.
    exit /b %ERRORLEVEL%
)

echo Starting client connecting to %HOST%:%PORT% ...
java -cp out rcomp.client.AtcClientApp %HOST% %PORT%