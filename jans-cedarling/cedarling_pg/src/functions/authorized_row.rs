// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Row-level authorization helpers for RLS policies using pre-built JSONB resource data.

use pgrx::datum::{AnyElement, JsonB};
use pgrx::prelude::*;
use serde_json::json;

use crate::authz_bridge;
use crate::authz_cache;
use crate::engine;
use crate::error::CedarlingError;
use crate::extension_log;
use crate::functions::authorized::{finalize_decision, finalize_error};
use crate::guc_config::{self, CedarlingMode, CedarlingStrategy};
use crate::status;
use crate::trace::{push_trace, AuthorizationTrace};

/// Authorize a JSONB resource under unsigned Cedar policy (no JWT tokens required).
///
/// `resource`: JSONB document containing a valid `EntityData` (`cedar_entity_mapping` required).
/// `action`: full Cedar action UID (e.g. `Jans::Action::"Read"`).
/// `context`: JSONB context object; `NULL` is treated as `{}`.
///
/// Inherits the same mode / fail-mode / shadow semantics and caching as
/// `cedarling_authorize_unsigned`. Suitable as an RLS `USING` predicate.
///
/// When `cedarling.strategy = mask` and the Cedar engine denies the request, this function:
/// 1. Computes a masked version of the row (using `cedarling.mask_rules` + default registry).
/// 2. Stashes it via `cedarling_get_masked_row()`.
/// 3. Returns `true` so RLS includes the row (with masked sensitive columns).
/// 4. Writes a trace with `decision=false, masked=true`.
#[pg_extern]
pub fn cedarling_authorized_row(
    resource: pgrx::datum::JsonB,
    action: &str,
    context: Option<pgrx::datum::JsonB>,
) -> bool {
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

    let (resource_type, resource_id) = crate::trace::extract_entity_info(&resource_str);
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
    if let Some(decision) = authz_cache::global_cache().lookup(ttl, &cache_key) {
        status::record_cache_hit();
        status::record_decision(decision);
        let timestamp = chrono::Utc::now().to_rfc3339();

        // Mask strategy on cached deny
        let (final_decision, masked) = apply_mask_strategy(decision, &resource, &timestamp);

        push_trace(AuthorizationTrace {
            timestamp,
            action: action_trimmed.to_string(),
            duration_ms: 0,
            decision: Some(decision),
            error_category: None,
            request_id: String::new(),
            resource_type,
            resource_id,
            principal_id: None,
            shadow,
            cache_hit: true,
            policy_hits: vec![],
            diag_errors: vec![],
            masked,
        });
        return if masked { true } else { finalize_decision(final_decision) };
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
            return finalize_with_mask_strategy_on_error(&ce, &resource);
        },
    };

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            status::record_error_msg(&e.to_string());
            let ce = CedarlingError::from(e);
            return finalize_with_mask_strategy_on_error(&ce, &resource);
        },
    };

    let start = std::time::Instant::now();
    let timestamp = chrono::Utc::now().to_rfc3339();

    match authz_bridge::authorize_unsigned_outcome_for_request(engine.as_ref(), request) {
        Ok(outcome) => {
            let duration_ms = start.elapsed().as_millis() as u64;
            authz_cache::global_cache().store(ttl, cache_key, outcome.decision);
            status::record_decision(outcome.decision);

            // Mask strategy on live deny
            let (final_decision, masked) =
                apply_mask_strategy(outcome.decision, &resource, &timestamp);

            push_trace(AuthorizationTrace {
                timestamp,
                action: action_trimmed.to_string(),
                duration_ms,
                decision: Some(outcome.decision),
                error_category: None,
                request_id: outcome.request_id,
                resource_type,
                resource_id,
                principal_id: None,
                shadow,
                cache_hit: false,
                policy_hits: outcome.policy_hits,
                diag_errors: outcome.diag_errors,
                masked,
            });
            if masked { true } else { finalize_decision(final_decision) }
        },
        Err(e) => {
            let duration_ms = start.elapsed().as_millis() as u64;
            extension_log::log_unsigned_bridge_failure(&e);
            let ce = CedarlingError::from(e);
            status::record_error_msg(&ce.to_string());
            push_trace(AuthorizationTrace {
                timestamp,
                action: action_trimmed.to_string(),
                duration_ms,
                decision: None,
                error_category: Some(ce.category()),
                request_id: String::new(),
                resource_type,
                resource_id,
                principal_id: None,
                shadow,
                cache_hit: false,
                policy_hits: vec![],
                diag_errors: vec![],
                masked: false,
            });
            finalize_with_mask_strategy_on_error(&ce, &resource)
        },
    }
}

