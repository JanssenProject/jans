#!/bin/sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

app_config_file=${SCHEDULER_HOME}/conf/config.properties
log_config_file=${SCHEDULER_HOME}/conf/logback.xml
scheduler_version=${CN_VERSION}

get_java_options() {
    if [ -n "${CN_KC_SCHEDULER_JAVA_OPTIONS}" ]; then
        echo " ${CN_KC_SCHEDULER_JAVA_OPTIONS} "
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

python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"

# shellcheck disable=SC2046
exec java \
    -Dapp.config="${app_config_file}" \
    -Dlogback.configurationFile="${log_config_file}" \
    -Dapp.version="${scheduler_version}" \
    -Dapp.home="${SCHEDULER_HOME}" \
    $(get_max_ram_percentage) \
    $(get_java_options) \
    -cp "${SCHEDULER_HOME}/lib/*" \
    io.jans.kc.scheduler.App
