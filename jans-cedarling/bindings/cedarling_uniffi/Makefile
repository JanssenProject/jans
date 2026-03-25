# Makefile for Cedarling UniFFI (iOS + Android)

# Variables
LIB_NAME=cedarling_uniffi
TARGET_DIR=../../target
RELEASE_DIR=$(TARGET_DIR)/release
IOS_OUT=ios/Mobile.xcframework
BINDINGS_DIR=output
ANDROID_JNI=androidApp/app/src/main/jniLibs
ANDROID_KOTLIN_OUT=androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi

# Detect OS-specific library
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
	LIB_FILE=lib$(LIB_NAME).dylib
else ifeq ($(UNAME_S),Linux)
	LIB_FILE=lib$(LIB_NAME).so
else
	LIB_FILE=$(LIB_NAME).dll
endif

#######################################
# Common
#######################################

build:
	cargo build -r -p $(LIB_NAME)

clean:
	cargo clean

#######################################
# iOS
#######################################

ios-setup:
	rustup target add aarch64-apple-ios-sim aarch64-apple-ios

ios-build: build ios-setup
	cargo build --release --target=aarch64-apple-ios-sim
	cargo build --release --target=aarch64-apple-ios

ios-bindings:
	cargo run --bin uniffi-bindgen generate \
		--library ./$(RELEASE_DIR)/$(LIB_FILE) \
		--language swift \
		--out-dir ./$(BINDINGS_DIR)

ios-xcframework: ios-build ios-bindings
	mv $(BINDINGS_DIR)/cedarling_uniffiFFI.modulemap $(BINDINGS_DIR)/module.modulemap || true
	xcodebuild -create-xcframework \
		-library ../../target/aarch64-apple-ios-sim/release/lib$(LIB_NAME).a -headers ./$(BINDINGS_DIR) \
		-library ../../target/aarch64-apple-ios/release/lib$(LIB_NAME).a -headers ./$(BINDINGS_DIR) \
		-output "$(IOS_OUT)"

ios: ios-xcframework

#######################################
# Android
#######################################

android-setup:
	cargo install cargo-ndk || true
	rustup target add \
		aarch64-linux-android \
		armv7-linux-androideabi \
		i686-linux-android \
		x86_64-linux-android

android-build: android-setup
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

all: ios android

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