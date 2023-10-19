#!/usr/bin/env sh

set -e

get_logging_files() {
    logs="resources/log4j2.xml"

    if [ -f /opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml ]; then
        logs="$logs,custom/config/log4j2-adminui.xml"
    fi
    echo $logs
}

get_prometheus_opt() {
    prom_opt=""

    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_opt="
            -javaagent:/opt/prometheus/jmx_prometheus_javaagent.jar=${CN_PROMETHEUS_PORT}:/opt/prometheus/prometheus-config.yaml
        "
    fi
    echo "${prom_opt}"
}

python3 /app/flex_aio/wait.py
python3 /app/flex_aio/jans_config_api/bootstrap.py
python3 /app/flex_aio/jans_config_api/upgrade.py
python3 /app/flex_aio/mod_context.py jans-config-api

# run config-api
cd /opt/jans/jetty/jans-config-api
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-config-api \
    -Dlog.base=/opt/jans/jetty/jans-config-api \
    -Dpython.home=/opt/jython \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=$(get_logging_files) \
    $(get_prometheus_opt) \
    ${CN_CONFIG_API_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8074 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
