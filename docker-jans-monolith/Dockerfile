FROM ubuntu:20.04

# Don't start any optional services except for the few we need.
RUN find /etc/systemd/system \
    /lib/systemd/system \
    -path '*.wants/*' \
    -not -name '*journald*' \
    -not -name '*systemd-tmpfiles*' \
    -not -name '*systemd-user-sessions*' \
    -exec rm \{} \;

# Disable list of suggested/ recommended dependencies
RUN echo 'APT::Install-Suggests "0";' >> /etc/apt/apt.conf.d/00-docker
RUN echo 'APT::Install-Recommends "0";' >> /etc/apt/apt.conf.d/00-docker
# Prevent prompt errors during package installation
RUN DEBIAN_FRONTEND=noninteractive \
  apt-get update \
  && apt-get install -y python3 tini curl ca-certificates dbus systemd iproute2 gpg python3-pip \
  && apt-get clean \
  # Cleaning up package lists
  && rm -rf /var/lib/apt/lists/*


RUN systemctl set-default multi-user.target
RUN systemctl mask dev-hugepages.mount sys-fs-fuse-connections.mount

# Workkaround agetty high CPU. https://bugzilla.redhat.com/show_bug.cgi?id=1046469
RUN rm -f /lib/systemd/system/systemd*udev*
RUN rm -f /lib/systemd/system/getty.target

#HEALTHCHECK --interval=35s --timeout=4s CMD systemctl is-active --quiet jans-auth.service || exit 1

# Ports required by jetty
EXPOSE 443 8080 3306 33060 1636

# =====================
# jans-linux-setup
# =====================

ENV JANS_SOURCE_VERSION=7262ae916214529c730dd13f50563a03555e8e91

# cleanup
RUN rm -rf /tmp/jans

# ======
# Python
# ======
COPY requirements.txt /app/requirements.txt
RUN pip3 install --no-cache-dir -U pip wheel \
    && pip3 install --no-cache-dir -r /app/requirements.txt \
    && pip3 uninstall -y pip wheel

# =======
# License
# =======

COPY LICENSE /licenses/LICENSE

# ==========
# SETUP ENVS
# ==========

ENV HOSTNAME="demoexample.jans.io" \
    ADMIN_PASS="" \
    ORG_NAME="Janssen" \
    EMAIL="support.jans.io" \
    CITY="Austin" \
    STATE="TX" \
    COUNTRY="US" \
    # Install with local mysql by default
    INSTALL_LDAP="false" \
    INSTALL_CONFIG_API="true" \
    INSTALL_SCIM_SERVER="true" \
    INSTALL_FIDO2="true" \
    INSTALL_CLIENT_API="true" \
    MYSQL_DATABASE="jans" \
    MYSQL_USER="jans" \
    MYSQL_PASSWORD=""

# ==========
# misc stuff
# ==========

LABEL name="janssenproject/monolith" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.3" \
    release="dev" \
    summary="Janssen Monolith Image" \
    description="Janssen Authorization server"

COPY scripts /app/scripts
RUN chmod +x /app/scripts/entrypoint.sh

# Workaround for docker/docker#27202, technique based on comments from docker/docker#9212
CMD ["/bin/bash", "-c", "exec /app/scripts/entrypoint.sh --log-target=journal 3>&1"]
#CMD ["bash", "/app/scripts/entrypoint.sh"]
