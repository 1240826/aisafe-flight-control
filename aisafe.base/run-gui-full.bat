@echo off
REM ============================================
REM AISafe Flight Control System - JavaFX GUI (Full)
REM ============================================
REM Usage: run-gui-full [SIM_HOST]
REM   SIM_HOST  = Simulator host IP (default: localhost)
REM   Example:  run-gui-full 192.168.1.100
REM ============================================
REM 1. Build project (quickbuild)
REM 2. Run bootstrap (demo data)
REM 3. Start JavaFX GUI with SCOMP simulator config
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
        echo [ERROR] Maven not found in PATH or standard locations.
        echo Install Apache Maven 3.9+ and add 'bin' to PATH, or install to C:\apache-maven-3.9.8\
        echo Download: https://maven.apache.org/download.cgi
        pause
        exit /b 1
    )
)

echo [OK] Maven found.
echo.

REM --- Step 1: Build ---
echo [INFO] Building project (quickbuild)...
call %MVN_CMD% -B install dependency:copy-dependencies -D maven.javadoc.skip=true
if %errorlevel% neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)
echo [OK] Build complete.
echo.

REM --- Step 2: Bootstrap ---
echo [INFO] Running bootstrap (demo data)...
echo.
SET BASE_CP=app\target\aisafe.app-1.4.0-SNAPSHOT.jar;app\target\dependency\*
java -cp "%BASE_CP%" eapli.aisafe.bootstrap.AISafeBootstrapApp -bootstrap:demo
echo [OK] Bootstrap done.
echo.

REM --- Step 3: Start JavaFX GUI ---
echo [INFO] Starting JavaFX GUI with SCOMP simulator config...
echo.

if "%1"=="" (
    set SIM_HOST=localhost
) else (
    set SIM_HOST=%1
)
SET SIM_PORT=9999
SET LOG_HOST=localhost

echo [INFO] Simulator: %SIM_HOST%:%SIM_PORT%
echo [INFO] Usage: run-gui-full.bat [SIM_HOST]  (e.g. run-gui-full.bat 192.168.1.100)
echo.

%MVN_CMD% javafx:run -pl app -DskipTests ^
    -Daisafe.simulator.host=%SIM_HOST% ^
    -Daisafe.simulator.port=%SIM_PORT% ^
    -Daisafe.logging.host=%LOG_HOST%

echo.
echo [INFO] Application closed.
pause
