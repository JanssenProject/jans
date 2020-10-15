GLUU_VERSION=4.2.2
IMAGE_NAME=gluufederation/certmanager
UNSTABLE_VERSION=dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION}"
	@docker build --rm --force-rm -t ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION} .

trivy-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION} using trivy"
	@trivy -d image ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION}

dockle-scan:
	@echo "[I] Scanning Docker image ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION} using dockle"
	@dockle -d ${IMAGE_NAME}:${GLUU_VERSION}_${UNSTABLE_VERSION}
