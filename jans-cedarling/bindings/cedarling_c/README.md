# Jans Cedarling C Bindings

C bindings for the Jans Cedarling authorization engine, providing policy-based access control through a native C API.

## Features

- Custom principal authorization (`cedarling_authorize_unsigned`)
- Multi-issuer authorization (`cedarling_authorize_multi_issuer`)
- Comprehensive logging capabilities
- Flexible configuration via JSON
- Context Data API for pushing external data into policy evaluation context
- Thread-safe instance management

## Installation

### Building from Source

**Prerequisites:**

- C compiler (GCC, Clang, or MSVC)
- Rust toolchain
- cbindgen (`cargo install cbindgen`)

**1. Clone the repository:**

```sh
git clone --depth 1 https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling/bindings/cedarling_c
```

**2. Build the Rust library:**

```sh
cargo build --release -p cedarling_c
```

**3. Generate the C header file:**

```sh
cbindgen --crate cedarling_c --output target/include/cedarling_c.h
```

**4. Copy the built artifacts:**

```sh
# Linux
cp target/release/libcedarling_c.so .

# macOS
cp target/release/libcedarling_c.dylib .

# Windows
cp target/release/cedarling_c.dll .
cp target/release/cedarling_c.dll.lib cedarling_c.lib
```

### Linking

**Linux/macOS:**

```sh
gcc -o myapp myapp.c -L. -lcedarling_c -Wl,-rpath,.
```

**Windows (MSVC):**

```sh
cl myapp.c cedarling_c.lib
```

## Usage

### Initialization

```c
#include "cedarling_c.h"

// Initialize the library
cedarling_init();

// Create a new instance with configuration
const char* config = "{"
    "\"CEDARLING_APPLICATION_NAME\": \"MyApp\","
    "\"CEDARLING_POLICY_STORE_ID\": \"your-policy-store-id\","
    "\"CEDARLING_LOG_LEVEL\": \"INFO\","
    "\"CEDARLING_LOG_TYPE\": \"std_out\","
    "\"CEDARLING_POLICY_STORE_LOCAL_FN\": \"/path/to/policy-store.yaml\""
"}";

CedarlingInstanceResult result;
int ret = cedarling_new(config, &result);
if (ret != 0) {
    printf("Error: %s\n", result.ERROR_MESSAGE);
    cedarling_free_instance_result(&result);
    return 1;
}

uint64_t instance_id = result.INSTANCE_ID;
cedarling_free_instance_result(&result);
```

### Unsigned Authorization

Use `cedarling_authorize_unsigned` when you have custom principals (not derived from JWTs):

```c
const char* request = "{"
    "\"principals\": ["
    "    {"
    "        \"cedar_entity_mapping\": {\"entity_type\": \"Jans::TestPrincipal1\", \"id\": \"user123\"},"
    "        \"is_ok\": true"
    "    }"
    "],"
    "\"action\": \"Jans::Action::\\\"UpdateForTestPrincipals\\\"\","
    "\"resource\": {"
    "    \"cedar_entity_mapping\": {\"entity_type\": \"Jans::Issue\", \"id\": \"random_id\"},"
    "    \"org_id\": \"some_long_id\","
    "    \"country\": \"US\""
    "},"
    "\"context\": {}"
"}";

CedarlingResult auth_result;
ret = cedarling_authorize_unsigned(instance_id, request, &auth_result);

if (ret == 0) {
    printf("Authorization result: %s\n", (char*)auth_result.DATA);
    // Parse the JSON result to check decision
} else {
    printf("Error: %s\n", auth_result.ERROR_MESSAGE);
}
cedarling_free_result(&auth_result);
```

### Multi-Issuer Authorization

Use `cedarling_authorize_multi_issuer` when working with JWTs from multiple issuers:

```c
const char* request = "{"
    "\"tokens\": ["
    "    {\"mapping\": \"Jans::Access_token\", \"payload\": \"eyJhbGciOiJIUzI1NiIs...\"},"
    "    {\"mapping\": \"Jans::Id_token\", \"payload\": \"eyJhbGciOiJIUzI1NiIs...\"}"
    "],"
    "\"action\": \"Jans::Action::\\\"Update\\\"\","
    "\"resource\": {"
    "    \"type\": \"Jans::Issue\","
    "    \"id\": \"random_id\""
    "},"
    "\"context\": {}"
"}";

CedarlingResult auth_result;
ret = cedarling_authorize_multi_issuer(instance_id, request, &auth_result);

if (ret == 0) {
    printf("Authorization result: %s\n", (char*)auth_result.DATA);
} else {
    printf("Error: %s\n", auth_result.ERROR_MESSAGE);
}
cedarling_free_result(&auth_result);
```

### Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace.

#### Push Data

```c
const char* value = "{\"role\": [\"admin\", \"editor\"], \"level\": 5}";
CedarlingResult push_result;
ret = cedarling_context_push(instance_id, "user:123", value, 300, &push_result);
if (ret != 0) {
    printf("Error: %s\n", push_result.ERROR_MESSAGE);
}
cedarling_free_result(&push_result);
```

#### Get Data

```c
CedarlingResult get_result;
ret = cedarling_context_get(instance_id, "user:123", &get_result);
if (ret == 0) {
    printf("Value: %s\n", (char*)get_result.DATA);  // Returns JSON or "null"
}
cedarling_free_result(&get_result);
```

#### Remove Data

```c
CedarlingResult remove_result;
ret = cedarling_context_remove(instance_id, "user:123", &remove_result);
if (ret == 0) {
    printf("Removed: %s\n", (char*)remove_result.DATA);  // {"removed": true/false}
}
cedarling_free_result(&remove_result);
```

#### Clear All Data

```c
CedarlingResult clear_result;
ret = cedarling_context_clear(instance_id, &clear_result);
cedarling_free_result(&clear_result);
```

#### List All Entries

```c
CedarlingResult list_result;
ret = cedarling_context_list(instance_id, &list_result);
if (ret == 0) {
    printf("Entries: %s\n", (char*)list_result.DATA);  // JSON array of entries
}
cedarling_free_result(&list_result);
```

#### Get Statistics

```c
CedarlingResult stats_result;
ret = cedarling_context_stats(instance_id, &stats_result);
if (ret == 0) {
    printf("Stats: %s\n", (char*)stats_result.DATA);
}
cedarling_free_result(&stats_result);
```

### Logging

Retrieve logs stored in memory:

```c
// Get all logs and clear the buffer
CedarlingStringArray logs;
ret = cedarling_pop_logs(instance_id, &logs);
for (size_t i = 0; i < logs.COUNT; i++) {
    printf("Log: %s\n", logs.ITEMS[i]);
}
cedarling_free_string_array(&logs);

// Get log IDs
CedarlingStringArray log_ids;
ret = cedarling_get_log_ids(instance_id, &log_ids);
cedarling_free_string_array(&log_ids);

// Get logs by tag
ret = cedarling_get_logs_by_tag(instance_id, "authorization", &logs);
cedarling_free_string_array(&logs);

// Get logs by request ID
ret = cedarling_get_logs_by_request_id(instance_id, "req-123", &logs);
cedarling_free_string_array(&logs);
```

### Trusted Issuer Loading Info

When a policy store includes `trusted-issuers/` entries, you can inspect loading status:

```c
bool loaded = false;
bool loaded_by_iss = false;
size_t total = 0;
size_t loaded_count = 0;

ret = cedarling_is_trusted_issuer_loaded_by_name(instance_id, "issuer_id", &loaded);
ret = cedarling_is_trusted_issuer_loaded_by_iss(
    instance_id,
    "https://issuer.example.org",
    &loaded_by_iss
);
ret = cedarling_total_issuers(instance_id, &total);
ret = cedarling_loaded_trusted_issuers_count(instance_id, &loaded_count);

CedarlingStringArray loaded_ids;
ret = cedarling_loaded_trusted_issuer_ids(instance_id, &loaded_ids);
if (ret == 0) {
    for (size_t i = 0; i < loaded_ids.COUNT; i++) {
        printf("Loaded trusted issuer: %s\n", loaded_ids.ITEMS[i]);
    }
}
cedarling_free_string_array(&loaded_ids);

CedarlingStringArray failed_ids;
ret = cedarling_failed_trusted_issuer_ids(instance_id, &failed_ids);
if (ret == 0) {
    for (size_t i = 0; i < failed_ids.COUNT; i++) {
        printf("Failed trusted issuer: %s\n", failed_ids.ITEMS[i]);
    }
}
cedarling_free_string_array(&failed_ids);
```

### Cleanup

