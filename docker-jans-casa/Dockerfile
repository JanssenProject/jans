FROM bellsoft/liberica-openjre-alpine:17.0.12@sha256:29cb2ee552c7c7a924b6a1b59802508dc5123e7edad1d65d575bbf07cd05fa6d

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache python3 openssl tini py3-cryptography py3-psycopg2 py3-grpcio curl \
    && apk add --no-cache --virtual .build-deps git wget zip

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

# ====
# Casa
# ====

ENV CN_VERSION=0.0.0-nightly
ENV CN_BUILD_DATE='2025-01-29 09:19'

ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/casa/${CN_VERSION}/casa-${CN_VERSION}.war

# Install Casa
COPY static/jetty-env.xml /tmp/WEB-INF/jetty-env.xml
RUN mkdir -p ${JETTY_BASE}/jans-casa/webapps \
    && wget -q ${CN_SOURCE_URL} -O /tmp/jans-casa.war \
    && cd /tmp \
    && zip -d jans-casa.war WEB-INF/jetty-web.xml \
    && zip -r jans-casa.war WEB-INF/jetty-env.xml \
    && cp jans-casa.war ${JETTY_BASE}/jans-casa/webapps/jans-casa.war \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-casa --add-module=server,deploy,resources,http,jsp,cdi-decorate,jmx,stats,logging-log4j2 --approve-all-licenses \
    && rm -rf /tmp/jans-casa.war /tmp/WEB-INF

# casa plugins
RUN mkdir -p ${JETTY_BASE}/jans-casa/plugins \
    && for casa_plugin in acct-linking authorized-clients bioid-plugin custom-branding strong-authn-settings; \
    do \
        wget -nv "https://jenkins.jans.io/maven/io/jans/casa/plugins/${casa_plugin}/${CN_VERSION}/${casa_plugin}-${CN_VERSION}-jar-with-dependencies.jar" -O "${JETTY_BASE}/jans-casa/plugins/${casa_plugin}-${CN_VERSION}.jar"; \
    done

# casa agama
RUN mkdir -p /usr/share/java \
    && wget -q https://maven.jans.io/maven/io/jans/casa-agama/${CN_VERSION}/casa-agama-${CN_VERSION}-project.zip -O /usr/share/java/casa-agama-project.zip

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=4ef6c9adb2e71b5b5c0dbacfc8716c452f77f2d2
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

RUN mkdir -p /app/static/rdbm \
    /app/schema \
    /app/templates/jans-casa

# sync static files from linux-setup
RUN cd /tmp/jans \
    && cp ${JANS_SETUP_DIR}/static/rdbm/sql_data_types.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/ldap_sql_data_type_mapping.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/static/rdbm/opendj_attributes_syntax.json /app/static/rdbm/ \
    && cp ${JANS_SETUP_DIR}/schema/jans_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/custom_schema.json /app/schema/ \
    && cp ${JANS_SETUP_DIR}/schema/opendj_types.json /app/schema/ \
    && cp -R ${JANS_SETUP_DIR}/templates/jans-casa/* /app/templates/jans-casa/ \
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
    PYTHON_HOME=/opt/jython \
    CN_DOCUMENT_STORE_TYPE=DB \
    CN_JACKRABBIT_URL=http://localhost:8080 \
    CN_JACKRABBIT_ADMIN_ID=admin \
    CN_JACKRABBIT_ADMIN_PASSWORD_FILE=/etc/jans/conf/jackrabbit_admin_password \
    CN_CASA_JAVA_OPTIONS="" \
    GOOGLE_PROJECT_ID="" \
    CN_GOOGLE_SECRET_MANAGER_PASSPHRASE=secret \
    CN_GOOGLE_SECRET_VERSION_ID=latest \
    CN_GOOGLE_SECRET_NAME_PREFIX=jans \
    CN_CASA_ADMIN_LOCK_FILE=/opt/jans/jetty/jans-casa/resources/.administrable \
    CN_PROMETHEUS_PORT="" \
    CN_AWS_SECRETS_ENDPOINT_URL="" \
    CN_AWS_SECRETS_PREFIX=jans \
    CN_AWS_SECRETS_REPLICA_FILE="" \
    CN_CASA_JWKS_SIZE_LIMIT=100000 \
    CN_CASA_JETTY_PORT=8080 \
    CN_CASA_JETTY_HOST=0.0.0.0 \
    CN_SHARE_AUTH_CONF=true \
    CN_JETTY_ARGS="" \
    CN_SSL_CERT_FROM_SECRETS=true

# ==========
# misc stuff
# ==========

EXPOSE $CN_CASA_JETTY_PORT

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/casa" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Janssen Casa" \
    org.opencontainers.image.description="Self-service portal for people to manage their account security preferences in the Janssen, like 2FA"

RUN mkdir -p /opt/jans/python/libs \
    ${JETTY_BASE}/jans-casa/static \
    ${JETTY_BASE}/jans-casa/plugins \
    ${JETTY_BASE}/jans-casa/logs \
    ${JETTY_BASE}/common/libs \
    ${JETTY_HOME}/temp \
    /etc/jans/conf/casa \
    /etc/certs

COPY templates /app/templates/
RUN cp /app/templates/jans-casa/jans-casa.xml ${JETTY_BASE}/jans-casa/webapps/ \
    && cp /app/templates/jans-casa/jans-casa_web_resources.xml ${JETTY_BASE}/jans-casa/webapps/
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

RUN sed -i 's/\(<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"\)\/\(>\)/\1\2<Set name="showContexts">false<\/Set><\/New>/' /opt/jetty/etc/jetty.xml

RUN ln -sf /usr/lib/jvm/jre /opt/java

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -G root -u 1000 jetty

# adjust ownership and permission
RUN chmod -R g=u ${JETTY_BASE}/jans-casa/static \
    && chmod -R g=u ${JETTY_BASE}/jans-casa/plugins \
    && chown -R 1000:0 ${JETTY_BASE}/jans-casa/resources \
    && chmod 664 ${JETTY_BASE}/jans-casa/resources/log4j2.xml \
    && chmod -R g=u ${JETTY_BASE}/jans-casa/logs \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /opt/java/lib/security/cacerts \
    && chown -R 1000:0 ${JETTY_BASE}/common/libs \
    && chown -R 1000:0 /usr/share/java \
    && chown -R 1000:0 /opt/prometheus \
    && chown 1000:0 ${JETTY_BASE}/jans-casa/webapps/jans-casa.xml \
    && chown -R 1000:0 ${JETTY_HOME}/temp \
    && chown -R 1000:0 /app/templates/jans-casa

USER 1000

RUN mkdir -p $HOME/.config/gcloud

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
