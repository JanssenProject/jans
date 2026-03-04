// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Data API trait and supporting types for the `DataStore`.

use std::time::Duration;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use super::entry::DataEntry;
use super::error::DataError;

/// Statistics about the `DataStore`.
///
/// Provides insight into the current state and usage of the data store,
/// including memory usage metrics and capacity information.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct DataStoreStats {
    /// Number of entries currently stored
    pub entry_count: usize,
    /// Maximum number of entries allowed (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Whether metrics tracking is enabled
    pub metrics_enabled: bool,
    /// Total size of all entries in bytes (approximate, based on JSON serialization)
    pub total_size_bytes: usize,
    /// Average size per entry in bytes (0 if no entries)
    pub avg_entry_size_bytes: usize,
    /// Percentage of capacity used (0.0-100.0, based on entry count)
    pub capacity_usage_percent: f64,
    /// Memory usage threshold percentage (from config)
    pub memory_alert_threshold: f64,
    /// Whether memory usage exceeds the alert threshold
    pub memory_alert_triggered: bool,
}

/// Trait defining the public API for data store operations.
///
/// This trait provides a consistent interface for pushing, retrieving,
/// and managing data in the store. All operations are thread-safe.
///
/// The [`Cedarling`](crate::Cedarling) struct implements this trait, providing
/// access to the data store through the main application instance.
///
/// # Example
///
/// ```no_run
/// use cedarling::{Cedarling, DataApi, DataError};
/// use serde_json::json;
/// use std::time::Duration;
///
/// fn use_data_api(cedarling: &Cedarling) -> Result<(), DataError> {
///     // Push data with a 5-minute TTL
///     cedarling.push_data_ctx(
///         "user_roles",
///         json!(["admin", "editor"]),
///         Some(Duration::from_secs(300)),
///     )?;
///
///     // Retrieve data
///     if let Some(roles) = cedarling.get_data_ctx("user_roles")? {
///         println!("User roles: {}", roles);
///     }
///
///     // List all entries with metadata
///     for entry in cedarling.list_data_ctx()? {
///         println!("Key: {}, Type: {:?}", entry.key, entry.data_type);
///     }
///
///     // Get store statistics
///     let stats = cedarling.get_stats_ctx()?;
///     println!("Entries: {}/{}", stats.entry_count, stats.max_entries);
///
///     // Remove data
///     cedarling.remove_data_ctx("user_roles")?;
///
///     // Clear all data
///     cedarling.clear_data_ctx()?;
///
///     Ok(())
/// }
/// ```
pub trait DataApi {
    /// Push a value into the store with an optional TTL.
    ///
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from configuration is used.
    fn push_data_ctx(
        &self,
        key: &str,
        value: Value,
        ttl: Option<Duration>,
    ) -> Result<(), DataError>;

    /// Get a value from the store by key.
    ///
    /// Returns `Ok(None)` if the key doesn't exist or the entry has expired.
    /// If metrics are enabled, increments the access count for the entry.
    fn get_data_ctx(&self, key: &str) -> Result<Option<Value>, DataError>;

    /// Get a data entry with full metadata by key.
    ///
    /// Returns `Ok(None)` if the key doesn't exist or the entry has expired.
    /// Includes metadata like creation time, expiration, access count, and type.
    fn get_data_entry_ctx(&self, key: &str) -> Result<Option<DataEntry>, DataError>;

    /// Remove a value from the store by key.
    ///
    /// Returns `Ok(true)` if the key existed and was removed, `Ok(false)` otherwise.
    fn remove_data_ctx(&self, key: &str) -> Result<bool, DataError>;

    /// Clear all entries from the store.
    fn clear_data_ctx(&self) -> Result<(), DataError>;

    /// List all entries with their metadata.
    ///
    /// Returns a vector of `DataEntry` containing key, value, type, and timing metadata.
    fn list_data_ctx(&self) -> Result<Vec<DataEntry>, DataError>;

    /// Get statistics about the data store.
    ///
    /// Returns current entry count, capacity limits, and configuration state.
    fn get_stats_ctx(&self) -> Result<DataStoreStats, DataError>;
}
