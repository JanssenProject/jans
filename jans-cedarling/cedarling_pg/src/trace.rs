// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Per-backend authorization trace ring buffer and diagnostic SQL functions.
//!
//! `cedarling_last_trace()` / `cedarling_recent_traces(limit)` surface the rolling log of
//! engine calls so operators can inspect recent decisions without enabling verbose server logging.
//!
//! `cedarling_explain(resource_json, action)` runs a one-off unsigned authorization outside the
//! cache / counter path — useful for interactive policy debugging in `psql`.

use std::collections::VecDeque;
use std::sync::Mutex;
use std::time::Instant;

use chrono::Utc;
use pgrx::prelude::*;
use serde_json::{json, Value};

use crate::authz_bridge;
use crate::engine;

/// Maximum number of authorization traces kept per backend process.
const MAX_TRACES: usize = 64;

/// A single recorded authorization event.
///
/// Resource JSON and token data are intentionally excluded to keep traces safe for server logs.
#[derive(Debug, Clone)]
pub(crate) struct AuthorizationTrace {
    /// RFC 3339 timestamp of when the authorization was initiated.
    pub timestamp: String,
    /// Cedar action UID (e.g. `Jans::Action::"Read"`).
    pub action: String,
    /// Elapsed time for the Cedarling engine call in milliseconds.
    pub duration_ms: u64,
    /// Raw Cedarling decision before mode / fail-mode adjustment; `None` on engine error.
    pub decision: Option<bool>,
    /// Error category from [`crate::error::CedarlingError::category`]; `None` on success.
    pub error_category: Option<&'static str>,
}

static RING: Mutex<VecDeque<AuthorizationTrace>> = Mutex::new(VecDeque::new());

/// Push a trace into the ring buffer. Oldest entry is silently dropped when the buffer is full.
pub(crate) fn push_trace(trace: AuthorizationTrace) {
    if let Ok(mut buf) = RING.lock() {
        if buf.len() >= MAX_TRACES {
            buf.pop_front();
        }
        buf.push_back(trace);
    }
}

fn trace_to_value(t: &AuthorizationTrace) -> Value {
    let decision_str: Value = match t.decision {
        Some(true) => json!("allow"),
        Some(false) => json!("deny"),
        None => Value::Null,
    };
    let mut obj = json!({
        "timestamp":   t.timestamp,
        "action":      t.action,
        "duration_ms": t.duration_ms,
        "decision":    decision_str,
    });
    if let Some(cat) = t.error_category {
        obj["error_category"] = json!(cat);
    }
    obj
}

/// Returns the most recent authorization trace recorded in this backend process, or `NULL` if none.
///
/// Traces are per-backend-process and reset on backend reconnect. Resource JSON and token data
/// are never included — only action, decision, duration, and optional error category.
#[pg_extern]
pub fn cedarling_last_trace() -> Option<pgrx::datum::JsonB> {
    RING.lock()
        .ok()
        .and_then(|buf| buf.back().map(|t| pgrx::datum::JsonB(trace_to_value(t))))
}

/// Returns the most recent `limit` authorization traces as a JSONB array (newest first).
///
/// `limit` defaults to 10 when `NULL`; clamped to 0..=64. Traces are per-backend-process.
#[pg_extern]
pub fn cedarling_recent_traces(limit: Option<i32>) -> pgrx::datum::JsonB {
    let count = limit.unwrap_or(10).max(0) as usize;
    let count = count.min(MAX_TRACES);
    let arr: Vec<Value> = RING
        .lock()
        .map(|buf| buf.iter().rev().take(count).map(trace_to_value).collect())
        .unwrap_or_default();
    pgrx::datum::JsonB(json!(arr))
}

