// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::{BTreeMap, HashMap, btree_map, hash_map};
use std::hash::BuildHasher;

use super::kventry::KvEntry;

/// Abstraction over backing map implementations for [`super::SparKV`].
pub trait Map<T> {
    /// Iterator over references to entries.
    type Iter<'a>: Iterator<Item = &'a KvEntry<T>>
    where
        Self: 'a,
        T: 'a;

    /// Iterator over references to keys.
    type KeysIter<'a>: Iterator<Item = &'a String>
    where
        Self: 'a;

    /// Owned iterator consuming the map.
    type IntoIter: Iterator<Item = KvEntry<T>>;

    fn new() -> Self;

    fn get(&self, key: &str) -> Option<&KvEntry<T>>;

    fn insert(&mut self, key: String, value: KvEntry<T>) -> Option<KvEntry<T>>;

    fn remove(&mut self, key: &str) -> Option<KvEntry<T>>;

    fn contains_key(&self, key: &str) -> bool;

    fn len(&self) -> usize;

    fn is_empty(&self) -> bool;

    fn clear(&mut self);

    fn keys(&self) -> Self::KeysIter<'_>;

    fn values(&self) -> Self::Iter<'_>;

    fn into_values(self) -> Self::IntoIter;
}

impl<T> Map<T> for BTreeMap<String, KvEntry<T>> {
    type Iter<'a>
        = btree_map::Values<'a, String, KvEntry<T>>
    where
        Self: 'a,
        T: 'a;
    type KeysIter<'a>
        = btree_map::Keys<'a, String, KvEntry<T>>
    where
        Self: 'a;
    type IntoIter = btree_map::IntoValues<String, KvEntry<T>>;

    #[inline]
    fn new() -> Self {
        BTreeMap::new()
    }

    #[inline]
    fn get(&self, key: &str) -> Option<&KvEntry<T>> {
        self.get(key)
    }

    #[inline]
    fn insert(&mut self, key: String, value: KvEntry<T>) -> Option<KvEntry<T>> {
        self.insert(key, value)
    }

    #[inline]
    fn remove(&mut self, key: &str) -> Option<KvEntry<T>> {
        self.remove(key)
    }

    #[inline]
    fn contains_key(&self, key: &str) -> bool {
        self.contains_key(key)
    }

    #[inline]
    fn len(&self) -> usize {
        self.len()
    }

    #[inline]
    fn is_empty(&self) -> bool {
        self.is_empty()
    }

    #[inline]
    fn clear(&mut self) {
        self.clear();
    }

    #[inline]
    fn keys(&self) -> Self::KeysIter<'_> {
        self.keys()
    }

    #[inline]
    fn values(&self) -> Self::Iter<'_> {
        self.values()
    }

    #[inline]
    fn into_values(self) -> Self::IntoIter {
        self.into_values()
    }
}

impl<T, S> Map<T> for HashMap<String, KvEntry<T>, S>
where
    S: BuildHasher + Default,
{
    type Iter<'a>
        = hash_map::Values<'a, String, KvEntry<T>>
    where
        Self: 'a,
        T: 'a;
    type KeysIter<'a>
        = hash_map::Keys<'a, String, KvEntry<T>>
    where
        Self: 'a;
    type IntoIter = hash_map::IntoValues<String, KvEntry<T>>;

    #[inline]
    fn new() -> Self {
        HashMap::default()
    }

    #[inline]
    fn get(&self, key: &str) -> Option<&KvEntry<T>> {
        self.get(key)
    }

    #[inline]
    fn insert(&mut self, key: String, value: KvEntry<T>) -> Option<KvEntry<T>> {
        self.insert(key, value)
    }

    #[inline]
    fn remove(&mut self, key: &str) -> Option<KvEntry<T>> {
        self.remove(key)
    }

    #[inline]
    fn contains_key(&self, key: &str) -> bool {
        self.contains_key(key)
    }

    #[inline]
    fn len(&self) -> usize {
        self.len()
    }

    #[inline]
    fn is_empty(&self) -> bool {
        self.is_empty()
    }

    #[inline]
    fn clear(&mut self) {
        self.clear();
    }

    #[inline]
    fn keys(&self) -> Self::KeysIter<'_> {
        self.keys()
    }

    #[inline]
    fn values(&self) -> Self::Iter<'_> {
        self.values()
    }

    #[inline]
    fn into_values(self) -> Self::IntoIter {
        self.into_values()
    }
}
