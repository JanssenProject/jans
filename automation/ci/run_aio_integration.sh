#!/usr/bin/env bash

# Whole jans-side integration-test flow, run ON the ephemeral CI VM (8 dedicated vCPU) rather than
# the 2-core GitHub runner. Run from the repo root (checkout rsync'd to /root/jans).
#
# Required env (set by the workflow over SSH):
#   JANS_FQDN, JANS_PERSISTENCE (MYSQL|PGSQL), LOG_LEVEL (INFO|TRACE),
#   DB_NAME, DB_USER, DB_PASSWORD,
#   CN_CONFIG_API_TEST_CLIENT_ID, CN_CONFIG_API_TEST_CLIENT_SECRET, CN_CONFIG_API_TEST_CLIENT_TRUSTED,
#   GITHUB_ACTOR, JANS_TOKEN  (consumed by .github/maven-settings.xml)

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

MVN_SETTINGS="$REPO_ROOT/.github/maven-settings.xml"
AIO_IMAGE_TAG="ghcr.io/janssenproject/jans/all-in-one:0.0.0-nightly"

# Which suites to run (comma-separated top-level modules, or "all"). The reactor + AIO are always
# built in full so runtime dependencies are honoured; this only gates which test suites execute.
TEST_MODULES="${TEST_MODULES:-all}"
want_module() {
  case "$TEST_MODULES" in all | "") return 0 ;; esac
  case ",${TEST_MODULES}," in *",$1,"*) return 0 ;; *) return 1 ;; esac
}
echo "[info] test modules: $TEST_MODULES"

# Resolve DB parameters from the persistence backend (mirrors the workflow's "Resolve DB
# parameters" step). RDBM_PORT/RDBM_SCHEMA feed render_test_profiles.py.
if [ "$JANS_PERSISTENCE" = "PGSQL" ]; then
  RDBM_JDBC="postgresql"
  RDBM_PORT="5432"
  RDBM_SCHEMA="public"
else
  RDBM_JDBC="mysql"
  RDBM_PORT="3306"
  RDBM_SCHEMA="$DB_NAME"
fi

# Capture container diagnostics to aio-logs/ on any exit, so an early failure still leaves an artifact.
mkdir -p aio-logs
collect_diag() {
  local rc=$?
  echo "::group::container diagnostics (exit=$rc)"
  docker ps -a > aio-logs/docker-ps.txt 2>&1 || true
  : > aio-logs/docker-state.txt
  for c in $(docker ps -aq 2>/dev/null); do
    nm=$(docker inspect --format '{{.Name}}' "$c" 2>/dev/null | tr -d '/')
    docker inspect "$c" --format '{{.Name}} status={{.State.Status}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}}' >> aio-logs/docker-state.txt 2>&1 || true
    docker logs --tail 3000 "$c" > "aio-logs/container-${nm:-$c}.log" 2>&1 || true
  done
  docker exec jans supervisorctl -c /app/conf/supervisord.conf status > aio-logs/supervisord-status.txt 2>&1 || true
  echo "::endgroup::"
}
trap collect_diag EXIT

