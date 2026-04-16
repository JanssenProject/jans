ARG BUILDPLATFORM="linux/amd64"
ARG BUILDERIMAGE="golang:1.25-trixie"
# Use distroless as minimal base image to package the manager binary
ARG BASEIMAGE="gcr.io/distroless/cc-debian12:nonroot"

# Stage 1: Go build
FROM --platform=$BUILDPLATFORM $BUILDERIMAGE AS builder
WORKDIR /src
COPY . .
RUN wget -q "https://github.com/JanssenProject/jans/releases/download/nightly/libcedarling_go-0.0.0_x86-64.so" \
        -O ./plugins/cedarling_opa/libcedarling_go.so && \
    make build

FROM $BASEIMAGE
WORKDIR /app
COPY --from=builder /src/build/opa-cedarling /app/
COPY --from=builder /src/opa-config.json /app/
COPY --from=builder /src/plugins/cedarling_opa/libcedarling_go.so /usr/local/lib/
ENV LD_LIBRARY_PATH=/usr/local/lib
ENTRYPOINT ["/app/opa-cedarling", "run", "--server","--config-file", "/app/opa-config.json"]
