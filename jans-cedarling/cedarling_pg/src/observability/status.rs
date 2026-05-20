// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Per-backend authorization counters exposed via `cedarling_status()`.

use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Mutex, OnceLock};

use pgrx::prelude::*;
use serde_json::json;

use crate::sync_mutex;

static TOTAL_REQUESTS: AtomicU64 = AtomicU64::new(0);
static ALLOWED: AtomicU64 = AtomicU64::new(0);
static DENIED: AtomicU64 = AtomicU64::new(0);
static ERRORS: AtomicU64 = AtomicU64::new(0);
static CACHE_HITS: AtomicU64 = AtomicU64::new(0);

/// RFC 3339 timestamp of the first `record_request()` call in this backend process.
static FIRST_REQUEST_TIME: OnceLock<String> = OnceLock::new();

/// Most recent error message and timestamp `(message, rfc3339_timestamp)`.
static LAST_ERROR: Mutex<Option<(String, String)>> = Mutex::new(None);

/// Cap for `last_error` and other status strings sourced from user-controlled input.
const STATUS_TEXT_MAX_CHARS: usize = 256;

/// Cap for `policy_version` echoed from the `cedarling.policy_version` GUC.
const POLICY_VERSION_MAX_CHARS: usize = 128;

fn truncate_status_text(s: &str, max_chars: usize) -> String {
    if s.chars().count() <= max_chars {
        return s.to_string();
    }
    s.chars().take(max_chars).collect()
}

/// Timestamp of the most recent policy update triggered by `cedarling_use_policy` / `cedarling_rollback_policy`.
static LAST_POLICY_UPDATE: Mutex<Option<String>> = Mutex::new(None);

pub(crate) fn record_request() {
    FIRST_REQUEST_TIME.get_or_init(|| chrono::Utc::now().to_rfc3339());
    TOTAL_REQUESTS.fetch_add(1, Ordering::Relaxed);
}

pub(crate) fn record_decision(allowed: bool) {
    if allowed {
        ALLOWED.fetch_add(1, Ordering::Relaxed);
    } else {
        DENIED.fetch_add(1, Ordering::Relaxed);
    }
}

/// Increments the error counter and stores `msg` as the most recent error.
pub(crate) fn record_error_msg(msg: &str) {
    ERRORS.fetch_add(1, Ordering::Relaxed);
    let ts = chrono::Utc::now().to_rfc3339();
    if let Ok(mut guard) = sync_mutex::lock(&LAST_ERROR) {
        *guard = Some((truncate_status_text(msg, STATUS_TEXT_MAX_CHARS), ts));
    }
}

/// Inject errors for unit tests (explicit failure path for degraded-status checks).
#[cfg(test)]
pub(crate) fn force_error_for_test(message: &str) {
    record_error_msg(message);
}

pub(crate) fn record_cache_hit() {
    CACHE_HITS.fetch_add(1, Ordering::Relaxed);
}

/// Records a timestamp for the most recent policy update.
pub(crate) fn record_policy_update() {
    let ts = chrono::Utc::now().to_rfc3339();
    if let Ok(mut guard) = sync_mutex::lock(&LAST_POLICY_UPDATE) {
        *guard = Some(ts);
    }
}

