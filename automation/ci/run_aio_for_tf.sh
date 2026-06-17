#!/usr/bin/env bash

# Bring up the AIO (all-in-one) demo stack from a PREBUILT image and wait until
# config-api is reachable, for the Terraform-provider acceptance tests. Unlike
# run_aio_integration.sh this builds nothing from source -- it pulls a published
# image and runs the same consul+vault+traefik+DB+AIO compose stack on the runner.
#
# Run from the repo root. Env:
#   JANS_FQDN          required, e.g. tf-123.jans.test (must resolve to 127.0.0.1)
#   JANS_PERSISTENCE   required, MYSQL | PGSQL
#   AIO_IMAGE_TAG      optional, default ghcr nightly all-in-one

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

: "${JANS_FQDN:?set JANS_FQDN}"
: "${JANS_PERSISTENCE:?set JANS_PERSISTENCE}"
AIO_IMAGE_TAG="${AIO_IMAGE_TAG:-ghcr.io/janssenproject/jans/all-in-one:0.0.0-nightly}"

# start_janssen_aio_demo.sh hardcodes the demo image as :0.0.0-nightly, so retag
# whatever we pulled to that name and let the compose file use it unchanged.
DEMO_TAG="ghcr.io/janssenproject/jans/all-in-one:0.0.0-nightly"
echo "[I] pulling $AIO_IMAGE_TAG"
docker pull "$AIO_IMAGE_TAG"
if [ "$AIO_IMAGE_TAG" != "$DEMO_TAG" ]; then
  docker tag "$AIO_IMAGE_TAG" "$DEMO_TAG"
fi

# mysql OOMs at the demo default (768M) under the test-data load; the runner has headroom.
export MYSQL_MEM_LIMIT="${MYSQL_MEM_LIMIT:-3G}"
# JANS_CI_CD_RUN switches the demo to FILE/TRACE logging so per-service logs land in the container.
export JANS_CI_CD_RUN=true

# Run the demo in the background and relax its mode-600 TLS certs as they appear, so the
# in-container configurator (uid 1000) can read ca.key/web_https.key on its first run.
bash automation/start_janssen_aio_demo.sh "$JANS_FQDN" "$JANS_PERSISTENCE" "" 127.0.0.1 &
demo_pid=$!
for _ in $(seq 1 120); do
  [ -d automation/jans-aio-demo/templates ] && chmod -R a+rX automation/jans-aio-demo/templates 2>/dev/null || true
  kill -0 "$demo_pid" 2>/dev/null || break
  sleep 2
done
wait "$demo_pid" 2>/dev/null || echo "[warn] demo readiness gate did not pass; re-checking below"

# Final gate: config-api fronts on the same host as jans-auth; openid-configuration 200 means
# traefik + auth + config-api are all serving.
end=$((SECONDS + 600))
while [ $SECONDS -lt $end ]; do
  code=$(curl -sk -o /dev/null -w '%{http_code}' "https://${JANS_FQDN}/.well-known/openid-configuration" || true)
  echo "openid-configuration: $code"
  [ "$code" = "200" ] && { echo "[I] AIO is ready"; exit 0; }
  sleep 15
done

echo "::error::AIO did not become ready in time"
docker ps -a || true
docker logs jans 2>&1 | tail -n 120 || true
exit 1
