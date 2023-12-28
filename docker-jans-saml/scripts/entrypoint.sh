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

# # export_keycloak_admin_creds() {
# admin_username=${KEYCLOAK_ADMIN:-admin}
# # export KEYCLOAK_ADMIN="${admin_username}"

# if [ -f /etc/jans/conf/keycloak_admin_password ]; then
#     admin_password="$(cat /etc/jans/conf/keycloak_admin_password)"
# else
#     admin_password="admin"
# fi
#     # export KEYCLOAK_ADMIN_PASSWORD="${admin_password}"
# # }

# export_keycloak_admin_creds
python3 "$basedir/wait.py"
python3 "$basedir/bootstrap.py"
python3 "$basedir/configure_kc.py" &

java_opts="$(get_max_ram_percentage) $(get_java_options)"
export JAVA_OPTS_APPEND="$java_opts"

# default admin credentials (will be changed)
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin

# shellcheck disable=SC2046
exec /opt/keycloak/bin/kc.sh start \
    -Dlog.base=/opt/keycloak/logs/ \
    -Djans.config.prop.path=/opt/keycloak/providers \
    --health-enabled=true \
    --metrics-enabled=true \
    --http-host="${CN_SAML_HOST}" \
    --http-port="${CN_SAML_PORT}" \
    --http-enabled=true \
    --http-relative-path=/kc \
    --hostname="localhost" \
    --hostname-admin="localhost" \
    --hostname-path=/kc \
    --hostname-strict-https=false \
    --log=console \
    --log-console-format='jans-saml - %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n' \
    --log-file=/opt/keycloak/logs/keycloak.log \
    --log-level=INFO
    # --db=dev-mem \
    # --optimized
