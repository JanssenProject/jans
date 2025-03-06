#!/bin/sh

set -e

export CN_CONTAINER_METADATA_NAMESPACE="${CN_CONFIG_KUBERNETES_NAMESPACE}"

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

# =======
# KC sync
# =======

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

run_kc_sync() {
    app_config_file=${SCHEDULER_HOME}/conf/config.properties
    log_config_file=${SCHEDULER_HOME}/conf/logback.xml
    scheduler_version=${CN_VERSION}

    python3 "$basedir/kc.py"

    exec java \
        -Dapp.config="${app_config_file}" \
        -Dlogback.configurationFile="${log_config_file}" \
        -Dapp.version="${scheduler_version}" \
        -Dapp.home="${SCHEDULER_HOME}" \
        $(get_max_ram_percentage) \
        $(get_java_options) \
        -cp "${SCHEDULER_HOME}/lib/*" \
        io.jans.kc.scheduler.App
}

# =======
# cleaner
# =======

run_cleanup() {
    shift
    exec python3 "$basedir/cleaner.py" "$@"
}

# ===========
# certmanager
# ===========

run_certmanager() {
    shift
    exec python3 "$basedir/certmanager.py" "$@"
}

# ==============
# misc. commands
# ==============

show_help() {
    cat << EOF
Usage: cloudtools [OPTIONS] COMMAND [ARGS]...

Commands:
  certmanager   Manage cert and crypto keys
  cleanup       Cleanup expired entries in persistence
  kc-sync       Sync config between config-jans-api and Keycloak
EOF
}

# ==========
# entrypoint
# ==========

top_cmd=$1

case $top_cmd in
    "certmanager")
        run_certmanager "$@"
        ;;
    "cleanup")
        run_cleanup "$@"
        ;;
    "kc-sync")
        run_kc_sync
        ;;
    *)
        show_help
esac
