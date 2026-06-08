@echo off
REM US044 — Weather Client App build & run script
set HOST=%1
if "%HOST%"=="" set HOST=localhost
set PORT=%2
if "%PORT%"=="" set PORT=1044
echo Building Weather Client App...
mkdir out 2>nul
javac -d out src\AISafeClientApp.java src\TcpClient.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed.
    exit /b %ERRORLEVEL%
)
echo Starting client connecting to %HOST%:%PORT% ...
java -cp out rcomp.client.AISafeClientApp %HOST% %PORT%
