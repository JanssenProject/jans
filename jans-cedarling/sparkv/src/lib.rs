/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

mod config;
mod error;
mod expentry;
mod kventry;

pub use config::Config;
pub use error::Error;
pub use expentry::ExpEntry;
pub use kventry::KvEntry;

use chrono::Duration;
use chrono::prelude::*;

pub struct SparKV {
    pub config: Config,
    data: std::collections::BTreeMap<String, KvEntry>,
    expiries: std::collections::BinaryHeap<ExpEntry>,
}

impl SparKV {
    pub fn new() -> Self {
        let config = Config::new();
        SparKV::with_config(config)
    }

    pub fn with_config(config: Config) -> Self {
        SparKV {
            config,
            data: std::collections::BTreeMap::new(),
            expiries: std::collections::BinaryHeap::new(),
        }
    }

    pub fn set(&mut self, key: &str, value: &str) -> Result<(), Error> {
        self.set_with_ttl(key, value, self.config.default_ttl)
    }

    pub fn set_with_ttl(&mut self, key: &str, value: &str, ttl: Duration) -> Result<(), Error> {
        self.clear_expired_if_auto();
        self.ensure_capacity_ignore_key(key)?;
        self.ensure_item_size(value)?;
        self.ensure_max_ttl(ttl)?;

        let item: KvEntry = KvEntry::new(key, value, ttl);
        let exp_item: ExpEntry = ExpEntry::from_kv_entry(&item);

        self.expiries.push(exp_item);
        self.data.insert(item.key.clone(), item);
        Ok(())
    }

    pub fn get(&self, key: &str) -> Option<String> {
        let item = self.get_item(key)?;
        Some(item.value.clone())
    }

    // Only returns if it is not yet expired
    pub fn get_item(&self, key: &str) -> Option<&KvEntry> {
        let item = self.data.get(key)?;
        if item.expired_at > Utc::now() {
            Some(item)
        } else {
            None
        }
    }

    pub fn get_keys(&self) -> Vec<String> {
        self.data
            .keys()
            .map(|key| key.to_string())// it clone the string
            .collect()
    }

    pub fn pop(&mut self, key: &str) -> Option<String> {
        self.clear_expired_if_auto();
        let item = self.data.remove(key)?;
        // Does not delete from BinaryHeap as it's expensive.
        Some(item.value)
    }

    pub fn len(&self) -> usize {
        self.data.len()
    }

    pub fn is_empty(&self) -> bool {
        self.data.len() == 0
    }

    pub fn contains_key(&self, key: &str) -> bool {
        self.data.contains_key(key)
    }

    pub fn clear_expired(&mut self) -> usize {
        let mut cleared_count: usize = 0;
        loop {
            let peeked = self.expiries.peek().cloned();
            match peeked {
                Some(exp_item) => {
                    if exp_item.is_expired() {
                        let kv_entry = self.data.get(&exp_item.key).unwrap();
                        if kv_entry.key == exp_item.key
                            && kv_entry.expired_at == exp_item.expired_at
                        {
                            cleared_count += 1;
                            self.pop(&exp_item.key);
                        }
                        self.expiries.pop();
                    } else {
                        break;
                    }
                },
                None => break,
            }
        }
        cleared_count
    }

    fn clear_expired_if_auto(&mut self) {
        if self.config.auto_clear_expired {
            self.clear_expired();
        }
    }

    fn ensure_capacity(&self) -> Result<(), Error> {
        if self.len() >= self.config.max_items {
            return Err(Error::CapacityExceeded);
        }
        Ok(())
    }

    fn ensure_capacity_ignore_key(&self, key: &str) -> Result<(), Error> {
        if self.contains_key(key) {
            return Ok(());
        }
        self.ensure_capacity()
    }

    fn ensure_item_size(&self, value: &str) -> Result<(), Error> {
        if value.len() > self.config.max_item_size {
            return Err(Error::ItemSizeExceeded);
        }
        Ok(())
    }

    fn ensure_max_ttl(&self, ttl: Duration) -> Result<(), Error> {
        if ttl > self.config.max_ttl {
            return Err(Error::TTLTooLong);
        }
        Ok(())
    }
}

