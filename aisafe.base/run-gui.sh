#!/usr/bin/env bash
# ============================================
# AISafe Flight Control System - JavaFX GUI (Quick)
# ============================================
# Assumes project already built. Skips build & bootstrap.
# Run run-gui-full.sh for the full pipeline.
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

echo "[INFO] Starting JavaFX GUI..."
echo "[INFO] Simulator: ${SIM_HOST}:${SIM_PORT}"
echo ""

"$MVN_CMD" javafx:run -pl app -DskipTests \
    -Daisafe.simulator.host="${SIM_HOST}" \
    -Daisafe.simulator.port="${SIM_PORT}" \
    -Daisafe.logging.host="${LOG_HOST}"

echo ""
echo "[INFO] Application closed."