#[cfg(not(test))]
fn policy_version_for_status() -> String {
    let raw = crate::guc_config::policy_version_utf8().unwrap_or_else(|| "latest".to_string());
    truncate_status_text(&raw, POLICY_VERSION_MAX_CHARS)
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

/// Ratio of cache hits to total requests, expressed as a fraction in `[0.0, 1.0]`.
///
/// Both counters are `u64` for atomic arithmetic but in practice never exceed
/// `2^32` per backend, well inside `f64`'s mantissa. The narrow `#[allow]` on
/// the cast documents that the precision loss is bounded by Postgres backend
/// lifetime, not by user input.
#[allow(clippy::cast_precision_loss)]
fn cache_hit_rate(cache_hits: u64, total: u64) -> f64 {
    cache_hits as f64 / total as f64
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
        || (total_requests > 0 && failed_requests.saturating_mul(4) >= total_requests)
    {
        "degraded"
    } else {
        "healthy"
    }
}

/// Returns per-backend authorization counters as a JSONB document.
///
/// Counters are process-local and reset on `PostgreSQL` backend restart.
///
/// Fields:
/// - `status`: `"healthy"` | `"degraded"` | `"unhealthy"`.
/// - `first_request_time`: RFC 3339 timestamp of the first authorize call in this backend (or `null` if none yet).
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
#[pg_extern(stable)]
pub fn cedarling_status() -> pgrx::datum::JsonB {
    let total = TOTAL_REQUESTS.load(Ordering::Relaxed);
    let allowed = ALLOWED.load(Ordering::Relaxed);
    let denied = DENIED.load(Ordering::Relaxed);
    let errors = ERRORS.load(Ordering::Relaxed);
    let cache_hits = CACHE_HITS.load(Ordering::Relaxed);

    let successful_requests = allowed + denied;
    let failed_requests = errors;
    let cache_hit_rate = if total > 0 {
        cache_hit_rate(cache_hits, total)
    } else {
        0.0
    };

    let (engine_loaded, trusted_issuers_loaded, trusted_issuers_failed) = engine_info_for_status();
    let policy_version = policy_version_for_status();

    let status = classify_status(
        engine_loaded,
        trusted_issuers_failed,
        total,
        failed_requests,
    );

    let first_request_time: serde_json::Value =
        FIRST_REQUEST_TIME.get().map_or(serde_json::Value::Null, |s| {
            serde_json::Value::String(s.clone())
        });

    let (last_error, last_error_time) = sync_mutex::lock(&LAST_ERROR).ok().and_then(|g| g.clone()).map_or(
        (serde_json::Value::Null, serde_json::Value::Null),
        |(msg, ts)| {
            (
                serde_json::Value::String(msg),
                serde_json::Value::String(ts),
            )
        },
    );

    let last_policy_update: serde_json::Value = sync_mutex::lock(&LAST_POLICY_UPDATE)
        .ok()
        .and_then(|g| g.clone())
        .map_or(serde_json::Value::Null, serde_json::Value::String);

    let obj = json!({
        "status":                  status,
        "first_request_time":      first_request_time,
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
        record_error_msg("unit test error");
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
    fn record_error_msg_truncates_long_messages() {
        let long = "x".repeat(STATUS_TEXT_MAX_CHARS + 50);
        record_error_msg(&long);
        let guard = LAST_ERROR.lock().expect("lock");
        let (msg, _) = guard.as_ref().expect("last error should be set");
        assert_eq!(msg.chars().count(), STATUS_TEXT_MAX_CHARS);
        assert_eq!(msg.as_str(), "x".repeat(STATUS_TEXT_MAX_CHARS));
    }

    #[test]
    fn truncate_status_text_respects_char_boundary_not_byte_boundary() {
        let s = "é".repeat(STATUS_TEXT_MAX_CHARS + 1);
        let out = truncate_status_text(&s, STATUS_TEXT_MAX_CHARS);
        assert_eq!(out.chars().count(), STATUS_TEXT_MAX_CHARS);
    }

    #[test]
    fn truncate_status_text_caps_policy_version_length() {
        let long = "v".repeat(POLICY_VERSION_MAX_CHARS + 20);
        let out = truncate_status_text(&long, POLICY_VERSION_MAX_CHARS);
        assert_eq!(out.chars().count(), POLICY_VERSION_MAX_CHARS);
    }

    #[test]
    fn record_policy_update_sets_timestamp() {
        record_policy_update();
        let guard = LAST_POLICY_UPDATE.lock().expect("lock");
        assert!(
            guard.is_some(),
            "last_policy_update should be set after record_policy_update()"
        );
    }

    #[test]
    fn force_error_for_test_increments_error_counter() {
        let e0 = ERRORS.load(Ordering::Relaxed);
        force_error_for_test("injected test error");
        assert_eq!(ERRORS.load(Ordering::Relaxed), e0 + 1);
    }

    #[test]
    fn forced_errors_trigger_degraded_when_engine_loaded() {
        let e0 = ERRORS.load(Ordering::Relaxed);
        let t0 = TOTAL_REQUESTS.load(Ordering::Relaxed);
        for _ in 0..4 {
            force_error_for_test("injected");
        }
        for _ in 0..4 {
            record_request();
        }
        let total = TOTAL_REQUESTS.load(Ordering::Relaxed) - t0;
        let errors = ERRORS.load(Ordering::Relaxed) - e0;
        assert_eq!(
            classify_status(true, 0, total, errors),
            "degraded",
            "four injected errors across four requests must degrade status"
        );
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
