#!/bin/sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

get_debug_opt() {
    debug_opt=""
    if [ -n "${CN_DEBUG_PORT}" ]; then
        debug_opt="
            -agentlib:jdwp=transport=dt_socket,address=${CN_DEBUG_PORT},server=y,suspend=n
        "
    fi
    echo "${debug_opt}"
}

move_builtin_jars() {
    # twilio, jsmpp, casa-config, jans-fido2-client
    for src in /opt/jans/jetty/jans-auth/_libs/*.jar; do
        fname=$(basename "$src")
        mv "$src" "/opt/jans/jetty/jans-auth/custom/libs/$fname"
    done
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
    if [ -n "${CN_AUTH_JAVA_OPTIONS}" ]; then
        echo " ${CN_AUTH_JAVA_OPTIONS} "
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

move_builtin_jars
get_prometheus_lib
python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/jks_sync.py" &
python3 "$basedir/mod_context.py" jans-auth
python3 "$basedir/auth_conf.py"

cd /opt/jans/jetty/jans-auth
# shellcheck disable=SC2046
exec java \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-auth \
    -Dlog.base=/opt/jans/jetty/jans-auth \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    -Dpython.home=/opt/jython \
    $(get_debug_opt) \
    $(get_max_ram_percentage) \
    $(get_prometheus_opt) \
    $(get_java_options) \
    -jar /opt/jetty/start.jar \
        jetty.http.host="${CN_AUTH_JETTY_HOST}" \
        jetty.http.port="${CN_AUTH_JETTY_PORT}" \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false \
        jetty.httpConfig.requestHeaderSize=$CN_JETTY_REQUEST_HEADER_SIZE
