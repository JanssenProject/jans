FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 curl tini py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git

# ===========
# Auth client
# ===========

# JAR files required to generate OpenID Connect keys
ENV CN_VERSION=0.0.0-nightly
ENV CN_BUILD_DATE='2024-12-20 08:29'

ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-auth-client/${CN_VERSION}/jans-auth-client-${CN_VERSION}-jar-with-dependencies.jar

RUN wget -q ${CN_SOURCE_URL} -P /app/javalibs/

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=aa1b2edaa8d7e3413bd57a7bd7cc86206086768b

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
ARG GIT_CLONE_DEPTH=100
RUN git clone --depth ${GIT_CLONE_DEPTH} --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add jans-pycloudlib

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

ENV CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_CONTAINER_METADATA=docker \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE=""

# ====
# misc
# ====

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/certmanager" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen Certmanager" \
    org.opencontainers.image.description="Manage certs and crypto keys for Janssen Server"

RUN mkdir -p /etc/certs /etc/jans/conf

COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

RUN ln -sf /usr/lib/jvm/jre /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 1000

# adjust ownership and permission
RUN chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-g", "--", "sh", "/app/scripts/entrypoint.sh"]
CMD ["--help"]
