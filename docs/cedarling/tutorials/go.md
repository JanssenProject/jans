---
tags:
  - cedarling
  - golang
  - getting-started
---

# Getting Started with Cedarling Go Bindings

Go bindings for the Jans Cedarling authorization engine, providing policy-based access control.

## Installation

### Build with dynamic linking

1. Download the appropriate pre-built binary for your platform from the Jans releases page or build it from source as
   described above.

2. Specify linker flags in your main.go file to link against the Cedarling library.

    ```go
    // #cgo LDFLAGS: -L. -lcedarling_go
    import "C"
    ```

    And make sure that the Cedarling library files are located in the same directory as your main package.

3. Use `go get` to fetch the Cedarling Go package

    ```sh
    go get github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go
    ```

4. Build your Go application

    ```sh
    go build .
    ```

5. Run the application

    - **Windows**

        - Place the Rust artifacts (`cedarling_go.dll` and `cedarling_go.lib`) alongside the Go binary.
        - Windows searches libraries in directories below in the
          following order
            1. The directory containing your Go executable (recommended location)
            2. Windows system directories (e.g., `C:\Windows\System32`)
            3. The `PATH` environment variable directories

    - **Linux**

        Add the library directory that contains `libcedarling_go.so` to the
        `LD_LIBRARY_PATH` environment variable

        ```sh
        export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH
        ```

    - **MacOS**

        Add the library directory that contains `libcedarling_go.dylib` to the
        `LD_LIBRARY_PATH` environment variable

        ```sh
        export DYLD_LIBRARY_PATH=$(pwd):$DYLD_LIBRARY_PATH
        ```

### Build from Source

Follow these instructions to build from source.

#### Prerequisites

- Go 1.20+
- Rust tool-chain

#### Steps to build from source

1. Build the Rust library

    Clone the Janssen repository:

    ```sh
    git clone --depth 1 https://github.com/JanssenProject/jans.git
    ```

    We use `--depth 1` to avoid cloning unnecessary history and minimalize the download size.

    Navigate to the Cedarling Go bindings directory:

    ```sh
    cd jans/jans-cedarling/bindings/cedarling_go
    ```

    ```sh
    cargo build --release -p cedarling_go
    ```

2. Copy the built artifacts to your application directory

    ```sh
    # Windows
    cp target/release/cedarling_go.dll .
    cp target/release/cedarling_go.dll.lib cedarling_go.lib

    # Linux
    cp target/release/libcedarling_go.so .

    # macOS
    cp target/release/libcedarling_go.dylib .
    ```

    or use scripts provided in the repository to automate this process:

    ```sh
    sh build_and_copy_artifacts.sh
    ```

    Run go test to ensure everything is working correctly:

    ```sh
    go test .
    ```

## Usage

### Initialization

```go
import "github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"

// Example configuration (populate dynamically in production)
config := map[string]any{
    "CEDARLING_APPLICATION_NAME":   "MyApp",
    "CEDARLING_POLICY_STORE_ID":    "your-policy-store-id",
    "CEDARLING_USER_AUTHZ":         "enabled",
    "CEDARLING_WORKLOAD_AUTHZ":     "enabled",
    "CEDARLING_LOG_LEVEL":          "INFO",
    "CEDARLING_LOG_TYPE":           "std_out",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json",
}

instance, err := cedarling_go.NewCedarling(config)
if err != nil {
    panic(err)
}
```

### Policy Store Sources

Go bindings support all native policy store source types. See [Cedarling Properties](../reference/cedarling-properties.md) for the full list of configuration options.

**Example configurations:**

```go
// Load from a directory
config := map[string]any{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store/",
    // ... other config
}

// Load from a local .cjar archive (Cedar Archive)
config := map[string]any{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.cjar",
    // ... other config
}

// Load from a remote .cjar archive (Cedar Archive)
config := map[string]any{
    "CEDARLING_APPLICATION_NAME":   "MyApp",
    "CEDARLING_POLICY_STORE_URI":   "https://example.com/policy-store.cjar",
    // ... other config
}
```

