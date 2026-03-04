# ==============================
# Configuration
# ==============================

APP_NAME := opa-cedarling
BUILD_DIR := build
PLUGIN_DIR := plugins
LIB_DIR := $(PLUGIN_DIR)/cedarling_opa

GO := go
CARGO := cargo

CGO_ENABLED := 1

# Adjust if your library name differs
LIB_NAME := cedarling_go
LIB_FILE := lib$(LIB_NAME).so

.PHONY: all
all: native build


.PHONY: native
native:
	@echo ">> Building Go Binding"
	$(CARGO) build --release -p cedarling_go 
	cp ../target/release/$(LIB_FILE) $(LIB_DIR)/

.PHONY: build
build:
	@echo ">> Building Go binary"
	CGO_ENABLED=$(CGO_ENABLED) \
	CGO_LDFLAGS="-L$(LIB_DIR)" \
	$(GO) build -o $(BUILD_DIR)/$(APP_NAME)


.PHONY: clean
clean:
	@echo ">> Cleaning"
	rm -rf $(BUILD_DIR)
	rm $(LIB_DIR)/$(LIB_FILE)

# ==============================
# Debug Build
# ==============================

.PHONY: debug
debug:
	CGO_ENABLED=$(CGO_ENABLED) \
	CGO_LDFLAGS="-L$(LIB_DIR)" \
	$(GO) build -x -v -o $(BUILD_DIR)/$(APP_NAME)
