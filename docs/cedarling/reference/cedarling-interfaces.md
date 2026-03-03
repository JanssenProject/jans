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

- `Entity(entity_type, id, payload)`

    Creates a `Principal` or a `Resource` entity.

    - `from_dict(value)`

        Creates a `Principal` or a `Resource` entity from a dictionary.

- `Request(tokens, action, resource, context)`

    Creates a `Request` object which contains inputs for the Cedarling's authorization call.

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

- `authorize(request)`

    Runs authorization against the provided `Request` object.

- `authorize_unsigned(request)`

    Runs unsigned authorization against the provided `RequestUnsigned` object. A trusted issuer is not required for this call.

- `authorize_multi_issuer(request)`

    Runs multi-issuer authorization against the provided `AuthorizeMultiIssuerRequest` object. Validates multiple JWT tokens from different issuers and evaluates policies based on token entities without requiring traditional user/workload principals.

### Authz Result

The following methods are called on the result obtained from the authorization call to view and analyze results, reasons and possible errors.

#### AuthorizeResult (for `authorize` and `authorize_unsigned`)

- `is_allowed()`

    Returns `true` only if the overall decision of the Cedarling is `true`.

- `workload()`

    Returns the decision of the `Workload` authorization

- `principal()`

    Returns the decision of the `Principal` authorization

- `request_id()`

    Returns the request ID of this authorization call. This is used to retrieve logs if the Cedarling is running in memory log mode.

- `decision`

    This field represents the decision of this authorization call (allow/deny)

- `diagnostics`

    This field contains additional information regarding the decision reached by Cedarling.

    - `reason`

      This field is a set of policy IDs used to reach an `allow` decision, if they exist

    - `errors`

      This field contains a list of errors during authorization, if they exist.

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
