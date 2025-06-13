# Cedarling Java binding

[UniFFI](https://mozilla.github.io/uniffi-rs/latest/) (Universal Foreign Function Interface) is a tool developed by Mozilla to simplify cross-language bindings, primarily between Rust and other languages like Kotlin, Swift, and Python. It allows Rust libraries to be used in these languages without manually writing complex foreign function interface (FFI) bindings.

Please refer to [this document](./cedarling-uniffi.md) for details on the structs, enums, and functions exposed by UniFFI bindings. This section outlines the process of generating the Kotlin binding for Cedarling using Cedarling UniFFI. The Kotlin binding is then wrapped in a Java class to enable convenient use in Java applications.

## Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 11 or higher
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

## Building from Source

1. Build Cedarling by executing below command from `./jans/jans-cedarling` of cloned jans project:
   ```bash
   cargo build -r -p cedarling_uniffi
   ```
   In `target/release`, you should find the `libcedarling_uniffi.dylib` (if Mac OS), `libcedarling_uniffi.so` (if Linux OS), or `libcedarling_uniffi.dll` (if Windows OS) file, depending on the operating system you are using.
   **Note:** You can use pre-built `libcedarling_uniffi.so` from the [Jans releases page](https://github.com/JanssenProject/jans/releases). Ref: `https://github.com/JanssenProject/jans/releases/download/{version}/libcedarling_uniffi-{version}.so`

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.
   ```bash
   cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling-java/src/main/kotlin/io/jans/cedarling
   ```
   **Note:** You can use pre-built kotlin binding from the [Jans releases page](https://github.com/JanssenProject/jans/releases). Ref: `https://github.com/JanssenProject/jans/releases/download/{version}/cedarling_uniffi-kotlin-{version}.zip`

3. Copy the generated `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file to resource directory of the `cedarling-java` Maven project. Replace `{build_file}` in the below commad with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.
   ```bash
   mkdir ./bindings/cedarling-java/src/main/resources
   cp ./target/release/{build_file} ./bindings/cedarling-java/src/main/resources
   ```

4. Change directory to `./bindings/cedarling-java` and run below command to build `cedarling-java` jar file. This will generate `cedarling-java-{version}-distribution.jar` at `./bindings/cedarling-java/target/`.
   ```bash
    mvn clean install
   ```
   
### Using Cedarling-java Maven dependency

If you are using pre-built binaries, add the following `repository` and `dependency` to the `pom.xml` of your Java Maven project to use Cedarling Java bindings.

```declarative
    <repositories>
        <repository>
            <id>jans</id>
            <name>Janssen project repository</name>
            <url>https://maven.jans.io/maven</url>
        </repository>
    </repositories>
```
```declarative
        <dependency>
            <groupId>io.jans</groupId>
            <artifactId>cedarling-java</artifactId>
            <version>{latest-jans-stable-version}</version>
        </dependency>
```

## Recipes

### **Recipe 1:** Using the Cedarling Java binding in custom scripts on the Janssen Auth Server (VM installation).

**Note:** This recipe is compatible with Jans version 1.4.0 and earlier.

- Upload [bootstrap.json](./cedarling-sample-inputs.md/#bootstrapjson), [policy-store.json](./cedarling-sample-inputs.md/#policy-storejson), [action.txt](./cedarling-sample-inputs.md/#actiontxt), [context.json](./cedarling-sample-inputs.md/#contextjson), [principals.json](./cedarling-sample-inputs.md/#principalsjson) and [resource.json](./cedarling-sample-inputs.md/#resourcejson) at `/opt/jans/jetty/jans-auth/custom/static` location of the auth server. The [Asset Screen](https://docs.jans.io/v1.6.0/janssen-server/config-guide/custom-assets-configuration/#asset-screen) can be used to upload assets.
- Upload the generate `cedarling-java-{version}-distribution.jar` at `/opt/jans/jetty/jans-auth/custom/libs` location of the auth server.
- The following Post Authn script has been created for calling Cedarling authorization. Add and enable the [Post Authn custom script](./cedarling-sample-inputs.md/#sample_cedarling_post_authntxt) (in Java) with following Custom Properties:
   
   |Key|Values|
   |---|------|
   |BOOTSTRAP_JSON_PATH|./custom/static/bootstrap.json|
   |ACTION_FILE_PATH|./custom/static/action.txt|
   |RESOURCE_FILE_PATH|./custom/static/resource.json|
   |CONTEXT_FILE_PATH|./custom/static/context.json|
   |PRINCIPALS_FILE_PATH|./custom/static/principals.json|

- Map the script with client used to perform authentication.
   ![](../../assets/cedarling-adding-client-script.png)

- The script runs after client authentication to invoke Cedarling authz.

### **Recipe 2:** Sample Java Maven project using the Kotlin binding

1. Build Cedarling by executing below command from `./jans/jans-cedarling` of cloned jans project:
    ```bash
    cargo build -r -p cedarling_uniffi
    ```
   In `target/release`, you should find the `libcedarling_uniffi.dylib` (if Mac OS), `libcedarling_uniffi.so` (if Linux OS), or `libcedarling_uniffi.dll` (if Windows OS) file, depending on the operating system you are using.
   **Note:** You can use pre-built `libcedarling_uniffi.so` from the [Jans releases page](https://github.com/JanssenProject/jans/releases). Ref: `https://github.com/JanssenProject/jans/releases/download/{version}/libcedarling_uniffi-{version}.so`

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.
    ```bash
    cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/javaApp/src/main/kotlin/org/example
    ```
   **Note:** You can use pre-built kotlin binding from the [Jans releases page](https://github.com/JanssenProject/jans/releases). Ref: `https://github.com/JanssenProject/jans/releases/download/{version}/cedarling_uniffi-kotlin-{version}.zip`

3. Copy the generated `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file to resource directory of the sample Java Maven project. Replace `{build_file}` in the below commad with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.
    ```bash
    cp ./target/release/{build_file} ./bindings/cedarling_uniffi/javaApp/src/main/resources
    ```git add 

4. Change directory to sample Java project (`./bindings/cedarling_uniffi/javaApp`) and run below command to run the main method of a Maven project from the terminal.
    ```bash
     mvn clean install
     mvn exec:java -Dexec.mainClass="org.example.Main"
    ```
The method will execute the steps for Cedarling initialization with a sample bootstrap configuration, run authorization with sample tokens, resource and context inputs and call log interface to print authorization logs on console.

#### Sample Java Maven Project

Note the following points in the sample Java Maven project to understand the changes required for using the Kotlin binding in other Java projects.

- The sample `tokens`, `resource` and `context` input files along with files containing `bootstrap configuration` and `policy- store` used by the sample application are present at `./bindings/cedarling_uniffi/javaApp/src/main/resources/config`.
- Refer to the Java code in org.example.Main to see how Cedarling's `init`, `authz`, and `log` interfaces are called using the Kotlin binding.

##### Added dependencies in pom.xml:

- **Java Native Access (JNA):** A Java library that allows Java code to call native shared libraries (like .so on Linux, .dll on Windows, or .dylib on macOS) without writing JNI (Java Native Interface) code.

- **kotlinx.coroutines:** Adds support for asynchronous programming using coroutines.

- **kotlin-stdlib-jdk:** The kotlin-stdlib-jdk8 library is a variant of the Kotlin standard library that includes additional features specifically designed to work with JDK 8 (Java Development Kit 8) or higher.

- **nimbus-jose-jwt:** The nimbus-jose-jwt library is a Java library used for working with JWTs (JSON Web Tokens) and JOSE (JavaScript Object Signing and Encryption) standards

- **jackson-databind:** The jackson-databind library is a core module of the Jackson JSON processing framework in Java
