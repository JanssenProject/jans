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
        prom_agent_version="0.17.2"

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${prom_agent_version}/jmx_prometheus_javaagent-${prom_agent_version}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

get_java_options() {
    if [ -n "${CN_CASA_JAVA_OPTIONS}" ]; then
        echo " ${CN_CASA_JAVA_OPTIONS} "
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
    if [ -n "${CN_CASA_JETTY_ARGS}" ]; then
        echo " ${CN_CASA_JETTY_ARGS} "
    else
        echo " ${CN_JETTY_ARGS} "
    fi
}

touch "$CN_CASA_ADMIN_LOCK_FILE"
get_prometheus_lib
python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/mod_context.py" jans-casa
python3 "$basedir/upgrade.py"
# python3 "$basedir/jca_sync.py" &
python3 "$basedir/auth_conf.py"

cd /opt/jans/jetty/jans-casa
# shellcheck disable=SC2046
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-casa \
    -Dlog.base=/opt/jans/jetty/jans-casa \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    -Dpython.home=/opt/jython \
    -Dadmin.lock=${CN_CASA_ADMIN_LOCK_FILE} \
    -Dcom.nimbusds.jose.jwk.source.RemoteJWKSet.defaultHttpSizeLimit=${CN_CASA_JWKS_SIZE_LIMIT} \
    $(get_max_ram_percentage) \
    $(get_prometheus_opt) \
    $(get_java_options) \
    -jar /opt/jetty/start.jar \
        jetty.http.host="${CN_CASA_JETTY_HOST}" \
        jetty.http.port="${CN_CASA_JETTY_PORT}" \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false \
        $(get_jetty_args)