```c
// Shutdown the instance
cedarling_shutdown(instance_id);

// Drop the instance
cedarling_drop(instance_id);

// Global cleanup
cedarling_cleanup();
```

### Error Handling

```c
// Get the last error message
const char* error = cedarling_get_last_error();
if (error) {
    printf("Last error: %s\n", error);
}

// Clear the last error
cedarling_clear_last_error();
```

## API Reference

### Instance Management

| Function | Description |
|----------|-------------|
| `cedarling_init()` | Initialize the library |
| `cedarling_new(config, result)` | Create a new instance from JSON config |
| `cedarling_new_with_env(config, result)` | Create instance with environment variable support |
| `cedarling_drop(instance_id)` | Drop an instance |
| `cedarling_shutdown(instance_id)` | Shutdown an instance |
| `cedarling_cleanup()` | Global cleanup |
| `cedarling_version()` | Get library version |

### Authorization

| Function | Description |
|----------|-------------|
| `cedarling_authorize_unsigned(instance_id, request, result)` | Authorize with custom principals |
| `cedarling_authorize_multi_issuer(instance_id, request, result)` | Authorize with JWT tokens |

### Trusted Issuer Loading Info

| Function | Description |
|----------|-------------|
| `cedarling_is_trusted_issuer_loaded_by_name(instance_id, issuer_id, out_result)` | Check loaded status by issuer ID |
| `cedarling_is_trusted_issuer_loaded_by_iss(instance_id, iss_claim, out_result)` | Check loaded status by issuer `iss` claim |
| `cedarling_total_issuers(instance_id, out_count)` | Get total number of discovered trusted issuers |
| `cedarling_loaded_trusted_issuers_count(instance_id, out_count)` | Get number of successfully loaded trusted issuers |
| `cedarling_loaded_trusted_issuer_ids(instance_id, result)` | Get IDs of successfully loaded trusted issuers |
| `cedarling_failed_trusted_issuer_ids(instance_id, result)` | Get IDs of trusted issuers that failed to load |

### Context Data API

| Function | Description |
|----------|-------------|
| `cedarling_context_push(instance_id, key, value, ttl_secs, result)` | Push data into context with optional TTL (`<=0` means default/no override) |
| `cedarling_context_get(instance_id, key, result)` | Get data by key |
| `cedarling_context_remove(instance_id, key, result)` | Remove data by key |
| `cedarling_context_clear(instance_id, result)` | Clear all data |
| `cedarling_context_list(instance_id, result)` | List all entries |
| `cedarling_context_stats(instance_id, result)` | Get store statistics |

### Logging

| Function | Description |
|----------|-------------|
| `cedarling_pop_logs(instance_id, result)` | Pop all logs |
| `cedarling_get_log_ids(instance_id, result)` | Get all log IDs |
| `cedarling_get_log_by_id(instance_id, log_id, result)` | Get log by ID |
| `cedarling_get_logs_by_tag(instance_id, tag, result)` | Get logs by tag |
| `cedarling_get_logs_by_request_id(instance_id, request_id, result)` | Get logs by request ID |
| `cedarling_get_logs_by_request_id_and_tag(instance_id, request_id, tag, result)` | Get logs by request ID and tag |

### Memory Management

| Function | Description |
|----------|-------------|
| `cedarling_free_string(str)` | Free a string |
| `cedarling_free_string_array(array)` | Free a string array |
| `cedarling_free_result(result)` | Free a result structure |
| `cedarling_free_instance_result(result)` | Free an instance result |

### Error Handling

| Function | Description |
|----------|-------------|
| `cedarling_get_last_error()` | Get last error message |
| `cedarling_clear_last_error()` | Clear last error |

## Configuration

For complete configuration documentation, see [cedarling-properties.md](../../../docs/cedarling/reference/cedarling-properties.md).

### Example Configuration

```json
{
    "CEDARLING_APPLICATION_NAME": "MyApp",
    "CEDARLING_POLICY_STORE_ID": "your-policy-store-id",
    "CEDARLING_LOG_LEVEL": "INFO",
    "CEDARLING_LOG_TYPE": "std_out",
    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.yaml",
    "CEDARLING_JWT_SIG_VALIDATION": "enabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "disabled"
}
```

## Building for Production

- Set `CEDARLING_LOG_LEVEL` to `WARN` or `ERROR`
- Enable JWT validation for production deployments
- Use release builds (`cargo build --release`)