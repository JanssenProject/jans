# ==============
# Assets sources
# ==============

# original Janssen base version
ARG BASE_VERSION=1.4.0-1

# the following ARGs set default base images
# they can be overriden in build process via --build-arg option
ARG JANS_CONFIGURATOR_IMAGE=ghcr.io/janssenproject/jans/configurator:${BASE_VERSION}
ARG JANS_PERSISTENCE_LOADER_IMAGE=ghcr.io/janssenproject/jans/persistence-loader:${BASE_VERSION}
ARG JANS_AUTH_IMAGE=ghcr.io/janssenproject/jans/auth-server:${BASE_VERSION}
ARG JANS_CONFIG_API_IMAGE=ghcr.io/janssenproject/jans/config-api:${BASE_VERSION}
ARG JANS_FIDO2_IMAGE=ghcr.io/janssenproject/jans/fido2:${BASE_VERSION}
ARG JANS_SCIM_IMAGE=ghcr.io/janssenproject/jans/scim:${BASE_VERSION}
ARG JANS_CASA_IMAGE=ghcr.io/janssenproject/jans/casa:${BASE_VERSION}
ARG JANS_LINK_IMAGE=ghcr.io/janssenproject/jans/link:${BASE_VERSION}
ARG JANS_KEYCLOAK_LINK_IMAGE=ghcr.io/janssenproject/jans/keycloak-link:${BASE_VERSION}
ARG JANS_SAML_IMAGE=ghcr.io/janssenproject/jans/saml:${BASE_VERSION}

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

FROM ${JANS_KEYCLOAK_LINK_IMAGE} AS jans-keycloak-link-src

FROM ${JANS_SAML_IMAGE} AS jans-saml-src

# ===
# app
# ===

FROM bellsoft/liberica-openjdk-alpine:17.0.12@sha256:83719ee0075de03ecc7a99db489fd72a022e49f09c0e6e6350bcc05310c3caf6

# hadolint ignore=DL3018
RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache tini bash curl openssl python3 py3-cryptography py3-psycopg2 py3-grpcio nginx \
    && apk add --no-cache --virtual .build-deps git wget

# ===========
# Assets sync
# ===========

ENV JANS_SOURCE_VERSION=0d0794cf0cd68b95283fe7634d9890069278e60b

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the assets
ARG GIT_CLONE_DEPTH=100
RUN git clone --depth ${GIT_CLONE_DEPTH} --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout ${JANS_SOURCE_VERSION} \
    && git sparse-checkout add jans-pycloudlib

# ------
# Python
# ------

# default pip timeout
ARG PIP_TIMEOUT=15
COPY /app/requirements.txt /app/requirements.txt
RUN mv /usr/lib/python3.12/EXTERNALLY-MANAGED /usr/lib/python3.12/EXTERNALLY-MANAGED.disabled \
    && python3 -m ensurepip \
    && pip3 install --no-cache-dir -U pip wheel setuptools --timeout ${PIP_TIMEOUT} \
    && pip3 install --no-cache-dir -r /app/requirements.txt --timeout ${PIP_TIMEOUT} \
    && pip3 uninstall -y pip wheel

# -------
# Cleanup
# -------

