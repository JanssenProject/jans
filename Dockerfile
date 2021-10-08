FROM alpine:3.13.6

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip tini curl openjdk11-jre-headless py3-cryptography py3-grpcio \
    && apk add --no-cache --virtual build-deps unzip wget git \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# ==========
# Client API
# ==========

ENV CN_VERSION=1.0.0-SNAPSHOT
ENV CN_BUILD_DATE='2021-10-08 06:18'
ENV CN_SOURCE_URL=https://maven.jans.io/maven/io/jans/jans-client-api-server/${CN_VERSION}/jans-client-api-server-${CN_VERSION}-distribution.zip

RUN wget -q ${CN_SOURCE_URL} -O /tmp/client-api.zip \
    && mkdir -p /opt/client-api \
    && unzip -qq /tmp/client-api.zip -d /opt/client-api \
    && rm /tmp/client-api.zip \
    && rm -rf /opt/client-api/conf/client-api-server.keystore /opt/client-api/conf/client-api-server.yml

EXPOSE 8443 8444

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
    CN_CONFIG_CONSUL_NAMESPACE=jans \
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
    CN_SECRET_VAULT_NAMESPACE=jans \
    CN_SECRET_KUBERNETES_NAMESPACE=default \
    CN_SECRET_KUBERNETES_SECRET=jans \
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false \
    CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_SECRET_GOOGLE_SECRET_VERSION_ID=latest \
    CN_SECRET_GOOGLE_SECRET_NAME_PREFIX=jans

# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=ldap \
    CN_PERSISTENCE_LDAP_MAPPING=default \
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
    CN_COUCHBASE_KEEPALIVE_TIMEOUT=2500

# ==============
# client-api ENV
# ==============

ENV CN_CLIENT_API_APPLICATION_CERT_CN=localhost \
    CN_CLIENT_API_ADMIN_CERT_CN=localhost \
    CN_CLIENT_API_BIND_IP_ADDRESSES="*"

# ===========
# Generic ENV
# ===========

ENV CN_MAX_RAM_PERCENTAGE=75.0 \
    CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_JAVA_OPTIONS="" \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json

# ====
# misc
# ====

LABEL name="Client API" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen" \
    version="1.0.0" \
    release="b10" \
    summary="Janssen Client API" \
    description="Client software to secure apps with OAuth 2.0, OpenID Connect, and UMA"

RUN mkdir -p /etc/certs /app/templates/ /deploy /etc/jans/conf
COPY scripts /app/scripts
COPY templates/*.tmpl /app/templates/
RUN chmod +x /app/scripts/entrypoint.sh

# # create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

 # adjust ownership
RUN chown -R 1000:1000 /app/templates \
    && chown -R 1000:1000 /etc/jans \
    && chown -R 1000:1000 /deploy \
    && chown -R 1000:1000 /tmp \
    && chgrp -R 0 /tmp && chmod -R g=u /tmp \
    && chgrp -R 0 /deploy && chmod -R g=u /deploy \
    && chgrp -R 0 /etc/certs && chmod -R g=u /etc/certs \
    && chgrp -R 0 /etc/jans && chmod -R g=u /etc/jans \
    && chmod -R +w /etc/ssl/certs/java/cacerts && chgrp -R 0 /etc/ssl/certs/java/cacerts && chmod -R g=u /etc/ssl/certs/java/cacerts

USER 1000

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
