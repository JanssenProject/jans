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

use super::config::DataStoreConfig;
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
pub struct DataStore {
    storage: RwLock<SparKV<Value>>,
    config: DataStoreConfig,
}

impl DataStore {
    /// Create a new DataStore with the given configuration.
    ///
    /// ## TTL Defaults
    ///
    /// - If `config.max_ttl` is `None`, uses 10 years (effectively infinite)
    /// - If `config.default_ttl` is `None`, uses 10 years (effectively infinite)
    pub fn new(config: DataStoreConfig) -> Self {
        let sparkv_config = SparKVConfig {
            max_items: config.max_entries,
            max_item_size: config.max_entry_size,
            max_ttl: config
                .max_ttl
                .map(|d| std_duration_to_chrono_duration(d))
                .unwrap_or_else(|| ChronoDuration::seconds(INFINITE_TTL_SECS)),
            default_ttl: config
                .default_ttl
                .map(|d| std_duration_to_chrono_duration(d))
                .unwrap_or_else(|| ChronoDuration::seconds(INFINITE_TTL_SECS)),
            auto_clear_expired: true,
            earliest_expiration_eviction: false,
        };

        // Use JSON string length as size calculator for accurate size checking
        let size_calculator: Option<fn(&Value) -> usize> =
            Some(|v| serde_json::to_string(v).map(|s| s.len()).unwrap_or(0));

        Self {
            storage: RwLock::new(SparKV::with_config_and_sizer(
                sparkv_config,
                size_calculator,
            )),
            config,
        }
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
    pub fn push(&self, key: &str, value: Value, ttl: Option<StdDuration>) -> Result<(), DataError> {
        // Validate key
        if key.is_empty() {
            return Err(DataError::InvalidKey);
        }

        // Check value size before storing
        let value_size = serde_json::to_string(&value)
            .map_err(DataError::from)?
            .len();

        if self.config.max_entry_size > 0 && value_size > self.config.max_entry_size {
            return Err(DataError::ValueTooLarge {
                size: value_size,
                max: self.config.max_entry_size,
            });
        }

        // Determine the effective TTL to use
        // Priority: explicit ttl > config.default_ttl > infinite (10 years)
        let requested_ttl = ttl
            .or(self.config.default_ttl)
            .unwrap_or(StdDuration::from_secs(INFINITE_TTL_SECS as u64));

        // If an explicit TTL was provided, validate it against max_ttl
        if ttl.is_some() {
            if let Some(max_ttl) = self.config.max_ttl {
                if requested_ttl > max_ttl {
                    return Err(DataError::TTLExceeded {
                        requested: requested_ttl,
                        max: max_ttl,
                    });
                }
            }
        }

        // Cap the effective TTL at max_ttl if set
        let effective_ttl = if let Some(max_ttl) = self.config.max_ttl {
            requested_ttl.min(max_ttl)
        } else {
            requested_ttl
        };

        // Convert to chrono::Duration
        let chrono_ttl = std_duration_to_chrono_duration(effective_ttl);

        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);

        // Use empty index keys since we don't need indexing for data store
        storage
            .set_with_ttl(&key, value, chrono_ttl, &[])
            .map_err(|e| match e {
                SparKVError::CapacityExceeded => DataError::StorageLimitExceeded {
                    max: self.config.max_entries,
                },
                SparKVError::ItemSizeExceeded => DataError::ValueTooLarge {
                    size: value_size,
                    max: self.config.max_entry_size,
                },
                SparKVError::TTLTooLong => {
                    // This shouldn't happen since we validated above, but handle it anyway
                    DataError::TTLExceeded {
                        requested: effective_ttl,
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
    /// Uses read lock for concurrent access.
    pub fn get(&self, key: &str) -> Option<Value> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage.get(key).cloned()
    }

    /// Remove a value from the store by key.
    ///
    /// Returns `true` if the key existed and was removed, `false` otherwise.
    /// Uses write lock for exclusive access.
    pub fn remove(&self, key: &str) -> bool {
        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
        storage.pop(key).is_some()
    }

    /// Clear all entries from the store.
    /// Uses write lock for exclusive access.
    pub fn clear(&self) {
        let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
        storage.clear();
    }

    /// Get the number of entries currently in the store.
    /// Uses read lock for concurrent access.
    pub fn count(&self) -> usize {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage.len()
    }

    /// List all keys currently in the store.
    /// Uses read lock for concurrent access.
    pub fn list_keys(&self) -> Vec<String> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage.get_keys()
    }

    /// Get all active (non-expired) entries as a HashMap.
    ///
    /// This is used for context injection during authorization.
    /// Uses read lock for concurrent access.
    pub fn get_all(&self) -> HashMap<String, Value> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage
            .iter()
            .map(|(k, v)| (k.clone(), v.clone()))
            .collect()
    }
}

/// Convert `std::time::Duration` to `chrono::Duration`.
///
/// Uses saturating conversion to prevent overflow for very large durations.
/// Durations exceeding `i64::MAX` seconds will be capped at a safe maximum.
fn std_duration_to_chrono_duration(d: StdDuration) -> ChronoDuration {
    let secs = d.as_secs();
    let nanos = d.subsec_nanos();

    // Saturating conversion: i64::MAX seconds is ~292 billion years
    // We cap at a safe maximum to prevent chrono panics
    // i64::MAX / 1000 (milliseconds per second) is a safe upper bound
    const MAX_SAFE_SECS: u64 = (i64::MAX / 1000) as u64;
    let secs_capped = secs.min(MAX_SAFE_SECS);

    ChronoDuration::seconds(secs_capped as i64) + ChronoDuration::nanoseconds(nanos as i64)
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;
    #[cfg(not(target_arch = "wasm32"))]
    use std::thread;
    use std::time::Duration as StdDuration;

    fn create_test_store() -> DataStore {
        DataStore::new(DataStoreConfig::default())
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
        let store = DataStore::new(config);

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
            max_entry_size: 10,
            ..Default::default()
        };
        let store = DataStore::new(config);

        // Small value should work
        store
            .push("key1", json!("small"), None)
            .expect("failed to push small value for max_entry_size test");

        // Large value should fail
        let large_value = json!("this is a very long string that exceeds the limit");
        let result = store.push("key2", large_value, None);
        assert!(matches!(result, Err(DataError::ValueTooLarge { .. })));
    }

    #[test]
    fn test_max_ttl() {
        let config = DataStoreConfig {
            max_ttl: Some(StdDuration::from_secs(60)),
            ..Default::default()
        };
        let store = DataStore::new(config);

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
        let store = DataStore::new(config);

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
}
