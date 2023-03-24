FROM bellsoft/liberica-openjre-alpine:11.0.16

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 curl tini py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/jre /usr/java/latest/jre

# ===========
# Auth client
# ===========

# JAR files required to generate OpenID Connect keys
ENV CN_VERSION=1.0.8-SNAPSHOT
ENV CN_BUILD_DATE='2023-02-07 08:25'
ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-auth-client/${CN_VERSION}/jans-auth-client-${CN_VERSION}-jar-with-dependencies.jar

RUN wget -q ${CN_SOURCE_URL} -P /app/javalibs/

# removed as they're not part of Janssen
# =================
# Shibboleth sealer
# =================

# RUN mkdir -p /app/javalibs \
#     && wget -q https://build.shibboleth.net/nexus/content/repositories/releases/net/shibboleth/utilities/java-support/7.5.1/java-support-7.5.1.jar -O /app/javalibs/java-support.jar \
#     && wget -q https://repo1.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar -P /app/javalibs/ \
#     && wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.26/slf4j-api-1.7.26.jar -P /app/javalibs/ \
#     && wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.26/slf4j-simple-1.7.26.jar -P /app/javalibs/

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools \
    && pip3 install --no-cache-dir -r /app/requirements.txt \
    && pip3 uninstall -y pip wheel

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
    CN_CONFIG_CONSUL_CONSISTENCY=default \
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
    CN_SECRET_VAULT_SCHEME=http \
    CN_SECRET_VAULT_HOST=localhost \
    CN_SECRET_VAULT_PORT=8200 \
    CN_SECRET_VAULT_VERIFY=false \
    CN_SECRET_VAULT_ROLE_ID_FILE=/etc/certs/vault_role_id \
    CN_SECRET_VAULT_SECRET_ID_FILE=/etc/certs/vault_secret_id \
    CN_SECRET_VAULT_CERT_FILE=/etc/certs/vault_client.crt \
    CN_SECRET_VAULT_KEY_FILE=/etc/certs/vault_client.key \
    CN_SECRET_VAULT_CACERT_FILE=/etc/certs/vault_ca.crt \
    CN_SECRET_VAULT_NAMESPACE=jans \
    CN_SECRET_KUBERNETES_NAMESPACE=default \
    CN_SECRET_KUBERNETES_SECRET=jans \
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false

# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=ldap \
    CN_HYBRID_MAPPING="{}" \
    CN_LDAP_URL=localhost:1636 \
    CN_LDAP_USE_SSL=true \
    CN_COUCHBASE_URL=localhost \
    CN_COUCHBASE_USER=admin \
    CN_COUCHBASE_CERT_FILE=/etc/certs/couchbase.crt \
    CN_COUCHBASE_PASSWORD_FILE=/etc/jans/conf/couchbase_password \
    CN_COUCHBASE_CONN_TIMEOUT=10000 \
    CN_COUCHBASE_CONN_MAX_WAIT=20000 \
    CN_COUCHBASE_SCAN_CONSISTENCY=not_bounded \
    CN_COUCHBASE_BUCKET_PREFIX=jans \
    CN_COUCHBASE_TRUSTSTORE_ENABLE=true \
    CN_COUCHBASE_KEEPALIVE_INTERVAL=30000 \
    CN_COUCHBASE_KEEPALIVE_TIMEOUT=2500 \
    CN_GOOGLE_SPANNER_INSTANCE_ID="" \
    CN_GOOGLE_SPANNER_DATABASE_ID=""

# ===========
# Generic ENV
# ===========

ENV CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_CONTAINER_METADATA=docker \
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

LABEL name="janssenproject/certmanager" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.8" \
    release="dev" \
    summary="Janssen Certmanager" \
    description="Manage certs and crypto keys for Janssen Server"

RUN mkdir -p /etc/certs /etc/jans/conf

COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

# adjust ownership and permission
RUN chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans

USER 1000

ENTRYPOINT ["tini", "-g", "--", "sh", "/app/scripts/entrypoint.sh"]
CMD ["--help"]
