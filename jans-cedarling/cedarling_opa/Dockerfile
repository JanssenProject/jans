ARG BUILDPLATFORM="linux/amd64"
ARG BUILDERIMAGE="golang:1.25-trixie"
# Use distroless as minimal base image to package the manager binary
ARG BASEIMAGE="gcr.io/distroless/cc-debian12:nonroot"

# Stage 1: Rust build
FROM rust:1.95-slim-bookworm AS rust-builder
ARG JANS_SOURCE_VERSION=c0ae216653f1a615ad053ac1f88f54dfbffd0842
RUN apt-get update \
    && apt-get install -y git protobuf-compiler \
    && rm -rf /var/cache/apt
WORKDIR /src
RUN git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans . \
    && git sparse-checkout init --cone \
    && git sparse-checkout add jans-cedarling \
    && git checkout ${JANS_SOURCE_VERSION} \
    && cd jans-cedarling \
    && cargo build --release -p cedarling_go

# Stage 2: Go build
FROM --platform=$BUILDPLATFORM $BUILDERIMAGE AS go-builder
WORKDIR /src
COPY . .
COPY --from=rust-builder /src/jans-cedarling/target/release/libcedarling_go.so ./plugins/cedarling_opa/libcedarling_go.so
RUN make build

# Stage 3: Base image
FROM $BASEIMAGE
WORKDIR /app
COPY --from=go-builder /src/build/opa-cedarling /app/
COPY --from=go-builder /src/demo /app/demo
COPY --from=go-builder /src/plugins/cedarling_opa/libcedarling_go.so /usr/local/lib/
ENV LD_LIBRARY_PATH=/usr/local/lib
EXPOSE 8181
LABEL org.opencontainers.image.url="ghcr.io/janssenproject/jans/cedarling-opa" \
    org.opencontainers.image.authors="Janssen Project <support@jans.io>" \
    org.opencontainers.image.vendor="Janssen Project" \
    org.opencontainers.image.version="0.0.0-nightly" \
    org.opencontainers.image.title="Cedarling OPA" \
    org.opencontainers.image.description="Janssen Cedarling OPA"
ENTRYPOINT ["/app/opa-cedarling"]
CMD ["run", "--server", "--addr", "0.0.0.0:8181", "--config-file", "/app/demo/opa-config.json", "/app/demo/rego"]
