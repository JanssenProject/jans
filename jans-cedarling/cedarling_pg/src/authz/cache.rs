// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Process-local cache for successful Cedarling authorization decisions (boolean results).
//!
//! Uses the workspace `sparkv` crate (`SparKV`)
//!
//! Only [`Ok`] outcomes from the Cedarling bridge are stored. Engine / parse / JWT errors follow
//! [`crate::guc_config::CedarlingFailMode`] and are **not** cached.
//!
//! Keys include a fingerprint of [`crate::guc_config::bootstrap_config_path_utf8`] so entries do not
//! spill across unrelated bootstrap configs when the [`crate::engine`] slot is recreated (e.g. after
//! administrator restart). Policy content changes without a restart are not detected (same as the
//! cached engine handle).

use std::hash::{DefaultHasher, Hash, Hasher};
use std::sync::{Mutex, OnceLock, PoisonError};

use chrono::Duration as ChronoDuration;
use sparkv::{Config as SparKvConfig, SparKV};

use crate::guc_config;

/// Maximum `cedarling.cache_ttl` (seconds) — must align with [`crate::guc_config`] upper bound.
const MAX_CACHE_TTL_SECS: i64 = 604_800;

const VARIANT_MULTI: u8 = 1;
const VARIANT_UNSIGNED: u8 = 2;

/// Stable hash of [`guc_config::bootstrap_config_path_utf8`] used as a policy-store identity segment.
#[must_use]
pub(crate) fn policy_segment_from_bootstrap_path() -> u64 {
    let mut h = DefaultHasher::new();
    guc_config::bootstrap_config_path_utf8()
        .unwrap_or_default()
        .hash(&mut h);
    guc_config::policy_version_utf8()
        .unwrap_or_default()
        .hash(&mut h);
    h.finish()
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, Hash)]
pub(crate) struct CacheKey {
    variant: u8,
    policy_seg: u64,
    body_fp: u64,
}

#[must_use]
pub(crate) fn multi_issuer_key(
    policy_seg: u64,
    tokens: &str,
    resource: &str,
    action: &str,
    context_trimmed: &str,
) -> CacheKey {
    CacheKey {
        variant: VARIANT_MULTI,
        policy_seg,
        body_fp: fingerprint_parts(&[tokens, resource, action, context_trimmed]),
    }
}

#[must_use]
pub(crate) fn unsigned_key(
    policy_seg: u64,
    principal_normalized: &str,
    resource: &str,
    action: &str,
    context_trimmed: &str,
) -> CacheKey {
    CacheKey {
        variant: VARIANT_UNSIGNED,
        policy_seg,
        body_fp: fingerprint_parts(&[principal_normalized, resource, action, context_trimmed]),
    }
}

fn fingerprint_parts(parts: &[&str]) -> u64 {
    let mut h = DefaultHasher::new();
    for p in parts {
        p.hash(&mut h);
    }
    h.finish()
}

#[must_use]
fn cache_key_string(key: &CacheKey) -> String {
    format!("{}:{}:{}", key.variant, key.policy_seg, key.body_fp)
}

fn sparkv_config_for_authz_cache(max_entries: usize) -> SparKvConfig {
    SparKvConfig {
        max_items: max_entries.max(1),
        max_item_size: 0,
        max_ttl: ChronoDuration::seconds(MAX_CACHE_TTL_SECS),
        default_ttl: ChronoDuration::seconds(300),
        auto_clear_expired: true,
        earliest_expiration_eviction: true,
    }
}

/// Per-backend-process decision cache (shared across connections in the same `PostgreSQL` backend).
pub(crate) struct AuthzDecisionCache {
    inner: Mutex<SparKV<bool>>,
    enabled: bool,
}

impl AuthzDecisionCache {
    fn new() -> Self {
        let max_entries = configured_cache_size();
        Self {
            inner: Mutex::new(SparKV::with_config(sparkv_config_for_authz_cache(max_entries))),
            enabled: max_entries > 0,
        }
    }

    /// Returns a cached decision when `ttl_secs > 0` and the key exists with a live TTL in `SparKV`.
    pub(crate) fn lookup(&self, ttl_secs: i32, key: &CacheKey) -> Option<bool> {
        if ttl_secs <= 0 || !self.enabled {
            return None;
        }
        let key_s = cache_key_string(key);
        let store = self.inner.lock().unwrap_or_else(PoisonError::into_inner);
        store.get(&key_s).copied()
    }

    /// Stores a successful authorization outcome with per-entry TTL (same as `cedarling.cache_ttl`
    /// at call time).
    pub(crate) fn store(&self, ttl_secs: i32, key: CacheKey, decision: bool) {
        if ttl_secs <= 0 || !self.enabled {
            return;
        }
        let ttl = ChronoDuration::seconds(i64::from(ttl_secs));
        let key_s = cache_key_string(&key);
        let mut store = self.inner.lock().unwrap_or_else(PoisonError::into_inner);
        let _ = store.set_with_ttl(&key_s, decision, ttl, &[]);
    }

    /// Clear all cached authorization decisions for the current backend process.
    pub(crate) fn clear_all(&self) {
        let mut store = self.inner.lock().unwrap_or_else(PoisonError::into_inner);
        store.clear();
    }

    /// Sub-second TTL for unit tests (production path uses whole seconds only).
    #[cfg(test)]
    fn store_ttl_chrono(&self, ttl: ChronoDuration, key: CacheKey, decision: bool) {
        let key_s = cache_key_string(&key);
        let mut store = self.inner.lock().unwrap_or_else(PoisonError::into_inner);
        let _ = store.set_with_ttl(&key_s, decision, ttl, &[]);
    }
}

static GLOBAL_CACHE: OnceLock<AuthzDecisionCache> = OnceLock::new();

pub(crate) fn global_cache() -> &'static AuthzDecisionCache {
    GLOBAL_CACHE.get_or_init(AuthzDecisionCache::new)
}

#[cfg(test)]
fn configured_cache_size() -> usize {
    8_192
}

#[cfg(not(test))]
fn configured_cache_size() -> usize {
    usize::try_from(guc_config::cache_size()).unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::thread;
    use std::time::Duration;

    #[test]
    fn ttl_zero_skips_store_and_lookup() {
        let c = AuthzDecisionCache::new();
        let key = multi_issuer_key(1, "{}", r#"{"x":1}"#, "A::\"a\"", "{}");
        c.store(0, key, true);
        assert_eq!(c.lookup(300, &key), None);
    }

    #[test]
    fn hit_then_miss_after_expiry() {
        let c = AuthzDecisionCache::new();
        let key = multi_issuer_key(7, "tok", "res", "act", "{}");
        c.store_ttl_chrono(ChronoDuration::milliseconds(50), key, false);
        assert_eq!(c.lookup(1, &key), Some(false));
        thread::sleep(Duration::from_millis(200));
        assert_eq!(c.lookup(1, &key), None);
    }

    #[test]
    fn multi_and_unsigned_keys_differ() {
        let seg = 42;
        let m = multi_issuer_key(seg, "{}", "r", "a", "{}");
        let u = unsigned_key(seg, "", "r", "a", "{}");
        assert_ne!(m, u);
    }
}
