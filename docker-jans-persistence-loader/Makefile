IMAGE_VERSION?=$(shell grep -Po 'org.opencontainers.image.version="\K.*?(?=")' Dockerfile)
IMAGE_NAME?=$(shell grep -Po 'org.opencontainers.image.url="\K.*?(?=")' Dockerfile)
DEV_VERSION?=$(shell echo ${IMAGE_VERSION} | cut -d '-' -f 1)_dev
IMAGE?=${IMAGE_NAME}:${DEV_VERSION}

# pass extra args to targets, for example:
# - `make build-dev ARGS="--no-cache"`
# - `make trivy-scan ARGS="-f json"`
# - `make grype-scan ARGS="-o json"`
ARGS?=

.PHONY: test clean all build-dev trivy-scan grype-scan
.DEFAULT_GOAL := build-dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE}"
	@docker build --rm --force-rm ${ARGS} -t ${IMAGE} .

trivy-scan:
	@echo "[I] Scanning Docker image ${IMAGE} using trivy"
	@trivy image --security-checks vuln ${ARGS} ${IMAGE}

grype-scan:
	@echo "[I] Scanning Docker image ${IMAGE} using grype"
	@grype -v ${ARGS} ${IMAGE}
