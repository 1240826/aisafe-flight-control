@echo off
REM ============================================
REM AISafe Flight Control System - JavaFX GUI (Quick)
REM ============================================
REM Usage: run-gui [SIM_HOST]
REM   SIM_HOST  = Simulator host IP (default: localhost)
REM   Example:  run-gui 192.168.1.100
REM ============================================
REM Prereq: project already built (mvn verify done).
REM ============================================

echo.
echo ============================================
echo  AISafe Flight Control System - JavaFX GUI
echo ============================================
echo.

REM --- Locate Maven ---
set MVN_CMD=mvn
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    if exist "C:\apache-maven-3.9.8\bin\mvn.cmd" (
        set MVN_CMD=C:\apache-maven-3.9.8\bin\mvn.cmd
    ) else (
        echo [ERROR] Maven not found.
        pause
        exit /b 1
    )
)

REM --- Simulator host (default localhost) ---
if "%1"=="" (
    set SIM_HOST=localhost
) else (
    set SIM_HOST=%1
)
SET SIM_PORT=9999
SET LOG_HOST=localhost

echo [INFO] Starting JavaFX GUI...
echo [INFO] Simulator: %SIM_HOST%:%SIM_PORT%
echo [INFO] Usage: run-gui.bat [SIM_HOST]  (e.g. run-gui.bat 192.168.1.100)
echo.

%MVN_CMD% javafx:run -pl app -DskipTests ^
    -Daisafe.simulator.host=%SIM_HOST% ^
    -Daisafe.simulator.port=%SIM_PORT% ^
    -Daisafe.logging.host=%LOG_HOST%

echo.
echo [INFO] Application closed.
pause
