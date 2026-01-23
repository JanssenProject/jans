// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::RwLock;
use std::time::Duration as StdDuration;

use chrono::Duration as ChronoDuration;
use serde_json::Value;
use sparkv::{Config as SparKVConfig, Error as SparKVError, SparKV};

use super::api::{DataApi, DataStoreStats};
use super::config::{ConfigValidationError, DataStoreConfig};
use super::entry::DataEntry;
use super::error::DataError;

const RWLOCK_EXPECT_MESSAGE: &str = "DataStore storage lock should not be poisoned";

/// Effectively infinite TTL in seconds (approximately 10 years).
/// This is used when `None` is specified for TTL to mean "no automatic expiration".
/// We use 10 years instead of i64::MAX to avoid chrono Duration overflow issues.
const INFINITE_TTL_SECS: i64 = 315_360_000; // 10 years in seconds

/// Thread-safe key-value data store with TTL support and capacity management.
///
/// Built on top of SparKV in-memory store for consistency with other Cedarling components.
/// Provides automatic expiration, capacity limits, and thread-safe concurrent access.
///
/// ## TTL Semantics
///
/// - `config.default_ttl = None` means entries without explicit TTL will effectively never expire (10 years)
/// - `config.max_ttl = None` means no upper limit on TTL values (10 years max)
/// - When both `ttl` parameter and `config.default_ttl` are `None`, entries use the infinite TTL
pub(crate) struct DataStore {
    storage: RwLock<SparKV<DataEntry>>,
    config: DataStoreConfig,
}

impl DataStore {
    /// Create a new DataStore with the given configuration.
    ///
    /// ## TTL Defaults
    ///
    /// - If `config.max_ttl` is `None`, uses 10 years (effectively infinite)
    /// - If `config.default_ttl` is `None`, uses 10 years (effectively infinite)
    ///
    /// # Errors
    ///
    /// Returns `ConfigValidationError` if the configuration is invalid.
    pub(crate) fn new(config: DataStoreConfig) -> Result<Self, ConfigValidationError> {
        // Validate configuration before creating the store
        config.validate()?;

        let sparkv_config = SparKVConfig {
            max_items: config.max_entries,
            max_item_size: config.max_entry_size,
            max_ttl: config
                .max_ttl
                .map(std_duration_to_chrono_duration)
                .unwrap_or_else(|| ChronoDuration::seconds(INFINITE_TTL_SECS)),
            default_ttl: config
                .default_ttl
                .map(std_duration_to_chrono_duration)
                .unwrap_or_else(|| ChronoDuration::seconds(INFINITE_TTL_SECS)),
            auto_clear_expired: true,
            earliest_expiration_eviction: false,
        };

        // Calculate size based on the serialized DataEntry
        let size_calculator: Option<fn(&DataEntry) -> usize> =
            Some(|entry| serde_json::to_string(entry).map(|s| s.len()).unwrap_or(0));

        Ok(Self {
            storage: RwLock::new(SparKV::with_config_and_sizer(
                sparkv_config,
                size_calculator,
            )),
            config,
        })
    }

    /// Push a value into the store with an optional TTL.
    ///
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from config will be used.
    /// If both are `None`, uses infinite TTL (10 years).
    ///
    /// # Errors
    ///
    /// Returns `DataError` if:
    /// - Key is empty
    /// - Value serialization fails
    /// - Value size exceeds `max_entry_size`
    /// - Storage capacity is exceeded
    /// - TTL exceeds `max_ttl`
    pub(crate) fn push(
        &self,
        key: &str,
        value: Value,
        ttl: Option<StdDuration>,
    ) -> Result<(), DataError> {
        // Validate key
        if key.is_empty() {
            return Err(DataError::InvalidKey);
        }

        // Create DataEntry with metadata
        let entry = DataEntry::new(key.to_string(), value, ttl);

        // Check entry size before storing (including metadata)
        let entry_size = serde_json::to_string(&entry)
            .map_err(DataError::from)?
            .len();

        if self.config.max_entry_size > 0 && entry_size > self.config.max_entry_size {
            return Err(DataError::ValueTooLarge {
                size: entry_size,
                max: self.config.max_entry_size,
            });
        }

        // Validate explicit TTL against max_ttl before calculating effective TTL
        if let Some(explicit_ttl) = ttl {
            if let Some(max_ttl) = self.config.max_ttl {
                if explicit_ttl > max_ttl {
                    return Err(DataError::TTLExceeded {
                        requested: explicit_ttl,
                        max: max_ttl,
                    });
                }
            }
        }

        // Calculate effective TTL using the helper function
        let chrono_ttl = get_effective_ttl(ttl, self.config.default_ttl, self.config.max_ttl);

        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);

