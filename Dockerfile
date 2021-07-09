FROM alpine:3.13

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache py3-pip curl tini py3-cryptography py3-grpcio py3-psycopg2 \
    && apk add --no-cache --virtual build-deps git

# ======
# Python
# ======

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

ENV CN_CONFIG_ADAPTER=consul \
    CN_CONFIG_CONSUL_HOST=localhost \
    CN_CONFIG_CONSUL_PORT=8500 \
    CN_CONFIG_CONSUL_CONSISTENCY=stale \
    CN_CONFIG_CONSUL_SCHEME=http \
    CN_CONFIG_CONSUL_VERIFY=false \
    CN_CONFIG_CONSUL_CACERT_FILE=/etc/certs/consul_ca.crt \
    CN_CONFIG_CONSUL_CERT_FILE=/etc/certs/consul_client.crt \
    CN_CONFIG_CONSUL_KEY_FILE=/etc/certs/consul_client.key \
    CN_CONFIG_CONSUL_TOKEN_FILE=/etc/certs/consul_token \
    CN_CONFIG_KUBERNETES_NAMESPACE=default \
    CN_CONFIG_KUBERNETES_CONFIGMAP=jans \
    CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG=false \
    CN_CONFIG_GOOGLE_SECRET_VERSION_ID=latest \
    CN_CONFIG_GOOGLE_SECRET_NAME_PREFIX=jans

# ==========
# Secret ENV
# ==========

ENV CN_SECRET_ADAPTER=vault \
    CN_SECRET_VAULT_SCHEME=http \
    CN_SECRET_VAULT_HOST=localhost \
    CN_SECRET_VAULT_PORT=8200 \
    CN_SECRET_VAULT_VERIFY=false \
    CN_SECRET_VAULT_ROLE_ID_FILE=/etc/certs/vault_role_id \
    CN_SECRET_VAULT_SECRET_ID_FILE=/etc/certs/vault_secret_id \
    CN_SECRET_VAULT_CERT_FILE=/etc/certs/vault_client.crt \
    CN_SECRET_VAULT_KEY_FILE=/etc/certs/vault_client.key \
    CN_SECRET_VAULT_CACERT_FILE=/etc/certs/vault_ca.crt \
    CN_SECRET_KUBERNETES_NAMESPACE=default \
    CN_SECRET_KUBERNETES_SECRET=jans \
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false \
    CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_SECRET_GOOGLE_SECRET_VERSION_ID=latest \
    CN_SECRET_GOOGLE_SECRET_NAME_PREFIX=jans


# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=couchbase \
    CN_PERSISTENCE_LDAP_MAPPING=default \
    CN_COUCHBASE_URL=localhost \
    CN_COUCHBASE_USER=admin \
    CN_COUCHBASE_CERT_FILE=/etc/certs/couchbase.crt \
    CN_COUCHBASE_PASSWORD_FILE=/etc/jans/conf/couchbase_password \
    CN_COUCHBASE_SUPERUSER="" \
    CN_COUCHBASE_SUPERUSER_PASSWORD_FILE=/etc/jans/conf/couchbase_superuser_password \
    CN_COUCHBASE_INDEX_NUM_REPLICA=0 \
    CN_LDAP_URL=localhost:1636 \
    CN_LDAP_USE_SSL=true \
    CN_GOOGLE_SPANNER_INSTANCE_ID="" \
    CN_GOOGLE_SPANNER_DATABASE_ID=""

# ===========
# Generic ENV
# ===========

ENV CN_CACHE_TYPE=NATIVE_PERSISTENCE \
    CN_REDIS_URL=localhost:6379 \
    CN_REDIS_TYPE=STANDALONE \
    CN_REDIS_USE_SSL=false \
    CN_REDIS_SSL_TRUSTSTORE="" \
    CN_REDIS_SENTINEL_GROUP="" \
    CN_MEMCACHED_URL=localhost:11211 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_CASA_ENABLED=false \
    CN_PASSPORT_ENABLED=false \
    CN_RADIUS_ENABLED=false \
    CN_SAML_ENABLED=false \
    CN_SCIM_ENABLED=false \
    CN_PERSISTENCE_SKIP_INITIALIZED=false \
    CN_DOCUMENT_STORE_TYPE=LOCAL \
    CN_JACKRABBIT_RMI_URL="" \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID_FILE=/etc/jans/conf/jackrabbit_admin_id \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json \
    CN_AUTH_SERVER_URL="" \
    CN_CONFIG_API_APPROVED_ISSUER=""

# ====
# misc
# ====

LABEL name="Persistence" \
    maintainer="Janssen <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.0" \
    release="b7" \
    summary="Janssen Authorization Server Persistence loader" \
    description="Generate initial data for persistence layer"

RUN mkdir -p /app/tmp /etc/certs /etc/jans/conf

COPY scripts /app/scripts
COPY static /app/static
COPY templates /app/templates
RUN chmod +x /app/scripts/entrypoint.sh

# # create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

 # adjust ownership
RUN chown -R 1000:1000 /tmp \
    && chown -R 1000:1000 /app/tmp/ \
    && chgrp -R 0 /tmp && chmod -R g=u /tmp \
    && chgrp -R 0 /app/tmp && chmod -R g=u /app/tmp \
    && chgrp -R 0 /etc/certs && chmod -R g=u /etc/certs \
    && chgrp -R 0 /etc/jans && chmod -R g=u /etc/jans
USER 1000
ENTRYPOINT ["tini", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
