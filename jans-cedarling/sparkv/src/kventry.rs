/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

use chrono::prelude::*;
use chrono::Duration;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KvEntry {
    pub key: String,
    pub value: String,
    pub expired_at: DateTime<Utc>,
}

impl KvEntry {
    pub fn new(key: &str, value: &str, expiration: Duration) -> Self {
        let expired_at: DateTime<Utc> = Utc::now() + expiration;
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
        let item = KvEntry::new(
            "key",
            "value",
            Duration::new(10, 0).expect("a valid duration"),
        );
        assert_eq!(item.key, "key");
        assert_eq!(item.value, "value");
        assert!(item.expired_at > Utc::now() + Duration::new(9, 0).expect("a valid duration"));
        assert!(item.expired_at <= Utc::now() + Duration::new(10, 0).expect("a valid duration"));
    }
}
