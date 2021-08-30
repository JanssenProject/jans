FROM alpine:3.13

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip tini curl openjdk11-jre-headless py3-cryptography py3-grpcio py3-psycopg2 \
    && apk add --no-cache --virtual build-deps wget git \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# =====
# Jetty
# =====

ARG JETTY_VERSION=9.4.43.v20210629
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/jans/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/${JETTY_VERSION}/jetty-distribution-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-distribution-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# ==========
# Config API
# ==========

ENV CN_VERSION=1.0.0-SNAPSHOT
ENV CN_BUILD_DATE='2021-08-30 13:23'
ENV CN_SOURCE_URL=https://maven.jans.io/maven/io/jans/jans-config-api-server/${CN_VERSION}/jans-config-api-server-${CN_VERSION}.war

# Install Jans Config API
RUN wget -q ${CN_SOURCE_URL} -O /tmp/jans-config-api.war \
    && mkdir -p ${JETTY_BASE}/jans-config-api/webapps/jans-config-api \
    && unzip -qq /tmp/jans-config-api.war -d ${JETTY_BASE}/jans-config-api/webapps/jans-config-api \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-config-api --add-to-start=server,deploy,annotations,resources,http,http-forwarded,threadpool,jsp,websocket \
    && rm -f /tmp/jans-config-api.war

# Ports exposed by config-api
EXPOSE 8074

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
    CN_COUCHBASE_KEEPALIVE_TIMEOUT=2500 \
    CN_GOOGLE_SPANNER_INSTANCE_ID="" \
    CN_GOOGLE_SPANNER_DATABASE_ID=""

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

LABEL name="Config API" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen" \
    version="1.0.0" \
    release="b10" \
    summary="Janssen Config API" \
    description=""

RUN mkdir -p /etc/certs /app/templates/ /deploy /etc/jans/conf
RUN touch /etc/hosts.back
COPY jetty/jans-config-api.xml ${JETTY_BASE}/jans-config-api/webapps/
COPY conf/*.tmpl /app/templates/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 jetty

# adjust ownership
RUN chown -R 1000:1000 /opt/jans/jetty \
    && chown -R 1000:1000 /opt/jetty \
    && chown -R 1000:1000 /deploy \
    && chown -R 1000:1000 /tmp \
    && chown -R 1000:1000 /etc/hosts.back \
    && chgrp -R 0 /etc/hosts.back && chmod -R g=u /etc/hosts.back \
    && chgrp -R 0 /opt/jans/jetty && chmod -R g=u /opt/jans/jetty \
    && chgrp -R 0 /opt/jetty && chmod -R g=u /opt/jetty \
    && chgrp -R 0 /tmp && chmod -R g=u /tmp \
    && chgrp -R 0 /deploy && chmod -R g=u /deploy \
    && chgrp -R 0 /etc/certs && chmod -R g=u /etc/certs \
    && chgrp -R 0 /etc/jans && chmod -R g=u /etc/jans \
    && chmod -R +w /etc/ssl/certs/java/cacerts && chgrp -R 0 /etc/ssl/certs/java/cacerts && chmod -R g=u /etc/ssl/certs/java/cacerts

USER 1000

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
