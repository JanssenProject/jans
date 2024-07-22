#!/bin/bash

if [ -z "$1" ]; then
    echo "The used db was not specified as argument, will use mysql as default"
    yaml="jans-mysql-compose.yml"
else
	case "$1" in
		mysql|ldap|postgres|couchbase|spanner)
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
    JANSSEN_VERSION="1.1.4_dev"
fi

if [ -z "$DATABASE_VOLUME_NAME" ]; then
    DATABASE_VOLUME_NAME="db-data"
fi

cd $SCRIPT_DIR

JANSSEN_IMAGE=${DOCKER_PROJECT}_${INSTALLED_JANSSEN_NAME}:${JANSSEN_VERSION}
if docker image inspect ${JANSSEN_IMAGE} &> /dev/null; then
	echo "after install janssen image found - it will be used"
else
	echo "no after install janssen image found - fresh installation with empty database will be executed"
	if docker volume inspect ${DOCKER_PROJECT}_${DATABASE_VOLUME_NAME} &> /dev/null; then
		docker volume rm ${DOCKER_PROJECT}_${DATABASE_VOLUME_NAME} &> /dev/null
	fi
	JANSSEN_IMAGE="ghcr.io/janssenproject/jans/monolith:${JANSSEN_VERSION}"
fi

export JANSSEN_IMAGE
docker compose -f ${yaml} up -d





