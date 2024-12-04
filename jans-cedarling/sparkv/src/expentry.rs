/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

use super::kventry::KvEntry;
use chrono::prelude::*;
use chrono::Duration;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExpEntry {
    pub key: String,
    pub expired_at: DateTime<Utc>,
}

impl ExpEntry {
    pub fn new(key: &str, expiration: Duration) -> Self {
        let expired_at: DateTime<Utc> = Utc::now() + expiration;
        Self {
            key: String::from(key),
            expired_at,
        }
    }

    pub fn from_kv_entry(kv_entry: &KvEntry) -> Self {
        Self {
            key: kv_entry.key.clone(),
            expired_at: kv_entry.expired_at,
        }
    }

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
        let item = ExpEntry::new("key", Duration::new(10, 0).expect("a valid duration"));
        assert_eq!(item.key, "key");
        assert!(item.expired_at > Utc::now() + Duration::new(9, 0).expect("a valid duration"));
        assert!(item.expired_at <= Utc::now() + Duration::new(10, 0).expect("a valid duration"));
    }

    #[test]
    fn test_from_kventry() {
        let kv_entry = KvEntry::new(
            "keyFromKV",
            "value from KV",
            Duration::new(10, 0).expect("a valid duration"),
        );
        let exp_item = ExpEntry::from_kv_entry(&kv_entry);
        assert_eq!(exp_item.key, "keyFromKV");
        assert_eq!(exp_item.expired_at, kv_entry.expired_at);
    }

    #[test]
    fn test_cmp() {
        let item_small = ExpEntry::new("k1", Duration::new(10, 0).expect("a valid duration"));
        let item_big = ExpEntry::new("k2", Duration::new(8000, 0).expect("a valid duration"));
        assert!(item_small > item_big); // reverse order
        assert!(item_big < item_small); // reverse order
    }

    #[test]
    fn test_is_expired() {
        let item = ExpEntry::new("k1", Duration::new(0, 100).expect("a valid duration"));
        std::thread::sleep(std::time::Duration::from_nanos(200));
        assert!(item.is_expired());
    }
}
