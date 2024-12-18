#!/bin/sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

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
        agent_version=${PROMETHEUS_AGENT_VERSION:-1.0.1}

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            curl -sS "https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${agent_version}/jmx_prometheus_javaagent-${agent_version}.jar" -o /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

get_java_options() {
    if [ -n "${CN_LINK_JAVA_OPTIONS}" ]; then
        echo " ${CN_LINK_JAVA_OPTIONS} "
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

get_jetty_args() {
    if [ -n "${CN_LINK_JETTY_ARGS}" ]; then
        echo " ${CN_LINK_JETTY_ARGS} "
    else
        echo " ${CN_JETTY_ARGS} "
    fi
}

get_prometheus_lib
python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/mod_context.py" jans-link
python3 "$basedir/upgrade.py"

cd /opt/jans/jetty/jans-link
# shellcheck disable=SC2046
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-link \
    -Dlog.base=/opt/jans/jetty/jans-link \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    -Dpython.home=/opt/jython \
    $(get_max_ram_percentage) \
    $(get_prometheus_opt) \
    $(get_java_options) \
    -jar /opt/jetty/start.jar \
        jetty.http.host="${CN_LINK_JETTY_HOST}" \
        jetty.http.port="${CN_LINK_JETTY_PORT}" \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false \
        $(get_jetty_args)
