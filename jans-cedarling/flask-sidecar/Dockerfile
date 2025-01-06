FROM python:3.10.16-slim-bookworm

# ===============
# Debian packages
# ===============

RUN apt-get update \
    && apt-get install -y bash unzip wget git curl


# ========================
# Install Python3 & Poetry
# ========================
ENV PYTHONFAULTHANDLER=1 \
  PYTHONUNBUFFERED=1 \
  PYTHONHASHSEED=random \
  PIP_NO_CACHE_DIR=off \
  PIP_DISABLE_PIP_VERSION_CHECK=on \
  PIP_DEFAULT_TIMEOUT=100 \
  # poetry:
  POETRY_VERSION=1.4.1 \
  POETRY_NO_INTERACTION=1 \
  POETRY_VIRTUALENVS_CREATE=false \
  POETRY_CACHE_DIR='/var/cache/pypoetry' \
  PATH="$PATH:/root/.poetry/bin"


RUN pip3 install "poetry==$POETRY_VERSION" gunicorn \
    && rm -rf /var/cache/apk/* /root/.cache /tmp/*

# ===============
# Project setup
# ===============
ENV JANS_SOURCE_VERSION=92cc167ade06492e25425fa14426722c9882b0ec

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

# create non-root user
RUN adduser --shell /bin/sh --uid 1000 web \
    && usermod -aG sudo web

WORKDIR /api

COPY --chown=1000:1000 ./secrets/bootstrap.json /api/

RUN git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans /tmp/jans \
        && cd /tmp/jans \
        && git sparse-checkout init --cone \
        && git checkout ${JANS_SOURCE_VERSION} \
        && git sparse-checkout set jans-cedarling \
        && cd jans-cedarling/bindings/cedarling_python \
        && cp -r ../../flask-sidecar/* /api \
        && version=$(sed -n 's/.*version = "\(.*\)"/\1/p' pyproject.toml) \
        && echo "${version}" > /api/cedarling_version \
        && release_version="${version}" \
        && if [ "$version" = "0.0.0" ]; then release_version="nightly"; fi \
        && wget -q https://github.com/JanssenProject/jans/releases/download/"${release_version}"/cedarling_python-"${version}"-cp310-cp310-manylinux_2_34_x86_64.whl -O /api/cedarling_python-"${version}"-cp310-cp310-manylinux_2_34_x86_64.whl \
        && rm -rf /tmp/jans

# Setting up proper permissions:
RUN chmod -R g=u /api \
    && chown -R 1000:1000 /api

# Project initialization:
RUN poetry add /api/cedarling_python-$(cat /api/cedarling_version)-cp310-cp310-manylinux_2_34_x86_64.whl \
    && poetry install --no-dev --no-root --no-interaction --no-ansi \
    # Cleaning poetry installation's cache for production:
    && rm -rf "$POETRY_CACHE_DIR"

ENV FLASK_APP=main.core:app \
    GUNICORN_LOG_LEVEL=${GUNICORN_LOG_LEVEL:-debug} \
    CEDARLING_BOOTSTRAP_CONFIG_FILE=${CEDARLING_BOOTSTRAP_CONFIG_FILE:-/api/bootstrap.json} \
    SIDECAR_DEBUG_RESPOSE=${SIDECAR_DEBUG_RESPONSE:-False}

EXPOSE 5000
LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/cedarling-flask-sidecar" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="AuthZen Flask API" \
    org.opencontainers.image.description="Flask API that implements the [AuthZen](https://openid.github.io/authzen/) specification with the [cedarling](../) python binding."

# Running as non-root user:
USER 1000

# Set env with user being web created above
ENV HOME=/home/web

ENV APP_MODE=${APP_MODE:-production}

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD "exec" "gunicorn" "main.core:app" "-b" ":5000" "--log-level" \
$GUNICORN_LOG_LEVEL "--workers" "1" "--threads" "8" \
"--worker-tmp-dir" "/dev/shm" "--worker-class" "gthread" \
"--access-logfile" "-"  "--error-logfile" "-" "--preload" \
"--log-config" "gunicorn_logging.conf"
