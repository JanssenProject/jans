CN_VERSION?=1.0.3
IMAGE_NAME=janssenproject/config-api
UNSTABLE_VERSION?=dev

.PHONY: test clean all build-dev trivy-scan grype-scan
.DEFAULT_GOAL := build-dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}"
	@docker build --rm --force-rm -t ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} .

trivy-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} using trivy"
	@trivy image --security-checks vuln ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}

grype-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} using grype"
	@grype -v ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}
