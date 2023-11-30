# ==============
# Assets sources
# ==============

# the following ARGs set default base images
# they can be overriden in build process via --build-arg option
ARG JANS_CONFIGURATOR_IMAGE=ghcr.io/janssenproject/jans/configurator:1.0.21_dev
ARG JANS_PERSISTENCE_LOADER_IMAGE=ghcr.io/janssenproject/jans/persistence-loader:1.0.21_dev
ARG JANS_AUTH_IMAGE=ghcr.io/janssenproject/jans/auth-server:1.0.21_dev
ARG JANS_CONFIG_API_IMAGE=ghcr.io/janssenproject/jans/config-api:1.0.21_dev
ARG JANS_FIDO2_IMAGE=ghcr.io/janssenproject/jans/fido2:1.0.21_dev
ARG JANS_SCIM_IMAGE=ghcr.io/janssenproject/jans/scim:1.0.21_dev
ARG JANS_CASA_IMAGE=ghcr.io/janssenproject/jans/casa:1.0.21_dev
ARG JANS_LINK_IMAGE=ghcr.io/janssenproject/jans/link:1.0.21_dev

# original Janssen version
ARG CN_VERSION=1.0.21-SNAPSHOT

# -----------
# base images
# -----------

FROM ${JANS_CONFIGURATOR_IMAGE} AS jans-configurator-src

FROM ${JANS_PERSISTENCE_LOADER_IMAGE} AS jans-persistence-loader-src

FROM ${JANS_AUTH_IMAGE} AS jans-auth-src

FROM ${JANS_CONFIG_API_IMAGE} AS jans-config-api-src

FROM ${JANS_FIDO2_IMAGE} AS jans-fido2-src

FROM ${JANS_SCIM_IMAGE} AS jans-scim-src

FROM ${JANS_CASA_IMAGE} AS jans-casa-src

FROM ${JANS_LINK_IMAGE} AS jans-link-src

# ===
# app
# ===

FROM bellsoft/liberica-openjdk-alpine:20-37@sha256:96fd701114a6809ce0ab68a63014d2c195ee8d504922bcdd5590b9710c4e2136

# hadolint ignore=DL3018
RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache tini bash curl openssl python3 py3-cryptography py3-psycopg2 py3-grpcio nginx \
    && apk add --no-cache --virtual .build-deps git wget

# ------
# Python
# ------

COPY /app/requirements.txt /app/requirements.txt
RUN python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools \
    && pip3 install --no-cache-dir --default-timeout=300 -r /app/requirements.txt \
    && pip3 uninstall -y pip wheel

# -------
# Cleanup
# -------

