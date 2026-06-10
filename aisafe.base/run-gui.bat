@echo off
REM ============================================
REM AISafe Flight Control System - JavaFX GUI (Quick)
REM ============================================
REM Assumes project already built (mvn verify done).
REM Skips build and bootstrap.
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

SET SIM_HOST=localhost
SET SIM_PORT=9999
SET LOG_HOST=localhost

echo [INFO] Starting JavaFX GUI...
echo [INFO] Simulator: %SIM_HOST%:%SIM_PORT%
echo.

%MVN_CMD% javafx:run -pl app -DskipTests ^
    -Daisafe.simulator.host=%SIM_HOST% ^
    -Daisafe.simulator.port=%SIM_PORT% ^
    -Daisafe.logging.host=%LOG_HOST%

echo.
echo [INFO] Application closed.
pause
