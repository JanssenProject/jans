FROM bellsoft/liberica-openjre-alpine:11.0.16

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl bash py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git zip \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/jre /usr/java/latest/jre

# =====
# Jetty
# =====

ARG JETTY_VERSION=11.0.11
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/jans/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-home-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# Ports required by jetty
EXPOSE 8080

# ======
# Jython
# ======

ARG JYTHON_VERSION=2.7.3
ARG JYTHON_BUILD_DATE='2022-08-01 07:49'
RUN wget -q https://maven.jans.io/maven/io/jans/jython-installer/${JYTHON_VERSION}/jython-installer-${JYTHON_VERSION}.jar -O /tmp/jython-installer.jar \
    && mkdir -p /opt/jython \
    && java -jar /tmp/jython-installer.jar -v -s -d /opt/jython \
    && rm -f /tmp/jython-installer.jar /tmp/*.properties

# install pydev debugger into Jython
RUN wget -q https://github.com/fabioz/PyDev.Debugger/archive/refs/tags/pydev_debugger_2_3_0.tar.gz -P /tmp \
    && tar xvf /tmp/pydev_debugger_2_3_0.tar.gz -C /tmp \
    && cd /tmp/PyDev.Debugger-pydev_debugger_2_3_0 \
    && /opt/jython/bin/pip install . \
    && rm -rf /tmp/pydev_debugger* /tmp/PyDev.Debugger-*

# uninstall Jython pip and easy_install to reduce vulnerabilities
RUN /opt/jython/bin/pip uninstall -y pip

# ===========
# Auth server
# ===========

ENV CN_VERSION=1.0.6-SNAPSHOT
ENV CN_BUILD_DATE='2022-12-01 12:38'
ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-auth-server/${CN_VERSION}/jans-auth-server-${CN_VERSION}.war

# Install Jans Auth
COPY jetty/jetty-env.xml /tmp/WEB-INF/jetty-env.xml
RUN mkdir -p ${JETTY_BASE}/jans-auth/webapps \
    && wget -q ${CN_SOURCE_URL} -O /tmp/jans-auth.war \
    && cd /tmp \
    && zip -d jans-auth.war WEB-INF/jetty-web.xml \
    && zip -r jans-auth.war WEB-INF/jetty-env.xml \
    && cp jans-auth.war ${JETTY_BASE}/jans-auth/webapps/jans-auth.war \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-auth --add-module=server,deploy,annotations,resources,http,http-forwarded,threadpool,jsp,websocket,cdi-decorate \
    && rm -rf /tmp/jans-auth.war /tmp/WEB-INF

# ===========
# Custom libs
# ===========

RUN mkdir -p /usr/share/java

ARG TWILIO_VERSION=7.17.0
RUN wget -q https://repo1.maven.org/maven2/com/twilio/sdk/twilio/${TWILIO_VERSION}/twilio-${TWILIO_VERSION}.jar -P /usr/share/java/

ARG JSMPP_VERSION=2.3.7
RUN wget -q https://repo1.maven.org/maven2/org/jsmpp/jsmpp/${JSMPP_VERSION}/jsmpp-${JSMPP_VERSION}.jar -P /usr/share/java/

# This will later be refactored and moved to be pulled from persitence or a central bucket
ARG CASA_CONFIG_VERSION=5.0.0-SNAPSHOT
ARG CASA_CONFIG_BUILD_DATE="2022-05-26 13:56"
RUN wget -q https://jenkins.gluu.org/maven/org/gluu/casa-config/${CASA_CONFIG_VERSION}/casa-config-${CASA_CONFIG_VERSION}.jar -P /usr/share/java/

# A workaround for Fido2 integration
ARG FIDO2_CLIENT_VERSION=1.0.1
ARG FIDO2_CLIENT_BUILD_DATE="2022-07-06 12:49"
RUN wget -q https://jenkins.jans.io/maven/io/jans/jans-fido2-client/${FIDO2_CLIENT_VERSION}/jans-fido2-client-${FIDO2_CLIENT_VERSION}.jar -P /usr/share/java/

# =====================
# Casa external scripts
# =====================

ARG FLEX_SOURCE_VERSION=48418cd351655a48c650075a6d60601c368a403d
ARG CASA_EXTRAS_DIR=casa/extras

RUN mkdir -p /opt/jans/python/libs
RUN git clone --filter blob:none --no-checkout https://github.com/GluuFederation/flex.git /tmp/flex \
    && cd /tmp/flex \
    && git sparse-checkout init --cone \
    && git checkout ${FLEX_SOURCE_VERSION} \
    && git sparse-checkout add ${CASA_EXTRAS_DIR} \
    && cd /opt/jans/python/libs \
    && cp /tmp/flex/${CASA_EXTRAS_DIR}/casa-external_* . \
    && rm -rf /tmp/flex

# ===========
# Agama files
# ===========

RUN mkdir -p ${JETTY_BASE}/jans-auth/agama/fl \
    ${JETTY_BASE}/jans-auth/agama/ftl \
    ${JETTY_BASE}/jans-auth/agama/scripts

# janssenproject/jans SHA commit
ENV JANS_SOURCE_VERSION=c9d1ed9cbb5de852fcada889f8c1f6be95d66c49

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the agama code
RUN git clone --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add agama/misc

RUN cp -R /tmp/jans/agama/misc/* ${JETTY_BASE}/jans-auth/agama/ \
    && rm -rf /tmp/jans

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel \
    && pip3 install --no-cache-dir --default-timeout=300 -r /app/requirements.txt \
    && pip3 uninstall -y pip wheel

# ==========
# Prometheus
# ==========

ARG PROMETHEUS_JAVAAGENT_VERSION=0.17.2
COPY conf/prometheus-config.yaml /opt/prometheus/
RUN mkdir -p /opt/prometheus \
    && wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${PROMETHEUS_JAVAAGENT_VERSION}/jmx_prometheus_javaagent-${PROMETHEUS_JAVAAGENT_VERSION}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-auth --add-module=jmx,stats

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

ENV CN_MAX_RAM_PERCENTAGE=75.0 \
    CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    PYTHON_HOME=/opt/jython \
    CN_DOCUMENT_STORE_TYPE=LOCAL \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID=admin \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    CN_JAVA_OPTIONS="" \
    CN_SYNC_JKS_ENABLED=false \
    CN_SYNC_JKS_INTERVAL=30 \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json \
    ADMIN_UI_JWKS=http://0.0.0.0:8080/jans-auth/restv1/jwks \
    CN_JETTY_REQUEST_HEADER_SIZE=8192 \
    CN_PROMETHEUS_PORT="" \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE=""

# ==========
# misc stuff
# ==========

LABEL name="janssenproject/auth-server" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.6" \
    release="dev" \
    summary="Janssen Authorization Server" \
    description="OAuth 2.0 server and client; OpenID Connect Provider (OP) & UMA Authorization Server (AS)"

RUN mkdir -p ${JETTY_BASE}/jans-auth/custom/pages \
    ${JETTY_BASE}/jans-auth/custom/static \
    ${JETTY_BASE}/jans-auth/custom/libs \
    ${JETTY_BASE}/jans-auth/custom/i18n \
    ${JETTY_BASE}/jans-auth/logs \
    ${JETTY_BASE}/common/libs/spanner \
    ${JETTY_BASE}/common/libs/couchbase \
    /etc/jans/conf \
    /app/templates

COPY certs /etc/certs
COPY jetty/jans-auth_web_resources.xml ${JETTY_BASE}/jans-auth/webapps/
COPY jetty/agama_web_resources.xml ${JETTY_BASE}/jans-auth/webapps/
COPY jetty/log4j2.xml ${JETTY_BASE}/jans-auth/resources/
COPY conf/*.tmpl /app/templates/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 jetty

COPY --chown=1000:0 jetty/jans-auth.xml ${JETTY_BASE}/jans-auth/webapps/

# adjust ownership and permission
RUN chmod -R g=u ${JETTY_BASE}/jans-auth/custom \
    && chmod -R g=u ${JETTY_BASE}/jans-auth/resources \
    && chmod -R g=u ${JETTY_BASE}/jans-auth/logs \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /usr/java/latest/jre/lib/security/cacerts \
    && chmod 664 /opt/jetty/etc/jetty.xml \
    && chmod 664 /opt/jetty/etc/webdefault.xml \
    && chown -R 1000:0 ${JETTY_BASE}/jans-auth/agama \
    && chown -R 1000:0 /opt/jans/python/libs \
    && chown -R 1000:0 ${JETTY_BASE}/common/libs \
    && chown -R 1000:0 /usr/share/java

USER 1000

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
