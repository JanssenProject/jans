FROM quay.io/keycloak/keycloak:25.0.6@sha256:82c5b7a110456dbd42b86ea572e728878549954cc8bd03cd65410d75328095d2 as kc-src

FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl py3-cryptography py3-psycopg2 py3-grpcio bash \
    && apk add --no-cache --virtual .build-deps wget git zip

# ========
# Keycloak
# ========

COPY --from=kc-src /opt/keycloak /opt/keycloak
RUN mkdir -p /opt/keycloak/logs \
    /opt/keycloak/providers

# ==============
# KC integration
# ==============

ENV CN_VERSION=0.0.0-nightly
ENV CN_BUILD_DATE='2024-12-20 09:15'

RUN wget -q https://jenkins.jans.io/maven/io/jans/kc-jans-spi/${CN_VERSION}/kc-jans-spi-${CN_VERSION}.jar -P /opt/keycloak/providers \
    && wget -q https://jenkins.jans.io/maven/io/jans/kc-jans-spi/${CN_VERSION}/kc-jans-spi-${CN_VERSION}-deps.zip -O /tmp/kc-jans-spi.zip \
    && unzip /tmp/kc-jans-spi.zip -d /opt/keycloak/providers \
    && rm -rf /tmp/kc-jans-spi.zip

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=a3a9bb2828b33e4573289f0330e48a9e44500c12
ARG JANS_SETUP_DIR=jans-linux-setup/jans_setup

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
ARG GIT_CLONE_DEPTH=100
RUN git clone --depth ${GIT_CLONE_DEPTH} --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add ${JANS_SETUP_DIR} \
    && git sparse-checkout add jans-pycloudlib

RUN mkdir -p /app/static/rdbm \
    /app/schema \
    /app/templates/jans-saml

# sync static files from linux-setup
RUN cd /tmp/jans \
    && cp ${JANS_SETUP_DIR}/static/rdbm/sql_data_types.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/ldap_sql_data_type_mapping.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/opendj_attributes_syntax.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/ \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-saml/* /app/templates/jans-saml

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
    CN_CONFIG_CONSUL_NAMESPACE=jans \
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

ENV CN_MAX_RAM_PERCENTAGE=75.0 \
    CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE="" \
    CN_SAML_HTTP_PORT=8083 \
    CN_SAML_HTTP_HOST=0.0.0.0 \
    CN_SAML_JAVA_OPTIONS="" \
    CN_SAML_KC_ADMIN_CREDENTIALS_FILE=/etc/jans/conf/kc_admin_creds \
    CN_SAML_KC_DB_PASSWORD_FILE=/etc/jans/conf/kc_db_password \
    CN_SSL_CERT_FROM_SECRETS=true \
    KC_LOG_LEVEL=INFO \
    KC_DB=mysql \
    KC_DB_USERNAME=keycloak \
    KC_DB_SCHEMA=keycloak \
    KC_DB_URL_HOST=localhost \
    KC_DB_URL_PORT=3306 \
    KC_DB_URL_DATABASE=keycloak \
    KC_DB_URL_PROPERTIES=?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4 \
    QUARKUS_TRANSACTION_MANAGER_ENABLE_RECOVERY=false

# ==========
# misc stuff
# ==========

EXPOSE $CN_SAML_HTTP_PORT

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/saml" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen SAML" \
    org.opencontainers.image.description=""

RUN mkdir -p /etc/certs \
    /etc/jans/conf \
    /opt/idp/configs/keycloak/metadata \
    /opt/idp/configs/keycloak/temp_metadata \
    /opt/idp/configs/keycloak/idp/metadata \
    /opt/idp/configs/keycloak/idp/temp_metadata \
    /opt/idp/configs/keycloak/sp/metadata \
    /opt/idp/configs/keycloak/sp/temp_metadata \
    /usr/share/java

COPY templates /app/templates/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

RUN ln -sf /usr/lib/jvm/jre /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 jans

# adjust ownership and permission
RUN chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /opt/java/lib/security/cacerts \
    && chown -R 1000:0 /opt/idp \
    && chown -R 1000:0 /usr/share/java \
    && chown -R 1000:0 /opt/keycloak/logs \
    && chown -R 1000:0 /opt/keycloak/conf \
    && chown -R 1000:0 /app/templates

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
