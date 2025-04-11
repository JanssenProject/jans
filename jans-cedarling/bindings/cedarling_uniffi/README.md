# Cedarling UniFFI binding

This module is designed to build Cedarling UniFFI binding for iOS and android apps.

## iOS

Read [this article](https://medium.com/@arnab.bdutta/janssen-cedarling-uniffi-bindings-for-native-apps-90f36982c894) for details.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

### Building

1. Build the library:

```bash
cargo build --release
```

In `target/release`, you should find the `libmobile.dylib`, `libmobile.so`, or `libmobile.dll` file, depending on the operating system you are using.

- **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
- **.dylib** (Dynamic Library) – This is the shared library format for macOS.
- **.dll** (Dynamic Link Library) - The shared library format used in Windows.

2. Generate the bindings for Swift by running the command below. Replace `{build_file}` with `libmobile.dylib`, `libmobile.so`, or `libmobile.dll`, depending on which file is generated in `target/release`.

```bash
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language swift --out-dir ./bindings/cedarling_uniffi/output
```

3. Building the iOS binaries and adding these targets to Rust.

```bash
rustup target add aarch64-apple-ios-sim aarch64-apple-ios
```

4. Build the library for Swift.

```bash
cargo build --release --target=aarch64-apple-ios-sim
cargo build --release --target=aarch64-apple-ios
```

You should have two binaries target/aarch64-apple-ios-sim/release/libmobile.a and target/aarch64-apple-ios/release/libmobile.a.

5. The XCFramework will allow us to import the library with zero effort in Xcode. First, we need to rename the file ./bindings/cedarling_uniffi/output/mobileFFI.modulemap to ./bindings/cedarling_uniffi/output/module.modulemap.

Then, we can create the XCFramework:

```bash
xcodebuild -create-xcframework \
        -library ./target/aarch64-apple-ios-sim/release/libmobile.a -headers ./bindings/cedarling_uniffi/output \
        -library ./target/aarch64-apple-ios/release/libmobile.a -headers ./bindings/cedarling_uniffi/output \
        -output "ios/Mobile.xcframework"
```

6. Open ./jans-cedarling/bindings/cedarling_uniffi/iOSApp in Xcode. Import both the XCFramework Mobile.xcframework and the Swift file bindings bindings/output/mobile.swift files into your project (drag and drop should work).

7. Run iOS project on simulator.

## Android

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

### Building

1. Build the library:

```bash
cargo build --release
```

In `target/release`, you should find the `libmobile.dylib`, `libmobile.so`, or `libmobile.dll` file, depending on the operating system you are using.

- **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
- **.dylib** (Dynamic Library) – This is the shared library format for macOS.
- **.dll** (Dynamic Link Library) - The shared library format used in Windows.

2. Set up cargo-ndk for cross-compiling:

```
cargo install cargo-ndk

```

3. Add targets for Android:

```
rustup target add \
        aarch64-linux-android \
        armv7-linux-androideabi \
        i686-linux-android \
        x86_64-linux-android
```

4. Compile the dynamic libraries in ./app/src/main/jniLibs (next to java and res directories) in the sample `androidApp` project.

```
cargo ndk -o ./bindings/cedarling_uniffi/androidApp/app/src/main/jniLibs \
        --manifest-path ./Cargo.toml \
        -t armeabi-v7a \
        -t arm64-v8a \
        -t x86 \
        -t x86_64 \
        build \
        -p cedarling_uniffi --release
```

5. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libmobile.dylib`, `libmobile.so`, or `libmobile.dll`, depending on which file is generated in `target/release`.


```
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi

```

6. Open the `androidApp` project on Android Studio and run the project on simulator. 