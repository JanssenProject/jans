#!/usr/bin/env bash

set -eo pipefail

# get script directory
basedir=$(dirname "$(readlink -f -- "$0")")

demo_dir="$basedir/jans-aio-demo"
demo_templates_dir="$demo_dir/templates"
demo_volumes_dir="$demo_dir/volumes"

prepare_dirs() {
    mkdir -p "$demo_dir"
    mkdir -p "$demo_templates_dir"
    touch "$demo_templates_dir/vault_role_id"
    touch "$demo_templates_dir/vault_secret_id"
    mkdir -p "$demo_volumes_dir"
}

prepare_certs() {
    echo "[I] Generating self-signed certificates"

    fqdn=$1

    if [[ ! -f "$demo_templates_dir/ca.key" ]]; then
        openssl genrsa -out "$demo_templates_dir/ca.key" 4096
        if [[ "$(id -u)" -eq 0 ]]; then
            chown 1000:1000 "$demo_templates_dir/ca.key"
        fi
    fi

    if [[ ! -f "$demo_templates_dir/ca.crt" ]]; then
        openssl req -x509 -new -nodes -key "$demo_templates_dir/ca.key" -sha256 -days 3650 -out "$demo_templates_dir/ca.crt" \
            -subj '/CN=Root CA/C=US/ST=TX/L=Austin/O=Gluu'
    fi

    if [[ ! -f "$demo_templates_dir/web_https.key" ]]; then
        openssl genrsa -out "$demo_templates_dir/web_https.key" 2048
        if [[ "$(id -u)" -eq 0 ]]; then
            chown 1000:1000 "$demo_templates_dir/web_https.key"
        fi
    fi

    if [[ ! -f "$demo_templates_dir/web_https.csr" ]]; then
        openssl req -new -key "$demo_templates_dir/web_https.key" -out "$demo_templates_dir/web_https.csr" \
            -subj "/CN=$fqdn/C=US/ST=TX/L=Austin/O=Gluu"
    fi

    if [[ ! -f "$demo_templates_dir/web_https.crt" ]]; then
        cat > "$demo_templates_dir/web_https.v3.ext" << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = $fqdn
EOF

        openssl x509 -req -in "$demo_templates_dir/web_https.csr" \
            -CA "$demo_templates_dir/ca.crt" -CAkey "$demo_templates_dir/ca.key" -CAcreateserial \
            -out "$demo_templates_dir/web_https.crt" -days 3650 -sha256 -extfile "$demo_templates_dir/web_https.v3.ext"

        rm -f "$demo_templates_dir/web_https.v3.ext"
    fi
}

