CN_VERSION?=1.0.6-SNAPSHOT
IMAGE_NAME=janssenproject/fido2
DEV_VERSION?=$(shell echo ${CN_VERSION} | cut -d '-' -f 1)_dev

.PHONY: test clean all build-dev trivy-scan grype-scan
.DEFAULT_GOAL := build-dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE_NAME}:${DEV_VERSION}"
	@docker build --rm --force-rm -t ${IMAGE_NAME}:${DEV_VERSION} .

trivy-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${DEV_VERSION} using trivy"
	@trivy image --security-checks vuln ${IMAGE_NAME}:${DEV_VERSION}

grype-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${DEV_VERSION} using grype"
	@grype -v ${IMAGE_NAME}:${DEV_VERSION}
