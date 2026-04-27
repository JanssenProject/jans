### Stage 1: Maturin build
FROM ghcr.io/astral-sh/uv:0.11.7-python3.11-trixie-slim AS builder

ARG RUST_VERSION=1.95.0
ARG JANS_SOURCE_VERSION=c0ae216653f1a615ad053ac1f88f54dfbffd0842

RUN apt-get update \
    && apt-get install -y bash git curl ca-certificates gcc protobuf-compiler \
    && curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain ${RUST_VERSION} \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/root/.cargo/bin:${PATH}" \
    UV_PYTHON_DOWNLOADS=0 \
    UV_NO_CACHE=1

WORKDIR /build

RUN git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans . \
        && git sparse-checkout init --cone \
        && git checkout ${JANS_SOURCE_VERSION} \
        && git sparse-checkout set jans-cedarling

RUN cd /build/jans-cedarling/bindings/cedarling_python \
    && uvx maturin build --release

### Stage 2: Dependencies
FROM ghcr.io/astral-sh/uv:0.11.7-python3.11-trixie-slim AS deps

ENV PATH="/opt/venv/bin:${PATH}" \
    UV_PYTHON_DOWNLOADS=0 \
    UV_PROJECT_ENVIRONMENT=/opt/venv \
    UV_NO_CACHE=1

WORKDIR /api

# Just dependencies
COPY pyproject.toml uv.lock /api/
RUN uv sync --locked --no-install-project --no-editable --no-group test

# Rest of the app
COPY . /api/
COPY --from=builder /build/jans-cedarling/target/wheels/*.whl /api/
# Sync the project
RUN uv sync --locked --no-editable --no-group test \
    && uv pip install --no-cache-dir /api/*.whl \
    && rm -rf /api/*.whl

### Stage 3: Final runtime
FROM python:3.11.14-slim-trixie@sha256:8ef21a26e7c342e978a68cf2d6b07627885930530064f572f432ea422a8c0907

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tini \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m --home /home/web --shell /bin/sh --uid 1000 web

WORKDIR /api
COPY --from=deps --chown=1000:1000 /opt/venv /opt/venv
COPY --from=deps --chown=1000:1000 /api /api
COPY --chown=1000:1000 docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh


ENV PATH="/opt/venv/bin:$PATH" \
    FLASK_APP=main.core:app \
    GUNICORN_LOG_LEVEL=${GUNICORN_LOG_LEVEL:-debug} \
    CEDARLING_BOOTSTRAP_CONFIG_FILE=${CEDARLING_BOOTSTRAP_CONFIG_FILE:-None} \
    SIDECAR_DEBUG_RESPONSE=${SIDECAR_DEBUG_RESPONSE:-False}

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

ENTRYPOINT ["/usr/bin/tini", "-e", "143", "-g", "--"]

CMD ["/docker-entrypoint.sh"]