impl Default for SparKV {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sparkv_config() {
        let config: Config = Config::new();
        assert_eq!(config.max_items, 10_000);
        assert_eq!(config.max_item_size, 500_000);
        assert_eq!(
            config.max_ttl,
            Duration::new(60 * 60, 0).expect("a valid duration")
        );
    }

    #[test]
    fn test_sparkv_new_with_config() {
        let config: Config = Config::new();
        let sparkv = SparKV::with_config(config);
        assert_eq!(sparkv.config, config);
    }

    #[test]
    fn test_len_is_empty() {
        let mut sparkv = SparKV::new();
        assert_eq!(sparkv.len(), 0);
        assert!(sparkv.is_empty());

        _ = sparkv.set("keyA", "value");
        assert_eq!(sparkv.len(), 1);
        assert!(!sparkv.is_empty());
    }

    #[test]
    fn test_set_get() {
        let mut sparkv = SparKV::new();
        _ = sparkv.set("keyA", "value");
        assert_eq!(sparkv.get("keyA"), Some(String::from("value")));
        assert_eq!(sparkv.expiries.len(), 1);

        // Overwrite the value
        _ = sparkv.set("keyA", "value2");
        assert_eq!(sparkv.get("keyA"), Some(String::from("value2")));
        assert_eq!(sparkv.expiries.len(), 2);

        assert!(sparkv.get("non-existent").is_none());
    }

    #[test]
    fn test_get_item() {
        let mut sparkv = SparKV::new();
        let item = KvEntry::new(
            "keyARaw",
            "value99",
            Duration::new(1, 0).expect("a valid duration"),
        );
        sparkv.data.insert(item.key.clone(), item);
        let get_result = sparkv.get_item("keyARaw");
        let unwrapped = get_result.unwrap();

        assert!(get_result.is_some());
        assert_eq!(unwrapped.key, "keyARaw");
        assert_eq!(unwrapped.value, "value99");

        assert!(sparkv.get_item("non-existent").is_none());
    }

    #[test]
    fn test_get_item_return_none_if_expired() {
        let mut sparkv = SparKV::new();
        _ = sparkv.set_with_ttl(
            "key",
            "value",
            Duration::new(0, 40000).expect("a valid duration"),
        );
        assert_eq!(sparkv.get("key"), Some(String::from("value")));

        std::thread::sleep(std::time::Duration::from_nanos(50000));
        assert_eq!(sparkv.get("key"), None);
    }

    #[test]
    fn test_set_should_fail_if_capacity_exceeded() {
        let mut config: Config = Config::new();
        config.max_items = 2;

        let mut sparkv = SparKV::with_config(config);
        let mut set_result = sparkv.set("keyA", "value");
        assert!(set_result.is_ok());
        assert_eq!(sparkv.get("keyA"), Some(String::from("value")));

        set_result = sparkv.set("keyB", "value2");
        assert!(set_result.is_ok());

        set_result = sparkv.set("keyC", "value3");
        assert!(set_result.is_err());
        assert_eq!(set_result.unwrap_err(), Error::CapacityExceeded);
        assert!(sparkv.get("keyC").is_none());

        // Overwrite existing key should not err
        set_result = sparkv.set("keyB", "newValue1234");
        assert!(set_result.is_ok());
        assert_eq!(sparkv.get("keyB"), Some(String::from("newValue1234")));
    }

    #[test]
    fn test_set_with_ttl() {
        let mut sparkv = SparKV::new();
        _ = sparkv.set("longest", "value");
        _ = sparkv.set_with_ttl(
            "longer",
            "value",
            Duration::new(2, 0).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "shorter",
            "value",
            Duration::new(1, 0).expect("a valid duration"),
        );

        assert_eq!(sparkv.get("longer"), Some(String::from("value")));
        assert_eq!(sparkv.get("shorter"), Some(String::from("value")));
        assert!(
            sparkv.get_item("longer").unwrap().expired_at
                > sparkv.get_item("shorter").unwrap().expired_at
        );
        assert!(
            sparkv.get_item("longest").unwrap().expired_at
                > sparkv.get_item("longer").unwrap().expired_at
        );
    }

