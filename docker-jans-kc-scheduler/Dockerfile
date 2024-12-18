FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl py3-cryptography py3-psycopg2 py3-grpcio bash \
    && apk add --no-cache --virtual .build-deps wget git zip

# ============
# KC scheduler
# ============

ENV CN_VERSION=0.0.0-nightly
ENV CN_BUILD_DATE='2024-10-27 08:54'
ENV SCHEDULER_HOME=/opt/kc-scheduler

RUN mkdir -p ${SCHEDULER_HOME}/bin \
    ${SCHEDULER_HOME}/conf \
    ${SCHEDULER_HOME}/lib \
    ${SCHEDULER_HOME}/logs

RUN wget -q https://jenkins.jans.io/maven/io/jans/kc-jans-scheduler/${CN_VERSION}/kc-jans-scheduler-${CN_VERSION}-deps.zip -O /tmp/kc-scheduler.zip \
    && unzip /tmp/kc-scheduler.zip -d ${SCHEDULER_HOME}/lib \
    && rm -rf /tmp/kc-scheduler.zip \
    && wget -q https://jenkins.jans.io/maven/io/jans/kc-jans-scheduler/${CN_VERSION}/kc-jans-scheduler-${CN_VERSION}.jar -P ${SCHEDULER_HOME}/lib

# ==================
# Logback extra libs
# ==================

RUN wget -q https://repo1.maven.org/maven2/org/codehaus/janino/janino/3.1.9/janino-3.1.9.jar -P ${SCHEDULER_HOME}/lib \
    && wget -q https://repo1.maven.org/maven2/org/codehaus/janino/commons-compiler/3.1.9/commons-compiler-3.1.9.jar -P ${SCHEDULER_HOME}/lib

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=39e74a153edf01db8ab43be81f5585bc4210818d

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
RUN git clone --depth 500 --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add jans-pycloudlib

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN mv /usr/lib/python3.12/EXTERNALLY-MANAGED /usr/lib/python3.12/EXTERNALLY-MANAGED.disabled \
    && python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools \
    && pip3 install --no-cache-dir -r /app/requirements.txt \
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
    CN_SSL_CERT_FROM_SECRETS=true

# ==========
# misc stuff
# ==========

EXPOSE $CN_SAML_HTTP_PORT

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/kc-scheduler" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen KC scheduler" \
    org.opencontainers.image.description=""

RUN mkdir -p /etc/certs \
    /etc/jans/conf

COPY templates /app/templates/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh \
    && ln -sf /app/scripts/entrypoint.sh ${SCHEDULER_HOME}/bin/kc-scheduler.sh

RUN ln -sf /usr/lib/jvm/jre /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 jans

# adjust ownership and permission
RUN chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /opt/java/lib/security/cacerts \
    && chown -R 1000:0 /opt/kc-scheduler

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/opt/kc-scheduler/bin/kc-scheduler.sh"]
