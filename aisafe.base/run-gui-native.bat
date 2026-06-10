@echo off
REM ============================================
REM AISafe Flight Control System - JavaFX GUI (Native Java)
REM ============================================
REM Runs directly with java -cp (no Maven at runtime)
REM Requires: Project already built with 'quickbuild.bat'
REM ============================================

echo.
echo ============================================
echo  AISafe Flight Control System - JavaFX GUI
echo ============================================
echo.

REM Check if built
if not exist "app\target\aisafe.app-1.4.0-SNAPSHOT.jar" (
    echo [ERROR] Project not built. Run quickbuild.bat first.
    pause
    exit /b 1
)

REM TCP simulator settings
SET SIM_HOST=localhost
SET SIM_PORT=9999
SET LOG_HOST=localhost

REM JavaFX module path - uses dependencies copied by maven
SET FX_LIBS=app\target\dependency

REM Build classpath: app jar + all dependencies
SET CP=app\target\aisafe.app-1.4.0-SNAPSHOT.jar;%FX_LIBS%\*

echo [INFO] Starting JavaFX GUI...
echo [INFO] Simulator: %SIM_HOST%:%SIM_PORT%
echo [INFO] Logging: %LOG_HOST%
echo.

REM Run with JavaFX modules on module path
java ^
    --module-path "%FX_LIBS%" ^
    --add-modules javafx.controls,javafx.fxml ^
    --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED ^
    --add-exports javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
    --add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.util=ALL-UNNAMED ^
    -Daisafe.simulator.host=%SIM_HOST% ^
    -Daisafe.simulator.port=%SIM_PORT% ^
    -Daisafe.logging.host=%LOG_HOST% ^
    -cp "%CP%" ^
    eapli.aisafe.ui.jfx.AISafeFX

echo.
echo [INFO] Application closed.
pause