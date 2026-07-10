# Getting Started with Cedarling Swift

- [Installation](#installation)
- [Usage](#usage)

## Installation

### Prerequisites

- Xcode 14 or later
- iOS 14+ deployment target

### Using pre-built release assets

The recommended way to use Cedarling in an iOS app is via the pre-built XCFramework published with each Janssen release. This avoids having to install Rust or build the native library yourself.

Go to the [Janssen releases page](https://github.com/JanssenProject/jans/releases) and download the following two files for your target version:

| File                                 | Purpose                                                            |
| ------------------------------------ | ------------------------------------------------------------------ |
| `cedarling_uniffi-ios-{version}.zip` | XCFramework containing Cedarling compiled for device and simulator |
| `cedarling_uniffi.swift`             | UniFFI-generated Swift source that exposes the Rust API            |

Unzip `cedarling_uniffi-ios-{version}.zip` to get `Cedarling.xcframework`.

> For the latest stable version see the [Janssen releases page](https://github.com/JanssenProject/jans/releases).

______________________________________________________________________

#### Add to Xcode (manual)

1. Open your project in Xcode and click the **project name** at the top of the file navigator.
1. Select your **app target** under *Targets* and open the **General** tab.
1. Scroll down to **Frameworks, Libraries, and Embedded Content**.
1. Drag `Cedarling.xcframework` into the list and set the embed option to **Do Not Embed** (it is a static library — embedding will cause a codesigning error).
1. Drag `cedarling_uniffi.swift` into your source group, ensuring **Add to target** is checked.
1. Press **⌘B** to verify the build.

______________________________________________________________________

### Building from Source

Refer to the [iOS build guide](https://docs.jans.io/nightly/cedarling/developer/mobile-apps/cedarling-ios/#manually-built-ios-assets) for steps to build the XCFramework from source using Rust and Xcode.

______________________________________________________________________

## Usage

### Initialization

Initialize Cedarling with a JSON bootstrap configuration before making any authorization calls.

```
import CedarlingFFI

/*
 * In a production environment, the bootstrap configuration should not be hardcoded.
 * Instead, load it dynamically from environment variables, a remote URL,
 * or a bundled configuration file.
 */
let bootstrapJson = """
{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_LOG_LEVEL":             "INFO",
    "CEDARLING_LOG_TYPE":              "std_out",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
}
"""

guard let adapter = try? CedarlingAdapter(config: bootstrapJson) else {
    print("Unable to initialize Cedarling")
    return
}
```

Or with explicit error handling:

```
do {
    let adapter = try CedarlingAdapter(config: bootstrapJson)
} catch let error as CedarlingException {
    print("Unable to initialize Cedarling: \(error.message)")
} catch {
    print("Unable to initialize Cedarling: \(error)")
}
```

______________________________________________________________________

### Policy Store Sources

Swift bindings support all native policy store source types. See [Cedarling Properties](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/index.md) for the full list of configuration options and [Policy Store Formats](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#policy-store-formats) for format details.

**Example configurations:**

```
// Load from a directory
let bootstrapJson = """
{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store/"
}
"""

// Load from a local .cjar archive (Cedar Archive)
let bootstrapJson = """
{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
}
"""

// Load from a remote .cjar archive (Cedar Archive)
let bootstrapJson = """
{
    "CEDARLING_APPLICATION_NAME": "MyApp",
    "CEDARLING_POLICY_STORE_URI": "https://example.com/policy-store.cjar"
}
"""
```

See [Policy Store Formats](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#policy-store-formats) for more details.

______________________________________________________________________

### Authorization

Cedarling provides authorization interfaces for evaluating access requests based on a principal (entity), action, resource, and context.

- [**Token-Based Authorization**](#token-based-authorization-multi-issuer) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly without JWTs. This is useful when you need to authorize based on internal application data.

______________________________________________________________________

#### Token-Based Authorization (Multi-Issuer)

For token-based authorization, use `authorizeMultiIssuer` which processes JWT tokens and maps them to Cedar entities based on the `token_metadata` configuration in your policy store.

**1. Prepare tokens**

Tokens are provided as a `[String: String]` dictionary with token type as key and its JWT value:

```
let tokens: [String: String] = [
    "Jans::Access_token":   "<access_token_jwt>",
    "Jans::id_token":       "<id_token_jwt>",
    "Jans::Userinfo_token": "<userinfo_token_jwt>"
]
```

**2. Define the resource**

```
let resource = """
{
    "cedar_entity_mapping": {
        "entity_type": "Jans::Application",
        "id": "app_id_001"
    },
    "name": "App Name",
    "url": {
        "host": "example.com",
        "path": "/admin-dashboard",
        "protocol": "https"
    }
}
"""
```

**3. Define the action**

```
let action = #"Jans::Action::"Read""#
```

**4. Define context (optional)**

```
let context = "{}"
```

**5. Authorize**

```
do {
    let result = try adapter.authorizeMultiIssuer(
        tokens: tokens,
        action: action,
        resource: resource,
        context: context
    )

    if result.decision {
        print("Access granted")
    } else {
        print("Access denied")
    }
} catch {
    print("Authorization error: \(error)")
}
```

See [Multi-Issuer Authorization](https://docs.jans.io/nightly/cedarling/reference/cedarling-multi-issuer/index.md) for more details.

______________________________________________________________________

#### Unsigned Authorization

For unsigned authorization, use `authorizeUnsigned` (JSON principal string, nullable for no asserted principal) or `authorizeUnsignedEntity` (optional `EntityData`) directly without JWTs.

**1. Define the resource**

This represents the *resource* that the action will be performed on, such as a protected API endpoint or file.

```
let resource = """
{
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "admin_ui_id"
    },
    "name": "App Name",
    "permission": "view_clients"
}
"""
```

**2. Define the action**

An *action* represents what the principal is trying to do to the resource — for example, read, write, or delete.

```
let action = #"Jans::Action::"Update""#
```

**3. Define context**

The *context* represents additional data that may affect the authorization decision.

```
let context = "{}"
```

**4. Define the principal (optional)**

```
let principalJson = """
{
    "cedar_entity_mapping": {
        "entity_type": "Jans::Workload",
        "id": "workload_123"
    },
    "client_id": "my_client"
}
"""

let principal = EntityData.fromJson(principalJson)
```

**5. Authorize**

```
do {
    let result = try adapter.authorizeUnsignedEntity(
        principal: principal,
        action: action,
        resource: resource,
        context: context
    )

    if result.decision {
        print("Access granted")
    } else {
        print("Access denied")
    }
} catch {
    print("Authorization error: \(error)")
}
```

______________________________________________________________________

### Logging

Retrieve logs using the `popLogs` function and related helpers.

```
// Get all logs and clear the buffer
let logs: [String] = adapter.popLogs()

// Get all log IDs (does not clear the buffer)
let logIds: [String] = adapter.logIds

// Get a specific log entry by ID
if let firstId = logIds.first {
    let logEntry: String = adapter.getLogById(id: firstId)
    print(logEntry)
}

// Get logs filtered by tag (e.g., "System")
let systemLogs: [String] = adapter.getLogsByTag(tag: "System")
```

> To use the log functions, set the bootstrap properties, **CEDARLING_LOG_TYPE** to **memory** and configure **CEDARLING_LOG_TTL** with an appropriate value (in seconds). See the [Log behavior](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/#optional-properties) documentation to learn about other log-related bootstrap properties.

______________________________________________________________________

## See Also

- [Using Pre-Built iOS Release Assets](https://docs.jans.io/nightly/cedarling/developer/mobile-apps/cedarling-ios/#using-pre-built-ios-release-assets)
- [Cedarling TBAC quickstart](https://docs.jans.io/nightly/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](https://docs.jans.io/nightly/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-application-asserted-identity)
