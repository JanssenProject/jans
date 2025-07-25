---
tags:
  - cedarling
  - c
  - getting-started
---

# Getting Started with Cedarling C Bindings

This guide explains how to use the Cedarling C bindings for the Jans Cedarling authorization engine, providing policy-based access control in C applications.

- [Requirements](#requirements)
- [Building](#building)
- [Including in Projects](#including-in-projects)
- [API Reference](#api-reference)
- [Code Example](#code-example)
- [Testing](#testing)
- [Extra Information](#extra-information)

## Requirements

- **C Compiler:** GCC or Clang (C99 or later recommended)
- **Rust Toolchain:** Required to build the Cedarling core and C bindings
- **GNU Make:** For building examples and tests
- **Linux, macOS, or Windows** (tested primarily on Linux)
- **Dependencies:**
  - `libdl`, `libpthread`, `libm` (standard on most Unix systems)
  - [cbindgen](https://github.com/mozilla/cbindgen) (for header generation, optional unless modifying the API)

## Building

### 1. Clone the Repository

```sh
git clone https://github.com/JanssenProject/jans.git
cd jans/jans-cedarling/bindings/cedarling_c
```

### 2. Build the Rust Library and C Bindings

Build the dynamic library and generate the C header:

```sh
cargo build --release
```

This produces:

- `target/release/libcedarling_c.so` (Linux)
- `target/release/cedarling_c.dll` (Windows)
- `target/release/libcedarling_c.dylib` (macOS)
- `target/include/cedarling_c.h` (C header, auto-generated)

### 3. Build and Run the Example

Navigate to the `examples` directory and build the example:

```sh
cd examples
make
make run
```

This compiles and runs `basic_example`, demonstrating initialization, authorization, logging, and cleanup.

### 4. Build and Run the Tests

Navigate to the `tests` directory and run:

```sh
cd ../tests
make
make run
```

This will build and execute a comprehensive test suite for the C bindings.

## Including in Projects

### 1. Link Against the Library

- Copy `libcedarling_c.so` (or `.dll`/`.dylib`) and `cedarling_c.h` to your project.
- Add the include path and link flags to your build system:

Example (GCC):

```sh
gcc -I/path/to/include -L/path/to/lib -lcedarling_c -ldl -lpthread -lm your_app.c -o your_app
```

- On Linux, set `LD_LIBRARY_PATH` to the directory containing `libcedarling_c.so` at runtime:

```sh
export LD_LIBRARY_PATH=/path/to/lib:$LD_LIBRARY_PATH
```

### 2. Include the Header

```c
#include "cedarling_c.h"
```

### 3. Distribute the Library

- Ship the shared library (`.so`, `.dll`, `.dylib`) and header with your application.
- Ensure runtime library path is set or the library is in a standard location.

## API Reference

The C API exposes functions for:

- Library initialization and cleanup
- Creating and destroying Cedarling instances
- Performing authorization (token-based and unsigned)
- Retrieving logs and log metadata
- Error handling and memory management

**Key Types:**

- `CedarlingInstanceResult` — result of instance creation
- `CedarlingResult` — result of authorization and log queries
- `CedarlingStringArray` — array of strings (logs, log IDs)
- `CedarlingErrorCode` — error codes

**Key Functions:**

- `int cedarling_init(void);`
- `int cedarling_new(const char* config_json, CedarlingInstanceResult* result);`
- `int cedarling_authorize(uint64_t instance_id, const char* request_json, CedarlingResult* result);`
- `int cedarling_pop_logs(uint64_t instance_id, CedarlingStringArray* result);`
- `void cedarling_free_result(CedarlingResult* result);`
- `void cedarling_free_string_array(CedarlingStringArray* array);`
- `const char* cedarling_get_last_error(void);`
- ...and more (see `cedarling_c.h` for the full list)

## Code Example

Below is a minimal example demonstrating initialization, authorization, log retrieval, and cleanup. See `examples/basic_example.c` for a full-featured version.

```c
#include <stdio.h>
#include <stdint.h>
#include "cedarling_c.h"

const char* CONFIG = "{\n"
    "  \"CEDARLING_APPLICATION_NAME\": \"MyApp\",\n"
    "  \"CEDARLING_POLICY_STORE_ID\": \"your-policy-store-id\",\n"
    "  ... (other config fields) ...\n"
    "}";

const char* REQUEST = "{ ... }"; // See example for full request JSON

int main() {
    // Initialize library
    if (cedarling_init() != 0) {
        printf("Failed to initialize Cedarling\n");
        return 1;
    }

    // Create instance
    CedarlingInstanceResult instance_result;
    if (cedarling_new(CONFIG, &instance_result) != 0) {
        printf("Instance creation failed: %s\n", instance_result.ERROR_MESSAGE);
        return 1;
    }
    uint64_t instance_id = instance_result.INSTANCE_ID;
    cedarling_free_instance_result(&instance_result);

    // Authorize
    CedarlingResult auth_result;
    if (cedarling_authorize(instance_id, REQUEST, &auth_result) == 0) {
        printf("Authorization result: %s\n", (char*)auth_result.DATA);
        cedarling_free_result(&auth_result);
    } else {
        printf("Authorization failed: %s\n", auth_result.ERROR_MESSAGE);
        cedarling_free_result(&auth_result);
    }

    // Retrieve logs
    CedarlingStringArray logs;
    if (cedarling_pop_logs(instance_id, &logs) == 0) {
        for (size_t i = 0; i < logs.COUNT; ++i) {
            printf("Log %zu: %s\n", i + 1, logs.ITEMS[i]);
        }
        cedarling_free_string_array(&logs);
    }

    // Shutdown and cleanup
    cedarling_shutdown(instance_id);
    cedarling_drop(instance_id);
    cedarling_cleanup();
    return 0;
}
```

## Testing

A comprehensive test suite is provided in `tests/test_cedarling.c`. To run:

```sh
cd tests
make
make run
```

This will execute tests for initialization, instance management, authorization, logging, error handling, and memory management.

## Extra Information

- **Header Generation:** The C header (`cedarling_c.h`) is auto-generated using [cbindgen](https://github.com/mozilla/cbindgen). If you change the Rust API, regenerate the header with:
  ```sh
  cargo build --release
  ```
- **Thread Safety:** Each Cedarling instance is independent and thread-safe. The library uses an internal Tokio runtime for async operations.
- **Error Handling:** Most functions return an error code. Use `cedarling_get_last_error()` for a human-readable error message. Always free result/error strings and arrays using the provided `free` functions.
- **Memory Management:** All strings and arrays returned by the library must be freed using the appropriate `cedarling_free_*` functions to avoid memory leaks.
- **API Reference:** See the generated `cedarling_c.h` for the full API, types, and documentation comments.
- **Advanced Usage:**
  - Unsigned authorization: use `cedarling_authorize_unsigned()`
  - Log filtering: use `cedarling_get_logs_by_tag()`, `cedarling_get_logs_by_request_id()`, etc.
  - Environment-based config: use `cedarling_new_with_env()`

---

## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)
- [Cedarling Rust Bindings](./rust.md)
- [Cedarling Python Bindings](./python.md)
- [Cedarling Go Bindings](./go.md)
- [Cedarling Java Bindings](./java.md)
- [Cedarling JavaScript Bindings](./javascript.md)
- [Cedarling Swift Bindings](./swift.md)
