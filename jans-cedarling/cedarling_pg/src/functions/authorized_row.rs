// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Row-level authorization helpers for RLS policies using pre-built JSONB resource data.

use pgrx::datum::{AnyElement, JsonB};
use pgrx::prelude::*;
use serde_json::json;

use crate::authz::bridge as authz_bridge;
use crate::authz::cache as authz_cache;
use crate::engine;
use crate::functions::error::CedarlingError;
use crate::observability::log as extension_log;
use crate::functions::authorized::{finalize_decision, finalize_error};
use crate::guc_config::{self, CedarlingMode};
#[cfg(not(test))]
use crate::guc_config::CedarlingStrategy;
use crate::observability::status as status;
use crate::observability::trace::{push_trace, AuthorizationTrace};

/// Authorize a JSONB resource under unsigned Cedar policy (no JWT tokens required).
///
/// `resource`: JSONB document containing a valid `EntityData` (`cedar_entity_mapping` required).
/// `action`: full Cedar action UID (e.g. `Jans::Action::"Read"`).
/// `context`: JSONB context object; `NULL` is treated as `{}`.
///
/// Inherits the same mode / fail-mode / shadow semantics and caching as
/// `cedarling_authorize_unsigned`. Suitable as an RLS `USING` predicate.
///
/// When `cedarling.strategy = mask` and the Cedar engine denies the request, this function
/// returns `true` (so RLS includes the row) and writes a trace with `decision=false, masked=true`.
/// Callers must apply column masking themselves by pairing this predicate with
/// `cedarling_mask_row(t, '<table>')` in the SELECT list.
#[pg_extern(volatile, parallel_restricted)]
#[allow(clippy::needless_pass_by_value)] // `#[pg_extern]` maps Rust parameters from PostgreSQL call convention; JsonB is moved in.
pub fn cedarling_authorized_row(
    resource: pgrx::datum::JsonB,
    action: Option<&str>,
    context: Option<pgrx::datum::JsonB>,
) -> bool {
    let Some(action) = action else {
        return finalize_error(&CedarlingError::RequestInvalid("NULL action".into()));
    };
    let action_trimmed = action.trim();
    if action_trimmed.is_empty() {
        return finalize_error(&CedarlingError::RequestInvalid("empty action".into()));
    }

    let context_value = context
        .map(|j| j.0)
        .or_else(|| {
            guc_config::context_utf8()
                .and_then(|s| serde_json::from_str::<serde_json::Value>(&s).ok())
        })
        .unwrap_or_else(|| json!({}));
    if !context_value.is_object() {
        return finalize_error(&CedarlingError::RequestInvalid(
            "context must be a JSON object".into(),
        ));
    }

    let resource_str = match serde_json::to_string(&resource.0) {
        Ok(s) => s,
        Err(e) => return finalize_error(&CedarlingError::JsonParsing(e.to_string())),
    };
    let context_str = match serde_json::to_string(&context_value) {
        Ok(s) => s,
        Err(e) => return finalize_error(&CedarlingError::JsonParsing(e.to_string())),
    };

    let (resource_type, resource_id) = crate::observability::trace::extract_entity_info(&resource_str);
    let shadow = matches!(guc_config::mode(), CedarlingMode::Shadow);

    status::record_request();

    let ttl = guc_config::cache_ttl_seconds();
    let cache_key = authz_cache::unsigned_key(
        authz_cache::policy_segment_from_bootstrap_path(),
        "",
        &resource_str,
        action_trimmed,
        &context_str,
    );
    match authz_cache::global_cache().lookup(ttl, &cache_key) {
        Ok(Some(decision)) => {
            return handle_row_cache_hit(
                decision,
                action_trimmed,
                &resource_type,
                &resource_id,
                shadow,
            );
        },
        Ok(None) => {},
        Err(_) => {
            let ce = CedarlingError::Engine("authorization cache mutex poisoned".into());
            return finalize_with_mask_strategy_on_error(&ce);
        },
    }

    let request = match authz_bridge::unsigned_request_from_json_parts(
        None,
        &resource_str,
        action_trimmed,
        &context_str,
    ) {
        Ok(r) => r,
        Err(e) => {
            extension_log::log_unsigned_bridge_failure(&e);
            status::record_error_msg(&e.to_string());
            let ce = CedarlingError::from(e);
            return finalize_with_mask_strategy_on_error(&ce);
        },
    };

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            status::record_error_msg(&e.to_string());
            let ce = CedarlingError::from(e);
            return finalize_with_mask_strategy_on_error(&ce);
        },
    };

    let start = std::time::Instant::now();
    let timestamp = chrono::Utc::now().to_rfc3339();

    let row_common = RowTraceCommon {
        timestamp: &timestamp,
        action: action_trimmed,
        resource_type: &resource_type,
        resource_id: &resource_id,
        shadow,
    };
    match authz_bridge::authorize_unsigned_outcome_for_request(engine.as_ref(), request) {
        Ok(outcome) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            authz_cache::global_cache().store(ttl, &cache_key, outcome.decision);
            handle_row_success(&row_common, duration_ms, outcome)
        },
        Err(e) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            extension_log::log_unsigned_bridge_failure(&e);
            let ce = CedarlingError::from(e);
            handle_row_error(&row_common, duration_ms, &ce)
        },
    }
}

