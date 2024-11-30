IMAGE_VERSION?=$(shell grep -Po 'org.opencontainers.image.version="\K.*?(?=")' Dockerfile)
IMAGE_URL=$(shell grep -Po 'org.opencontainers.image.url="\K.*?(?=")' Dockerfile)
IMAGE?=${IMAGE_URL}:${IMAGE_VERSION}

# pass extra args to the targets, for example:
#
# - `make build-dev ARGS="--no-cache"`
# - `make trivy-scan TRIVY_ARGS="-f json"`
# - `make grype-scan GRYPE_ARGS="-o json"`
ARGS?=

.PHONY: test clean all build-dev trivy-scan grype-scan
.DEFAULT_GOAL := build-dev

build-dev:
	@echo "[I] Building OCI image ${IMAGE}"
	@docker build --rm --force-rm ${ARGS} -t ${IMAGE} .

trivy-scan:
	@echo "[I] Scanning OCI image ${IMAGE} using trivy"
	@trivy image --scanners vuln ${ARGS} ${IMAGE}

grype-scan:
	@echo "[I] Scanning OCI image ${IMAGE} using grype"
	@grype -v ${ARGS} ${IMAGE}