RUN apk del .build-deps \
    && rm -rf /var/cache/apk/* /tmp/jans

# -------
# License
# -------

COPY LICENSE /licenses/LICENSE

# -----------
# assets sync
# -----------

RUN mkdir -p /app/bin /usr/share/java

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
COPY --from=jans-config-api-src /var/log/adminui /var/log/adminui
COPY --from=jans-config-api-src /opt/jans/bin /opt/jans/bin
RUN ln -sf /app/jans_aio/jans_config_api/entrypoint.sh /app/bin/jans-config-api-entrypoint.sh

COPY --from=jans-fido2-src /opt/jans/jetty/jans-fido2 /opt/jans/jetty/jans-fido2
COPY --from=jans-fido2-src /app/templates/jans-fido2 /app/templates/jans-fido2
COPY --from=jans-fido2-src /etc/jans/conf/fido2 /etc/jans/conf/fido2
COPY --from=jans-fido2-src /app/static/fido2 /app/static/fido2
COPY --from=jans-fido2-src /app/scripts /app/jans_aio/jans_fido2
RUN ln -sf /app/jans_aio/jans_fido2/entrypoint.sh /app/bin/jans-fido2-entrypoint.sh

COPY --from=jans-scim-src /opt/jans/jetty/jans-scim /opt/jans/jetty/jans-scim
COPY --from=jans-scim-src /app/templates/jans-scim /app/templates/jans-scim
COPY --from=jans-scim-src /app/scripts /app/jans_aio/jans_scim
RUN ln -sf /app/jans_aio/jans_scim/entrypoint.sh /app/bin/jans-scim-entrypoint.sh

COPY --from=jans-casa-src /opt/jans/jetty/jans-casa /opt/jans/jetty/jans-casa
COPY --from=jans-casa-src /app/templates/jans-casa /app/templates/jans-casa
COPY --from=jans-casa-src /app/scripts /app/jans_aio/jans_casa
COPY --from=jans-casa-src /usr/share/java/casa-agama-project.zip /usr/share/java
RUN ln -sf /app/jans_aio/jans_casa/entrypoint.sh /app/bin/jans-casa-entrypoint.sh

COPY --from=jans-link-src /opt/jans/jetty/jans-link /opt/jans/jetty/jans-link
COPY --from=jans-link-src /app/templates/jans-link /app/templates/jans-link
COPY --from=jans-link-src /app/scripts /app/jans_aio/jans_link
RUN ln -sf /app/jans_aio/jans_link/entrypoint.sh /app/bin/jans-link-entrypoint.sh

COPY --from=jans-keycloak-link-src /opt/jans/jetty/jans-keycloak-link /opt/jans/jetty/jans-keycloak-link
COPY --from=jans-keycloak-link-src /app/templates/jans-keycloak-link /app/templates/jans-keycloak-link
COPY --from=jans-keycloak-link-src /app/scripts /app/jans_aio/jans_keycloak_link
COPY --from=jans-keycloak-link-src /var/jans/cr-snapshots /var/jans/cr-snapshots
RUN ln -sf /app/jans_aio/jans_keycloak_link/entrypoint.sh /app/bin/jans-keycloak-link-entrypoint.sh

COPY --from=jans-saml-src /app/templates/jans-saml /app/templates/jans-saml
COPY --from=jans-saml-src /app/scripts /app/jans_aio/jans_saml
COPY --from=jans-saml-src /opt/idp /opt/idp
COPY --from=jans-saml-src /opt/keycloak /opt/keycloak
RUN ln -sf /app/jans_aio/jans_saml/entrypoint.sh /app/bin/jans-saml-entrypoint.sh

# ----
# misc
# ----

RUN ln -sf /usr/lib/jvm/jdk /opt/java

RUN mkdir -p /opt/jans/configurator/db \
    /opt/jans/configurator/certs \
    /opt/jetty/temp \
    /etc/nginx/jans-aio

COPY app /app

# CN version as env var (with suffix if any, i.e. SNAPSHOT)
ENV CN_VERSION=1.4.0

# set directory contains installer code that will be added to Python sys.path
ENV PYTHONPATH=/app

ENV JETTY_BASE=/opt/jans/jetty \
    JETTY_HOME=/opt/jetty \
    CN_AIO_COMPONENTS="configurator,persistence-loader,jans-auth,jans-config-api,jans-fido2,jans-scim,jans-casa" \
    CN_AIO_ENABLE_MONITOR=false \
    CN_ENABLE_STDOUT_LOG_PREFIX=true \
    CN_DUO_ENABLED=false \
    CN_AUTH_JAVA_OPTIONS="" \
    CN_CONFIG_API_JAVA_OPTIONS="" \
    CN_FIDO2_JAVA_OPTIONS="" \
    CN_SCIM_JAVA_OPTIONS=""  \
    CN_JETTY_REQUEST_HEADER_SIZE=8192 \
    CN_JETTY_IDLE_TIMEOUT=30000 \
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
    CN_KEYCLOAK_LINK_JETTY_HOST=127.0.0.1 \
    CN_KEYCLOAK_LINK_JETTY_PORT=9092 \
    CN_KEYCLOAK_LINK_JAVA_OPTIONS="" \
    CN_SAML_HTTP_HOST=127.0.0.1 \
    CN_SAML_HTTP_PORT=8083 \
    CN_SAML_JAVA_OPTIONS="" \
    CN_SAML_KC_ADMIN_CREDENTIALS_FILE=/etc/jans/conf/kc_admin_creds \
    CN_SAML_KC_DB_PASSWORD_FILE=/etc/jans/conf/kc_db_password \
    KC_LOG_LEVEL=INFO \
    KC_DB=mysql \
    KC_DB_USERNAME=keycloak \
    KC_DB_SCHEMA=keycloak \
    KC_DB_URL_HOST=localhost \
    KC_DB_URL_PORT=3306 \
    KC_DB_URL_DATABASE=keycloak \
    KC_DB_URL_PROPERTIES=?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4 \
    QUARKUS_TRANSACTION_MANAGER_ENABLE_RECOVERY=false

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/all-in-one" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="1.4.0-1" \
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
    ${JETTY_BASE}/jans-auth/agama \
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
    ${JETTY_BASE}/jans-keycloak-link/resources \
    ${JETTY_BASE}/jans-keycloak-link/logs \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /var/log/adminui \
    /etc/nginx/jans-aio \
    /usr/share/java \
    /var/jans/cr-snapshots \
    /opt/idp \
    /opt/keycloak/logs \
    /opt/keycloak/conf

RUN chmod -R g=u ${JETTY_HOME}/temp \
    ${JETTY_BASE}/common/libs \
    ${JETTY_BASE}/jans-auth/agama \
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
    ${JETTY_BASE}/jans-keycloak-link/resources \
    ${JETTY_BASE}/jans-keycloak-link/logs \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /var/log/adminui \
    /etc/nginx/jans-aio \
    /usr/share/java \
    /var/jans/cr-snapshots \
    /opt/idp \
    /opt/keycloak/logs \
    /opt/keycloak/conf

RUN chown 1000:1000 /opt/java/lib/security/cacerts \
    /run/nginx/nginx.pid \
    /etc/nginx/http.d/default.conf

# run as non-root user
USER 1000

RUN mkdir -p $HOME/.config/gcloud

WORKDIR /app

EXPOSE 8080

CMD ["sh", "/app/bin/entrypoint.sh"]
