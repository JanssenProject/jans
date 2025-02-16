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
cargo build
```

In target/debug, you should find the libmobile.dylib file.

2. Generate the bindings:

```bash
cargo run --bin uniffi-bindgen generate --library ./target/debug/libmobile.dylib --language swift --out-dir ./bindings/cedarling_uniffi/output
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

- WIP