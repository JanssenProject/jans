FROM alpine:3.16

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache python3 curl tini py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps git wget

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools \
    && pip3 install --no-cache-dir --default-timeout=300 -r /app/requirements.txt \
    && pip3 uninstall -y pip wheel

# =====================
# jans-linux-setup sync
# =====================

# janssenproject/jans SHA commit
ENV JANS_SOURCE_VERSION=438192893dc3e091de60fc711dd219054dad374a
ARG JANS_SETUP_DIR=jans-linux-setup/jans_setup
ARG JANS_SCRIPT_CATALOG_DIR=docs/script-catalog
ARG JANS_CONFIG_API_RESOURCES=jans-config-api/server/src/main/resources

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the jans-linux-setup code
RUN git clone --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add ${JANS_SETUP_DIR} \
    && git sparse-checkout add ${JANS_SCRIPT_CATALOG_DIR} \
    && git sparse-checkout add ${JANS_CONFIG_API_RESOURCES}

RUN mkdir -p /app/static /app/static/couchbase /app/schema /app/openbanking/static /app/static/opendj /app/templates

# sync static files from linux-setup
RUN cd /tmp/jans \
    && cp -R ${JANS_SETUP_DIR}/static/extension /app/static/extension \
    && cp -R ${JANS_SETUP_DIR}/openbanking/static/extension /app/openbanking/static/extension \
    && cp ${JANS_SETUP_DIR}/static/couchbase/index.json /app/static/couchbase/index.json \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/opendj_types.json \
    && cp -R ${JANS_SETUP_DIR}/static/rdbm /app/static/rdbm \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/jans_schema.json \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/custom_schema.json \
    && cp ${JANS_SETUP_DIR}/static/opendj/index.json /app/static/opendj/index.json \
    && cp -R ${JANS_SCRIPT_CATALOG_DIR} /app/script-catalog \
    && cp ${JANS_SETUP_DIR}/static/extension/introspection/introspection_role_based_scope.py /app/openbanking/static/extension/introspection/ \
    && cp ${JANS_CONFIG_API_RESOURCES}/config-api-rs-protect.json -P /app/static/ \
    && cp ${JANS_SETUP_DIR}/templates/*.ldif /app/templates \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-auth /app/templates/jans-auth \
    && cp ${JANS_SETUP_DIR}/openbanking/templates/scopes.ldif /app/templates/scopes.ob.ldif \
    && cp ${JANS_SETUP_DIR}/openbanking/templates/attributes.ldif /app/templates/attributes.ob.ldif \
    && cp ${JANS_SETUP_DIR}/static/metric/o_metric.ldif /app/templates/o_metric.ldif \
    && cp ${JANS_SETUP_DIR}/static/cache-refresh/o_site.ldif /app/templates/o_site.ldif \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-fido2 /app/templates/jans-fido2 \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-scim /app/templates/jans-scim \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-cli /app/templates/jans-cli

# cleanup
RUN rm -rf /tmp/jans

# =======
# Cleanup
# =======

RUN apk del .build-deps \
    && rm -rf /var/cache/apk/*

# =======
# License
# =======

COPY LICENSE /licenses/LICENSE

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
    CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG=false

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
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false


# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=couchbase \
    CN_HYBRID_MAPPING="{}" \
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
    CN_SCIM_ENABLED=false \
    CN_PERSISTENCE_SKIP_INITIALIZED=false \
    CN_DOCUMENT_STORE_TYPE=LOCAL \
    CN_JACKRABBIT_RMI_URL="" \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID_FILE=/etc/jans/conf/jackrabbit_admin_id \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE=""

# ====
# misc
# ====

LABEL name="janssenproject/persistence-loader" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.7" \
    release="dev" \
    summary="Janssen Authorization Server Persistence loader" \
    description="Generate initial data for persistence layer"

RUN mkdir -p /app/custom_ldif /etc/certs /etc/jans/conf

COPY scripts /app/scripts
# this overrides existing templates
COPY templates /app/templates
RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

# adjust ownership and permission
RUN chmod -R g=u /app/custom_ldif \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans

USER 1000

ENTRYPOINT ["tini", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