/// Runs a one-off unsigned Cedar authorization and returns a diagnostic JSONB trace.
///
/// **Bypasses the decision cache** and **does not increment `cedarling_status()` counters** —
/// safe to call repeatedly for interactive debugging.
///
/// `resource_json`: EntityData JSON (must contain `cedar_entity_mapping`).
/// `action`: full Cedar action UID (e.g. `Jans::Action::"Read"`).
///
/// The returned object always contains `timestamp`, `action`, `duration_ms`, and `decision`
/// (`"allow"` | `"deny"` | `null`). An `error` field is added on failure.
#[pg_extern]
pub fn cedarling_explain(resource_json: &str, action: &str) -> pgrx::datum::JsonB {
    let start = Instant::now();
    let timestamp = Utc::now().to_rfc3339();
    let action_trimmed = action.trim();
    let resource_trimmed = resource_json.trim();

    let elapsed_ms = || start.elapsed().as_millis() as u64;

    if action_trimmed.is_empty() {
        return pgrx::datum::JsonB(json!({
            "timestamp":   timestamp,
            "action":      action_trimmed,
            "duration_ms": elapsed_ms(),
            "decision":    Value::Null,
            "error":       "empty action",
        }));
    }
    if resource_trimmed.is_empty() {
        return pgrx::datum::JsonB(json!({
            "timestamp":   timestamp,
            "action":      action_trimmed,
            "duration_ms": elapsed_ms(),
            "decision":    Value::Null,
            "error":       "empty resource JSON",
        }));
    }

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            return pgrx::datum::JsonB(json!({
                "timestamp":        timestamp,
                "action":           action_trimmed,
                "duration_ms":      elapsed_ms(),
                "decision":         Value::Null,
                "error":            format!("engine unavailable: {e}"),
                "engine_available": false,
            }));
        },
    };

    let request = match authz_bridge::unsigned_request_from_json_parts(
        None,
        resource_trimmed,
        action_trimmed,
        "{}",
    ) {
        Ok(r) => r,
        Err(e) => {
            return pgrx::datum::JsonB(json!({
                "timestamp":        timestamp,
                "action":           action_trimmed,
                "duration_ms":      elapsed_ms(),
                "decision":         Value::Null,
                "error":            e.to_string(),
                "engine_available": true,
            }));
        },
    };

    match authz_bridge::authorize_unsigned_decision_for_request(engine.as_ref(), request) {
        Ok(decision) => pgrx::datum::JsonB(json!({
            "timestamp":        timestamp,
            "action":           action_trimmed,
            "duration_ms":      elapsed_ms(),
            "decision":         if decision { "allow" } else { "deny" },
            "engine_available": true,
        })),
        Err(e) => pgrx::datum::JsonB(json!({
            "timestamp":        timestamp,
            "action":           action_trimmed,
            "duration_ms":      elapsed_ms(),
            "decision":         Value::Null,
            "error":            e.to_string(),
            "engine_available": true,
        })),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_trace(decision: Option<bool>, err_cat: Option<&'static str>) -> AuthorizationTrace {
        AuthorizationTrace {
            timestamp: "2026-04-27T00:00:00Z".into(),
            action: "T::Action::\"Read\"".into(),
            duration_ms: 1,
            decision,
            error_category: err_cat,
        }
    }

    #[test]
    fn push_then_last_trace_returns_some() {
        push_trace(make_trace(Some(true), None));
        assert!(
            cedarling_last_trace().is_some(),
            "ring should have at least one trace after push"
        );
        let v = cedarling_last_trace().unwrap().0;
        assert_eq!(v["action"], "T::Action::\"Read\"");
        assert_eq!(v["duration_ms"], 1u64);
    }

    #[test]
    fn allow_and_deny_serialize_correctly() {
        let allow_v = trace_to_value(&make_trace(Some(true), None));
        assert_eq!(allow_v["decision"], "allow");

        let deny_v = trace_to_value(&make_trace(Some(false), None));
        assert_eq!(deny_v["decision"], "deny");

        let err_v = trace_to_value(&make_trace(None, Some("engine")));
        assert!(err_v["decision"].is_null());
        assert_eq!(err_v["error_category"], "engine");
    }

    #[test]
    fn trace_without_error_omits_error_category_field() {
        let v = trace_to_value(&make_trace(Some(true), None));
        assert!(
            v.get("error_category").is_none(),
            "error_category should not be present on success"
        );
    }

    #[test]
    fn ring_does_not_exceed_max_traces() {
        for i in 0..MAX_TRACES + 5 {
            push_trace(AuthorizationTrace {
                timestamp: "2026-01-01T00:00:00Z".into(),
                action: format!("T::A::\"{i}\""),
                duration_ms: i as u64,
                decision: Some(true),
                error_category: None,
            });
        }
        let buf = RING.lock().expect("lock");
        assert!(
            buf.len() <= MAX_TRACES,
            "ring buffer must never exceed MAX_TRACES ({MAX_TRACES})"
        );
    }

    #[test]
    fn recent_traces_limit_zero_returns_empty_array() {
        push_trace(make_trace(Some(true), None));
        let result = cedarling_recent_traces(Some(0));
        let arr = result.0.as_array().expect("should be array");
        assert!(arr.is_empty(), "limit=0 should return empty array");
    }

    #[test]
    fn recent_traces_null_limit_defaults_to_ten() {
        let result = cedarling_recent_traces(None);
        let arr = result.0.as_array().expect("should be array");
        assert!(arr.len() <= 10, "null limit should default to 10 entries");
    }
}