/// Finalize an error through fail-open/fail-closed mode and then apply masking strategy.
///
/// This keeps `strategy=mask` behavior consistent for explicit deny decisions and
/// deny-on-error outcomes (e.g. missing engine in fail-closed mode).
fn finalize_with_mask_strategy_on_error(err: &CedarlingError, resource: &JsonB) -> bool {
    let decision = finalize_error(err);
    let (final_decision, masked) = apply_mask_strategy(decision, resource, "");
    if masked { true } else { finalize_decision(final_decision) }
}

/// When `strategy=mask` and `decision=false`, compute + stash the masked row and return
/// `(false, true)`. Otherwise return `(decision, false)`.
fn apply_mask_strategy(
    decision: bool,
    resource: &JsonB,
    _timestamp: &str,
) -> (bool, bool) {
    if decision || !matches!(guc_config::strategy(), CedarlingStrategy::Mask) {
        return (decision, false);
    }
    let entity_type = resource
        .0
        .get("cedar_entity_mapping")
        .and_then(|m| m.get("entity_type"))
        .and_then(|v| v.as_str())
        .unwrap_or("");
    let table_name = crate::mask::config::table_name_for_entity_type(entity_type);
    let salt = guc_config::mask_hash_salt_bytes();
    crate::mask::compute_and_stash_masked_row(&resource.0, &table_name, &salt);
    (false, true)
}

/// AnyElement wrapper for row-based unsigned authorization.
///
/// Materializes the row to canonical Cedar `EntityData` JSON and then reuses the JSONB
/// unsigned path (`cedarling_authorized_row`).
#[pg_extern(name = "cedarling_authorized_row")]
pub fn cedarling_authorized_row_from_anyelement(
    record: AnyElement,
    action: &str,
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

/// AnyElement wrapper for JWT/multi-issuer authorization.
///
/// Reads tokens from the fallback chain: explicit bundle (none here) → `cedarling.tokens` GUC.
#[pg_extern]
pub fn cedarling_authorized_row_jwt(record: AnyElement, action: Option<&str>) -> bool {
    let action = action.unwrap_or("Read");
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
            .map(|j: pgrx::datum::JsonB| j.0)
            .unwrap_or_else(|| json!({}));
        assert!(context_value.is_object());
        assert_eq!(context_value, json!({}));
    }

    #[test]
    fn error_variants_deny_in_fail_closed_mode() {
        let cases = vec![
            CedarlingError::RequestInvalid("empty action".into()),
            CedarlingError::JsonParsing("bad json".into()),
            CedarlingError::Engine("no engine".into()),
        ];
        for err in &cases {
            assert!(err.should_deny(), "{err:?} must deny in fail-closed mode");
        }
    }

    #[test]
    fn apply_mask_strategy_noop_when_decision_true() {
        let resource = JsonB(json!({"cedar_entity_mapping": {"entity_type": "T", "id": "x"}}));
        let (d, masked) = apply_mask_strategy(true, &resource, "ts");
        assert!(d);
        assert!(!masked);
    }

    #[test]
    fn apply_mask_strategy_noop_when_strategy_filter() {
        // strategy() returns Filter by default in tests (no pg GUC).
        let resource = JsonB(json!({"cedar_entity_mapping": {"entity_type": "T", "id": "x"}}));
        let (d, masked) = apply_mask_strategy(false, &resource, "ts");
        assert!(!d);
        assert!(!masked, "filter strategy should not mask");
    }
}
