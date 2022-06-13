#!/bin/sh

set -e

python3 /app/scripts/wait.py
python3 /app/scripts/bootstrap.py

# run jans-client-api
cd /opt/jans/jetty/jans-client-api

# shellcheck disable=SC2086
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djava.net.preferIPv4Stack=true \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-client-api \
    -Dlog.base=/opt/jans/jetty/jans-client-api \
    -Djava.io.tmpdir=/tmp \
    -Dpython.home=/opt/jython \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
