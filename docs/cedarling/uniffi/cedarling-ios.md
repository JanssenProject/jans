# Using UniFFI binding iOS App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Swift for iOS. This section covers how to build and use Cedarling UniFFI binding in an iOS app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

## Building

1. Clone the jans monorepo and change directory to /path/of/jans/jans-cedarling

   ```
    git clone https://github.com/JanssenProject/jans.git
    cd /path/of/jans/jans-cedarling
   ```

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

We have included [a sample iOS app](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_uniffi/iOSApp) using Cedarling UniFFI binding for making authorisation decisions. Here is a demonstration video of its working.

<iframe src="https://www.loom.com/share/553e53a95a4a4e40ae54fc256538d7f9?sid=2e80f4bb-326d-4aa2-8d3b-f6abe62956db" allowfullscreen></iframe>
