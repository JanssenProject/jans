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
python3 /app/flex_aio/jans_fido2/bootstrap.py
python3 /app/flex_aio/jans_fido2/upgrade.py
python3 /app/flex_aio/mod_context.py jans-fido2

cd /opt/jans/jetty/jans-fido2
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-fido2 \
    -Dlog.base=/opt/jans/jetty/jans-fido2 \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    -Dpython.home=/opt/jython \
    $(get_prometheus_opt) \
    ${CN_FIDO2_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8073 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
