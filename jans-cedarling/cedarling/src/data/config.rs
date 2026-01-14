// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

/// Configuration for the DataStore component.
///
/// Controls storage limits, TTL behavior, and capacity management.
#[derive(Debug, Clone)]
pub struct DataStoreConfig {
    /// Maximum number of data entries (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Default TTL for entries without explicit expiration
    pub default_ttl: Option<Duration>,
    /// Maximum allowed TTL
    pub max_ttl: Option<Duration>,
}

impl Default for DataStoreConfig {
    fn default() -> Self {
        Self {
            max_entries: 10_000,
            max_entry_size: 1_048_576, // 1MB
            default_ttl: None,
            max_ttl: Some(Duration::from_secs(3600)), // 1 hour
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_config() {
        let config = DataStoreConfig::default();
        assert_eq!(config.max_entries, 10_000);
        assert_eq!(config.max_entry_size, 1_048_576);
        assert_eq!(config.default_ttl, None);
        assert_eq!(config.max_ttl, Some(Duration::from_secs(3600)));
    }
}
