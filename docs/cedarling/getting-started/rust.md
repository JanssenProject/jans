---
tags:
  - cedarling
  - rust
  - getting-started
---

# Getting Started with Cedarling Rust

- [Installation](#installation)
- [Usage](#usage)

## Installation

The Cedarling library is not yet uploaded to [crates.io](jans-cedarling) so the only way to use them right now is by including it directly from the source in your project.

To get started, clone the [Jans](https://github.com/JanssenProject/jans) repository. The source can be found in the [`jans-cedarling/cedarling`] directory.

## Usage

### Initialization

```rs
use cedarling::*;

// Load the bootstrap properties from the environment variable, using default values
// for unset properties
let bootstrap_config = BootstrapConfig.from_env();

// Initialize Cedarling
let cedarling = Cedarling::new(bootstrap_config)
```

See the [bootstrap properties docs](../cedarling-properties.md) for other config loading options.

### Authorization

Cedarling provides two main interfaces for performing authorization checks: **Token-Based Authorization** and **Unsigned Authorization**. Both methods involve evaluating access requests based on various factors, including principals (entities), actions, resources, and context. The difference lies in how the Principals are provided.

- [**Token-Based Authorization**](#token-based-authorization) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data, or when tokens are not available.

#### Token-Based Authorization

To perform an authorization check, follow these steps:

**1. Prepare tokens**

```rust
let access_token = "your_access_token_here".to_string();
let id_token = "your_id_token_here".to_string();
let userinfo_token = "your_userinfo_token_here".to_string();
```

Your _principals_ will be built from these tokens.

**2. Define the resource**

```rust
use std::collections::HashMap;
use serde_json::json;

let resource = EntityData {
    entity_type: "Jans::Application".to_string(),
    id: "app_id_001".to_string(),
    attributes: HashMap::from_iter([
        ("protocol".to_string(), json!("https")),
        ("host".to_string(), json!("example.com")),
        ("path".to_string(), json!("/admin-dashboard")),
    ]),
};
```

**3. Define the action**

```rust
let action = r#"Jans::Action::"Read""#.to_string();
```

**4. Define Context**

```rust
use std::time::{SystemTime, UNIX_EPOCH};
use serde_json::json;

let context = json!({
    "current_time": SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis(),
    "device_health": ["Healthy"],
    "fraud_indicators": ["Allowed"],
    "geolocation": ["America"],
    "network": "127.0.0.1",
    "network_type": "Local",
    "operating_system": "Linux",
    "user_agent": "Linux"
});
```

**5. Build the request**

```rust
use std::collections::HashMap;

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
```

**6. Authorize**

```rust
let result = cedarling.authorize(request).await?;

match result.decision {
    true => println!("Access granted"),
    false => println!("Access denied: {:?}", result.diagnostics),
}
```

#### Unsigned Authorization

In unsigned authorization, you pass a set of Principals directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available.

**1. Define the Principals**

```rust
use cedarling::*;
use std::collections::HashMap;
use serde_json::json;

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
```

**2. Define the Resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```rust
use std::collections::HashMap;
use serde_json::json;

let resource = EntityData {
    entity_type: "Jans::Application".to_string(),
    id: "app_id_001".to_string(),
    attributes: HashMap::from_iter([
        ("protocol".to_string(), json!("https")),
        ("host".to_string(), json!("example.com")),
        ("path".to_string(), json!("/admin-dashboard")),
    ]),
};
```

**3. Define the Action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```rust
let action = r#"Jans::Action::"Read""#.to_string();
```

**4. Define the Context**

The _context_ represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```rust
use std::time::{SystemTime, UNIX_EPOCH};
use serde_json::json;

let context = json!({
    "current_time": SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis(),
    "device_health": ["Healthy"],
    "fraud_indicators": ["Allowed"],
    "geolocation": ["America"],
    "network": "127.0.0.1",
    "network_type": "Local",
    "operating_system": "Linux",
    "user_agent": "Linux"
});
```

**5. Build the Request**

Now you'll construct the **_request_** by including the _principals_, _action_, and _context_.

```rust
use std::collections::HashMap;

let request = RequestUnsigned {
    principals,
    action,
    resource,
    context,
};
```

**6. Perform Authorization**

Finally, call the `authorize_unsigned` function to check whether the principals are allowed to perform the specified action on the resource.

```rust
let result = cedarling.authorize_unsigned(request).await?;

match result.decision {
    true => println!("Access granted"),
    false => println!("Access denied: {:?}", result.diagnostics),
}
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```rust
let logs = cedarling.pop_logs();
println!("{:#?}", logs);
```

For more detailed logging capabilities, see the [Cedarling Rust Developer Guide](../cedarling-rust.md#log-retrieval).

```

---

## See Also

- [Cedarling Rust Developer Guide](../cedarling-rust.md)
- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)
```
