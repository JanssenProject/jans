# Using UniFFI binding Android App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Kotlin for Android. This section covers how to build and use Cedarling UniFFI binding in an Android app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

## Building

1. Build the library:
    ```bash
    cargo build
    ```
In target/debug, you should find the libmobile.dylib file.

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
            build --release
    ```

5. Generate the bindings for Kotlin:
    ```
    cargo run --bin uniffi-bindgen generate --library ./target/debug/libmobile.dylib --language kotlin --out-dir ./bindings/cedarling_uniffi/androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi
    ```

6. We have included a sample android app using Cedarling UniFFI binding for making authorisation decisions. Open the `androidApp` project on Android Studio and run the project on simulator.

<div style="position: relative; padding-bottom: 104.75728155339806%; height: 0;"><iframe src="https://www.loom.com/embed/463de78bd3174f2ca7d2b2f2fb2915cd?sid=01bd3481-857f-4981-9414-e81852fa3079" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"></iframe></div>