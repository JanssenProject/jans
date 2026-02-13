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

Cedarling currently provides two modes of authorization:

**Standard (Token-based) Interface**

- Extracts the **Principal** from a JWT.
- Accepts the **Context** as a structured map.

=== "JavaScript"

    ```js
    const tokens = {
      access_token: "<access_token>",
      id_token: "<id_token>",
      userinfo_token: "<userinfo_token>",
    };

    const action = 'Jans::Action::"Read"';

    const resource = {
      cedar_entity_mapping: {
        entity_type: "Jans::Application",
        id: "app_id_001"
      },
      name: "Some Application",
      url: {
        host: "example.com",
        path: "/admin-dashboard",
        protocol: "https",
      },
    };

    const context = {
      current_time: Date.now(),
      device_health: ["Healthy"],
      fraud_indicators: ["Allowed"],
      geolocation: ["America"],
      network: "127.0.0.1",
      network_type: "Local",
      operating_system: "Linux",
    };

    const result = await cedarling.authorize({
      tokens,
      action,
      resource,
      context,
    });
    ```

=== "Python"

    ```py
    import time

    tokens = {
        "access_token": "<access_token>",
        "id_token": "<id_token>",
        "userinfo_token": "<userinfo_token>"
    }

    action = 'Jans::Action::"Read"'

    resource = {
        "cedar_entity_mapping": {
            "entity_type": "Jans::Application",
            "id": "app_id_001"
        },
        "name": "Some Application",
        "url": {
            "host": "example.com",
            "path": "/admin-dashboard",
            "protocol": "https"
        }
    }

    context = {
        "current_time": int(time.time() * 1000),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
    }

    result = cedarling.authorize(Request(
        tokens,
        action,
        resource,
        context,
    ))
    ```

=== "Rust"

    ```rs
    use std::collections::HashMap;
    use chrono::Utc;
    use serde_json::json;

    let tokens = HashMap::from([
        ("access_token".to_string(), "<access_token>".to_string()),
        ("id_token".to_string(), "<id_token>".to_string()),
        ("userinfo_token".to_string(), "<userinfo_token>".to_string()),
    ]);

    let action = "Jans::Action::\"Read\"".to_string();

    let resource = json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Application",
            "id": "app_id_001"
        },
        "name": "Some Application",
        "url": {
            "host": "example.com",
            "path": "/admin-dashboard",
            "protocol": "https"
        }
    });

    let context = json!({
        "current_time": Utc::now().timestamp_millis(),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
    });

    let result = cedarling.authorize(Request {
        tokens,
        action,
        resource,
        context,
    });
    ```

=== "Kotlin"

    ```kotlin
    val tokens = mapOf(
        "access_token" to "<access_token>",
        "id_token" to "<id_token>",
        "userinfo_token" to "<userinfo_token>"
    )

    val action = """Jans::Action::"Read""""

    val resource = mapOf(
        "cedar_entity_mapping" to mapOf(
            "entity_type" to "Jans::Application",
            "id" to "app_id_001"
        ),
        "name" to "Some Application",
        "url" to mapOf(
            "host" to "example.com",
            "path" to "/admin-dashboard",
            "protocol" to "https"
        )
    )

    val context = mapOf(
        "current_time" to System.currentTimeMillis(),
        "device_health" to listOf("Healthy"),
        "fraud_indicators" to listOf("Allowed"),
        "geolocation" to listOf("America"),
        "network" to "127.0.0.1",
        "network_type" to "Local",
        "operating_system" to "Linux"
    )

    val result = cedarling?.authorize(
        tokens,
        action,
        resource["cedar_entity_mapping"]?.get("entity_type") as String,
        resource["cedar_entity_mapping"]?.get("id") as String,
        anyToJson(resource),
        context
    )
    ```

=== "Swift"

    ```swift
    let tokens: [String: String] = [
        "access_token": "<access_token>",
        "id_token": "<id_token>",
        "userinfo_token": "<userinfo_token>"
    ]

    let action = #"Jans::Action::"Read""#

    let resource: [String: Any] = [
        "cedar_entity_mapping": [
            "entity_type": "Jans::Application",
            "id": "app_id_001"
        ],
        "name": "Some Application",
        "url": [
            "host": "example.com",
            "path": "/admin-dashboard",
            "protocol": "https"
        ]
    ]

    let context: [String: Any] = [
        "current_time": Int(Date().timeIntervalSince1970 * 1000),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux"
    ]

    let payloadJsonString = try JSONSerialization.data(
        withJSONObject: resource,
        options: []
    ).toUtf8String()

    let result: AuthorizeResult = try cedarling.authorize(
        tokens: tokens,
        action: action,
        resourceType: resource["cedar_entity_mapping"]?.get("entity_type") as! String,
        resourceId: resource["cedar_entity_mapping"]?.get("id") as! String,
        payload: payloadJsonString,
        context: context
    )
    ```

