/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KvEntry {
    pub key: String,
    pub value: String,
    pub expired_at: std::time::Instant,
}

impl KvEntry {
    pub fn new(key: &str, value: &str, expiration: std::time::Duration) -> Self {
        let expired_at: std::time::Instant = std::time::Instant::now() + expiration;
        Self {
            key: String::from(key),
            value: String::from(value),
            expired_at,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let item = KvEntry::new("key", "value", std::time::Duration::from_secs(10));
        assert_eq!(item.key, "key");
        assert_eq!(item.value, "value");
        assert!(item.expired_at > std::time::Instant::now() + std::time::Duration::from_secs(9));
        assert!(item.expired_at <= std::time::Instant::now() + std::time::Duration::from_secs(10));
    }
}
