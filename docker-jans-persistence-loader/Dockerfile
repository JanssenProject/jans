FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache python3 curl tini py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps git wget \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/jre /usr/java/latest/jre

# ===========
# Assets sync
# ===========

# janssenproject/jans SHA commit
ENV JANS_SOURCE_VERSION=b8a7e1a493a2e7059a8e220eb6ff4305866bfc70
ARG JANS_SETUP_DIR=jans-linux-setup/jans_setup
ARG JANS_SCRIPT_CATALOG_DIR=docs/script-catalog
ARG JANS_CONFIG_API_RESOURCES=jans-config-api/server/src/main/resources

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
ARG GIT_CLONE_DEPTH=100
RUN git clone --depth ${GIT_CLONE_DEPTH} --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add ${JANS_SETUP_DIR} \
    && git sparse-checkout add ${JANS_SCRIPT_CATALOG_DIR} \
    && git sparse-checkout add ${JANS_CONFIG_API_RESOURCES} \
    && git sparse-checkout add jans-pycloudlib

RUN mkdir -p /app/static /app/schema /app/static/opendj /app/templates

# sync static files from linux-setup
RUN cd /tmp/jans \
    && cp -R ${JANS_SETUP_DIR}/static/extension /app/static/extension \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/opendj_types.json \
    && cp -R ${JANS_SETUP_DIR}/static/rdbm /app/static/rdbm \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/custom_schema.json \
    && cp -R ${JANS_SCRIPT_CATALOG_DIR} /app/script-catalog \
    && cp ${JANS_CONFIG_API_RESOURCES}/config-api-rs-protect.json -P /app/static/ \
    && cp ${JANS_SETUP_DIR}/templates/*.ldif /app/templates \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-auth /app/templates/jans-auth \
    && cp ${JANS_SETUP_DIR}/static/metric/o_metric.ldif /app/templates/o_metric.ldif \
    && cp ${JANS_SETUP_DIR}/static/site/site.ldif /app/templates/o_site.ldif \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-cli /app/templates/jans-cli

# ======
# Python
# ======

# default pip timeout
ARG PIP_TIMEOUT=15
COPY requirements.txt /app/requirements.txt
RUN mv /usr/lib/python3.12/EXTERNALLY-MANAGED /usr/lib/python3.12/EXTERNALLY-MANAGED.disabled \
    && python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools --timeout ${PIP_TIMEOUT} \
    && pip3 install --no-cache-dir -r /app/requirements.txt --timeout ${PIP_TIMEOUT} \
    && pip3 uninstall -y pip wheel

# =======
# Cleanup
# =======

RUN apk del .build-deps \
    && rm -rf /var/cache/apk/* /tmp/jans

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
    CN_SECRET_VAULT_VERIFY=false \
    CN_SECRET_VAULT_ROLE_ID_FILE=/etc/certs/vault_role_id \
    CN_SECRET_VAULT_SECRET_ID_FILE=/etc/certs/vault_secret_id \
    CN_SECRET_VAULT_CERT_FILE=/etc/certs/vault_client.crt \
    CN_SECRET_VAULT_KEY_FILE=/etc/certs/vault_client.key \
    CN_SECRET_VAULT_CACERT_FILE=/etc/certs/vault_ca.crt \
    CN_SECRET_VAULT_NAMESPACE="" \
    CN_SECRET_VAULT_ADDR=http://localhost:8200 \
    CN_SECRET_VAULT_KV_PATH=secret \
    CN_SECRET_VAULT_PREFIX=jans \
    CN_SECRET_VAULT_APPROLE_PATH=approle \
    CN_SECRET_KUBERNETES_NAMESPACE=default \
    CN_SECRET_KUBERNETES_SECRET=jans \
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false


# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=sql \
    CN_HYBRID_MAPPING="{}"

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
    CN_PERSISTENCE_IMPORT_BUILTIN_LDIF=true \
    CN_DOCUMENT_STORE_TYPE=DB \
    CN_JACKRABBIT_RMI_URL="" \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID_FILE=/etc/jans/conf/jackrabbit_admin_id \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE="" \
    CN_DUO_ENABLED=true \
    CN_LOCK_ENABLED=false

# ====
# misc
# ====

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/persistence-loader" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen Authorization Server Persistence loader" \
    org.opencontainers.image.description="Generate initial data for persistence layer"

RUN mkdir -p /app/custom_ldif /etc/certs /etc/jans/conf

COPY scripts /app/scripts
# this overrides existing templates
COPY templates /app/templates

# disable keyRegeneration
RUN sed -i -r 's/("keyRegenerationEnabled":)true/\1false/g' /app/templates/jans-auth/jans-auth-config.json

RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 1000

# adjust ownership and permission
RUN chmod -R g=u /app/custom_ldif \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