        // Use empty index keys since we don't need indexing for data store
        storage
            .set_with_ttl(key, entry, chrono_ttl, &[])
            .map_err(|e| match e {
                SparKVError::CapacityExceeded => DataError::StorageLimitExceeded {
                    max: self.config.max_entries,
                },
                SparKVError::ItemSizeExceeded => DataError::ValueTooLarge {
                    size: entry_size,
                    max: self.config.max_entry_size,
                },
                SparKVError::TTLTooLong => {
                    // This shouldn't happen since we validated above, but handle it anyway
                    DataError::TTLExceeded {
                        requested: ttl.unwrap_or_default(),
                        max: self
                            .config
                            .max_ttl
                            .unwrap_or(StdDuration::from_secs(INFINITE_TTL_SECS as u64)),
                    }
                },
            })?;

        Ok(())
    }

    /// Get a value from the store by key.
    ///
    /// Returns `None` if the key doesn't exist or the entry has expired.
    /// If metrics are enabled, increments the access count for the entry.
    pub(crate) fn get(&self, key: &str) -> Option<Value> {
        self.get_entry(key).map(|entry| entry.value)
    }

    /// Get a data entry with full metadata by key.
    ///
    /// Returns `None` if the key doesn't exist or the entry has expired.
    /// If metrics are enabled, increments the access count for the entry.
    /// Uses read lock initially for better concurrency, upgrading to write lock only when metrics are enabled.
    pub(crate) fn get_entry(&self, key: &str) -> Option<DataEntry> {
        // First, try with read lock for better concurrency
        let entry = {
            let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
            storage.get(key).cloned()
        };

        let mut entry = entry?;

        // Check if entry has expired
        if let Some(expires_at) = entry.expires_at {
            if chrono::Utc::now() > expires_at {
                // Entry is expired, optionally remove it from storage
                let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
                storage.pop(key);
                return None;
            }
        }

        // Only acquire write lock if metrics are enabled
        if self.config.enable_metrics {
            entry.increment_access();

            // Calculate remaining TTL to preserve expiration
            let remaining_ttl = if let Some(expires_at) = entry.expires_at {
                let now = chrono::Utc::now();
                expires_at
                    .signed_duration_since(now)
                    .to_std()
                    .ok()
                    .map(std_duration_to_chrono_duration)
                    .unwrap_or_else(|| {
                        get_effective_ttl(None, self.config.default_ttl, self.config.max_ttl)
                    })
            } else {
                // No expiration, use effective TTL
                get_effective_ttl(None, self.config.default_ttl, self.config.max_ttl)
            };

            // Acquire write lock to update the entry with incremented access count
            let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
            let _ = storage.set_with_ttl(key, entry.clone(), remaining_ttl, &[]);
        }

        Some(entry)
    }

    /// Remove a value from the store by key.
    ///
    /// Returns `true` if the key existed and was removed, `false` otherwise.
    /// Uses write lock for exclusive access.
    pub(crate) fn remove(&self, key: &str) -> bool {
        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
        storage.pop(key).is_some()
    }

    /// Clear all entries from the store.
    /// Uses write lock for exclusive access.
    pub(crate) fn clear(&self) {
        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
        storage.clear();
    }

    /// Get the number of entries currently in the store.
    /// Uses read lock for concurrent access.
    pub(crate) fn count(&self) -> usize {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage.len()
    }

    /// List all keys currently in the store.
    /// Uses read lock for concurrent access.
    pub(crate) fn list_keys(&self) -> Vec<String> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage.get_keys()
    }

    /// Get all active (non-expired) entries as a HashMap.
    ///
    /// This is used for context injection during authorization.
    /// Returns only the values, not the metadata.
    pub(crate) fn get_all(&self) -> HashMap<String, Value> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage
            .iter()
            .map(|(k, entry)| (k.clone(), entry.value.clone()))
            .collect()
    }

    /// List all entries with their full metadata, excluding expired entries.
    fn list_entries(&self) -> Vec<DataEntry> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage
            .iter()
            .filter(|(_, entry)| !entry.is_expired())
            .map(|(_, entry)| entry.clone())
            .collect()
    }
}