    #[test]
    fn test_ensure_max_ttl() {
        let mut config: Config = Config::new();
        config.max_ttl = Duration::new(3600, 0).expect("a valid duration");
        config.default_ttl = Duration::new(5000, 0).expect("a valid duration");
        let mut sparkv = SparKV::with_config(config);

        let set_result_long_def = sparkv.set("default is longer than max", "should fail");
        assert!(set_result_long_def.is_err());
        assert_eq!(set_result_long_def.unwrap_err(), Error::TTLTooLong);

        let set_result_ok = sparkv.set_with_ttl(
            "shorter",
            "ok",
            Duration::new(3599, 0).expect("a valid duration"),
        );
        assert!(set_result_ok.is_ok());

        let set_result_ok_2 = sparkv.set_with_ttl(
            "exact",
            "ok",
            Duration::new(3600, 0).expect("a valid duration"),
        );
        assert!(set_result_ok_2.is_ok());

        let set_result_not_ok = sparkv.set_with_ttl(
            "not",
            "not ok",
            Duration::new(3601, 0).expect("a valid duration"),
        );
        assert!(set_result_not_ok.is_err());
        assert_eq!(set_result_not_ok.unwrap_err(), Error::TTLTooLong);
    }

    #[test]
    fn test_delete() {
        let mut sparkv = SparKV::new();
        _ = sparkv.set("keyA", "value");
        assert_eq!(sparkv.get("keyA"), Some(String::from("value")));
        assert_eq!(sparkv.expiries.len(), 1);

        let deleted_value = sparkv.pop("keyA");
        assert_eq!(deleted_value, Some(String::from("value")));
        assert!(sparkv.get("keyA").is_none());
        assert_eq!(sparkv.expiries.len(), 1); // it does not delete
    }

    #[test]
    fn test_clear_expired() {
        let mut config: Config = Config::new();
        config.auto_clear_expired = false;
        let mut sparkv = SparKV::with_config(config);
        _ = sparkv.set_with_ttl(
            "not-yet-expired",
            "v",
            Duration::new(0, 90).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "expiring",
            "value",
            Duration::new(1, 0).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "not-expired",
            "value",
            Duration::new(60, 0).expect("a valid duration"),
        );
        std::thread::sleep(std::time::Duration::from_nanos(2))
    }

    #[test]
    fn test_clear_expired_with_overwritten_key() {
        let mut config: Config = Config::new();
        config.auto_clear_expired = false;
        let mut sparkv = SparKV::with_config(config);
        _ = sparkv.set_with_ttl(
            "no-longer",
            "value",
            Duration::new(0, 1).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "no-longer",
            "v",
            Duration::new(90, 0).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "not-expired",
            "value",
            Duration::new(60, 0).expect("a valid duration"),
        );
        std::thread::sleep(std::time::Duration::from_nanos(2));
        assert_eq!(sparkv.expiries.len(), 3); // overwriting key does not update expiries
        assert_eq!(sparkv.len(), 2);

        let cleared_count = sparkv.clear_expired();
        assert_eq!(cleared_count, 0); // no longer expiring
        assert_eq!(sparkv.expiries.len(), 2); // should have cleared the expiries
        assert_eq!(sparkv.len(), 2); // but not actually deleting
    }

    #[test]
    fn test_clear_expired_with_auto_clear_expired_enabled() {
        let mut config: Config = Config::new();
        config.auto_clear_expired = true; // explicitly setting it to true
        let mut sparkv = SparKV::with_config(config);
        _ = sparkv.set_with_ttl(
            "no-longer",
            "value",
            Duration::new(1, 0).expect("a valid duration"),
        );
        _ = sparkv.set_with_ttl(
            "no-longer",
            "v",
            Duration::new(90, 0).expect("a valid duration"),
        );
        std::thread::sleep(std::time::Duration::from_secs(2));
        _ = sparkv.set_with_ttl(
            "not-expired",
            "value",
            Duration::new(60, 0).expect("a valid duration"),
        );
        assert_eq!(sparkv.expiries.len(), 2); // diff from above, because of auto clear
        assert_eq!(sparkv.len(), 2);

        // auto clear 2
        _ = sparkv.set_with_ttl(
            "new-",
            "value",
            Duration::new(60, 0).expect("a valid duration"),
        );
        assert_eq!(sparkv.expiries.len(), 3); // should have cleared the expiries
        assert_eq!(sparkv.len(), 3); // but not actually deleting
    }
}
