// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Per-backend authorization counters exposed via `cedarling_status()`.

use std::sync::atomic::{AtomicU64, Ordering};

use pgrx::prelude::*;
use serde_json::json;

static TOTAL_REQUESTS: AtomicU64 = AtomicU64::new(0);
static ALLOWED: AtomicU64 = AtomicU64::new(0);
static DENIED: AtomicU64 = AtomicU64::new(0);
static ERRORS: AtomicU64 = AtomicU64::new(0);
static CACHE_HITS: AtomicU64 = AtomicU64::new(0);

pub(crate) fn record_request() {
    TOTAL_REQUESTS.fetch_add(1, Ordering::Relaxed);
}

pub(crate) fn record_decision(allowed: bool) {
    if allowed {
        ALLOWED.fetch_add(1, Ordering::Relaxed);
    } else {
        DENIED.fetch_add(1, Ordering::Relaxed);
    }
}

pub(crate) fn record_error() {
    ERRORS.fetch_add(1, Ordering::Relaxed);
}

pub(crate) fn record_cache_hit() {
    CACHE_HITS.fetch_add(1, Ordering::Relaxed);
}

/// Returns per-backend authorization counters as a JSONB document.
///
/// Counters are process-local and reset on PostgreSQL backend restart.
///
/// Fields:
/// - `total_requests`: all authorization calls (valid input, past cache check).
/// - `allowed`: raw Cedarling ALLOW decisions (before shadow/fail-mode adjustment).
/// - `denied`: raw Cedarling DENY decisions.
/// - `errors`: calls that hit an error path (engine down, parse failure, etc.).
/// - `cache_hits`: calls served from the in-process decision cache.
#[pg_extern]
pub fn cedarling_status() -> pgrx::datum::JsonB {
    pgrx::datum::JsonB(json!({
        "total_requests": TOTAL_REQUESTS.load(Ordering::Relaxed),
        "allowed":        ALLOWED.load(Ordering::Relaxed),
        "denied":         DENIED.load(Ordering::Relaxed),
        "errors":         ERRORS.load(Ordering::Relaxed),
        "cache_hits":     CACHE_HITS.load(Ordering::Relaxed),
    }))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn counters_are_independent() {
        let t0 = TOTAL_REQUESTS.load(Ordering::Relaxed);
        let a0 = ALLOWED.load(Ordering::Relaxed);
        let d0 = DENIED.load(Ordering::Relaxed);
        let e0 = ERRORS.load(Ordering::Relaxed);
        let c0 = CACHE_HITS.load(Ordering::Relaxed);

        record_request();
        record_decision(true);
        record_decision(false);
        record_error();
        record_cache_hit();

        assert_eq!(TOTAL_REQUESTS.load(Ordering::Relaxed), t0 + 1);
        assert_eq!(ALLOWED.load(Ordering::Relaxed), a0 + 1);
        assert_eq!(DENIED.load(Ordering::Relaxed), d0 + 1);
        assert_eq!(ERRORS.load(Ordering::Relaxed), e0 + 1);
        assert_eq!(CACHE_HITS.load(Ordering::Relaxed), c0 + 1);
    }

    #[test]
    fn cedarling_status_json_has_required_keys() {
        let status = cedarling_status();
        let v = &status.0;
        assert!(v.get("total_requests").is_some());
        assert!(v.get("allowed").is_some());
        assert!(v.get("denied").is_some());
        assert!(v.get("errors").is_some());
        assert!(v.get("cache_hits").is_some());
    }
}
