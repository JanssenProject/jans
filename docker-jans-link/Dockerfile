FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git zip

# =====
# Jetty
# =====

ARG JETTY_VERSION=12.0.19
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/jans/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-home-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# ======
# Jython
# ======

ARG JYTHON_VERSION=2.7.3
ARG JYTHON_BUILD_DATE='2022-08-01 17:37'
RUN wget -q https://maven.jans.io/maven/io/jans/jython-installer/${JYTHON_VERSION}/jython-installer-${JYTHON_VERSION}.jar -O /tmp/jython-installer.jar \
    && mkdir -p /opt/jython \
    && java -jar /tmp/jython-installer.jar -v -s -d /opt/jython -e ensurepip \
    && rm -f /tmp/jython-installer.jar /tmp/*.properties

# ====
# Link
# ====


ENV CN_VERSION=1.7.0
ENV CN_BUILD_DATE='2025-07-23 13:16'

ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-link-server/${CN_VERSION}/jans-link-server-${CN_VERSION}.war

# Install Link
RUN mkdir -p ${JETTY_BASE}/jans-link/webapps \
    && wget -q ${CN_SOURCE_URL} -O ${JETTY_BASE}/jans-link/webapps/jans-link.war \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-link --add-modules=server,resources,http,forwarded,ee9-jsp,ee9-deploy,ee9-websocket-jakarta,ee9-cdi-decorate,jmx,logging-log4j2 --approve-all-licenses

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=329a1139194fd9819506150ec73225b425767064
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

RUN mkdir -p /etc/jans/conf \
    /app/static/rdbm \
    /app/schema \
    /app/templates/jans-link

# sync static files from linux-setup
RUN cd /tmp/jans \
    && cp ${JANS_SETUP_DIR}/static/rdbm/sql_data_types.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/ldap_sql_data_type_mapping.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/opendj_attributes_syntax.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/ \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-link/configuration.ldif /app/templates/jans-link/ \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-link/jans-link-config.json /app/templates/jans-link/ \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-link/jans-link-static-config.json /app/templates/jans-link/ \
    && mkdir -p org/eclipse/jetty \
    && cp ${JANS_SETUP_DIR}/static/favicon.ico org/eclipse/jetty/favicon.ico \
    && zip -r ${JETTY_HOME}/lib/jetty-server-${JETTY_VERSION}.jar org/eclipse/jetty/favicon.ico

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

# ==========
# Prometheus
# ==========

COPY static/prometheus-config.yaml /opt/prometheus/

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
    CN_LINK_JAVA_OPTIONS="" \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_PROMETHEUS_PORT="" \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE="" \
    CN_LINK_JETTY_PORT=9091 \
    CN_LINK_JETTY_HOST=0.0.0.0 \
    CN_JETTY_ARGS="" \
    CN_SSL_CERT_FROM_SECRETS=true

# ==========
# misc stuff
# ==========

EXPOSE $CN_LINK_JETTY_PORT

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/link" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen Link" \
    org.opencontainers.image.description=""

RUN mkdir -p /etc/certs \
    ${JETTY_BASE}/jans-link/logs \
    ${JETTY_BASE}/jans-link/custom/libs \
    ${JETTY_BASE}/common/libs \
    ${JETTY_HOME}/temp \
    /usr/share/java \
    /var/jans/link-snapshots

COPY templates /app/templates/
RUN cp /app/templates/jans-link/jans-link.xml ${JETTY_BASE}/jans-link/webapps/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

RUN sed -i 's/\(<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"\)\/\(>\)/\1\2<Set name="showContexts">false<\/Set><\/New>/' /opt/jetty/etc/jetty.xml

RUN ln -sf /usr/lib/jvm/jre /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 jetty

# adjust ownership and permission
RUN chmod 664 ${JETTY_BASE}/jans-link/resources/log4j2.xml \
    && chmod -R g=u ${JETTY_BASE}/jans-link/logs \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /opt/java/lib/security/cacerts \
    && chown -R 1000:0 ${JETTY_BASE}/common/libs \
    && chown -R 1000:0 /usr/share/java \
    && chown -R 1000:0 /opt/prometheus \
    && chown 1000:0 ${JETTY_BASE}/jans-link/webapps/jans-link.xml \
    && chown -R 1000:0 /var/jans/link-snapshots \
    && chown -R 1000:0 ${JETTY_HOME}/temp \
    && chown -R 1000:0 /app/templates

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