RUN apk del .build-deps \
    && rm -rf /var/cache/apk/*

# -------
# License
# -------

COPY LICENSE /licenses/LICENSE

# -----------
# assets sync
# -----------

RUN mkdir -p /app/bin

COPY --from=jans-configurator-src /opt/jans/configurator/javalibs /opt/jans/configurator/javalibs
COPY --from=jans-configurator-src /app/scripts /app/jans_aio/configurator
RUN ln -sf /app/jans_aio/configurator/entrypoint.sh /app/bin/configurator-entrypoint.sh

COPY --from=jans-persistence-loader-src /app/templates /app/templates
COPY --from=jans-persistence-loader-src /app/schema /app/schema
COPY --from=jans-persistence-loader-src /app/static /app/static
COPY --from=jans-persistence-loader-src /app/script-catalog /app/script-catalog
COPY --from=jans-persistence-loader-src /app/scripts /app/jans_aio/persistence_loader
RUN ln -sf /app/jans_aio/persistence_loader/entrypoint.sh /app/bin/persistence-loader-entrypoint.sh

COPY --from=jans-auth-src /opt/jetty /opt/jetty
COPY --from=jans-auth-src /opt/jython /opt/jython
COPY --from=jans-auth-src /opt/jans/jetty /opt/jans/jetty
COPY --from=jans-auth-src /etc/certs /etc/certs
COPY --from=jans-auth-src /opt/jans/python/libs /opt/jans/python/libs
COPY --from=jans-auth-src /app/templates /app/templates
COPY --from=jans-auth-src /opt/prometheus /opt/prometheus
COPY --from=jans-auth-src /app/scripts /app/jans_aio/jans_auth
RUN ln -sf /app/jans_aio/jans_auth/entrypoint.sh /app/bin/jans-auth-entrypoint.sh

COPY --from=jans-config-api-src /opt/jans/jetty/jans-config-api /opt/jans/jetty/jans-config-api
COPY --from=jans-config-api-src /app/templates/jans-config-api /app/templates/jans-config-api
COPY --from=jans-config-api-src /usr/bin/facter /usr/local/bin/facter
COPY --from=jans-config-api-src /app/scripts /app/jans_aio/jans_config_api
RUN ln -sf /app/jans_aio/jans_config_api/entrypoint.sh /app/bin/jans-config-api-entrypoint.sh

COPY --from=jans-fido2-src /opt/jans/jetty/jans-fido2 /opt/jans/jetty/jans-fido2
COPY --from=jans-fido2-src /app/templates/jans-fido2 /app/templates/jans-fido2
COPY --from=jans-fido2-src /etc/jans/conf/fido2 /etc/jans/conf/fido2
COPY --from=jans-fido2-src /app/scripts /app/jans_aio/jans_fido2
RUN ln -sf /app/jans_aio/jans_fido2/entrypoint.sh /app/bin/jans-fido2-entrypoint.sh

COPY --from=jans-scim-src /opt/jans/jetty/jans-scim /opt/jans/jetty/jans-scim
COPY --from=jans-scim-src /app/templates/jans-scim /app/templates/jans-scim
COPY --from=jans-scim-src /app/scripts /app/jans_aio/jans_scim
RUN ln -sf /app/jans_aio/jans_scim/entrypoint.sh /app/bin/jans-scim-entrypoint.sh

COPY --from=jans-casa-src /opt/jans/jetty/jans-casa /opt/jans/jetty/jans-casa
COPY --from=jans-casa-src /app/templates/jans-casa /app/templates/jans-casa
COPY --from=jans-casa-src /app/scripts /app/jans_aio/jans_casa
COPY --from=jans-casa-src /app/static/extension/person_authentication/Casa.py /app/static/extension/person_authentication/Casa.py
RUN ln -sf /app/jans_aio/jans_casa/entrypoint.sh /app/bin/jans-casa-entrypoint.sh

COPY --from=jans-link-src /opt/jans/jetty/jans-link /opt/jans/jetty/jans-link
COPY --from=jans-link-src /app/templates/jans-link /app/templates/jans-link
COPY --from=jans-link-src /app/scripts /app/jans_aio/jans_link
RUN ln -sf /app/jans_aio/jans_link/entrypoint.sh /app/bin/jans-link-entrypoint.sh

# ----
# misc
# ----

RUN ln -sf /usr/lib/jvm/jdk /opt/java

RUN mkdir -p /opt/jans/configurator/db \
    /opt/jans/configurator/certs \
    /opt/jetty/temp \
    /etc/nginx/jans-aio \
    /usr/share/java

COPY app /app

# set directory contains installer code that will be added to Python sys.path
ENV PYTHONPATH=/app

ENV JETTY_BASE=/opt/jans/jetty \
    JETTY_HOME=/opt/jetty \
    CN_AIO_COMPONENTS="" \
    CN_AIO_ENABLE_MONITOR=false \
    CN_ENABLE_STDOUT_LOG_PREFIX=true \
    CN_DUO_ENABLED=false \
    CN_AUTH_JAVA_OPTIONS="" \
    CN_CONFIG_API_JAVA_OPTIONS="" \
    CN_FIDO2_JAVA_OPTIONS="" \
    CN_SCIM_JAVA_OPTIONS=""  \
    CN_JETTY_REQUEST_HEADER_SIZE=8192 \
    CN_CONFIG_API_CREATE_SCOPES=true \
    CN_AUTH_JETTY_HOST=127.0.0.1 \
    CN_AUTH_JETTY_PORT=8081 \
    CN_CONFIG_API_JETTY_HOST=127.0.0.1 \
    CN_CONFIG_API_JETTY_PORT=8074 \
    CN_FIDO2_JETTY_HOST=127.0.0.1 \
    CN_FIDO2_JETTY_PORT=8073 \
    CN_SCIM_JETTY_HOST=127.0.0.1 \
    CN_SCIM_JETTY_PORT=8087 \
    CN_CASA_JETTY_HOST=127.0.0.1 \
    CN_CASA_JETTY_PORT=8082 \
    CN_CASA_ADMIN_LOCK_FILE=/opt/jans/jetty/jans-casa/resources/.administrable \
    CN_CASA_JWKS_SIZE_LIMIT=100000 \
    CN_CASA_JAVA_OPTIONS="" \
    CN_LINK_JETTY_HOST=127.0.0.1 \
    CN_LINK_JETTY_PORT=9091 \
    CN_LINK_JAVA_OPTIONS="" \
    CN_SHARE_AUTH_CONF=false \
    CN_SQL_PASSWORD_FILE=/etc/jans/conf/sql_password \
    CN_COUCHBASE_PASSWORD_FILE=/etc/jans/conf/couchbase_password \
    CN_COUCHBASE_SUPERUSER_PASSWORD_FILE=/etc/jans/conf/couchbase_superuser_password \
    CN_LDAP_PASSWORD_FILE=/etc/jans/conf/ldap_password \
    CN_LDAP_TRUSTSTORE_PASSWORD_FILE=/etc/jans/conf/ldap_truststore_password

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/all-in-one" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="1.0.21" \
    org.opencontainers.image.title="Janssen All-in-One" \
    org.opencontainers.image.description=""

# forward logs to stdout and stderr
RUN ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

RUN cp /app/templates/nginx/*-upstream.conf /etc/nginx/jans-aio/ \
    && cp /app/templates/nginx/*-location.conf /etc/nginx/jans-aio

RUN touch /run/nginx/nginx.pid

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -u 1000 jans \
    && addgroup jans root

RUN chmod +x /app/bin/entrypoint.sh

# adjust ownership and permission
RUN chown -R 1000:1000 ${JETTY_HOME}/temp \
    ${JETTY_BASE}/common/libs \
    ${JETTY_BASE}/jans-auth/custom \
    ${JETTY_BASE}/jans-auth/resources \
    ${JETTY_BASE}/jans-auth/logs \
    ${JETTY_BASE}/jans-config-api/custom \
    ${JETTY_BASE}/jans-config-api/resources \
    ${JETTY_BASE}/jans-config-api/logs \
    ${JETTY_BASE}/jans-config-api/_plugins \
    ${JETTY_BASE}/jans-fido2/custom \
    ${JETTY_BASE}/jans-fido2/resources \
    ${JETTY_BASE}/jans-fido2/logs \
    ${JETTY_BASE}/jans-scim/custom \
    ${JETTY_BASE}/jans-scim/resources \
    ${JETTY_BASE}/jans-scim/logs \
    ${JETTY_BASE}/jans-casa/static \
    ${JETTY_BASE}/jans-casa/plugins \
    ${JETTY_BASE}/jans-casa/resources \
    ${JETTY_BASE}/jans-casa/logs \
    ${JETTY_BASE}/jans-link/resources \
    ${JETTY_BASE}/jans-link/logs \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /etc/nginx/jans-aio \
    /usr/share/java

RUN chmod -R g=u ${JETTY_HOME}/temp \
    ${JETTY_BASE}/common/libs \
    ${JETTY_BASE}/jans-auth/custom \
    ${JETTY_BASE}/jans-auth/resources \
    ${JETTY_BASE}/jans-auth/logs \
    ${JETTY_BASE}/jans-config-api/custom \
    ${JETTY_BASE}/jans-config-api/resources \
    ${JETTY_BASE}/jans-config-api/logs \
    ${JETTY_BASE}/jans-config-api/_plugins \
    ${JETTY_BASE}/jans-fido2/custom \
    ${JETTY_BASE}/jans-fido2/resources \
    ${JETTY_BASE}/jans-fido2/logs \
    ${JETTY_BASE}/jans-scim/custom \
    ${JETTY_BASE}/jans-scim/resources \
    ${JETTY_BASE}/jans-scim/logs \
    ${JETTY_BASE}/jans-casa/static \
    ${JETTY_BASE}/jans-casa/plugins \
    ${JETTY_BASE}/jans-casa/resources \
    ${JETTY_BASE}/jans-casa/logs \
    ${JETTY_BASE}/jans-link/resources \
    ${JETTY_BASE}/jans-link/logs \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /etc/nginx/jans-aio \
    /usr/share/java

RUN chown 1000:1000 /opt/java/lib/security/cacerts \
    /run/nginx/nginx.pid \
    /etc/nginx/http.d/default.conf

# run as non-root user
USER 1000

RUN mkdir -p $HOME/.config/gcloud

WORKDIR /app

EXPOSE 8080

CMD ["sh", "/app/bin/entrypoint.sh"]
