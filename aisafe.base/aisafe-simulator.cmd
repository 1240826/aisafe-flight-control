@echo off
REM Bridge: ProcessBuilderSimulationRunner -> simulator-bridge.ps1
REM %1 = input JSON path, %2 = output report path, %3 = weather JSON path (optional)

set BRIDGE_SCRIPT=%~dp0scripts\simulator-bridge.ps1

if "%3"=="" (
    powershell.exe -ExecutionPolicy Bypass -File "%BRIDGE_SCRIPT%" "%~1" "%~2" ""
) else (
    powershell.exe -ExecutionPolicy Bypass -File "%BRIDGE_SCRIPT%" "%~1" "%~2" "%~3"
)

exit /b %ERRORLEVEL%
