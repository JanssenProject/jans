// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::Mutex;
use std::time::Duration as StdDuration;

use chrono::Duration as ChronoDuration;
use serde_json::Value;
use sparkv::{Config as SparKVConfig, Error as SparKVError, SparKV};

use super::config::DataStoreConfig;
use super::error::DataError;

const MUTEX_EXPECT_MESSAGE: &str = "DataStore storage mutex should unlock";

/// Thread-safe key-value data store with TTL support and capacity management.
///
/// Built on top of SparKV in-memory store for consistency with other Cedarling components.
/// Provides automatic expiration, capacity limits, and thread-safe concurrent access.
pub struct DataStore {
    storage: Mutex<SparKV<Value>>,
    config: DataStoreConfig,
}

impl DataStore {
    /// Create a new DataStore with the given configuration.
    pub fn new(config: DataStoreConfig) -> Self {
        let sparkv_config = SparKVConfig {
            max_items: config.max_entries,
            max_item_size: config.max_entry_size,
            max_ttl: config
                .max_ttl
                .map(|d| std_duration_to_chrono_duration(d))
                .unwrap_or_else(|| ChronoDuration::seconds(60 * 60)), // Default 1 hour
            default_ttl: config
                .default_ttl
                .map(|d| std_duration_to_chrono_duration(d))
                .unwrap_or_else(|| ChronoDuration::seconds(5 * 60)), // Default 5 minutes
            auto_clear_expired: true,
            earliest_expiration_eviction: false,
        };

        // Use JSON string length as size calculator for accurate size checking
        let size_calculator: Option<fn(&Value) -> usize> =
            Some(|v| serde_json::to_string(v).map(|s| s.len()).unwrap_or(0));

        Self {
            storage: Mutex::new(SparKV::with_config_and_sizer(
                sparkv_config,
                size_calculator,
            )),
            config,
        }
    }

    /// Push a value into the store with an optional TTL.
    ///
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from config will be used (if set).
    ///
    /// # Errors
    ///
    /// Returns `DataError` if:
    /// - Key is empty
    /// - Value serialization fails
    /// - Value size exceeds `max_entry_size`
    /// - Storage capacity is exceeded
    /// - TTL exceeds `max_ttl`
    pub fn push(
        &self,
        key: String,
        value: Value,
        ttl: Option<StdDuration>,
    ) -> Result<(), DataError> {
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

        // Validate TTL
        let ttl_to_use = ttl.or(self.config.default_ttl);
        if let Some(ttl_val) = ttl_to_use {
            if let Some(max_ttl) = self.config.max_ttl {
                if ttl_val > max_ttl {
                    return Err(DataError::TTLExceeded {
                        requested: ttl_val,
                        max: max_ttl,
                    });
                }
            }
        }

        // Convert std::time::Duration to chrono::Duration
        let chrono_ttl = ttl_to_use
            .map(std_duration_to_chrono_duration)
            .unwrap_or_else(|| ChronoDuration::seconds(5 * 60)); // Default 5 minutes

        let mut storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);

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
                        requested: ttl_to_use.unwrap_or_default(),
                        max: self.config.max_ttl.unwrap_or_default(),
                    }
                },
            })?;

        Ok(())
    }

    /// Get a value from the store by key.
    ///
    /// Returns `None` if the key doesn't exist or the entry has expired.
    pub fn get(&self, key: &str) -> Option<Value> {
        let storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage.get(key).cloned()
    }

    /// Remove a value from the store by key.
    ///
    /// Returns `true` if the key existed and was removed, `false` otherwise.
    pub fn remove(&self, key: &str) -> bool {
        let mut storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage.pop(key).is_some()
    }

    /// Clear all entries from the store.
    pub fn clear(&self) {
        let mut storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage.clear();
    }

    /// Get the number of entries currently in the store.
    pub fn count(&self) -> usize {
        let storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage.len()
    }

    /// List all keys currently in the store.
    pub fn list_keys(&self) -> Vec<String> {
        let storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage.get_keys()
    }

    /// Get all active (non-expired) entries as a HashMap.
    ///
    /// This is used for context injection during authorization.
    pub fn get_all(&self) -> HashMap<String, Value> {
        let storage = self.storage.lock().expect(MUTEX_EXPECT_MESSAGE);
        storage
            .iter()
            .map(|(k, v)| (k.clone(), v.clone()))
            .collect()
    }
}

/// Convert `std::time::Duration` to `chrono::Duration`.
///
/// Note: This conversion may lose precision for very large durations,
/// but should be sufficient for TTL values (typically seconds to hours).
fn std_duration_to_chrono_duration(d: StdDuration) -> ChronoDuration {
    let secs = d.as_secs();
    let nanos = d.subsec_nanos();
    ChronoDuration::seconds(secs as i64) + ChronoDuration::nanoseconds(nanos as i64)
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push simple value");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Push a complex value
        let complex_value = json!({
            "name": "test",
            "count": 42,
            "active": true
        });
        store
            .push("key2".to_string(), complex_value.clone(), None)
            .expect("failed to push complex value");
        assert_eq!(store.get("key2"), Some(complex_value));
    }

    #[test]
    fn test_push_replace_existing_key() {
        let store = create_test_store();

        store
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push initial value");
        assert_eq!(store.get("key1"), Some(json!("value1")));

        // Replace with new value
        store
            .push("key1".to_string(), json!("value2"), None)
            .expect("failed to replace value");
        assert_eq!(store.get("key1"), Some(json!("value2")));
    }

    #[test]
    fn test_push_empty_key() {
        let store = create_test_store();

        let result = store.push("".to_string(), json!("value"), None);
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
            .push("key1".to_string(), json!("value1"), None)
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push key1 for clear test");
        store
            .push("key2".to_string(), json!("value2"), None)
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push key1 for count test");
        assert_eq!(store.count(), 1);

        store
            .push("key2".to_string(), json!("value2"), None)
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push key1 for list_keys test");
        store
            .push("key2".to_string(), json!("value2"), None)
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push key1 for get_all test");
        store
            .push("key2".to_string(), json!("value2"), None)
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
            .push(
                "key1".to_string(),
                json!("value1"),
                Some(StdDuration::from_millis(100)),
            )
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
            .push("key1".to_string(), json!("value1"), None)
            .expect("failed to push key1 for max_entries test");
        store
            .push("key2".to_string(), json!("value2"), None)
            .expect("failed to push key2 for max_entries test");

        // Third entry should fail
        let result = store.push("key3".to_string(), json!("value3"), None);
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
            .push("key1".to_string(), json!("small"), None)
            .expect("failed to push small value for max_entry_size test");

        // Large value should fail
        let large_value = json!("this is a very long string that exceeds the limit");
        let result = store.push("key2".to_string(), large_value, None);
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
            .push(
                "key1".to_string(),
                json!("value1"),
                Some(StdDuration::from_secs(30)),
            )
            .expect("failed to push value with valid TTL");

        // TTL exceeding limit should fail
        let result = store.push(
            "key2".to_string(),
            json!("value2"),
            Some(StdDuration::from_secs(120)),
        );
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
            .push("key1".to_string(), json!("value1"), None)
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
            .push("str".to_string(), json!("test"), None)
            .expect("failed to push string value");
        assert_eq!(store.get("str"), Some(json!("test")));

        // Number
        store
            .push("num".to_string(), json!(42), None)
            .expect("failed to push number value");
        assert_eq!(store.get("num"), Some(json!(42)));

        // Boolean
        store
            .push("bool".to_string(), json!(true), None)
            .expect("failed to push boolean value");
        assert_eq!(store.get("bool"), Some(json!(true)));

        // Array
        store
            .push("arr".to_string(), json!([1, 2, 3]), None)
            .expect("failed to push array value");
        assert_eq!(store.get("arr"), Some(json!([1, 2, 3])));

        // Object
        let obj = json!({
            "a": 1,
            "b": "test",
            "c": [1, 2, 3]
        });
        store
            .push("obj".to_string(), obj.clone(), None)
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
                        .push(key, json!(format!("value_{}_{}", i, j)), None)
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
            store
                .push(format!("key_{}", i), json!(i), None)
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
