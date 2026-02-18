// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::RwLock;
use std::time::Duration as StdDuration;

use chrono::{Duration as ChronoDuration, Utc};
use serde_json::Value;
use sparkv::{Config as SparKVConfig, Error as SparKVError, SparKV};

use super::config::{ConfigValidationError, DataStoreConfig};
use super::entry::DataEntry;
use super::error::DataError;

const RWLOCK_EXPECT_MESSAGE: &str = "DataStore storage lock should not be poisoned";

/// Effectively infinite TTL in seconds (approximately 10 years).
/// This is used when `None` is specified for TTL to mean "no automatic expiration".
/// We use 10 years instead of `i64::MAX` to avoid chrono Duration overflow issues.
const INFINITE_TTL_SECS: i64 = 315_360_000; // 10 years in seconds

/// Maximum safe seconds value for duration conversion.
/// `i64::MAX / 1000` ensures we can safely add nanoseconds without overflow.
/// This is approximately 9.2 quadrillion seconds (~292 billion years).
const MAX_SAFE_DURATION_SECS: u64 = (i64::MAX / 1000) as u64;

/// Thread-safe key-value data store with TTL support and capacity management.
///
/// Built on top of `SparKV` in-memory store for consistency with other Cedarling components.
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
    /// Create a new `DataStore` with the given configuration.
    ///
    /// ## TTL Defaults
    ///
    /// - If `config.max_ttl` is `None`, uses 10 years (effectively infinite)
    /// - If `config.default_ttl` is `None`, uses 10 years (effectively infinite)
    ///
    /// Returns `ConfigValidationError` if the configuration is invalid.
    pub(crate) fn new(config: DataStoreConfig) -> Result<Self, ConfigValidationError> {
        // Validate configuration before creating the store
        config.validate()?;

        let sparkv_config = SparKVConfig {
            max_items: config.max_entries,
            max_item_size: config.max_entry_size,
            max_ttl: config.max_ttl.map_or_else(
                || ChronoDuration::seconds(INFINITE_TTL_SECS),
                std_duration_to_chrono_duration,
            ),
            default_ttl: config.default_ttl.map_or_else(
                || ChronoDuration::seconds(INFINITE_TTL_SECS),
                std_duration_to_chrono_duration,
            ),
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

        // Validate explicit TTL against max_ttl before calculating effective TTL
        if let Some(explicit_ttl) = ttl
            && let Some(max_ttl) = self.config.max_ttl
            && explicit_ttl > max_ttl
        {
            return Err(DataError::TTLExceeded {
                requested: explicit_ttl,
                max: max_ttl,
            });
        }

        // Calculate effective TTL using the helper function
        let effective_ttl_chrono = get_effective_ttl(ttl, self.config.default_ttl, self.config.max_ttl);
        
        // Convert back to StdDuration for DataEntry::new
        // chrono::Duration can be converted to std::time::Duration via num_seconds
        let effective_ttl_std = if effective_ttl_chrono.num_seconds() >= 0 {
            StdDuration::from_secs(effective_ttl_chrono.num_seconds() as u64)
        } else {
            StdDuration::ZERO
        };

        // Create DataEntry with metadata using effective TTL
        let entry = DataEntry::new(key.to_string(), value, Some(effective_ttl_std));

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

        let chrono_ttl = effective_ttl_chrono;

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
    /// When metrics are enabled, uses write lock up-front to avoid TOCTOU issues.
    pub(crate) fn get_entry(&self, key: &str) -> Option<DataEntry> {
        if self.config.enable_metrics {
            // Acquire write lock up-front when metrics are enabled to avoid TOCTOU
            let mut storage = self.storage.write().expect(RWLOCK_EXPECT_MESSAGE);
            let mut entry = storage.get(key)?.clone();

            // Check if entry has expired
            if let Some(expires_at) = entry.expires_at
                && chrono::Utc::now() > expires_at
            {
                storage.pop(key);
                return None;
            }

            entry.increment_access();

            // Calculate remaining TTL to preserve expiration
            let remaining_ttl = if let Some(expires_at) = entry.expires_at {
                let now = chrono::Utc::now();
                expires_at
                    .signed_duration_since(now)
                    .to_std()
                    .ok()
                    .map_or_else(
                        || get_effective_ttl(None, self.config.default_ttl, self.config.max_ttl),
                        std_duration_to_chrono_duration,
                    )
            } else {
                // No expiration, use effective TTL
                get_effective_ttl(None, self.config.default_ttl, self.config.max_ttl)
            };

            let _ = storage.set_with_ttl(key, entry.clone(), remaining_ttl, &[]);
            Some(entry)
        } else {
            // Fast path: use read lock when metrics are disabled
            let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
            let entry = storage.get(key)?.clone();

            // Check if entry has expired
            if let Some(expires_at) = entry.expires_at
                && chrono::Utc::now() > expires_at
            {
                return None;
            }

            Some(entry)
        }
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

    /// Get all active (non-expired) entries as a `HashMap`.
    ///
    /// This is used for context injection during authorization.
    /// Returns only the values, not the metadata.
    /// Filters out expired entries to prevent leaking expired items into authorization contexts.
    pub(crate) fn get_all(&self) -> HashMap<String, Value> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        let now = chrono::Utc::now();
        storage
            .iter()
            .filter(|(_, entry)| !entry.is_expired(now))
            .map(|(k, entry)| (k.clone(), entry.value.clone()))
            .collect()
    }

    /// List all entries with their full metadata, excluding expired entries.
    pub(crate) fn list_entries(&self) -> Vec<DataEntry> {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        let now = Utc::now();
        storage
            .iter()
            .filter(|(_, entry)| !entry.is_expired(now))
            .map(|(_, entry)| entry.clone())
            .collect()
    }

    /// Get the configuration for this store.
    pub(crate) fn config(&self) -> &DataStoreConfig {
        &self.config
    }

    /// Calculate the total size of all entries in bytes.
    ///
    /// Size is computed based on JSON serialization of each entry.
    pub(crate) fn total_size(&self) -> usize {
        let storage = self.storage.read().expect(RWLOCK_EXPECT_MESSAGE);
        storage
            .iter()
            .map(|(_, entry)| serde_json::to_string(entry).map(|s| s.len()).unwrap_or(0))
            .sum()
    }
}

/// Convert `std::time::Duration` to `chrono::Duration`.
///
/// Uses saturating conversion to prevent overflow for very large durations.
/// Durations exceeding `i64::MAX` seconds will be capped at a safe maximum.
pub(super) fn std_duration_to_chrono_duration(d: StdDuration) -> ChronoDuration {
    let secs = d.as_secs();
    let nanos = d.subsec_nanos();

    // Saturating conversion: cap at MAX_SAFE_DURATION_SECS to prevent chrono panics.
    // After capping, the value is guaranteed to fit in i64.
    let secs_capped = secs.min(MAX_SAFE_DURATION_SECS);

    // SAFETY: secs_capped <= MAX_SAFE_DURATION_SECS < i64::MAX, so this cast is safe
    #[allow(clippy::cast_possible_wrap)]
    let secs_i64 = secs_capped as i64;

    ChronoDuration::seconds(secs_i64) + ChronoDuration::nanoseconds(i64::from(nanos))
}

/// Get the effective TTL to use, respecting `max_ttl` constraints.
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
    use test_utils::assert_eq;

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
        assert!(
            matches!(result, Err(DataError::InvalidKey)),
            "push with empty key should return DataError::InvalidKey"
        );
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
                    let key = format!("key_{i}_{j}");
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
                    let key = format!("key_{i}_{j}");
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
            let key = format!("key_{i}");
            store
                .push(&key, json!(i), None)
                .expect("failed to populate store for concurrent remove test");
        }

        let mut handles = vec![];

        // Spawn threads that remove entries
        for i in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                store_clone.remove(&format!("key_{i}"));
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
        use crate::CedarType;
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
            DataStore::new(valid_config).is_ok(),
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
    // Additional store method tests
    // ==========================================================================

    #[test]
    fn test_list_entries() {
        let store = create_test_store();

        store
            .push("alpha", json!("a"), None)
            .expect("push should succeed");
        store
            .push("beta", json!("b"), None)
            .expect("push should succeed");

        let entries = store.list_entries();

        assert_eq!(entries.len(), 2);

        let keys: Vec<&str> = entries.iter().map(|e| e.key.as_str()).collect();
        assert!(keys.contains(&"alpha"));
        assert!(keys.contains(&"beta"));
    }

    #[test]
    fn test_config_accessor() {
        let config = DataStoreConfig {
            max_entries: 100,
            max_entry_size: 512,
            enable_metrics: true,
            ..Default::default()
        };
        let store = DataStore::new(config).expect("should create store");

        let retrieved_config = store.config();

        assert_eq!(retrieved_config.max_entries, 100);
        assert_eq!(retrieved_config.max_entry_size, 512);
        assert!(retrieved_config.enable_metrics);
    }

    // ==========================================================================
    // Context injection tests
    // ==========================================================================

    #[test]
    fn test_get_all_returns_all_values() {
        let store = create_test_store();

        store
            .push("user_role", json!("admin"), None)
            .expect("push should succeed");
        store
            .push(
                "feature_flags",
                json!({"dark_mode": true, "beta": false}),
                None,
            )
            .expect("push should succeed");
        store
            .push("rate_limit", json!(100), None)
            .expect("push should succeed");

        let all_data = store.get_all();

        assert_eq!(all_data.len(), 3);
        assert_eq!(all_data.get("user_role"), Some(&json!("admin")));
        assert_eq!(
            all_data.get("feature_flags"),
            Some(&json!({"dark_mode": true, "beta": false}))
        );
        assert_eq!(all_data.get("rate_limit"), Some(&json!(100)));
    }

    #[test]
    fn test_get_all_empty_store() {
        let store = create_test_store();
        let all_data = store.get_all();
        assert!(all_data.is_empty());
    }

    #[test]
    fn test_get_all_returns_values_not_metadata() {
        let store = create_test_store();

        store
            .push("key", json!({"nested": {"value": 42}}), None)
            .expect("push should succeed");

        let all_data = store.get_all();

        // get_all should return just the value, not the DataEntry wrapper
        let value = all_data.get("key").expect("key should exist");
        assert_eq!(value, &json!({"nested": {"value": 42}}));
    }

    #[test]
    fn test_get_all_suitable_for_context_injection() {
        let store = create_test_store();

        // Simulate typical context data
        store
            .push("device_type", json!("mobile"), None)
            .expect("push should succeed");
        store
            .push("geo", json!({"country": "US", "region": "CA"}), None)
            .expect("push should succeed");
        store
            .push("permissions", json!(["read", "write"]), None)
            .expect("push should succeed");

        let all_data = store.get_all();

        // Convert to JSON Value (as would be done in context building)
        let data_value: Value = Value::Object(all_data.into_iter().collect());

        // Verify structure is suitable for Cedar context
        assert!(data_value.is_object());
        let obj = data_value.as_object().unwrap();
        assert_eq!(obj.get("device_type"), Some(&json!("mobile")));
        assert_eq!(
            obj.get("geo"),
            Some(&json!({"country": "US", "region": "CA"}))
        );
        assert_eq!(obj.get("permissions"), Some(&json!(["read", "write"])));
    }

    #[test]
    fn test_total_size_calculation() {
        let store = create_test_store();

        // Empty store should have 0 size
        assert_eq!(store.total_size(), 0);

        // Add some entries
        store
            .push("key1", json!("short"), None)
            .expect("push should succeed");
        let size_after_one = store.total_size();
        assert!(
            size_after_one > 0,
            "size should be positive after adding entry"
        );

        store
            .push("key2", json!({"nested": {"data": "value"}}), None)
            .expect("push should succeed");
        let size_after_two = store.total_size();
        assert!(
            size_after_two > size_after_one,
            "size should increase after adding more entries"
        );

        // Remove an entry, size should decrease
        store.remove("key1");
        let size_after_remove = store.total_size();
        assert!(
            size_after_remove < size_after_two,
            "size should decrease after removing entry"
        );
    }

    #[test]
    fn test_memory_alert_threshold_validation() {
        // Valid threshold
        let config = DataStoreConfig {
            memory_alert_threshold: 80.0,
            ..Default::default()
        };
        assert!(config.validate().is_ok());

        // Edge case: 0%
        let config_zero = DataStoreConfig {
            memory_alert_threshold: 0.0,
            ..Default::default()
        };
        assert!(config_zero.validate().is_ok());

        // Edge case: 100%
        let config_hundred = DataStoreConfig {
            memory_alert_threshold: 100.0,
            ..Default::default()
        };
        assert!(config_hundred.validate().is_ok());

        // Invalid: negative
        let config_negative = DataStoreConfig {
            memory_alert_threshold: -1.0,
            ..Default::default()
        };
        assert!(
            matches!(config_negative.validate(), Err(_)),
            "memory_alert_threshold = -1.0 should fail validation"
        );

        // Invalid: over 100%
        let config_over = DataStoreConfig {
            memory_alert_threshold: 101.0,
            ..Default::default()
        };
        assert!(
            matches!(config_over.validate(), Err(_)),
            "memory_alert_threshold = 101.0 should fail validation"
        );
    }

    // ============================================================
    // Concurrent Access and Stress Tests
    // ============================================================

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_stress_concurrent_read_write() {
        use std::sync::Arc;
        use std::thread;

        let store = Arc::new(create_test_store());
        let mut handles = vec![];

        // 5 writer threads, each writing 100 entries
        for writer_id in 0..5 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for i in 0..100 {
                    let key = format!("writer_{writer_id}_key_{i}");
                    store_clone
                        .push(&key, json!({"writer": writer_id, "value": i}), None)
                        .expect("concurrent push should succeed");
                }
            });
            handles.push(handle);
        }

        // 10 reader threads, each doing 200 reads
        for reader_id in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for i in 0..200 {
                    // Try to read various keys (some may exist, some may not)
                    let key = format!("writer_{}_key_{}", reader_id % 5, i % 100);
                    let _ = store_clone.get(&key);
                    // Also read count
                    let _ = store_clone.count();
                }
            });
            handles.push(handle);
        }

        // Wait for all threads
        for handle in handles {
            handle.join().expect("thread should not panic");
        }

        // Verify final state
        let count = store.count();
        assert_eq!(
            count, 500,
            "should have 5 writers * 100 entries = 500 entries"
        );
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_stress_concurrent_mixed_operations() {
        use std::sync::Arc;
        use std::sync::atomic::{AtomicUsize, Ordering};
        use std::thread;

        let store = Arc::new(create_test_store());
        let successful_pushes = Arc::new(AtomicUsize::new(0));
        let successful_removes = Arc::new(AtomicUsize::new(0));

        // Pre-populate with some data
        for i in 0..50 {
            store
                .push(&format!("pre_{i}"), json!(i), None)
                .expect("pre-populate should succeed");
        }

        let mut handles = vec![];

        // Writers
        for writer_id in 0..3 {
            let store_clone = store.clone();
            let pushes = successful_pushes.clone();
            let handle = thread::spawn(move || {
                for i in 0..50 {
                    let key = format!("writer_{writer_id}_item_{i}");
                    if store_clone.push(&key, json!({"id": i}), None).is_ok() {
                        pushes.fetch_add(1, Ordering::SeqCst);
                    }
                }
            });
            handles.push(handle);
        }

        // Removers
        for remover_id in 0..2 {
            let store_clone = store.clone();
            let removes = successful_removes.clone();
            let handle = thread::spawn(move || {
                for i in 0..25 {
                    // Try to remove pre-populated entries
                    let key = format!("pre_{}", remover_id * 25 + i);
                    if store_clone.remove(&key) {
                        removes.fetch_add(1, Ordering::SeqCst);
                    }
                }
            });
            handles.push(handle);
        }

        // Readers
        for _ in 0..5 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for i in 0..100 {
                    let _ = store_clone.get(&format!("pre_{}", i % 50));
                    let _ = store_clone.get(&format!("writer_0_item_{}", i % 50));
                    let _ = store_clone.list_entries();
                }
            });
            handles.push(handle);
        }

        // Wait for all threads
        for handle in handles {
            handle.join().expect("thread should not panic");
        }

        // Log results (not strictly asserting due to race conditions in counts)
        let final_pushes = successful_pushes.load(Ordering::SeqCst);
        let final_removes = successful_removes.load(Ordering::SeqCst);
        let final_count = store.count();

        // Basic sanity check
        assert!(final_pushes > 0, "should have completed some pushes");
        assert!(final_removes > 0, "should have completed some removes");
        assert!(final_count > 0, "store should not be empty");
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_stress_rapid_clear_while_writing() {
        use std::sync::Arc;
        use std::thread;

        let store = Arc::new(create_test_store());
        let mut handles = vec![];

        // Writers continuously adding
        for writer_id in 0..3 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for i in 0..200 {
                    let key = format!("key_{writer_id}_{i}");
                    let _ = store_clone.push(&key, json!(i), None);
                    // Small yield to allow interleaving
                    thread::yield_now();
                }
            });
            handles.push(handle);
        }

        // Clearer periodically clearing
        let store_clone = store.clone();
        let clear_handle = thread::spawn(move || {
            for _ in 0..10 {
                thread::sleep(StdDuration::from_micros(100));
                store_clone.clear();
            }
        });
        handles.push(clear_handle);

        // Wait for all threads
        for handle in handles {
            handle.join().expect("thread should not panic");
        }

        // Store may or may not be empty depending on timing - main test is no panics
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_concurrent_get_all_for_context() {
        use std::sync::Arc;
        use std::thread;

        let store = Arc::new(create_test_store());

        // Pre-populate
        for i in 0..10 {
            store
                .push(&format!("data_{i}"), json!({"index": i}), None)
                .expect("pre-populate should succeed");
        }

        let mut handles = vec![];

        // Multiple threads calling get_all (simulating concurrent authorization requests)
        for _ in 0..10 {
            let store_clone = store.clone();
            let handle = thread::spawn(move || {
                for _ in 0..100 {
                    let data = store_clone.get_all();
                    // Verify data is consistent (has entries)
                    assert!(
                        !data.is_empty() || store_clone.count() == 0,
                        "get_all should return data or store is empty"
                    );
                }
            });
            handles.push(handle);
        }

        // One writer thread modifying data
        let store_clone = store.clone();
        let write_handle = thread::spawn(move || {
            for i in 0..50 {
                let key = format!("new_data_{i}");
                let _ = store_clone.push(&key, json!({"new_index": i}), None);
            }
        });
        handles.push(write_handle);

        // Wait for all threads
        for handle in handles {
            handle.join().expect("thread should not panic");
        }
    }
}
