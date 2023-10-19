IMAGE_VERSION?=$(shell grep -Po 'org.opencontainers.image.version="\K.*?(?=")' ../../Dockerfile)
IMAGE_NAME?=$(shell grep -Po 'org.opencontainers.image.url="\K.*?(?=")' ../../Dockerfile)
DEV_VERSION?=$(shell echo ${IMAGE_VERSION} | cut -d '-' -f 1)_dev
INGRESS?=nginx

# double dollar signs for char escape in sed inside Makefile
export NODE_IP_ADDR?=$(shell ip route get 1 | sed 's/^.*src \([^ ]*\).*$$/\1/;q')
export FLEX_AIO_IMAGE?=${IMAGE_NAME}:${DEV_VERSION}
export FLEX_AIO_SSA?=$(shell cat ssa.jwt | base64 -w0)

.PHONY: test clean all install uninstall build-dev
.DEFAULT_GOAL := install

install:
	@echo "[I] Installing ${FLEX_AIO_IMAGE}"
	@envsubst < base.tmpl.yaml | microk8s.kubectl apply -f -
	@case ${INGRESS} in \
		nginx|istio) \
			microk8s.kubectl apply -f ${INGRESS}-ingress.yaml \
			;; \
		*) \
			;; \
	esac

uninstall:
	@echo "[I] Uninstalling ${FLEX_AIO_IMAGE}"
	@case ${INGRESS} in \
		nginx|istio) \
			microk8s.kubectl delete -f ${INGRESS}-ingress.yaml \
			;; \
		*) \
			;; \
	esac
	@envsubst < base.tmpl.yaml | microk8s.kubectl delete -f -

build-dev:
	@cd ../.. && $(MAKE) build-dev
	@echo "[I] Importing ${FLEX_AIO_IMAGE} into microk8s container registry"
	@docker save ${FLEX_AIO_IMAGE} > flex-aio.tar && sudo microk8s.ctr image import flex-aio.tar
