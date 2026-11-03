# Builds the native library and the generated Kotlin that the Maven build compiles against.
# Run `make` before `mvn`; both outputs are generated, so git ignores them.

WORKSPACE := ../..
LIB_NAME := jans_trace_uniffi
LIB_FILE := lib$(LIB_NAME).so
KOTLIN_OUT := src/main/kotlin
RESOURCES := src/main/resources

# JNA looks for the library under its platform prefix on the classpath.
ARCH := $(shell uname -m)
ifeq ($(ARCH),x86_64)
	JNA_PREFIX := linux-x86-64
else ifeq ($(ARCH),aarch64)
	JNA_PREFIX := linux-aarch64
else
	$(error unsupported architecture $(ARCH))
endif

.PHONY: native build kotlin lib clean

native: kotlin lib

build:
	cd $(WORKSPACE) && cargo build --release --locked -p $(LIB_NAME)

kotlin: build
	rm -rf $(KOTLIN_OUT)
	cd $(WORKSPACE) && cargo run --release --locked -p $(LIB_NAME) --bin uniffi-bindgen -- \
		generate --no-format --language kotlin \
		--library target/release/$(LIB_FILE) \
		--out-dir bindings/jans-trace-java/$(KOTLIN_OUT)

lib: build
	mkdir -p $(RESOURCES)/$(JNA_PREFIX)
	cp $(WORKSPACE)/target/release/$(LIB_FILE) $(RESOURCES)/$(JNA_PREFIX)/$(LIB_FILE)

clean:
	rm -rf $(KOTLIN_OUT) $(RESOURCES)
