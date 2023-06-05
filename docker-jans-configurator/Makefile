CN_VERSION?=1.0.14-SNAPSHOT
IMAGE_NAME=ghcr.io/janssenproject/jans/configurator
DEV_VERSION?=$(shell echo ${CN_VERSION} | cut -d '-' -f 1)_dev

# pass extra build args, i.e. `make build-dev BUILD_ARGS="--no-cache"`
BUILD_ARGS?=

# pass extra trivy args, i.e. `make trivy-scan TRIVY_ARGS="-f json"`
TRIVY_ARGS?=

# pass extra grype args, i.e. `make grype-scan GRYPE_ARGS="-o json"`
GRYPE_ARGS?=

.PHONY: test clean all build-dev trivy-scan grype-scan
.DEFAULT_GOAL := build-dev

build-dev:
	@echo "[I] Building OCI image ${IMAGE_NAME}:${DEV_VERSION}"
	@docker build --rm --force-rm ${BUILD_ARGS} -t ${IMAGE_NAME}:${DEV_VERSION} .

trivy-scan:
	@echo "[I] Scanning OCI image ${IMAGE_NAME}:${DEV_VERSION} using trivy"
	@trivy image --security-checks vuln ${TRIVY_ARGS} ${IMAGE_NAME}:${DEV_VERSION}

grype-scan:
	@echo "[I] Scanning OCI image ${IMAGE_NAME}:${DEV_VERSION} using grype"
	@grype -v ${GRYPE_ARGS} ${IMAGE_NAME}:${DEV_VERSION}
