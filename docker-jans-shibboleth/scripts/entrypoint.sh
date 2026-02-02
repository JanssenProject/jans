#!/bin/bash

set -e

# ==============
# Janssen Shibboleth IDP Entrypoint
# ==============

python3 /app/scripts/wait.py

if [ ! -f /app/.initialized ]; then
    python3 /app/scripts/bootstrap.py
    touch /app/.initialized
fi

python3 /app/scripts/healthcheck.py &

cd /opt/jans/jetty/shibboleth-idp

exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=${CN_MAX_RAM_PERCENTAGE} \
    -Djava.security.egd=file:/dev/urandom \
    -Djetty.home=/opt/jetty \
    -Djetty.base=/opt/jans/jetty/shibboleth-idp \
    -Didp.home=/opt/shibboleth-idp \
    -Dlogback.configurationFile=/opt/shibboleth-idp/conf/logback.xml \
    -jar /opt/jetty/start.jar
