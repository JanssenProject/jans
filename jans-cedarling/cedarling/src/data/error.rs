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
