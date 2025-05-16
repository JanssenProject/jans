# Cedarling Java binding

This guide explores the process of generating the Kotlin binding for Cedarling using [Cedarling UniFFI](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_uniffi). The Kotlin binding is then wrapped in a Java class to enable convenient use in Java applications.

## Building from Source

If you are using pre-built binaries from the [Jans releases page](https://github.com/JanssenProject/jans/releases), you can skip this step. Otherwise, follow these instructions to build from source.

### Prerequisites:

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 17
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

### Building Kotlin binding

1. Build Cedarling:

```bash
cargo build --release
```
In `target/release`, you should find the `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file, depending on the operating system you are using.

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling-java/src/main/kotlin/io/jans/cedarling
```

3. Copy the generated `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file to resource directory of the `cedarling-java` Maven project. Replace `{build_file}` in the below commad with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cp ./target/release/{build_file} ./bindings/cedarling-java/src/main/resources
```

4. Run below command to build `cedarling-java` jar file. This will generate `cedarling-java-{version}-distribution.jar` at `./bindings/cedarling-java/target/`.

```bash
 mvn clean install
```

## Build  with dynamic linking

WIP

## Recipes

### Using the Cedarling Java binding in custom scripts on the Janssen Auth Server (VM installation).

1. Upload [bootstrap.json](./docs/bootstrap.json), [policy-store.json](./docs/policy-store.json), [action.txt](./docs/action.txt), [context.json](./docs/context.json), [principals.json](./docs/principals.json) and [resource.json](./docs/resource.json) at `/opt/jans/jetty/jans-auth/custom/static` location of the auth server.
2. Upload the generate `cedarling-java-{version}-distribution.jar` at `/opt/jans/jetty/jans-auth/custom/libs` location of the auth server.
3. The following Post Authn script has been created for calling Cedarling authorization. Add and enable the following [Post Authn custom script](./docs/sample_cedarling_post_authn.txt) (in Java) with following Custom Properties:

|Key|Values|
|---|------|
|BOOTSTRAP_JSON_PATH|./custom/static/bootstrap.json|
|ACTION_FILE_PATH|./custom/static/action.txt|
|RESOURCE_FILE_PATH|./custom/static/resource.json|
|CONTEXT_FILE_PATH|./custom/static/context.json|
|PRINCIPALS_FILE_PATH|./custom/static/principals.json|

**Note:** The [Asset Sreen](https://docs.jans.io/v1.6.0/janssen-server/config-guide/custom-assets-configuration/#asset-screen) can be used to upload assets.

4. Map the script with client used to perform authentication.

![](./docs/mapping_post_authn_script_with_client.png)

5. The script runs after client authentication to invoke Cedarling authz.
