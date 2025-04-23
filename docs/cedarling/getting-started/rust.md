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

```rs
let access_token = "<access_token>";
let id_token = "<id_token>";
let userinfo_token = "<userinfo_token>";
```

Your *principals* will be built from this tokens.

**2. Define the resource**

```rs
use std::collections::HashMap;
use serde_json::json;

let resource = EntityData {
  entity_type: "Jans::Application".to_string(),
  id: "app_id_001".to_string(),
  payload: HashMap::from_iter([
    ("protocol".to_string(), json!("https")),
    ("host".to_string(), json!("example.com")),
    ("path".to_string(), json!("/admin-dashboard")),
  ]),
}
```

**3. Define the action**

```rs
let action = r#"Jans::Action::"Read""#.to_string();
```

**4. Define Context**

```rs
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

```rs
use std::collection::HashMap;

let request = Request {
  tokens: HashMap::from([
    ("access_token".to_string(), access_token),
    ("id_token".to_string(), id_token),
    ("userinfo_token".to_string(), userinfo_token),
  ]),
  action: action,
  resource: resource,
  context: context,
};
```

**6. Authorize**

```rs
let authorize_result = cedarling.authorize(request).await;
```

#### Unsigned Authorization

In unsigned authorization, you pass a set of Principals directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available.

**1. Define the Principals**

```rs
use cedarling::*;

let principals = vec![
  EntityData {
    entity_type: "Jans::Workload".to_string(),
    id: "some_workload_id".to_string(),
    payload: HashMap::from_iter([
      ("client_id".to_string(), json!("some_client_id")),
    ]),
  },
  EntityData {
    entity_type: "Jans::User".to_string(),
    id: "random_user_id".to_string(),
    payload: HashMap::from_iter([
      ("roles".to_string(), json!(["admin", "manager"])),
    ]),
  },
]
```

**2. Define the Resource**

This represents the *resource* that the action will be performed on, such as a protected API endpoint or file.

```rs
use std::collections::HashMap;
use serde_json::json;

let resource = EntityData {
  entity_type: "Jans::Application".to_string(),
  id: "app_id_001".to_string(),
  payload: HashMap::from_iter([
    ("protocol".to_string(), json!("https")),
    ("host".to_string(), json!("example.com")),
    ("path".to_string(), json!("/admin-dashboard")),
  ]),
}
```

**3. Define the Action**

An *action* represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```rs
let action = r#"Jans::Action::"Read""#.to_string();
```

**4. Define the Context**

The *context* represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```rs
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

Now you'll construct the ***request*** by including the *principals*, *action*, and *context*.

```rs
use std::collection::HashMap;

let request = RequestUnsigned {
  principals,
  action,
  resource,
  context,
};
```

**6. Perform Authorization**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

```rs
let result = cedarling.authorize_unsigned(request).await;
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```rs
let logs = cedarling.pop_logs();
println!("{:#?}", logs);
```

---

## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)
