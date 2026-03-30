# Makefile for Cedarling UniFFI (iOS + Android)

# Variables
LIB_NAME=cedarling_uniffi
TARGET_DIR=../../target
RELEASE_DIR=$(TARGET_DIR)/release
IOS_OUT=ios/Mobile.xcframework
BINDINGS_DIR=build
ANDROID_JNI=androidApp/app/src/main/jniLibs
ANDROID_KOTLIN_OUT=androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi
BUILD_TYPE ?= release

# Detect OS-specific library
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
	LIB_FILE=lib$(LIB_NAME).dylib
	IOS_AVAILABLE := 1
else ifeq ($(UNAME_S),Linux)
	LIB_FILE=lib$(LIB_NAME).so
else
	LIB_FILE=$(LIB_NAME).dll
endif

#Check BUILD_TYPE
ifeq ($(BUILD_TYPE),release)
    CARGO_FLAGS = --release
else
    CARGO_FLAGS =
endif

#######################################
# Common
#######################################

build:
	@echo "Building $(LIB_NAME)..."
	@cargo build $(CARGO_FLAGS) -p $(LIB_NAME) || { \
	    echo "❌ Failed to build $(LIB_NAME)"; \
	    exit 1; \
    }

clean:
	@echo "Cleaning build artifacts..."
	@cargo clean || { \
        echo "❌ cargo clean failed"; \
        exit 1; \
    }
	@echo "Removing gnerated files..."
	@rm -rf androidApp/app/src/main/java/com/example/androidapp/cedarling/ || exit 1
	@rm -rf javaApp/src/main/kotlin/ || exit 1
	@rm -rf $(BINDINGS_DIR) || exit 1
	@rm -rf $(ANDROID_JNI) || exit 1
	@rm -rf $(IOS_OUT) || exit 1


#######################################
# iOS
#######################################

ios-build: build
	cargo build --release --target=aarch64-apple-ios-sim
	cargo build --release --target=aarch64-apple-ios

ios-bindings:
	cargo run --bin uniffi-bindgen generate \
		--library ./$(RELEASE_DIR)/$(LIB_FILE) \
		--language swift \
		--out-dir ./$(BINDINGS_DIR)

ios-xcframework:
	@if [ ! -f "$(BINDINGS_DIR)/cedarling_uniffiFFI.modulemap" ]; then \
		echo "Error: modulemap file not found"; \
		exit 1; \
    fi
	mv $(BINDINGS_DIR)/cedarling_uniffiFFI.modulemap $(BINDINGS_DIR)/module.modulemap
	xcodebuild -create-xcframework \
		-library ../../target/aarch64-apple-ios-sim/release/lib$(LIB_NAME).a -headers ./$(BINDINGS_DIR) \
		-library ../../target/aarch64-apple-ios/release/lib$(LIB_NAME).a -headers ./$(BINDINGS_DIR) \
		-output "$(IOS_OUT)"

ios:
ifeq ($(IOS_AVAILABLE),1)
	$(MAKE) ios-build
	$(MAKE) ios-bindings
	$(MAKE) ios-xcframework
else
	@echo "Skipping iOS build: not running on macOS"
endif

#######################################
# Android
#######################################

android-setup:
	@command -v cargo-ndk >/dev/null 2>&1 || { \
		echo "cargo-ndk not found. Installing..."; \
		cargo install cargo-ndk || exit 1; \
	}
	rustup target add \
		aarch64-linux-android \
		armv7-linux-androideabi \
		i686-linux-android \
		x86_64-linux-android

android-build:
	$(MAKE) android-setup
	# Check if ANDROID_NDK_HOME is set
	@if [ -z "$(ANDROID_NDK_HOME)" ]; then \
		echo "ANDROID_NDK_HOME is not set"; \
		exit 1; \
	fi
	cargo ndk -o ./$(ANDROID_JNI) \
		--manifest-path ./Cargo.toml \
		-t armeabi-v7a \
		-t arm64-v8a \
		-t x86 \
		-t x86_64 \
		build -p $(LIB_NAME) --release

android-bindings:
	cargo run --bin uniffi-bindgen generate \
		--library ./$(RELEASE_DIR)/$(LIB_FILE) \
		--language kotlin \
		--out-dir ./$(ANDROID_KOTLIN_OUT)

android: build android-build android-bindings

#######################################
# All
#######################################

all: ios android java

.PHONY: build clean ios ios-build ios-bindings ios-xcframework ios-setup android android-build android-bindings android-setup all

#######################################
# Kotlin Binding for Java (Maven)
#######################################

JAVA_KOTLIN_OUT=javaApp/src/main/kotlin/org/example
JAVA_RESOURCES=javaApp/src/main/resources

java-build: build

java-bindings:
	cargo run --bin uniffi-bindgen generate \
		--library $(RELEASE_DIR)/$(LIB_FILE) \
		--language kotlin \
		--out-dir ./$(JAVA_KOTLIN_OUT)

java-copy-lib:
	cp $(RELEASE_DIR)/$(LIB_FILE) ./$(JAVA_RESOURCES)

java: java-build java-bindings java-copy-lib