#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

cd /opt/jans/jetty/jans-scim
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-scim \
    -Dlog.base=/opt/jans/jetty/jans-scim \
    -Djava.io.tmpdir=/tmp \
    -Dpython.home=/opt/jython \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar
