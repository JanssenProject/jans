/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

use chrono::Duration;
use chrono::prelude::*;

use super::kventry::KvEntry;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExpEntry {
    pub key: String,
    pub expired_at: DateTime<Utc>,
}

impl ExpEntry {
    pub fn new<S: AsRef<str>>(key: S, expiration: Duration) -> Self {
        let expired_at: DateTime<Utc> = Utc::now() + expiration;
        Self {
            key: key.as_ref().into(),
            expired_at,
        }
    }

    pub fn from_kv_entry<T>(kv_entry: &KvEntry<T>) -> Self {
        Self {
            key: kv_entry.key.clone(),
            expired_at: kv_entry.expired_at,
        }
    }

    #[must_use]
    pub fn is_expired(&self) -> bool {
        self.expired_at < Utc::now()
    }
}

impl Ord for ExpEntry {
    // Match in opposite direction (min-heap), so that the smallest element is at the top.
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        match self.expired_at.cmp(&other.expired_at) {
            std::cmp::Ordering::Less => std::cmp::Ordering::Greater,
            std::cmp::Ordering::Equal => std::cmp::Ordering::Equal,
            std::cmp::Ordering::Greater => std::cmp::Ordering::Less,
        }
    }
}

impl PartialOrd for ExpEntry {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let before = Utc::now();
        let item = ExpEntry::new("key", Duration::seconds(10));
        let after = Utc::now();
        assert_eq!(
            item.key, "key",
            "ExpEntry key should match the provided key"
        );
        assert!(
            item.expired_at >= before + Duration::seconds(10),
            "expired_at should be at least before + 10s"
        );
        assert!(
            item.expired_at <= after + Duration::seconds(10),
            "expired_at should be at most after + 10s"
        );
    }

    #[test]
    fn test_from_kventry() {
        let kv_entry = KvEntry::new("keyFromKV", "value from KV", Duration::seconds(10));
        let exp_item = ExpEntry::from_kv_entry(&kv_entry);
        assert_eq!(
            exp_item.key, "keyFromKV",
            "ExpEntry key should match KvEntry key"
        );
        assert_eq!(
            exp_item.expired_at, kv_entry.expired_at,
            "ExpEntry expiry should match KvEntry expiry"
        );
    }

    #[test]
    fn test_cmp() {
        let item_small = ExpEntry::new("k1", Duration::seconds(10));
        let item_big = ExpEntry::new("k2", Duration::seconds(8000));
        assert!(
            item_small > item_big,
            "min-heap: smaller TTL should be greater in ordering"
        );
        assert!(
            item_big < item_small,
            "min-heap: larger TTL should be smaller in ordering"
        );
    }

    #[test]
    fn test_is_expired() {
        let item = ExpEntry::new("k1", Duration::seconds(0));
        std::thread::sleep(std::time::Duration::from_nanos(200));
        assert!(
            item.is_expired(),
            "entry with 0s TTL should be expired after a short sleep"
        );
    }
}
