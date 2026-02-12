#!/bin/bash

set -e

# ==============
# Janssen Shibboleth IDP Entrypoint
# ==============

python3 /app/scripts/wait.py

if [ ! -f /app/.initialized ]; then
    python3 /app/scripts/bootstrap.py
    touch /app/.initialized
fi

python3 /app/scripts/healthcheck.py &

# Use JETTY_BASE from environment (matches Dockerfile and entrypoint.py)
JETTY_BASE="${JETTY_BASE:-/opt/shibboleth-idp/jetty}"

cd "${JETTY_BASE}"

exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage="${CN_MAX_RAM_PERCENTAGE:-75}" \
    -Djava.security.egd=file:/dev/urandom \
    -Djetty.home="${JETTY_HOME:-/opt/jetty}" \
    -Djetty.base="${JETTY_BASE}" \
    -Didp.home="${SHIBBOLETH_HOME:-/opt/shibboleth-idp}" \
    -Dlogback.configurationFile="${SHIBBOLETH_HOME:-/opt/shibboleth-idp}/conf/logback.xml" \
    -jar "${JETTY_HOME:-/opt/jetty}/start.jar"
