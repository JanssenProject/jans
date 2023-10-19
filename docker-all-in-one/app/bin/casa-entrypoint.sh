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

# ==========
# ENTRYPOINT
# ==========

python3 /app/flex_aio/wait.py
python3 /app/flex_aio/casa/bootstrap.py
python3 /app/flex_aio/casa/upgrade.py
# python3 /app/scripts/jca_sync.py &
# python3 /app/flex_aio/casa/auth_conf.py
python3 /app/flex_aio/mod_context.py casa

# create administrable file ootb
touch "$GLUU_CASA_ADMIN_LOCK_FILE"

# run Casa server
cd /opt/jans/jetty/casa
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/casa \
    -Dlog.base=/opt/jans/jetty/casa \
    -Dpython.home=/opt/jython \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    -Dadmin.lock=${GLUU_CASA_ADMIN_LOCK_FILE} \
    -Dcom.nimbusds.jose.jwk.source.RemoteJWKSet.defaultHttpSizeLimit=${GLUU_CASA_JWKS_SIZE_LIMIT} \
    $(get_prometheus_opt) \
    ${GLUU_CASA_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar \
        jetty.http.port=8082 \
        jetty.deploy.scanInterval=0 \
        jetty.httpConfig.sendServerVersion=false
