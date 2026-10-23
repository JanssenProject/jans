// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::issuer_utils::IssClaim;

use super::IssuerConfig;
use ahash::{HashMap, HashMapExt};
use std::{
    collections::HashSet,
    sync::{Arc, RwLock},
};

const MUTEX_POISONED_ERR: &str =
    "IssuerIndex RwLock poisoned due to another thread panicking while holding the lock";

/// The loaded issuers, by issuer id, and the `iss` values they cover. Several
/// issuers may share one `iss`, so it is kept as a set rather than a key.
///
/// Thread-safe for concurrent reads and writes.
pub(super) struct IssuerIndex {
    issuer_idx: RwLock<HashMap<String, Arc<IssuerConfig>>>,
    iss_index: RwLock<HashSet<IssClaim>>,
}

impl IssuerIndex {
    /// Create a new, empty `IssuerIndex`
    pub(super) fn new() -> Self {
        Self {
            issuer_idx: RwLock::new(HashMap::new()),
            iss_index: RwLock::new(HashSet::new()),
        }
    }

    /// Adds a loaded issuer under its id and records its `iss`.
    pub(super) fn insert(&self, iss: IssClaim, config: IssuerConfig) {
        self.issuer_idx
            .write()
            .expect(MUTEX_POISONED_ERR)
            .insert(config.issuer_id.clone(), Arc::new(config));

        self.iss_index
            .write()
            .expect(MUTEX_POISONED_ERR)
            .insert(iss);
    }

    /// Get the number of issuer configurations in the index; counted by issuer
    /// id, since several issuers may share one `iss`.
    pub(super) fn len(&self) -> usize {
        let index = self.issuer_idx.read().expect(MUTEX_POISONED_ERR);
        index.len()
    }

    /// Check if the index contains a specific iss claim
    pub(super) fn contains_iss(&self, iss: &IssClaim) -> bool {
        let index = self.iss_index.read().expect(MUTEX_POISONED_ERR);
        index.contains(iss)
    }

    /// Find the iss claim for a given issuer ID (policy store key)
    /// Returns `true` if issuer with the given ID is found
    pub(super) fn is_issuer_id_present(&self, issuer_id: &str) -> bool {
        self.issuer_idx
            .read()
            .expect(MUTEX_POISONED_ERR)
            .get(issuer_id)
            .is_some()
    }

    /// Get all issuer IDs in the index of loaded trusted issuers
    pub(super) fn loaded_issuer_ids(&self) -> HashSet<String> {
        let index = self.issuer_idx.read().expect(MUTEX_POISONED_ERR);
        index.keys().cloned().collect()
    }
}
