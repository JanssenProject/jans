#!/bin/sh
set -e

# =========
# FUNCTIONS
# =========

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
    #twilio, jsmpp, casa-config, jans-fido2-client
    for src in /usr/share/java/*.jar; do
        fname=$(basename "$src")
        cp "$src" "/opt/jans/jetty/jans-auth/custom/libs/$fname"
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

# ==========
# ENTRYPOINT
# ==========

move_builtin_jars
get_prometheus_lib
python3 /app/scripts/wait.py
python3 /app/scripts/bootstrap.py
python3 /app/scripts/jks_sync.py &
python3 /app/scripts/mod_context.py jans-auth
python3 /app/scripts/auth_conf.py

# run auth-server
cd /opt/jans/jetty/jans-auth
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-auth \
    -Dlog.base=/opt/jans/jetty/jans-auth \
    -Dpython.home=/opt/jython \
    -Djava.io.tmpdir=/tmp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    $(get_debug_opt) \
    $(get_prometheus_opt) \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false \
        jetty.httpConfig.requestHeaderSize=$CN_JETTY_REQUEST_HEADER_SIZE
