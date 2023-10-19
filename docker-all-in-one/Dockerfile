# ==============
# Assets sources
# ==============

# the following ARGs set default base images
# they can be overriden in build process via --build-arg option
ARG JANS_CONFIGURATOR_IMAGE=ghcr.io/janssenproject/jans/configurator:1.0.20_dev
ARG JANS_PERSISTENCE_LOADER_IMAGE=ghcr.io/janssenproject/jans/persistence-loader:1.0.20_dev
ARG JANS_AUTH_IMAGE=ghcr.io/janssenproject/jans/auth-server:1.0.20_dev
ARG JANS_CONFIG_API_IMAGE=ghcr.io/janssenproject/jans/config-api:1.0.20_dev
ARG JANS_FIDO2_IMAGE=ghcr.io/janssenproject/jans/fido2:1.0.20_dev
ARG JANS_SCIM_IMAGE=ghcr.io/janssenproject/jans/scim:1.0.20_dev
ARG FLEX_CASA_IMAGE=ghcr.io/gluufederation/flex/casa:1.0.20_dev
ARG FLEX_ADMIN_UI_IMAGE=ghcr.io/gluufederation/flex/admin-ui:1.0.20_dev

# original Janssen version
ARG CN_VERSION=1.0.20-SNAPSHOT

# -----------
# base images
# -----------

FROM ${JANS_CONFIGURATOR_IMAGE} AS jans-configurator-src

FROM ${JANS_PERSISTENCE_LOADER_IMAGE} AS jans-persistence-loader-src

FROM ${JANS_AUTH_IMAGE} AS jans-auth-src

FROM ${JANS_CONFIG_API_IMAGE} AS jans-config-api-src

FROM ${JANS_FIDO2_IMAGE} AS jans-fido2-src

FROM ${JANS_SCIM_IMAGE} AS jans-scim-src

FROM ${FLEX_CASA_IMAGE} AS flex-casa-src

FROM ${FLEX_ADMIN_UI_IMAGE} AS flex-admin-ui-src

# ===
# app
# ===

FROM bellsoft/liberica-openjdk-alpine:11.0.19

# hadolint ignore=DL3018
RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache tini bash curl openssl python3 py3-cryptography py3-psycopg2 py3-grpcio nodejs npm nginx \
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
COPY --from=jans-configurator-src /app/scripts /app/flex_aio/configurator
RUN ln -sf /app/flex_aio/configurator/entrypoint.sh /app/bin/configurator-entrypoint.sh

COPY --from=jans-persistence-loader-src /app/templates /app/templates
COPY --from=jans-persistence-loader-src /app/schema /app/schema
COPY --from=jans-persistence-loader-src /app/static /app/static
COPY --from=jans-persistence-loader-src /app/script-catalog /app/script-catalog
COPY --from=jans-persistence-loader-src /app/scripts /app/flex_aio/persistence_loader
RUN ln -sf /app/flex_aio/persistence_loader/entrypoint.sh /app/bin/persistence-loader-entrypoint.sh

