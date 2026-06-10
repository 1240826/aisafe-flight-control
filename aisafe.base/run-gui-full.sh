#!/usr/bin/env bash
# ============================================
# AISafe Flight Control System - JavaFX GUI (Full)
# ============================================
# 1. Build project (quickbuild)
# 2. Run bootstrap (demo data)
# 3. Start JavaFX GUI with SCOMP simulator config
# ============================================

set -e

echo ""
echo "============================================"
echo "  AISafe Flight Control System - JavaFX GUI"
echo "============================================"
echo ""

# Locate Maven
MVN_CMD=$(command -v mvn 2>/dev/null) || true
if [ -z "$MVN_CMD" ]; then
    if [ -x "/opt/apache-maven-3.9.8/bin/mvn" ]; then
        MVN_CMD="/opt/apache-maven-3.9.8/bin/mvn"
    elif [ -x "/usr/local/apache-maven-3.9.8/bin/mvn" ]; then
        MVN_CMD="/usr/local/apache-maven-3.9.8/bin/mvn"
    else
        echo "[ERROR] Maven not found."
        echo "Install Apache Maven 3.9+ and add 'bin' to PATH."
        exit 1
    fi
fi

SIM_HOST="${1:-localhost}"
SIM_PORT="${2:-9999}"
LOG_HOST="${3:-localhost}"

# --- Step 1: Build ---
echo "[INFO] Building project..."
"$MVN_CMD" -B verify dependency:copy-dependencies -D maven.javadoc.skip=true
echo "[OK] Build complete."
echo ""

# --- Step 2: Bootstrap ---
echo "[INFO] Running bootstrap (demo data)..."
CP="app/target/aisafe.app-1.4.0-SNAPSHOT.jar:app/target/dependency/*"
java -cp "$CP" eapli.aisafe.bootstrap.AISafeBootstrapApp -bootstrap:demo
echo "[OK] Bootstrap done."
echo ""

# --- Step 3: Run JavaFX GUI ---
echo "[INFO] Starting JavaFX GUI..."
echo "[INFO] Simulator: ${SIM_HOST}:${SIM_PORT}"
echo ""

"$MVN_CMD" javafx:run -pl app -DskipTests \
    -Daisafe.simulator.host="${SIM_HOST}" \
    -Daisafe.simulator.port="${SIM_PORT}" \
    -Daisafe.logging.host="${LOG_HOST}"

echo ""
echo "[INFO] Application closed."
