/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */
use chrono::Duration;
use chrono::prelude::*;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KvEntry<T> {
    pub key: String,
    pub value: T,
    pub expired_at: DateTime<Utc>,
}

impl<T> KvEntry<T> {
    pub fn new<S: AsRef<str>>(key: S, value: T, expiration: Duration) -> Self {
        let expired_at: DateTime<Utc> = Utc::now() + expiration;
        Self {
            key: key.as_ref().into(),
            value,
            expired_at,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let item = KvEntry::<String>::new("key", "value".into(), Duration::seconds(10));
        assert_eq!(item.key, "key");
        assert_eq!(item.value, "value");
        assert!(item.expired_at > Utc::now() + Duration::seconds(9));
        assert!(item.expired_at <= Utc::now() + Duration::seconds(10));
    }
}