COPY --from=jans-auth-src /opt/jetty /opt/jetty
COPY --from=jans-auth-src /opt/jython /opt/jython
COPY --from=jans-auth-src /opt/jans/jetty/jans-auth /opt/jans/jetty/jans-auth
COPY --from=jans-auth-src /opt/jans/jetty/common /opt/jans/jetty/common
COPY --from=jans-auth-src /usr/share/java /opt/jans/jetty/jans-auth/custom/libs
COPY --from=jans-auth-src /etc/certs /etc/certs
COPY --from=jans-auth-src /opt/jans/python/libs /opt/jans/python/libs
COPY --from=jans-auth-src /app/templates/jans-couchbase.properties.tmpl /app/templates/etc/jans/conf/jans-couchbase.properties
COPY --from=jans-auth-src /app/templates/jans-ldap.properties.tmpl /app/templates/etc/jans/conf/jans-ldap.properties
COPY --from=jans-auth-src /app/templates/jans-mysql.properties.tmpl /app/templates/etc/jans/conf/jans-mysql.properties
COPY --from=jans-auth-src /app/templates/jans-pgsql.properties.tmpl /app/templates/etc/jans/conf/jans-pgsql.properties
COPY --from=jans-auth-src /app/templates/jans-spanner.properties.tmpl /app/templates/etc/jans/conf/jans-spanner.properties
COPY --from=jans-auth-src /app/templates/jans.properties.tmpl /app/templates/etc/jans/conf/jans.properties
COPY --from=jans-auth-src /app/templates/salt.tmpl /app/templates/etc/jans/conf/salt
COPY --from=jans-auth-src /app/templates/log4j2.xml /app/templates/opt/jans/jetty/jans-auth/resources/log4j2.xml
COPY --from=jans-auth-src /opt/jans/jetty/jans-auth/webapps/jans-auth.xml /app/templates/opt/jans/jetty/jans-auth/webapps/jans-auth.xml
COPY --from=jans-auth-src /opt/prometheus /opt/prometheus

COPY --from=jans-config-api-src /opt/jans/jetty/jans-config-api /opt/jans/jetty/jans-config-api
COPY --from=jans-config-api-src /app/templates/jans-config-api /app/templates/jans-config-api
COPY --from=jans-config-api-src /app/templates/log4j2.xml /app/templates/opt/jans/jetty/jans-config-api/resources/log4j2.xml
COPY --from=jans-config-api-src /app/plugins/admin-ui/log4j2-adminui.xml /app/templates/opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml
COPY --from=jans-config-api-src /opt/jans/jetty/jans-config-api/webapps/jans-config-api.xml /app/templates/opt/jans/jetty/jans-config-api/webapps/jans-config-api.xml
COPY --from=jans-config-api-src /usr/share/java /opt/jans/jetty/jans-config-api/_plugins
COPY --from=jans-config-api-src /usr/bin/facter /usr/local/bin/facter

COPY --from=jans-fido2-src /opt/jans/jetty/jans-fido2 /opt/jans/jetty/jans-fido2
COPY --from=jans-fido2-src /app/templates/jans-fido2 /app/templates/jans-fido2
COPY --from=jans-fido2-src /etc/jans/conf/fido2 /etc/jans/conf/fido2
COPY --from=jans-fido2-src /app/templates/log4j2.xml /app/templates/opt/jans/jetty/jans-fido2/resources/log4j2.xml
COPY --from=jans-fido2-src /opt/jans/jetty/jans-fido2/webapps/jans-fido2.xml /app/templates/opt/jans/jetty/jans-fido2/webapps/jans-fido2.xml

COPY --from=jans-scim-src /opt/jans/jetty/jans-scim /opt/jans/jetty/jans-scim
COPY --from=jans-scim-src /app/templates/jans-scim /app/templates/jans-scim
COPY --from=jans-scim-src /app/static /app/static
COPY --from=jans-scim-src /app/templates/log4j2.xml /app/templates/opt/jans/jetty/jans-scim/resources/log4j2.xml
COPY --from=jans-scim-src /opt/jans/jetty/jans-scim/webapps/jans-scim.xml /app/templates/opt/jans/jetty/jans-scim/webapps/jans-scim.xml

COPY --from=flex-casa-src /opt/jans/jetty/casa /opt/jans/jetty/casa
COPY --from=flex-casa-src /app/templates /app/templates
COPY --from=flex-casa-src /app/static /app/static
COPY --from=flex-casa-src /app/templates/log4j2.xml /app/templates/opt/jans/jetty/casa/resources/log4j2.xml
COPY --from=flex-casa-src /opt/jans/jetty/casa/webapps/casa.xml /app/templates/opt/jans/jetty/casa/webapps/casa.xml

