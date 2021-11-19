#!/bin/sh

set -e

python3 /app/scripts/wait.py

if [ ! -f /deploy/touched ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

# run the server
# customized `/opt/client-api/bin/client-api-start.sh`
exec java \
    -Djava.net.preferIPv4Stack=true \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    ${CN_JAVA_OPTIONS} \
    -cp /opt/client-api/client-api.jar:/opt/client-api/lib/* \
    io.jans.ca.server.RpServerApplication server /opt/client-api/conf/client-api-server.yml
