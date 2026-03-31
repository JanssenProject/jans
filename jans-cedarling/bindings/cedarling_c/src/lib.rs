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
/// # Safety
/// Initialize the Cedarling library
/// This Function should be called before any other functions
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_init() -> c_int {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(initialize_runtime)) {
        Ok(code) => code as c_int,
        Err(_) => {
            set_last_error("panic during runtime initialization");
            CedarlingErrorCode::Internal as c_int
        },
    }
}
/// # Safety
/// Create a new Cedarling instance
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_new(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if config_json.is_null() {
        set_last_error("null config_json pointer");
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
            set_last_error("Invalid config JSON string");
            *result = CedarlingInstanceResult::error(code, "Invalid config JSON string");
            return code as c_int;
        },
    };

    let instance_result = create_instance(&config_str);
    unsafe { *result = instance_result };
    unsafe { (*result).error_code as c_int }
}
/// # Safety
/// Create a new Cedarling instance with environment variables support
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_new_with_env(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
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
                set_last_error("Invalid config JSON string");
                *result = CedarlingInstanceResult::error(code, "Invalid config JSON string");
                return code as c_int;
            },
        }
    };
    let instance_result = create_instance_with_env(config_str.as_deref());
    unsafe { *result = instance_result };
    unsafe { (*result).error_code as c_int }
}
/// # Safety
/// Drop a cedarling instance
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_drop(instance_id: u64) -> c_int {
    drop_instance(instance_id) as c_int
}
/// # Safety
/// Authorize an unsigned request
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_authorize_unsigned(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if request_json.is_null() {
        set_last_error("null request_json pointer");
        unsafe {
            *result =
                CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null request_json pointer");
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
}

/// # Safety
/// Authorize a multi-issuer request
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_authorize_multi_issuer(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if request_json.is_null() {
        set_last_error("null request_json pointer");
        unsafe {
            *result =
                CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null request_json pointer");
        }
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let request_str = match c_string_to_string(request_json) {
        Ok(s) => s,
        Err(code) => unsafe {
            set_last_error("Invalid request JSON string");
            *result = CedarlingResult::error(code, "Invalid request JSON string");
            return code as c_int;
        },
    };

    let auth_result = authorize_multi_issuer(instance_id, &request_str);
    unsafe { *result = auth_result };
    unsafe { (*result).error_code as c_int }
}

// Context Data API functions

/// # Safety
/// Push context data
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_push(
    instance_id: u64,
    key: *const c_char,
    value_json: *const c_char,
    ttl_secs: i64,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if key.is_null() {
        set_last_error("null key pointer");
        unsafe {
            *result = CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
        }
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if value_json.is_null() {
        set_last_error("null value_json pointer");
        unsafe {
            *result =
                CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null value_json pointer");
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
}

/// # Safety
/// Get context data by key
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_get(
    instance_id: u64,
    key: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if key.is_null() {
        set_last_error("null key pointer");
        unsafe {
            *result = CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
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
}

/// # Safety
/// Remove context data by key
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_remove(
    instance_id: u64,
    key: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if key.is_null() {
        set_last_error("null key pointer");
        unsafe {
            *result = CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null key pointer");
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
}

/// # Safety
/// Clear all context data
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_clear(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let clear_result = context_clear(instance_id);
    unsafe { *result = clear_result };
    unsafe { (*result).error_code as c_int }
}

/// # Safety
/// List all context entries with metadata
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_list(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let list_result = context_list(instance_id);
    unsafe { *result = list_result };
    unsafe { (*result).error_code as c_int }
}

/// # Safety
/// Get context stats
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_context_stats(
    instance_id: u64,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let stats_result = context_stats(instance_id);
    unsafe { *result = stats_result };
    unsafe { (*result).error_code as c_int }
}

/// # Safety
/// Pop all logs from an instance
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_pop_logs(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}
/// # Safety
/// Get a log by ID
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_log_by_id(
    instance_id: u64,
    log_id: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    clear_last_error();
    if result.is_null() {
        set_last_error("null result pointer");
        return CedarlingErrorCode::InvalidArgument as c_int;
    }
    if log_id.is_null() {
        set_last_error("null log_id pointer");
        unsafe {
            *result = CedarlingResult::error(CedarlingErrorCode::InvalidArgument, "null log_id pointer");
        }
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let id_str = match c_string_to_string(log_id) {
        Ok(s) => s,
        Err(code) => unsafe {
            set_last_error("Invalid log_id C string");
            *result = CedarlingResult::error(code, "Invalid log_id C string");
            return code as c_int;
        },
    };

    let log_result = get_log_by_id(instance_id, &id_str);

    unsafe { *result = log_result };
    unsafe { (*result).error_code as c_int }
}
/// # Safety
/// Get all log IDs
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_log_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}
/// # Safety
/// Get logs by tag
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_tag(
    instance_id: u64,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}
/// # Safety
/// Get logs by request ID
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id(
    instance_id: u64,
    request_id: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}
/// # Safety
/// Get logs by request ID and tag
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id_and_tag(
    instance_id: u64,
    request_id: *const c_char,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}

/// # Safety
/// Check whether a trusted issuer was loaded by issuer identifier
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_is_trusted_issuer_loaded_by_name(
    instance_id: u64,
    issuer_id: *const c_char,
    out_result: *mut bool,
) -> c_int {
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
}

/// # Safety
/// Check whether a trusted issuer was loaded by `iss` claim
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_is_trusted_issuer_loaded_by_iss(
    instance_id: u64,
    iss_claim: *const c_char,
    out_result: *mut bool,
) -> c_int {
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
}

/// # Safety
/// Get total number of trusted issuers discovered
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_total_issuers(instance_id: u64, out_count: *mut usize) -> c_int {
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
}

/// # Safety
/// Get number of trusted issuers loaded successfully
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_loaded_trusted_issuers_count(
    instance_id: u64,
    out_count: *mut usize,
) -> c_int {
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
}

/// # Safety
/// Get trusted issuer IDs loaded successfully
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_loaded_trusted_issuer_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}

/// # Safety
/// Get trusted issuer IDs that failed to load
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_failed_trusted_issuer_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
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
}

/// # Safety
/// Shutdown a Cedarling instance
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_shutdown(instance_id: u64) -> c_int {
    shutdown_instance(instance_id) as c_int
}
/// # Safety
/// Free a string returned by Cedarling functions
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_string(str_ptr: *mut c_char) {
    if !str_ptr.is_null() {
        unsafe {
            // Convert the raw pointer back to a CString and drop it
            let _ = std::ffi::CString::from_raw(str_ptr);
        }
    }
}
/// # Safety
/// Free a string array returned by Cedarling functions
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_string_array(array: *mut CedarlingStringArray) {
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
            // Free the array of pointers
            let _ = Vec::from_raw_parts(array_ref.items, array_ref.count, array_ref.count);
        }
        // Clear the struct to prevent double-free
        array_ref.items = ptr::null_mut();
        array_ref.count = 0;
    }
}

/// # Safety
/// Free a CedarlingResult structure
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_result(result: *mut CedarlingResult) {
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
}

/// # Safety
/// Free a CedarlingInstanceResult structure
///
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cedarling_free_instance_result(result: *mut CedarlingInstanceResult) {
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
}

/// # Safety
/// Get the last error message
///
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_get_last_error() -> *const c_char {
    get_last_error()
}

/// # Safety
/// Clear last error message
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_clear_last_error() {
    clear_last_error();
}

/// # Safety
/// Get the Cedarling library version
///
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_version() -> *const c_char {
    concat!(env!("CARGO_PKG_VERSION"), "\0").as_ptr() as *const c_char
}

/// # Safety
/// Global library cleanup helper.
///
/// This function only clears thread-local last-error state and performs no unsafe memory access.
/// It is thread-safe and idempotent: it can be called multiple times with the same effect.
/// There are no preconditions; call it at process shutdown or whenever you want to reset error state.
///
/// # Postconditions
/// The calling thread's `cedarling_get_last_error()` value is reset to null until a new error is set.
#[unsafe(no_mangle)]
pub extern "C" fn cedarling_cleanup() {
    // Perform any necessary cleanup
    clear_last_error();
}
