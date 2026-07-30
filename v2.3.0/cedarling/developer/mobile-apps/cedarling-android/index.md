# Using UniFFI binding Android App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Kotlin for Android. This section covers how to build and use Cedarling UniFFI binding in an Android app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

## Building

1. Ask toolchain manager to install support for compiling Rust code for aarch64-linux, armv7-linux, i686-linux and x86_64-linux.

   ```
       rustup target add \
           aarch64-linux-android \
           armv7-linux-androideabi \
           i686-linux-android \
           x86_64-linux-android
   ```

1. Run below command to build and import binding into Android project.

   ```
   make android
   ```

   Use `make android BUILD_TYPE=release` or `make android BUILD_TYPE=debug` to build in `release` or `debug` mode. If `BUILD_TYPE` is not specified, the `release` profile is used by default.

1. We have included a sample android app using Cedarling UniFFI binding for making authorisation decisions. Open the `./bindings/cedarling_uniffi/androidApp` project on Android Studio.

1. Press ctrl key twice on Android Studio to open Run Anything dialog.

1. Enter `gradle wrapper --gradle-version 8.7` and press enter key. This will generate gradle wrapper at `{jans_monorep_path}/jans-cedarling/bindings/cedarling_uniffi/androidApp/gradle/wrapper`.

1. Run the project on simulator.
