#!/bin/sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

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

get_prometheus_lib() {
    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_agent_version="0.17.2"

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${prom_agent_version}/jmx_prometheus_javaagent-${prom_agent_version}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

get_java_options() {
    if [ -n "${CN_CONFIG_API_JAVA_OPTIONS}" ]; then
        echo " ${CN_CONFIG_API_JAVA_OPTIONS} "
    else
        # backward-compat
        echo " ${CN_JAVA_OPTIONS} "
    fi
}

get_max_ram_percentage() {
    if [ -n "${CN_MAX_RAM_PERCENTAGE}" ]; then
        echo " -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE "
    fi
}

get_prometheus_lib
python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/mod_context.py" jans-config-api
python3 "$basedir/upgrade.py"

cd /opt/jans/jetty/jans-config-api
# shellcheck disable=SC2046
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-config-api \
    -Dlog.base=/opt/jans/jetty/jans-config-api \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=$(get_logging_files) \
    -Dpython.home=/opt/jython \
    $(get_max_ram_percentage) \
    $(get_prometheus_opt) \
    $(get_java_options) \
    -jar /opt/jetty/start.jar \
        jetty.http.host="${CN_CONFIG_API_JETTY_HOST}" \
        jetty.http.port="${CN_CONFIG_API_JETTY_PORT}" \
        jetty.http.idleTimeout="${CN_JETTY_IDLE_TIMEOUT}" \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
