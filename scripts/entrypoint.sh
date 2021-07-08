#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched  ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

python3 /app/scripts/mod_context.py

# run config-api
mkdir -p /opt/jetty/temp
cd /opt/jans/jetty/jans-config-api
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-config-api \
    -Dlog.base=/opt/jans/jetty/jans-config-api \
    -Djava.io.tmpdir=/opt/jetty/temp \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar jetty.http.port=8074
