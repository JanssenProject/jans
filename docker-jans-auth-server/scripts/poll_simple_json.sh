#!/usr/bin/env sh

set -e

attempt=1

while [ $attempt -le 30 ]
do
    if [ -f /tmp/mysql_simple_json.sh ]; then
        # shellcheck disable=1091
        . /tmp/mysql_simple_json.sh
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done