prepare_vault_files() {
    echo "[I] Preparing files for Vault"

    if [[ ! -f "$demo_templates_dir/vault_policy.hcl" ]]; then
        cat > "$demo_templates_dir/vault_policy.hcl" << EOF
path "secret/jans/*" {
    capabilities = ["create", "list", "read", "delete", "update"]
}
EOF
    fi

    if [[ ! -f "$demo_templates_dir/vault-entrypoint.sh" ]]; then
        cat > "$demo_templates_dir/vault-entrypoint.sh" << "EOF"
#!/usr/bin/env sh

set -e

# Delegate to original entrypoint
exec /usr/local/bin/docker-entrypoint.sh "$@" &
VAULT_PID=$!

echo "[I] Configuring Vault"

vault_status=""

retries=1
while [ "$retries" -le 5 ] ;do
    vault_status=$(vault status 2>/dev/null ||:)
    if [ -n "$vault_status" ]; then
        break
    fi

    echo "[W] Unable to get seal status in Vault; retrying ..."
    retries=$((retries+1))
    sleep 5
done

if [ "$(echo "$vault_status" | grep 'Initialized' | awk -F " " '{print $2}')" = "false" ]; then
    echo "[I] Initializing Vault with 1 recovery key and token"
    vault operator init -key-shares=1 -key-threshold=1 > /vault/config/vault_key_token.txt
    sleep 1
fi

if [ "$(echo "$vault_status" | grep 'Sealed' | awk -F " " '{print $2}')" = "true" ]; then
    echo "[I] Unsealing Vault"
    vault_unseal_key=$(grep 'Unseal Key 1' /vault/config/vault_key_token.txt | awk -F ": " '{print $2}')
    vault operator unseal "$vault_unseal_key"
    sleep 1
fi

# do login to enable secrets
vault_token=$(grep 'Initial Root Token' /vault/config/vault_key_token.txt | awk -F ": " '{print $2}')
# probably logged in already
vault login -no-print "$vault_token" 2>/dev/null ||:
sleep 1

vault_engines=$(vault secrets list 2>/dev/null ||:)
if [ -n "$vault_engines" ]; then
    if [ "$(echo "$vault_engines" | grep 'secret/')" = "" ]; then
        echo "[I] Enabling Vault KV v1"
        vault secrets enable -path=secret -version=1 kv
        sleep 1
    fi
fi

vault_policies=$(vault policy list 2>/dev/null ||:)
if [ -n "$vault_policies" ]; then
    if [ "$(echo "$vault_policies" | grep 'jans')" = "" ]; then
        echo "[I] Creating Vault policy for Janssen"
        vault policy write jans /vault/config/vault_policy.hcl
        sleep 1
    fi
fi

vault_auths=$(vault auth list 2>/dev/null ||:)
if [ -n "$vault_auths" ]; then
    if [ "$(echo "$vault_auths" | grep 'approle/')" = "" ]; then
        vault auth enable approle
        vault write auth/approle/role/jans \
            policies=jans secret_id_ttl=0 token_num_uses=0 token_ttl=20m token_max_ttl=30m secret_id_num_uses=0
        sleep 1
    fi
fi

role_id=$(vault read -field=role_id auth/approle/role/jans/role-id 2>/dev/null ||:)
if [ -n "$role_id" ]; then
    echo "$role_id" > /vault/config/vault_role_id
fi

secret_id=$(vault write -f -field=secret_id auth/approle/role/jans/secret-id 2>/dev/null ||:)
if [ -n "$secret_id" ]; then
    echo "$secret_id" > /vault/config/vault_secret_id
fi

echo "[I] Vault configured"
wait $VAULT_PID
EOF
    fi
}

prepare_traefik_files() {
    echo "[I] Preparing files for Traefik"

    if [[ ! -f "$demo_templates_dir/traefik-tls.yaml" ]]; then
        cat > "$demo_templates_dir/traefik-tls.yaml" << EOF
tls:
  stores:
    default:
      defaultCertificate:
        certFile: /etc/certs/web_https.crt
        keyFile: /etc/certs/web_https.key
  certificates:
    - certFile: /etc/certs/web_https.crt
      keyFile: /etc/certs/web_https.key
EOF
    fi
}

