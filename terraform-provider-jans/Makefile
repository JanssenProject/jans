NAME=jans
HOSTNAME=janssenproject
BINARY=terraform-provider-${NAME}
VERSION=0.1.0
OS_ARCH=darwin_amd64

test:
	go test -v ./...

build:
	go build -o ${BINARY}

install: build
	mkdir -p ~/.terraform.d/plugins/${HOSTNAME}/${NAME}/${VERSION}/${OS_ARCH}
	mv ${BINARY} ~/.terraform.d/plugins/${HOSTNAME}/${NAME}/${VERSION}/${OS_ARCH}

generate-docs:
	tfplugindocs generate