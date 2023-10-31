#!/bin/sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

get_java_options() {
    if [ -n "${CN_SAML_JAVA_OPTIONS}" ]; then
        echo " ${CN_SAML_JAVA_OPTIONS} "
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

java_opts="$(get_max_ram_percentage) $(get_java_options)"
export JAVA_OPTS_APPEND="$java_opts"

# shellcheck disable=SC2046
exec bash /opt/keycloak/bin/kc.sh start \
    --db=dev-mem \
    -Dlog.base=/opt/keycloak/logs/ \
    -Djans.config.prop.path=/opt/keycloak/providers
    # --optimized