prepare_compose_files() {
    echo "[I] Preparing docker compose file"

    fqdn=$1
    persistence_type=$2
    image_version=$3
    ipaddr=$4
    log_target=$5
    log_level=$6

    if [[ "$persistence_type" == "MYSQL" ]]; then
        db_port=3306
        db_host=mysql
        db_dialect=mysql
    else
        db_port=5432
        db_host=postgresql
        db_dialect=pgsql
    fi

    cat > "$basedir/compose.yaml" << EOF
networks:
  default:
    name: jans-aio-demo
services:
  consul:
    image: hashicorp/consul:1.22
    command: agent -server -bootstrap -ui
    container_name: consul
    environment:
      - CONSUL_BIND_INTERFACE=eth0
      - CONSUL_CLIENT_INTERFACE=eth0
      - CONSUL_HTTP_ADDR=http://consul:8500
    volumes:
      - $demo_volumes_dir/consul:/consul/data
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 128M
    ports:
      - "127.0.0.1:8500:8500"

  vault:
    image: hashicorp/vault:1.21
    command: vault server -config=/vault/config
    entrypoint: ["sh", "/docker-entrypoint.sh"]
    container_name: vault
    volumes:
      - $demo_volumes_dir/vault/config:/vault/config
      - $demo_volumes_dir/vault/data:/vault/data
      - $demo_volumes_dir/vault/logs:/vault/logs
      - $demo_templates_dir/vault_policy.hcl:/vault/config/vault_policy.hcl
      - $demo_templates_dir/vault-entrypoint.sh:/docker-entrypoint.sh
      - $demo_templates_dir/vault_role_id:/vault/config/vault_role_id
      - $demo_templates_dir/vault_secret_id:/vault/config/vault_secret_id
    cap_add:
      - IPC_LOCK
    environment:
      - VAULT_REDIRECT_INTERFACE=eth0
      - VAULT_CLUSTER_INTERFACE=eth0
      - VAULT_ADDR=http://0.0.0.0:8200
      - VAULT_LOCAL_CONFIG={"backend":{"consul":{"address":"consul:8500","path":"vault/"}},"listener":{"tcp":{"address":"0.0.0.0:8200","tls_disable":1}},"ui":true}
    restart: unless-stopped
    depends_on:
      - consul
    deploy:
      resources:
        limits:
          memory: 768M
    ports:
      - "127.0.0.1:8200:8200"

  traefik:
    image: traefik:v3.6.1
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--providers.file.directory=/etc/traefik/conf.d"
      - "--providers.file.watch=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.web.http.redirections.entryPoint.to=websecure"
      - "--entrypoints.web.http.redirections.entryPoint.scheme=https"
      - "--entrypoints.websecure.address=:443"
      - "--entrypoints.websecure.http.tls=true"
      - "--log.level=warn"
      - "--log.format=json"
      - "--accessLog=true"
      - "--accessLog.format=json"
    container_name: traefik
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 128M
    ports:
      - "80:80"
      - "443:443"
      - "127.0.0.1:8090:8080" # UI
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - $demo_templates_dir/traefik-tls.yaml:/etc/traefik/conf.d/traefik-tls.yaml
      - $demo_templates_dir/web_https.crt:/etc/certs/web_https.crt
      - $demo_templates_dir/web_https.key:/etc/certs/web_https.key

EOF

    if [[ "$persistence_type" == "MYSQL" ]]; then
        cat >> "$basedir/compose.yaml" << EOF
  mysql:
    image: mysql:9.5.0
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --bind-address=0.0.0.0
    container_name: mysql
    environment:
      - MYSQL_ROOT_PASSWORD=Test1234#
      - MYSQL_USER=jans
      - MYSQL_PASSWORD=Test1234#
      - MYSQL_DATABASE=jans
    volumes:
      - $demo_volumes_dir/mysql:/var/lib/mysql
    cap_add:
      - SYS_NICE
    deploy:
      resources:
        limits:
          memory: 768M
    ports:
      - "127.0.0.1:3306:3306"

EOF
    else
        cat >> "$basedir/compose.yaml" << EOF
  postgresql:
    image: postgres:14
    container_name: postgresql
    environment:
      - POSTGRES_USER=jans
      - POSTGRES_PASSWORD=Test1234#
      - POSTGRES_DB=jans
    volumes:
      - $demo_volumes_dir/postgresql:/var/lib/postgresql/data
    deploy:
      resources:
        limits:
          memory: 768M
    ports:
      - "127.0.0.1:5432:5432"

EOF
    fi

    cat >> "$basedir/compose.yaml" << EOF
  jans:
    image: ghcr.io/janssenproject/jans/all-in-one:$image_version
    container_name: jans
    extra_hosts:
      - "$fqdn:$ipaddr"
    environment:
      - CN_CONFIG_CONSUL_HOST=consul
      - CN_CONFIG_CONSUL_NAMESPACE=jans
      - CN_SECRET_VAULT_ADDR=http://vault:8200
      - CN_SECRET_VAULT_PREFIX=jans
      - CN_PERSISTENCE_TYPE=sql
      - CN_SQL_DB_HOST=$db_host
      - CN_SQL_DB_PORT=$db_port
      - CN_SQL_DB_NAME=jans
      - CN_SQL_DB_USER=jans
      - CN_SQL_DB_DIALECT=$db_dialect
      - CN_AUTH_APP_LOGGERS={"auth_log_target":"$log_target","auth_log_level":"$log_level"}
      - CN_CASA_APP_LOGGERS={"casa_log_target":"$log_target","casa_log_level":"$log_level"}
      - CN_CONFIG_API_APP_LOGGERS={"config_api_log_target":"$log_target","config_api_log_level":"$log_level"}
      - CN_FIDO2_APP_LOGGERS={"fido2_log_target":"$log_target","fido2_log_level":"$log_level"}
      - CN_SCIM_APP_LOGGERS={"scim_log_target":"$log_target","scim_log_level":"$log_level"}
      - CN_LOCK_ENABLED=true
      - CN_CONFIG_API_PLUGINS=scim,fido2,user-mgt,lock
      - CN_AIO_COMPONENTS=configurator,persistence-loader,jans-auth,jans-config-api,jans-fido2,jans-scim,jans-casa
    volumes:
      - $demo_templates_dir/configuration.json:/etc/jans/conf/configuration.json:ro
      - $demo_templates_dir/ca.crt:/opt/jans/configurator/certs/ca.crt
      - $demo_templates_dir/ca.key:/opt/jans/configurator/certs/ca.key
      - $demo_templates_dir/web_https.crt:/opt/jans/configurator/certs/web_https.crt
      - $demo_templates_dir/web_https.key:/opt/jans/configurator/certs/web_https.key
      - $demo_templates_dir/web_https.csr:/opt/jans/configurator/certs/web_https.csr
      - $demo_templates_dir/vault_role_id:/etc/certs/vault_role_id
      - $demo_templates_dir/vault_secret_id:/etc/certs/vault_secret_id
    depends_on:
      - consul
      - vault
    deploy:
      resources:
        limits:
          memory: 6G
    healthcheck:
      test: ["CMD", "python3", "/app/jans_aio/jans_auth/healthcheck.py"]
      interval: 30s
      timeout: 20s
      retries: 5
      start_period: 30s
    labels:
      # custom-label for app discovery (e.g. key rotation push)
      - "APP_NAME=auth-server"
      # traefik
      - "traefik.enable=true"
      # routers
      - "traefik.http.routers.aio.rule=Host(\`$fqdn\`) && PathPrefix(\`/\`)"
      - "traefik.http.routers.aio.entrypoints=websecure"
      # healthcheck
      - "traefik.http.services.aio.loadBalancer.healthCheck.path=/jans-auth/sys/health-check"
      - "traefik.http.services.aio.loadBalancer.healthCheck.interval=30s"
      - "traefik.http.services.aio.loadBalancer.healthCheck.timeout=20s"

EOF
}

