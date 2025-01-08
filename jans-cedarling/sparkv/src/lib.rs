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

pub struct SparKV<T> {
    pub config: Config,
    data: std::collections::BTreeMap<String, KvEntry<T>>,
    expiries: std::collections::BinaryHeap<ExpEntry>,
    size_calculator : Option<fn(&T) -> usize>,
}

pub struct Iter<'a, T: 'a>
{
     btree_value_iter : std::collections::btree_map::Values<'a, String, KvEntry<T>>,
}

impl<'a, T> Iterator for Iter<'a, T> {
    type Item = (&'a String,&'a T);

    fn next(&mut self) -> Option<Self::Item> {
        self.btree_value_iter.next().map(|kventry| (&kventry.key, &kventry.value) )
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        self.btree_value_iter.size_hint()
    }
}

pub struct DrainIter<T>
{
     value_iter : std::collections::btree_map::IntoValues<String, KvEntry<T>>,
}

impl<T> Iterator for DrainIter<T> {
    type Item = (String,T);

    fn next(&mut self) -> Option<Self::Item> {
        self.value_iter.next().map(|kventry| (kventry.key, kventry.value) )
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        self.value_iter.size_hint()
    }
}

impl<T> SparKV<T> {
    pub fn new() -> Self {
        let config = Config::new();
        SparKV::with_config(config)
    }

    pub fn with_config(config: Config) -> Self {
        SparKV {
            config,
            data: std::collections::BTreeMap::new(),
            expiries: std::collections::BinaryHeap::new(),
            // This will underestimate the size of most things.
            size_calculator: Some(|v| std::mem::size_of_val(v)),
        }
    }

    pub fn with_config_and_sizer(config: Config, sizer: Option<fn(&T) -> usize>) -> Self {
        SparKV {
            config,
            data: std::collections::BTreeMap::new(),
            expiries: std::collections::BinaryHeap::new(),
            size_calculator: sizer,
        }
    }

    pub fn set(&mut self, key: &str, value: T) -> Result<(), Error>
    {
        self.set_with_ttl(key, value, self.config.default_ttl)
    }

    pub fn set_with_ttl(&mut self, key: &str, value: T, ttl: Duration) -> Result<(), Error> {
        self.clear_expired_if_auto();
        self.ensure_capacity_ignore_key(key)?;
        self.ensure_item_size(&value)?;
        self.ensure_max_ttl(ttl)?;

        let item: KvEntry<T> = KvEntry::new(key, value, ttl);
        let exp_item: ExpEntry = ExpEntry::from_kv_entry(&item);

        self.expiries.push(exp_item);
        self.data.insert(key.into(), item);
        Ok(())
    }

    pub fn get(&self, key: &str) -> Option<&T>
    {
        Some(&self.get_item(key)?.value)
    }

    // Only returns if it is not yet expired
    pub fn get_item(&self, key: &str) -> Option<&KvEntry<T>> {
        let item = self.data.get(key)?;
        (item.expired_at > Utc::now()).then_some(item)
    }

    pub fn get_keys(&self) -> Vec<String> {
        self.data.keys().cloned().collect()
    }

    pub fn iter(&self) -> Iter<T> {
        Iter{ btree_value_iter: self.data.values() }
    }

    /// Return an iterator of (key,value) : (String,T) which empties the container.
    /// All entries will be owned by the iterator, and yielded entries will not be checked against expiry.
    /// All entries and expiries will be cleared.
    pub fn drain(&mut self) -> DrainIter<T> {
        // assume that slightly-expired entries should be returned.
        self.expiries.clear();
        let data_only = std::mem::take(&mut self.data);
        DrainIter{ value_iter: data_only.into_values() }
    }

    pub fn pop(&mut self, key: &str) -> Option<T> {
        self.clear_expired_if_auto();
        let item = self.data.remove(key)?;
        // Does not delete expiry entry from BinaryHeap as it's expensive.
        Some(item.value)
    }

    pub fn len(&self) -> usize {
        self.data.len()
    }

    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    pub fn contains_key(&self, key: &str) -> bool {
        self.data.contains_key(key)
    }

    pub fn clear_expired(&mut self) -> usize {
        let mut cleared_count: usize = 0;
        while let Some(exp_item) = self.expiries.peek().cloned() {
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
                break
            }
        }
        cleared_count
    }

    pub fn clear(&mut self) {
        self.data.clear();
        self.expiries.clear();
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

    fn ensure_item_size(&self, value: &T) -> Result<(), Error> {
        if let Some(calc) = self.size_calculator {
            if calc(value) > self.config.max_item_size {
                return Err(Error::ItemSizeExceeded)
            }
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

impl<T> Default for SparKV<T> {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests;

#[cfg(test)]
mod test_json_value;
