/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */
use std::collections::{BTreeMap, BinaryHeap, HashMap};

mod config;
mod error;
mod expentry;
mod index;
mod kventry;
mod map;

pub use config::Config;
pub use error::Error;
pub use expentry::ExpEntry;
use index::HashMapIndex;
pub use kventry::KvEntry;
pub use map::Map;

use chrono::Duration;
use chrono::prelude::*;

/// BTreeMap-backed `SparKV`. Provides sorted iteration for consumers like `MemoryLogger`.
pub type BTreeSparKV<T> = SparKV<BTreeMap<String, KvEntry<T>>, T>;

/// HashMap-backed `SparKV`. Provides O(1) lookups for consumers that do not need sorted iteration.
pub type HashMapSparKV<T> = SparKV<HashMap<String, KvEntry<T>, ahash::RandomState>, T>;

pub struct SparKV<M, T>
where
    M: Map<T>,
{
    pub config: Config,
    data: M,
    index: HashMapIndex,
    expiries: BinaryHeap<ExpEntry>,
    /// An optional function that calculates the memory size of a value.
    ///
    /// Used by `ensure_item_size`.
    ///
    /// If this function is not provided, the container will enforce
    /// `Config.max_item_size` on the basis of `std::mem::size_of_val` which
    /// probably won't be what you expect.
    size_calculator: Option<fn(&T) -> usize>,
}

/// See the [`SparKV::iter`] function
pub struct Iter<'a, T, M>
where
    M: Map<T> + 'a,
{
    value_iter: M::Iter<'a>,
    // Required for the compiler to prove T: 'a on this struct.
    // The GAT bound `where T: 'a` on Map::Iter<'a> constrains the
    // associated type but does not propagate to Iter's own well-formedness.
    _phantom: std::marker::PhantomData<&'a T>,
}

impl<'a, T, M> Iterator for Iter<'a, T, M>
where
    M: Map<T> + 'a,
{
    type Item = (&'a String, &'a T);

    fn next(&mut self) -> Option<Self::Item> {
        self.value_iter
            .next()
            .map(|kventry| (&kventry.key, &kventry.value))
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        self.value_iter.size_hint()
    }
}

/// See the [`SparKV::drain`] function
pub struct DrainIter<T, M>
where
    M: Map<T>,
{
    value_iter: M::IntoIter,
}

impl<T, M> Iterator for DrainIter<T, M>
where
    M: Map<T>,
{
    type Item = (String, T);

    fn next(&mut self) -> Option<Self::Item> {
        self.value_iter
            .next()
            .map(|kventry| (kventry.key, kventry.value))
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        self.value_iter.size_hint()
    }
}

