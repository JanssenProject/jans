#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched ]; then
    python3 /app/scripts/entrypoint.py
    touch /deploy/touched
fi

cd /opt/gluu/jetty/scim
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$JANS_MAX_RAM_PERCENTAGE \
    -Dgluu.base=/etc/gluu \
    -Dserver.base=/opt/gluu/jetty/scim \
    -Dlog.base=/opt/gluu/jetty/scim \
    -Djava.io.tmpdir=/tmp \
    -Dpython.home=/opt/jython \
    ${JANS_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar
