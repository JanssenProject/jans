// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::{HashMap, HashSet};
use std::hash::Hash;

/// Util type to guarantee compile type check
#[derive(Hash, Eq, PartialEq, Clone)]
pub(crate) struct IndexKey(pub String);

#[derive(Debug, Hash, Eq, PartialEq, Clone, PartialOrd, Ord)]
pub(crate) struct ValueKey(pub String);

/// HashMapIndex allows to group keys (to origin value) by index key.
/// And get it efficiently.
pub(crate) struct HashMapIndex {
    buckets: HashMap<IndexKey, HashSet<ValueKey>>,
    // is used to track what `IndexKey` is used for `ValueKey`
    index_tracker: HashMap<ValueKey, HashSet<IndexKey>>,
}

impl HashMapIndex {
    pub fn new() -> Self {
        Self {
            buckets: HashMap::new(),
            index_tracker: HashMap::new(),
        }
    }

    fn add_to_bucket(&mut self, index_key: IndexKey, value_key: ValueKey) {
        match self.buckets.get_mut(&index_key) {
            Some(bucket) => {
                bucket.insert(value_key);
            },
            None => {
                let mut bucket = HashSet::new();
                bucket.insert(value_key);
                self.buckets.insert(index_key, bucket);
            },
        };
    }

    fn add_to_index_tracker(&mut self, index_key: IndexKey, value_key: ValueKey) {
        match self.index_tracker.get_mut(&value_key) {
            Some(index_tracker) => {
                index_tracker.insert(index_key);
            },
            None => {
                let mut index_tracker = HashSet::new();
                index_tracker.insert(index_key);
                self.index_tracker.insert(value_key, index_tracker);
            },
        };
    }

    /// Add `value_key` to storage by `index_key`
    pub fn add_key_value(&mut self, index_key: IndexKey, value_key: ValueKey) {
        self.add_to_index_tracker(index_key.clone(), value_key.clone());
        self.add_to_bucket(index_key, value_key);
    }

    /// Get iterator with all elements in the index by `index_key`
    pub fn get_by_index_key<'a>(
        &'a self,
        index_key: &IndexKey,
    ) -> impl Iterator<Item = &'a ValueKey> {
        self.buckets.get(index_key).into_iter().flatten()
    }

    /// Remove `value_key` from the index storage
    pub fn remove_value_key(&mut self, value_key: &ValueKey) {
        // remove value kays from all buckets
        if let Some(index_keys) = self.index_tracker.get(value_key) {
            for index_key in index_keys {
                if let Some(bucket) = self.buckets.get_mut(index_key) {
                    bucket.remove(value_key);
                }
            }
        };
        // and remove from tracker
        self.index_tracker.remove(value_key);
    }

    /// Clear all index entries
    pub fn clear(&mut self) {
        self.buckets.clear();
        self.index_tracker.clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_add_key_value() {
        let mut index = HashMapIndex::new();
        let index_key = IndexKey("index1".to_string());
        let value_key = ValueKey("value1".to_string());

        index.add_key_value(index_key.clone(), value_key.clone());

        let values: Vec<&ValueKey> = index.get_by_index_key(&index_key).collect();
        assert_eq!(values, vec![&value_key]);
    }

    #[test]
    fn test_get_by_index_key() {
        let mut index = HashMapIndex::new();
        let index_key = IndexKey("index1".to_string());
        let value_key1 = ValueKey("value1".to_string());
        let value_key2 = ValueKey("value2".to_string());

        index.add_key_value(index_key.clone(), value_key1.clone());
        index.add_key_value(index_key.clone(), value_key2.clone());

        let mut values: Vec<&ValueKey> = index.get_by_index_key(&index_key).collect();
        values.sort();
        assert_eq!(values, vec![&value_key1, &value_key2]);
    }

    #[test]
    fn test_remove_value_key() {
        let mut index = HashMapIndex::new();
        let index_key = IndexKey("index1".to_string());
        let value_key = ValueKey("value1".to_string());

        index.add_key_value(index_key.clone(), value_key.clone());
        index.remove_value_key(&value_key);

        let values: Vec<&ValueKey> = index.get_by_index_key(&index_key).collect();
        assert!(values.is_empty());
    }

    #[test]
    fn test_clear() {
        let mut index = HashMapIndex::new();
        let index_key = IndexKey("index1".to_string());
        let value_key = ValueKey("value1".to_string());

        index.add_key_value(index_key.clone(), value_key.clone());
        index.clear();

        assert!(index.buckets.is_empty());
        assert!(index.index_tracker.is_empty());
    }
}
