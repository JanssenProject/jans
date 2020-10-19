FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

# symlink JVM
RUN mkdir -p /usr/lib/jvm/default-jvm /usr/java/latest \
    && ln -sf /opt/java/openjdk /usr/lib/jvm/default-jvm/jre \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip tini curl \
    && apk add --no-cache --virtual build-deps unzip wget git

# ==========
# Client API
# ==========

ENV JANS_VERSION=5.0.0-SNAPSHOT
ENV JANS_BUILD_DATE="2020-09-24 08:33"

RUN wget -q https://ox.gluu.org/maven/org/gluu/oxd-server/${JANS_VERSION}/oxd-server-${JANS_VERSION}-distribution.zip -O /oxd.zip \
    && mkdir -p /opt/oxd-server \
    && unzip -qq /oxd.zip -d /opt/oxd-server \
    && rm /oxd.zip \
    && rm -rf /opt/oxd-server/conf/oxd-server.keystore /opt/oxd-server/conf/oxd-server.yml

EXPOSE 8443 8444

# ======
# Python
# ======

RUN apk add --no-cache py3-cryptography
COPY requirements.txt /app/requirements.txt
RUN pip3 install -U pip \
    && pip3 install --no-cache-dir -r /app/requirements.txt \
    && rm -rf /src/pygluu-containerlib/.git

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

ENV JANS_PERSISTENCE_TYPE=ldap \
    JANS_PERSISTENCE_LDAP_MAPPING=default \
    JANS_LDAP_URL=localhost:1636 \
    JANS_COUCHBASE_URL=localhost \
    JANS_COUCHBASE_USER=admin \
    JANS_COUCHBASE_CERT_FILE=/etc/certs/couchbase.crt \
    JANS_COUCHBASE_PASSWORD_FILE=/etc/gluu/conf/couchbase_password \
    JANS_COUCHBASE_CONN_TIMEOUT=10000 \
    JANS_COUCHBASE_CONN_MAX_WAIT=20000 \
    JANS_COUCHBASE_SCAN_CONSISTENCY=not_bounded

# =======
# oxD ENV
# =======

ENV JANS_CLIENT_API_APPLICATION_CERT_CN="localhost" \
    JANS_CLIENT_API_ADMIN_CERT_CN="localhost" \
    JANS_CLIENT_API_BIND_IP_ADDRESSES="*"

# ===========
# Generic ENV
# ===========

ENV JANS_MAX_RAM_PERCENTAGE=75.0 \
    JANS_WAIT_MAX_TIME=300 \
    JANS_WAIT_SLEEP_DURATION=10 \
    JANS_JAVA_OPTIONS="" \
    JANS_SSL_CERT_FROM_SECRETS=false

# ====
# misc
# ====

LABEL name="Client API" \
    maintainer="Gluu Inc. <support@gluu.org>" \
    vendor="Janssen" \
    version="5.0.0" \
    release="dev" \
    summary="Gluu client API" \
    description="Client software to secure apps with OAuth 2.0, OpenID Connect, and UMA"

RUN mkdir -p /etc/certs /app/templates/ /deploy /etc/gluu/conf
COPY scripts /app/scripts
COPY templates/*.tmpl /app/templates/
RUN chmod +x /app/scripts/entrypoint.sh

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
