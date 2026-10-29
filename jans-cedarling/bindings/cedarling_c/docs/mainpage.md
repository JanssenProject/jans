# Cedarling C API

Cedarling is a local authorization engine built on the Rust Cedar Engine. The C
bindings expose it as a native library: create an instance from a JSON
bootstrap configuration, send authorization requests as JSON, and read results
and logs back as JSON strings.

Every function is declared in cedarling_c.h. Complete, compilable programs are
listed on the [Examples](examples.html) page.

## Minimal example

```c
#include <stdio.h>
#include "cedarling_c.h"

int main(void) {
    if (cedarling_init() != 0) {
        return 1;
    }

    CedarlingInstanceResult instance;
    if (cedarling_new(bootstrap_config_json, &instance) != 0) {
        fprintf(stderr, "init failed: %s\n", instance.error_message);
        cedarling_free_instance_result(&instance);
        return 1;
    }
    uint64_t id = instance.instance_id;
    cedarling_free_instance_result(&instance);

    CedarlingResult result;
    if (cedarling_authorize_unsigned(id, request_json, &result) == 0) {
        printf("%s\n", (char *)result.data);
    }
    cedarling_free_result(&result);

    cedarling_shutdown(id);
    cedarling_cleanup();
    return 0;
}
```

`bootstrap_config_json` and `request_json` are JSON strings; see
basic_example.c for complete values.

## Memory ownership

Anything the library returns belongs to the caller and must be released with
the matching function: `cedarling_free_string`, `cedarling_free_result`,
`cedarling_free_instance_result` or `cedarling_free_string_array`.

## Errors

Functions return `0` on success. Result structs carry an `error_code`
(`SUCCESS` is `0`) and an `error_message`; branch on `error_code`. For failures
not described by a result struct, `cedarling_get_last_error()` returns a
thread-local message, which must be freed with `cedarling_free_string`.
