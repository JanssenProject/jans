FROM bellsoft/liberica-openjdk-alpine:17.0.12@sha256:83719ee0075de03ecc7a99db489fd72a022e49f09c0e6e6350bcc05310c3caf6

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl bash py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git zip

# =====
# Jetty
# =====

ARG JETTY_VERSION=11.0.24
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
    && java -jar /tmp/jython-installer.jar -v -s -d /opt/jython \
    && rm -f /tmp/jython-installer.jar /tmp/*.properties

# install pydev debugger into Jython
RUN wget -q https://github.com/fabioz/PyDev.Debugger/archive/refs/tags/pydev_debugger_2_3_0.tar.gz -P /tmp \
    && tar xvf /tmp/pydev_debugger_2_3_0.tar.gz -C /tmp \
    && cd /tmp/PyDev.Debugger-pydev_debugger_2_3_0 \
    && /opt/jython/bin/pip install . \
    && rm -rf /tmp/pydev_debugger* /tmp/PyDev.Debugger-*

# uninstall modules included in Jython ensurepip to reduce vulnerabilities
RUN /opt/jython/bin/pip uninstall -y pip setuptools

# ===========
# Auth server
# ===========

ENV CN_VERSION=1.4.0
ENV CN_BUILD_DATE='2025-01-29 13:09'

ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-auth-server/${CN_VERSION}/jans-auth-server-${CN_VERSION}.war

# Install Jans Auth
COPY static/jetty-env.xml /tmp/WEB-INF/jetty-env.xml
RUN mkdir -p ${JETTY_BASE}/jans-auth/webapps \
    && wget -q ${CN_SOURCE_URL} -O /tmp/jans-auth.war \
    && cd /tmp \
    && zip -d jans-auth.war WEB-INF/jetty-web.xml \
    && zip -r jans-auth.war WEB-INF/jetty-env.xml \
    && cp jans-auth.war ${JETTY_BASE}/jans-auth/webapps/jans-auth.war \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-auth --add-module=server,deploy,annotations,resources,http,http-forwarded,threadpool,jsp,websocket,cdi-decorate,jmx,stats,logging-log4j2 --approve-all-licenses \
    && rm -rf /tmp/jans-auth.war /tmp/WEB-INF

# ===========
# Custom libs
# ===========

RUN mkdir -p /usr/share/java \
    ${JETTY_BASE}/jans-auth/_libs

ARG TWILIO_VERSION=7.17.0
ARG JSMPP_VERSION=2.3.7

RUN wget -q https://repo1.maven.org/maven2/com/twilio/sdk/twilio/${TWILIO_VERSION}/twilio-${TWILIO_VERSION}.jar -P ${JETTY_BASE}/jans-auth/_libs/ \
    && wget -q https://repo1.maven.org/maven2/org/jsmpp/jsmpp/${JSMPP_VERSION}/jsmpp-${JSMPP_VERSION}.jar -P ${JETTY_BASE}/jans-auth/_libs/ \
    && for custom_lib in casa-config jans-fido2-client jans-fido2-model agama-inbound jans-lock-service jans-lock-model; \
    do \
        wget -nv "https://jenkins.jans.io/maven/io/jans/${custom_lib}/${CN_VERSION}/${custom_lib}-${CN_VERSION}.jar" -P "${JETTY_BASE}/jans-auth/_libs"; \
    done

# ===========
# Assets sync
# ===========

RUN mkdir -p ${JETTY_BASE}/jans-auth/agama/fl \
    ${JETTY_BASE}/jans-auth/agama/ftl \
    ${JETTY_BASE}/jans-auth/agama/scripts \
    /app/templates \
    /app/static/rdbm \
    /app/schema

ENV JANS_SOURCE_VERSION=3b2540e3e1f94e10794e78ca09e552de3022bd30
ARG JANS_SETUP_DIR=jans-linux-setup/jans_setup

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
ARG GIT_CLONE_DEPTH=100
RUN git clone --depth ${GIT_CLONE_DEPTH} --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add agama/misc \
    && git sparse-checkout add jans-linux-setup/jans_setup \
    && git sparse-checkout add jans-pycloudlib \
    && git sparse-checkout add jans-config-api/plugins/docs \
    && cp ${JANS_SETUP_DIR}/static/rdbm/sql_data_types.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/ldap_sql_data_type_mapping.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/opendj_attributes_syntax.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/ \
    && cp -R agama/misc/* ${JETTY_BASE}/jans-auth/agama/ \
    && cp -R ${JANS_SETUP_DIR}/static/auth/conf /etc/certs \
    && mkdir -p /opt/jans/python/libs \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-lock /app/templates/ \
    && mkdir -p org/eclipse/jetty \
    && cp ${JANS_SETUP_DIR}/static/favicon.ico org/eclipse/jetty/favicon.ico \
    && zip -r ${JETTY_HOME}/lib/jetty-server-${JETTY_VERSION}.jar org/eclipse/jetty/favicon.ico \
    && cp jans-config-api/plugins/docs/lock-plugin-swagger.yaml /app/templates/jans-lock/

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
    PYTHON_HOME=/opt/jython \
    CN_DOCUMENT_STORE_TYPE=DB \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID=admin \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    CN_AUTH_JAVA_OPTIONS="" \
    CN_SYNC_JKS_ENABLED=false \
    CN_SYNC_JKS_INTERVAL=30 \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_JETTY_REQUEST_HEADER_SIZE=8192 \
    CN_JETTY_ARGS="" \
    CN_PROMETHEUS_PORT="" \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE="" \
    CN_AUTH_JETTY_PORT=8080 \
    CN_AUTH_JETTY_HOST=0.0.0.0 \
    CN_SHARE_AUTH_CONF=true \
    CN_LOCK_ENABLED=false \
    CN_SSL_CERT_FROM_SECRETS=true

# @TODO: revisit the usage (if any)
ENV ADMIN_UI_JWKS=http://${CN_AUTH_JETTY_HOST}:${CN_AUTH_JETTY_PORT}/jans-auth/restv1/jwks

# ==========
# misc stuff
# ==========

EXPOSE $CN_AUTH_JETTY_PORT

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/auth-server" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="1.4.0-1" \
    org.opencontainers.image.title="Janssen Authorization Server" \
    org.opencontainers.image.description="OAuth 2.0 server and client; OpenID Connect Provider (OP) & UMA Authorization Server (AS)"

RUN mkdir -p ${JETTY_BASE}/jans-auth/custom/pages \
    ${JETTY_BASE}/jans-auth/custom/static \
    ${JETTY_BASE}/jans-auth/custom/libs \
    ${JETTY_BASE}/jans-auth/custom/i18n \
    ${JETTY_BASE}/jans-auth/logs \
    ${JETTY_BASE}/common/libs \
    ${JETTY_HOME}/temp \
    /etc/jans/conf \
    /app/templates \
    /etc/certs

COPY templates /app/templates/
RUN cp /app/templates/jans-auth/jans-auth.xml ${JETTY_BASE}/jans-auth/webapps/ \
    && cp /app/templates/jans-auth/jans-auth_web_resources.xml ${JETTY_BASE}/jans-auth/webapps/ \
    && cp /app/templates/jans-auth/agama_web_resources.xml ${JETTY_BASE}/jans-auth/webapps/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

RUN sed -i 's/\(<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"\)\/\(>\)/\1\2<Set name="showContexts">false<\/Set><\/New>/' /opt/jetty/etc/jetty.xml

RUN ln -sf /usr/lib/jvm/jdk /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 jetty

# adjust ownership and permission
RUN chmod -R g=u ${JETTY_BASE}/jans-auth/custom \
    && chmod 664 ${JETTY_BASE}/jans-auth/resources/log4j2.xml \
    && chmod -R g=u ${JETTY_BASE}/jans-auth/logs \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /opt/java/lib/security/cacerts \
    && chown -R 1000:0 ${JETTY_BASE}/jans-auth/agama \
    && chown -R 1000:0 ${JETTY_BASE}/jans-auth/custom \
    && chown -R 1000:0 ${JETTY_BASE}/jans-auth/resources \
    && chown -R 1000:0 /opt/jans/python/libs \
    && chown -R 1000:0 ${JETTY_BASE}/common/libs \
    && chown -R 1000:0 /usr/share/java \
    && chown -R 1000:0 /opt/prometheus \
    && chown 1000:0 ${JETTY_BASE}/jans-auth/webapps/jans-auth.xml \
    && chown -R 1000:0 ${JETTY_HOME}/temp \
    && chown -R 1000:0 ${JETTY_BASE}/jans-auth/_libs \
    && chown -R 1000:0 /app/templates

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
