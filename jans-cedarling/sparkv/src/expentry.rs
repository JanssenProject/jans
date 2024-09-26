/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

use super::kventry::KvEntry;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExpEntry {
    pub key: String,
    pub expired_at: std::time::Instant,
}

impl ExpEntry {
    pub fn new(key: &str, expiration: std::time::Duration) -> Self {
        let expired_at: std::time::Instant = std::time::Instant::now() + expiration;
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
        self.expired_at < std::time::Instant::now()
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
        let item = ExpEntry::new("key", std::time::Duration::from_secs(10));
        assert_eq!(item.key, "key");
        assert!(item.expired_at > std::time::Instant::now() + std::time::Duration::from_secs(9));
        assert!(item.expired_at <= std::time::Instant::now() + std::time::Duration::from_secs(10));
    }

    #[test]
    fn test_from_kventry() {
        let kv_entry = KvEntry::new(
            "keyFromKV",
            "value from KV",
            std::time::Duration::from_secs(10),
        );
        let exp_item = ExpEntry::from_kv_entry(&kv_entry);
        assert_eq!(exp_item.key, "keyFromKV");
        assert_eq!(exp_item.expired_at, kv_entry.expired_at);
    }

    #[test]
    fn test_cmp() {
        let item_small = ExpEntry::new("k1", std::time::Duration::from_secs(10));
        let item_big = ExpEntry::new("k2", std::time::Duration::from_secs(8000));
        assert!(item_small > item_big); // reverse order
        assert!(item_big < item_small); // reverse order
    }

    #[test]
    fn test_is_expired() {
        let item = ExpEntry::new("k1", std::time::Duration::from_millis(1));
        assert!(!item.is_expired());
        std::thread::sleep(std::time::Duration::from_millis(2));
        assert!(item.is_expired());
    }
}