See [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for more details.

### Authorization

Cedarling provides two main interfaces for performing authorization checks: **Token-Based Authorization** and **Unsigned Authorization**. Both methods involve evaluating access requests based on various factors, including principals (entities), actions, resources, and context. The difference lies in how the Principals are provided.

- [**Token-Based Authorization**](#token-based-authorization) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data, or when tokens are not available.

#### Token-Based Authorization

**1. Define the resource:**

```go
resource := cedarling_go.EntityData{
    CedarMapping: cedarling_go.CedarMapping{
        EntityType: "Jans::Issue",
        ID:         "random_id",
    },
    Payload: map[string]any{
        "org_id":  "some_long_id",
        "country": "US",
    },
}
```

**2. Define the action:**

```go
action := `Jans::Action::"Update"`
```

**3. Build the request with tokens:**

```go
request := cedarling_go.Request{
    Tokens: map[string]string{
        "access_token":   "your.jwt.token",
        "id_token":       "your.id.token",
        "userinfo_token": "your.userinfo.token",
    },
    Action:   action,
    Resource: resource,
}
```

**4. Authorize:**

```go
result, err := instance.Authorize(request)
if err != nil {
    // Handle error
}

if result.Decision {
    fmt.Println("Access granted")
} else {
    fmt.Println("Access denied")
}
```

#### Unsigned Authorization

In unsigned authorization, you pass a set of Principals directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available.

**1. Define the principals:**

```go
principals := []cedarling_go.EntityData{
    {
        CedarMapping: cedarling_go.CedarMapping{
            EntityType: "Jans::User",
            ID:         "random_id",
        },
        Payload: map[string]any{
            "role":    []string{"admin"},
            "country": "US",
            "sub":     "random_sub",
        },
    },
}
```

**2. Build the request:**

```go
request := cedarling_go.RequestUnsigned{
    Principals: principals,
    Action:     `Jans::Action::"Update"`,
    Resource:   resource, // From previous example
}
```

**3. Authorize:**

```go
result, err := instance.AuthorizeUnsigned(request)
if err != nil {
    // Handle error
}
if result.Decision {
   fmt.Println("Access granted")
} else {
    fmt.Println("Access denied")
}
```

#### Multi-Issuer Authorization

Multi-issuer authorization allows you to make authorization decisions based on multiple JWT tokens from different issuers in a single request without requiring traditional User/Workload principals.

**1. Create tokens with explicit type mappings:**

```go
tokens := []cedarling_go.TokenInput{
    {
        Mapping: "Jans::Access_Token",
        Payload: "eyJhbGciOiJIUzI1NiIs...",
    },
    {
        Mapping: "Jans::Id_Token",
        Payload: "eyJhbGciOiJFZERTQSIs...",
    },
    {
        Mapping: "Acme::DolphinToken",  // Custom token type
        Payload: "ey1b6cfMef21084633a7...",
    },
}
```

**2. Define the resource:**

```go
resource := cedarling_go.EntityData{
    CedarMapping: cedarling_go.CedarMapping{
        EntityType: "Jans::Document",
        ID:         "doc_123",
    },
    Payload: map[string]any{
        "owner":          "alice@example.com",
        "classification": "confidential",
    },
}
```

**3. Build the multi-issuer request:**

```go
request := cedarling_go.AuthorizeMultiIssuerRequest{
    Tokens:   tokens,
    Action:   `Jans::Action::"Read"`,
    Resource: resource,
    Context: map[string]any{
        "ip_address": "54.9.21.201",
        "time":       time.Now().Unix(),
    },
}
```

**4. Authorize:**

```go
result, err := instance.AuthorizeMultiIssuer(request)
if err != nil {
    // Handle error
    fmt.Printf("Authorization failed: %v\n", err)
    return
}

if result.Decision {
    fmt.Println("Access granted")
    fmt.Printf("Request ID: %s\n", result.RequestID)

    // Access detailed Cedar response
    fmt.Printf("Cedar decision: %s\n", result.Response.Decision().ToString())

    // Get diagnostic information
    diagnostics := result.Response.Diagnostics()
    if len(diagnostics.Reason()) > 0 {
        fmt.Printf("Policies used: %v\n", diagnostics.Reason())
    }
    if len(diagnostics.Errors()) > 0 {
        fmt.Printf("Errors: %v\n", diagnostics.Errors())
    }
} else {
    fmt.Println("Access denied")
}
```

**Key differences from standard authorization:**

- No User/Workload principals - authorization based purely on token entities
- Supports multiple tokens from different issuers in a single request
- Tokens referenced in policies as `context.tokens.{issuer}_{token_type}`
- Custom token types supported via `Mapping` field

**Policy Example for Multi-Issuer:**

```cedar
// Require token from specific issuer with claim
permit(
  principal,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  context has tokens.acme_access_token &&
  context.tokens.acme_access_token.hasTag("scope") &&
  context.tokens.acme_access_token.getTag("scope").contains("read:documents")
};

// Require tokens from multiple issuers
permit(
  principal,
  action == Trade::Action::"Vote",
  resource in Trade::Election
) when {
  context has tokens.trade_association_access_token &&
  context.tokens.trade_association_access_token.hasTag("member_status") &&
  context.tokens.trade_association_access_token.getTag("member_status").contains("Corporate Member") &&
  context has tokens.company_access_token &&
  context.tokens.company_access_token.hasTag("employee_id")
};
```

### Logging

Retrieve logs stored in memory:

```go
// Get all logs and clear the buffer
logs := instance.PopLogs()

// Get a specific log by ID
log := instance.GetLogById("log123")

// Get logs by tag (e.g., "info")
logs := instance.GetLogsByTag("info")
```

## Defined API

Auto-generated documentation is available on [pkg.go.dev](https://pkg.go.dev/github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go).

## See Also

- [Multi-Issuer Authorization Details](../cedarling-authz.md#multi-issuer-authorization-authorize_multi_issuer)
- [JWT Mapping for Multi-Issuer](../cedarling-jwt-mapping.md#multi-issuer-jwt-mapping-authorize_multi_issuer)
- [Policy Store Configuration](../cedarling-policy-store.md#multi-issuer-token-entities)
