---
tags:
  - cedarling
  - rust
  - developer
  - documentation
---

# Cedarling Rust Developer Guide

- [Requirements](#requirements)
- [Installation](#installation)
- [Building](#building)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Examples](#examples)
- [Testing and Debugging](#testing-and-debugging)

## Requirements

### Rust Installation

If you haven't installed Rust yet, follow the official installation guide:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

After installation, restart your terminal or run:

```bash
source $HOME/.cargo/env
```

Verify the installation:

```bash
rustc --version
cargo --version
```

## Installation

The Cedarling Rust library is not yet available on [crates.io](https://crates.io), so you need to include it directly from the source in your project.

### Adding to Your Project

**1. Clone the Jans repository**

```bash
git clone https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling/cedarling
```

**2. Add to your `Cargo.toml`**

```toml
[dependencies]
cedarling = { path = "path/to/jans/jans-cedarling/cedarling" }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
tokio = { version = "1.0", features = ["full"] }
```

**3. Alternative: Using Git Dependency**

```toml
[dependencies]
cedarling = { git = "https://github.com/JanssenProject/jans", package = "cedarling" }
```

## Building

### Building from Source

**1. Clone the repository**

```bash
git clone https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling
```

**2. Build the library**

```bash
cargo build --release
```

**3. Run tests**

```bash
cargo test --workspace
```

**4. Generate documentation**

```bash
cargo doc -p cedarling --no-deps --open
```

### Building Examples

The Cedarling project includes several examples that demonstrate different use cases:

```bash
# Run JWT validation example
cargo run -p cedarling --example authorize_with_jwt_validation

# Run unsigned authorization example
cargo run -p cedarling --example authorize_unsigned

# Run logging example
cargo run -p cedarling --example log_init -- stdout
```

## Usage

### Initialization

The first step is to initialize Cedarling with your configuration:

```rust
use cedarling::*;
use std::collections::HashSet;
use jsonwebtoken::Algorithm;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: true,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256
        ]),
    };

    // Initialize Cedarling with configuration
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "my_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_YAML.to_string()),
        },
        jwt_config,
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::new(serde_json::json!({
                "and": [
                    {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
                    {"===": [{"var": "Jans::User"}, "ALLOW"]}
                ]
            })).unwrap(),
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
        lock_config: None,
    }).await?;

    Ok(())
}
```

### Token-Based Authorization

Token-based authorization uses JWT tokens to extract principal information:

```rust
use cedarling::*;
use std::collections::HashMap;
use serde_json::json;

async fn perform_token_authorization(cedarling: &Cedarling) -> Result<(), Box<dyn std::error::Error>> {
    // 1. Prepare JWT tokens
    let access_token = "your_access_token_here".to_string();
    let id_token = "your_id_token_here".to_string();
    let userinfo_token = "your_userinfo_token_here".to_string();

    // 2. Define the resource
    let resource = EntityData {
        entity_type: "Jans::Application".to_string(),
        id: "app_id_001".to_string(),
        attributes: HashMap::from_iter([
            ("protocol".to_string(), json!("https")),
            ("host".to_string(), json!("example.com")),
            ("path".to_string(), json!("/admin-dashboard")),
        ]),
    };

    // 3. Define the action
    let action = r#"Jans::Action::"Read""#.to_string();

    // 4. Define context
    let context = json!({
        "current_time": std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis(),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
        "user_agent": "Linux"
    });

    // 5. Build the request
    let request = Request {
        tokens: HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]),
        action,
        resource,
        context,
    };

    // 6. Perform authorization
    let result = cedarling.authorize(request).await?;

    match result.decision {
        true => println!("Access granted"),
        false => println!("Access denied: {:?}", result.diagnostics),
    }

    Ok(())
}
```

### Unsigned Authorization

Unsigned authorization allows you to pass principals directly without JWT tokens:

```rust
use cedarling::*;
use std::collections::HashMap;
use serde_json::json;

async fn perform_unsigned_authorization(cedarling: &Cedarling) -> Result<(), Box<dyn std::error::Error>> {
    // 1. Define principals directly
    let principals = vec![
        EntityData {
            entity_type: "Jans::Workload".to_string(),
            id: "some_workload_id".to_string(),
            attributes: HashMap::from_iter([
                ("client_id".to_string(), json!("some_client_id")),
            ]),
        },
        EntityData {
            entity_type: "Jans::User".to_string(),
            id: "random_user_id".to_string(),
            attributes: HashMap::from_iter([
                ("roles".to_string(), json!(["admin", "manager"])),
                ("email".to_string(), json!("user@example.com")),
            ]),
        },
    ];

    // 2. Define the resource
    let resource = EntityData {
        entity_type: "Jans::Application".to_string(),
        id: "app_id_001".to_string(),
        attributes: HashMap::from_iter([
            ("protocol".to_string(), json!("https")),
            ("host".to_string(), json!("example.com")),
            ("path".to_string(), json!("/admin-dashboard")),
        ]),
    };

    // 3. Define the action
    let action = r#"Jans::Action::"Update""#.to_string();

    // 4. Define context
    let context = json!({
        "current_time": std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis(),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
        "user_agent": "Linux"
    });

    // 5. Build the unsigned request
    let request = RequestUnsigned {
        principals,
        action,
        resource,
        context,
    };

    // 6. Perform authorization
    let result = cedarling.authorize_unsigned(request).await?;

    match result.decision {
        true => println!("Access granted"),
        false => println!("Access denied: {:?}", result.diagnostics),
    }

    Ok(())
}
```

### Log Retrieval

Cedarling provides comprehensive logging capabilities:

```rust
use cedarling::*;

async fn retrieve_logs(cedarling: &Cedarling) -> Result<(), Box<dyn std::error::Error>> {
    // Get all log IDs
    let log_ids = cedarling.get_log_ids();
    println!("Available log IDs: {:?}", log_ids);

    // Get specific log by ID
    for id in &log_ids {
        if let Some(log_entry) = cedarling.get_log_by_id(id) {
            println!("Log entry {}: {:?}", id, log_entry);
        }
    }

    // Pop all logs (retrieves and clears the buffer)
    let logs = cedarling.pop_logs();
    println!("Retrieved {} log entries", logs.len());

    for (i, log) in logs.iter().enumerate() {
        println!("Log {}: {:?}", i, log);
    }

    Ok(())
}
```

### Complete Example

Here's a complete example showing initialization, authorization, and logging:

```rust
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
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::new(serde_json::json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            )).unwrap(),
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
        lock_config: None,
    }).await?;

    // Perform unsigned authorization
    let principals = vec![EntityData {
        entity_type: "Jans::User".to_string(),
        id: "test_user".to_string(),
        attributes: HashMap::from_iter([
            ("roles".to_string(), json!(["admin"])),
            ("email".to_string(), json!("admin@example.com")),
        ]),
    }];

    let resource = EntityData {
        entity_type: "Jans::Application".to_string(),
        id: "admin_panel".to_string(),
        attributes: HashMap::from_iter([
            ("permission_level".to_string(), json!("admin")),
        ]),
    };

    let request = RequestUnsigned {
        principals,
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

#### `Cedarling`

The main struct for interacting with Cedarling.

```rust
pub struct Cedarling {
    // Implementation details
}
```

#### `BootstrapConfig`

Configuration for initializing Cedarling.

```rust
pub struct BootstrapConfig {
    pub application_name: String,
    pub log_config: LogConfig,
    pub policy_store_config: PolicyStoreConfig,
    pub jwt_config: JwtConfig,
    pub authorization_config: AuthorizationConfig,
    pub entity_builder_config: EntityBuilderConfig,
    pub lock_config: Option<LockConfig>,
}
```

#### `Request`

Token-based authorization request.

```rust
pub struct Request {
    pub tokens: HashMap<String, String>,
    pub action: String,
    pub resource: EntityData,
    pub context: serde_json::Value,
}
```

#### `RequestUnsigned`

Unsigned authorization request.

```rust
pub struct RequestUnsigned {
    pub principals: Vec<EntityData>,
    pub action: String,
    pub resource: EntityData,
    pub context: serde_json::Value,
}
```

#### `EntityData`

Represents an entity in the authorization system.

```rust
pub struct EntityData {
    pub entity_type: String,
    pub id: String,
    pub attributes: HashMap<String, serde_json::Value>,
}
```

### Main Methods

#### `Cedarling::new()`

Initialize a new Cedarling instance.

```rust
pub async fn new(config: &BootstrapConfig) -> Result<Self, CedarlingError>
```

#### `authorize()`

Perform token-based authorization.

```rust
pub async fn authorize(&self, request: Request) -> Result<AuthorizeResult, CedarlingError>
```

#### `authorize_unsigned()`

Perform unsigned authorization.

```rust
pub async fn authorize_unsigned(&self, request: RequestUnsigned) -> Result<AuthorizeResult, CedarlingError>
```

#### `pop_logs()`

Retrieve and clear all logs.

```rust
pub fn pop_logs(&self) -> Vec<LogEntry>
```

#### `get_log_ids()`

Get all available log IDs.

```rust
pub fn get_log_ids(&self) -> Vec<String>
```

#### `get_log_by_id()`

Get a specific log entry by ID.

```rust
pub fn get_log_by_id(&self, id: &str) -> Option<LogEntry>
```

## Configuration

### Bootstrap Properties

Cedarling can be configured using bootstrap properties. See the [bootstrap properties documentation](../cedarling-properties.md) for complete configuration options.

### Environment Variables

You can also configure Cedarling using environment variables:

```bash
export CEDARLING_APPLICATION_NAME="my_app"
export CEDARLING_LOG_TYPE="stdout"
export CEDARLING_LOG_LEVEL="INFO"
export CEDARLING_POLICY_STORE_LOCAL_FN="/path/to/policy-store.yaml"
```

### Configuration Loading

```rust
use cedarling::*;

// Load from environment variables
let config = BootstrapConfig::from_env();

// Load from JSON string
let config = BootstrapConfig::from_json(json_string)?;

// Load from file
let config = BootstrapConfig::from_file("config.json")?;
```

## Examples

The Cedarling project includes several examples in the `examples/` directory:

### JWT Validation Example

```bash
cargo run -p cedarling --example authorize_with_jwt_validation
```

### Unsigned Authorization Example

```bash
cargo run -p cedarling --example authorize_unsigned
```

### Logging Examples

```bash
# Stdout logging
cargo run -p cedarling --example log_init -- stdout

# Memory logging with TTL
cargo run -p cedarling --example log_init -- memory 3600

# Disable logging
cargo run -p cedarling --example log_init -- off
```

### Lock Server Integration

```bash
cargo run -p cedarling --example lock_integration
```

## Testing and Debugging

### Running Tests

```bash
# Run all tests
cargo test --workspace

# Run specific test
cargo test -p cedarling test_name

# Run tests with output
cargo test -- --nocapture
```

### Running Benchmarks

```bash
# Run all benchmarks
cargo bench -p cedarling

# Run specific benchmark
cargo bench -p cedarling benchmark_name
```

### Code Coverage

```bash
# Install coverage tool
cargo install cargo-llvm-cov

# Generate coverage report
cargo llvm-cov --html --open
```

### Profiling

```bash
# Run profiling example
cargo run --example profiling
```

### Debugging

Enable debug logging:

```rust
let config = BootstrapConfig {
    log_config: LogConfig {
        log_level: LogLevel::DEBUG,
        ..Default::default()
    },
    // ... other config
};
```

## See Also

- [Getting Started with Cedarling Rust](../getting-started/rust.md)
- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)
- [Cedarling Properties](../cedarling-properties.md)
