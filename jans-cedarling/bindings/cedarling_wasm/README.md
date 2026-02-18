# Cedarling WASM

This module is designed to build cedarling for browser wasm.

## Building

For building we use [`wasm-pack`](https://developer.mozilla.org/en-US/docs/WebAssembly/Rust_to_Wasm) for install you can use command `cargo install wasm-pack`

Build cedarling in release:

```bash
wasm-pack build --release --target web
```

Build cedarling in dev mode

```bash
wasm-pack build --target web --dev
```

Result files will be in `pkg` folder.

## Testing

For WASM testing we use `wasm-pack` and it allows to make test in `node`, `chrome`, `firefox`, `safari`. You just need specify appropriate flag.

Example for firefox.

```bash
wasm-pack test --firefox
```

## Run browser example

To run example using `index.html` you need execute following steps:

1. Build wasm cedarling.
2. Run webserver using `python3 -m http.server` or any other.
3. Visit example app [localhost](http://localhost:8000/), on this app you will get log in browser console.
   - Also you can try use cedarling with web app using [cedarling_app](http://localhost:8000/cedarling_app.html), using custom bootstrap properties and request.

## WASM Usage

After building WASM bindings in folder `pkg` you can find where you can find `cedarling_wasm.js` and `cedarling_wasm.d.ts` where is defined interface for application.

In `index.html` described simple usage of `cedarling wasm` API:

```js
import { BOOTSTRAP_CONFIG, REQUEST } from "/example_data.js"; // Import js objects: bootstrap config and request
import initWasm, { init } from "/pkg/cedarling_wasm.js";

async function main() {
  await initWasm(); // Initialize the WebAssembly module

  let instance = await init(BOOTSTRAP_CONFIG);
  let result = await instance.authorize(REQUEST);
  console.log("result:", result);
}
main().catch(console.error);
```

Before using any function from library you need initialize WASM runtime by calling `initWasm` function.

### Defined API

```ts
/**
 * Create a new instance of the Cedarling application.
 * This function can take as config parameter the eather `Map` other `Object`
 */
export function init(config: any): Promise<Cedarling>;

/**
 * Create a new instance of the Cedarling application from archive bytes.
 *
 * This function allows loading a policy store from a Cedar Archive (.cjar)
 * that was fetched with custom logic (e.g., with authentication headers).
 *
 * # Arguments
 * * `config` - Bootstrap configuration (Map or Object). Policy store config is ignored.
 * * `archive_bytes` - The .cjar archive bytes (Uint8Array)
 *
 * # Example
 * ```javascript
 * const response = await fetch(url, { headers: { Authorization: 'Bearer ...' } });
 * const bytes = new Uint8Array(await response.arrayBuffer());
 * const cedarling = await init_from_archive_bytes(config, bytes);
 * ```
 */
export function init_from_archive_bytes(config: any, archive_bytes: Uint8Array): Promise<Cedarling>;

/**
 * The instance of the Cedarling application.
 */
export class Cedarling {
  /**
   * Create a new instance of the Cedarling application.
   * Assume that config is `Object`
   */
  static new(config: object): Promise<Cedarling>;
  /**
   * Create a new instance of the Cedarling application.
   * Assume that config is `Map`
   */
  static new_from_map(config: Map<any, any>): Promise<Cedarling>;
  /**
   * Authorize request
   * makes authorization decision based on the [`Request`]
   */
  authorize(request: any): Promise<AuthorizeResult>;
  /**
   * Authorize request for unsigned principals.
   * makes authorization decision based on the [`RequestUnsigned`]
   */
  authorize_unsigned(request: any): Promise<AuthorizeResult>;
  /**
   * Authorize multi-issuer request.
   * Makes authorization decision based on multiple JWT tokens from different issuers
   */
  authorize_multi_issuer(request: any): Promise<MultiIssuerAuthorizeResult>;
  /**
   * Get logs and remove them from the storage.
   * Returns `Array` of `Map`
   */
  pop_logs(): Array<any>;
  /**
   * Get specific log entry.
   * Returns `Map` with values or `null`.
   */
  get_log_by_id(id: string): any;
  /**
   * Returns a list of all log ids.
   * Returns `Array` of `String`
   */
  get_log_ids(): Array<any>;
  /**
   * Get logs by tag, like `log_kind` or `log level`.
   * Tag can be `log_kind`, `log_level`.
   */
  get_logs_by_tag(tag: string): any[];
  /**
   * Get logs by request_id.
   * Return log entries that match the given request_id.
   */
  get_logs_by_request_id(request_id: string): any[];
  /**
   * Get log by request_id and tag, like composite key `request_id` + `log_kind`.
   * Tag can be `log_kind`, `log_level`.
   * Return log entries that match the given request_id and tag.
   */
  get_logs_by_request_id_and_tag(request_id: string, tag: string): any[];
  /**
   * Push a value into the data store with an optional TTL.
   * If the key already exists, the value will be replaced.
   * If TTL is not provided, the default TTL from configuration is used.
   *
   * # Arguments
   * * `key` - The key for the data entry
   * * `value` - The value to store (any JSON-serializable value)
   * * `ttl_secs` - Optional TTL in seconds (undefined uses default from config)
   *
   * # Example
   * ```javascript
   * await cedarling.push_data_ctx("user:123", { name: "John", age: 30 }, 3600);
   * await cedarling.push_data_ctx("config", { setting: "value" }); // Uses default TTL
   * ```
   */
  push_data_ctx(key: string, value: any, ttl_secs?: number): Promise<void>;
  /**
   * Get a value from the data store by key.
   * Returns null if the key doesn't exist or the entry has expired.
   *
   * # Arguments
   * * `key` - The key to retrieve
   *
   * # Example
   * ```javascript
   * const value = await cedarling.get_data_ctx("user:123");
   * if (value) {
   *   console.log(value.name);
   * }
   * ```
   */
  get_data_ctx(key: string): Promise<any>;
  /**
   * Get a data entry with full metadata by key.
   * Returns null if the key doesn't exist or the entry has expired.
   * Includes metadata like creation time, expiration, access count, and type.
   *
   * # Arguments
   * * `key` - The key to retrieve
   *
   * # Example
   * ```javascript
   * const entry = cedarling.get_data_entry_ctx("user:123");
   * if (entry) {
   *   console.log(`Created: ${entry.created_at}, Access count: ${entry.access_count}`);
   * }
   * ```
   */
  get_data_entry_ctx(key: string): Promise<any>;
  /**
   * Remove a value from the data store by key.
   * Returns true if the key existed and was removed, false otherwise.
   *
   * # Arguments
   * * `key` - The key to remove
   *
   * # Example
   * ```javascript
   * const removed = await cedarling.remove_data_ctx("user:123");
   * ```
   */
  remove_data_ctx(key: string): Promise<boolean>;
  /**
   * Clear all entries from the data store.
   *
   * # Example
   * ```javascript
   * await cedarling.clear_data_ctx();
   * ```
   */
  clear_data_ctx(): Promise<void>;
  /**
   * List all entries with their metadata.
   * Returns an array of data entries containing key, value, type, and timing metadata.
   *
   * # Example
   * ```javascript
   * const entries = await cedarling.list_data_ctx();
   * entries.forEach(entry => {
   *   console.log(`Key: ${entry.key}, Type: ${entry.data_type}`);
   * });
   * ```
   */
  list_data_ctx(): Promise<any[]>;
  /**
   * Get statistics about the data store.
   * Returns current entry count, capacity limits, and configuration state.
   *
   * # Example
   * ```javascript
   * const stats = await cedarling.get_stats_ctx();
   * console.log(`Entries: ${stats.entry_count}/${stats.max_entries}`);
   * ```
   */
  get_stats_ctx(): Promise<DataStoreStats>;
}

/**
 * A WASM wrapper for the Rust `cedarling::AuthorizeResult` struct.
 * Represents the result of an authorization request.
 */
export class AuthorizeResult {
  /**
   * Convert `AuthorizeResult` to json string value
   */
  json_string(): string;
  /**
   * Result of authorization where principal is `Jans::Workload`
   */
  workload?: AuthorizeResultResponse;
  /**
   * Result of authorization where principal is `Jans::User`
   */
  person?: AuthorizeResultResponse;
  /**
   * Get result for a specific principal
   */
  principal(principal: string): AuthorizeResultResponse | undefined;
  /**
   * Result of authorization
   * true means `ALLOW`
   * false means `Deny`
   *
   * this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
   */
  decision: boolean;
  /**
   * Request ID of the authorization request
   */
  request_id: string;
}

/**
 * A WASM wrapper for the Rust `cedarling::MultiIssuerAuthorizeResult` struct.
 * Represents the result of a multi-issuer authorization request.
 */
export class MultiIssuerAuthorizeResult {
  /**
   * Convert `MultiIssuerAuthorizeResult` to json string value
   */
  json_string(): string;
  /**
   * Result of Cedar policy authorization
   */
  response: AuthorizeResultResponse;
  /**
   * Result of authorization
   * true means `ALLOW`
   * false means `Deny`
   */
  decision: boolean;
  /**
   * Request ID of the authorization request
   */
  request_id: string;
}

/**
 * A WASM wrapper for the Rust `cedar_policy::Response` struct.
 * Represents the result of an authorization request.
 */
export class AuthorizeResultResponse {
  /**
   * Authorization decision
   */
  readonly decision: boolean;
  /**
   * Diagnostics providing more information on how this decision was reached
   */
  readonly diagnostics: Diagnostics;
}

/**
 * Diagnostics
 * ===========
 *
 * Provides detailed information about how a policy decision was made, including policies that contributed to the decision and any errors encountered during evaluation.
 */
export class Diagnostics {
  /**
   * `PolicyId`s of the policies that contributed to the decision.
   * If no policies applied to the request, this set will be empty.
   *
   * The ids should be treated as unordered,
   */
  readonly reason: string[];
  /**
   * Errors that occurred during authorization. The errors should be
   * treated as unordered, since policies may be evaluated in any order.
   */
  readonly errors: PolicyEvaluationError[];
}

/**
 * PolicyEvaluationError
 * =====================
 *
 * Represents an error that occurred when evaluating a Cedar policy.
 */
export class PolicyEvaluationError {
  /**
   * Id of the policy with an error
   */
  readonly id: string;
  /**
   * Underlying evaluation error string representation
   */
  readonly error: string;
}

/**
 * DataStoreStats
 * ==============
 *
 * Statistics about the DataStore, providing insight into the current state
 * and usage of the data store, including memory usage metrics and capacity information.
 */
export class DataStoreStats {
  /**
   * Number of entries currently stored
   */
  readonly entry_count: number;
  /**
   * Maximum number of entries allowed (0 = unlimited)
   */
  readonly max_entries: number;
  /**
   * Maximum size per entry in bytes (0 = unlimited)
   */
  readonly max_entry_size: number;
  /**
   * Whether metrics tracking is enabled
   */
  readonly metrics_enabled: boolean;
  /**
   * Total size of all entries in bytes (approximate, based on JSON serialization)
   */
  readonly total_size_bytes: number;
  /**
   * Average size per entry in bytes (0 if no entries)
   */
  readonly avg_entry_size_bytes: number;
  /**
   * Percentage of capacity used (0.0-100.0, based on entry count)
   */
  readonly capacity_usage_percent: number;
  /**
   * Memory usage threshold percentage (from config)
   */
  readonly memory_alert_threshold: number;
  /**
   * Whether memory usage exceeds the alert threshold
   */
  readonly memory_alert_triggered: boolean;
}
```

## Configuration

### Policy Store Sources

Cedarling supports multiple ways to load policy stores. **In WASM environments, only URL-based loading is available** (no filesystem access).

#### WASM-Supported Options

```javascript
// Option 1: Fetch policy store from URL (simple)
const BOOTSTRAP_CONFIG = {
  CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
  // ... other config
};
const cedarling = await init(BOOTSTRAP_CONFIG);

// Option 2: Inline JSON string (for embedded policy stores)
// policyStoreJson is the policy store JSON as a string
// See: https://docs.jans.io/stable/cedarling/reference/cedarling-policy-store/
const policyStoreJson = '{"cedar_version":"4.0","policy_stores":{...}}';
const BOOTSTRAP_CONFIG = {
  CEDARLING_POLICY_STORE_LOCAL: policyStoreJson,
  // ... other config
};
const cedarling = await init(BOOTSTRAP_CONFIG);

// Option 3: Custom fetch with auth headers (use init_from_archive_bytes)
const response = await fetch("https://example.com/policy-store.cjar", {
  headers: { Authorization: `Bearer ${token}` },
});
const bytes = new Uint8Array(await response.arrayBuffer());
const cedarling = await init_from_archive_bytes(BOOTSTRAP_CONFIG, bytes);
```

> **Note:** Directory-based loading and file-based loading are **NOT supported in WASM** (no filesystem access). Use URL-based loading or `init_from_archive_bytes` for custom fetch scenarios.

#### Cedar Archive (.cjar) Format

For the new directory-based format in WASM, package the directory structure as a `.cjar` file (ZIP archive):

```bash
cd policy-store && zip -r ../policy-store.cjar .
```

See [Policy Store Formats](../../../docs/cedarling/reference/cedarling-policy-store.md#policy-store-formats) for details on the directory structure and metadata.json format.

### ID Token Trust Mode

The `CEDARLING_ID_TOKEN_TRUST_MODE` property controls how ID tokens are validated:

- **`strict`** (default): Enforces strict validation rules
  - ID token `aud` must match access token `client_id`
  - If userinfo token is present, its `sub` must match the ID token `sub`
- **`never`**: Disables ID token validation (useful for testing)
- **`always`**: Always validates ID tokens when present
- **`ifpresent`**: Validates ID tokens only if they are provided

### Testing Configuration

For testing scenarios, you may want to disable JWT validation. You can configure this in your bootstrap configuration:

```javascript
const BOOTSTRAP_CONFIG = {
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
  CEDARLING_ID_TOKEN_TRUST_MODE: "never",
};
```

For complete configuration documentation, see [cedarling-properties.md](../../../docs/cedarling/cedarling-properties.md) or on [our page](https://docs.jans.io/stable/cedarling/cedarling-properties/).

## Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace.

### Push Data

Store data with an optional TTL (Time To Live):

```javascript
// Push data without TTL (uses default from config)
await cedarling.push_data_ctx("user:123", {
  role: ["admin", "editor"],
  country: "US"
});

// Push data with TTL (5 minutes = 300 seconds)
await cedarling.push_data_ctx("config:app", { setting: "value" }, 300);

// Push different data types
await cedarling.push_data_ctx("key1", "string_value");
await cedarling.push_data_ctx("key2", 42);
await cedarling.push_data_ctx("key3", [1, 2, 3]);
await cedarling.push_data_ctx("key4", { nested: "data" });
```

### Get Data

Retrieve stored data:

```javascript
// Get data by key
const value = await cedarling.get_data_ctx("user:123");
if (value) {
  console.log(`User roles: ${value.role}`);
}
```

### Get Data Entry with Metadata

Get a data entry with full metadata including creation time, expiration, access count, and type:

```javascript
const entry = cedarling.get_data_entry_ctx("user:123");
if (entry) {
  console.log(`Key: ${entry.key}`);
  console.log(`Created at: ${entry.created_at}`);
  console.log(`Access count: ${entry.access_count}`);
  console.log(`Data type: ${entry.data_type}`);
  console.log(`Value:`, entry.value);
}
```

### Remove Data

Remove a specific entry:

```javascript
// Remove data by key
const removed = await cedarling.remove_data_ctx("user:123");
if (removed) {
  console.log("Entry was removed");
} else {
  console.log("Entry did not exist");
}
```

### Clear All Data

Remove all entries from the data store:

```javascript
await cedarling.clear_data_ctx();
```

### List All Data

List all entries with their metadata:

```javascript
const entries = await cedarling.list_data_ctx();
entries.forEach(entry => {
  console.log(`Key: ${entry.key}, Type: ${entry.data_type}, Created: ${entry.created_at}`);
});
```

### Get Statistics

Get statistics about the data store:

```javascript
const stats = await cedarling.get_stats_ctx();
console.log(`Entries: ${stats.entry_count}/${stats.max_entries}`);
console.log(`Total size: ${stats.total_size_bytes} bytes`);
console.log(`Capacity usage: ${stats.capacity_usage_percent}%`);
```

### Error Handling

The Context Data API methods throw errors for different error conditions:

```javascript
try {
  await cedarling.push_data_ctx("", { data: "value" }); // Empty key
} catch (error) {
  if (error.message.includes("InvalidKey")) {
    console.log("Invalid key provided");
  }
}

try {
  const value = await cedarling.get_data_ctx("nonexistent");
} catch (error) {
  if (error.message.includes("KeyNotFound")) {
    console.log("Key not found");
  }
}
```

### Using Data in Cedar Policies

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace:

```cedar
permit(
    principal,
    action == Action::"read",
    resource
) when {
    context.data["user:123"].role.contains("admin")
};
```

The data is injected into the evaluation context before policy evaluation, allowing policies to make decisions based on dynamically pushed data.
