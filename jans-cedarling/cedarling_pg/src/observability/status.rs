// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Per-backend authorization counters exposed via `cedarling_status()`.

use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Mutex, OnceLock};

use pgrx::prelude::*;
use serde_json::json;

static TOTAL_REQUESTS: AtomicU64 = AtomicU64::new(0);
static ALLOWED: AtomicU64 = AtomicU64::new(0);
static DENIED: AtomicU64 = AtomicU64::new(0);
static ERRORS: AtomicU64 = AtomicU64::new(0);
static CACHE_HITS: AtomicU64 = AtomicU64::new(0);

/// RFC 3339 timestamp of the first `record_request()` call in this backend process.
static START_TIME: OnceLock<String> = OnceLock::new();

/// Most recent error message and timestamp `(message, rfc3339_timestamp)`.
static LAST_ERROR: Mutex<Option<(String, String)>> = Mutex::new(None);

/// Timestamp of the most recent policy update triggered by `cedarling_use_policy` / `cedarling_rollback_policy`.
static LAST_POLICY_UPDATE: Mutex<Option<String>> = Mutex::new(None);

pub(crate) fn record_request() {
    START_TIME.get_or_init(|| chrono::Utc::now().to_rfc3339());
    TOTAL_REQUESTS.fetch_add(1, Ordering::Relaxed);
}

pub(crate) fn record_decision(allowed: bool) {
    if allowed {
        ALLOWED.fetch_add(1, Ordering::Relaxed);
    } else {
        DENIED.fetch_add(1, Ordering::Relaxed);
    }
}

/// Increments the error counter.
///
/// Kept for backward compatibility; prefer [`record_error_msg`] when an error message is available.
#[allow(dead_code)]
pub(crate) fn record_error() {
    ERRORS.fetch_add(1, Ordering::Relaxed);
}

/// Increments the error counter and stores `msg` as the most recent error.
pub(crate) fn record_error_msg(msg: &str) {
    ERRORS.fetch_add(1, Ordering::Relaxed);
    let ts = chrono::Utc::now().to_rfc3339();
    if let Ok(mut guard) = LAST_ERROR.lock() {
        *guard = Some((msg.to_string(), ts));
    }
}

pub(crate) fn record_cache_hit() {
    CACHE_HITS.fetch_add(1, Ordering::Relaxed);
}

/// Records a timestamp for the most recent policy update.
pub(crate) fn record_policy_update() {
    let ts = chrono::Utc::now().to_rfc3339();
    if let Ok(mut guard) = LAST_POLICY_UPDATE.lock() {
        *guard = Some(ts);
    }
}

#[cfg(not(test))]
fn policy_version_for_status() -> String {
    crate::guc_config::policy_version_utf8().unwrap_or_else(|| "latest".to_string())
}

#[cfg(test)]
fn policy_version_for_status() -> String {
    "latest".to_string()
}

#[cfg(not(test))]
fn engine_info_for_status() -> (bool, u64, u64) {
    use cedarling::TrustedIssuerLoadingInfo;
    match crate::engine::peek_cedarling() {
        Some(e) => (
            true,
            e.loaded_trusted_issuers_count() as u64,
            e.failed_trusted_issuer_ids().len() as u64,
        ),
        None => (false, 0, 0),
    }
}

#[cfg(test)]
fn engine_info_for_status() -> (bool, u64, u64) {
    (false, 0, 0)
}

fn classify_status(
    engine_loaded: bool,
    trusted_issuers_failed: u64,
    total_requests: u64,
    failed_requests: u64,
) -> &'static str {
    if !engine_loaded {
        "unhealthy"
    } else if trusted_issuers_failed > 0
        || (total_requests > 0 && failed_requests as f64 / total_requests as f64 >= 0.25)
    {
        "degraded"
    } else {
        "healthy"
    }
}

