# Using UniFFI binding iOS App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Swift for iOS. This section covers how to build and use Cedarling UniFFI binding in an iOS app.

## Using Pre-Built iOS Release Assets

If you do not want to build from source, you can use the pre-built XCFramework published with each Janssen release. This is the recommended approach for most iOS app developers.

### Prerequisites

- iOS 14+ deployment target
- Xcode 14 or later
- Go to the Janssen [releases](https://github.com/JanssenProject/jans/releases) page and download the following two files for your target version:

| File                               | Purpose                                                                            |
| ---------------------------------- | ---------------------------------------------------------------------------------- |
| cedarling_uniffi-ios-{version}.zip | The XCFramework containing Cedarling's compiled libraries for device and simulator |
| cedarling_uniffi.swift             | UniFFI-generated Swift source that exposes the Rust API                            |

1. Unzip cedarling_uniffi-ios-{version}.zip to get Cedarling.xcframework.
1. Open your project in Xcode and click the project name at the top of the file navigator. Select your app target (not the project) under Targets.
1. Open the General tab and scroll down to Frameworks, Libraries, and Embedded Content. Drag `Cedarling.xcframework` into the list. Set the embed option to **Do Not Embed** (the XCFramework contains static libraries — embedding will cause a codesigning error at archive time).
1. Drag `cedarling_uniffi.swift` into your project's source group in the file navigator. When prompted, ensure **Add to target** is checked for your app target.
1. Build and Run the project to verify the setup.

## Manually Built iOS Assets

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).

- Xcode 14 or later

- Clone the jans monorepo and change directory to `/path/of/jans/jans-cedarling/bindings/cedarling_uniffi`.

  ```
   git clone https://github.com/JanssenProject/jans.git
   cd /path/of/jans/jans-cedarling/bindings/cedarling_uniffi
  ```

- Ask toolchain manager to install support for compiling Rust code for iOS devices and iOS Simulator

  ```
  rustup target add aarch64-apple-darwin aarch64-apple-ios-sim aarch64-apple-ios
  ```

- Run below command to build and import binding into iOS project:

  ```
  make ios
  ```

  Use `make ios BUILD_TYPE=release` or `make ios BUILD_TYPE=debug` to build in `release` or `debug` mode. If `BUILD_TYPE` is not specified, the `release` profile is used by default.

- Open `./bindings/cedarling_uniffi/iOSApp` in Xcode. Import both the XCFramework `./bindings/ios/Mobile.xcframework` and the Swift file bindings `./bindings/build/cedarling_uniffi.swift` files into your project (drag and drop should work).

- Run iOS project on simulator.

We have included [a sample iOS app](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_uniffi/iOSApp) using Cedarling UniFFI binding for making authorisation decisions. Here is a demonstration video of its working.
