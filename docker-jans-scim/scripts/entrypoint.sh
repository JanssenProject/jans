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

get_prometheus_lib() {
    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_agent_version="0.17.2"

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${prom_agent_version}/jmx_prometheus_javaagent-${prom_agent_version}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

get_prometheus_lib
python3 /app/scripts/wait.py
python3 /app/scripts/bootstrap.py
python3 /app/scripts/upgrade.py
python3 /app/scripts/mod_context.py jans-scim

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
    $(get_prometheus_opt) \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar jetty.deploy.scanInterval=0 jetty.httpConfig.sendServerVersion=false
