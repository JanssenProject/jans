# Cedarling UniFFI binding

This module is designed to build Cedarling UniFFI bindings for iOS and Android apps. The Kotlin bindings can also be used from Java projects to run Cedarling authorization.

Read [this article](./docs/DetailBuildSteps.md) for detail steps.

## iOS

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Xcode: Available on the Mac App Store.

### Building

1. Ask toolchain manager to install support for compiling Rust code for iOS devices and iOS Simulator

```bash
rustup target add aarch64-apple-darwin aarch64-apple-ios-sim aarch64-apple-ios
```

2. Run below command to build and import binding into iOS project.

```bash
make ios
```
3. Open `./bindings/cedarling_uniffi/iOSApp` in Xcode. Import both the XCFramework from `./bindings/ios/Mobile.xcframework` and the Swift file bindings `./bindings/build/cedarling_uniffi.swift` files into your project (drag and drop should work).

4. Run iOS project (./bindings/cedarling_uniffi/iOSApp) on simulator.

## Android

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Android Studio: Download from [the official site](https://developer.android.com/studio).

### Building

1. Run below command to build and import binding into Android project.

```bash
make android
```

2. Open the `./bindings/cedarling_uniffi/androidApp` project on Android Studio and run the project on simulator.

## Kotlin Binding

Here we delve into the process of generating the Kotlin binding for cedarling and use it in a sample Java Maven project to run the authorization.

### Prerequisites

- Rust: Install it from [the official Rust website](https://www.rust-lang.org/tools/install).
- Java Development Kit (JDK): version 11
- Apache Maven: Install it from [Apache Maven Website](https://maven.apache.org/download.cgi)

### Building and Testing

1. Build Cedarling:

```bash
make java
```

2. Change directory to sample Java project (`./bindings/cedarling_uniffi/javaApp`) and run below command to run the main method of a Maven project from the terminal.

```bash
 mvn clean install
 mvn exec:java -Dexec.mainClass="org.example.Main"
```

The method will execute the steps for Cedarling initialization with a sample bootstrap configuration, run authorization using `authorizeUnsigned` with sample principals, resource and context inputs, and call the log interface to print authorization logs on the console. The sample `principals`, `resource` and `context` input files used by the sample application are present at `./bindings/cedarling_uniffi/javaApp/src/main/resources/config`.

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

### Initializing Cedarling (UniFFI)

Regenerate Kotlin/Swift bindings from the library (`uniffi-bindgen generate …`) so these constructors match your checkout. Exact method names and argument labels are in the generated `cedarling_uniffi.kt` / `cedarling_uniffi.swift`; the table below is a rough map from the Rust API:

| Rust constructor | Kotlin (typical) | Swift (typical) |
| --- | --- | --- |
| `load_from_json` | `Cedarling.loadFromJson(...)` | `Cedarling.loadFromJson(config:)` |
| `load_from_file` | `Cedarling.loadFromFile(...)` | `Cedarling.loadFromFile(path:)` |
| `load_from_json_with_archive_bytes` | `Cedarling.loadFromJsonWithArchiveBytes(...)` | `Cedarling.loadFromJsonWithArchiveBytes(config:archiveBytes:)` |

- **`load_from_json`** — Policy store location comes from the JSON (`CEDARLING_POLICY_STORE_LOCAL_FN`, `CEDARLING_POLICY_STORE_URI`, or `CEDARLING_POLICY_STORE_LOCAL`), same as core Cedarling bootstrap rules.
- **`load_from_file`** — Load bootstrap from a path, then resolve the policy store from fields in that file.
- **`load_from_json_with_archive_bytes`** — Pass the bootstrap JSON as a string **and** the raw bytes of a `.cjar` archive. Fields `CEDARLING_POLICY_STORE_LOCAL`, `CEDARLING_POLICY_STORE_URI`, and `CEDARLING_POLICY_STORE_LOCAL_FN` in the JSON are **ignored**; the archive is the only policy source. This mirrors the WASM helper `init_from_archive_bytes` and fits **Android `assets/`**, where you open files with `AssetManager` (no ordinary filesystem path for native code), or any host that already has the archive in memory.

**Kotlin (Android assets):**

```kotlin
val bootstrapJson =
    assets.open("bootstrap.json").bufferedReader().use { it.readText() }
val archiveBytes = assets.open("policy-store.cjar").readBytes()
val cedarling = Cedarling.loadFromJsonWithArchiveBytes(bootstrapJson, archiveBytes)
```

**Swift (bundle resources):**

```swift
let bootstrapUrl = Bundle.main.url(forResource: "bootstrap", withExtension: "json")!
let cjarUrl = Bundle.main.url(forResource: "policy-store", withExtension: "cjar")!
let bootstrapJson = try String(contentsOf: bootstrapUrl)
let archiveBytes = try Data(contentsOf: cjarUrl)
let cedarling = try Cedarling.loadFromJsonWithArchiveBytes(
    config: bootstrapJson,
    archiveBytes: archiveBytes
)
```

### Authorization APIs

The UniFFI binding exposes two authorization methods:

- **`authorizeUnsigned`**: Pass a list of principal entity data, action, resource, and context. Use when you have principal attributes (e.g. from your app or session) and no JWTs.
- **`authorizeMultiIssuer`**: Pass a list of token inputs (mapping + JWT payload), action, resource, and optional context. Use when you have multiple JWTs from different issuers.

`AuthorizeResult` contains `principals` (per-principal decisions), `decision`, and `requestId` (for log correlation). The legacy token-based `authorize` API has been removed.

### Testing Configuration

For testing scenarios, you may want to disable JWT validation. You can configure this in your bootstrap configuration:

```json
{
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_JWT_STATUS_VALIDATION": "disabled"
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
