---
tags:
  - administration
  - authorization / authz
  - Cedar
  - Cedarling
  - interfaces
---

# Cedarling Interfaces

Cedarling provides a number of methods to interface with the Cedar engine.
These are described below.

## Init

These methods are used to create a `BootstrapConfig` object, which is needed to initialize a Cedarling instance. [Bootstrap properties](./cedarling-properties.md) are required to do this.

- `load_from_file(path)`
  
    Creates a `BootstrapConfig` object by loading properties from a file

- `load_from_json(config_json)`

    Creates a `BootstrapConfig` object by reading in a string encoded JSON object containing properties.

- `from_env(options)`

    Creates a `BootstrapConfig` object by reading environment variables. If a dictionary is passed in, it will override environment variables.

- `Cedarling(bootstrap_config)`

    Initializes an instance of the Cedarling engine by reading the bootstrap configuration.

## Authz

These methods are called to create an authorization request, run authorization, and get decisions back.

- `EntityData(cedar_mapping, attributes)`

    Creates a principal or resource entity.

  - `cedar_mapping`: A `CedarEntityMapping` object with `entity_type` (Cedar type name) and `id` (entity ID).
  - `attributes`: A map of attribute names to values.

  - `from_json(json_str)` — Creates an `EntityData` from a JSON string (Rust).
  - `from_dict(value)` — Creates an `EntityData` from a dictionary (Python bindings).

    **Note on field naming:** The Rust struct field is named `cedar_mapping`, but it serializes to `cedar_entity_mapping` in JSON (via `#[serde(rename)]`). When constructing in Rust, use `cedar_mapping`. When passing JSON (via `from_json`) or a Python dict (via `from_dict`), use `cedar_entity_mapping` as the key.

- `RequestUnsigned(principals, action, resource, context)`

    Creates a `RequestUnsigned` object which contains inputs for Cedarling's unsigned authorization call.

- `TokenInput(mapping, payload)`

    Creates a `TokenInput` object representing a JWT token with an explicit type mapping. Used for multi-issuer authorization.

  - `mapping`: A string specifying the Cedar entity type (e.g., "Jans::Access_Token", "Acme::DolphinToken")
  - `payload`: The JWT token string

- `AuthorizeMultiIssuerRequest(tokens, action, resource, context)`

    Creates an `AuthorizeMultiIssuerRequest` object for multi-issuer authorization.

  - `tokens`: Array of `TokenInput` objects
  - `action`: The action to be authorized (required)
  - `resource`: The resource entity being accessed (required)
  - `context`: Optional additional context for policy evaluation

- `authorize_unsigned(request)`

    Runs unsigned authorization against the provided `RequestUnsigned` object. A trusted issuer is not required for this call.

- `authorize_multi_issuer(request)`

    Runs multi-issuer authorization against the provided `AuthorizeMultiIssuerRequest` object. Validates multiple JWT tokens from different issuers and evaluates policies based on token entities.

### Policy Introspection

These methods return metadata about policies that are potentially applicable to a given authorization request, **without executing authorization**. This is useful for admin tooling, policy auditing, and debugging.

Both methods perform scope-level filtering only (principal/action/resource constraints). Policies with `when`/`unless` conditions may still not apply at evaluation time — the returned set is a superset of truly applicable policies.

- `get_matching_policies_unsigned(principals, actions, resources)`

    Returns metadata for all policies whose scope constraints are compatible with the given principals, actions, and resources.

  - `principals`: Array of `EntityData` objects — their `entity_type` is matched against policy principal constraints
  - `actions`: Array of action strings (e.g., `Jans::Action::"Read"`) — matched against policy action constraints
  - `resources`: Array of `EntityData` objects — their `entity_type` is matched against policy resource constraints

  Returns a list of `PolicyMetadata` objects.

- `get_matching_policies_multi_issuer(tokens, actions, resources)`

    Returns metadata for all policies whose scope constraints are compatible with the given token-derived principals, actions, and resources. Tokens are validated and their mapping types are used as principal entity types.

  - `tokens`: Array of `TokenInput` objects — their `mapping` field is used as the principal entity type
  - `actions`: Array of action strings
  - `resources`: Array of `EntityData` objects

  Returns a list of `PolicyMetadata` objects.

#### PolicyMetadata

Each returned `PolicyMetadata` object contains:

- `id`: The policy ID (string)
- `annotations`: Key-value pairs from Cedar policy annotations (`@key("value")`), returned as a map of strings
- `source`: The Cedar policy source code in human-readable Cedar syntax

#### Example (Rust)