impl DataApi for DataStore {
    fn push_data(
        &self,
        key: &str,
        value: Value,
        ttl: Option<StdDuration>,
    ) -> Result<(), DataError> {
        self.push(key, value, ttl)
    }

    fn get_data(&self, key: &str) -> Result<Option<Value>, DataError> {
        Ok(self.get(key))
    }

    fn get_data_entry(&self, key: &str) -> Result<Option<DataEntry>, DataError> {
        Ok(self.get_entry(key))
    }

    fn remove_data(&self, key: &str) -> Result<bool, DataError> {
        Ok(self.remove(key))
    }

    fn clear_data(&self) -> Result<(), DataError> {
        self.clear();
        Ok(())
    }

    fn list_data(&self) -> Result<Vec<DataEntry>, DataError> {
        Ok(self.list_entries())
    }

    fn get_stats(&self) -> Result<DataStoreStats, DataError> {
        Ok(DataStoreStats {
            entry_count: self.count(),
            max_entries: self.config.max_entries,
            max_entry_size: self.config.max_entry_size,
            metrics_enabled: self.config.enable_metrics,
        })
    }
}

/// Convert `std::time::Duration` to `chrono::Duration`.
///
/// Uses saturating conversion to prevent overflow for very large durations.
/// Durations exceeding `i64::MAX` seconds will be capped at a safe maximum.
pub(super) fn std_duration_to_chrono_duration(d: StdDuration) -> ChronoDuration {
    let secs = d.as_secs();
    let nanos = d.subsec_nanos();

    // Saturating conversion: i64::MAX seconds is ~292 billion years
    // We cap at a safe maximum to prevent chrono panics
    // i64::MAX / 1000 (milliseconds per second) is a safe upper bound
    const MAX_SAFE_SECS: u64 = (i64::MAX / 1000) as u64;
    let secs_capped = secs.min(MAX_SAFE_SECS);

    ChronoDuration::seconds(secs_capped as i64) + ChronoDuration::nanoseconds(nanos as i64)
}

