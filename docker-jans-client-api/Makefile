CN_VERSION=1.0.2
IMAGE_NAME=janssenproject/client-api
UNSTABLE_VERSION=dev

.PHONY: test clean all build-dev

build-dev:
	@echo "[I] Building Docker image ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION}"
	@docker build --rm --force-rm -t ${IMAGE_NAME}:${CN_VERSION}_${UNSTABLE_VERSION} .
