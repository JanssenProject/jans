# Getting Started with Cedarling Java

- [Installation](#installation)
- [Usage](#usage)

## Installation

### Using the package manager

**Prerequisites**

- Java Development Kit (JDK): version 11 or higher
- A GitHub Personal Access Token (PAT) with `read:packages` scope

GitHub Packages requires authentication even for public packages. Add your credentials to ~/.m2/settings.xml (outside the project — never commit this file). The must match the repository used in pom.xml:

```
<settings>
    <servers>
        <server>
            <id>jans</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_PAT</password>
        </server>
    </servers>
</settings>
```

Generate a PAT at GitHub → Settings → Developer settings → Personal access tokens and enable the read:packages scope. Then add the following repository and dependency to your project's pom.xml:

```
<repositories>
    <repository>
        <id>jans</id>
        <name>Janssen project repository</name>
        <url>https://maven.pkg.github.com/JanssenProject/jans</url>
    </repository>
</repositories>
```

```
<dependency>
    <groupId>io.jans</groupId>
    <artifactId>cedarling-java</artifactId>
    <version>2.2.0</version>
</dependency>
```

> Replace `2.2.0` with the [latest stable Jans release](https://github.com/JanssenProject/jans/releases).

### Building from Source

Refer to the following [guide](https://docs.jans.io/nightly/cedarling/developer/cedarling-kotlin/#building-from-source) for steps to build the Java binding from source.

Note

The Cedarling dependency available in the GitHub Maven Registry works only in a Linux environment.

Refer to the following guide for instructions on building the Java bindings to work on macOS or Windows.

## Usage

### Initialization

We need to initialize Cedarling first.

```
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
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar"
}
""";

try {
    CedarlingAdapter adapter = new CedarlingAdapter();
    adapter.loadFromJson(bootstrapJsonStr);
} catch (CedarlingException e) {
    System.out.println("Unable to initialize Cedarling" + e.getMessage());
} catch (Exception e) {
    System.out.println("Unable to initialize Cedarling" + e.getMessage());
}
```

### Policy Store Sources

Java bindings support all native policy store source types. See [Cedarling Properties](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/index.md) for the full list of configuration options and [Policy Store Formats](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#policy-store-formats) for format details.

**Example configurations:**

```
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

See [Policy Store Formats](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#policy-store-formats) for more details.

### Authorization

Cedarling provides authorization interfaces for evaluating access requests based on a principal (entity), action, resource, and context.

- [**Token-Based Authorization**](#token-based-authorization-multi-issuer) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly without JWTs. This is useful when you need to authorize based on internal application data.

#### Token-Based Authorization (Multi-Issuer)

For token-based authorization, use `authorizeMultiIssuer` which processes JWT tokens and maps them to Cedar entities based on the `token_metadata` configuration in your policy store.

**1. Prepare tokens**

Tokens are provided as a Map with token type as key and its JWT format as value:

```
Map<String, String> tokens = new HashMap<>();
tokens.put("Jans::Access_token", "<access_token_jwt>");
tokens.put("Jans::id_token", "<id_token_jwt>");
tokens.put("Jans::Userinfo_token", "<userinfo_token_jwt>");
```

**2. Define the resource**

```
String resourceString = """
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
    """;
JSONObject resource = new JSONObject(resourceString);
```

**3. Define the action**

```
String action = "Jans::Action::\"Read\"";
```

**4. Define Context (optional)**

```
String contextString = "{}";
JSONObject context = new JSONObject(contextString);
```

**5. Authorize**

```
MultiIssuerAuthorizeResult result = adapter.authorizeMultiIssuer(tokens, action, resource, context);

if(result.getDecision()) {
    System.out.println("Access granted");
} else {
        System.out.println("Access denied");
}
```

See [Multi-Issuer Authorization](https://docs.jans.io/nightly/cedarling/reference/cedarling-multi-issuer/index.md) for more details.

#### Unsigned Authorization

For unsigned authorization, use `authorizeUnsigned` (JSON principal string, nullable for no asserted principal) or `authorizeUnsignedEntity` (optional `EntityData`) directly without JWTs.

**1. Define the resource:**

This represents the *resource* that the action will be performed on, such as a protected API endpoint or file.

```
JSONObject resource = new JSONObject();
resource.put("cedar_entity_mapping", new JSONObject()
    .put("entity_type", "Jans::Issue")
    .put("id", "admin_ui_id"));
resource.put("name", "App Name");
resource.put("permission", "view_clients");
```

**2. Define the action:**

An *action* represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```
String action = "Jans::Action::\"Update\"";
```

**3. Define Context**

The *context* represents additional data that may affect the authorization decision.

```
JSONObject context = new JSONObject();
```

**4. Define the principal (optional)**

```
EntityData principal =
    EntityData.Companion.fromJson(new JSONObject()
        .put("cedar_entity_mapping", new JSONObject()
            .put("entity_type", "Jans::Workload")
            .put("id", "workload_123"))
        .put("client_id", "my_client")
        .toString());
// Or pass null to `authorizeUnsignedEntity` / null principal JSON to `authorizeUnsigned`
// when using partial evaluation without an asserted principal.
```

**5. Authorize**

```
AuthorizeResult result = adapter.authorizeUnsignedEntity(principal, action, resource, context);
if(result.getDecision()) {
    System.out.println("Access granted");
} else {
    System.out.println("Access denied");
}
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```
// Get all logs and clear the buffer
List<String> logEntrys = adapter.popLogs();
// Get a specific log by ID
List<String> logEntrys = adapter.getLogIds();
String logEntry = adapter.getLogById(logEntrys.get(0));
// Get logs by tag (e.g., "System")
adapter.getLogsByTag("System");
```

## Defined API

Defined APIs are listed in the [Cedarling Java API documentation](https://janssenproject.github.io/developer-docs/jans-cedarling/bindings/cedarling-java/io/jans/cedarling/binding/wrapper/CedarlingAdapter.html).

## See Also

- [Cedarling TBAC quickstart](https://docs.jans.io/nightly/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](https://docs.jans.io/nightly/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-application-asserted-identity)
