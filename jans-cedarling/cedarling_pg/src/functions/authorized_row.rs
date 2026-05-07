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
use crate::guc_config;
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
        return finalize_decision(decision);
    }

    status::record_request();

    let request = match authz_bridge::unsigned_request_from_json_parts(
        None,
        &resource_str,
        action_trimmed,
        &context_str,
    ) {
        Ok(r) => r,
        Err(e) => {
            extension_log::log_unsigned_bridge_failure(&e);
            status::record_error();
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            status::record_error();
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let start = std::time::Instant::now();
    let timestamp = chrono::Utc::now().to_rfc3339();

    match authz_bridge::authorize_unsigned_decision_for_request(engine.as_ref(), request) {
        Ok(decision) => {
            let duration_ms = start.elapsed().as_millis() as u64;
            authz_cache::global_cache().store(ttl, cache_key, decision);
            status::record_decision(decision);
            push_trace(AuthorizationTrace {
                timestamp,
                action: action_trimmed.to_string(),
                duration_ms,
                decision: Some(decision),
                error_category: None,
            });
            finalize_decision(decision)
        },
        Err(e) => {
            let duration_ms = start.elapsed().as_millis() as u64;
            extension_log::log_unsigned_bridge_failure(&e);
            let ce = CedarlingError::from(e);
            status::record_error();
            push_trace(AuthorizationTrace {
                timestamp,
                action: action_trimmed.to_string(),
                duration_ms,
                decision: None,
                error_category: Some(ce.category()),
            });
            finalize_error(&ce)
        },
    }
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
}
