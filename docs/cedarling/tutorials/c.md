---
tags:
  - cedarling
  - c
  - getting-started
---

# Getting Started with Cedarling C Bindings

C bindings for the Jans Cedarling authorization engine, providing policy-based access control through a native C API.

## Installation

### Prerequisites

- C compiler (GCC, Clang, or MSVC)
- Rust toolchain
- cbindgen (`cargo install cbindgen`)

### Build from Source

1. Clone the Janssen repository:

    ```sh
    git clone --depth 1 https://github.com/JanssenProject/jans.git
    ```

2. Navigate to the Cedarling C bindings directory:

    ```sh
    cd jans/jans-cedarling/bindings/cedarling_c
    ```

3. Build the Rust library:

    ```sh
    cargo build --release -p cedarling_c
    ```

4. Generate the C header file:

    ```sh
    cbindgen --crate cedarling_c --output target/include/cedarling_c.h
    ```

5. Copy the built artifacts to your project:

    - **Linux**: `cp target/release/libcedarling_c.so .`
    - **macOS**: `cp target/release/libcedarling_c.dylib .`
    - **Windows**: `cp target/release/cedarling_c.dll .` and `cp target/release/cedarling_c.dll.lib cedarling_c.lib`

### Linking Your Application

**Linux/macOS:**

```sh
gcc -o myapp myapp.c -L. -lcedarling_c -Wl,-rpath,.
```

**Windows (MSVC):**

```sh
cl myapp.c cedarling_c.lib
```

**Runtime Notes:**

- **Linux**: Add the library directory to `LD_LIBRARY_PATH`:
  ```sh
  export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH
  ```

- **macOS**: Add the library directory to `DYLD_LIBRARY_PATH`:
  ```sh
  export DYLD_LIBRARY_PATH=$(pwd):$DYLD_LIBRARY_PATH
  ```

- **Windows**: Place `cedarling_c.dll` alongside your executable or in a directory in `PATH`.

## Configuration

Before performing authorization, you need to configure a Cedarling instance. Configuration is provided as a JSON string.

### Basic Configuration

```c
const char* config = "{"
    "\"CEDARLING_APPLICATION_NAME\": \"MyApp\","
    "\"CEDARLING_POLICY_STORE_ID\": \"your-policy-store-id\","
    "\"CEDARLING_LOG_LEVEL\": \"INFO\","
    "\"CEDARLING_LOG_TYPE\": \"std_out\","
    "\"CEDARLING_POLICY_STORE_LOCAL_FN\": \"/path/to/policy-store.yaml\""
"}";
```

### Configuration Properties

| Property | Description |
|----------|-------------|
| `CEDARLING_APPLICATION_NAME` | Name of your application |
| `CEDARLING_POLICY_STORE_ID` | ID of the policy store |
| `CEDARLING_LOG_LEVEL` | Logging level (DEBUG, INFO, WARN, ERROR) |
| `CEDARLING_LOG_TYPE` | Log output type (std_out, memory, off) |
| `CEDARLING_POLICY_STORE_LOCAL_FN` | Path to local policy store file |
| `CEDARLING_JWT_SIG_VALIDATION` | Enable/disable JWT signature validation |
| `CEDARLING_JWT_STATUS_VALIDATION` | Enable/disable JWT status validation |

For complete configuration documentation, see [Configuration Properties](../reference/cedarling-properties.md).

## Initialize Cedarling

