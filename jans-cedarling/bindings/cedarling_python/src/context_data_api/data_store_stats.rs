/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;

/// DataStoreStats
/// ==============
///
/// Statistics about the DataStore.
///
/// Attributes
/// ----------
/// entry_count : int
///     Number of entries currently stored
/// max_entries : int
///     Maximum number of entries allowed (0 = unlimited)
/// max_entry_size : int
///     Maximum size per entry in bytes (0 = unlimited)
/// metrics_enabled : bool
///     Whether metrics tracking is enabled
/// total_size_bytes : int
///     Total size of all entries in bytes (approximate, based on JSON serialization)
/// avg_entry_size_bytes : int
///     Average size per entry in bytes (0 if no entries)
/// capacity_usage_percent : float
///     Percentage of capacity used (0.0-100.0, based on entry count)
/// memory_alert_threshold : float
///     Memory usage threshold percentage (from config)
/// memory_alert_triggered : bool
///     Whether memory usage exceeds the alert threshold
#[derive(Debug, Clone)]
#[pyclass(get_all)]
pub struct DataStoreStats {
    /// Number of entries currently stored
    entry_count: usize,
    /// Maximum number of entries allowed (0 = unlimited)
    max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    max_entry_size: usize,
    /// Whether metrics tracking is enabled
    metrics_enabled: bool,
    /// Total size of all entries in bytes (approximate, based on JSON serialization)
    total_size_bytes: usize,
    /// Average size per entry in bytes (0 if no entries)
    avg_entry_size_bytes: usize,
    /// Percentage of capacity used (0.0-100.0, based on entry count)
    capacity_usage_percent: f64,
    /// Memory usage threshold percentage (from config)
    memory_alert_threshold: f64,
    /// Whether memory usage exceeds the alert threshold
    memory_alert_triggered: bool,
}

impl From<cedarling::DataStoreStats> for DataStoreStats {
    fn from(value: cedarling::DataStoreStats) -> Self {
        Self {
            entry_count: value.entry_count,
            max_entries: value.max_entries,
            max_entry_size: value.max_entry_size,
            metrics_enabled: value.metrics_enabled,
            total_size_bytes: value.total_size_bytes,
            avg_entry_size_bytes: value.avg_entry_size_bytes,
            capacity_usage_percent: value.capacity_usage_percent,
            memory_alert_threshold: value.memory_alert_threshold,
            memory_alert_triggered: value.memory_alert_triggered,
        }
    }
}
