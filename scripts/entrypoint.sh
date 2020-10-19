#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched ]; then
    python3 /app/scripts/entrypoint.py
    touch /deploy/touched
fi

# run the server
# customized `/opt/oxd-server/bin/oxd-start.sh`
exec java \
    -Djava.net.preferIPv4Stack=true \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$JANS_MAX_RAM_PERCENTAGE \
    ${JANS_JAVA_OPTIONS} \
    -cp /opt/oxd-server/oxd-server.jar:/opt/oxd-server/lib/* \
    org.gluu.oxd.server.OxdServerApplication server /opt/oxd-server/conf/oxd-server.yml