/// Returns per-backend authorization counters as a JSONB document.
///
/// Counters are process-local and reset on PostgreSQL backend restart.
///
/// Fields:
/// - `status`: `"healthy"` | `"degraded"` | `"unhealthy"`.
/// - `start_time`: RFC 3339 timestamp of the first request (or `null` if no requests yet).
/// - `policy_version`: active policy version from `cedarling.policy_version` GUC.
/// - `engine_loaded`: whether the Cedarling engine is currently initialized.
/// - `trusted_issuers_loaded`: number of successfully loaded trusted issuers.
/// - `trusted_issuers_failed`: number of trusted issuers that failed to load.
/// - `total_requests`: all authorization calls after basic input validation (including cache hits).
/// - `successful_requests`: `allowed + denied` (calls that received a decision).
/// - `failed_requests`: calls that hit an error path.
/// - `allowed`: raw Cedarling ALLOW decisions (before shadow/fail-mode adjustment).
/// - `denied`: raw Cedarling DENY decisions.
/// - `errors`: calls that hit an error path (engine down, parse failure, etc.).
/// - `cache_hits`: calls served from the in-process decision cache.
/// - `cache_hit_rate`: `cache_hits / total_requests` (0.0 when no requests).
/// - `last_error`: most recent error message (`null` if none).
/// - `last_error_time`: RFC 3339 timestamp of the most recent error (`null` if none).
#[pg_extern]
pub fn cedarling_status() -> pgrx::datum::JsonB {
    let total = TOTAL_REQUESTS.load(Ordering::Relaxed);
    let allowed = ALLOWED.load(Ordering::Relaxed);
    let denied = DENIED.load(Ordering::Relaxed);
    let errors = ERRORS.load(Ordering::Relaxed);
    let cache_hits = CACHE_HITS.load(Ordering::Relaxed);

    let successful_requests = allowed + denied;
    let failed_requests = errors;
    let cache_hit_rate = if total > 0 {
        cache_hits as f64 / total as f64
    } else {
        0.0
    };

    let (engine_loaded, trusted_issuers_loaded, trusted_issuers_failed) = engine_info_for_status();
    let policy_version = policy_version_for_status();

    let status = classify_status(engine_loaded, trusted_issuers_failed, total, failed_requests);

    let start_time: serde_json::Value = START_TIME
        .get()
        .map(|s| serde_json::Value::String(s.clone()))
        .unwrap_or(serde_json::Value::Null);

    let (last_error, last_error_time) = LAST_ERROR
        .lock()
        .ok()
        .and_then(|g| g.clone())
        .map(|(msg, ts)| {
            (
                serde_json::Value::String(msg),
                serde_json::Value::String(ts),
            )
        })
        .unwrap_or((serde_json::Value::Null, serde_json::Value::Null));

    let last_policy_update: serde_json::Value = LAST_POLICY_UPDATE
        .lock()
        .ok()
        .and_then(|g| g.clone())
        .map(serde_json::Value::String)
        .unwrap_or(serde_json::Value::Null);

    let obj = json!({
        "status":                  status,
        "start_time":              start_time,
        "policy_version":          policy_version,
        "last_policy_update":      last_policy_update,
        "engine_loaded":           engine_loaded,
        "trusted_issuers_loaded":  trusted_issuers_loaded,
        "trusted_issuers_failed":  trusted_issuers_failed,
        "total_requests":          total,
        "successful_requests":     successful_requests,
        "failed_requests":         failed_requests,
        "allowed":                 allowed,
        "denied":                  denied,
        "errors":                  errors,
        "cache_hits":              cache_hits,
        "cache_hit_rate":          cache_hit_rate,
        "last_error":              last_error,
        "last_error_time":         last_error_time,
    });

    pgrx::datum::JsonB(obj)
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
        // New fields
        assert!(v.get("status").is_some());
        assert!(v.get("engine_loaded").is_some());
        assert!(v.get("successful_requests").is_some());
        assert!(v.get("failed_requests").is_some());
        assert!(v.get("cache_hit_rate").is_some());
    }

    #[test]
    fn record_error_msg_stores_message() {
        let e0 = ERRORS.load(Ordering::Relaxed);
        record_error_msg("test error");
        assert_eq!(ERRORS.load(Ordering::Relaxed), e0 + 1);
        let guard = LAST_ERROR.lock().expect("lock");
        let (msg, _ts) = guard.as_ref().expect("last error should be set");
        assert_eq!(msg, "test error");
    }

    #[test]
    fn record_policy_update_sets_timestamp() {
        record_policy_update();
        let guard = LAST_POLICY_UPDATE.lock().expect("lock");
        assert!(guard.is_some(), "last_policy_update should be set after record_policy_update()");
    }

    #[test]
    fn classify_status_covers_unhealthy_degraded_healthy() {
        assert_eq!(
            classify_status(false, 0, 100, 0),
            "unhealthy",
            "engine not loaded must be unhealthy"
        );
        assert_eq!(
            classify_status(true, 1, 100, 0),
            "degraded",
            "failed trusted issuers must degrade status"
        );
        assert_eq!(
            classify_status(true, 0, 4, 1),
            "degraded",
            "error rate >= 25% must degrade status"
        );
        assert_eq!(
            classify_status(true, 0, 100, 1),
            "healthy",
            "loaded engine with low error rate and no issuer failures is healthy"
        );
    }
}
