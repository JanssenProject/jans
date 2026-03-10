# Cedarling UniFFI binding

This module is designed to build Cedarling UniFFI bindings for iOS and Android apps. The Kotlin bindings can also be used from Java projects to run Cedarling authorization.

## iOS

Read [this article](https://medium.com/@arnab.bdutta/janssen-cedarling-uniffi-bindings-for-native-apps-90f36982c894) for details.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

### Building

1. Build the library:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file, depending on the operating system you are using.

- **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
- **.dylib** (Dynamic Library) – This is the shared library format for macOS.
- **.dll** (Dynamic Link Library) - The shared library format used in Windows.

2. Generate the bindings for Swift by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language swift --out-dir ./bindings/cedarling_uniffi/output
```

3. Building the iOS binaries and adding these targets to Rust.

```bash
rustup target add aarch64-apple-ios-sim aarch64-apple-ios
```

4. Build the library for Swift.

```bash
cargo build --release --target=aarch64-apple-ios-sim
cargo build --release --target=aarch64-apple-ios
```

You should have two binaries target/aarch64-apple-ios-sim/release/libcedarling_uniffi.a and target/aarch64-apple-ios/release/libcedarling_uniffi.a.

5. The XCFramework will allow us to import the library with zero effort in Xcode. First, we need to rename the file ./bindings/cedarling_uniffi/output/cedarling_uniffiFFI.modulemap to ./bindings/cedarling_uniffi/output/module.modulemap.

Then, we can create the XCFramework:

```bash
xcodebuild -create-xcframework \
        -library ./target/aarch64-apple-ios-sim/release/libcedarling_uniffi.a -headers ./bindings/cedarling_uniffi/output \
        -library ./target/aarch64-apple-ios/release/libcedarling_uniffi.a -headers ./bindings/cedarling_uniffi/output \
        -output "ios/Mobile.xcframework"
```

6. Open ./jans-cedarling/bindings/cedarling_uniffi/iOSApp in Xcode. Import both the XCFramework Mobile.xcframework and the Swift file bindings bindings/output/cedarling_uniffi.swift files into your project (drag and drop should work).

7. Run iOS project on simulator.

## Android

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

### Building

1. Build the library:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file, depending on the operating system you are using.

- **.so** (Shared Object) – This is the shared library format used in Linux and other Unix-based operating systems.
- **.dylib** (Dynamic Library) – This is the shared library format for macOS.
- **.dll** (Dynamic Link Library) - The shared library format used in Windows.

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
        build \
        -p cedarling_uniffi --release
```

5. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/androidApp/app/src/main/java/com/example/androidapp/cedarling/uniffi

```

6. Open the `androidApp` project on Android Studio and run the project on simulator.

## Kotlin Binding

Here we delve into the process of generating the Kotlin binding for cedarling and use it in a sample Java Maven project to run the authorization.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 11
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

### Building and Testing

1. Build Cedarling:

```bash
cargo build -r -p cedarling_uniffi
```

In `target/release`, you should find the `libcedarling_uniffi.dylib` (if Mac OS), `libcedarling_uniffi.so` (if Linux OS), or `libcedarling_uniffi.dll` (if Windows OS) file, depending on the operating system you are using.

2. Generate the bindings for Kotlin by running the command below. Replace `{build_file}` with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cargo run --bin uniffi-bindgen generate --library ./target/release/{build_file} --language kotlin --out-dir ./bindings/cedarling_uniffi/javaApp/src/main/kotlin/org/example
```

3. Copy the generated `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll` file to resource directory of the sample Java Maven project. Replace `{build_file}` in the below commad with `libcedarling_uniffi.dylib`, `libcedarling_uniffi.so`, or `libcedarling_uniffi.dll`, depending on which file is generated in `target/release`.

```bash
cp ./target/release/{build_file} ./bindings/cedarling_uniffi/javaApp/src/main/resources
```

4. Change directory to sample Java project (`./bindings/cedarling_uniffi/javaApp`) and run below command to run the main method of a Maven project from the terminal.

```bash
 mvn clean install
 mvn exec:java -Dexec.mainClass="org.example.Main"
```

The method will execute the steps for Cedarling initialization with a sample bootstrap configuration, run authorization with sample tokens, resource and context inputs and call log interface to print authorization logs on console. The sample `tokens`, `resource` and `context` input files used by the sample application are present at `./bindings/cedarling_uniffi/javaApp/src/main/resources/config`.

## Configuration

### Policy Store Sources

Cedarling supports multiple ways to load policy stores:

#### Legacy Single-File Formats

```json
{
  "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json",
  "CEDARLING_POLICY_STORE_URI": "https://lock-server.example.com/policy-store"
}
```

#### New Directory-Based Format

Policy stores can be structured as directories with human-readable Cedar files:

```text
policy-store/
├── metadata.json           # Required: Store metadata (id, name, version)
├── manifest.json           # Optional: File checksums for integrity validation
├── schema.cedarschema      # Required: Cedar schema (human-readable)
├── policies/               # Required: .cedar policy files
│   ├── allow-read.cedar
│   └── deny-guest.cedar
├── templates/              # Optional: .cedar template files
├── entities/               # Optional: .json entity files
└── trusted-issuers/        # Optional: .json issuer configurations
```

**metadata.json structure:**

```json
{
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies",
    "version": "1.0.0"
  }
}
```

#### Cedar Archive (.cjar) Format

Policy stores can be packaged as `.cjar` files (ZIP archives) for easy distribution:

- Single file for versioning and deployment
- Works across all platforms
- Supports integrity validation via manifest

### ID Token Trust Mode

The `CEDARLING_ID_TOKEN_TRUST_MODE` property controls how ID tokens are validated:

- **`strict`** (default): Enforces strict validation rules
  - ID token `aud` must match access token `client_id`
  - If userinfo token is present, its `sub` must match the ID token `sub`
- **`never`**: Disables ID token validation (useful for testing)
- **`always`**: Always validates ID tokens when present
- **`ifpresent`**: Validates ID tokens only if they are provided

### Testing Configuration

For testing scenarios, you may want to disable JWT validation. You can configure this in your bootstrap configuration:

```json
{
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
  "CEDARLING_ID_TOKEN_TRUST_MODE": "never"
}
```

For complete configuration documentation, see [cedarling-properties.md](../../../docs/cedarling/cedarling-properties.md).

## Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace.

**Note:** These Swift examples assume they are used inside a throwing context (e.g., within a function marked `throws`) or you can wrap calls in `do-catch` to handle errors. Methods like `getDataCtx`, `getDataEntryCtx`, `removeDataCtx`, `clearDataCtx`, `listDataCtx`, and `getStatsCtx` are throwing methods that can raise `DataException`.

### Push Data

Store data with an optional TTL (Time To Live):

**Kotlin:**

```kotlin
// JsonValue is represented as String in Kotlin/JVM bindings.
val value = """{"role":["admin","editor"],"country":"US"}"""

// Push data without TTL (uses default from config)
cedarling.pushDataCtx("user:123", value, null)

// Push data with TTL (5 minutes = 300 seconds)
cedarling.pushDataCtx("config:app", value, 300L)
```

**Swift:**

```swift
// Push data without TTL (uses default from config)
let value = JsonValue(value: "{\"role\":[\"admin\",\"editor\"],\"country\":\"US\"}")
try cedarling.pushDataCtx(key: "user:123", value: value, ttlSecs: nil)

// Push data with TTL (5 minutes = 300 seconds)
try cedarling.pushDataCtx(key: "config:app", value: value, ttlSecs: 300)
```

### Get Data

Retrieve stored data:

**Kotlin:**

```kotlin
val result = cedarling.getDataCtx("user:123")
if (result != null) {
    // result is already a JSON string
}
```

**Swift:**

```swift
if let result = try cedarling.getDataCtx(key: "user:123") {
    let jsonStr = result.inner()
    // Parse and use the JSON string
}
```

### Get Data Entry with Metadata

Get a data entry with full metadata including creation time, expiration, access count, and type:

**Kotlin:**

```kotlin
val entry = cedarling.getDataEntryCtx("user:123")
if (entry != null) {
    println("Key: ${entry.key}")
    println("Created at: ${entry.createdAt}")
    println("Expires at: ${entry.expiresAt}")
    println("Access count: ${entry.accessCount}")
    println("Data type: ${entry.dataType}")
}
```

**Swift:**

```swift
if let entry = try cedarling.getDataEntryCtx(key: "user:123") {
    print("Key: \(entry.key)")
    print("Created at: \(entry.createdAt)")
    print("Expires at: \(entry.expiresAt)")
    print("Access count: \(entry.accessCount)")
    print("Data type: \(entry.dataType)")
}
```

### Remove Data

Remove a specific entry:

**Kotlin:**

```kotlin
val removed = cedarling.removeDataCtx("user:123")
if (removed) {
    println("Entry was removed")
} else {
    println("Entry did not exist")
}
```

**Swift:**

```swift
let removed = try cedarling.removeDataCtx(key: "user:123")
if removed {
    print("Entry was removed")
} else {
    print("Entry did not exist")
}
```

### Clear All Data

Remove all entries from the data store:

**Kotlin:**

```kotlin
cedarling.clearDataCtx()
```

**Swift:**

```swift
try cedarling.clearDataCtx()
```

### List All Data

List all entries with their metadata:

**Kotlin:**

```kotlin
val entries = cedarling.listDataCtx()
entries.forEach { entry ->
    println("Key: ${entry.key}, Type: ${entry.dataType}, Created: ${entry.createdAt}")
}
```

**Swift:**

```swift
let entries = try cedarling.listDataCtx()
for entry in entries {
    print("Key: \(entry.key), Type: \(entry.dataType), Created: \(entry.createdAt)")
}
```

### Get Statistics

Get statistics about the data store:

**Kotlin:**

```kotlin
val stats = cedarling.getStatsCtx()
println("Entries: ${stats.entryCount}/${stats.maxEntries}")
println("Total size: ${stats.totalSizeBytes} bytes")
println("Capacity usage: ${stats.capacityUsagePercent}%")
```

**Swift:**

```swift
let stats = try cedarling.getStatsCtx()
print("Entries: \(stats.entryCount)/\(stats.maxEntries)")
print("Total size: \(stats.totalSizeBytes) bytes")
print("Capacity usage: \(stats.capacityUsagePercent)%")
```

### Error Handling

The Context Data API methods throw `DataException`:

**Kotlin:**

```kotlin
try {
    cedarling.pushDataCtx("", """{"data":"value"}""", null) // Empty key
} catch (e: DataException.DataOperationFailed) {
    println("Data operation failed: ${e.message}")
}
```

**Swift:**

```swift
do {
    let value = JsonValue(value: "{\"data\":\"value\"}")
    try cedarling.pushDataCtx(key: "", value: value, ttlSecs: nil) // Empty key
} catch {
    print("Data operation failed: \(error)")
}
```

### Using Data in Cedar Policies

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace:

**Note:** The `context.data` symbol must be declared in your Cedar schema as a compatible map/record type for the example to validate. The schema should include an entry like a record/map keyed by resource identifiers (so expressions like `context.data["user:123"].role` are allowed). See the [Cedar schema documentation](https://docs.cedar-policy.com/schema/) for schema typing rules.

```cedar
permit(
    principal,
    action == Jans::Action::"read",
    resource
) when {
    context.data has "user:123" &&
    context.data["user:123"].role.contains("admin")
};
```

The data is injected into the evaluation context before policy evaluation, allowing policies to make decisions based on dynamically pushed data.
