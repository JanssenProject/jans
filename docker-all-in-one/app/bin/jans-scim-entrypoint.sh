#!/usr/bin/env sh

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

python3 /app/flex_aio/wait.py
python3 /app/flex_aio/jans_scim/bootstrap.py
python3 /app/flex_aio/jans_scim/upgrade.py
python3 /app/flex_aio/mod_context.py jans-scim

cd /opt/jans/jetty/jans-scim
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-scim \
    -Dlog.base=/opt/jans/jetty/jans-scim \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dpython.home=/opt/jython \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    $(get_prometheus_opt) \
    ${CN_SCIM_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8087 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
