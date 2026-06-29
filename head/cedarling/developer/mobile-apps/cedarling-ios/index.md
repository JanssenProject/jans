# Using UniFFI binding iOS App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Swift for iOS. This section covers how to build and use Cedarling UniFFI binding in an iOS app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

## Building

1. Clone the jans monorepo and change directory to `/path/of/jans/jans-cedarling/bindings/cedarling_uniffi`.

   ```
    git clone https://github.com/JanssenProject/jans.git
    cd /path/of/jans/jans-cedarling/bindings/cedarling_uniffi
   ```

1. Ask toolchain manager to install support for compiling Rust code for iOS devices and iOS Simulator

   ```
   rustup target add aarch64-apple-darwin aarch64-apple-ios-sim aarch64-apple-ios
   ```

1. Run below command to build and import binding into iOS project:

   ```
   make ios
   ```

   Use `make ios BUILD_TYPE=release` or `make ios BUILD_TYPE=debug` to build in `release` or `debug` mode. If `BUILD_TYPE` is not specified, the `release` profile is used by default.

1. Open `./bindings/cedarling_uniffi/iOSApp` in Xcode. Import both the XCFramework `./bindings/ios/Mobile.xcframework` and the Swift file bindings `./bindings/build/cedarling_uniffi.swift` files into your project (drag and drop should work).

1. Run iOS project on simulator.

We have included [a sample iOS app](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_uniffi/iOSApp) using Cedarling UniFFI binding for making authorisation decisions. Here is a demonstration video of its working.
