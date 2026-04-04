---
tags:
  - cedar
  - cedarling
  - getting-started
---

# Getting Started with Cedarling

**Cedarling** is a lightweight, embeddable Policy Decision Point (PDP) that enables fast, fine-grained, and decentralized access control across modern applications. Built on the Rust-based [Cedar](https://cedarpolicy.com/) engine, Cedarling is designed for both client-side and server-side use, supporting environments like browsers, mobile apps, cloud-native services, and API gateways.

Cedarling supports both [Token-Based Access Control (TBAC)](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac) using JWTs and unsigned authorization requests. In both cases, it enforces policies locally for low-latency and consistent Zero Trust security.

You can integrate Cedarling into your application using the following language libraries:

- [C](./c.md)
- [JavaScript](./javascript.md)
- [Python](./python.md)
- [Rust](./rust.md)
- [Kotlin](./kotlin.md)
- [Swift](./swift.md)
- [Golang](./go.md)
- [Java](./java.md)

Alternatively, you can use the [Cedarling Sidecar](../developer/sidecar/cedarling-sidecar-overview.md) for a drop-in deployment.

From here, you can either jump directly to the language-specific examples above or continue reading for a high-level overview of how Cedarling works.

---

## Cedarling Interfaces

The main way you will interact with Cedarling are through the following interfaces

- [Initialization](#initialization)
- [Authorization](#authorization)
- [Context Data API](#context-data-api)
- [Logging](#logging)

### Initialization

The initialization or `init` interface is how you will initialize Cedarling. Initialization involves loading:

**Bootstrap Configuration**

- A set of properties that will tells how Cedarling behaves within your application.
- Learn more in the [bootstrap properties guide](../reference/cedarling-properties.md).

**Policy Store**

- A JSON file containing the schema, policies, trusted issuers, and token metadata schema used for making authorization decisions.
- Learn more in the [policy store guide](../reference/cedarling-policy-store.md).

The bootstrap configuration and policy store directly influence how Cedarling performs [authorization](#authorization).

=== "C"

    ```c
    #include "cedarling_c.h"
    
    // Initialize the library
    cedarling_init();
    
    // Create instance with JSON config
    const char* config = "{\"CEDARLING_APPLICATION_NAME\": \"My App\", ...}";
    CedarlingInstanceResult result;
    cedarling_new(config, &result);
    uint64_t instance_id = result.INSTANCE_ID;
    ```

=== "JavaScript"

    ```js
    import initWasm, { init } from "/pkg/cedarling_wasm.js";

    let cedarling = await init({});
    ```

=== "Python"

    ```py
    cedarling = Cedarling(BootstrapConfig.load_from_file(
      "./bootstrap_props.json"
    ))
    ```

=== "Rust"

    ```rs
    let cedarling = Cedarling::new(&BootstrapConfig::from_env().unwrap());
    ```

=== "Kotlin"

    ```kt
    // Assigning a multi-line JSON string representing the Bootstrap configuration for Cedarling
    val bootstrapConfig: String = """
        {
            "CEDARLING_APPLICATION_NAME": "My App",
        ...
        }
    """;

    val cedarling: Cedarling? = Cedarling.loadFromJson(bootstrapConfig);
    ```

=== "Swift"

    ```swift
    // Assigning a multi-line JSON string representing the Bootstrap configuration for Cedarling
    let bootstrapConfig: String = """
        {
            "CEDARLING_APPLICATION_NAME": "My App",
        ...
        }
    """

    let cedarling = try Cedarling.loadFromJson(config: bootstrapConfig)
    ```

=== "Go"

    ```go

    config := map[string]any{
        "CEDARLING_APPLICATION_NAME": "My App",
    }
    instance, err := cedarling_go.NewCedarling(config)
    if err != nil {
        panic!("Error during init: %s", err)
    }
    ```

=== "Java"

    ```java

    import uniffi.cedarling_uniffi.*;
    ...
     /*
    * In a production environment, the bootstrap configuration should not be hardcoded.
    * Instead, it should be loaded dynamically from external sources such as environment variables,
    * configuration files, or a centralized configuration service.
    */
    String bootstrapJsonStr = """
        {
            "CEDARLING_APPLICATION_NAME":   "MyApp",
            ...
        }
    """;

    try {
        CedarlingAdapter cedarlingAdapter = new CedarlingAdapter();
        cedarlingAdapter.loadFromJson(bootstrapJsonStr);
    } catch (CedarlingException e) {
        System.out.println("Unable to initialize Cedarling" + e.getMessage());
    } catch (Exception e) {
        System.out.println("Unable to initialize Cedarling" + e.getMessage());
    }
    ```

### Authorization

The authorization, or `authz`, interface is used to evaluate access control decisions by answering the question:

> Is this **Action**, on this **Resource**, in this **Context**, allowed for these **Principals**?

When using Cedarling, **Action** and **Resource** are typically defined in the [policy store](../reference/cedarling-policy-store.md), while **Principal** and **Context** are supplied at runtime via the `authz` interface.

Cedarling provides two authorization methods. Not sure which to use? See the [quick start decision guide](../quick-start/cedarling-quick-start.md#which-authorization-method-should-i-use).

**Multi-Issuer Authorization (Token-based) — Recommended**

- Uses `authorize_multi_issuer` to process JWT tokens from multiple issuers.
- Token data is mapped to Cedar entities based on `token_metadata` configuration in the policy store.
- Authorization decisions are made based on context values derived from token payloads.
- **Recommended for most production deployments** where you have JWT tokens from trusted identity providers.

[More information](../reference/cedarling-multi-issuer.md)

**Unsigned Authorization**

- Uses `authorize_unsigned` to accept **Principals** directly without requiring JWTs.
- This makes authorization decisions by passing a set of **Principals** directly.
- The **Context** is passed in as-is in a map-like structure.
- When using multiple principals, configure [`CEDARLING_PRINCIPAL_BOOLEAN_OPERATION`](../reference/cedarling-principal-boolean-operations.md) to control how per-principal decisions are combined.
- Use when your application has already authenticated the principal, for testing, or for service-to-service calls with upstream verification.

See the language-specific tutorials for detailed examples of both authorization methods:

- [JavaScript](./javascript.md)
- [Python](./python.md)
- [Rust](./rust.md)
- [Kotlin](./kotlin.md)
- [Swift](./swift.md)
- [Go](./go.md)
- [Java](./java.md)

### Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace. This enables dynamic, runtime-based authorization decisions.

=== "C"

    ```c
    // Push data with optional TTL
    const char* value = "{\"role\":[\"admin\",\"editor\"],\"country\":\"US\"}";
    CedarlingResult result;
    cedarling_context_push(instance_id, "user:123", value, &result);
    cedarling_free_result(&result);

    // Get data
    cedarling_context_get(instance_id, "user:123", &result);
    if (result.DATA) {
        printf("User data: %s\n", (char*)result.DATA);
    }
    cedarling_free_result(&result);

    // Get statistics
    cedarling_context_stats(instance_id, &result);
    printf("Stats: %s\n", (char*)result.DATA);
    cedarling_free_result(&result);
    ```

=== "JavaScript"

    ```js
    // Push data with optional TTL (5 minutes = 300 seconds)
    cedarling.push_data_ctx("user:123", {
      role: ["admin", "editor"],
      country: "US"
    }, 300);

    // Get data
    const value = cedarling.get_data_ctx("user:123");
    if (value) {
      console.log(`User roles: ${value.role}`);
    }

    // Get statistics
    const stats = cedarling.get_stats_ctx();
    console.log(`Entries: ${stats.entry_count}/${stats.max_entries}`);
    ```

=== "Python"

    ```py
    # Push data with optional TTL (5 minutes = 300 seconds)
    cedarling.push_data_ctx("user:123", {
        "role": ["admin", "editor"],
        "country": "US"
    }, ttl_secs=300)

    # Get data
    value = cedarling.get_data_ctx("user:123")
    if value is not None:
        print(f"User roles: {value['role']}")

    # Get statistics
    stats = cedarling.get_stats_ctx()
    print(f"Entries: {stats.entry_count}/{stats.max_entries}")
    ```

=== "Rust"

    ```rs
    use std::time::Duration;
    use serde_json::json;

    // Push data with optional TTL (5 minutes)
    cedarling.push_data_ctx(
        "user:123",
        json!({
            "role": ["admin", "editor"],
            "country": "US"
        }),
        Some(Duration::from_secs(300))
    )?;

    // Get data
    if let Some(value) = cedarling.get_data_ctx("user:123")? {
        println!("User roles: {:?}", value["role"]);
    }

    // Get statistics
    let stats = cedarling.get_stats_ctx()?;
    println!("Entries: {}/{}", stats.entry_count, stats.max_entries);
    ```

=== "Kotlin"

    ```kotlin
    // Push data with optional TTL (5 minutes = 300 seconds)
    // JsonValue is represented as String in Kotlin/JVM bindings
    val value = """{"role":["admin","editor"],"country":"US"}"""
    cedarling.pushDataCtx("user:123", value, 300L)

    // Get data
    val result = cedarling.getDataCtx("user:123")
    if (result != null) {
        // Parse and use the JSON string
    }

    // Get statistics
    val stats = cedarling.getStatsCtx()
    println("Entries: ${stats.entryCount}/${stats.maxEntries}")
    ```

=== "Swift"

    ```swift
    // Push data with optional TTL (5 minutes = 300 seconds)
    let value = JsonValue(value: "{\"role\":[\"admin\",\"editor\"],\"country\":\"US\"}")
    try cedarling.pushDataCtx(key: "user:123", value: value, ttlSecs: 300)

    // Get data
    if let result = try cedarling.getDataCtx(key: "user:123") {
        let jsonStr = result.inner()
        // Parse and use the JSON string
    }

    // Get statistics
    let stats = try cedarling.getStatsCtx()
    print("Entries: \(stats.entryCount)/\(stats.maxEntries)")
    ```

=== "Go"

    ```go
    package main

    import "time"

    func main() {
        // Push data with optional TTL (5 minutes)
        userData := map[string]any{
            "role":    []string{"admin", "editor"},
            "country": "US",
        }
        ttl := 5 * time.Minute
        err := instance.PushData("user:123", userData, &ttl)
        if err != nil {
            // Handle error
        }

    // Get data
    value, err := instance.GetData("user:123")
    if err != nil {
        // Handle error
    }
    if value != nil {
        // Use the value
    }

    // Get statistics
    stats, err := instance.GetStats()
    if err != nil {
        // Handle error
    }
    fmt.Printf("Entries: %d/%d\n", stats.EntryCount, stats.MaxEntries)
    ```

=== "Java"

    ```java
    import uniffi.cedarling_uniffi.*;

    // Push data with optional TTL (5 minutes = 300 seconds)
    // In Java/Kotlin bindings, JsonValue is represented as a plain String
    String value = "{\"role\":[\"admin\",\"editor\"],\"country\":\"US\"}";
    Cedarling cedarling = adapter.getCedarling();
    cedarling.pushDataCtx("user:123", value, 300L);

    // Get data
    String result = cedarling.getDataCtx("user:123");
    if (result != null) {
        // Parse and use the JSON string
        JSONObject data = new JSONObject(result);
    }

    // Get statistics
    DataStoreStats stats = cedarling.getStatsCtx();
    System.out.println("Entries: " + stats.getEntryCount() + "/" + stats.getMaxEntries());
    ```

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace:

```cedar
permit(
    principal,
    action == Action::"read",
    resource
) when {
    context.data has "user:123" &&
    context.data["user:123"].role.contains("admin")
};
```

### Logging

Cedarling supports logging of both **decision** and **system** events, useful for auditing and troubleshooting. Logging is optional and can be configured (or disabled) via the [bootstrap properties](../reference/cedarling-properties.md).

---

## What's next?

You're now ready to dive deeper into Cedarling. From here, you could either:

- See how you can use [RBAC with Cedarling using signed tokens (TBAC)](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac) — the recommended starting point.
- Explore how to use [Cedarling's Unsigned interface](../quick-start/cedarling-quick-start.md#implement-rbac-using-application-asserted-identity) for custom auth flows or testing.
- Use the [Cedarling Sidecar](../developer/sidecar/cedarling-sidecar-overview.md) for a quick, zero-code deployment.
- Learn more about [why Cedarling exists](../README.md#why-zero-trust-needs-cedarlings) and the problems it solves.