```rust
use cedarling::{Cedarling, EntityData, CedarEntityMapping, PolicyMetadata};

let principals = vec![EntityData {
    cedar_mapping: CedarEntityMapping {
        entity_type: "Jans::User".to_string(),
        id: "user1".to_string(),
    },
    attributes: Default::default(),
}];

let actions = vec![r#"Jans::Action::"Read""#.to_string()];

let resources = vec![EntityData {
    cedar_mapping: CedarEntityMapping {
        entity_type: "Jans::Document".to_string(),
        id: "doc1".to_string(),
    },
    attributes: Default::default(),
}];

let policies = cedarling
    .get_matching_policies_unsigned(principals, actions, resources)
    .await
    .expect("failed to get matching policies");

for policy in &policies {
    println!("Policy: {} (annotations: {:?})", policy.id, policy.annotations);
    println!("Source:\n{}", policy.source);
}
```

### Authz Result

The following methods are called on the result obtained from the authorization call to view and analyze results, reasons and possible errors.

#### AuthorizeResult (for `authorize_unsigned`)

- `decision`

    A boolean field representing the overall authorization decision (`true` = allow, `false` = deny). The decision is computed by applying the `principal_bool_operator` across all principal results.

- `principals`

    A map of principal type names and entity UIDs to their Cedar `Response` objects. Each response provides:

  - `decision()`: Whether this principal was allowed or denied
  - `diagnostics()`: Detailed information including `reason()` (set of policy IDs) and `errors()` (list of evaluation errors)

- `request_id`

    The request ID for this authorization call, used for log retrieval when running in memory log mode.

- `cedar_decision()`

    Returns the Cedar `Decision` enum (`Allow` or `Deny`) based on the `decision` field.

#### MultiIssuerAuthorizeResult (for `authorize_multi_issuer`)

- `decision`

    A boolean field representing whether the authorization request is allowed (`true`) or denied (`false`)

- `response`

    The Cedar policy engine response containing detailed decision information

  - `decision()` - Returns the decision (Allow/Deny)
  - `diagnostics()` - Returns diagnostics including reasons and errors
    - `reason()` - Set of policy IDs that contributed to an Allow decision
    - `errors()` - List of errors encountered during policy evaluation

- `request_id`

    The request ID for this authorization call, used for log retrieval and auditing

## Logs

These methods are called to retrieve logs from the memory of the Cedarling instance when it is running in `memory` mode.

- `pop_logs()`

    Removes and returns the latest log from the memory of the Cedarling instance

- `get_log_by_id(id)`

    Retrieves a log given the ID of an active log entry.

- `get_log_ids()`

    Returns the list of all active log entries in Cedarling's memory.

