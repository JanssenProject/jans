FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

# symlink JVM
RUN mkdir -p /usr/lib/jvm/default-jvm /usr/java/latest \
    && ln -sf /opt/java/openjdk /usr/lib/jvm/default-jvm/jre \
    && ln -sf /usr/lib/jvm/default-jvm/jre /usr/java/latest/jre

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache openssl py3-pip tini curl \
    && apk add --no-cache --virtual build-deps wget git gcc musl-dev python3-dev libffi-dev openssl-dev

# ==========
# Config API
# ==========

ENV CN_VERSION=1.0.0-SNAPSHOT
ENV CN_BUILD_DATE='2020-12-23 13:18'
ENV CN_SOURCE_URL=https://maven.jans.io/maven/io/jans/jans-config-api/${CN_VERSION}/jans-config-api-${CN_VERSION}-runner.jar

RUN mkdir -p /opt/config-api \
    && wget -q ${CN_SOURCE_URL} -O /opt/config-api/jans-config-api-runner.jar

EXPOSE 8074
EXPOSE 9444

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
    CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG=false

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
    CN_SECRET_KUBERNETES_USE_KUBE_CONFIG=false

# ===============
# Persistence ENV
# ===============

ENV CN_PERSISTENCE_TYPE=ldap \
    CN_PERSISTENCE_LDAP_MAPPING=default \
    CN_LDAP_URL=localhost:1636 \
    CN_COUCHBASE_URL=localhost \
    CN_COUCHBASE_USER=admin \
    CN_COUCHBASE_CERT_FILE=/etc/certs/couchbase.crt \
    CN_COUCHBASE_PASSWORD_FILE=/etc/jans/conf/couchbase_password \
    CN_COUCHBASE_CONN_TIMEOUT=10000 \
    CN_COUCHBASE_CONN_MAX_WAIT=20000 \
    CN_COUCHBASE_SCAN_CONSISTENCY=not_bounded

# ===========
# Generic ENV
# ===========

ENV CN_MAX_RAM_PERCENTAGE=75.0 \
    CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_JAVA_OPTIONS="" \
    CN_SSL_CERT_FROM_SECRETS=false \
    CN_NAMESPACE=jans

# ====
# misc
# ====

LABEL name="Config API" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen" \
    version="1.0.0" \
    release="a1" \
    summary="Janssen Config API" \
    description=""

RUN mkdir -p /etc/certs /app/templates/ /deploy /etc/jans/conf
COPY conf/*.tmpl /app/templates/
COPY scripts /app/scripts
RUN touch /etc/hosts.back config_api.log jans-config-api-access.log
RUN chmod +x /app/scripts/entrypoint.sh

# # create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 1000

 # adjust ownership
RUN chown -R 1000:1000 /etc/jans \
    && chown -R 1000:1000 /deploy \
    && chown -R 1000:1000 /tmp \
    && chown -R 1000:1000 /etc/hosts.back \
    && chown -R 1000:1000 config_api.log \
    && chown -R 1000:1000 jans-config-api-access.log \
    && chgrp -R 0 /etc/hosts.back && chmod -R g=u /etc/hosts.back \
    && chgrp -R 0 config_api.log && chmod -R g=u config_api.log \
    && chgrp -R 0 jans-config-api-access.log && chmod -R g=u jans-config-api-access.log \
    && chgrp -R 0 /tmp && chmod -R g=u /tmp \
    && chgrp -R 0 /deploy && chmod -R g=u /deploy \
    && chgrp -R 0 /etc/certs && chmod -R g=u /etc/certs \
    && chgrp -R 0 /etc/jans && chmod -R g=u /etc/jans
USER 1000


ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["sh", "/app/scripts/entrypoint.sh"]
