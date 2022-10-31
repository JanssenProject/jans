#!/bin/sh

set -e

get_prometheus_opt() {
    prom_opt=""

    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_opt="
            -javaagent:/opt/prometheus/jmx_prometheus_javaagent.jar=${CN_PROMETHEUS_PORT}:/opt/prometheus/prometheus-config.yaml
        "
    fi
    echo "${prom_opt}"
}

python3 /app/scripts/wait.py
python3 /app/scripts/bootstrap.py
python3 /app/scripts/mod_context.py jans-fido2

cd /opt/jans/jetty/jans-fido2
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-fido2 \
    -Dlog.base=/opt/jans/jetty/jans-fido2 \
    -Djava.io.tmpdir=/tmp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    $(get_prometheus_opt) \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar jetty.deploy.scanInterval=0 jetty.httpConfig.sendServerVersion=false
