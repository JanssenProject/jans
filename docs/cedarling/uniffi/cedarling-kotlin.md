# Cedarling Kotlin binding 

[UniFFI](https://mozilla.github.io/uniffi-rs/latest/) (Universal Foreign Function Interface) is a tool developed by Mozilla to simplify cross-language bindings, primarily between Rust and other languages like Kotlin, Swift, and Python. It allows Rust libraries to be used in these languages without manually writing complex foreign function interface (FFI) bindings.

Please refer to [this document](./cedarling-uniffi.md) for details on the structs, enums, and functions exposed by UniFFI bindings. This section outlines the steps to generate the Cedarling Kotlin binding and use it in a sample Java Maven project.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 17 or higher
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

## Building Kotlin binding

1. Build Cedarling:

    ```bash
    cargo build --release
    ```
In `target/release`, you should find the `libmobile.dylib`, `libmobile.so`, or `libmobile.dll` file, depending on the operating system you are using.

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libmobile.dylib`, `libmobile.so`, or `libmobile.dll`, depending on which file is generated in `target/release`.

    ```bash
    cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/javaApp/src/main/java/org/example
    ```

3. Copy the generated `libmobile.dylib`, `libmobile.so`, or `libmobile.dll` file to resource directory of the sample Java Maven project. Replace `{build_file}` in the below commad with `libmobile.dylib`, `libmobile.so`, or `libmobile.dll`, depending on which file is generated in `target/release`.

    ```bash
    cp ./target/release/{build_file} ./bindings/cedarling_uniffi/javaApp/src/main/resources
    ```

4. Change directory to sample Java project (`./bindings/cedarling_uniffi/javaApp`) and run below command to run the main method of a Maven project from the terminal.

    ```bash
     mvn exec:java -Dexec.mainClass="org.example.Main"
    ```

The method will execute the steps for Cedarling initialization with a sample bootstrap configuration, run authorization with sample tokens, resource and context inputs and call log interface to print authorization logs on console. 

## Sample Java Maven Project

Note the following points in the sample Java Maven project to understand the changes required for using the Kotlin binding in other Java projects.

1. The sample `tokens`, `resource` and `context` input files along with files containing `bootstrap configuration` and `policy- store` used by the sample application are present at `./bindings/cedarling_uniffi/javaApp/src/main/resources/config`.
2. Refer to the Java code in org.example.Main to see how Cedarling's `init`, `authz`, and `log` interfaces are called using the Kotlin binding.
3. Added dependencies in pom.xml:

- **Java Native Access (JNA):** A Java library that allows Java code to call native shared libraries (like .so on Linux, .dll on Windows, or .dylib on macOS) without writing JNI (Java Native Interface) code. 
- **kotlinx.coroutines:** Adds support for asynchronous programming using coroutines.
- **kotlin-stdlib-jdk:** The kotlin-stdlib-jdk8 library is a variant of the Kotlin standard library that includes additional features specifically designed to work with JDK 8 (Java Development Kit 8) or higher.
- **nimbus-jose-jwt:** The nimbus-jose-jwt library is a Java library used for working with JWTs (JSON Web Tokens) and JOSE (JavaScript Object Signing and Encryption) standards
- **jackson-databind:** The jackson-databind library is a core module of the Jackson JSON processing framework in Java
