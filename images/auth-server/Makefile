CN_VERSION=1.0.0
IMAGE_NAME=janssenproject/auth-server
UNSTABLE_VERSION=dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}"
	@docker build --rm --force-rm -t ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} .

trivy-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} using trivy"
	@trivy -d image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}

dockle-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} using dockle"
	@dockle -d ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}
