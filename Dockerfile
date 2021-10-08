FROM alpine:3.13.6

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip curl tini openjdk11-jre-headless py3-cryptography py3-grpcio \
    && apk add --no-cache --virtual build-deps wget git \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# ===========
# Auth client
# ===========

# JAR files required to generate OpenID Connect keys
ENV CN_VERSION=1.0.0-SNAPSHOT
ENV CN_BUILD_DATE='2021-10-08 04:13'
ENV CN_SOURCE_URL=https://maven.jans.io/maven/io/jans/jans-auth-client/${CN_VERSION}/jans-auth-client-${CN_VERSION}-jar-with-dependencies.jar

RUN wget -q ${CN_SOURCE_URL} -P /app/javalibs/

# =================
# Shibboleth sealer
# =================

# @TODO: remove them?
# RUN wget -q https://build.shibboleth.net/nexus/content/repositories/releases/net/shibboleth/utilities/java-support/7.5.1/java-support-7.5.1.jar -P /app/javalibs/ \
#     && wget -q https://repo1.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar -P /app/javalibs/ \
#     && wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.26/slf4j-api-1.7.26.jar -P /app/javalibs/ \
#     && wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.26/slf4j-simple-1.7.26.jar -P /app/javalibs/

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN pip3 install --no-cache-dir -U pip \
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

# ===========
# Generic ENV
# ===========

ENV CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json \
    CN_CONFIGURATION_SKIP_INITIALIZED=false

# ====
# misc
# ====

LABEL name="configuration-manager" \
    maintainer="Janssen <support@jans.io>" \
    vendor="Janssen" \
    version="1.0.0" \
    release="b11" \
    summary="Janssen Configuration Manager" \
    description="Manage config and secret"

COPY scripts /app/scripts
RUN mkdir -p /etc/certs /app/db \
    && chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

 # adjust ownership
RUN chown -R 1000:1000 /tmp \
    && chown -R 1000:1000 /app/db \
    && chgrp -R 0 /app/db && chmod -R g=u /app/db \
    && chgrp -R 0 /tmp && chmod -R g=u /tmp \
    && chgrp -R 0 /etc/certs && chmod -R g=u /etc/certs \
    && chmod -R +w /etc/ssl/certs/java/cacerts && chgrp -R 0 /etc/ssl/certs/java/cacerts && chmod -R g=u /etc/ssl/certs/java/cacerts

USER 1000

ENTRYPOINT ["tini", "-g", "--", "sh", "/app/scripts/entrypoint.sh"]
CMD ["--help"]
