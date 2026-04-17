// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

/// Configuration for the `DataStore` component.
///
/// Controls storage limits, TTL behavior, capacity management, and metrics.
///
/// ## TTL Semantics
///
/// - `default_ttl: None` means entries without explicit TTL will effectively never expire.
///   The implementation uses an internal sentinel cap of 10 years (not user-configurable).
/// - `max_ttl: None` means no user-configured upper limit on TTL values.
///   The implementation uses an internal sentinel cap of 10 years (not user-configurable).
/// - If both are `Some`, the effective TTL will always respect `max_ttl` constraints
///
/// ## Examples
///
/// ```no_run
/// use std::time::Duration;
/// use cedarling::DataStoreConfig;
///
/// // No configured expiration (uses internal long sentinel TTL)
/// let config = DataStoreConfig {
///     default_ttl: None,  // No automatic expiration
///     max_ttl: None,      // No user-configured upper limit
///     ..Default::default()
/// };
///
/// // All entries expire after 5 minutes by default, max 1 hour
/// // When both are Some, max_ttl constrains the effective TTL
/// let config = DataStoreConfig {
///     default_ttl: Some(Duration::from_secs(300)),  // 5 minutes
///     max_ttl: Some(Duration::from_secs(3600)),     // 1 hour (constrains effective TTL)
///     ..Default::default()
/// };
/// ```
#[derive(Debug, Clone, PartialEq)]
pub struct DataStoreConfig {
    /// Maximum number of data entries (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Default TTL for entries without explicit expiration.
    /// `None` means entries effectively never expire.
    /// The implementation uses an internal sentinel cap of 10 years (not user-configurable).
    pub default_ttl: Option<Duration>,
    /// Maximum allowed TTL.
    /// `None` means no user-configured upper limit on TTL values.
    /// The implementation uses an internal sentinel cap of 10 years (not user-configurable).
    pub max_ttl: Option<Duration>,
    /// Enable metrics tracking (access counts, timestamps)
    pub enable_metrics: bool,
    /// Memory usage alert threshold as a percentage (0.0-100.0).
    /// When capacity usage exceeds this threshold, an alert is triggered.
    /// Default: 80.0 (80%)
    pub memory_alert_threshold: f64,
}

impl Default for DataStoreConfig {
    fn default() -> Self {
        Self {
            max_entries: 10_000,
            max_entry_size: 1_048_576, // 1MB
            default_ttl: None,
            max_ttl: Some(Duration::from_secs(3600)), // 1 hour
            enable_metrics: true,
            memory_alert_threshold: 80.0, // 80%
        }
    }
}

/// Error returned when `DataStoreConfig` validation fails.
#[derive(Debug, thiserror::Error)]
pub enum ConfigValidationError {
    /// `default_ttl` exceeds `max_ttl`
    #[error("default_ttl ({default:?}) exceeds max_ttl ({max:?})")]
    DefaultTtlExceedsMax {
        /// The default TTL value that exceeds the maximum
        default: Duration,
        /// The maximum TTL value
        max: Duration,
    },
    /// `memory_alert_threshold` is outside valid range
    #[error("memory_alert_threshold ({value}) must be between 0.0 and 100.0")]
    MemoryAlertThreshold {
        /// The invalid threshold value
        value: f64,
    },
}

impl DataStoreConfig {
    /// Validate the configuration for consistency.
    ///
    /// Returns `ConfigValidationError` when `default_ttl` exceeds `max_ttl` (if both are set)
    /// or when `memory_alert_threshold` is outside the range 0.0..=100.0.
    pub fn validate(&self) -> Result<(), ConfigValidationError> {
        // Check if default_ttl exceeds max_ttl
        if let (Some(default), Some(max)) = (self.default_ttl, self.max_ttl)
            && default > max
        {
            return Err(ConfigValidationError::DefaultTtlExceedsMax { default, max });
        }

        // Validate memory_alert_threshold is in valid range
        if !(0.0..=100.0).contains(&self.memory_alert_threshold) {
            return Err(ConfigValidationError::MemoryAlertThreshold {
                value: self.memory_alert_threshold,
            });
        }

        Ok(())
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
        assert!(config.enable_metrics);
        assert!(
            (config.memory_alert_threshold - 80.0).abs() < f64::EPSILON,
            "memory_alert_threshold should be 80.0"
        );
    }

    #[test]
    fn test_valid_config() {
        let config = DataStoreConfig {
            default_ttl: Some(Duration::from_secs(300)),
            max_ttl: Some(Duration::from_secs(3600)),
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when default_ttl is less than max_ttl"
        );
    }

    #[test]
    fn test_default_ttl_exceeds_max() {
        let config = DataStoreConfig {
            default_ttl: Some(Duration::from_secs(7200)), // 2 hours
            max_ttl: Some(Duration::from_secs(3600)),     // 1 hour
            ..Default::default()
        };
        assert!(
            matches!(
                config.validate(),
                Err(ConfigValidationError::DefaultTtlExceedsMax { .. })
            ),
            "expected DataStoreConfig::validate() to return ConfigValidationError when default_ttl exceeds max_ttl"
        );
    }

    #[test]
    fn test_none_ttls_are_valid() {
        let config = DataStoreConfig {
            default_ttl: None,
            max_ttl: None,
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when both TTL values are None"
        );
    }

    #[test]
    fn test_only_default_ttl_is_valid() {
        let config = DataStoreConfig {
            default_ttl: Some(Duration::from_secs(300)),
            max_ttl: None,
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when only default_ttl is set"
        );
    }

    #[test]
    fn test_only_max_ttl_is_valid() {
        let config = DataStoreConfig {
            default_ttl: None,
            max_ttl: Some(Duration::from_secs(3600)),
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when only max_ttl is set"
        );
    }

    #[test]
    fn test_memory_alert_threshold_valid() {
        let config = DataStoreConfig {
            memory_alert_threshold: 0.0,
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when memory_alert_threshold is 0.0"
        );

        let config = DataStoreConfig {
            memory_alert_threshold: 100.0,
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when memory_alert_threshold is 100.0"
        );

        let config = DataStoreConfig {
            memory_alert_threshold: 50.0,
            ..Default::default()
        };
        assert!(
            matches!(config.validate(), Ok(())),
            "expected DataStoreConfig::validate() to succeed when memory_alert_threshold is 50.0"
        );
    }

    #[test]
    fn test_memory_alert_threshold_invalid() {
        let config = DataStoreConfig {
            memory_alert_threshold: -1.0,
            ..Default::default()
        };
        assert!(
            matches!(
                config.validate(),
                Err(ConfigValidationError::MemoryAlertThreshold { .. })
            ),
            "expected DataStoreConfig::validate() to return MemoryAlertThreshold when memory_alert_threshold is < 0.0"
        );

        let config = DataStoreConfig {
            memory_alert_threshold: 101.0,
            ..Default::default()
        };
        assert!(
            matches!(
                config.validate(),
                Err(ConfigValidationError::MemoryAlertThreshold { .. })
            ),
            "expected DataStoreConfig::validate() to return MemoryAlertThreshold when memory_alert_threshold is > 100.0"
        );
    }
}
