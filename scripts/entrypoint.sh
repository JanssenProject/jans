#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched  ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

# quarkus.log.file.enable=false
# quarkus.log.file.path=logs/config_api.log

exec java \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dquarkus.http.insecure-requests=enabled \
    -Dquarkus.http.port=8074 \
    -Dquarkus.http.ssl-port=9444 \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/config-api/jans-config-api-runner.jar
