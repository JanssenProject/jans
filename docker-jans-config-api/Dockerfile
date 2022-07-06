FROM bellsoft/liberica-openjre-alpine:11.0.15

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade \
    && apk add --no-cache openssl python3 tini curl py3-cryptography py3-psycopg2 py3-grpcio \
    && apk add --no-cache --virtual .build-deps wget git zip \
    && mkdir -p /usr/java/latest \
    && ln -sf /usr/lib/jvm/jre /usr/java/latest/jre

# =====
# Jetty
# =====

ARG JETTY_VERSION=11.0.8
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/jans/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-home-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# ==========
# Config API
# ==========

ENV CN_VERSION=1.0.2-SNAPSHOT
ENV CN_BUILD_DATE='2022-07-06 12:54'
ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-config-api-server/${CN_VERSION}/jans-config-api-server-${CN_VERSION}.war

# Install Jans Config API
COPY jetty/jetty-env.xml /tmp/WEB-INF/jetty-env.xml
RUN mkdir -p ${JETTY_BASE}/jans-config-api/webapps \
    && wget -q ${CN_SOURCE_URL} -O /tmp/jans-config-api.war \
    && cd /tmp \
    && zip -d jans-config-api.war WEB-INF/jetty-web.xml \
    && zip -r jans-config-api.war WEB-INF/jetty-env.xml \
    && cp jans-config-api.war ${JETTY_BASE}/jans-config-api/webapps/jans-config-api.war \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-config-api --add-module=server,deploy,annotations,resources,http,http-forwarded,threadpool,jsp,websocket,cdi-decorate \
    && rm -rf /tmp/jans-config-api.war /tmp/WEB-INF

# Ports exposed by config-api
EXPOSE 8074

# ======
# Facter
# ======

ARG PYFACTER_VERSION=9d8478ee47dc5498a766e010e8d3a3451b46e541
RUN wget -q https://github.com/GluuFederation/gluu-snap/raw/${PYFACTER_VERSION}/facter/facter -O /usr/bin/facter \
    && chmod +x /usr/bin/facter

# =======
# Plugins
# =======

RUN mkdir -p /usr/share/java

ENV SCIM_PLUGIN_BUILD_DATE='2022-07-06 12:55'
ENV SCIM_PLUGIN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-config-api/plugins/scim-plugin/${CN_VERSION}/scim-plugin-${CN_VERSION}-distribution.jar
RUN wget -q ${SCIM_PLUGIN_SOURCE_URL} -O /usr/share/java/scim-plugin.jar

ENV ADMIN_UI_PLUGIN_BUILD_DATE='2022-07-06 12:54'
ENV ADMIN_UI_PLUGIN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-config-api/plugins/admin-ui-plugin/${CN_VERSION}/admin-ui-plugin-${CN_VERSION}-distribution.jar
RUN wget -q ${ADMIN_UI_PLUGIN_SOURCE_URL} -O /usr/share/java/admin-ui-plugin.jar

ENV FIDO2_PLUGIN_BUILD_DATE='2022-07-06 12:55'
ENV FIDO2_PLUGIN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-config-api/plugins/fido2-plugin/${CN_VERSION}/fido2-plugin-${CN_VERSION}-distribution.jar
RUN wget -q ${FIDO2_PLUGIN_SOURCE_URL} -O /usr/share/java/fido2-plugin.jar

ENV USER_MGT_PLUGIN_BUILD_DATE='2022-07-06 12:55'
ENV USER_MGT_PLUGIN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-config-api/plugins/user-mgt-plugin/${CN_VERSION}/user-mgt-plugin-${CN_VERSION}-distribution.jar
RUN wget -q ${USER_MGT_PLUGIN_SOURCE_URL} -O /usr/share/java/user-mgt-plugin.jar

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

ARG PROMETHEUS_JAVAAGENT_VERSION=0.17.0
COPY conf/prometheus-config.yaml /opt/prometheus/
RUN mkdir -p /opt/prometheus \
    && wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${PROMETHEUS_JAVAAGENT_VERSION}/jmx_prometheus_javaagent-${PROMETHEUS_JAVAAGENT_VERSION}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/jans-config-api --add-module=jmx,stats

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
    CN_JAVA_OPTIONS="" \
    GOOGLE_PROJECT_ID="" \
    GOOGLE_APPLICATION_CREDENTIALS=/etc/jans/conf/google-credentials.json \
    CN_PROMETHEUS_PORT=""

# ====
# misc
# ====

LABEL name="janssenproject/config-api" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.2" \
    release="1" \
    summary="Janssen Config API" \
    description=""

RUN mkdir -p /etc/certs \
    /app/templates/ \
    /etc/jans/conf \
    ${JETTY_BASE}/jans-config-api/custom/libs \
    ${JETTY_BASE}/jans-config-api/custom/config \
    ${JETTY_BASE}/jans-config-api/logs

RUN touch /etc/hosts.back
COPY jetty/log4j2.xml ${JETTY_BASE}/jans-config-api/resources/
COPY conf/*.tmpl /app/templates/
COPY plugins /app/plugins
COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 jetty

COPY --chown=1000:0 jetty/jans-config-api.xml ${JETTY_BASE}/jans-config-api/webapps/

# adjust ownership and permission
RUN chmod -R g=u ${JETTY_BASE}/jans-config-api/custom \
    && chmod -R g=u ${JETTY_BASE}/jans-config-api/resources \
    && chmod -R g=u ${JETTY_BASE}/jans-config-api/logs \
    && chmod -R g=u /etc/certs \
    && chmod -R g=u /etc/jans \
    && chmod 664 /etc/hosts.back \
    && chmod 664 /usr/java/latest/jre/lib/security/cacerts \
    && chmod 664 /opt/jetty/etc/jetty.xml \
    && chmod 664 /opt/jetty/etc/webdefault.xml

USER 1000

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
