// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![cfg(not(target_arch = "wasm32"))]

use std::os::raw::{c_char, c_int};
use std::ptr;

mod c_interface;
mod types;

use c_interface::*;
use types::*;

/// Prevent Rust panics from crossing the C ABI (undefined behavior for the C caller).
macro_rules! ffi_guard_int {
    ($body:block) => {
        match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> c_int { $body })) {
            Ok(v) => v,
            Err(_) => {
                set_last_error("internal panic caught at FFI boundary");
                CedarlingErrorCode::Internal as c_int
            },
        }
    };
}

macro_rules! ffi_guard_void {
    ($body:block) => {
        if std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body)).is_err() {
            set_last_error("internal panic caught at FFI boundary");
        }
    };
}

macro_rules! ffi_guard_ptr_mut {
    ($body:block) => {
        match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> *mut c_char { $body })) {
            Ok(v) => v,
            Err(_) => {
                set_last_error("internal panic caught at FFI boundary");
                ptr::null_mut()
            },
        }
    };
}

macro_rules! ffi_guard_ptr_const {
    ($body:block) => {
        match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> *const c_char { $body }))
        {
            Ok(v) => v,
            Err(_) => {
                set_last_error("internal panic caught at FFI boundary");
                ptr::null()
            },
        }
    };
}

/// Initialize the Cedarling runtime.
///
/// Call before other library functions (per your process/thread model). This entry point has no
/// pointer parameters and is safe to call from C.
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_init() -> c_int {
    ffi_guard_int!({ initialize_runtime() as c_int })
}
/// Create a new Cedarling instance from JSON bootstrap configuration.
///
/// # Safety
///
/// - `config_json` must be a valid pointer to a NUL-terminated UTF-8 C string.
/// - `result` must point to writable, properly aligned memory for [`CedarlingInstanceResult`].
/// - After reading the output, call [`cedarling_free_instance_result`] (including when `error_message` is set).
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_new(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if config_json.is_null() {
            unsafe {
                *result = CedarlingInstanceResult::error(
                    CedarlingErrorCode::InvalidArgument,
                    "null config_json pointer",
                );
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let config_str = match c_string_to_string(config_json) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingInstanceResult::error(code, "Invalid config JSON string");
                return code as c_int;
            },
        };

        let instance_result = create_instance(&config_str);
        unsafe { *result = instance_result };
        unsafe { (*result).error_code as c_int }
    })
}
/// Create a new Cedarling instance, merging JSON config with environment-variable overrides.
///
/// # Safety
///
/// - `config_json` may be null; if non-null, it must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable, properly aligned memory for [`CedarlingInstanceResult`].
/// - After reading the output, call [`cedarling_free_instance_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_new_with_env(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let config_str = if config_json.is_null() {
            None
        } else {
            match c_string_to_string(config_json) {
                Ok(s) => Some(s),
                Err(code) => unsafe {
                    *result = CedarlingInstanceResult::error(code, "Invalid config JSON string");
                    return code as c_int;
                },
            }
        };
        let instance_result = create_instance_with_env(config_str.as_deref());
        unsafe { *result = instance_result };
        unsafe { (*result).error_code as c_int }
    })
}
/// Remove an instance from the runtime registry (no graceful shutdown).
///
/// # Safety
///
/// No raw pointers are passed. `instance_id` is validated by the library; unknown IDs return an error.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_drop(instance_id: u64) -> c_int {
    ffi_guard_int!({ drop_instance(instance_id) as c_int })
}
/// Authorize an unsigned request from JSON.
///
/// # Safety
///
/// - `request_json` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_authorize_unsigned(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if request_json.is_null() {
            unsafe {
                *result = CedarlingResult::error(
                    CedarlingErrorCode::InvalidArgument,
                    "null request_json pointer",
                );
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let request_str = match c_string_to_string(request_json) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid request JSON string");
                return code as c_int;
            },
        };

        let auth_result = authorize_unsigned(instance_id, &request_str);
        unsafe { *result = auth_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Authorize a multi-issuer request (JSON body).
///
/// # Safety
///
/// - `request_json` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_authorize_multi_issuer(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if request_json.is_null() {
            unsafe {
                *result = CedarlingResult::error(
                    CedarlingErrorCode::InvalidArgument,
                    "null request_json pointer",
                );
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let request_str = match c_string_to_string(request_json) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid request JSON string");
                return code as c_int;
            },
        };

        let auth_result = authorize_multi_issuer(instance_id, &request_str);
        unsafe { *result = auth_result };
        unsafe { (*result).error_code as c_int }
    })
}