prepare_jans_configuration() {
    echo "[I] Preparing Janssen configuration file"

    fqdn=$1
    ssl_ca_cert=""
    ssl_ca_key=""
    ssl_cert=""
    ssl_key=""
    ssl_csr=""

    if [[ -f "$demo_templates_dir/ca.crt" ]]; then
        ssl_ca_cert=$(base64 -w0 "$demo_templates_dir/ca.crt")
    fi

    if [[ -f "$demo_templates_dir/ca.key" ]]; then
        ssl_ca_key=$(base64 -w0 "$demo_templates_dir/ca.key")
    fi

    if [[ -f "$demo_templates_dir/web_https.crt" ]]; then
        ssl_cert=$(base64 -w0 "$demo_templates_dir/web_https.crt")
    fi

    if [[ -f "$demo_templates_dir/web_https.key" ]]; then
        ssl_key=$(base64 -w0 "$demo_templates_dir/web_https.key")
    fi

    if [[ -f "$demo_templates_dir/web_https.csr" ]]; then
        ssl_csr=$(base64 -w0 "$demo_templates_dir/web_https.csr")
    fi


    cat > "$demo_templates_dir/configuration.json" << EOF
{
    "_secret": {
        "admin_password": "Test1234#",
        "sql_password": "Test1234#",
        "ssl_ca_cert": "$ssl_ca_cert",
        "ssl_ca_key": "$ssl_ca_key",
        "ssl_cert": "$ssl_cert",
        "ssl_key": "$ssl_key",
        "ssl_csr": "$ssl_csr"
    },
    "_configmap": {
        "city": "Austin",
        "country_code": "US",
        "admin_email": "support@$fqdn",
        "hostname": "$fqdn",
        "state": "TX",
        "orgName": "Gluu",
        "optional_scopes": "[\"sql\"]"
    }
}
EOF
}

