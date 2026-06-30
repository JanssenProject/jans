---
tags:
  - cedarling
  - kotlin
  - getting-started
---

# Getting Started with Cedarling Kotlin

- [Installation](#installation)
- [Usage](#usage)

## Installation

### Prerequisites

- Java Development Kit (JDK): version 11 or higher
- A Kotlin/Gradle project (Gradle Kotlin DSL recommended)
- A GitHub Personal Access Token (PAT) with `read:packages` scope

### Add the repository and dependency

GitHub Packages requires authentication even for public packages. Store your credentials in `~/.gradle/gradle.properties` (outside the project — never commit this file):

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_PAT
```

Then in your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "jans"
        url = uri("https://maven.pkg.github.com/JanssenProject/jans")
        credentials {
            username = project.findProperty("gpr.user") as String?
                ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.token") as String?
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.jans:cedarling-java:2.2.0")
}
```

> Replace `2.2.0` with the [latest stable Jans release](https://github.com/JanssenProject/jans/releases).

After editing `build.gradle.kts`, click **Load Gradle Changes** in IntelliJ (or press the 🐘 elephant icon) to sync.

### Building from Source

Refer to the following [guide](../developer/cedarling-kotlin.md#building-from-source) for steps to build the Kotlin binding from source.

---

## Usage

### Initialization

Initialize Cedarling with a JSON bootstrap configuration before making any authorization calls.

```kotlin
import uniffi.cedarling_uniffi.*
import io.jans.cedarling.binding.wrapper.CedarlingAdapter

/*
 * In a production environment, the bootstrap configuration should not be hardcoded.
 * Instead, load it dynamically from environment variables, configuration files,
 * or a centralized configuration service.
 */
val bootstrapJson = """
    {
        "CEDARLING_APPLICATION_NAME":      "MyApp",
        "CEDARLING_LOG_LEVEL":             "INFO",
        "CEDARLING_LOG_TYPE":              "std_out",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
    }
""".trimIndent()

val adapter = try {
    CedarlingAdapter().also { it.loadFromJson(bootstrapJson) }
} catch (e: CedarlingException) {
    println("Unable to initialize Cedarling: ${e.message}")
    return
} catch (e: Exception) {
    println("Unable to initialize Cedarling: ${e.message}")
    return
}
```

---

### Policy Store Sources

Kotlin bindings support all native policy store source types. See [Cedarling Properties](../reference/cedarling-properties.md) for the full list of configuration options and [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for format details.

**Example configurations:**

```kotlin
// Load from a directory
val bootstrapJson = """
    {
        "CEDARLING_APPLICATION_NAME":      "MyApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store/"
    }
""".trimIndent()

// Load from a local .cjar archive (Cedar Archive)
val bootstrapJson = """
    {
        "CEDARLING_APPLICATION_NAME":      "MyApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
    }
""".trimIndent()

// Load from a remote .cjar archive (Cedar Archive)
val bootstrapJson = """
    {
        "CEDARLING_APPLICATION_NAME": "MyApp",
        "CEDARLING_POLICY_STORE_URI": "https://example.com/policy-store.cjar"
    }
""".trimIndent()
```

See [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for more details.

---

### Authorization

Cedarling provides authorization interfaces for evaluating access requests based on a principal (entity), action, resource, and context.

- [**Token-Based Authorization**](#token-based-authorization-multi-issuer) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly without JWTs. This is useful when you need to authorize based on internal application data.

---

#### Token-Based Authorization (Multi-Issuer)

For token-based authorization, use `authorizeMultiIssuer` which processes JWT tokens and maps them to Cedar entities based on the `token_metadata` configuration in your policy store.

**1. Prepare tokens**

Tokens are provided as a `Map<String, String>` with the token type as key and its JWT value:

```kotlin
val tokens = mapOf(
    "Jans::Access_token"   to "<access_token_jwt>",
    "Jans::id_token"       to "<id_token_jwt>",
    "Jans::Userinfo_token" to "<userinfo_token_jwt>"
)
```

**2. Define the resource**

```kotlin
import org.json.JSONObject

val resource = JSONObject("""
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
""".trimIndent())
```

**3. Define the action**

```kotlin
val action = """Jans::Action::"Read""""
```

**4. Define context (optional)**

```kotlin
val context = JSONObject("{}")
```

**5. Authorize**

```kotlin
val result = adapter.authorizeMultiIssuer(tokens, action, resource, context)

if (result.decision) {
    println("Access granted")
} else {
    println("Access denied")
}
```

See [Multi-Issuer Authorization](../reference/cedarling-multi-issuer.md) for more details.

---

#### Unsigned Authorization

For unsigned authorization, use `authorizeUnsigned` (JSON principal string, nullable for no asserted principal) or `authorizeUnsignedEntity` (optional `EntityData`) directly without JWTs.

**1. Define the resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```kotlin
val resource = JSONObject().apply {
    put("cedar_entity_mapping", JSONObject()
        .put("entity_type", "Jans::Issue")
        .put("id", "admin_ui_id"))
    put("name", "App Name")
    put("permission", "view_clients")
}
```

**2. Define the action**

An _action_ represents what the principal is trying to do to the resource — for example, read, write, or delete.

```kotlin
val action = """Jans::Action::"Update""""
```

**3. Define context**

The _context_ represents additional data that may affect the authorization decision.

```kotlin
val context = JSONObject()
```

**4. Define the principal (optional)**

```kotlin
val principal = EntityData.Companion.fromJson(
    JSONObject()
        .put("cedar_entity_mapping", JSONObject()
            .put("entity_type", "Jans::Workload")
            .put("id", "workload_123"))
        .put("client_id", "my_client")
        .toString()
)
// Pass null to authorizeUnsignedEntity / null principal JSON to authorizeUnsigned
// for partial evaluation without an asserted principal.
```

**5. Authorize**

```kotlin
val result = adapter.authorizeUnsignedEntity(principal, action, resource, context)

if (result.decision) {
    println("Access granted")
} else {
    println("Access denied")
}
```

---

### Logging

Retrieve logs using the `popLogs` function and related helpers.

```kotlin
// Get all logs and clear the buffer
val logs: List<String> = adapter.popLogs()

// Get all log IDs
val logIds: List<String> = adapter.logIds

// Get a specific log entry by ID
val logEntry: String = adapter.getLogById(logIds.first())

// Get logs by tag (e.g., "System")
val systemLogs = adapter.getLogsByTag("System")
```

---

## Defined API

Defined APIs are listed [here](https://janssenproject.github.io/developer-docs/jans-cedarling/bindings/cedarling-java/io/jans/cedarling/binding/wrapper/CedarlingAdapter.html).

## See Also

- [Cedarling TBAC quickstart](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](../quick-start/cedarling-quick-start.md#implement-rbac-using-application-asserted-identity)
- [Getting Started with Cedarling Java](./java.md)