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
    # move twilio lib
    if [ ! -f /opt/jans/jetty/jans-auth/custom/libs/twilio.jar ]; then
        # mv /usr/share/java/twilio.jar /opt/jans/jetty/jans-auth/custom/libs/twilio.jar
        cp /usr/share/java/twilio.jar /opt/jans/jetty/jans-auth/custom/libs/twilio.jar
    fi

    # move jsmpp lib
    if [ ! -f /opt/jans/jetty/jans-auth/custom/libs/jsmpp.jar ]; then
        # mv /usr/share/java/jsmpp.jar /opt/jans/jetty/jans-auth/custom/libs/jsmpp.jar
        cp /usr/share/java/jsmpp.jar /opt/jans/jetty/jans-auth/custom/libs/jsmpp.jar
    fi
}

# ==========
# ENTRYPOINT
# ==========

move_builtin_jars
python3 /app/scripts/wait.py

if [ ! -f /deploy/touched ]; then
    python3 /app/scripts/bootstrap.py
    touch /deploy/touched
fi

python3 /app/scripts/jks_sync.py &
python3 /app/scripts/mod_context.py

# run auth-server
cd /opt/jans/jetty/jans-auth
mkdir -p /opt/jetty/temp
exec java \
    -server \
    -XX:+DisableExplicitGC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=$CN_MAX_RAM_PERCENTAGE \
    -Djans.base=/etc/jans \
    -Dserver.base=/opt/jans/jetty/jans-auth \
    -Dlog.base=/opt/jans/jetty/jans-auth \
    -Dpython.home=/opt/jython \
    -Djava.io.tmpdir=/opt/jetty/temp \
    -Dlog4j2.configurationFile=resources/log4j2.xml \
    $(get_debug_opt) \
    ${CN_JAVA_OPTIONS} \
    -jar /opt/jetty/start.jar
