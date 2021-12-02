#!/bin/sh

set -e

copy_builtin_plugins() {
    if [ ! -f /opt/jans/jetty/jans-config-api/custom/libs/scim-plugin.jar ]; then
        cp /usr/share/java/scim-plugin.jar /opt/jans/jetty/jans-config-api/custom/libs/
    fi

    if [ ! -f /opt/jans/jetty/jans-config-api/custom/libs/admin-ui-plugin.jar ]; then
        cp /usr/share/java/admin-ui-plugin.jar /opt/jans/jetty/jans-config-api/custom/libs/
    fi
}

python3 /app/scripts/wait.py

copy_builtin_plugins

if [ ! -f /deploy/touched  ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

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
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar jetty.http.port=8074
