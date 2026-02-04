# =================================
# Janssen Shibboleth IDP Container
# Shibboleth IdP 5.1.6
# =================================
FROM bellsoft/liberica-openjdk-alpine:17.0.17@sha256:0e8ae43555ad8e2d69431c2078df256c01b625a8a3010e76b5a6baff0d165d2c

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk upgrade --available \
    && apk add --no-cache openssl python3 tini curl bash py3-cryptography \
    && apk add --no-cache --virtual .build-deps wget git zip

# =====
# Jetty
# =====

ARG JETTY_VERSION=12.0.25
ARG JETTY_HOME=/opt/jetty
ARG JETTY_BASE=/opt/shibboleth-idp/jetty
ARG JETTY_USER_HOME_LIB=/home/jetty/lib

# Install Jetty
RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz -O /tmp/jetty.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty-home-${JETTY_VERSION} ${JETTY_HOME} \
    && rm -rf /tmp/jetty.tar.gz

# ===============
# Shibboleth IdP
# ===============

ARG SHIBBOLETH_VERSION=5.1.6
ARG SHIBBOLETH_HOME=/opt/shibboleth-idp

ENV SHIBBOLETH_VERSION=${SHIBBOLETH_VERSION}
ENV SHIBBOLETH_HOME=${SHIBBOLETH_HOME}

# Download and extract Shibboleth IdP
RUN wget -q https://shibboleth.net/downloads/identity-provider/archive/${SHIBBOLETH_VERSION}/shibboleth-identity-provider-${SHIBBOLETH_VERSION}.tar.gz -O /tmp/shibboleth-idp.tar.gz \
    && mkdir -p /opt/shibboleth-idp-installer \
    && tar -xzf /tmp/shibboleth-idp.tar.gz -C /opt/shibboleth-idp-installer --strip-components=1 \
    && rm -rf /tmp/shibboleth-idp.tar.gz

# ===============
# Janssen modules
# ===============

ENV CN_VERSION=0.0.0-nightly
ENV CN_BUILD_DATE='2025-02-02 00:00'

# Download Janssen Shibboleth IDP WAR
# Set CN_REQUIRE_WAR_DOWNLOAD=false for development builds when WAR is not available
ARG CN_REQUIRE_WAR_DOWNLOAD=true
ENV CN_SOURCE_URL=https://jenkins.jans.io/maven/io/jans/jans-shibboleth-idp-webapp/${CN_VERSION}/jans-shibboleth-idp-webapp-${CN_VERSION}.war

RUN mkdir -p ${SHIBBOLETH_HOME}/war \
    && if [ "${CN_REQUIRE_WAR_DOWNLOAD}" = "true" ]; then \
         wget -q --tries=3 ${CN_SOURCE_URL} -O ${SHIBBOLETH_HOME}/war/idp.war \
         && test -s ${SHIBBOLETH_HOME}/war/idp.war; \
       else \
         wget -q ${CN_SOURCE_URL} -O ${SHIBBOLETH_HOME}/war/idp.war || echo "WAR not available, will be built locally"; \
       fi

# Download Janssen pycloudlib (pinned to specific ref for reproducibility)
ARG JANS_PYCLOUDLIB_REF=main
RUN wget -q https://github.com/JanssenProject/jans/archive/refs/heads/${JANS_PYCLOUDLIB_REF}.tar.gz -O /tmp/jans.tar.gz \
    && mkdir -p /tmp/jans \
    && tar -xf /tmp/jans.tar.gz -C /tmp/jans --strip-components=1 \
    && cd /tmp/jans/jans-pycloudlib \
    && python3 -m pip install --no-cache-dir --break-system-packages . \
    && rm -rf /tmp/jans.tar.gz /tmp/jans

# Cleanup
RUN apk del .build-deps \
    && rm -rf /var/cache/apk/*

# ======
# Python
# ======

COPY requirements.txt /app/requirements.txt
RUN pip3 install --no-cache-dir --break-system-packages -r /app/requirements.txt \
    && rm -rf /app/requirements.txt

# ========
# Scripts
# ========

RUN mkdir -p /app/scripts /app/templates /app/static
COPY scripts /app/scripts/
COPY templates /app/templates/
COPY static /app/static/
RUN chmod +x /app/scripts/*.py

# ==================
# Non-root user
# ==================

ARG UID=1000
ARG GID=1000

RUN addgroup -g ${GID} -S shibboleth \
    && adduser -S -D -H -u ${UID} -G shibboleth -s /sbin/nologin shibboleth

# Create and set ownership of writable directories
RUN mkdir -p ${SHIBBOLETH_HOME}/conf \
    ${SHIBBOLETH_HOME}/credentials \
    ${SHIBBOLETH_HOME}/metadata \
    ${SHIBBOLETH_HOME}/logs \
    ${SHIBBOLETH_HOME}/jetty \
    ${SHIBBOLETH_HOME}/jetty/webapps \
    /app \
    /tmp \
    && chown -R shibboleth:shibboleth ${SHIBBOLETH_HOME} \
    && chown -R shibboleth:shibboleth /app \
    && chown -R shibboleth:shibboleth ${JETTY_HOME} \
    && chmod -R 755 /app/scripts

# =====
# Ports
# =====

EXPOSE 8080

# ===========
# Environment
# ===========

ENV CN_WAIT_MAX_TIME=300 \
    CN_WAIT_SLEEP_DURATION=10 \
    CN_MAX_RAM_PERCENTAGE=75 \
    CN_JAVA_OPTIONS="-Xms256m -Xmx512m" \
    CN_SHIBBOLETH_LOG_LEVEL=INFO \
    CN_PYCLOUDLIB_LOG_LEVEL=INFO \
    CN_HEALTH_CHECK_INTERVAL=30 \
    CN_SHIBBOLETH_PORT=8080 \
    SHIBBOLETH_HOME=${SHIBBOLETH_HOME} \
    JETTY_HOME=${JETTY_HOME} \
    JETTY_BASE=${JETTY_BASE}

# =======
# Volumes
# =======

VOLUME ["/opt/shibboleth-idp/conf", "/opt/shibboleth-idp/credentials", "/opt/shibboleth-idp/metadata", "/opt/shibboleth-idp/logs"]

# ===========
# Healthcheck
# ===========

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/idp/status || exit 1

# =======
# Entrypoint
# =======

LABEL maintainer="Janssen Project <jans@googlegroups.com>" \
      org.opencontainers.image.source="https://github.com/JanssenProject/jans" \
      org.opencontainers.image.description="Janssen Shibboleth Identity Provider ${SHIBBOLETH_VERSION}" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.vendor="Janssen Project" \
      org.opencontainers.image.url="ghcr.io/janssenproject/jans/shibboleth" \
      org.opencontainers.image.version="0.0.0-nightly"

USER shibboleth

ENTRYPOINT ["tini", "-e", "143", "-g", "--"]
CMD ["python3", "/app/scripts/entrypoint.py"]
