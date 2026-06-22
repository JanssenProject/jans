ARG BUILDPLATFORM="linux/amd64"
ARG BUILDERIMAGE="golang:1.26-trixie@sha256:82c8738642b733f7d2f44b057375ec90d17c294ff6689aedc628afa9c2b2ceff"
# Use distroless as minimal base image to package the manager binary
ARG BASEIMAGE="gcr.io/distroless/cc-debian12:nonroot@sha256:bd2899c12b335c827750ccf2359879eab09c09b206023dcebea408947d54127c"

# Stage 1: Rust build
FROM rust:1.95-slim-bookworm@sha256:d7482085ff5b415f84dba5647ae71606650bdef00db7aeb69f4b3d170c3e4082 AS rust-builder
ARG JANS_SOURCE_VERSION=e0571e40361954214a99b09dd6a67eae3949a59c
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
    org.opencontainers.image.version="2.2.0-1" \
    org.opencontainers.image.title="Cedarling OPA" \
    org.opencontainers.image.description="Janssen Cedarling OPA"
ENTRYPOINT ["/app/opa-cedarling"]
CMD ["run", "--server", "--addr", "0.0.0.0:8181", "--config-file", "/app/demo/opa-config.json", "/app/demo/rego"]
