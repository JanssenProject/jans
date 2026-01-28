// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

/// Errors that can occur during data store operations.
#[derive(Debug, thiserror::Error)]
pub enum DataError {
    /// Invalid key provided (key is empty)
    #[error("invalid key: key cannot be empty")]
    InvalidKey,

    /// Key not found in store
    #[error("key not found: {key}")]
    KeyNotFound {
        /// The key that was not found
        key: String,
    },

    /// Storage limit exceeded
    #[error("storage limit exceeded: max {max} entries")]
    StorageLimitExceeded {
        /// Maximum allowed entries
        max: usize,
    },

    /// TTL exceeds maximum allowed
    #[error("TTL ({requested:?}) exceeds max ({max:?})")]
    TTLExceeded {
        /// The requested TTL
        requested: Duration,
        /// The maximum allowed TTL
        max: Duration,
    },

    /// Value too large
    #[error("value size {size} bytes exceeds max {max} bytes")]
    ValueTooLarge {
        /// Actual size in bytes
        size: usize,
        /// Maximum allowed size in bytes
        max: usize,
    },

    /// Serialization error
    #[error("serialization error: {0}")]
    SerializationError(#[from] serde_json::Error),
}

/// Errors that can occur during value mapping operations.
#[derive(Debug, thiserror::Error)]
pub enum ValueMappingError {
    /// Failed to construct a Cedar expression
    #[error("expression construction failed: {0}")]
    ExpressionConstruction(#[from] cedar_policy::ExpressionConstructionError),

    /// Type mismatch between expected and actual types
    #[error("type mismatch: expected {expected}, found {actual}")]
    TypeMismatch {
        /// The expected type
        expected: String,
        /// The actual type found
        actual: String,
    },

    /// Invalid extension type format
    #[error("invalid {extension_type} format: {value}")]
    InvalidExtensionFormat {
        /// The extension type (e.g., "decimal", "ipaddr")
        extension_type: String,
        /// The invalid value
        value: String,
    },

    /// Path not found in nested structure
    #[error("path not found: {path}")]
    PathNotFound {
        /// The path that was not found
        path: String,
    },

    /// Invalid path format
    #[error("invalid path: {path}")]
    InvalidPath {
        /// The invalid path
        path: String,
    },

    /// Null values are not supported in Cedar
    #[error("null values are not supported in Cedar")]
    NullNotSupported,

    /// Entity reference is invalid
    #[error("invalid entity reference: {reason}")]
    InvalidEntityReference {
        /// The reason the entity reference is invalid
        reason: String,
    },

    /// Value size exceeds limits
    #[error("value size {size} exceeds limit {limit}")]
    ValueTooLarge {
        /// Actual size
        size: usize,
        /// Maximum allowed size
        limit: usize,
    },

    /// Nested errors from collections
    #[error("errors in collection: {0:?}")]
    CollectionErrors(Vec<ValueMappingError>),

    /// Number cannot be represented as Cedar Long or Decimal
    #[error("number cannot be represented in Cedar: {value}")]
    NumberNotRepresentable {
        /// The original number string that could not be converted
        value: String,
    },
}

/// Errors that can occur during validation.
#[derive(Debug, thiserror::Error)]
pub enum ValidationError {
    /// Value type does not match expected type
    #[error("type mismatch at {path}: expected {expected}, found {actual}")]
    TypeMismatch {
        /// The path where the error occurred
        path: String,
        /// The expected type
        expected: String,
        /// The actual type found
        actual: String,
    },

    /// Value exceeds maximum nesting depth
    #[error("maximum nesting depth ({max}) exceeded at {path}")]
    MaxDepthExceeded {
        /// The path where the error occurred
        path: String,
        /// The maximum allowed depth
        max: usize,
    },

    /// String value exceeds maximum length
    #[error("string at {path} exceeds maximum length ({length} > {max})")]
    StringTooLong {
        /// The path where the error occurred
        path: String,
        /// The actual length
        length: usize,
        /// The maximum allowed length
        max: usize,
    },

    /// Array exceeds maximum length
    #[error("array at {path} exceeds maximum length ({length} > {max})")]
    ArrayTooLong {
        /// The path where the error occurred
        path: String,
        /// The actual length
        length: usize,
        /// The maximum allowed length
        max: usize,
    },

    /// Object has too many keys
    #[error("object at {path} has too many keys ({count} > {max})")]
    TooManyKeys {
        /// The path where the error occurred
        path: String,
        /// The actual number of keys
        count: usize,
        /// The maximum allowed keys
        max: usize,
    },

    /// Null values are not supported
    #[error("null value at {path} is not supported in Cedar")]
    NullNotSupported {
        /// The path where the error occurred
        path: String,
    },

    /// Invalid extension type format
    #[error("invalid {extension_type} format at {path}: {value}")]
    InvalidExtensionFormat {
        /// The path where the error occurred
        path: String,
        /// The extension type
        extension_type: String,
        /// The invalid value
        value: String,
    },

    /// Invalid entity reference
    #[error("invalid entity reference at {path}: {reason}")]
    InvalidEntityReference {
        /// The path where the error occurred
        path: String,
        /// The reason for the error
        reason: String,
    },

    /// Invalid key format
    #[error("invalid key at {path}: {reason}")]
    InvalidKey {
        /// The path where the error occurred
        path: String,
        /// The reason for the error
        reason: String,
    },

    /// Number out of range
    #[error("number at {path} is out of i64 range")]
    NumberOutOfRange {
        /// The path where the error occurred
        path: String,
    },

    /// Multiple validation errors
    #[error("multiple validation errors: {0:?}")]
    Multiple(Vec<ValidationError>),
}
