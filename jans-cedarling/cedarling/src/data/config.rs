// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

/// Configuration for the DataStore component.
///
/// Controls storage limits, TTL behavior, and capacity management.
///
/// ## TTL Semantics
///
/// - `default_ttl: None` means entries without explicit TTL will effectively never expire (10 years)
/// - `max_ttl: None` means no upper limit on TTL values (10 years max)
/// - If both are `Some`, the effective TTL will always respect `max_ttl` constraints
///
/// ## Examples
///
/// ```
/// use std::time::Duration;
/// use cedarling::data::DataStoreConfig;
///
/// // No expiration by default, but cap at 1 hour when explicitly set
/// let config = DataStoreConfig {
///     default_ttl: None,  // No automatic expiration (10 years)
///     max_ttl: Some(Duration::from_secs(3600)),  // But cap at 1 hour if specified
///     ..Default::default()
/// };
///
/// // All entries expire after 5 minutes by default, max 1 hour
/// let config = DataStoreConfig {
///     default_ttl: Some(Duration::from_secs(300)),  // 5 minutes
///     max_ttl: Some(Duration::from_secs(3600)),  // 1 hour
///     ..Default::default()
/// };
/// ```
#[derive(Debug, Clone)]
pub struct DataStoreConfig {
    /// Maximum number of data entries (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Default TTL for entries without explicit expiration.
    /// `None` means entries effectively never expire (uses 10 years).
    pub default_ttl: Option<Duration>,
    /// Maximum allowed TTL.
    /// `None` means no upper limit on TTL values (uses 10 years).
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
