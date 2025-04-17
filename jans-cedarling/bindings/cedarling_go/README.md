# Jans Cedarling Go Bindings

Go bindings for the Jans Cedarling authorization engine, providing policy-based access control.

## Features

- Token-based authorization (`Authorize()`)
- Custom principal authorization (`AuthorizeUnsigned()`)
- Comprehensive logging capabilities
- Flexible configuration options

## Installation

### Prerequisites

- Go 1.20+
- Rust toolchain (for building from source)

### Building from Source

1. Build the Rust library:

```bash
cargo build --release
```

1. Copy the built artifacts to folder with your application:

```bash
# Windows
cp target/release/cedarling_go.dll .
cp target/release/cedarling_go.dll.lib cedarling_go.lib

# Linux
cp target/release/libcedarling_go.so .

# macOS
cp target/release/libcedarling_go.dylib .
```

1. Build your Go application with dynamic linking:

```bash
go build .
```

Make sure Rust build artifacts (e.g. libcedarling_go.so) are located in the same directory as the Go binary or ensure the directory is included in the `LD_LIBRARY_PATH` environment variable (on Linux), for example:

```bash
export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH
```

## Usage

### Initialization

```go
import "github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"

config := map[string]any{
    "CEDARLING_APPLICATION_NAME": "MyApp",
    "CEDARLING_POLICY_STORE_ID": "your-policy-store-id",
    "CEDARLING_USER_AUTHZ": "enabled",
    "CEDARLING_WORKLOAD_AUTHZ": "enabled",
    "CEDARLING_LOG_LEVEL": "INFO",
    "CEDARLING_LOG_TYPE": "std_out",
    "CEDARLING_LOG_LEVEL": "INFO",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json",
}

instance, err := cedarling_go.NewCedarling(config)
...
```

### Token-based Authorization

```go
resource := cedarling_go.EntityData{
    EntityType: "MyApp::Resource",
    ID:         "resource123",
    Payload: map[string]any{
        "owner": "user123",
        "status": "active",
    },
}

request := cedarling_go.Request{
    Tokens: map[string]string{
        "access_token":   "your.jwt.token",
        "id_token":       "your.id.token",
        "userinfo_token": "your.userinfo.token",
    },
    Action:   "MyApp::Action::Update",
    Resource: resource,
}

result, err := instance.Authorize(request)
if err != nil {
    // handle error
}

if result.Decision {
    fmt.Println("Access granted")
} else {
    fmt.Println("Access denied")
}
```

### Custom Principal Authorization

```go
principals := []cedarling_go.EntityData{
    {
        EntityType: "MyApp::Principal",
        ID:         "principal1",
        Payload: map[string]any{
            "role": "admin",
        },
    },
}

request := cedarling_go.RequestUnsigned{
    Principals: principals,
    Action:     "MyApp::Action::View",
    Resource:   resource,
}

result, err := instance.AuthorizeUnsigned(request)
```

### Logging

Logging API can be used only with memory logger.

```go
// Get all logs
logs := instance.PopLogs()

// Get specific log by ID
log := instance.GetLogById("log123")

// Get logs by tag
logs := instance.GetLogsByTag("info")
```

## Building for Production

For production deployments, consider:

- Setting appropriate log levels
- Enabling JWT validation when using token-based auth

## For developers

We use [rust2go](https://github.com/ihciah/rust2go) to convert Rust code to Go. This tool helps in maintaining the bindings up-to-date with minimal effort.

This solution provides a seamless way to integrate Rust-based app with Golang application. But it support only basic types. Currently we use something like RPC approach and JSON for communication between Rust and Go.

If you change rust bindings to generate golang part you need to run:

```bash
rust2go-cli --src src/user.rs --dst internal/gen.go --without-main
```

And update `internal/gen.go` package name to `internal` and make other changes accordingly.
