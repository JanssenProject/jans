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

get_logging_files() {
    logs="resources/log4j2.xml"

    if [ -f /opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml ]; then
        logs="$logs,custom/config/log4j2-adminui.xml"
    fi
    echo $logs
}

python3 /app/scripts/wait.py

copy_builtin_plugins

python3 /app/scripts/bootstrap.py

# run config-api
cd /opt/jans/jetty/jans-config-api
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-config-api \
    -Dlog.base=/opt/jans/jetty/jans-config-api \
    -Djava.io.tmpdir=/tmp \
    -Dlog4j2.configurationFile=$(get_logging_files) \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8074 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