```c
#include <stdio.h>
#include <stdint.h>
#include "cedarling_c.h"

int main() {
    // Initialize the library
    if (cedarling_init() != 0) {
        printf("Failed to initialize Cedarling library\n");
        return 1;
    }

    // Print version
    printf("Cedarling version: %s\n", cedarling_version());

    // Configuration JSON
    const char* config = "{"
        "\"CEDARLING_APPLICATION_NAME\": \"MyApp\","
        "\"CEDARLING_POLICY_STORE_ID\": \"example-policy-store\","
        "\"CEDARLING_LOG_LEVEL\": \"INFO\","
        "\"CEDARLING_LOG_TYPE\": \"std_out\","
        "\"CEDARLING_POLICY_STORE_LOCAL_FN\": \"./policy-store.yaml\""
    "}";

    // Create a new instance
    CedarlingInstanceResult instance_result;
    int ret = cedarling_new(config, &instance_result);
    
    if (ret != 0) {
        printf("Failed to create instance: %s\n", instance_result.error_message);
        cedarling_free_instance_result(&instance_result);
        return 1;
    }

    uint64_t instance_id = instance_result.instance_id;
    printf("Instance created with ID: %llu\n", (unsigned long long)instance_id);
    cedarling_free_instance_result(&instance_result);

    // Use instance for authorization...

    // Cleanup
    cedarling_shutdown(instance_id);
    cedarling_drop(instance_id);
    // Shuts down all library-held instances and clears this thread's last error
    cedarling_cleanup();

    return 0;
}
```

## Authorization

Cedarling provides two main authorization interfaces:

- **Multi-Issuer Authorization**: Processes JWT tokens from multiple issuers. Token data is mapped to Cedar entities based on `token_metadata` configuration.
- **Unsigned Authorization**: Allows you to pass principals directly, bypassing tokens entirely. Useful for internal data or when tokens are not available.

### Multi-Issuer Authorization

Use `cedarling_authorize_multi_issuer` when authorizing with JWT tokens:

**1. Define the request JSON:**

```c
const char* request = "{"
    "\"tokens\": ["
    "    {\"mapping\": \"Jans::Access_token\", \"payload\": \"eyJhbGciOiJIUzI1NiIs...\"},"
    "    {\"mapping\": \"Jans::Id_token\", \"payload\": \"eyJhbGciOiJIUzI1NiIs...\"}"
    "],"
    "\"action\": \"Jans::Action::\\\"Update\\\"\","
    "\"resource\": {"
    "    \"cedar_entity_mapping\": {\"entity_type\": \"Jans::Issue\", \"id\": \"random_id\"},"
    "    \"org_id\": \"some_long_id\""
    "},"
    "\"context\": {}"
"}";
```

**2. Authorize:**

```c
CedarlingResult auth_result;
int ret = cedarling_authorize_multi_issuer(instance_id, request, &auth_result);

if (ret == 0) {
    printf("Authorization result: %s\n", (char*)auth_result.data);
    // Parse JSON result to check decision
} else {
    printf("Authorization error: %s\n", auth_result.error_message);
}
cedarling_free_result(&auth_result);
```

See [Multi-Issuer Authorization](../reference/cedarling-multi-issuer.md) for more details.

### Unsigned Authorization

Use `cedarling_authorize_unsigned` when you have custom principals (not derived from JWTs):

**1. Define the request JSON with principals:**

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
```

**2. Authorize:**

```c
CedarlingResult auth_result;
int ret = cedarling_authorize_unsigned(instance_id, request, &auth_result);

if (ret == 0) {
    char* result_str = (char*)auth_result.data;
    printf("Authorization result: %s\n", result_str);
    
    // Check if decision is true
    if (strstr(result_str, "\"decision\":true") != NULL) {
        printf("Access granted\n");
    } else {
        printf("Access denied\n");
    }
} else {
    printf("Authorization error: %s\n", auth_result.error_message);
}
cedarling_free_result(&auth_result);
```

## Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace.

### Push Data

Store data in the context for use in policy evaluation:

TTL is given in seconds; use `<= 0` when you do not want to override the store default.

```c
const char* value = "{\"role\": [\"admin\", \"editor\"], \"level\": 5}";
CedarlingResult push_result;
int ret = cedarling_context_push(instance_id, "user:123", value, 300, &push_result);
if (ret != 0) {
    printf("Error pushing data: %s\n", push_result.error_message);
}
cedarling_free_result(&push_result);
```

### Get Data

Retrieve stored data by key:

```c
CedarlingResult get_result;
int ret = cedarling_context_get(instance_id, "user:123", &get_result);
if (ret == 0) {
    char* value = (char*)get_result.data;
    if (strcmp(value, "null") != 0) {
        printf("Value: %s\n", value);
    } else {
        printf("Key not found\n");
    }
}
cedarling_free_result(&get_result);
```

### Remove Data

Remove a specific entry:

```c
CedarlingResult remove_result;
int ret = cedarling_context_remove(instance_id, "user:123", &remove_result);
if (ret == 0) {
    printf("Remove result: %s\n", (char*)remove_result.data);  // {"removed": true/false}
}
cedarling_free_result(&remove_result);
```

### Clear All Data

Remove all entries from the data store:

```c
CedarlingResult clear_result;
int ret = cedarling_context_clear(instance_id, &clear_result);
cedarling_free_result(&clear_result);
```

### List All Entries

List all entries with their metadata:

```c
CedarlingResult list_result;
int ret = cedarling_context_list(instance_id, &list_result);
if (ret == 0) {
    printf("Entries: %s\n", (char*)list_result.data);  // JSON array
}
cedarling_free_result(&list_result);
```

### Get Statistics

Get statistics about the data store:

```c
CedarlingResult stats_result;
int ret = cedarling_context_stats(instance_id, &stats_result);
if (ret == 0) {
    printf("Stats: %s\n", (char*)stats_result.data);
}
cedarling_free_result(&stats_result);
```

### Using Data in Cedar Policies

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace:

```cedar
permit(
    principal,
    action == Jans::Action::"read",
    resource
) when {
    context.data["user:123"].role.contains("admin")
};
```

## Logging

Retrieve logs stored in memory:

```c
// Get all logs and clear the buffer
CedarlingStringArray logs;
int ret = cedarling_pop_logs(instance_id, &logs);
if (ret == 0) {
    printf("Retrieved %zu logs\n", logs.count);
    for (size_t i = 0; i < logs.count; i++) {
        printf("Log %zu: %s\n", i + 1, logs.items[i]);
    }
    cedarling_free_string_array(&logs);
}

// Get all log IDs
CedarlingStringArray log_ids;
ret = cedarling_get_log_ids(instance_id, &log_ids);
if (ret == 0) {
    printf("Found %zu log IDs\n", log_ids.count);
    cedarling_free_string_array(&log_ids);
}

// Get logs by tag
CedarlingStringArray tagged_logs;
ret = cedarling_get_logs_by_tag(instance_id, "authorization", &tagged_logs);
cedarling_free_string_array(&tagged_logs);

// Get logs by request ID
CedarlingStringArray request_logs;
ret = cedarling_get_logs_by_request_id(instance_id, "req-123", &request_logs);
cedarling_free_string_array(&request_logs);

// Get a specific log by ID
CedarlingResult log_result;
ret = cedarling_get_log_by_id(instance_id, "log-id-here", &log_result);
if (ret == 0) {
    printf("Log: %s\n", (char*)log_result.data);
} else if (log_result.error_code == KEY_NOT_FOUND) {
    /* Log id does not exist (instance was valid) */
}
cedarling_free_result(&log_result);
```

## Error Handling

All functions return an error code (0 for success, non-zero for failure). Additional error information is available through:

```c
// Last error string is owned by the caller — free with cedarling_free_string
char* error = cedarling_get_last_error();
if (error) {
    printf("Last error: %s\n", error);
    cedarling_free_string(error);
}

