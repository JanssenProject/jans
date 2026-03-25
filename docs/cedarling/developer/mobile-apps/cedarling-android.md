
# Using UniFFI binding Android App

Cedarling UniFFI binding exposes its `init`, `authz` and `log` interfaces to different languages and on all the different platforms, including Kotlin for Android. This section covers how to build and use Cedarling UniFFI binding in an Android app.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

## Building

1.  Run below command to build and import binding into Android project.
    ```bash
    make android
    ```
  
2. We have included a sample android app using Cedarling UniFFI binding for making authorisation decisions. Open the `./bindings/cedarling_uniffi/androidApp` project on Android Studio and run the project on simulator.

<div style="position: relative; padding-bottom: 104.75728155339806%; height: 0;"><iframe src="https://www.loom.com/embed/463de78bd3174f2ca7d2b2f2fb2915cd?sid=01bd3481-857f-4981-9414-e81852fa3079" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"></iframe></div>
