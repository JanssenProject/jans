## Pre-requisite  for set-up:
1. Set up cargo-ndk for cross-compiling:
```
cargo install cargo-ndk
```
2. Add targets for Android:

```
rustup target add \
        aarch64-linux-android \
        armv7-linux-androideabi \
        i686-linux-android \
        x86_64-linux-android
```

## How to Run  
Run following command sequentially:
1. cargo build
2. cargo ndk -o ./bindings/cedarling_android/androidProj/app/src/main/jniLibs --manifest-path ./Cargo.toml -t armeabi-v7a -t arm64-v8a -t x86 -t x86_64 build --release
3. cargo run --bin uniffi-bindgen generate --library ./target/debug/libmobile.dylib --language kotlin --out-dir ./bindings/cedarling_android/androidProj/app/src/main/java/com/example/rust_android
4. Open android project `./bindings/cedarling_android/androidProj` using Android Studio and run the project using emulator.

## References:
1. https://forgen.tech/en/blog/post/building-an-android-app-with-rust-using-uniffi#step-1-setting-up-your-rust-library
2. https://mozilla.github.io/uniffi-rs/latest/