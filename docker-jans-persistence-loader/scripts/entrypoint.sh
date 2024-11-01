#!/usr/bin/env sh

set -e

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

python3 "$basedir/wait.py"
sh "$basedir/poll_simple_json.sh" &
exec python3 "$basedir/bootstrap.py"