fn handle_row_cache_hit(
    decision: bool,
    action: &str,
    resource_type: &str,
    resource_id: &str,
    shadow: bool,
) -> bool {
    status::record_cache_hit();
    status::record_decision(decision);
    let timestamp = chrono::Utc::now().to_rfc3339();
    let (final_decision, masked) = apply_mask_strategy(decision);
    push_trace(AuthorizationTrace {
        timestamp,
        action: action.to_string(),
        duration_ms: 0,
        decision: Some(decision),
        error_category: None,
        request_id: String::new(),
        resource_type: resource_type.to_string(),
        resource_id: resource_id.to_string(),
        principal_id: None,
        shadow,
        cache_hit: true,
        policy_hits: vec![],
        diag_errors: vec![],
        masked,
        policy_version: None,
        batch_id: None,
    });
    if masked { true } else { finalize_decision(final_decision) }
}

/// Common per-row trace fields shared by the success and error branches.
struct RowTraceCommon<'a> {
    timestamp: &'a str,
    action: &'a str,
    resource_type: &'a str,
    resource_id: &'a str,
    shadow: bool,
}

fn handle_row_success(
    c: &RowTraceCommon<'_>,
    duration_ms: u64,
    outcome: crate::authz::bridge::AuthorizeOutcome,
) -> bool {
    status::record_decision(outcome.decision);
    let (final_decision, masked) = apply_mask_strategy(outcome.decision);
    push_trace(AuthorizationTrace {
        timestamp: c.timestamp.to_string(),
        action: c.action.to_string(),
        duration_ms,
        decision: Some(outcome.decision),
        error_category: None,
        request_id: outcome.request_id,
        resource_type: c.resource_type.to_string(),
        resource_id: c.resource_id.to_string(),
        principal_id: None,
        shadow: c.shadow,
        cache_hit: false,
        policy_hits: outcome.policy_hits,
        diag_errors: outcome.diag_errors,
        masked,
        policy_version: None,
        batch_id: None,
    });
    if masked { true } else { finalize_decision(final_decision) }
}

fn handle_row_error(
    c: &RowTraceCommon<'_>,
    duration_ms: u64,
    ce: &CedarlingError,
) -> bool {
    status::record_error_msg(&ce.to_string());
    push_trace(AuthorizationTrace {
        timestamp: c.timestamp.to_string(),
        action: c.action.to_string(),
        duration_ms,
        decision: None,
        error_category: Some(ce.category()),
        request_id: String::new(),
        resource_type: c.resource_type.to_string(),
        resource_id: c.resource_id.to_string(),
        principal_id: None,
        shadow: c.shadow,
        cache_hit: false,
        policy_hits: vec![],
        diag_errors: vec![],
        masked: false,
        policy_version: None,
        batch_id: None,
    });
    finalize_with_mask_strategy_on_error(ce)
}