- `get_logs_by_tag(tag)`

    Returns the list of all logs with a given tag. A tag can be either the type of log (System, Decision, Metric) or the [log level](./cedarling-logs.md#system-log-levels)

- `get_logs_by_request_id(request_id)`

    Returns the list of all logs with a given request ID. This request ID is obtained from an authorization result.

- `get_logs_by_request_id_and_tag(request_id, tag)`

    Returns the list of all logs with a given request ID **and** tag.

## Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace. This enables dynamic, runtime-based authorization decisions without modifying policies.

### Push Data

- `push_data_ctx(key, value, ttl_secs)`

  Pushes a value into the data store with an optional TTL (Time To Live).
  
  - `key`: The key for the data entry (string)
  - `value`: The value to store (any JSON-serializable/Cedar value: object, array, string, number, boolean, or null)
  - `ttl_secs`: Optional TTL in seconds. If not provided, uses the default TTL from configuration.
  
  **Returns:** `None` on success (or a boolean success flag, depending on the binding).
  
  **Errors:** The method may raise the following errors:
  - `InvalidKey`: When the key is empty
  - `StorageLimitExceeded`: When the configured storage capacity (`max_entries`) is exceeded
  - `ValueTooLarge`: When the entry size (including metadata) exceeds `max_entry_size`
  - `TTLExceeded`: When the requested TTL exceeds the configured `max_ttl` limit
  
  If the key already exists, the value will be replaced.

### Get Data

- `get_data_ctx(key)`

  Retrieves a value from the data store by key.
  
  - `key`: The key to retrieve (string)
  
  Returns the value if found, or `None`/`null` if the key doesn't exist or the entry has expired.

### Get Data Entry

- `get_data_entry_ctx(key)`

  Retrieves a data entry with full metadata by key.
  
  - `key`: The key to retrieve (string)
  
  Returns a `DataEntry` object containing:
  - `key`: The entry key
  - `value`: The stored value
  - `data_type`: The inferred Cedar type (String, Long, Bool, Set, Record, Entity, Ip, Decimal, DateTime, Duration)
  - `created_at`: Timestamp when the entry was created (RFC 3339 format)
  - `expires_at`: Timestamp when the entry expires (RFC 3339 format, or null if no TTL)
  - `access_count`: Number of times this entry has been accessed

### Remove Data

- `remove_data_ctx(key)`

  Removes a value from the data store by key.
  
  - `key`: The key to remove (string)
  
  Returns `true` if the key existed and was removed, `false` otherwise.

### Clear Data

- `clear_data_ctx()`

  Removes all entries from the data store.

### List Data

- `list_data_ctx()`

  Returns a list of all entries with their metadata.
  
  Returns an array of `DataEntry` objects containing key, value, type, and timing metadata.

### Get Statistics

- `get_stats_ctx()`

  Returns statistics about the data store.
  
  Returns a `DataStoreStats` object containing:
  - `entry_count`: Number of entries currently stored
  - `max_entries`: Maximum number of entries allowed (0 = unlimited)
  - `max_entry_size`: Maximum size per entry in bytes (0 = unlimited)
  - `metrics_enabled`: Whether metrics tracking is enabled
  - `total_size_bytes`: Total size of all entries in bytes
  - `avg_entry_size_bytes`: Average size per entry in bytes
  - `capacity_usage_percent`: Percentage of capacity used (0.0-100.0)
  - `memory_alert_threshold`: Memory usage threshold percentage (from config)
  - `memory_alert_triggered`: Whether memory usage exceeds the alert threshold

### Schema Requirements

To use the Context Data API, your Cedar schema must include a `data` field in the action's context. You must explicitly define the expected structure of the data — Cedar does not support arbitrary/untyped records.

```cedar
namespace MyApp {
  // Define the structure of nested data objects
  type DataConfig = {
    "enabled": Bool
  };

  // Define all possible fields that can be pushed via push_data_ctx
  // Use optional fields (?) since not all keys may be present at evaluation time
  type DataContext = {
    "user_level"?: String,
    "feature_enabled"?: Bool,
    "config"?: DataConfig
  };

  action "read" appliesTo {
    principal: [User],
    resource: [Document],
    context: {
      "data"?: DataContext,
      // ... other context fields
    }
  };
}
```

The `data` field should be optional (`"data"?`) since it is only present when data has been pushed via `push_data_ctx`. Each key you plan to push must be declared in the `DataContext` type — Cedar will reject values with undeclared fields.

Always use `has` checks in policies before accessing data fields, since they are optional:

```cedar
context has data && context.data has user_level && context.data.user_level == "premium"
```

### Using Data in Cedar Policies

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace. The `context.data` values follow a three-tier resolution precedence:

1. **Inline request context values** (highest precedence): Values provided directly in the authorization request context override all other sources.
2. **Pushed data** (from the Context Data API): Data pushed via `push_data_ctx` overrides the default context.
3. **Default context** (lowest precedence): Values from the default context configuration are used when not overridden by higher-precedence sources.

When keys collide, higher-precedence values shadow lower-precedence ones. The `context.data` namespace combines values from all three sources, with inline values taking precedence over pushed data, and pushed data taking precedence over default context values.

**Example with safe key and attribute checks:**

```cedar
permit(
    principal,
    action == Action::"read",
    resource
) when {
    context.data has "user:123" &&
    context.data["user:123"] has "role" &&
    context.data["user:123"].role.contains("admin")
};
```

The data is injected into the evaluation context before policy evaluation, allowing policies to make decisions based on dynamically pushed data without requiring policy changes.

## Trusted Issuer Loading Info

The `TrustedIssuerLoadingInfo` trait provides information about the loading status of trusted issuers. This is useful for health checks, diagnostics, and verifying that Cedarling has successfully loaded the expected issuers before processing authorization requests.

This interface is particularly useful when `CEDARLING_TRUSTED_ISSUER_LOADER_TYPE` is set to `ASYNC`. In async mode, Cedarling starts accepting requests immediately while issuers load in the background. Use these methods to check whether all issuers are ready before relying on authorization results.

### Methods

- `is_trusted_issuer_loaded_by_name(issuer_id)` — Returns `true` if the trusted issuer with the given policy store key is loaded.

- `is_trusted_issuer_loaded_by_iss(iss_claim)` — Returns `true` if the trusted issuer with the given `iss` claim value is loaded.

- `total_issuers()` — Returns the total number of trusted issuers expected to be loaded (from the policy store configuration).

- `loaded_trusted_issuers_count()` — Returns the number of trusted issuers that have been successfully loaded.

- `loaded_trusted_issuer_ids()` — Returns the set of issuer IDs that have been successfully loaded.

- `failed_trusted_issuer_ids()` — Returns the set of issuer IDs that encountered errors during loading. Failed issuers are still counted toward total processing count.

### Example

```rust
use cedarling::{Cedarling, TrustedIssuerLoadingInfo};

fn check_health(cedarling: &Cedarling) {
    let total = cedarling.total_issuers();
    let loaded = cedarling.loaded_trusted_issuers_count();
    let failed = cedarling.failed_trusted_issuer_ids();

    println!("Issuers: {loaded}/{total} loaded");
    if !failed.is_empty() {
        println!("Failed issuers: {:?}", failed);
    }
}
```
