#!/bin/sh

set -e

export CN_CONTAINER_METADATA_NAMESPACE="${CN_CONFIG_KUBERNETES_NAMESPACE}"

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

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
    *)
        show_help
esac
