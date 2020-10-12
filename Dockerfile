FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

# symlink JVM
RUN mkdir -p /usr/lib/jvm/default-jvm /usr/java/latest \
    && ln -sf /opt/java/openjdk /usr/lib/jvm/default-jvm/jre \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip tini curl bash \
    && apk add --no-cache --virtual build-deps wget git

# =====
# Jetty
# =====

ARG JETTY_VERSION=9.4.26.v20200117
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/gluu/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/${JETTY_VERSION}/jetty-distribution-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-distribution-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# Ports required by jetty
EXPOSE 8080

# ======
# Jython
# ======

ARG JYTHON_VERSION=2.7.2
RUN wget -q https://ox.gluu.org/dist/jython/${JYTHON_VERSION}/jython-installer-${JYTHON_VERSION}.jar -O /tmp/jython-installer.jar \
    && mkdir -p /opt/jython \
    && java -jar /tmp/jython-installer.jar -v -s -d /opt/jython \
    && rm -f /tmp/jython-installer.jar /tmp/*.properties

# ====
# SCIM
# ====

ENV JANS_VERSION=4.2.1.Final
ENV JANS_BUILD_DATE="2020-09-24 08:32"

# Install SCIM
RUN wget -q https://ox.gluu.org/maven/org/gluu/scim-server/${JANS_VERSION}/scim-server-${JANS_VERSION}.war -O /tmp/scim.war \
    && mkdir -p ${JETTY_BASE}/scim/webapps/scim \
    && unzip -qq /tmp/scim.war -d ${JETTY_BASE}/scim/webapps/scim \
    && java -jar ${JETTY_HOME}/start.jar jetty.home=${JETTY_HOME} jetty.base=${JETTY_BASE}/scim --add-to-start=server,deploy,resources,http,http-forwarded,jsp,websocket \
    && rm -f /tmp/scim.war

# ======
# Python
# ======

RUN apk add --no-cache py3-cryptography
COPY requirements.txt /app/requirements.txt
RUN pip3 install -U pip wheel \
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

# ===========
# Generic ENV
# ===========

ENV JANS_MAX_RAM_PERCENTAGE=75.0 \
    JANS_WAIT_MAX_TIME=300 \
    JANS_WAIT_SLEEP_DURATION=10 \
    JANS_JAVA_OPTIONS="" \
    GLUU_SSL_CERT_FROM_SECRETS=false

# ==========
# misc stuff
# ==========

LABEL name="SCIM" \
    maintainer="Gluu Inc. <support@gluu.org>" \
    vendor="Gluu Federation" \
    version="4.2.1" \
    release="02" \
    summary="Gluu SCIM" \
    description="SCIM server"

RUN mkdir -p /etc/certs /deploy \
    /etc/gluu/conf \
    /app/templates

COPY jetty/*.xml ${JETTY_BASE}/scim/webapps/
COPY conf/*.tmpl /app/templates/

COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
