FROM alpine:3.11

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache py3-pip curl tini \
    && apk add --no-cache --virtual build-deps git wget

# ======
# Python
# ======

RUN apk add --no-cache py3-cryptography py3-multidict py3-yarl
COPY requirements.txt /app/requirements.txt
RUN pip3 install -U pip \
    && pip3 install --no-cache-dir -r /app/requirements.txt \
    && rm -rf /src/jans-pycloudlib/.git

# =======
# Cleanup
# =======

RUN apk del build-deps \
    && rm -rf /var/cache/apk/*

# =======
# License
# =======

RUN mkdir -p /licenses
COPY LICENSE /licenses/

# ==========
# Config ENV
# ==========

ENV JANS_CONFIG_ADAPTER=consul \
    JANS_CONFIG_CONSUL_HOST=localhost \
    JANS_CONFIG_CONSUL_PORT=8500 \
    JANS_CONFIG_CONSUL_CONSISTENCY=stale \
    JANS_CONFIG_CONSUL_SCHEME=http \
    JANS_CONFIG_CONSUL_VERIFY=false \
    JANS_CONFIG_CONSUL_CACERT_FILE=/etc/certs/consul_ca.crt \
    JANS_CONFIG_CONSUL_CERT_FILE=/etc/certs/consul_client.crt \
    JANS_CONFIG_CONSUL_KEY_FILE=/etc/certs/consul_client.key \
    JANS_CONFIG_CONSUL_TOKEN_FILE=/etc/certs/consul_token \
    JANS_CONFIG_KUBERNETES_NAMESPACE=default \
    JANS_CONFIG_KUBERNETES_CONFIGMAP=gluu \
    JANS_CONFIG_KUBERNETES_USE_KUBE_CONFIG=false

# ==========
# Secret ENV
# ==========

ENV JANS_SECRET_ADAPTER=vault \
    JANS_SECRET_VAULT_SCHEME=http \
    JANS_SECRET_VAULT_HOST=localhost \
    JANS_SECRET_VAULT_PORT=8200 \
    JANS_SECRET_VAULT_VERIFY=false \
    JANS_SECRET_VAULT_ROLE_ID_FILE=/etc/certs/vault_role_id \
    JANS_SECRET_VAULT_SECRET_ID_FILE=/etc/certs/vault_secret_id \
    JANS_SECRET_VAULT_CERT_FILE=/etc/certs/vault_client.crt \
    JANS_SECRET_VAULT_KEY_FILE=/etc/certs/vault_client.key \
    JANS_SECRET_VAULT_CACERT_FILE=/etc/certs/vault_ca.crt \
    JANS_SECRET_KUBERNETES_NAMESPACE=default \
    JANS_SECRET_KUBERNETES_SECRET=gluu \
    JANS_SECRET_KUBERNETES_USE_KUBE_CONFIG=false

# ===============
# Persistence ENV
# ===============

ENV JANS_PERSISTENCE_TYPE=couchbase \
    JANS_PERSISTENCE_LDAP_MAPPING=default \
    JANS_COUCHBASE_URL=localhost \
    JANS_COUCHBASE_USER=admin \
    JANS_COUCHBASE_CERT_FILE=/etc/certs/couchbase.crt \
    JANS_COUCHBASE_PASSWORD_FILE=/etc/gluu/conf/couchbase_password \
    JANS_COUCHBASE_SUPERUSER="" \
    JANS_COUCHBASE_SUPERUSER_PASSWORD_FILE=/etc/gluu/conf/couchbase_superuser_password \
    JANS_LDAP_URL=localhost:1636

# ===========
# Generic ENV
# ===========

ENV JANS_CACHE_TYPE=NATIVE_PERSISTENCE \
    JANS_REDIS_URL=localhost:6379 \
    JANS_REDIS_TYPE=STANDALONE \
    JANS_REDIS_USE_SSL=false \
    JANS_REDIS_SSL_TRUSTSTORE="" \
    JANS_REDIS_SENTINEL_GROUP="" \
    JANS_MEMCACHED_URL=localhost:11211 \
    JANS_WAIT_SLEEP_DURATION=10 \
    JANS_OXTRUST_API_ENABLED=false \
    JANS_OXTRUST_API_TEST_MODE=false \
    JANS_CASA_ENABLED=false \
    JANS_PASSPORT_ENABLED=false \
    JANS_RADIUS_ENABLED=false \
    JANS_SAML_ENABLED=false \
    JANS_SCIM_ENABLED=false \
    JANS_SCIM_TEST_MODE=false \
    JANS_PERSISTENCE_SKIP_EXISTING=true \
    JANS_DOCUMENT_STORE_TYPE=LOCAL \
    JANS_JACKRABBIT_RMI_URL="" \
    JANS_JACKRABBIT_URL=http://localhost:8080 \
    JANS_JACKRABBIT_ADMIN_ID_FILE=/etc/gluu/conf/jackrabbit_admin_id \
    JANS_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/gluu/conf/jackrabbit_admin_password

# ====
# misc
# ====

LABEL name="Persistence" \
    maintainer="Janssen <support@jans.io>" \
    vendor="Janssen Project" \
    version="5.0.0" \
    release="dev" \
    summary="Janssen Authorization Server Persistence loader" \
    description="Generate initial data for persistence layer"

RUN mkdir -p /app/tmp /etc/certs /etc/gluu/conf

COPY scripts /app/scripts
COPY static /app/static
COPY templates /app/templates
RUN chmod +x /app/scripts/entrypoint.sh

ENTRYPOINT ["tini", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
