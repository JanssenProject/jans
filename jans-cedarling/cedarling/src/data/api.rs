// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Data API trait and supporting types for the DataStore.

use std::time::Duration;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use super::entry::DataEntry;
use super::error::DataError;

/// Statistics about the DataStore.
///
/// Provides insight into the current state and usage of the data store.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct DataStoreStats {
    /// Number of entries currently stored
    pub entry_count: usize,
    /// Maximum number of entries allowed (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Whether metrics tracking is enabled
    pub metrics_enabled: bool,
}

/// Trait defining the public API for data store operations.
///
/// This trait provides a consistent interface for pushing, retrieving,
/// and managing data in the store. All operations are thread-safe.
///
/// # Examples
///
/// ```ignore
/// use cedarling::data::{DataApi, DataStore, DataStoreConfig};
/// use serde_json::json;
/// use std::time::Duration;
///
/// let store = DataStore::new(DataStoreConfig::default())?;
///
/// // Push data with TTL
/// store.push_data("user_roles", json!(["admin", "editor"]), Some(Duration::from_secs(300)))?;
///
/// // Retrieve data
/// if let Some(roles) = store.get_data("user_roles")? {
///     println!("User roles: {}", roles);
/// }
///
/// // List all entries with metadata
/// for entry in store.list_data()? {
///     println!("Key: {}, Type: {:?}", entry.key, entry.data_type);
/// }
///
/// // Get store statistics
/// let stats = store.get_stats()?;
/// println!("Entries: {}/{}", stats.entry_count, stats.max_entries);
/// ```
pub trait DataApi {
    /// Push a value into the store with an optional TTL.
    ///
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from configuration is used.
    fn push_data(&self, key: &str, value: Value, ttl: Option<Duration>) -> Result<(), DataError>;

    /// Get a value from the store by key.
    ///
    /// Returns `Ok(None)` if the key doesn't exist or the entry has expired.
    /// If metrics are enabled, increments the access count for the entry.
    fn get_data(&self, key: &str) -> Result<Option<Value>, DataError>;

    /// Get a data entry with full metadata by key.
    ///
    /// Returns `Ok(None)` if the key doesn't exist or the entry has expired.
    /// Includes metadata like creation time, expiration, access count, and type.
    fn get_data_entry(&self, key: &str) -> Result<Option<DataEntry>, DataError>;

    /// Remove a value from the store by key.
    ///
    /// Returns `Ok(true)` if the key existed and was removed, `Ok(false)` otherwise.
    fn remove_data(&self, key: &str) -> Result<bool, DataError>;

    /// Clear all entries from the store.
    fn clear_data(&self) -> Result<(), DataError>;

    /// List all entries with their metadata.
    ///
    /// Returns a vector of `DataEntry` containing key, value, type, and timing metadata.
    fn list_data(&self) -> Result<Vec<DataEntry>, DataError>;

    /// Get statistics about the data store.
    ///
    /// Returns current entry count, capacity limits, and configuration state.
    fn get_stats(&self) -> Result<DataStoreStats, DataError>;
}