install_docker() {
    echo "[I] Checking docker"

    docker_exists=$(type docker 2>/dev/null ||:)

    if [[ -z "$docker_exists" ]]; then
        echo "[W] docker doesn't exist; installing ..."

        # setup docker repository
        sudo apt remove $(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc | cut -f1)
        sudo apt update
        sudo apt install -y ca-certificates curl
        sudo install -m 0755 -d /etc/apt/keyrings
        sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
        sudo chmod a+r /etc/apt/keyrings/docker.asc

        sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

        # install docker packages
        sudo apt update
        sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    else
        echo "[I] docker is available"
    fi
}

check_jans_readiness() {
    cid=""
    retries=1

    echo "[I] Waiting 120 seconds for services to initialize before starting health checks. Hang on..."
    sleep 120

    while [[ "$retries" -le 20 ]]; do
        cid=$(docker ps --filter network=jans-aio-demo --filter name=jans --filter health=healthy -q ||:)
        if [[ -n "$cid" ]]; then
            echo "[I] Janssen is ready to accept request"
            break
        else
            echo "[W] Janssen is not ready yet; retrying in 10 seconds ..."
            retries=$((retries+1))
            sleep 10
        fi
    done

    if [[ -z "$cid" ]]; then
        echo "[W] Janssen unable to accept request after 20 retries, please check the logs for details"
        exit 1
    fi
}

# ===============
# main entrypoint
# ===============

JANS_FQDN=$1
JANS_PERSISTENCE=$2
JANS_VERSION=$3
EXT_IP=$4
JANS_CI_CD_RUN=$5

if [[ ! "$JANS_FQDN" ]]; then
    read -rp "Enter Hostname [demoexample.jans.io]: " JANS_FQDN
fi

if ! [[ $JANS_FQDN == *"."*"."* ]]; then
    echo "[E] Hostname provided is invalid or empty. Please enter a FQDN with the format demoexample.jans.io"
    exit 1
fi

if [[ ! "$JANS_PERSISTENCE" ]]; then
    read -rp "Enter persistence type [MYSQL|PGSQL]: " JANS_PERSISTENCE
fi

if [[ $JANS_PERSISTENCE != "MYSQL" ]] && [[ $JANS_PERSISTENCE != "PGSQL" ]]; then
    echo "[E] Incorrect entry. Please enter either MYSQL or PGSQL"
    exit 1
fi

if [[ -z $JANS_VERSION ]]; then
    JANS_VERSION="0.0.0-nightly"
fi

LOG_TARGET="FILE"
LOG_LEVEL="TRACE"

if [[ -z $JANS_CI_CD_RUN ]]; then
    LOG_TARGET="STDOUT"
    LOG_LEVEL="INFO"
fi

if [[ -z $EXT_IP  ]]; then
    EXT_IP=$(curl --silent --max-time 10 ipinfo.io/ip || true)
    if [[ -z "$EXT_IP" ]]; then
        echo "[E] Unable to determine external IP. Please provide it as the 4th argument."
        exit 1
    fi
fi

install_docker
prepare_dirs
prepare_certs "$JANS_FQDN"
prepare_vault_files
prepare_traefik_files
prepare_jans_configuration "$JANS_FQDN"
prepare_compose_files "$JANS_FQDN" "$JANS_PERSISTENCE" "$JANS_VERSION" "$EXT_IP" "$LOG_TARGET" "$LOG_LEVEL"

docker compose -f "$basedir/compose.yaml" up -d
echo "[I] Janssen is starting up!"
echo "[I] To check the progress, run 'docker compose -f $basedir/compose.yaml logs -f' in separate terminal"
echo "[I] Checking if Janssen is ready to accept requests (expected time ~3–5 minutes) ..."
check_jans_readiness
