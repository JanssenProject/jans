# Self-contained source build for the cedarling_pg PostgreSQL extension.
#
# Build context must be the `jans-cedarling/` workspace root (where Cargo.toml
# lives). From the outer repo:
#
#   docker build \
#       -f jans-cedarling/cedarling_pg/Dockerfile \
#       -t cedarling_pg:dev-pg16 \
#       jans-cedarling
#
# Run:
#
#   docker run --rm -e POSTGRES_PASSWORD=pw -p 5432:5432 cedarling_pg:dev-pg16
#
# Operators still need to set `cedarling.bootstrap_config` to a path inside the
# container before any authorize call will succeed; see the integration docs.

ARG RUST_VERSION=1.95.0
ARG ALPINE_VERSION=3.21
ARG PG_MAJOR=17

# --- Builder stage ----------------------------------------------------------

FROM rust:${RUST_VERSION}-alpine${ALPINE_VERSION} AS builder

ARG PG_MAJOR

# pgrx + cedarling native build deps on Alpine.
#  - build-base, musl-dev, linux-headers: musl toolchain
#  - clang-dev, llvm-dev: bindgen runtime (pgrx-pg-sys)
#  - pkgconf, perl: openssl-sys / native crate builds
#  - openssl-dev: TLS dependencies
#  - postgresql${PG_MAJOR}-dev: server headers + pg_config matching the runtime
#  - bash: cargo pgrx init runs a bash script
RUN apk add --no-cache \
        build-base musl-dev linux-headers \
        clang-dev llvm-dev \
        pkgconf perl \
        openssl-dev \
        postgresql${PG_MAJOR}-dev \
        bash git

# musl static-CRT default would produce a fully static .so that Postgres
# cannot dlopen; force dynamic linking for the extension.
ENV RUSTFLAGS="-C target-feature=-crt-static"

# Match the cedarling_pg crate's pinned pgrx version (=0.18.0).
RUN cargo install --locked cargo-pgrx --version 0.18.0

# Point pgrx at Alpine's pg_config so the .so links against the exact libpq
# the runtime image will dlopen at extension load time.
RUN cargo pgrx init --pg${PG_MAJOR} "$(which pg_config)"

WORKDIR /build

# Minimal copy — only cedarling_pg's path-dependency closure. The full
# workspace also has bindings/*, cedarling_pg_codegen, cedarling_opa, etc.,
# none of which are reachable from cedarling_pg's compile graph.
COPY Cargo.toml Cargo.lock rust-toolchain.toml ./
COPY cedarling/    cedarling/
COPY cedarling_pg/ cedarling_pg/
COPY http_utils/   http_utils/
COPY sparkv/       sparkv/
COPY test_utils/   test_utils/

# The workspace `members` list globs `bindings/*` and names
# `cedarling_pg_codegen`; cargo refuses to load the workspace if those
# manifests are missing. Strip them so the slim COPY above is enough.
RUN sed -i \
        -e 's|"bindings/\*", *||g' \
        -e 's|, *"bindings/\*"||g' \
        -e 's|"cedarling_pg_codegen", *||g' \
        -e 's|, *"cedarling_pg_codegen"||g' \
        Cargo.toml

ARG PG_MAJOR
# Build + install the extension. After install, stash the produced files in
# /cedarling_out/ so the runtime stage can place them via pg_config-discovered
# paths (avoids hardcoding Alpine vs Debian PG layout differences).
RUN cargo pgrx install --release \
        --manifest-path cedarling_pg/Cargo.toml \
        --features "pg${PG_MAJOR}" \
        --pg-config "$(which pg_config)" \
 && mkdir -p /cedarling_out \
 && cp "$(pg_config --pkglibdir)/cedarling_pg.so"              /cedarling_out/ \
 && cp "$(pg_config --sharedir)/extension/cedarling_pg.control" /cedarling_out/ \
 && cp "$(pg_config --sharedir)"/extension/cedarling_pg--*.sql  /cedarling_out/

# --- Runtime stage ----------------------------------------------------------

FROM postgres:${PG_MAJOR}-alpine${ALPINE_VERSION}

LABEL org.opencontainers.image.title="cedarling_pg" \
      org.opencontainers.image.description="Alpine PostgreSQL with the cedarling_pg authorization extension built from source." \
      org.opencontainers.image.source="https://github.com/JanssenProject/jans" \
      org.opencontainers.image.licenses="Apache-2.0"

# Stage artifacts, then place them at the runtime image's pg_config-reported
# paths. Doing path discovery in the runtime keeps the Dockerfile portable
# across Alpine / Debian / bookworm-slim PostgreSQL images.
COPY --from=builder /cedarling_out/ /tmp/cedarling_pg_out/
RUN set -eux; \
    PKGLIB="$(pg_config --pkglibdir)"; \
    SHAREDIR="$(pg_config --sharedir)"; \
    install -m 0755 /tmp/cedarling_pg_out/cedarling_pg.so      "${PKGLIB}/cedarling_pg.so"; \
    install -m 0644 /tmp/cedarling_pg_out/cedarling_pg.control "${SHAREDIR}/extension/cedarling_pg.control"; \
    install -m 0644 /tmp/cedarling_pg_out/cedarling_pg--*.sql  "${SHAREDIR}/extension/"; \
    rm -rf /tmp/cedarling_pg_out

# Auto-create the extension on first start. Operators still need to set
# cedarling.bootstrap_config inside the container before any authorize call.
COPY cedarling_pg/scripts/docker-initdb.sh /docker-entrypoint-initdb.d/10-cedarling_pg.sh
