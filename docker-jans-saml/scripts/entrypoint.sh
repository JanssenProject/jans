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

export_keycloak_admin_creds() {
    creds_file=${CN_SAML_KC_ADMIN_CREDENTIALS_FILE:-/etc/jans/conf/kc_admin_creds}
    creds="$(base64 -d < ${creds_file})"
    admin_username=$(echo "$creds" | awk -F ":" '{print $1}')
    admin_password=$(echo "$creds" | awk -F ":" '{print $2}')
    export KEYCLOAK_ADMIN="$admin_username"
    export KEYCLOAK_ADMIN_PASSWORD="$admin_password"
    # remove file for security purpose
    rm -f "$creds_file"
}

python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/configure_kc.py" &
python3 "$basedir/upgrade.py"
export_keycloak_admin_creds

java_opts="$(get_max_ram_percentage) $(get_java_options)"
export JAVA_OPTS_APPEND="$java_opts -Djans.base=/etc/jans -Dlog.base=/opt/keycloak/logs/ -Djans.config.prop.path=/opt/keycloak/providers"

# build optimized KC for production (https://www.keycloak.org/server/configuration#_optimize_the_keycloak_startup)
/opt/keycloak/bin/kc.sh build --http-relative-path=/kc

# shellcheck disable=SC2046
exec /opt/keycloak/bin/kc.sh start \
    --log=console \
    --log-console-format='jans-saml - %d{yyyy-MM-dd HH:mm:ss,SSS} - %-5p - [%c] (%t) %s%e%n' \
    --optimized \
    --verbose
