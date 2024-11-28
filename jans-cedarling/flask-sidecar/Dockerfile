FROM python:3.10.14-alpine3.19
# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache musl-dev g++ cargo \
    bash openssl unzip wget git

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

# System deps:
RUN pip install "poetry==$POETRY_VERSION" maturin patchelf
ENV JANS_SOURCE_VERSION=871c36da8a2550ee2febd7c0d72c521898b226c3

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 web

WORKDIR /api

# Setting up proper permissions:
RUN chown -R 1000:1000 /api

RUN git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans /tmp/jans \
        && cd /tmp/jans \
        && git sparse-checkout init --cone \
        && git checkout ${JANS_SOURCE_VERSION} \
        && git sparse-checkout set jans-cedarling \
        && cd jans-cedarling/bindings/cedarling_python \
        && maturin build \
        && cp -r ../../flask-sidecar/* /api \
        && chown -R 1000:1000 /api \
        && version=$(sed -n 's/.*version = "\(.*\)"/\1/p' pyproject.toml) \
        && mv ../../target/wheels/cedarling_python-"$version"-cp312-cp312-manylinux_2_34_x86_64.whl /api/cedarling_python.whl \
        && rm -rf /tmp/jans


# Project initialization:
RUN poetry add /api/cedarling_python.whl \
    && poetry install --no-dev --no-root --no-interaction --no-ansi \
    # Cleaning poetry installation's cache for production:
    && rm -rf "$POETRY_CACHE_DIR"




ENV FLASK_APP=main.core:app \
    GUNICORN_LOG_LEVEL=${GUNICORN_LOG_LEVEL:-debug}

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
ENV HOME /home/web

ENV APP_MODE=${APP_MODE:-production}


ENTRYPOINT ["/docker-entrypoint.sh"]

CMD "exec" "gunicorn" "main.core:app" "-b" ":5000" "--log-level" \
$GUNICORN_LOG_LEVEL "--workers" "1" "--threads" "8" \
"--worker-tmp-dir" "/dev/shm" "--worker-class" "gthread" \
"--access-logfile" "-"  "--error-logfile" "-" "--preload" \
"--log-config" "gunicorn_logging.conf"
