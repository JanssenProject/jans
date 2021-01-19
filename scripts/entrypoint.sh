#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched  ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

cd /opt/jans/jans-config-api
exec java \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dquarkus.profile=prod \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jans/jans-config-api/jans-config-api-runner.jar