# ---------------------------------------------------------------------------
# Seed the build caches shipped with the checkout
# ---------------------------------------------------------------------------
# ci-cache/ arrives inside the rsync'd checkout and holds downloaded dependencies only. io/jans is
# excluded so a stale 0.0.0-nightly from another commit can't be resolved silently; the cedarling
# target dir is excluded to keep the entry inside the repo's shared 10 GB cache budget.
CACHE_DIR="$REPO_ROOT/ci-cache"
if [ -d "$CACHE_DIR" ]; then
  echo "::group::seed build caches"
  du -sh "$CACHE_DIR"/* 2>/dev/null || true
  mkdir -p "$HOME/.m2/repository" "$HOME/.cargo"
  [ -d "$CACHE_DIR/m2" ] && cp -a "$CACHE_DIR/m2/." "$HOME/.m2/repository/"
  [ -d "$CACHE_DIR/cargo" ] && cp -a "$CACHE_DIR/cargo/." "$HOME/.cargo/"
  echo "::endgroup::"
fi

# ---------------------------------------------------------------------------
# Build the cedarling native lib (cedarling-java + jans-lock tests need it)
# ---------------------------------------------------------------------------
# cedarling-java's pom fetches libcedarling_uniffi-<ver>.so + the kotlin bindings from
# cedarling.base.url. Build them from source and serve them locally so those modules build without
# the release (mirrors build-test.yml). Best-effort: a failure only skips the cedarling-java /
# jans-lock tests, not the rest of the run.
echo "::group::build cedarling native lib"
set +e
export DEBIAN_FRONTEND=noninteractive
command -v cc     >/dev/null 2>&1 || apt-get -o DPkg::Lock::Timeout=600 install -y -qq build-essential pkg-config libssl-dev
command -v protoc >/dev/null 2>&1 || apt-get -o DPkg::Lock::Timeout=600 install -y -qq protobuf-compiler
command -v zip    >/dev/null 2>&1 || apt-get -o DPkg::Lock::Timeout=600 install -y -qq zip unzip
command -v cargo  >/dev/null 2>&1 || curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y >/dev/null
export PATH="$HOME/.cargo/bin:$PATH"
CEDARLING_NV=0.0.0
CED_OPTS=""   # set only on the ready path below; consumers are gated on CED_READY, not on this
CED_READY=0
ced_serve="$REPO_ROOT/cedarling-native"; mkdir -p "$ced_serve"
# Build BOTH artifacts, &&-chained so any failing step aborts the whole prep (set -e is suppressed
# inside an if-condition subshell), then serve + confirm reachable before enabling the consumers.
if ( cd jans-cedarling/bindings/cedarling_uniffi &&
     cargo build -r --locked -p cedarling_uniffi &&
     cp ../../target/release/libcedarling_uniffi.so "$ced_serve/libcedarling_uniffi-${CEDARLING_NV}.so" &&
     cargo run --locked --bin uniffi-bindgen generate \
       --library "$REPO_ROOT/jans-cedarling/target/release/libcedarling_uniffi.so" --language kotlin --out-dir ./ &&
     zip -qr "$ced_serve/cedarling_uniffi-kotlin-${CEDARLING_NV}.zip" uniffi ); then
  ( cd "$ced_serve" && exec python3 -m http.server 8099 >/dev/null 2>&1 ) &
  for _ in $(seq 1 10); do
    curl -sf "http://127.0.0.1:8099/libcedarling_uniffi-${CEDARLING_NV}.so" -o /dev/null \
      && curl -sf "http://127.0.0.1:8099/cedarling_uniffi-kotlin-${CEDARLING_NV}.zip" -o /dev/null \
      && { CED_READY=1; CED_OPTS="-Dcedarling.base.url=http://127.0.0.1:8099 -Dcedarling.native.version=${CEDARLING_NV}"; break; }
    sleep 1
  done
fi
[ "$CED_READY" = 1 ] || echo "[warn] cedarling native prep failed; cedarling-java + jans-lock will be skipped"
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Build jans modules + serve the locally-built config-api artifacts
# ---------------------------------------------------------------------------
# Build the reactor before the AIO image and serve the locally-built config-api WAR + plugins
# (coordinate-named in ~/.m2) to its docker build, so Phase D doesn't depend on the nightly release.
echo "::group::build jans modules"
set -e
# server-fips is a packaging-only WAR variant nothing downstream consumes (the service images fetch
# the plain jans-<svc>-<ver>.war); git-commit-id-plugin cost 8 min per module against the shallow
# checkout for cosmetic git.properties. The reactor was also serial on an 8-core VM.
MVN_PAR="-T 1C"
MVN_SKIPS="-Dmaven.gitcommitid.skip=true"
NO_FIPS="-pl !server-fips"
for mod in jans-bom jans-orm jans-core jans-auth-server jans-scim jans-config-api jans-fido2; do
  pl=""
  case "$mod" in jans-auth-server | jans-scim | jans-config-api | jans-fido2) pl="$NO_FIPS" ;; esac
  echo "::group::build $mod"
  mvn $MVN_PAR -B -ntp -s "$MVN_SETTINGS" -Dcfg=default -Dmaven.test.skip=true $MVN_SKIPS -fae \
    $pl -f "$mod/pom.xml" clean install
  echo "::endgroup::"
done
set +e
# Extra-coverage modules (best-effort; tested in the unit phase). Built after the core reactor so a
# failure can't block the AIO build or the core suites. $CED_OPTS is harmless to agama.
for mod in agama jans-cedarling/bindings/cedarling-java jans-lock/lock-server; do
  case "$mod" in
    *cedarling*|*lock*) [ "$CED_READY" = 1 ] || { echo "[info] skip build $mod (cedarling native lib not ready)"; continue; } ;;
  esac
  pl=""
  case "$mod" in *lock-server) pl="$NO_FIPS" ;; esac
  echo "::group::build $mod"
  mvn $MVN_PAR -B -ntp -s "$MVN_SETTINGS" -Dcfg=default -Dmaven.test.skip=true $MVN_SKIPS -fae $CED_OPTS \
    $pl -f "$mod/pom.xml" clean install || echo "[warn] build $mod failed; its tests will be skipped"
  echo "::endgroup::"
done
set -e
if [ -z "${AIO_IMAGE:-}" ]; then
  LOCAL_RELEASE="$REPO_ROOT/local-release"
  mkdir -p "$LOCAL_RELEASE"
  find "$HOME/.m2/repository/io/jans" -type f -path "*/0.0.0-nightly/*" \
    \( -name '*.war' -o -name '*-distribution.jar' -o -name '*-agama-pw.gama' \) \
    -exec cp -f {} "$LOCAL_RELEASE/" \;
  echo "serving local artifacts on :8088"; ls "$LOCAL_RELEASE"
  ( cd "$LOCAL_RELEASE" && exec python3 -m http.server 8088 >/dev/null 2>&1 ) &
fi
set +e
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Build the AIO image (with integration-test env baked in)
# ---------------------------------------------------------------------------
echo "::group::build AIO image"
set -e
if [ -n "${AIO_IMAGE:-}" ]; then
  docker pull "$AIO_IMAGE"
  base_image="$AIO_IMAGE"
else
  # Independent of each other, so build concurrently; serially they cost ~17 min. Per-image logs
  # because parallel output interleaves unreadably.
  pids=""
  docker build -t local/persistence-loader:ci ./docker-jans-persistence-loader \
    > aio-logs/image-persistence-loader.log 2>&1 &
  pids="$pids $!:persistence-loader"
  # Build every service image from the PR artifacts served on :8088 (CN_RELEASE_DOWNLOAD_URL), so the
  # running AIO exercises this checkout's auth/scim/fido2/config-api code -- not the nightly release.
  # Otherwise a fix under test would only reach the client-side suites, never the live server.
  for svc in config-api auth-server scim fido2; do
    docker build --network=host --build-arg CN_RELEASE_DOWNLOAD_URL=http://127.0.0.1:8088 \
      -t "local/$svc:ci" "./docker-jans-$svc" > "aio-logs/image-$svc.log" 2>&1 &
    pids="$pids $!:$svc"
  done
  # set -e does not fire for a background job, so collect every exit status explicitly.
  img_rc=0
  for p in $pids; do
    wait "${p%%:*}" || {
      echo "::error::image build failed: ${p#*:}"
      tail -n 40 "aio-logs/image-${p#*:}.log" || true
      img_rc=1
    }
  done
  [ "$img_rc" -eq 0 ] || exit 1
  docker build -t local/aio:ci \
    --build-arg JANS_PERSISTENCE_LOADER_IMAGE=local/persistence-loader:ci \
    --build-arg JANS_CONFIG_API_IMAGE=local/config-api:ci \
    --build-arg JANS_AUTH_IMAGE=local/auth-server:ci \
    --build-arg JANS_SCIM_IMAGE=local/scim:ci \
    --build-arg JANS_FIDO2_IMAGE=local/fido2:ci \
    ./docker-jans-all-in-one
  base_image="local/aio:ci"
fi
# Bake the integration-test env into a thin layer, tagged as the image the AIO demo
# compose expects, so start_janssen_aio_demo.sh uses it unchanged.
cat > Dockerfile.ci-aio <<EOF
FROM ${base_image}
# info surfaces the reason for a 400 nginx raises itself; the buffers stop jans-auth's session
# headers overflowing them ("upstream sent too big header" -> 502). Defaults ship unchanged.
ENV CN_AIO_NGINX_LOG_LEVEL=info
ENV CN_AIO_NGINX_PROXY_BUFFER_SIZE=16k
ENV CN_AIO_NGINX_PROXY_BUFFERS="8 16k"
ENV CN_AIO_NGINX_PROXY_BUSY_BUFFERS_SIZE=32k
ENV CN_AIO_NGINX_LARGE_CLIENT_HEADER_BUFFERS="4 16k"
ENV CN_PERSISTENCE_LOAD_TEST_DATA=true
ENV CN_SCIM_ENABLED=true
ENV CN_CONFIG_API_TEST_CLIENT_ID=${CN_CONFIG_API_TEST_CLIENT_ID}
ENV CN_CONFIG_API_TEST_CLIENT_SECRET=${CN_CONFIG_API_TEST_CLIENT_SECRET}
ENV CN_CONFIG_API_TEST_CLIENT_TRUSTED=${CN_CONFIG_API_TEST_CLIENT_TRUSTED}
EOF
docker build -t "$AIO_IMAGE_TAG" - < Dockerfile.ci-aio
set +e
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Start the AIO demo stack (consul + vault + traefik + DB + AIO)
# ---------------------------------------------------------------------------
echo "::group::start AIO demo stack"
# TRACE (detailed FILE logs) is opt-in via LOG_LEVEL: the demo enables TRACE + FILE logging
# when JANS_CI_CD_RUN is set; default stays INFO/STDOUT (lower memory).
[ "${LOG_LEVEL:-INFO}" = "TRACE" ] && export JANS_CI_CD_RUN=true && echo "[info] AIO log level: TRACE/FILE" || true
# The demo default (768M) OOM-kills mysql under the test-data load; the CI VM has 16-32GB.
export MYSQL_MEM_LIMIT="${MYSQL_MEM_LIMIT:-3G}"
# Run the demo in the background and relax its mode-600 TLS certs as they appear, so the
# in-container configurator (uid 1000) reads ca.key/web_https.key on its FIRST run. A restart
# instead would re-run key-gen against a half-initialised keystore and corrupt jansConfWebKeys.
bash automation/start_janssen_aio_demo.sh "$JANS_FQDN" "$JANS_PERSISTENCE" "" 127.0.0.1 &
demo_pid=$!
for _ in $(seq 1 120); do
  [ -d automation/jans-aio-demo/templates ] && chmod -R a+rX automation/jans-aio-demo/templates 2>/dev/null || true
  kill -0 "$demo_pid" 2>/dev/null || break
  sleep 2
done
wait "$demo_pid" 2>/dev/null || echo "[warn] demo-script readiness gate did not pass; re-checking below"
end=$((SECONDS + 600)); ok=""; poll=0
while [ $SECONDS -lt $end ]; do
  code=$(curl -sk -o /dev/null -w '%{http_code}' "https://${JANS_FQDN}/.well-known/openid-configuration" || true)
  echo "openid-configuration: $code"
  if [ "$code" = "200" ]; then ok=1; break; fi
  poll=$((poll + 1))
  if [ $((poll % 6)) -eq 0 ]; then
    echo "--- jans-auth progress (poll ${poll}) ---"
    docker logs jans 2>&1 | grep -aiE "jans-auth -|configurator -|persistence-loader -|ERROR|Exception|Started oejs.Server|FATAL|exited|WaitError" | tail -n 10 || true
  fi
  sleep 20
done
if [ -z "$ok" ]; then
  echo "::error::AIO did not become healthy in time"
  docker compose -f automation/compose.yaml ps || true
  echo "--- DB container state + logs (mysql/postgresql often the cause of an UnknownHostException) ---"
  docker ps -a --format '{{.Names}} {{.Status}}' | grep -E 'mysql|postgresql' || true
  docker logs mysql 2>&1 | tail -n 80 || true
  docker logs postgresql 2>&1 | tail -n 80 || true
  docker exec jans supervisorctl -c /app/conf/supervisord.conf status 2>&1 || true
  docker logs jans 2>&1 | grep -aiE "jans-auth -|configurator -|persistence-loader -|ERROR|Exception|Traceback|Started oejs.Server|FATAL|exited|WaitError|OutOfMemory" | tail -n 200 || true
  echo "--- KEY MATERIAL DIAGNOSTIC (where does the jansConfWebKeys date come from?) ---"
  echo "[configurator /etc/certs/auth-keys.json head]"; docker exec jans sh -c 'head -c 400 /etc/certs/auth-keys.json' 2>&1 || true; echo
  docker exec jans python3 -c 'import base64; from jans.pycloudlib import get_manager; from jans.pycloudlib.persistence.sql import SqlClient, doc_id_from_dn; m=get_manager(); s=m.secret.get("auth_openid_key_base64") or ""; print("[secret auth_openid_key_base64 decoded :300]", (base64.b64decode(s).decode("utf-8","replace")[:300] if s else "EMPTY")); c=SqlClient(m); r=c.get("jansAppConf", doc_id_from_dn("ou=jans-auth,ou=configuration,o=jans"), ["jansConfWebKeys"]) or {}; print("[DB jansConfWebKeys :300]", str(r.get("jansConfWebKeys"))[:300])' 2>&1 || true
  exit 1
fi
for ep in scim-configuration fido2-configuration; do
  curl -sk -o /dev/null -w "$ep: %{http_code}\n" "https://${JANS_FQDN}/.well-known/$ep" || true
done
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Reload jans-scim (pick up test SCIM custom attributes)
# ---------------------------------------------------------------------------
echo "::group::reload jans-scim"
# jans-scim caches the SCIM extension at init; restart it so the loader's scimCustom*
# attributes are recognised (jans-linux-setup likewise restarts jans-scim after loading).
docker exec jans supervisorctl -c /app/conf/supervisord.conf restart jans-scim || true
for _ in $(seq 1 30); do
  code=$(curl -sk -o /dev/null -w "%{http_code}" "https://${JANS_FQDN}/.well-known/scim-configuration" || true)
  [ "$code" = "200" ] && { echo "jans-scim back up"; break; }
  sleep 4
done
# diagnostic: confirm the scimCustom* attrs in the DB and whether jans-scim built the extension
docker exec jans python3 -c 'from jans.pycloudlib import get_manager; from jans.pycloudlib.persistence.sql import SqlClient, doc_id_from_dn; c=SqlClient(get_manager()); [print("scim attr", i, c.get("jansAttr", doc_id_from_dn(f"inum={i},ou=attributes,o=jans"), ["jansAttrName","jansScimCustomAttr","jansStatus"])) for i in ("ADA6","70F0","653A")]' 2>&1 || true
curl -sk "https://${JANS_FQDN}/jans-scim/restv1/v2/Schemas" 2>/dev/null | grep -aoiE "scimCustom[A-Za-z]+|urn:[^\"]*extension[^\"]*User" | sort -u | head || echo "(SCIM /Schemas not readable without auth)"
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Import AIO CA certificate into the JDK truststore
# ---------------------------------------------------------------------------
echo "::group::import AIO CA"
set -e
# JAVA_HOME may be unset under a non-login SSH shell; resolve the JDK from the maven JVM.
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
export JAVA_HOME
"${JAVA_HOME}/bin/keytool" -import -trustcacerts -noprompt -alias "${JANS_FQDN}" \
  -file automation/jans-aio-demo/templates/ca.crt \
  -keystore "${JAVA_HOME}/lib/security/cacerts" -storepass changeit
set +e
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Extract live secrets from the AIO
# ---------------------------------------------------------------------------
echo "::group::extract live secrets"
set -e
# adapter-agnostic: read through the AIO's own pycloudlib manager
payload=$(docker exec -i -e DB_PASSWORD="$DB_PASSWORD" jans python3 - <<'PY'
import json
from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import encode_text
import os
m = get_manager()
salt = m.secret.get("encoded_salt")
print(json.dumps({
    "salt": salt,
    "scim_id": m.config.get("scim_client_id") or "",
    "scim_pw": m.secret.get("scim_client_pw") or "",
    "jca_id": m.config.get("jca_client_id") or "",
    "jca_enc": m.secret.get("jca_client_encoded_pw") or "",
    "jks_pass": m.secret.get("auth_openid_jks_pass") or m.config.get("auth_openid_jks_pass") or "",
    "db_pw_enc": encode_text(os.environ["DB_PASSWORD"], salt).decode(),
}))
PY
)
echo "$payload" > "$REPO_ROOT/aio-secrets.json"
set +e
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Render test profiles
# ---------------------------------------------------------------------------
echo "::group::render test profiles"
set -e
read_json() { python3 -c "import json;print(json.load(open('$REPO_ROOT/aio-secrets.json'))['$1'])"; }
export JANS_FQDN
export ENCODE_SALT="$(read_json salt)"
export SCIM_CLIENT_ID="$(read_json scim_id)"
export SCIM_CLIENT_PW="$(read_json scim_pw)"
export JCA_CLIENT_ID="$(read_json jca_id)"
export JCA_CLIENT_ENCODED_PW="$(read_json jca_enc)"
export JCA_TEST_CLIENT_ID="$CN_CONFIG_API_TEST_CLIENT_ID"
export JCA_TEST_CLIENT_SECRET="$CN_CONFIG_API_TEST_CLIENT_SECRET"
export RDBM_NAME_STR="$RDBM_JDBC"
export RDBM_DB="$DB_NAME"
export RDBM_SCHEMA_NAME="$RDBM_SCHEMA"
export RDBM_PORT="$RDBM_PORT"
export RDBM_USER="$DB_USER"
export RDBM_PASSWORD_ENC="$(read_json db_pw_enc)"
python3 .github/workflows/scripts/render_test_profiles.py
set +e
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Wire the auth-client tests to the AIO's own signing keys
# ---------------------------------------------------------------------------
# Client-signing tests register with clientJwksUri=<fqdn>/jans-auth-client/test/resources/jwks.json
# and sign with clientKeyStoreFile. The AIO doesn't serve that jwks path, so give the test client the
# AIO's own keystore and serve the AIO's public JWKS there (same keypair) via a traefik nginx sidecar.
echo "::group::wire test client to AIO keys"
set +e
JKS_PASS="$(read_json jks_pass)"
docker cp jans:/etc/certs/auth-keys.jks "$REPO_ROOT/aio-auth-keys.jks"
convert_ks() {  # AIO keystore (type ambiguous) -> PKCS12 with the template's "secret" password
  local dst="$1"; rm -f "$dst"; mkdir -p "$(dirname "$dst")"
  for st in pkcs12 JKS; do
    "${JAVA_HOME}/bin/keytool" -importkeystore -noprompt \
      -srckeystore "$REPO_ROOT/aio-auth-keys.jks" -srcstoretype "$st" -srcstorepass "$JKS_PASS" \
      -destkeystore "$dst" -deststoretype PKCS12 -deststorepass secret >/dev/null 2>&1 && return 0
    rm -f "$dst"
  done
  return 1
}
for prof in jans-auth-server/client/profiles jans-auth-server/server/profiles; do
  convert_ks "$REPO_ROOT/$prof/$JANS_FQDN/client_keystore.p12" \
    && echo "wrote AIO keystore -> $prof/$JANS_FQDN/client_keystore.p12" \
    || echo "[warn] keystore conversion failed for $prof"
done
# Serve the AIO's public JWKS (+ a sector_identifier) at the path the tests expect.
res="$REPO_ROOT/test-resources/jans-auth-client/test/resources"
mkdir -p "$res"
curl -sk "https://${JANS_FQDN}/jans-auth/restv1/jwks" -o "$res/jwks.json"
printf '["https://%s/jans-auth-rp/home.htm","https://client.example.com/cb","https://client.example.com/cb1","https://client.example.com/cb2","https://client.example.com/cb3"]\n' \
  "$JANS_FQDN" > "$res/sector_identifier.js"
NET=$(docker inspect traefik -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' | awk '{print $1}')
docker rm -f testres >/dev/null 2>&1 || true
docker run -d --name testres --network "$NET" \
  -v "$res:/usr/share/nginx/html/jans-auth-client/test/resources:ro" \
  --label "traefik.enable=true" \
  --label "traefik.http.routers.testres.rule=Host(\`${JANS_FQDN}\`) && PathPrefix(\`/jans-auth-client\`)" \
  --label "traefik.http.routers.testres.entrypoints=websecure" \
  --label "traefik.http.routers.testres.priority=5000" \
  --label "traefik.http.services.testres.loadbalancer.server.port=80" \
  nginx:alpine >/dev/null
jcode=""
for _ in $(seq 1 20); do
  jcode=$(curl -sk -o /dev/null -w '%{http_code}' "https://${JANS_FQDN}/jans-auth-client/test/resources/jwks.json" || true)
  [ "$jcode" = "200" ] && break; sleep 2
done
echo "served test jwks.json via traefik: $jcode"
set +e
echo "::endgroup::"

# Modules were already built + installed before the AIO image (see "build jans modules" above), so
# the per-suite `mvn test` below resolves them from the local repo without rebuilding.
mkdir -p test-reports aio-logs

# ---------------------------------------------------------------------------
# Run integration suites (against the AIO)
# ---------------------------------------------------------------------------
echo "::group::run integration suites"
# HTTP suites vs the live AIO; per-suite output -> aio-logs/ (the run log is too large to fetch).
# auth-client is the slowest (HtmlUnit browser flows), hence the generous timeout.
for entry in jans-scim:jans-scim/client jans-config-api:jans-config-api \
             jans-fido2:jans-fido2/client jans-auth-server:jans-auth-server/client; do
  mod="${entry%%:*}"; dir="${entry#*:}"
  want_module "$mod" || { echo "[info] skipping $dir ($mod not selected)"; continue; }
  echo "::group::test $dir"
  suitelog="aio-logs/test-$(printf '%s' "$dir" | tr / _).log"
  timeout -k 30 2400 bash -c \
    "cd '$dir' && mvn -B -ntp -s '$MVN_SETTINGS' -Dcfg='$JANS_FQDN' -DfailIfNoTests=false $MVN_SKIPS test" \
    > "$suitelog" 2>&1 || echo "[warn] $dir reported failures or timed out"
  echo "----- tail $suitelog -----"; tail -n 25 "$suitelog" 2>/dev/null || true
  echo "::endgroup::"
done
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Run unit suites
# ---------------------------------------------------------------------------
echo "::group::run unit suites"
# In-process unit suites (no live server); each hard-bounded with `timeout` as a safety net.
# UserJansExtUidAttributeTest is already baselined as known-failing and spends 303s timing out
# against an LDAP pool the AIO does not have; server-fips has no tests but adds ~6 min of compile.
OPTS="-B -ntp -s $MVN_SETTINGS -Dcfg=default -Dmaven.test.failure.ignore=true -DfailIfNoTests=false $MVN_SKIPS"
want_module jans-orm && timeout -k 30 600 mvn $OPTS -f jans-orm/pom.xml test > aio-logs/unit-jans-orm.log 2>&1 || echo "[warn/skip] jans-orm units"
want_module jans-core && timeout -k 30 600 mvn $OPTS -f jans-core/pom.xml test > aio-logs/unit-jans-core.log 2>&1 || echo "[warn/skip] jans-core units"
want_module jans-auth-server && timeout -k 30 600 mvn $OPTS -Dtest='!UserJansExtUidAttributeTest' -f jans-auth-server/pom.xml -pl model,common,server test > aio-logs/unit-jans-auth-server.log 2>&1 || echo "[warn/skip] jans-auth-server units"
want_module agama && timeout -k 30 600 mvn $OPTS -f agama/pom.xml test > aio-logs/unit-agama.log 2>&1 || echo "[warn/skip] agama units"
[ "$CED_READY" = 1 ] && want_module jans-cedarling && timeout -k 30 600 mvn $OPTS $CED_OPTS -f jans-cedarling/bindings/cedarling-java/pom.xml test > aio-logs/unit-cedarling-java.log 2>&1 || echo "[warn/skip] cedarling-java units"
[ "$CED_READY" = 1 ] && want_module jans-lock && timeout -k 30 600 mvn $OPTS $CED_OPTS $NO_FIPS -f jans-lock/lock-server/pom.xml test > aio-logs/unit-jans-lock.log 2>&1 || echo "[warn/skip] jans-lock units"
# fido2-server units: exclude the two *DeviceRegistration* TestNG tests (need an embedded Weld+DB
# harness that does not exist here) and the MDS test (hits mds3.fido.tools over the network).
want_module jans-fido2 && timeout -k 30 600 mvn $OPTS -Dtest='!Fido2DeviceRegistration*,!FetchMdsProviderServiceTest' -f jans-fido2/server/pom.xml test > aio-logs/unit-fido2-server.log 2>&1 || echo "[warn/skip] fido2-server units"
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Repack the build caches for the runner to save
# ---------------------------------------------------------------------------
# Only the leg the workflow elected to save bothers repacking; the sibling's copy is discarded.
if [ "${SAVE_CACHE:-1}" = 1 ]; then
  echo "::group::repack build caches"
  mkdir -p "$CACHE_DIR"
  rsync -a --delete --exclude 'io/jans' "$HOME/.m2/repository/" "$CACHE_DIR/m2/" 2>/dev/null || true
  # Registry + git sources only: ~/.cargo/bin is rustup's own install, re-fetched each run.
  rsync -a --include 'registry/***' --include 'git/***' --exclude '*' \
    "$HOME/.cargo/" "$CACHE_DIR/cargo/" 2>/dev/null || true
  # Deps alone should land near 3 GB; much larger means something unintended got in.
  sz=$(du -sm "$CACHE_DIR" 2>/dev/null | cut -f1)
  if [ -n "$sz" ] && [ "$sz" -gt 5000 ]; then
    echo "::warning::build cache is ${sz}MB, above the 5000MB guard -- check the per-component sizes below"
  fi
  du -sh "$CACHE_DIR"/* 2>/dev/null || true
  echo "::endgroup::"
else
  echo "[info] SAVE_CACHE=0; the sibling matrix leg repacks the build cache"
fi

# ---------------------------------------------------------------------------
# Collect surefire reports
# ---------------------------------------------------------------------------
echo "::group::collect surefire reports"
mkdir -p test-reports
# Sweep every reactor so auth-client + unit reports are captured (path-prefixed names).
find jans-orm jans-core jans-auth-server jans-scim jans-config-api jans-fido2 \
     agama jans-cedarling/bindings/cedarling-java jans-lock/lock-server \
  -path '*/target/surefire-reports/*.xml' 2>/dev/null | while read -r f; do
  mod=$(printf '%s' "$f" | sed -E 's#/target/surefire-reports/.*##; s#[/ ]+#_#g')
  cp "$f" "test-reports/${mod}-$(basename "$f")" 2>/dev/null || true
done
echo "collected $(find test-reports -name '*.xml' 2>/dev/null | wc -l) report files"
echo "::endgroup::"

# ---------------------------------------------------------------------------
# Collect AIO logs
# ---------------------------------------------------------------------------
echo "::group::collect AIO logs"
mkdir -p aio-logs
# container log carries STDOUT/INFO output (incl. configurator + persistence-loader)
docker logs jans > aio-logs/aio-container.log 2>&1 || true
docker logs traefik > aio-logs/traefik.log 2>&1 || true
docker exec jans supervisorctl -c /app/conf/supervisord.conf status > aio-logs/supervisord-status.txt 2>&1 || true
docker compose -f automation/compose.yaml ps > aio-logs/compose-ps.txt 2>&1 || true
# per-service jetty logs (where FILE/TRACE output lands)
for s in jans-auth jans-config-api jans-scim jans-fido2 jans-casa; do
  docker cp "jans:/opt/jans/jetty/$s/logs" "aio-logs/$s" 2>/dev/null || true
done
echo "::endgroup::"

echo "[info] run_aio_integration.sh complete"
