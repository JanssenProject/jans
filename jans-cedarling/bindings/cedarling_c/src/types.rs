use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int};
use std::ptr;

/// Error codes returend by cedarling functions.
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
    /// Memory allocation error
    MemoryError = 6,
    /// Internal error
    Internal = 7,
}

/// Result structure for operations that return data
#[repr(C)]
pub struct CedarlingResult {
    /// Error code (0=Success))
    pub error_code: CedarlingErrorCode,
    /// Data pointer (null if error)
    pub data: *mut char,
    /// Error message (null if success)
    pub error_message: *mut c_char,
}

///Structure for returning arrays of strings
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
    /// Error code (0=Success))
    pub error_code: CedarlingErrorCode,
    /// Instance ID (0 if error)
    pub instance_id: u64,
    /// Error message (null if success)
    pub error_message: *mut c_char,
}

impl CedarlingResult {
    pub fn success(data: String) -> Self {
        let c_string = match CString::new(data) {
            Ok(s) => s,
            Err(_) => return Self::error(CedarlingErrorCode::Internal, "Invalid string data"),
        };

        CedarlingResult {
            error_code: CedarlingErrorCode::Success,
            data: c_string.into_raw() as *mut char,
            error_message: ptr::null_mut(),
        }
    }

    pub fn error(code: CedarlingErrorCode, message: &str) -> Self {
        let error_msg = match CString::new(message) {
            Ok(s) => s.into_raw(),
            Err(_) => ptr::null_mut(),
        };

        CedarlingResult {
            error_code: code,
            data: ptr::null_mut(),
            error_message: error_msg,
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
        let error_msg = match CString::new(message) {
            Ok(s) => s.into_raw(),
            Err(_) => ptr::null_mut(),
        };

        Self {
            error_code: code,
            instance_id: 0,
            error_message: error_msg,
        }
    }
}

impl CedarlingStringArray {
    pub fn new(strings: Vec<String>) -> Self {
        if strings.is_empty() {
            return CedarlingStringArray {
                items: ptr::null_mut(),
                count: 0,
            };
        }
        let mut c_strings: Vec<*mut c_char> = Vec::with_capacity(strings.len());

        for s in strings.clone() {
            match CString::new(s) {
                Ok(c_str) => c_strings.push(c_str.into_raw()),
                Err(_) => {
                    // Clean up previously allocated strings
                    for ptr in c_strings {
                        unsafe {
                            let _ = CString::from_raw(ptr);
                        }; // Convert back to CString to free memory
                    }
                    return CedarlingStringArray {
                        items: ptr::null_mut(),
                        count: 0,
                    };
                },
            };
        }
        let items_ptr = c_strings.as_mut_ptr();
        std::mem::forget(c_strings); // Prevent Deallocation
        Self {
            items: items_ptr,
            count: strings.len(),
        }
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

pub fn string_to_c_string(s: &str) -> *mut c_char {
    match CString::new(s) {
        Ok(c_string) => c_string.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

/// thread-local storage for last error message
thread_local! {
    static LAST_ERROR: std::cell::RefCell<Option<CString>> = std::cell::RefCell::new(None);
}

pub fn set_last_error(message: &str) {
    let c_string =
        CString::new(message).unwrap_or_else(|_| CString::new("Invalid error message").unwrap());
    LAST_ERROR.with(|last_error| {
        *last_error.borrow_mut() = Some(c_string);
    });
}

pub fn get_last_error() -> *mut c_char {
    LAST_ERROR.with(|last_error| {
        if let Some(ref c_string) = *last_error.borrow() {
            c_string.as_ptr() as *mut c_char
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
