#!/usr/bin/env sh

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

get_prometheus_opt() {
    prom_opt=""

    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_opt="
            -javaagent:/opt/prometheus/jmx_prometheus_javaagent.jar=${CN_PROMETHEUS_PORT}:/opt/prometheus/prometheus-config.yaml
        "
    fi
    echo "${prom_opt}"
}

# ==========
# ENTRYPOINT
# ==========

python3 /app/flex_aio/wait.py
python3 /app/flex_aio/jans_auth/bootstrap.py
python3 /app/flex_aio/mod_context.py jans-auth
# python3 /app/flex_aio/jans_auth/auth_conf.py

cd /opt/jans/jetty/jans-auth
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-auth \
    -Dlog.base=/opt/jans/jetty/jans-auth \
    -Dpython.home=/opt/jython \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    $(get_debug_opt) \
    $(get_prometheus_opt) \
    ${CN_AUTH_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8081 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false \
        jetty.httpConfig.requestHeaderSize=$CN_JETTY_REQUEST_HEADER_SIZE