/// Get the effective TTL to use, respecting max_ttl constraints.
///
/// # TTL Resolution Logic
///
/// 1. If `ttl` is provided explicitly, use it (subject to `max_ttl` cap)
/// 2. Otherwise, use `default_ttl` from config (subject to `max_ttl` cap)
/// 3. If both are None, use effectively infinite duration (10 years)
/// 4. Always respect `max_ttl` if set, capping the result
///
/// This ensures that the effective TTL always respects `max_ttl` constraints,
/// even when using default or infinite TTLs.
fn get_effective_ttl(
    ttl: Option<StdDuration>,
    default_ttl: Option<StdDuration>,
    max_ttl: Option<StdDuration>,
) -> ChronoDuration {
    // Determine the requested TTL
    let requested_ttl = ttl.or(default_ttl);

    // If no TTL is specified, use effectively infinite duration (10 years)
    let effective = requested_ttl.unwrap_or(StdDuration::from_secs(INFINITE_TTL_SECS as u64));

    // Respect max_ttl if set, capping the result
    let capped = if let Some(max) = max_ttl {
        effective.min(max)
    } else {
        effective
    };

    std_duration_to_chrono_duration(capped)
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;
    #[cfg(not(target_arch = "wasm32"))]
    use std::thread;
    use std::time::Duration as StdDuration;

    fn create_test_store() -> DataStore {
        DataStore::new(DataStoreConfig::default()).expect("should create store")
    }

    #[test]
    fn test_push_and_get() {
        let store = create_test_store();

        // Push a simple value
        store
            .push("key1", json!("value1"), None)
            .expect("failed to push simple value");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Push a complex value
        let complex_value = json!({
            "name": "test",
            "count": 42,
            "active": true
        });
        store
            .push("key2", complex_value.clone(), None)
            .expect("failed to push complex value");
        assert_eq!(store.get("key2"), Some(complex_value));
    }

    #[test]
    fn test_push_replace_existing_key() {
        let store = create_test_store();

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push initial value");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Replace with new value
        store
            .push("key1", json!("value2"), None)
            .expect("failed to replace value");
        assert_eq!(store.get("key1"), Some(json!("value2")));
    }

    #[test]
    fn test_push_empty_key() {
        let store = create_test_store();

        let result = store.push("", json!("value"), None);
        assert!(matches!(result, Err(DataError::InvalidKey)));
    }

    #[test]
    fn test_get_nonexistent_key() {
        let store = create_test_store();

        assert_eq!(store.get("nonexistent"), None);
    }

    #[test]
    fn test_remove() {
        let store = create_test_store();

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push value for remove test");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Remove existing key
        assert!(store.remove("key1"));
        assert_eq!(store.get("key1"), None);

        // Remove non-existent key
        assert!(!store.remove("nonexistent"));
    }

    #[test]
    fn test_clear() {
        let store = create_test_store();

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push key1 for clear test");
        store
            .push("key2", json!("value2"), None)
            .expect("failed to push key2 for clear test");
        assert_eq!(store.count(), 2);

        store.clear();
        assert_eq!(store.count(), 0);
        assert_eq!(store.get("key1"), None);
        assert_eq!(store.get("key2"), None);
    }

    #[test]
    fn test_count() {
        let store = create_test_store();

        assert_eq!(store.count(), 0);

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push key1 for count test");
        assert_eq!(store.count(), 1);

        store
            .push("key2", json!("value2"), None)
            .expect("failed to push key2 for count test");
        assert_eq!(store.count(), 2);

        store.remove("key1");
        assert_eq!(store.count(), 1);
    }

    #[test]
    fn test_list_keys() {
        let store = create_test_store();

        assert!(store.list_keys().is_empty());

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push key1 for list_keys test");
        store
            .push("key2", json!("value2"), None)
            .expect("failed to push key2 for list_keys test");

        let keys = store.list_keys();
        assert_eq!(keys.len(), 2);
        assert!(keys.contains(&"key1".to_string()));
        assert!(keys.contains(&"key2".to_string()));
    }

    #[test]
    fn test_get_all() {
        let store = create_test_store();

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push key1 for get_all test");
        store
            .push("key2", json!("value2"), None)
            .expect("failed to push key2 for get_all test");

        let all = store.get_all();
        assert_eq!(all.len(), 2);
        assert_eq!(all.get("key1"), Some(&json!("value1")));
        assert_eq!(all.get("key2"), Some(&json!("value2")));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_ttl_expiration() {
        let store = create_test_store();

        // Push with very short TTL
        store
            .push("key1", json!("value1"), Some(StdDuration::from_millis(100)))
            .expect("failed to push value with TTL");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Wait for expiration
        thread::sleep(StdDuration::from_millis(150));

        // Entry should be expired
        assert_eq!(store.get("key1"), None);
    }

    #[test]
    fn test_max_entries() {
        let config = DataStoreConfig {
            max_entries: 2,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push key1 for max_entries test");
        store
            .push("key2", json!("value2"), None)
            .expect("failed to push key2 for max_entries test");

        // Third entry should fail
        let result = store.push("key3", json!("value3"), None);
        assert!(matches!(
            result,
            Err(DataError::StorageLimitExceeded { max: 2 })
        ));
    }

    #[test]
    fn test_max_entry_size() {
        let config = DataStoreConfig {
            max_entry_size: 150,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        // Small value should work
        store
            .push("key1", json!("small"), None)
            .expect("failed to push small value for max_entry_size test");

        // Large value should fail
        let large_value = json!(
            "this is a very long string that exceeds the limit and it needs to be even longer to exceed 150 bytes including metadata"
        );
        let result = store.push("key2", large_value, None);
        assert!(matches!(result, Err(DataError::ValueTooLarge { .. })));
    }

    #[test]
    fn test_max_ttl() {
        let config = DataStoreConfig {
            max_ttl: Some(StdDuration::from_secs(60)),
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        // TTL within limit should work
        store
            .push("key1", json!("value1"), Some(StdDuration::from_secs(30)))
            .expect("failed to push value with valid TTL");

        // TTL exceeding limit should fail
        let result = store.push("key2", json!("value2"), Some(StdDuration::from_secs(120)));
        assert!(matches!(result, Err(DataError::TTLExceeded { .. })));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_default_ttl() {
        let config = DataStoreConfig {
            default_ttl: Some(StdDuration::from_millis(100)),
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        // Push without explicit TTL should use default
        store
            .push("key1", json!("value1"), None)
            .expect("failed to push value with default TTL");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Wait for expiration
        thread::sleep(StdDuration::from_millis(150));

        // Entry should be expired
        assert_eq!(store.get("key1"), None);
    }

    #[test]
    fn test_various_json_types() {
        let store = create_test_store();

        // String
        store
            .push("str", json!("test"), None)
            .expect("failed to push string value");
        assert_eq!(store.get("str"), Some(json!("test")));

        // Number
        store
            .push("num", json!(42), None)
            .expect("failed to push number value");
        assert_eq!(store.get("num"), Some(json!(42)));

        // Boolean
        store
            .push("bool", json!(true), None)
            .expect("failed to push boolean value");
        assert_eq!(store.get("bool"), Some(json!(true)));

        // Array
        store
            .push("arr", json!([1, 2, 3]), None)
            .expect("failed to push array value");
        assert_eq!(store.get("arr"), Some(json!([1, 2, 3])));

        // Object
        let obj = json!({
            "a": 1,
            "b": "test",
            "c": [1, 2, 3]
        });
        store
            .push("obj", obj.clone(), None)
            .expect("failed to push object value");
        assert_eq!(store.get("obj"), Some(obj));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_thread_safety() {
        let store = create_test_store();
        let store = std::sync::Arc::new(store);

        let mut handles = vec![];

        // Spawn multiple threads pushing values
        for i in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for j in 0..10 {
                    let key = format!("key_{}_{}", i, j);
                    store_clone
                        .push(&key, json!(format!("value_{}_{}", i, j)), None)
                        .expect("failed to push value in thread");
                }
            });
            handles.push(handle);
        }

        // Wait for all threads
        for handle in handles {
            handle.join().expect("thread panicked");
        }

        // Verify all entries are present
        assert_eq!(store.count(), 100);

        // Verify we can read from all threads
        let mut read_handles = vec![];
        for i in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for j in 0..10 {
                    let key = format!("key_{}_{}", i, j);
                    let expected = json!(format!("value_{}_{}", i, j));
                    assert_eq!(store_clone.get(&key), Some(expected));
                }
            });
            read_handles.push(handle);
        }

        for handle in read_handles {
            handle.join().expect("read thread panicked");
        }
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_concurrent_remove() {
        let store = create_test_store();
        let store = std::sync::Arc::new(store);

        // Populate store
        for i in 0..20 {
            let key = format!("key_{}", i);
            store
                .push(&key, json!(i), None)
                .expect("failed to populate store for concurrent remove test");
        }

        let mut handles = vec![];

        // Spawn threads that remove entries
        for i in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                store_clone.remove(&format!("key_{}", i));
            });
            handles.push(handle);
        }

        for handle in handles {
            handle.join().expect("remove thread panicked");
        }

        // Verify some entries were removed
        assert!(store.count() < 20);
    }

    #[test]
    fn test_get_entry_with_metadata() {
        let store = create_test_store();

        store
            .push("key1", json!("value1"), Some(StdDuration::from_secs(60)))
            .expect("failed to push value");

        let entry = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry.key, "key1");
        assert_eq!(entry.value, json!("value1"));
        assert_eq!(entry.data_type, crate::CedarType::String);
        assert_eq!(entry.access_count, 1); // Incremented by get_entry
        assert!(entry.expires_at.is_some());
    }

    #[test]
    fn test_metrics_tracking() {
        let config = DataStoreConfig {
            enable_metrics: true,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push value");

        // First access
        let entry1 = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry1.access_count, 1);

        // Second access
        let entry2 = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry2.access_count, 2);

        // Third access
        let entry3 = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry3.access_count, 3);
    }

    #[test]
    fn test_metrics_disabled() {
        let config = DataStoreConfig {
            enable_metrics: false,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        store
            .push("key1", json!("value1"), None)
            .expect("failed to push value");

        // Access multiple times
        let entry1 = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry1.access_count, 0); // Not incremented

        let entry2 = store.get_entry("key1").expect("entry should exist");
        assert_eq!(entry2.access_count, 0); // Still not incremented
    }

    #[test]
    fn test_cedar_type_inference() {
        let store = create_test_store();

        store
            .push("string", json!("test"), None)
            .expect("failed to push string");
        store
            .push("number", json!(42), None)
            .expect("failed to push number");
        store
            .push("bool", json!(true), None)
            .expect("failed to push bool");
        store
            .push("array", json!([1, 2, 3]), None)
            .expect("failed to push array");
        store
            .push("object", json!({"key": "value"}), None)
            .expect("failed to push object");
        store
            .push("entity", json!({"type": "User", "id": "123"}), None)
            .expect("failed to push entity");

        use crate::CedarType;
        assert_eq!(
            store.get_entry("string").unwrap().data_type,
            CedarType::String
        );
        assert_eq!(
            store.get_entry("number").unwrap().data_type,
            CedarType::Long
        );
        assert_eq!(store.get_entry("bool").unwrap().data_type, CedarType::Bool);
        assert_eq!(store.get_entry("array").unwrap().data_type, CedarType::Set);
        assert_eq!(
            store.get_entry("object").unwrap().data_type,
            CedarType::Record
        );
        assert_eq!(
            store.get_entry("entity").unwrap().data_type,
            CedarType::Entity
        );
    }

    #[test]
    fn test_config_validation() {
        // Valid config
        let valid_config = DataStoreConfig {
            default_ttl: Some(StdDuration::from_secs(300)),
            max_ttl: Some(StdDuration::from_secs(3600)),
            ..Default::default()
        };
        assert!(
            matches!(DataStore::new(valid_config), Ok(_)),
            "expected DataStore::new() to succeed with valid DataStoreConfig"
        );

        // Invalid config: default_ttl > max_ttl
        let invalid_config = DataStoreConfig {
            default_ttl: Some(StdDuration::from_secs(7200)),
            max_ttl: Some(StdDuration::from_secs(3600)),
            ..Default::default()
        };
        assert!(
            matches!(
                DataStore::new(invalid_config),
                Err(ConfigValidationError::DefaultTtlExceedsMax { .. })
            ),
            "expected DataStore::new() to return ConfigValidationError when default_ttl exceeds max_ttl"
        );
    }

    // ==========================================================================
    // DataApi trait tests
    // ==========================================================================

    #[test]
    fn test_data_api_push_and_get() {
        let store = create_test_store();

        // Use trait methods
        store
            .push_data("api_key", json!({"role": "admin"}), None)
            .expect("push_data should succeed");

        let result = store.get_data("api_key").expect("get_data should succeed");
        assert_eq!(result, Some(json!({"role": "admin"})));

        // Non-existent key
        let missing = store
            .get_data("nonexistent")
            .expect("get_data should succeed for missing key");
        assert_eq!(missing, None);
    }

    #[test]
    fn test_data_api_get_entry() {
        let store = create_test_store();

        store
            .push_data(
                "entry_key",
                json!("test_value"),
                Some(StdDuration::from_secs(600)),
            )
            .expect("push_data should succeed");

        let entry = store
            .get_data_entry("entry_key")
            .expect("get_data_entry should succeed")
            .expect("entry should exist");

        assert_eq!(entry.key, "entry_key");
        assert_eq!(entry.value, json!("test_value"));
        assert!(entry.expires_at.is_some());
    }

    #[test]
    fn test_data_api_remove() {
        let store = create_test_store();

        store
            .push_data("to_remove", json!(123), None)
            .expect("push_data should succeed");

        let removed = store
            .remove_data("to_remove")
            .expect("remove_data should succeed");
        assert!(removed, "should return true when key existed");

        let removed_again = store
            .remove_data("to_remove")
            .expect("remove_data should succeed");
        assert!(!removed_again, "should return false when key doesn't exist");

        assert_eq!(
            store
                .get_data("to_remove")
                .expect("get_data should succeed"),
            None
        );
    }

    #[test]
    fn test_data_api_clear() {
        let store = create_test_store();

        store
            .push_data("key1", json!(1), None)
            .expect("push_data should succeed");
        store
            .push_data("key2", json!(2), None)
            .expect("push_data should succeed");
        store
            .push_data("key3", json!(3), None)
            .expect("push_data should succeed");

        assert_eq!(store.count(), 3);

        store.clear_data().expect("clear_data should succeed");

        assert_eq!(store.count(), 0);
    }

    #[test]
    fn test_data_api_list() {
        let store = create_test_store();

        store
            .push_data("alpha", json!("a"), None)
            .expect("push_data should succeed");
        store
            .push_data("beta", json!("b"), None)
            .expect("push_data should succeed");

        let entries = store.list_data().expect("list_data should succeed");

        assert_eq!(entries.len(), 2);

        let keys: Vec<&str> = entries.iter().map(|e| e.key.as_str()).collect();
        assert!(keys.contains(&"alpha"));
        assert!(keys.contains(&"beta"));
    }

    #[test]
    fn test_data_api_stats() {
        let config = DataStoreConfig {
            max_entries: 100,
            max_entry_size: 512,
            enable_metrics: true,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        store
            .push_data("stat_key", json!("value"), None)
            .expect("push_data should succeed");

        let stats = store.get_stats().expect("get_stats should succeed");

        assert_eq!(stats.entry_count, 1);
        assert_eq!(stats.max_entries, 100);
        assert_eq!(stats.max_entry_size, 512);
        assert!(stats.metrics_enabled);
    }

    #[test]
    fn test_data_api_ttl_handling() {
        let store = create_test_store();

        // Push with explicit TTL
        store
            .push_data(
                "ttl_key",
                json!("expires_soon"),
                Some(StdDuration::from_secs(60)),
            )
            .expect("push_data with TTL should succeed");

        let entry = store
            .get_data_entry("ttl_key")
            .expect("get_data_entry should succeed")
            .expect("entry should exist");

        assert!(
            entry.expires_at.is_some(),
            "entry should have expiration time"
        );
    }

    #[test]
    fn test_data_api_ttl_exceeded_error() {
        let config = DataStoreConfig {
            max_ttl: Some(StdDuration::from_secs(60)), // 1 minute max
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        // Try to push with TTL exceeding max
        let result = store.push_data(
            "long_ttl",
            json!("value"),
            Some(StdDuration::from_secs(120)), // 2 minutes
        );

        assert!(
            matches!(result, Err(DataError::TTLExceeded { .. })),
            "expected TTLExceeded error when TTL exceeds max_ttl"
        );
    }
}
