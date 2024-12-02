FROM python:3.10.14-alpine3.19 

# ===============
# Alpine packages
# ===============

RUN apk update \
    && apk add --no-cache musl-dev g++ \
    bash openssl-dev unzip wget git curl 

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs > rustup.sh
RUN chmod 700 rustup.sh
RUN ./rustup.sh -y

ENV PATH="/root/.cargo/bin:${PATH}"
RUN cargo --help

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


RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python
RUN mv /usr/lib/python3.11/EXTERNALLY-MANAGED /usr/lib/python3.11/EXTERNALLY-MANAGED.disabled
RUN python3 -m ensurepip
RUN pip3 install "poetry==$POETRY_VERSION" maturin[patchelf]
RUN apk add --no-cache \
        git
# System deps:
ENV JANS_SOURCE_VERSION=feat-jans-cedarling-sidecar

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

# create non-root user
RUN adduser -s /bin/sh -D -G root -u 1000 web

WORKDIR /api

RUN git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans /tmp/jans \
        && cd /tmp/jans \
        && git sparse-checkout init --cone \
        && git checkout ${JANS_SOURCE_VERSION} \
        && git sparse-checkout set jans-cedarling \
        && cd jans-cedarling/bindings/cedarling_python \
        && maturin build \
        && cp -r ../../flask-sidecar/* /api \
        && version=$(sed -n 's/.*version = "\(.*\)"/\1/p' pyproject.toml) \
        && echo "$version" > /api/cedarling_version \
        && mv ../../target/wheels/cedarling_python-"$version"-cp310-cp310-musllinux_1_2_x86_64.whl /api/ \
        && rm -rf /tmp/jans

# Setting up proper permissions:
RUN chown -R 1000:1000 /api

# Project initialization:
RUN poetry add /api/cedarling_python-$(cat /api/cedarling_version)-cp310-cp310-musllinux_1_2_x86_64.whl \
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
ENV HOME=/home/web

ENV APP_MODE=${APP_MODE:-production}


ENTRYPOINT ["/docker-entrypoint.sh"]

CMD "exec" "gunicorn" "main.core:app" "-b" ":5000" "--log-level" \
$GUNICORN_LOG_LEVEL "--workers" "1" "--threads" "8" \
"--worker-tmp-dir" "/dev/shm" "--worker-class" "gthread" \
"--access-logfile" "-"  "--error-logfile" "-" "--preload" \
"--log-config" "gunicorn_logging.conf"
