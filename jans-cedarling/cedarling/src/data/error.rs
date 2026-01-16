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
    #[error("key not found: {0}")]
    KeyNotFound(String),

    /// Storage limit exceeded
    #[error("storage limit exceeded: max {max} entries")]
    StorageLimitExceeded { max: usize },

    /// TTL exceeds maximum allowed
    #[error("TTL exceeds maximum: requested {requested:?}, max {max:?}")]
    TTLExceeded { requested: Duration, max: Duration },

    /// Value too large
    #[error("value too large: {size} bytes exceeds max {max} bytes")]
    ValueTooLarge { size: usize, max: usize },

    /// Serialization error
    #[error("serialization error: {0}")]
    SerializationError(#[from] serde_json::Error),
}