/// Finalize an error through fail-open/fail-closed mode and then apply masking strategy.
///
/// This keeps `strategy=mask` behavior consistent for explicit deny decisions and
/// deny-on-error outcomes (e.g. missing engine in fail-closed mode).
fn finalize_with_mask_strategy_on_error(err: &CedarlingError) -> bool {
    let decision = finalize_error(err);
    let (final_decision, masked) = apply_mask_strategy(decision);
    if masked { true } else { finalize_decision(final_decision) }
}

#[cfg(test)]
fn strategy_is_mask() -> bool {
    false
}

#[cfg(not(test))]
fn strategy_is_mask() -> bool {
    matches!(guc_config::strategy(), CedarlingStrategy::Mask)
}

/// When `strategy=mask` and `decision=false`, return `(false, true)` so the caller surfaces
/// the row as allowed and records `masked=true` in the trace. Otherwise `(decision, false)`.
fn apply_mask_strategy(decision: bool) -> (bool, bool) {
    if decision || !strategy_is_mask() {
        return (decision, false);
    }
    (false, true)
}

/// `AnyElement` wrapper for row-based unsigned authorization.
///
/// Materializes the row to canonical Cedar `EntityData` JSON and then reuses the JSONB
/// unsigned path (`cedarling_authorized_row`).
#[pg_extern(name = "cedarling_authorized_row", volatile, parallel_restricted)]
pub fn cedarling_authorized_row_from_anyelement(
    record: AnyElement,
    action: Option<&str>,
    context: Option<JsonB>,
) -> bool {
    let resource_json = match crate::resource::row::build_resource_json_from_row(record) {
        Ok(v) => v,
        Err(e) => return finalize_error(&CedarlingError::from(e)),
    };
    let resource_value: serde_json::Value = match serde_json::from_str(&resource_json) {
        Ok(v) => v,
        Err(e) => return finalize_error(&CedarlingError::JsonParsing(e.to_string())),
    };
    cedarling_authorized_row(JsonB(resource_value), action, context)
}

/// `AnyElement` wrapper for JWT/multi-issuer authorization.
///
/// Reads tokens from the fallback chain: explicit bundle (none here) → `cedarling.tokens` GUC.
#[pg_extern(volatile, parallel_restricted)]
pub fn cedarling_authorized_row_jwt(record: AnyElement, action: Option<&str>) -> bool {
    let Some(action) = action else {
        return finalize_error(&CedarlingError::RequestInvalid("NULL action".into()));
    };
    let resource_json = match crate::resource::row::build_resource_json_from_row(record) {
        Ok(v) => v,
        Err(e) => return finalize_error(&CedarlingError::from(e)),
    };
    crate::functions::authorized::cedarling_authorized(&resource_json, None, action)
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    #[test]
    fn context_null_treated_as_empty_object() {
        let context_value = None::<pgrx::datum::JsonB>
            .map_or_else(|| json!({}), |j: pgrx::datum::JsonB| j.0);
        assert!(context_value.is_object());
        assert_eq!(context_value, json!({}));
    }

    #[test]
    fn apply_mask_strategy_noop_when_decision_true() {
        let (d, masked) = apply_mask_strategy(true);
        assert!(d);
        assert!(!masked);
    }

    #[test]
    fn apply_mask_strategy_noop_when_strategy_filter() {
        // strategy() returns Filter by default in tests (no pg GUC).
        let (d, masked) = apply_mask_strategy(false);
        assert!(!d);
        assert!(!masked, "filter strategy should not mask");
    }
}
