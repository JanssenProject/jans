FROM ubuntu:22.04@sha256:e9569c25505f33ff72e88b2990887c9dcf230f23259da296eb814fc2b41af999


# Don't start any optional services except for the few we need.
RUN find /etc/systemd/system \
    /lib/systemd/system \
    -path '*.wants/*' \
    -not -name '*journald*' \
    -not -name '*systemd-tmpfiles*' \
    -not -name '*systemd-user-sessions*' \
    -exec rm \{} \;

# Disable list of suggested/ recommended dependencies
RUN echo 'APT::Install-Suggests "0";' >> /etc/apt/apt.conf.d/00-docker \
    && echo 'APT::Install-Recommends "0";' >> /etc/apt/apt.conf.d/00-docker

# Prevent prompt errors during package installation
RUN DEBIAN_FRONTEND=noninteractive \
    apt-get update \
    && apt-get install -y python3 tini curl ca-certificates dbus systemd iproute2 gpg python3-pip python3-dev libpq-dev gcc python3-psycopg2 git maven \
    # install certbot
    && apt-get -y install libaugeas0 \
    && pip install ruamel.yaml certbot certbot-apache \
    && apt-get clean \
    # Cleaning up package lists
    && rm -rf /var/lib/apt/lists/*


RUN systemctl set-default multi-user.target \
    && systemctl mask dev-hugepages.mount sys-fs-fuse-connections.mount

# Workaround agetty high CPU. https://bugzilla.redhat.com/show_bug.cgi?id=1046469
RUN rm -f /lib/systemd/system/systemd*udev* \
    && rm -f /lib/systemd/system/getty.target

HEALTHCHECK --interval=35s --timeout=4s CMD /opt/jans/scripts/jans-auth check | grep "Jetty running pid" || exit 1

# Ports required by jetty
EXPOSE 443 8080 1636

# =====================
# jans-linux-setup
# =====================

ENV JANS_SOURCE_VERSION=856f9fed1d58a6d41503a0459bbe04f52b0bb8e7

# cleanup
RUN rm -rf /tmp/jans

# =======
# License
# =======

COPY LICENSE /licenses/LICENSE

# ==========
# SETUP ENVS
# ==========

ENV CN_HOSTNAME="demoexample.jans.io" \
    CN_ADMIN_PASS="" \
    CN_ORG_NAME="Janssen" \
    CN_EMAIL="support.jans.io" \
    CN_CITY="Austin" \
    CN_STATE="TX" \
    CN_COUNTRY="US" \
    TEST_CLIENT_ID="9876baac-de39-4c23-8a78-674b59df8c09" \
    TEST_CLIENT_SECRET="" \
    TEST_CLIENT_TRUSTED="true" \
    CN_INSTALL_MYSQL="false" \
    CN_INSTALL_PGSQL="false" \
    CN_INSTALL_CONFIG_API="true" \
    CN_INSTALL_SCIM="true" \
    CN_INSTALL_FIDO2="true" \
    CN_INSTALL_CASA="true" \
    CN_INSTALL_KC_LINK="true" \
    CN_INSTALL_LINK="true" \
    CN_INSTALL_LOCK="true" \
    CN_INSTALL_SAML="false" \
    CN_INSTALL_OPA="true" \
    RDBMS_DATABASE="jans" \
    RDBMS_USER="jans" \
    RDBMS_PASSWORD="" \
    RDBMS_HOST="" \
    IS_FQDN_REGISTERED="false" \
    RUN_TESTS="false"

# ==========
# misc stuff
# ==========

LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/monolith" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="1.3.0" \
    org.opencontainers.image.title="Janssen Monolith Image" \
    org.opencontainers.image.description="Janssen Authorization server"

COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

CMD ["/bin/bash", "-c", "rm -rf /opt/jetty-11.0/temp/*; exec /app/scripts/entrypoint.sh --log-target=journal 3>&1"]