// Clear the last error
cedarling_clear_last_error();
```

### Error Codes

Values match the `CedarlingErrorCode` enum in `cedarling_c.h` (names are `SCREAMING_SNAKE_CASE` in C).

| Code | Name | Description |
|------|------|-------------|
| 0 | Success | Operation completed successfully |
| 1 | InvalidArgument | Invalid argument provided |
| 2 | InstanceNotFound | Instance not found |
| 3 | JsonError | JSON parsing error |
| 4 | AuthorizationError | Authorization error |
| 5 | ConfigurationError | Configuration error |
| 6 | Internal | Internal error |
| 7 | KeyNotFound | Requested key or id does not exist (e.g. unknown log id) |

## Memory Management

- **Do not free** the pointer from `cedarling_version()`: it points at static data for the library version string.
- **Must free** the pointer from `cedarling_get_last_error()` with `cedarling_free_string()` when non-null (each call returns a fresh copy).

Free struct payloads with the matching API after use:

```c
// Owned CedarlingResult
cedarling_free_result(&result);

// Owned CedarlingInstanceResult
cedarling_free_instance_result(&instance_result);

// Owned CedarlingStringArray
cedarling_free_string_array(&array);

// Owned standalone C string (e.g. from cedarling_get_last_error)
cedarling_free_string(str);
```

## Complete Example

```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "cedarling_c.h"

int main() {
    // Initialize
    if (cedarling_init() != 0) {
        printf("Failed to initialize\n");
        return 1;
    }

    printf("Cedarling version: %s\n", cedarling_version());

    // Configuration
    const char* config = "{"
        "\"CEDARLING_APPLICATION_NAME\": \"ExampleApp\","
        "\"CEDARLING_POLICY_STORE_ID\": \"example-store\","
        "\"CEDARLING_LOG_LEVEL\": \"DEBUG\","
        "\"CEDARLING_LOG_TYPE\": \"memory\","
        "\"CEDARLING_POLICY_STORE_LOCAL_FN\": \"./policy-store.yaml\""
    "}";

    // Create instance
    CedarlingInstanceResult instance_result;
    if (cedarling_new(config, &instance_result) != 0) {
        printf("Error: %s\n", instance_result.error_message);
        cedarling_free_instance_result(&instance_result);
        return 1;
    }
    
    uint64_t instance_id = instance_result.instance_id;
    cedarling_free_instance_result(&instance_result);

    // Push context data
    CedarlingResult push_result;
    cedarling_context_push(instance_id, "app:config",
        "{\"feature_flags\": {\"premium\": true}}", 0, &push_result);
    cedarling_free_result(&push_result);

    // Authorize
    const char* request = "{"
        "\"principals\": [{"
        "    \"cedar_entity_mapping\": {\"entity_type\": \"Jans::TestPrincipal1\", \"id\": \"user1\"},"
        "    \"is_ok\": true"
        "}],"
        "\"action\": \"Jans::Action::\\\"UpdateForTestPrincipals\\\"\","
        "\"resource\": {\"cedar_entity_mapping\": {\"entity_type\": \"Jans::Issue\", \"id\": \"res1\"}, \"org_id\": \"org1\", \"country\": \"US\"},"
        "\"context\": {}"
    "}";

    CedarlingResult auth_result;
    if (cedarling_authorize_unsigned(instance_id, request, &auth_result) == 0) {
        printf("Authorization: %s\n", (char*)auth_result.data);
    } else {
        printf("Authorization error: %s\n", auth_result.error_message);
    }
    cedarling_free_result(&auth_result);

    // Get logs
    CedarlingStringArray logs;
    if (cedarling_pop_logs(instance_id, &logs) == 0) {
        printf("Logs: %zu entries\n", logs.count);
        cedarling_free_string_array(&logs);
    }

    // Cleanup
    cedarling_shutdown(instance_id);
    cedarling_drop(instance_id);
    cedarling_cleanup();  /* all instances + this thread's last error */

    printf("Done!\n");
    return 0;
}
```

## Thread Safety

All functions in the Cedarling C library are thread-safe. Multiple threads can safely use the same Cedarling instance concurrently.

## Next Steps

- Explore [Cedar Policy Language](../reference/cedarling-policy-store.md) for writing authorization policies
- Learn about [Multi-Issuer Authorization](../reference/cedarling-multi-issuer.md) for JWT-based authorization
- Review [Configuration Properties](../reference/cedarling-properties.md) for all available options

