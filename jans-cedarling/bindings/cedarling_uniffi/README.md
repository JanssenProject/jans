# Cedarling UniFFI binding

This module is designed to build Cedarling UniFFI binding for iOS and android apps. The Kotlin binding can be Java Projects to run cedarling authz.

## iOS

Read [this article](https://medium.com/@arnab.bdutta/janssen-cedarling-uniffi-bindings-for-native-apps-90f36982c894) for details.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

### Building

1. Build the library:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file, depending on the operating system you are using.

- **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
- **.dylib** (Dynamic Library) – This is the shared library format for macOS.
- **.dll** (Dynamic Link Library) - The shared library format used in Windows.

2. Generate the bindings for Swift by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

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

You should have two binaries target/aarch64-apple-ios-sim/release/libcedarling_uniffi.a and target/aarch64-apple-ios/release/libcedarling_uniffi.a.

5. The XCFramework will allow us to import the library with zero effort in Xcode. First, we need to rename the file ./bindings/cedarling_uniffi/output/cedarling_uniffiFFI.modulemap to ./bindings/cedarling_uniffi/output/module.modulemap.

Then, we can create the XCFramework:

```bash
xcodebuild -create-xcframework \
        -library ./target/aarch64-apple-ios-sim/release/libcedarling_uniffi.a -headers ./bindings/cedarling_uniffi/output \
        -library ./target/aarch64-apple-ios/release/libcedarling_uniffi.a -headers ./bindings/cedarling_uniffi/output \
        -output "ios/Mobile.xcframework"
```

6. Open ./jans-cedarling/bindings/cedarling_uniffi/iOSApp in Xcode. Import both the XCFramework Mobile.xcframework and the Swift file bindings bindings/output/cedarling_uniffi.swift files into your project (drag and drop should work).

7. Run iOS project on simulator.

## Android

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

### Building

1. Build the library:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file, depending on the operating system you are using.

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

5. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi

```

6. Open the `androidApp` project on Android Studio and run the project on simulator.

## Kotlin Binding

Here we delve into the process of generating the Kotlin binding for cedarling and use it in a sample Java Maven project to run the authorization.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 11
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

### Building and Testing

1. Build Cedarling:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib` (if Mac OS), `libcedarling_uniffi.so` (if Linux OS), or `libcedarling_uniffi.dll` (if Windows OS) file, depending on the operating system you are using.

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/javaApp/src/main/kotlin/org/example
```

3. Copy the generated `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file to resource directory of the sample Java Maven project. Replace `{build_file}` in the below commad with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cp ./target/release/{build_file} ./bindings/cedarling_uniffi/javaApp/src/main/resources
```

4. Change directory to sample Java project (`./bindings/cedarling_uniffi/javaApp`) and run below command to run the main method of a Maven project from the terminal.

```bash
 mvn clean install
 mvn exec:java -Dexec.mainClass="org.example.Main"
```

The method will execute the steps for Cedarling initialization with a sample bootstrap configuration, run authorization with sample tokens, resource and context inputs and call log interface to print authorization logs on console. The sample `tokens`, `resource` and `context` input files used by the sample application are present at `./bindings/cedarling_uniffi/javaApp/src/main/resources/config`.

## Configuration

### ID Token Trust Mode

The `CEDARLING_ID_TOKEN_TRUST_MODE` property controls how ID tokens are validated:

- **`strict`** (default): Enforces strict validation rules
  - ID token `aud` must match access token `client_id`
  - If userinfo token is present, its `sub` must match the ID token `sub`
- **`never`**: Disables ID token validation (useful for testing)
- **`always`**: Always validates ID tokens when present
- **`ifpresent`**: Validates ID tokens only if they are provided

### Testing Configuration

For testing scenarios, you may want to disable JWT validation. You can configure this in your bootstrap configuration:

```json
{
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
  "CEDARLING_ID_TOKEN_TRUST_MODE": "never"
}
```

For complete configuration documentation, see [cedarling-properties.md](../../../docs/cedarling/cedarling-properties.md).
