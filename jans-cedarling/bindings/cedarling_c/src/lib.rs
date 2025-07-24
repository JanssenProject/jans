/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 */

#![cfg(not(target_arch = "wasm32"))]

use std::os::raw::{c_char, c_int};
use std::ptr;

mod c_interface;
mod types;

use c_interface::*;
use types::*;
/// # Safety
/// Intialize the Cedarling library
/// This Function should be called before any other functions
#[no_mangle]
pub extern "C" fn cedarling_init() -> c_int {
    // Initialize any global state if needed
    0 // Success
}
/// # Safety
/// Create a new Cedarling instance
///
/// # Arguments
/// * `config_json` - JSON string containing the configuration for the Cedarling instance
/// * `result` - Pointer to a CedarlingInstanceResult structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_new(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
    if config_json.is_null() || result.is_null() {
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
}
/// # Safety
/// Create a new Cedarling instance with environment variables support
///
/// # Arguments
/// * `config_json` - Optional JSON string containing the configuration for the Cedarling instance (can be null)
/// * `result` - Pointer to a CedarlingInstanceResult structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_new_with_env(
    config_json: *const c_char,
    result: *mut CedarlingInstanceResult,
) -> c_int {
    if result.is_null() {
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
}
/// # Safety
/// Drop a cedarling instance
///
/// # Arguments
/// * `instance_id` - ID of the instance to be dropped
#[no_mangle]
pub unsafe extern "C" fn cedarling_drop(instance_id: u64) {
    drop_instance(instance_id);
}
/// # Safety
/// Authorize a request
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to use for authorization
/// * `request_json` - JSON string containing the request to be authorized
/// * `result` - Pointer to a CedarlingResult structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_authorize(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    if request_json.is_null() || result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let request_str = match c_string_to_string(request_json) {
        Ok(s) => s,
        Err(code) => unsafe {
            *result = CedarlingResult::error(code, "Invalid request JSON string");
            return code as c_int;
        },
    };

    let auth_result = authorize(instance_id, &request_str);
    unsafe { *result = auth_result };
    unsafe { (*result).error_code as c_int }
}
/// # Safety
/// Authrorize an unsigned request
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to use for authorization
/// * `request_json` - JSON string containing the request to be authorized
/// * `result` - Pointer to a CedarlingResult structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_authorize_unsigned(
    instance_id: u64,
    request_json: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    if request_json.is_null() || result.is_null() {
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
/// Pop all logs from an instance
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to pop logs from
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
unsafe extern "C" fn cedarling_pop_logs(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    if result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let logs = pop_logs(instance_id);

    unsafe { *result = logs };
    CedarlingErrorCode::Success as c_int
}
/// # Safety
/// Get a log by ID
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to get the log from
/// * `log_id` - ID of the log to retrieve
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_get_log_by_id(
    instance_id: u64,
    log_id: *const c_char,
    result: *mut CedarlingResult,
) -> c_int {
    if log_id.is_null() || result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let id_str = match c_string_to_string(log_id) {
        Ok(s) => s,
        Err(code) => unsafe {
            *result = CedarlingResult::error(code, "Invalid request JSON string");
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
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to get the log IDs from
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_get_log_ids(
    instance_id: u64,
    result: *mut CedarlingStringArray,
) -> c_int {
    if result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let log_ids = get_log_ids(instance_id);

    unsafe { *result = log_ids };
    CedarlingErrorCode::Success as c_int
}
/// # Safety
/// Get logs by tag
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to get the log IDs from
/// * `tag` - C string containing the tag to filter logs by
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_get_logs_by_tag(
    instance_id: u64,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    if tag.is_null() || result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let tag_str = match c_string_to_string(tag) {
        Ok(s) => s,
        Err(_) => unsafe {
            *result = CedarlingStringArray {
                items: ptr::null_mut(),
                count: 0,
            };
            return CedarlingErrorCode::InvalidArgument as c_int;
        },
    };

    let logs = get_logs_by_tag(instance_id, &tag_str);

    unsafe { *result = logs };
    CedarlingErrorCode::Success as c_int
}
/// # Safety
/// Get logs by request ID
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to get the log IDs from
/// * `request_id` - request ID to filter logs by
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id(
    instance_id: u64,
    request_id: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    if request_id.is_null() || result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let request_id_str = match c_string_to_string(request_id) {
        Ok(s) => s,
        Err(_) => unsafe {
            *result = CedarlingStringArray {
                items: ptr::null_mut(),
                count: 0,
            };
            return CedarlingErrorCode::InvalidArgument as c_int;
        },
    };

    let logs = get_logs_by_request_id(instance_id, &request_id_str);

    unsafe { *result = logs };
    CedarlingErrorCode::Success as c_int
}
/// # Safety
/// Get logs by request ID and tag
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to get the log IDs from
/// * `request_id` - request ID to filter logs by
/// * `tag` - C string containing the tag to filter logs by
/// * `result` - Pointer to a CedarlingStringArray structure to store the result
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_get_logs_by_request_id_and_tag(
    instance_id: u64,
    request_id: *const c_char,
    tag: *const c_char,
    result: *mut CedarlingStringArray,
) -> c_int {
    if request_id.is_null() || tag.is_null() || result.is_null() {
        return CedarlingErrorCode::InvalidArgument as c_int;
    }

    let request_id_str = match c_string_to_string(request_id) {
        Ok(s) => s,
        Err(_) => unsafe {
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
            *result = CedarlingStringArray {
                items: ptr::null_mut(),
                count: 0,
            };
            return CedarlingErrorCode::InvalidArgument as c_int;
        },
    };

    let logs = get_logs_by_request_id_and_tag(instance_id, &request_id_str, &tag_str);

    unsafe { *result = logs };
    CedarlingErrorCode::Success as c_int
}

/// # Safety
/// Shutdown a Cedarling instance
///
/// # Arguments
/// * `instance_id` - ID of the Cedarling instance to be shut down
///
/// # Returns
/// * 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn cedarling_shutdown(instance_id: u64) -> c_int {
    shutdown_instance(instance_id) as c_int
}
/// # Safety
/// Free a string returned by Cedarling functions
///
/// # Arguments
/// * `str_ptr` - Pointer to the string to be freed
#[no_mangle]
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
/// # Arguments
/// * `array` - Pointer to the CedarlingStringArray to be freed
#[no_mangle]
pub unsafe extern "C" fn cedarling_free_string_array(array: *mut CedarlingStringArray) {
    if array.is_null() {
        return;
    }

    unsafe {
        let array_ref = &*array;
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
    }
}

/// # Safety
/// Free a CedarlingResult structure
///
/// # Arguments
/// * `result` - Pointer to the CedarlingResult structure to be freed
#[no_mangle]
pub unsafe extern "C" fn cedarling_free_result(result: *mut CedarlingResult) {
    if result.is_null() {
        return;
    }

    unsafe {
        let result_ref = &*result;
        if !result_ref.data.is_null() {
            // Convert the raw pointer back to a CString and drop it
            let _ = std::ffi::CString::from_raw(result_ref.data as *mut i8);
        }
        if !result_ref.error_message.is_null() {
            // Convert the raw pointer back to a CString and drop it
            let _ = std::ffi::CString::from_raw(result_ref.error_message);
        }
    }
}

/// # Safety
/// Free a CedarlingInstanceResult structure
///
/// # Arguments
/// * `result` - Pointer to the CedarlingInstanceResult structure to be freed
#[no_mangle]
pub unsafe extern "C" fn cedarling_free_instance_result(result: *mut CedarlingInstanceResult) {
    if result.is_null() {
        return;
    }

    unsafe {
        let result_ref = &*result;
        if !result_ref.error_message.is_null() {
            // Convert the raw pointer back to a CString and drop it
            let _ = std::ffi::CString::from_raw(result_ref.error_message);
        }
    }
}

/// # Safety
/// Get the last error message
///
/// # Returns
/// * Pointer to the last error message string (do not free this pointer)
#[no_mangle]
pub extern "C" fn cedarling_get_last_error() -> *const c_char {
    get_last_error()
}

/// # Safety
/// Clear last error message
#[no_mangle]
pub extern "C" fn cedarling_clear_last_error() {
    clear_last_error();
}

/// # Safety
/// Get the Cedarling library version
///
/// # Returns
/// * Pointer to a C string containing the version (do not free this pointer)
#[no_mangle]
pub extern "C" fn cedarling_version() -> *const c_char {
    concat!(env!("CARGO_PKG_VERSION"), "\0").as_ptr() as *const c_char
}

#[no_mangle]
pub extern "C" fn cedarling_cleanup() {
    // Perform any necessary cleanup
    clear_last_error();
}