// Context Data API functions

/// Push context data (JSON value) under `key` with optional TTL in seconds.
///
/// # Safety
///
/// - `key` and `value_json` must be valid NUL-terminated UTF-8 C strings.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_push(
    instance_id: u64,
    key: *const c_char,
    value_json: *const c_char,
    ttl_secs: i64,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if key.is_null() {
            unsafe {
                *result =
                    CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if value_json.is_null() {
            unsafe {
                *result = CedarlingResult::error(
                    CedarlingErrorCode::InvalidArgument,
                    "null value_json pointer",
                );
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let key_str = match c_string_to_string(key) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid key string");
                return code as c_int;
            },
        };

        let value_str = match c_string_to_string(value_json) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid value JSON string");
                return code as c_int;
            },
        };

        let push_result = context_push(instance_id, &key_str, &value_str, ttl_secs);
        unsafe { *result = push_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Get context data (JSON value only) by key.
///
/// On success, `data` is the JSON text for the value, or the JSON literal `null` when the key is
/// absent **or** when the stored value is JSON null—those two cases are not distinguished here.
/// Use [`cedarling_context_get_entry`] to tell them apart (missing keys yield no entry object).
///
/// # Safety
///
/// - `key` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_get(
    instance_id: u64,
    key: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if key.is_null() {
            unsafe {
                *result =
                    CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let key_str = match c_string_to_string(key) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid key string");
                return code as c_int;
            },
        };

        let get_result = context_get(instance_id, &key_str);
        unsafe { *result = get_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Get one context data entry (value plus metadata: `key`, `data_type`, timestamps, `access_count`) as JSON.
///
/// # Safety
///
/// - `key` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_get_entry(
    instance_id: u64,
    key: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if key.is_null() {
            unsafe {
                *result =
                    CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let key_str = match c_string_to_string(key) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid key string");
                return code as c_int;
            },
        };

        let entry_result = context_get_entry(instance_id, &key_str);
        unsafe { *result = entry_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Remove context data by key.
///
/// # Safety
///
/// - `key` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_remove(
    instance_id: u64,
    key: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if key.is_null() {
            unsafe {
                *result =
                    CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let key_str = match c_string_to_string(key) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid key string");
                return code as c_int;
            },
        };

        let remove_result = context_remove(instance_id, &key_str);
        unsafe { *result = remove_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Clear all context data for the instance.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_clear(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let clear_result = context_clear(instance_id);
        unsafe { *result = clear_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// List all context entries (JSON array of entries with metadata).
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_list(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let list_result = context_list(instance_id);
        unsafe { *result = list_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Get context store statistics as JSON.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_stats(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let stats_result = context_stats(instance_id);
        unsafe { *result = stats_result };
        unsafe { (*result).error_code as c_int }
    })
}

/// Pop all logs from an instance into a string array.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingStringArray`] storage.
/// - On success, free the array (and nested strings) once with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_pop_logs(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        if result.is_null() {
            clear_last_error();
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        clear_last_error();
        match pop_logs(instance_id) {
            Ok(logs) => unsafe {
                *result = logs;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}
/// Get a single log entry by ID (JSON in the result's `data` field).
///
/// # Safety
///
/// - `log_id` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingResult`] storage; release with [`cedarling_free_result`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_log_by_id(
    instance_id: u64,
    log_id: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if log_id.is_null() {
            unsafe {
                *result = CedarlingResult::error(
                    CedarlingErrorCode::InvalidArgument,
                    "null log_id pointer",
                );
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let id_str = match c_string_to_string(log_id) {
            Ok(s) => s,
            Err(code) => unsafe {
                *result = CedarlingResult::error(code, "Invalid log_id C string");
                return code as c_int;
            },
        };

        let log_result = get_log_by_id(instance_id, &id_str);

        unsafe { *result = log_result };
        unsafe { (*result).error_code as c_int }
    })
}
/// Get all log IDs as a string array.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingStringArray`] storage.
/// - On success, free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_log_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        if result.is_null() {
            clear_last_error();
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        clear_last_error();
        match get_log_ids(instance_id) {
            Ok(log_ids) => unsafe {
                *result = log_ids;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}
/// Get logs matching `tag` as a string array of JSON lines.
///
/// # Safety
///
/// - `tag` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingStringArray`] storage; on success free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_tag(
    instance_id: u64,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if tag.is_null() {
            set_last_error("null tag pointer");
            unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let tag_str = match c_string_to_string(tag) {
            Ok(s) => s,
            Err(_) => unsafe {
                set_last_error("Invalid tag string");
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };

        match get_logs_by_tag(instance_id, &tag_str) {
            Ok(logs) => unsafe {
                *result = logs;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}
/// Get logs for a request ID as a string array of JSON lines.
///
/// # Safety
///
/// - `request_id` must be a valid NUL-terminated UTF-8 C string.
/// - `result` must point to writable [`CedarlingStringArray`] storage; on success free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id(
    instance_id: u64,
    request_id: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if request_id.is_null() {
            set_last_error("null request_id pointer");
            unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let request_id_str = match c_string_to_string(request_id) {
            Ok(s) => s,
            Err(_) => unsafe {
                set_last_error("Invalid request_id string");
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };

        match get_logs_by_request_id(instance_id, &request_id_str) {
            Ok(logs) => unsafe {
                *result = logs;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}
/// Get logs for a request ID and tag as a string array of JSON lines.
///
/// # Safety
///
/// - `request_id` and `tag` must be valid NUL-terminated UTF-8 C strings.
/// - `result` must point to writable [`CedarlingStringArray`] storage; on success free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id_and_tag(
    instance_id: u64,
    request_id: *const c_char,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if result.is_null() {
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if request_id.is_null() {
            set_last_error("null request_id pointer");
            unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if tag.is_null() {
            set_last_error("null tag pointer");
            unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
            }
            return CedarlingErrorCode::InvalidArgument as c_int;
        }

        let request_id_str = match c_string_to_string(request_id) {
            Ok(s) => s,
            Err(_) => unsafe {
                set_last_error("Invalid request_id string");
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };

        let tag_str = match c_string_to_string(tag) {
            Ok(s) => s,
            Err(_) => unsafe {
                set_last_error("Invalid tag string");
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };

        match get_logs_by_request_id_and_tag(instance_id, &request_id_str, &tag_str) {
            Ok(logs) => unsafe {
                *result = logs;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}

/// Returns whether a trusted issuer was loaded, keyed by issuer identifier.
///
/// # Safety
///
/// - `issuer_id` must be a valid NUL-terminated UTF-8 C string.
/// - `out_result` must point to writable `bool` storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_is_trusted_issuer_loaded_by_name(
    instance_id: u64,
    issuer_id: *const c_char,
    out_result: *mut bool,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if out_result.is_null() {
            set_last_error("null out_result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if issuer_id.is_null() {
            set_last_error("null issuer_id");
            unsafe { *out_result = false };
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        let issuer_id_str = match c_string_to_string(issuer_id) {
            Ok(s) => s,
            Err(_) => {
                set_last_error("invalid issuer_id C string");
                unsafe { *out_result = false };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };
        match is_trusted_issuer_loaded_by_name(instance_id, &issuer_id_str) {
            Ok(loaded) => unsafe {
                *out_result = loaded;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *out_result = false;
                code as c_int
            },
        }
    })
}

/// Returns whether a trusted issuer was loaded for the given JWT `iss` claim value.
///
/// # Safety
///
/// - `iss_claim` must be a valid NUL-terminated UTF-8 C string.
/// - `out_result` must point to writable `bool` storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_is_trusted_issuer_loaded_by_iss(
    instance_id: u64,
    iss_claim: *const c_char,
    out_result: *mut bool,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if out_result.is_null() {
            set_last_error("null out_result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        if iss_claim.is_null() {
            set_last_error("null iss_claim");
            unsafe { *out_result = false };
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        let iss_claim_str = match c_string_to_string(iss_claim) {
            Ok(s) => s,
            Err(_) => {
                set_last_error("invalid iss_claim C string");
                unsafe { *out_result = false };
                return CedarlingErrorCode::InvalidArgument as c_int;
            },
        };
        match is_trusted_issuer_loaded_by_iss(instance_id, &iss_claim_str) {
            Ok(loaded) => unsafe {
                *out_result = loaded;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *out_result = false;
                code as c_int
            },
        }
    })
}

/// Write the total number of discovered trusted issuers into `*out_count`.
///
/// # Safety
///
/// - `out_count` must point to writable `usize` storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_total_issuers(instance_id: u64, out_count: *mut usize) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if out_count.is_null() {
            set_last_error("null out_count pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        match total_issuers(instance_id) {
            Ok(count) => unsafe {
                *out_count = count;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *out_count = 0;
                code as c_int
            },
        }
    })
}

/// Write the number of successfully loaded trusted issuers into `*out_count`.
///
/// # Safety
///
/// - `out_count` must point to writable `usize` storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_loaded_trusted_issuers_count(
    instance_id: u64,
    out_count: *mut usize,
) -> c_int {
    ffi_guard_int!({
        clear_last_error();
        if out_count.is_null() {
            set_last_error("null out_count pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        match loaded_trusted_issuers_count(instance_id) {
            Ok(count) => unsafe {
                *out_count = count;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *out_count = 0;
                code as c_int
            },
        }
    })
}

/// List issuer IDs that loaded successfully.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingStringArray`] storage.
/// - On success, free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_loaded_trusted_issuer_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        if result.is_null() {
            clear_last_error();
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        clear_last_error();
        match loaded_trusted_issuer_ids(instance_id) {
            Ok(ids) => unsafe {
                *result = ids;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}

/// List issuer IDs that failed to load.
///
/// # Safety
///
/// - `result` must point to writable [`CedarlingStringArray`] storage.
/// - On success, free with [`cedarling_free_string_array`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_failed_trusted_issuer_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    ffi_guard_int!({
        if result.is_null() {
            clear_last_error();
            set_last_error("null result pointer");
            return CedarlingErrorCode::InvalidArgument as c_int;
        }
        clear_last_error();
        match failed_trusted_issuer_ids(instance_id) {
            Ok(ids) => unsafe {
                *result = ids;
                CedarlingErrorCode::Success as c_int
            },
            Err(code) => unsafe {
                *result = CedarlingStringArray {
                    items: ptr::null_mut(),
                    count: 0,
                };
                code as c_int
            },
        }
    })
}

/// Gracefully shut down an instance (flush resources, then remove from registry).
///
/// # Safety
///
/// No raw pointers are passed. `instance_id` is validated by the library.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_shutdown(instance_id: u64) -> c_int {
    ffi_guard_int!({ shutdown_instance(instance_id) as c_int })
}
/// Free a heap-allocated C string returned by this library (for example from [`cedarling_get_last_error`]).
///
/// # Safety
///
/// - `str_ptr` may be null (no-op).
/// - If non-null, `str_ptr` must be a pointer previously returned by this library as an owned string, and must not be freed twice.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_string(str_ptr: *mut c_char) {
    ffi_guard_void!({
        if !str_ptr.is_null() {
            unsafe {
                // Convert the raw pointer back to a CString and drop it
                let _ = std::ffi::CString::from_raw(str_ptr);
            }
        }
    })
}
/// Free a [`CedarlingStringArray`] produced on the success path of log or issuer-ID functions.
///
/// # Safety
///
/// - `array` may be null (no-op).
/// - If non-null, `array` must point to a struct exactly as filled by this library (including `items`/`count`), and must not be freed twice.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_string_array(array: *mut CedarlingStringArray) {
    ffi_guard_void!({
        if array.is_null() {
            return;
        }

        unsafe {
            let array_ref = &mut *array;
            if !array_ref.items.is_null() {
                for i in 0..array_ref.count {
                    let item_ptr = *array_ref.items.add(i);
                    if !item_ptr.is_null() {
                        // Convert the raw pointer back to a CString and drop it
                        let _ = std::ffi::CString::from_raw(item_ptr);
                    }
                }
                let slice_ptr = ptr::slice_from_raw_parts_mut(array_ref.items, array_ref.count);
                drop(Box::from_raw(slice_ptr));
            }
            // Clear the struct to prevent double-free
            array_ref.items = ptr::null_mut();
            array_ref.count = 0;
        }
    })
}

/// Release `data` and `error_message` inside a [`CedarlingResult`] filled by this library.
///
/// # Safety
///
/// - `result` may be null (no-op).
/// - If non-null, `result` must point to a [`CedarlingResult`] whose pointer fields were produced by this library (or are null), and this function must not run twice on the same populated struct without reinitialization.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_result(result: *mut CedarlingResult) {
    ffi_guard_void!({
        if result.is_null() {
            return;
        }

        unsafe {
            let result_ref = &mut *result;
            if !result_ref.data.is_null() {
                // Convert the raw pointer back to a CString and drop it
                let _ = std::ffi::CString::from_raw(result_ref.data);
                result_ref.data = ptr::null_mut();
            }
            if !result_ref.error_message.is_null() {
                // Convert the raw pointer back to a CString and drop it
                let _ = std::ffi::CString::from_raw(result_ref.error_message);
                result_ref.error_message = ptr::null_mut();
            }
        }
    })
}

/// Release `error_message` inside a [`CedarlingInstanceResult`] filled by this library.
///
/// # Safety
///
/// - `result` may be null (no-op).
/// - If non-null, `error_message` must either be null or a pointer produced by this library; do not double-free.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_instance_result(result: *mut CedarlingInstanceResult) {
    ffi_guard_void!({
        if result.is_null() {
            return;
        }

        unsafe {
            let result_ref = &mut *result;
            if !result_ref.error_message.is_null() {
                // Convert the raw pointer back to a CString and drop it
                let _ = std::ffi::CString::from_raw(result_ref.error_message);
                result_ref.error_message = ptr::null_mut();
            }
            result_ref.instance_id = 0;
        }
    })
}

/// Returns a thread-local auxiliary error string for failures that are **not** already described in
/// an out-parameter (for example [`CedarlingResult`] / [`CedarlingInstanceResult`] carry their own
/// `error_message`).
///
/// The return value is null if no such message is stored. If non-null, it must be released with
/// [`cedarling_free_string`].
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_get_last_error() -> *mut c_char {
    ffi_guard_ptr_mut!({ get_last_error() })
}

/// Clears the thread-local string read by [`cedarling_get_last_error`].
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_clear_last_error() {
    ffi_guard_void!({ clear_last_error() })
}

/// Returns the Cedarling library version as a NUL-terminated static string.
///
/// The returned pointer refers to read-only memory embedded in the library binary.
/// It is valid for the lifetime of the process and **must not** be passed to
/// `cedarling_free_string` or any other deallocation function.
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_version() -> *const c_char {
    ffi_guard_ptr_const!({ concat!(env!("CARGO_PKG_VERSION"), "\0").as_ptr() as *const c_char })
}

/// Shut down and remove **all** Cedarling instances held by the library, then clear this thread's
/// auxiliary last-error string ([`cedarling_get_last_error`]).
///
/// Safe to call from any thread; repeated calls are effectively idempotent after the first full
/// teardown. No pointer parameters.
///
/// After a successful teardown, no instances remain registered and this thread's
/// [`cedarling_get_last_error`] returns null until a new auxiliary error is stored.
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_cleanup() {
    ffi_guard_void!({
        let _ = cleanup_runtime();
        // Keep historical behavior: reset caller-visible last error state.
        clear_last_error();
    })
}
