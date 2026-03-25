---
tags:
  - cedarling
  - java
  - getting-started
---

# Getting Started with Cedarling Java

## Installation

### Using the package manager

**Prerequisites**

- Java Development Kit (JDK): version 11 or higher

To use Cedarling Java bindings in Java Maven Project add following
`repository` and `dependency` in pom.xml of the project.

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

### Building from Source

Refer to the following [guide](../developer/cedarling-kotlin.md#building-from-source) for steps to build the Java binding from source.

## Usage

### Initialization

We need to initialize Cedarling first.

```java

        import uniffi.cedarling_uniffi.*;
        import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
        ...

        /*
         * In a production environment, the bootstrap configuration should not be hardcoded.
         * Instead, it should be loaded dynamically from external sources such as environment variables,
         * configuration files, or a centralized configuration service.
         */
        String bootstrapJsonStr = """
            {
            "CEDARLING_APPLICATION_NAME":   "MyApp",
            "CEDARLING_LOG_LEVEL":          "INFO",
            "CEDARLING_LOG_TYPE":           "std_out",
            "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json"
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

### Policy Store Sources

Java bindings support all native policy store source types. See [Cedarling Properties](../reference/cedarling-properties.md) for the full list of configuration options and [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for format details.

**Example configurations:**

```java
// Load from a directory
String bootstrapJsonStr = """
    {
        "CEDARLING_APPLICATION_NAME": "MyApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store/"
    }
    """;

// Load from a local .cjar archive (Cedar Archive)
String bootstrapJsonStr = """
    {
        "CEDARLING_APPLICATION_NAME": "MyApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
    }
    """;

// Load from a remote .cjar archive (Cedar Archive)
String bootstrapJsonStr = """
    {
        "CEDARLING_APPLICATION_NAME": "MyApp",
        "CEDARLING_POLICY_STORE_URI": "https://example.com/policy-store.cjar"
    }
    """;
```

See [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for more details.

### Authorization

Cedarling provides authorization interfaces for evaluating access requests based on principals (entities), actions, resources, and context.

- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly without JWTs. This is useful when you need to authorize based on internal application data.
- [**Custom Principal Authorization**](#custom-principal-authorization-unsigned) is an alternative approach for defining custom principals.

#### Unsigned Authorization

For unsigned authorization, use `authorizeUnsigned` which accepts principals directly without JWTs.

**1. Define the resource:**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```java
    JSONObject resource = new JSONObject();
    resource.put("cedar_entity_mapping", new JSONObject()
        .put("entity_type", "Jans::Issue")
        .put("id", "admin_ui_id"));
    resource.put("name", "App Name");
    resource.put("permission", "view_clients");
```

**2. Define the action:**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```java
String action = "Jans::Action::\"Update\"";
```

**3. Define Context**

The _context_ represents additional data that may affect the authorization decision.

```java
    JSONObject context = new JSONObject();
```

**4. Define Principals**

```java
    List<EntityData> principals = List.of(
        EntityData.Companion.fromJson(new JSONObject()
            .put("cedar_entity_mapping", new JSONObject()
                .put("entity_type", "Jans::Workload")
                .put("id", "workload_123"))
            .put("client_id", "my_client")
            .toString())
    );
```

**5. Authorize**

```java
    AuthorizeResult result = adapter.authorizeUnsigned(principals, action, resource, context);
    if(result.getDecision()) {
        System.out.println("Access granted");
    } else {
        System.out.println("Access denied");
    }
```

#### Custom Principal Authorization (Unsigned)

**1. Define principals:**

```java
    String principals = """
        const principals = [
          {
            "cedar_entity_mapping": {
              "entity_type": "Jans::Workload",
              "id": "some_workload_id"
            },
            "client_id": "some_client_id",
          },
          {
            "cedar_entity_mapping": {
              "entity_type": "Jans::User",
              "id": "random_user_id"
            },
            "role": ["admin", "manager"]
          },
        ];
        """;
```

Similarly, create and initialize String variables with action, resource, context.

**2. Authorize**

Finally, call the `authorizeUnsigned` function to check whether the principals are allowed to perform the specified action on the resource.

```java
        List<EntityData> principals = List.of(EntityData.Companion.fromJson(principals));

        AuthorizeResult result = adapter.authorizeUnsigned(principals, action, new JSONObject(resource), new JSONObject(context));
        if(result.getDecision()) {
            System.out.println("Access granted");
        } else {
            System.out.println("Access denied");
        }
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```java
    // Get all logs and clear the buffer
    List<String> logEntrys = adapter.popLogs();
    // Get a specific log by ID
    List<String> logEntrys = adapter.getLogIds();
    String logEntry = adapter.getLogById(logEntrys.get(0));
    // Get logs by tag (e.g., "System")
    adapter.getLogsByTag("System");
```

## Defined API

Defined APIs are listed [here](https://janssenproject.github.io/developer-docs/jans-cedarling/bindings/cedarling-java/io/jans/cedarling/binding/wrapper/CedarlingAdapter.html)

## See Also

- [Cedarling TBAC quickstart](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](../quick-start/cedarling-quick-start.md#implement-rbac-using-application-asserted-identity)
