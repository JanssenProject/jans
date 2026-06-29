# Cedarling Rust Developer Guide

## Including in the project

The Cedarling library is not yet uploaded to [crates.io](https://crates.io), so the only way to use it right now is by including it directly from the source in your project.

**1. Clone the Jans repository**

```
git clone https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling/cedarling
```

**2. Add to your `Cargo.toml`**

```
[dependencies]
cedarling = { path = "path/to/jans/jans-cedarling/cedarling" }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
tokio = { version = "1.0", features = ["full"] }
```

**3. Alternative: Using Git Dependency**

```
[dependencies]
cedarling = { git = "https://github.com/JanssenProject/jans", package = "cedarling" }
```

## Building

### Building Cedarling

To build an executable library for Cedarling, follow the instructions [here](https://docs.jans.io/head/cedarling/tutorials/rust/#building-from-source).

### Building Examples

The Cedarling project includes several examples that demonstrate different use cases. Follow the steps below to build these examples.

### Building Examples

**1. Clone the repository**

```
git clone https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling
```

**2. Build the library**

```
cargo build --release
```

**3. Run tests**

```
cargo test --workspace
```

**4. Generate documentation**

```
cargo doc -p cedarling --no-deps --open
```

**5. Build examples**

```
# Run unsigned authorization example
cargo run -p cedarling --example authorize_unsigned

# Run multi-issuer profiling example
cargo run -p cedarling --example profiling_multi_issuer

# Stdout logging
cargo run -p cedarling --example log_init -- stdout

# Memory logging with TTL
cargo run -p cedarling --example log_init -- memory 3600

# Disable logging
cargo run -p cedarling --example log_init -- off

# Lock Server Integration
cargo run -p cedarling --example lock_integration
```

### Complete Example

Here's a complete example showing initialization, authorization, and logging:

```
use cedarling::*;
use std::collections::{HashMap, HashSet};
use jsonwebtoken::Algorithm;
use serde_json::json;

static POLICY_STORE_YAML: &str = include_str!("../../test_files/policy-store_ok.yaml");

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize Cedarling
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "example_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600, // 1 hour
                max_item_size: None,
                max_items: None,
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_YAML.to_string()),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
        }.allow_all_algorithms(),
        authorization_config: AuthorizationConfig::default(),
        lock_config: None,
    }).await?;

    // Perform unsigned authorization
    let principal = Some(EntityData {
        cedar_entity_mapping: CedarEntityMapping {
            entity_type: "Jans::User".to_string(),
            id: "test_user".to_string(),
        },
        attributes: HashMap::from_iter([
            ("email".to_string(), json!("admin@example.com")),
        ]),
    });

    let resource = EntityData {
        cedar_entity_mapping: CedarEntityMapping {
            entity_type: "Jans::Application".to_string(),
            id: "admin_panel".to_string(),
        },
        attributes: HashMap::from_iter([
            ("permission_level".to_string(), json!("admin")),
        ]),
    };

    let request = RequestUnsigned {
        principal,
        action: r#"Jans::Action::"Read""#.to_string(),
        resource,
        context: json!({}),
    };

    let result = cedarling.authorize_unsigned(request).await?;
    println!("Authorization result: {}", result.decision);

    // Retrieve logs
    let logs = cedarling.pop_logs();
    println!("Retrieved {} log entries", logs.len());

    Ok(())
}
```

## API Reference

### Core Types

- `Cedarling`

  The main struct for interacting with Cedarling.

  ```
  pub struct Cedarling {
      // Implementation details
  }
  ```

#### `BootstrapConfig`

Configuration for initializing Cedarling.

```
pub struct BootstrapConfig {
    pub application_name: String,
    pub log_config: LogConfig,
    pub policy_store_config: PolicyStoreConfig,
    pub jwt_config: JwtConfig,
    pub authorization_config: AuthorizationConfig,
    pub lock_config: Option<LockConfig>,
}
```

#### `RequestUnsigned`

Unsigned authorization request. `principal` is optional — when `None`, Cedarling evaluates the request with Cedar's partial evaluator and fails closed on any residual policy that could otherwise permit the request.

```
pub struct RequestUnsigned {
    pub principal: Option<EntityData>,
    pub action: String,
    pub resource: EntityData,
    pub context: serde_json::Value,
}
```

#### `EntityData`

Represents an entity in the authorization system.

```
pub struct EntityData {
    pub cedar_entity_mapping: CedarEntityMapping,
    pub attributes: HashMap<String, serde_json::Value>,
}
```

#### `CedarEntityMapping`

Represents the entity type and id mapping.

```
pub struct CedarEntityMapping {
    pub entity_type: String,
    pub id: String,
}
```

### Main Methods

#### `Cedarling::new()`

Initialize a new Cedarling instance.

```
pub async fn new(config: &BootstrapConfig) -> Result<Self, CedarlingError>
```

#### `authorize_unsigned()`

Perform unsigned authorization with an optional directly-provided principal.

```
pub async fn authorize_unsigned(&self, request: RequestUnsigned) -> Result<AuthorizeResult, CedarlingError>
```

#### `authorize_multi_issuer()`

Perform token-based authorization using multi-issuer tokens.

```
pub async fn authorize_multi_issuer(&self, request: AuthorizeMultiIssuerRequest) -> Result<MultiIssuerAuthorizeResult, AuthorizeError>
```

#### `pop_logs()`

Retrieve and clear all logs.

```
pub fn pop_logs(&self) -> Vec<LogEntry>
```

#### `get_log_ids()`

Get all available log IDs.

```
pub fn get_log_ids(&self) -> Vec<String>
```

#### `get_log_by_id()`

Get a specific log entry by ID.

```
pub fn get_log_by_id(&self, id: &str) -> Option<LogEntry>
```

## Configuration

### Bootstrap Properties

Cedarling can be configured using bootstrap properties. See the [bootstrap properties documentation](https://docs.jans.io/head/cedarling/reference/cedarling-properties/index.md) for complete configuration options.

### Environment Variables

You can also configure Cedarling using environment variables:

```
export CEDARLING_APPLICATION_NAME="my_app"
export CEDARLING_LOG_TYPE="stdout"
export CEDARLING_LOG_LEVEL="INFO"
export CEDARLING_POLICY_STORE_LOCAL_FN="/path/to/policy-store.yaml"
```

### Configuration Loading

```
use cedarling::*;

// Load from environment variables
let config = BootstrapConfig::from_env();

// Load from JSON string
let config = BootstrapConfig::from_json(json_string)?;

// Load from file
let config = BootstrapConfig::from_file("config.json")?;
```

## Testing and Debugging

### Running Tests

```
# Run all tests
cargo test --workspace

# Run specific test
cargo test -p cedarling test_name

# Run tests with output
cargo test -- --nocapture
```

### Running Benchmarks

```
# Run all benchmarks
cargo bench -p cedarling

# Run specific benchmark
cargo bench -p cedarling benchmark_name
```

### Code Coverage

```
# Install coverage tool
cargo install cargo-llvm-cov

# Generate coverage report
cargo llvm-cov --html --open
```

### Profiling

```
# Run profiling example
cargo run --example profiling
```

### Debugging

Enable debug logging:

```
let config = BootstrapConfig {
    log_config: LogConfig {
        log_level: LogLevel::DEBUG,
        ..Default::default()
    },
    // ... other config
};
```

## See Also

- [Getting Started with Cedarling Rust](https://docs.jans.io/head/cedarling/tutorials/rust/index.md)
- [Cedarling TBAC quickstart](https://docs.jans.io/head/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](https://docs.jans.io/head/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-application-asserted-identity)
- [Cedarling Sidecar Tutorial](https://docs.jans.io/head/cedarling/developer/sidecar/cedarling-sidecar-tutorial/index.md)
- [Cedarling Properties](https://docs.jans.io/head/cedarling/reference/cedarling-properties/index.md)
