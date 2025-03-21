# Using UniFFI binding iOS App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Swift for iOS. This section covers how to build and use Cedarling UniFFI binding in an iOS app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

## Building

1. Clone the jans monorepo and change directory to `/path/of/jans/jans-cedarling`.
   ```bash
    git clone https://github.com/JanssenProject/jans.git
    cd /path/of/jans/jans-cedarling
   ```

2. Build the library:
   ```bash
   cargo build --release
   ```
   In `target/debug`, you should find the `libmobile.dylib`, `libmobile.so`, or `libmobile.dll` file, depending on the operating system you are using.

   - **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
   - **.dylib** (Dynamic Library) – This is the shared library format for macOS.
   - **.dll** (Dynamic Link Library) - The shared library format used in Windows.

3. Generate the bindings for Swift by running the command below. Replace `{build_file}` with `libmobile.dylib`, `libmobile.so`, or `libmobile.dll`, depending on which file is generated in `target/release`.
   ```bash
   cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language swift --out-dir ./bindings/cedarling_uniffi/output
   ```

4. Building the iOS binaries and adding these targets to Rust.
   ```bash
   rustup target add aarch64-apple-ios-sim aarch64-apple-ios
   ```

5. Build the library for Swift.
   ```bash
   cargo build --release --target=aarch64-apple-ios-sim
   cargo build --release --target=aarch64-apple-ios
   ```
   You should have two binaries `target/aarch64-apple-ios-sim/release/libmobile.a` and `target/aarch64-apple-ios/release/libmobile.a`.

6. The XCFramework will allow us to import the library with zero effort in Xcode. First, we need to rename the file ./bindings/cedarling_uniffi/output/mobileFFI.modulemap to ./bindings/cedarling_uniffi/output/module.modulemap. Then, we can create the XCFramework:
   ```bash
   xcodebuild -create-xcframework \
           -library ./target/aarch64-apple-ios-sim/release/libmobile.a -headers ./bindings/cedarling_uniffi/output \
           -library ./target/aarch64-apple-ios/release/libmobile.a -headers ./bindings/cedarling_uniffi/output \
           -output "ios/Mobile.xcframework"
   ```

7. Open `./jans-cedarling/bindings/cedarling_uniffi/iOSApp` in Xcode. Import both the XCFramework Mobile.xcframework and the Swift file bindings `bindings/output/mobile.swift` files into your project (drag and drop should work).

8. Run iOS project on simulator.

We have included [a sample iOS app](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_uniffi/iOSApp) using Cedarling UniFFI binding for making authorisation decisions. Here is a demonstration video of its working.


<div style="position: relative; padding-bottom: 214.62686567164178%; height: 0;"><iframe src="https://www.loom.com/embed/9ee1a9cf5cc04c1ea17fdc638ca45625?sid=c22f2463-22f0-4531-8749-403a1e6a25db" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"></iframe></div>