COPY --from=flex-admin-ui-src /opt/flex/admin-ui /opt/flex/admin-ui
COPY --from=flex-admin-ui-src /app/templates/admin-ui /app/templates/admin-ui
COPY --from=flex-admin-ui-src /run/nginx/nginx.pid /run/nginx/nginx.pid
COPY --from=flex-admin-ui-src /app/scripts /app/flex_aio/admin_ui
RUN ln -sf /app/flex_aio/admin_ui/entrypoint.sh /app/bin/admin-ui-entrypoint.sh

# ----
# misc
# ----

RUN ln -sf /usr/lib/jvm/jdk /opt/java

RUN mkdir -p /opt/jans/configurator/db \
    /opt/jans/configurator/certs \
    /opt/jetty/temp \
    /opt/flex/admin-ui/_plugins \
    /etc/nginx/flex-aio \
    /var/lib/nginx/html/admin

COPY app /app

# set directory contains installer code that will be added to Python sys.path
ENV PYTHONPATH=/app

ENV JETTY_BASE=/opt/jans/jetty \
    JETTY_HOME=/opt/jetty \
    FLEX_COMPONENTS="" \
    GLUU_CASA_ADMIN_LOCK_FILE=/opt/jans/jetty/casa/resources/.administrable \
    GLUU_CASA_JWKS_SIZE_LIMIT=100000 \
    GLUU_CASA_JAVA_OPTIONS="" \
    GLUU_ADMIN_UI_ENABLE_NGINX=false \
    GLUU_ADMIN_UI_PUBLIC_DIR=/var/lib/nginx/html/admin \
    CN_ENABLE_STDOUT_LOG_PREFIX=true \
    CN_DUO_ENABLED=false \
    CN_AUTH_JAVA_OPTIONS="" \
    CN_CONFIG_API_JAVA_OPTIONS="" \
    CN_FIDO2_JAVA_OPTIONS="" \
    CN_SCIM_JAVA_OPTIONS=""  \
    CN_JETTY_REQUEST_HEADER_SIZE=8192 \
    CN_CONFIG_API_CREATE_SCOPES=true \
    FLEX_AIO_ENABLE_MONITOR=false

LABEL org.opencontainers.image.url="gluufederation/flex-aio" \
    org.opencontainers.image.authors="Gluu Inc. <support@gluu.org>" \
    org.opencontainers.image.vendor="Gluu Federation" \
    org.opencontainers.image.version="1.0.0" \
    org.opencontainers.image.title="Gluu Flex Cloud" \
    org.opencontainers.image.description=""

# forward logs to stdout and stderr
RUN ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

RUN cp /app/templates/nginx/*-upstream.conf /etc/nginx/flex-aio/ \
    && cp /app/templates/nginx/*-location.conf /etc/nginx/flex-aio \
    && cp /app/templates/nginx/admin-index.html /var/lib/nginx/html/admin/index.html

# create non-root user
RUN adduser -s /bin/sh -h /home/1000 -D -u 1000 flex \
    && addgroup flex root

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
    ${JETTY_BASE}/casa/static \
    ${JETTY_BASE}/casa/plugins \
    ${JETTY_BASE}/casa/resources \
    ${JETTY_BASE}/casa/logs \
    /opt/flex/admin-ui/_plugins \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /etc/nginx/flex-aio

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
    ${JETTY_BASE}/casa/static \
    ${JETTY_BASE}/casa/plugins \
    ${JETTY_BASE}/casa/resources \
    ${JETTY_BASE}/casa/logs \
    /opt/flex/admin-ui/_plugins \
    /etc/certs \
    /etc/jans \
    /opt/jans/python/libs \
    /opt/jans/configurator \
    /opt/prometheus \
    /app \
    /var/lib/nginx \
    /var/log/nginx \
    /etc/nginx/flex-aio

RUN chown 1000:1000 /opt/flex/admin-ui \
    /opt/java/lib/security/cacerts \
    /run/nginx/nginx.pid \
    /etc/nginx/http.d/default.conf

# run as non-root user
USER 1000

RUN mkdir -p $HOME/.config/gcloud

WORKDIR /app

EXPOSE 8080

CMD ["sh", "/app/bin/entrypoint.sh"]
