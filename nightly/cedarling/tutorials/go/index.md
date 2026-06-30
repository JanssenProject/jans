# Getting Started with Cedarling Go Bindings

Go bindings for the Jans Cedarling authorization engine, providing policy-based access control.

## Installation

### Build with dynamic linking

1. Download the appropriate pre-built binary for your platform from the Jans releases page or build it from source as described above.

1. Specify linker flags in your main.go file to link against the Cedarling library.

   ```
   // #cgo LDFLAGS: -L. -lcedarling_go
   import "C"
   ```

   And make sure that the Cedarling library files are located in the same directory as your main package.

1. Use `go get` to fetch the Cedarling Go package

   ```
   go get github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go
   ```

1. Build your Go application

   ```
   go build .
   ```

1. Run the application

   - **Windows**

     - Place the Rust artifacts (`cedarling_go.dll` and `cedarling_go.lib`) alongside the Go binary.
     - Windows searches libraries in directories below in the following order
       1. The directory containing your Go executable (recommended location)
       1. Windows system directories (e.g., `C:\Windows\System32`)
       1. The `PATH` environment variable directories

   - **Linux**

     Add the library directory that contains `libcedarling_go.so` to the `LD_LIBRARY_PATH` environment variable

     ```
     export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH
     ```

   - **MacOS**

     Add the library directory that contains `libcedarling_go.dylib` to the `LD_LIBRARY_PATH` environment variable

     ```
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

   ```
   git clone --depth 1 https://github.com/JanssenProject/jans.git
   ```

   We use `--depth 1` to avoid cloning unnecessary history and minimalize the download size.

   Navigate to the Cedarling Go bindings directory:

   ```
   cd jans/jans-cedarling/bindings/cedarling_go
   ```

   ```
   cargo build --release -p cedarling_go
   ```

1. Copy the built artifacts to your application directory

   ```
   # Windows
   cp target/release/cedarling_go.dll .
   cp target/release/cedarling_go.dll.lib cedarling_go.lib

   # Linux
   cp target/release/libcedarling_go.so .

   # macOS
   cp target/release/libcedarling_go.dylib .
   ```

   or use scripts provided in the repository to automate this process:

   ```
   sh build_and_copy_artifacts.sh
   ```

   Run go test to ensure everything is working correctly:

   ```
   go test .
   ```

## Usage

### Initialization

```
import "github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"

// Example configuration (populate dynamically in production)
config := map[string]any{
    "CEDARLING_APPLICATION_NAME":      "MyApp",
    "CEDARLING_LOG_LEVEL":             "INFO",
    "CEDARLING_LOG_TYPE":              "std_out",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json",
}

instance, err := cedarling_go.NewCedarling(config)
if err != nil {
    panic(err)
}
```

### Policy Store Sources

Go bindings support all native policy store source types. See [Cedarling Properties](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/index.md) for the full list of configuration options.

**Example configurations:**

```
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

See [Policy Store Formats](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#policy-store-formats) for more details.

### Authorization

Cedarling provides two main interfaces for performing authorization checks:

- [**Multi-Issuer Authorization**](#multi-issuer-authorization) processes JWT tokens from multiple issuers. Token data is mapped to Cedar entities based on `token_metadata` configuration.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass a principal directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data.

#### Multi-Issuer Authorization

**1. Define the resource:**

```
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

```
action := `Jans::Action::"Update"`
```

**3. Build the request with tokens:**

```
tokens := []cedarling_go.TokenInput{
    {Mapping: "Jans::Access_token", Payload: "your.jwt.token"},
    {Mapping: "Jans::Id_token", Payload: "your.id.token"},
}

request := cedarling_go.AuthorizeMultiIssuerRequest{
    Tokens:   tokens,
    Action:   action,
    Resource: resource,
}
```

**4. Authorize:**

```
result, err := instance.AuthorizeMultiIssuer(request)
if err != nil {
    // Handle error
}

if result.Decision {
    fmt.Println("Access granted")
} else {
    fmt.Println("Access denied")
}
```

See [Multi-Issuer Authorization](https://docs.jans.io/nightly/cedarling/reference/cedarling-multi-issuer/index.md) for more details.

#### Unsigned Authorization

In unsigned authorization, you pass a Principal directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available. The principal is optional — set it to `nil` to evaluate the request with partial evaluation.

**1. Define the principal:**

```
principal := &cedarling_go.EntityData{
    CedarMapping: cedarling_go.CedarMapping{
        EntityType: "Jans::User",
        ID:         "random_id",
    },
    Payload: map[string]any{
        "role":    []string{"admin"},
        "country": "US",
        "sub":     "random_sub",
    },
}
```

**2. Build the request:**

```
request := cedarling_go.RequestUnsigned{
    Principal: principal,
    Action:    `Jans::Action::"Update"`,
    Resource:  resource, // From previous example
}
```

**3. Authorize:**

```
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

```
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

```
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

```
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

```
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

```
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

```
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

- [Multi-Issuer Authorization Details](https://docs.jans.io/nightly/cedarling/reference/cedarling-authz/index.md)
- [Policy Store Configuration](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/index.md)
