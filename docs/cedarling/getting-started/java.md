---
tags:
  - cedarling
  - java
  - getting-started
---

# Getting Started with Cedarling Java

- [Installation](#installation)
- [Usage](#usage)

## Installation 

### Building from Source

Refer to the following [guide](../uniffi/cedarling-kotlin.md#building-from-source) for steps to build the Java binding from source.

### Using Cedarling-java Maven dependency

#### Prerequisites

- Java Development Kit (JDK): version 11 or higher

To use Cedarling Java bindings in Java Maven Project add following `repository` and `dependency` in pom.xml of the project

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

## Usage

### Initialization

We need to initialize Cedarling first.

```java

        import uniffi.cedarling_uniffi.*;
        ...
        
        //In production, bootstrap config should load dynamically.
        String bootstrapJsonStr = """
            {
            "CEDARLING_APPLICATION_NAME":   "MyApp",
            "CEDARLING_POLICY_STORE_ID":    "your-policy-store-id",
            "CEDARLING_USER_AUTHZ":         "enabled",
            "CEDARLING_WORKLOAD_AUTHZ":     "enabled",
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

### Token-Based Authorization

**1. Define the resource:**

This represents the *resource* that the action will be performed on, such as a protected API endpoint or file.

```java
    String resource = """
        {
          "app_id": "app_id_001",
          "id": "admin_ui_id",
          "name": "App Name",
          "permission": "view_clients",
          "type": "Jans::Issue"
        }
        """;
```
**2. Define the action:**

An *action* represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```java
String action = "Jans::Action::\"Update\"";
```

**3. Define Context**

The *context* represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```java
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
```

**4. Prepare tokens**

```java
    String accessToken = "<access_token>";
    String idToken = "<id_token>";
    String userinfoToken = "<userinfo_token>";
```

**5. Authorize**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

```java
    //Generate Map containing tokens
    Map<String, String> tokens = Map.of(
        "access_token", accessToken,
        "id_token", idToken,
        "userinfo_token", userinfoToken
    );

    // Perform authorization
    AuthorizeResult result = adapter.authorize(tokens, action, new JSONObject(resource), new JSONObject(context));
    if(result.getDecision()) {
        System.out.println("Access granted");
    } else {
        System.out.println("Access denied");
    }
```

### Custom Principal Authorization (Unsigned)

**1. Define principals:**

```java
    String principals = """
        const principals = [
          {
            "entity_type": "Jans::Workload",
            "id": "some_workload_id",
            "client_id": "some_client_id",
          },
          {
            "entity_type": "Jans::User",
            "id": "random_user_id",
            "roles": ["admin", "manager"]
          },
        ];
        """;
```

Similarly, create and initialize String variables with action, resource, context as done in [Token-Based Authorization](#token-based-authorization).

**2. Authorize**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

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


## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)