impl<M, T> SparKV<M, T>
where
    M: Map<T>,
{
    #[must_use]
    pub fn new() -> Self {
        let config = Config::new();
        SparKV::with_config(config)
    }

    #[must_use]
    pub fn with_config(config: Config) -> Self {
        SparKV {
            config,
            data: M::new(),
            expiries: BinaryHeap::new(),
            index: HashMapIndex::new(),
            // This will underestimate the size of most things.
            size_calculator: Some(|v| std::mem::size_of_val(v)),
        }
    }

    /// Provide optional size function. See [`SparKV::size_calculator`] comments.
    #[must_use]
    pub fn with_config_and_sizer(config: Config, sizer: Option<fn(&T) -> usize>) -> Self {
        SparKV {
            config,
            data: M::new(),
            expiries: BinaryHeap::new(),
            index: HashMapIndex::new(),
            size_calculator: sizer,
        }
    }

    pub fn set(&mut self, key: &str, value: T, index_keys: &[String]) -> Result<(), Error> {
        self.set_with_ttl(key, value, self.config.default_ttl, index_keys)
    }

    pub fn set_with_ttl(
        &mut self,
        key: &str,
        value: T,
        ttl: Duration,
        index_keys: &[String],
    ) -> Result<(), Error> {
        self.clear_expired_if_auto();
        self.ensure_item_size(&value)?;
        self.ensure_max_ttl(ttl)?;

        if let Err(err) = self.ensure_capacity_ignore_key(key) {
            // check if we have no capacity and config parameter is active
            // we remove last element, like lru cache
            if err == Error::CapacityExceeded && self.config.earliest_expiration_eviction {
                self.remove_last();
                // If nothing could be evicted (or invariants are broken), keep signaling capacity error.
                self.ensure_capacity_ignore_key(key)?;
            } else {
                return Err(err);
            }
        }

        let item: KvEntry<T> = KvEntry::new(key, value, ttl);
        let exp_item: ExpEntry = ExpEntry::from_kv_entry(&item);

        self.expiries.push(exp_item);
        self.index
            .add_key_value(index::IndexKey(key.into()), index::ValueKey(key.into()));
        self.data.insert(key.into(), item);

        for index_key in index_keys {
            self.add_additional_index(key, index_key);
        }
        Ok(())
    }

    #[must_use]
    pub fn get(&self, key: &str) -> Option<&T> {
        Some(&self.get_item(key)?.value)
    }

    // Only returns if it is not yet expired
    #[must_use]
    pub fn get_item(&self, key: &str) -> Option<&KvEntry<T>> {
        let item = self.data.get(key)?;
        (item.expired_at > Utc::now()).then_some(item)
    }

    #[must_use]
    pub fn get_oldest_key_by_expiration(&self) -> Option<&ExpEntry> {
        self.expiries.peek()
    }

    #[must_use]
    pub fn get_keys(&self) -> Vec<String> {
        self.data.keys().cloned().collect()
    }

    /// Return an iterator of (key,value) : (&String,&T).
    #[must_use]
    pub fn iter(&self) -> Iter<'_, T, M> {
        Iter {
            value_iter: self.data.values(),
            _phantom: std::marker::PhantomData,
        }
    }

    /// Return an iterator of (key,value) : (String,T) which empties the container.
    /// All entries will be owned by the iterator, and yielded entries will not be checked against expiry.
    /// All entries and expiries will be cleared.
    pub fn drain(&mut self) -> DrainIter<T, M> {
        // assume that slightly-expired entries should be returned.
        self.expiries.clear();
        self.index.clear();
        let data_only = std::mem::replace(&mut self.data, M::new());
        DrainIter {
            value_iter: data_only.into_values(),
        }
    }

    pub fn pop(&mut self, key: &str) -> Option<T> {
        let item = self.data.remove(key)?;
        self.index.remove_value_key(&index::ValueKey(item.key));

        // Does not delete expiry entry from BinaryHeap as it's expensive.
        Some(item.value)
    }

    #[must_use]
    pub fn len(&self) -> usize {
        self.data.len()
    }

    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    #[must_use]
    pub fn contains_key(&self, key: &str) -> bool {
        self.data.contains_key(key)
    }

    /// Removes only last element from expires `BinaryHeap`, even if value is not expired.
    /// Is used when [`Config::earliest_expiration_eviction`] is true
    fn remove_last(&mut self) {
        while let Some(exp_item) = self.expiries.pop() {
            if let Some(kv_entry) = self.data.get(&exp_item.key)
                && kv_entry.key == exp_item.key
                && kv_entry.expired_at == exp_item.expired_at
            {
                self.pop(&exp_item.key);
                return;
            }
        }
    }

    pub fn clear_expired(&mut self) -> usize {
        let mut cleared_count: usize = 0;
        while let Some(exp_item) = self.expiries.peek().cloned() {
            if exp_item.is_expired() {
                let should_pop = match self.data.get(&exp_item.key) {
                    Some(kv_entry) => {
                        kv_entry.key == exp_item.key && kv_entry.expired_at == exp_item.expired_at
                    },
                    None => false,
                };

                if should_pop {
                    cleared_count += 1;
                    self.pop(&exp_item.key);
                }
                // remove current item from expiries
                self.expiries.pop();
            } else {
                break;
            }
        }
        cleared_count
    }

    /// Add additional index key for an existing value.
    /// If key not found do nothing.
    pub fn add_additional_index(&mut self, key: &str, index_key: &str) {
        if !self.data.contains_key(key) {
            return;
        }
        let value_key = index::ValueKey(key.into());
        self.index
            .add_key_value(index::IndexKey(index_key.into()), value_key);
    }

    /// Get values by index key.
    /// Iterator is sorted by value keys.
    pub fn get_by_index_key<'a>(&'a self, index_key: &'a str) -> impl Iterator<Item = &'a T> {
        let mut keys = self
            .index
            .get_by_index_key(&index::IndexKey(index_key.into()))
            .collect::<Vec<_>>();
        keys.sort();

        keys.into_iter()
            .filter_map(move |value_key| self.data.get(&value_key.0).map(|entry| &entry.value))
    }

    /// Remove all values in database by index key
    pub fn remove_by_index<'a>(&'a mut self, index_key: &'a str) -> usize {
        // keys is cloned to avoid borrowing issues of mutable `self`
        let keys: Vec<_> = self
            .index
            .get_by_index_key(&index::IndexKey(index_key.into()))
            .cloned()
            .collect();

        keys.into_iter().filter_map(|key| self.pop(&key.0)).count()
    }

    /// Empty the container. That is, remove all key-values and expiries.
    pub fn clear(&mut self) {
        self.data.clear();
        self.expiries.clear();
        self.index.clear();
    }

    fn clear_expired_if_auto(&mut self) {
        if self.config.auto_clear_expired {
            self.clear_expired();
        }
    }

    fn ensure_capacity(&self) -> Result<(), Error> {
        if self.config.max_items == 0 {
            // no limit
            return Ok(());
        }

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
        if self.config.max_item_size == 0 {
            // no limit
            return Ok(());
        }

        if let Some(calc) = self.size_calculator
            && calc(value) > self.config.max_item_size
        {
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

impl<'a, M, T> IntoIterator for &'a SparKV<M, T>
where
    M: Map<T>,
{
    type Item = (&'a String, &'a T);
    type IntoIter = Iter<'a, T, M>;

    fn into_iter(self) -> Self::IntoIter {
        self.iter()
    }
}

impl<M, T> Default for SparKV<M, T>
where
    M: Map<T>,
{
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests;

#[cfg(test)]
mod test_json_value;
