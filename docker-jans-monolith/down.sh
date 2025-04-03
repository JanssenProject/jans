#!/bin/bash

if [ -z "$1" ]; then
    echo "The used db was not specified as argument, will use mysql as default"
    yaml="jans-mysql-compose.yml"
else
	case "$1" in
		mysql|postgres)
			yaml="jans-${1}-compose.yml"
			;;
		*)
			yaml="${1}"
			;;
	esac
fi

# Get the directory of the script
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
DOCKER_PROJECT=$(basename "$SCRIPT_DIR")

if [ -z "$INSTALLED_JANSSEN_NAME" ]; then
    INSTALLED_JANSSEN_NAME="after-install-jans"
fi

if [ -z "$JANSSEN_VERSION" ]; then
    JANSSEN_VERSION="2.0.0-1"
fi

if [ -z "$JANSSEN_SERVICE_NAME" ]; then
    JANSSEN_SERVICE_NAME="jans"
fi

cd $SCRIPT_DIR

JANSSEN_IMAGE="${DOCKER_PROJECT}_${INSTALLED_JANSSEN_NAME}:${JANSSEN_VERSION}"
JANSSEN_CONTAINER="${DOCKER_PROJECT}-${JANSSEN_SERVICE_NAME}-1"

if ! docker image inspect ${JANSSEN_IMAGE} &> /dev/null; then
	if docker exec "${JANSSEN_CONTAINER}" sh -c '[ -e /janssen/deployed ]'; then
		echo "installation of janssen was sucessfull - an after install image will be created (this can take a while)"
		docker stop ${JANSSEN_CONTAINER} &> /dev/null;
		docker commit ${JANSSEN_CONTAINER} ${JANSSEN_IMAGE}
		#ensure the down will be the same as on up
		JANSSEN_IMAGE="ghcr.io/janssenproject/jans/monolith:${JANSSEN_VERSION}"
	fi
fi
export JANSSEN_IMAGE
docker compose -f ${yaml} down