=== "Go"

    ```go

    tokens := map[string]string {
        "access_token": "<access_token>",
        "id_token": "<id_token>",
        "userinfo_token": "<userinfo_token>"
    }
    action := "Jans::Action::\"Read\""
    resource := cedarling_go.EntityData{
        CedarMapping: cedarling_go.CedarMapping{
            EntityType: "Jans::Application",
            ID: "app_id_001",
        },
        Payload: map[string]any{
            "name": "Some Application",
            "url": map[string]string{
                "host": "example.com",
                "path": "/admin-dashboard",
                "protocol": "https",
            },
        },
    }
    context := map[string]any{
        "current_time": time.Now().Unix(),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
    }
    request := cedarling_go.Request{
        Tokens: tokens,
        Action: action,
        Resource: resource,
        Context: context,
    }
    result, err := cedarling_instance.Authorize(request)
    if err != nil {
        panic!(err)
    }
    ```

=== "Java"

    ```java

    String resource = """
        {
          "cedar_entity_mapping": {
            "entity_type": "Jans::Application",
            "id": "app_id_001"
          },
          "name": "Some Application",
          "url": {
            "host": "example.com",
            "path": "/admin-dashboard",
            "protocol": "https"
          }
        }
    """;

    String action = "Jans::Action::\"Update\"";

    String context = """
        {
          "device_health": ["Healthy"],
          "fraud_indicators": ["Allowed"],
          "geolocation": ["America"],
          "network": "127.0.0.1",
          "network_type": "Local",
          "operating_system": "Linux",
          "user_agent": "Linux"
        }
    """;

    Map<String, String> tokens = Map.of(
        "access_token", "<access_token>",
        "id_token", "<id_token>",
        "userinfo_token", "<userinfo_token>"
    );

    // Perform authorization
    AuthorizeResult result = adapter.authorize(tokens, action, new JSONObject(resource), new JSONObject(context));
    if(result.getDecision()) {
        System.out.println("Access granted");
    } else {
        System.out.println("Access denied");
    }

    ```

**Unsigned Authorization**

- Accepts the **Principal** directly without requiring a JWT.
- This makes authorization decisions by passing a set of **Principals** directly.
- Similar to the standard interface, the **Context** is passed in as-is in a map-like structure.

**Multi-Issuer Authorization**

- Doesn't use principal and all token information is stored in context.
- This makes authorization decisions by rules based on context values (token payloads).

[More information](../reference/cedarling-multi-issuer.md)

### Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace. This enables dynamic, runtime-based authorization decisions.

=== "JavaScript"

    ```js
    // Push data with optional TTL (5 minutes = 300 seconds)
    await cedarling.push_data_ctx("user:123", {
      role: ["admin", "editor"],
      country: "US"
    }, 300);

    // Get data
    const value = await cedarling.get_data_ctx("user:123");
    if (value) {
      console.log(`User roles: ${value.role}`);
    }

    // Get statistics
    const stats = await cedarling.get_stats_ctx();
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
    if value:
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
    import "time"

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
    context.data["user:123"].role.contains("admin")
};
```

### Logging

Cedarling supports logging of both **decision** and **system** events, useful for auditing and troubleshooting. Logging is optional and can be configured (or disabled) via the [bootstrap properties](../reference/cedarling-properties.md).

---

## What's next?

You're now ready to dive deeper into Cedarling. From here, you could either:

- See how you can use [RBAC with Cedarling using signed tokens](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac).
- Explore how to use [Cedarling's Unsigned interface](../quick-start/cedarling-quick-start.md#implement-rbac-using-cedarling).
- Use the [Cedarling Sidecar](../developer/sidecar/cedarling-sidecar-overview.md) for a quick, zero-code deployment.
- Learn more about [why Cedarling exists](../README.md#why-zero-trust-needs-cedarlings) and the problems it solves.
 RBAC using signed tokens (TBAC)
