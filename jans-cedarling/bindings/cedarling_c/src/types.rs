// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::ptr;

/// Error codes returned by cedarling functions.
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum CedarlingErrorCode {
    /// Success - no error
    Success = 0,
    /// Invalid argument provided
    InvalidArgument = 1,
    /// Instance not found
    InstanceNotFound = 2,
    /// JSON parsing error
    JsonError = 3,
    /// Authorization error
    AuthorizationError = 4,
    /// Configuration error
    ConfigurationError = 5,
    /// Internal error
    Internal = 6,
    /// Requested key or id does not exist (e.g. unknown log id)
    KeyNotFound = 7,
}

/// Result structure for operations that return data
#[repr(C)]
pub struct CedarlingResult {
    /// Error code (`Success` = 0). Check this first to detect failure.
    pub error_code: CedarlingErrorCode,
    /// Data pointer (null if error)
    pub data: *mut c_char,
    /// Human-readable detail on failure (null on success). Prefer `error_code` for control flow.
    pub error_message: *mut c_char,
}

/// Structure for returning arrays of strings
#[repr(C)]
pub struct CedarlingStringArray {
    /// Array of string pointers
    pub items: *mut *mut c_char,
    /// Number of items in the array
    pub count: usize,
}

/// Structure for instance creation results
#[repr(C)]
pub struct CedarlingInstanceResult {
    /// Error code (`Success` = 0). Check this first to detect failure.
    pub error_code: CedarlingErrorCode,
    /// Instance ID (0 if error)
    pub instance_id: u64,
    /// Human-readable detail on failure (null on success). Prefer `error_code` for control flow.
    pub error_message: *mut c_char,
}

/// When an error `message` cannot be represented as a C string (e.g. interior NUL), use this text
/// so FFI callers still receive a non-null `error_message` whenever `error_code` indicates failure.
const ERROR_MESSAGE_CSTRING_FALLBACK: &str = "Invalid error message encoding (interior NUL byte)";

fn error_message_to_c_ptr(message: &str) -> *mut c_char {
    match CString::new(message) {
        Ok(s) => s.into_raw(),
        Err(_) => CString::new(ERROR_MESSAGE_CSTRING_FALLBACK)
            .expect("ERROR_MESSAGE_CSTRING_FALLBACK must be valid as CString")
            .into_raw(),
    }
}

impl CedarlingResult {
    pub fn success(data: String) -> Self {
        let c_string = match CString::new(data) {
            Ok(s) => s,
            Err(_) => return Self::error(CedarlingErrorCode::Internal, "Invalid string data"),
        };

        CedarlingResult {
            error_code: CedarlingErrorCode::Success,
            data: c_string.into_raw(),
            error_message: ptr::null_mut(),
        }
    }

    pub fn error(code: CedarlingErrorCode, message: &str) -> Self {
        CedarlingResult {
            error_code: code,
            data: ptr::null_mut(),
            error_message: error_message_to_c_ptr(message),
        }
    }
}

impl CedarlingInstanceResult {
    pub fn success(instance_id: u64) -> Self {
        Self {
            error_code: CedarlingErrorCode::Success,
            instance_id,
            error_message: ptr::null_mut(),
        }
    }

    pub fn error(code: CedarlingErrorCode, message: &str) -> Self {
        Self {
            error_code: code,
            instance_id: 0,
            error_message: error_message_to_c_ptr(message),
        }
    }
}

impl CedarlingStringArray {
    /// Build a string array for FFI. On interior NUL bytes, sets last error and
    /// returns [`CedarlingErrorCode::Internal`].
    pub fn try_new(strings: Vec<String>) -> Result<Self, CedarlingErrorCode> {
        if strings.is_empty() {
            return Ok(CedarlingStringArray {
                items: ptr::null_mut(),
                count: 0,
            });
        }
        let count = strings.len();
        let mut c_strings: Vec<*mut c_char> = Vec::with_capacity(count);

        for s in strings {
            match CString::new(s) {
                Ok(c_str) => c_strings.push(c_str.into_raw()),
                Err(e) => {
                    for ptr in c_strings {
                        unsafe {
                            let _ = CString::from_raw(ptr);
                        }
                    }
                    set_last_error(&format!(
                        "CedarlingStringArray::try_new failed: CString::new error: {}",
                        e
                    ));
                    return Err(CedarlingErrorCode::Internal);
                },
            };
        }

        let mut boxed = c_strings.into_boxed_slice();
        let count = boxed.len();
        let items_ptr = boxed.as_mut_ptr();
        std::mem::forget(boxed);
        Ok(CedarlingStringArray {
            items: items_ptr,
            count,
        })
    }
}

/// Utility functions for string conversion
pub fn c_string_to_string(c_str: *const c_char) -> Result<String, CedarlingErrorCode> {
    if c_str.is_null() {
        return Err(CedarlingErrorCode::InvalidArgument);
    }
    unsafe {
        match CStr::from_ptr(c_str).to_str() {
            Ok(s) => Ok(s.to_string()),
            Err(_) => Err(CedarlingErrorCode::InvalidArgument),
        }
    }
}

// thread-local storage for last error message
// Removing unused attribute since it doesn't apply to thread_local macro
thread_local! {
    static LAST_ERROR: std::cell::RefCell<Option<CString>> = const { std::cell::RefCell::new(None) };
}

pub fn set_last_error(message: &str) {
    let c_string =
        CString::new(message).unwrap_or_else(|_| CString::new("Invalid error message").unwrap());
    LAST_ERROR.with(|last_error| {
        *last_error.borrow_mut() = Some(c_string);
    });
}

/// Returns a newly allocated copy of the current thread's last error string.
///
/// The caller owns the returned pointer and must free it with
/// `cedarling_free_string`. Returns null if no error is currently stored or
/// if allocation fails.
pub fn get_last_error() -> *mut c_char {
    LAST_ERROR.with(|last_error| {
        if let Some(ref c_string) = *last_error.borrow() {
            match CString::new(c_string.as_bytes()) {
                Ok(copy) => copy.into_raw(),
                Err(_) => ptr::null_mut(),
            }
        } else {
            ptr::null_mut()
        }
    })
}

pub fn clear_last_error() {
    LAST_ERROR.with(|last_error| {
        *last_error.borrow_mut() = None;
    });
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cedarling_result_error_uses_fallback_when_message_has_interior_nul() {
        let r = CedarlingResult::error(CedarlingErrorCode::Internal, "bad\0tail");
        assert_eq!(r.error_code, CedarlingErrorCode::Internal);
        assert!(!r.error_message.is_null());
        unsafe {
            let msg = CString::from_raw(r.error_message);
            assert_eq!(msg.to_str().unwrap(), ERROR_MESSAGE_CSTRING_FALLBACK);
        }
    }

    #[test]
    fn cedarling_instance_result_error_uses_fallback_when_message_has_interior_nul() {
        let r = CedarlingInstanceResult::error(CedarlingErrorCode::JsonError, "x\0y");
        assert_eq!(r.error_code, CedarlingErrorCode::JsonError);
        assert_eq!(r.instance_id, 0);
        assert!(!r.error_message.is_null());
        unsafe {
            let msg = CString::from_raw(r.error_message);
            assert_eq!(msg.to_str().unwrap(), ERROR_MESSAGE_CSTRING_FALLBACK);
        }
    }
}
