// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-facing authorization functions: [`cedarling_authorized`] (JWT / multi-issuer)
//! and [`cedarling_authorize_unsigned`] (pre-built principal + resource entities, no JWTs).

use pgrx::prelude::*;

use crate::authz::bridge as authz_bridge;
use crate::authz::bridge::AuthorizeOutcome;
use crate::authz::cache as authz_cache;
use crate::engine;
use crate::functions::error::CedarlingError;
use crate::guc_config::{self, CedarlingFailMode, CedarlingMode};
use crate::observability::log as extension_log;
use crate::observability::status as status;
use crate::observability::trace::{push_trace, AuthorizationTrace};

/// Fields shared across every trace this module records.
struct TraceCommon<'a> {
    timestamp: String,
    action: &'a str,
    resource_type: String,
    resource_id: String,
    principal_id: Option<String>,
    shadow: bool,
}

fn record_cache_hit_trace(c: &TraceCommon<'_>, decision: bool) {
    status::record_cache_hit();
    status::record_decision(decision);
    push_trace(AuthorizationTrace {
        timestamp: c.timestamp.clone(),
        action: c.action.to_string(),
        duration_ms: 0,
        decision: Some(decision),
        error_category: None,
        request_id: String::new(),
        resource_type: c.resource_type.clone(),
        resource_id: c.resource_id.clone(),
        principal_id: c.principal_id.clone(),
        shadow: c.shadow,
        cache_hit: true,
        policy_hits: vec![],
        diag_errors: vec![],
        masked: false,
        policy_version: None,
        batch_id: None,
    });
}

fn record_success_trace(c: &TraceCommon<'_>, duration_ms: u64, outcome: AuthorizeOutcome) {
    status::record_decision(outcome.decision);
    push_trace(AuthorizationTrace {
        timestamp: c.timestamp.clone(),
        action: c.action.to_string(),
        duration_ms,
        decision: Some(outcome.decision),
        error_category: None,
        request_id: outcome.request_id,
        resource_type: c.resource_type.clone(),
        resource_id: c.resource_id.clone(),
        principal_id: c.principal_id.clone(),
        shadow: c.shadow,
        cache_hit: false,
        policy_hits: outcome.policy_hits,
        diag_errors: outcome.diag_errors,
        masked: false,
        policy_version: None,
        batch_id: None,
    });
}

fn record_error_trace(c: &TraceCommon<'_>, duration_ms: u64, ce: &CedarlingError) {
    status::record_error_msg(&ce.to_string());
    push_trace(AuthorizationTrace {
        timestamp: c.timestamp.clone(),
        action: c.action.to_string(),
        duration_ms,
        decision: None,
        error_category: Some(ce.category()),
        request_id: String::new(),
        resource_type: c.resource_type.clone(),
        resource_id: c.resource_id.clone(),
        principal_id: c.principal_id.clone(),
        shadow: c.shadow,
        cache_hit: false,
        policy_hits: vec![],
        diag_errors: vec![],
        masked: false,
        policy_version: None,
        batch_id: None,
    });
}

/// Returns whether Cedarling **allows** the request (`true` = allow, `false` = deny).
///
/// - **`token_bundle`**: pass a JSON token bundle ([`crate::token_bundle`] shapes). If `NULL` or
///   blank, [`guc_config::tokens_utf8`] (`cedarling.tokens`) is used instead.
/// - **`action`**: full Cedar action UID string (e.g. `Jans::Action::"Read"`).
///
/// **Mode:**
/// - `enforcement` (default) and `instrumentation` both return the evaluated decision.
/// - `shadow` always returns `true`; the evaluated decision is still computed and logged.
///
/// **Errors:** JWT / engine / parse failures are **not** raised as SQL errors by default: the
/// function returns `false` when [`CedarlingFailMode::Closed`] and `true` when
/// [`CedarlingFailMode::Open`] ("fail open"), per `cedarling.fail_mode`. Shadow mode always
/// returns `true` regardless of error class.
///
/// **Logging:** structured messages go to the server log at or above [`guc_config::CedarlingLogLevelGuc`]
/// (`cedarling.log_level`). JWT material is never logged.
#[pg_extern(volatile, parallel_restricted)]
#[allow(clippy::needless_pass_by_value)] // `#[pg_extern]` maps parameters from PostgreSQL calling convention
pub fn cedarling_authorized(resource_json: &str, token_bundle: Option<&str>, action: &str) -> bool {
    cedarling_authorized_inner(resource_json, token_bundle, action)
}

fn cedarling_authorized_inner(
    resource_json: &str,
    token_bundle: Option<&str>,
    action: &str,
) -> bool {
    let action_trimmed = action.trim();
    if action_trimmed.is_empty() {
        return finalize_error(&CedarlingError::RequestInvalid("empty action".into()));
    }

    let resource_trimmed = resource_json.trim();
    if resource_trimmed.is_empty() {
        return finalize_error(&CedarlingError::ResourceConstruction(
            "empty resource JSON".into(),
        ));
    }

    let Some(token_str) = resolve_token_bundle(token_bundle) else {
        return finalize_error(&CedarlingError::Configuration(
            "no token bundle (argument blank and cedarling.tokens unset)".into(),
        ));
    };

    let (resource_type, resource_id) = crate::observability::trace::extract_entity_info(resource_trimmed);
    let shadow = matches!(guc_config::mode(), CedarlingMode::Shadow);

    status::record_request();

    let context_from_guc = guc_config::context_utf8();
    let context_trimmed = context_from_guc.as_deref().unwrap_or("");
    let ttl = guc_config::cache_ttl_seconds();
    let cache_key = authz_cache::multi_issuer_key(
        authz_cache::policy_segment_from_bootstrap_path(),
        token_str.as_str(),
        resource_trimmed,
        action_trimmed,
        context_trimmed,
    );

    let mut common = TraceCommon {
        timestamp: chrono::Utc::now().to_rfc3339(),
        action: action_trimmed,
        resource_type,
        resource_id,
        principal_id: None,
        shadow,
    };

    match authz_cache::global_cache().lookup(ttl, &cache_key) {
        Ok(Some(decision)) => {
            record_cache_hit_trace(&common, decision);
            return finalize_decision(decision);
        },
        Ok(None) => {},
        Err(_) => {
            return finalize_error(&CedarlingError::Engine(
                "authorization cache mutex poisoned".into(),
            ));
        },
    }

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            status::record_error_msg(&e.to_string());
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let start = std::time::Instant::now();
    common.timestamp = chrono::Utc::now().to_rfc3339();

    match authz_bridge::authorize_multi_issuer_outcome(
        engine.as_ref(),
        token_str.as_str(),
        resource_trimmed,
        action_trimmed,
        context_from_guc.as_deref(),
    ) {
        Ok(outcome) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            authz_cache::global_cache().store(ttl, &cache_key, outcome.decision);
            let decision = outcome.decision;
            record_success_trace(&common, duration_ms, outcome);
            finalize_decision(decision)
        },
        Err(e) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            extension_log::log_multi_issuer_bridge_failure(&e);
            let ce = CedarlingError::from(e);
            record_error_trace(&common, duration_ms, &ce);
            finalize_error(&ce)
        },
    }
}

/// Same error semantics as [`cedarling_authorized`], but calls [`authorize_unsigned`](cedarling::blocking::Cedarling::authorize_unsigned).
///
/// - **`principal_json`**: `NULL` or blank → no principal (Cedar partial-evaluation where applicable).
/// - **`resource_json`**: required [`EntityData`](cedarling::EntityData) JSON.
/// - **`context_json`**: Cedar request context; must be a JSON **object** (use `"{}"` if unused).
///
/// **Logging:** same rules as [`cedarling_authorized`] (`cedarling.log_level`, no secrets).
#[pg_extern(volatile, parallel_restricted)]
#[allow(clippy::needless_pass_by_value)] // `#[pg_extern]` maps parameters from PostgreSQL calling convention
pub fn cedarling_authorize_unsigned(
    principal_json: Option<&str>,
    resource_json: &str,
    action: &str,
    context_json: &str,
) -> bool {
    cedarling_authorize_unsigned_inner(principal_json, resource_json, action, context_json)
}

fn cedarling_authorize_unsigned_inner(
    principal_json: Option<&str>,
    resource_json: &str,
    action: &str,
    context_json: &str,
) -> bool {
    let action_trimmed = action.trim();
    if action_trimmed.is_empty() {
        return finalize_error(&CedarlingError::RequestInvalid("empty action".into()));
    }

    let resource_trimmed = resource_json.trim();
    if resource_trimmed.is_empty() {
        return finalize_error(&CedarlingError::ResourceConstruction(
            "empty resource JSON".into(),
        ));
    }

    let (resource_type, resource_id) = crate::observability::trace::extract_entity_info(resource_trimmed);
    // Derive principal_id from the principal JSON if one was provided.
    let principal_id: Option<String> = principal_json
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .and_then(|s| {
            let (_, id) = crate::observability::trace::extract_entity_info(s);
            if id.is_empty() { None } else { Some(id) }
        });
    let shadow = matches!(guc_config::mode(), CedarlingMode::Shadow);

    status::record_request();

    let principal_norm = principal_json
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .unwrap_or("");
    let context_trimmed = context_json.trim();

    let ttl = guc_config::cache_ttl_seconds();
    let cache_key = authz_cache::unsigned_key(
        authz_cache::policy_segment_from_bootstrap_path(),
        principal_norm,
        resource_trimmed,
        action_trimmed,
        context_trimmed,
    );

    let mut common = TraceCommon {
        timestamp: chrono::Utc::now().to_rfc3339(),
        action: action_trimmed,
        resource_type,
        resource_id,
        principal_id,
        shadow,
    };

    match authz_cache::global_cache().lookup(ttl, &cache_key) {
        Ok(Some(decision)) => {
            record_cache_hit_trace(&common, decision);
            return finalize_decision(decision);
        },
        Ok(None) => {},
        Err(_) => {
            return finalize_error(&CedarlingError::Engine(
                "authorization cache mutex poisoned".into(),
            ));
        },
    }

    let request = match authz_bridge::unsigned_request_from_json_parts(
        principal_json,
        resource_trimmed,
        action_trimmed,
        context_json,
    ) {
        Ok(r) => r,
        Err(e) => {
            extension_log::log_unsigned_bridge_failure(&e);
            status::record_error_msg(&e.to_string());
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            status::record_error_msg(&e.to_string());
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let start = std::time::Instant::now();
    common.timestamp = chrono::Utc::now().to_rfc3339();

    match authz_bridge::authorize_unsigned_outcome_for_request(engine.as_ref(), request) {
        Ok(outcome) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            authz_cache::global_cache().store(ttl, &cache_key, outcome.decision);
            let decision = outcome.decision;
            record_success_trace(&common, duration_ms, outcome);
            finalize_decision(decision)
        },
        Err(e) => {
            let duration_ms = crate::observability::trace::duration_millis(start.elapsed());
            extension_log::log_unsigned_bridge_failure(&e);
            let ce = CedarlingError::from(e);
            record_error_trace(&common, duration_ms, &ce);
            finalize_error(&ce)
        },
    }
}

// ── Batch authorize ─────────────────────────────────────────────────
// Both flows record one `record_request()` per SQL call, one
// `record_decision()` per item on success, one `record_error_msg()` on
// batch failure, and one per-item trace either way — matching the
// single-item observability contract. Row contract on failure: see each
// pg_extern's docstring.

fn finalize_batch_decision(decision: bool) -> bool {
    // Shadow flattens every decision to `true`; the raw decision is recorded on
    // status counters and traces upstream so operators can still see the raw call.
    if matches!(guc_config::mode(), CedarlingMode::Shadow) {
        return true;
    }
    decision
}

/// Length of the top-level `items` array from `request_json`, without
/// deserializing the whole request. `None` on unparseable / missing / non-array.
fn peek_batch_item_count(request_json: &str) -> Option<usize> {
    let value: serde_json::Value = serde_json::from_str(request_json).ok()?;
    value.get("items")?.as_array().map(Vec::len)
}

/// Fail-closed / mode-respecting rows on batch-level failure: N rows when the
/// item count is peekable, otherwise one sentinel row at `item_index = -1`.
/// Also pushes one per-row error trace so operators see item-level detail.
fn batch_failure_rows(
    request_json: &str,
    err: &CedarlingError,
) -> Vec<(i32, bool, Option<String>, String)> {
    let decision = finalize_error(err);
    let shadow = matches!(guc_config::mode(), CedarlingMode::Shadow);
    let timestamp = chrono::Utc::now().to_rfc3339();
    let category = err.category();
    let error_col = Some(category.to_string());
    match peek_batch_item_count(request_json) {
        Some(n) if n > 0 => (0..n)
            .map(|idx| {
                push_batch_error_trace(&timestamp, shadow, category, None, idx as i32);
                (
                    i32::try_from(idx).unwrap_or(i32::MAX),
                    decision,
                    error_col.clone(),
                    String::new(),
                )
            })
            .collect(),
        _ => {
            push_batch_error_trace(&timestamp, shadow, category, None, -1);
            vec![(-1, decision, error_col, String::new())]
        },
    }
}

fn push_batch_error_trace(
    timestamp: &str,
    shadow: bool,
    error_category: &'static str,
    batch_id: Option<String>,
    item_index: i32,
) {
    push_trace(AuthorizationTrace {
        timestamp: timestamp.to_string(),
        action: format!("cedarling.batch.item[{item_index}]"),
        duration_ms: 0,
        decision: None,
        error_category: Some(error_category),
        request_id: String::new(),
        resource_type: String::new(),
        resource_id: String::new(),
        principal_id: None,
        shadow,
        cache_hit: false,
        policy_hits: vec![],
        diag_errors: vec![],
        masked: false,
        policy_version: None,
        batch_id,
    });
}

/// Evaluate a batch of unsigned requests. Returns one row per item, in order,
/// each carrying the shared `batch_id` for audit correlation.
///
/// **Row contract on failure:** obeys `cedarling.mode` / `cedarling.fail_mode`
/// like [`cedarling_authorized`]. If the item count can be peeked from the
/// request JSON, yields N rows with decision from [`finalize_error`] and empty
/// `batch_id`; otherwise a single sentinel row at `item_index = -1` — so
/// `bool_and(decision)` never collapses to `NULL` on a batch-level failure.
#[pg_extern(volatile, parallel_restricted)]
#[allow(clippy::needless_pass_by_value)]
pub fn cedarling_authorize_unsigned_batch(
    request_json: &str,
) -> TableIterator<
    'static,
    (
        name!(item_index, i32),
        name!(decision, bool),
        name!(error_category, Option<String>),
        name!(batch_id, String),
    ),
> {
    status::record_request();
    let start = std::time::Instant::now();
    let rows = match execute_unsigned_batch(request_json) {
        Ok(outcome) => success_rows(outcome, start.elapsed()),
        Err(ce) => batch_failure_rows(request_json, &ce),
    };
    TableIterator::new(rows)
}

/// Evaluate a batch of multi-issuer requests. Same shape and failure contract
/// as [`cedarling_authorize_unsigned_batch`].
#[pg_extern(volatile, parallel_restricted)]
#[allow(clippy::needless_pass_by_value)]
pub fn cedarling_authorize_multi_issuer_batch(
    request_json: &str,
) -> TableIterator<
    'static,
    (
        name!(item_index, i32),
        name!(decision, bool),
        name!(error_category, Option<String>),
        name!(batch_id, String),
    ),
> {
    status::record_request();
    let start = std::time::Instant::now();
    let rows = match execute_multi_issuer_batch(request_json) {
        Ok(outcome) => success_rows(outcome, start.elapsed()),
        Err(ce) => batch_failure_rows(request_json, &ce),
    };
    TableIterator::new(rows)
}

/// Build the per-item response rows and emit `record_decision` + `push_trace`
/// once per item. Ok slots use the raw Cedar decision (shadow flip only
/// touches the returned SQL row); Err slots synthesize a fail-closed decision
/// via [`finalize_error`], populate `error_category`, and emit an error trace.
fn success_rows(
    outcome: authz_bridge::BatchOutcome,
    total_elapsed: std::time::Duration,
) -> Vec<(i32, bool, Option<String>, String)> {
    let batch_id = outcome.batch_id;
    let batch_id_trace = Some(batch_id.clone());
    let shadow = matches!(guc_config::mode(), CedarlingMode::Shadow);
    let timestamp = chrono::Utc::now().to_rfc3339();
    let total_ms = crate::observability::trace::duration_millis(total_elapsed);
    outcome
        .items
        .into_iter()
        .enumerate()
        .map(|(idx, item)| {
            let idx_i32 = i32::try_from(idx).unwrap_or(i32::MAX);
            match item.kind {
                authz_bridge::BatchItemKind::Ok(ok) => {
                    let raw_decision = ok.decision;
                    status::record_decision(raw_decision);
                    push_trace(AuthorizationTrace {
                        timestamp: timestamp.clone(),
                        action: item.action,
                        duration_ms: total_ms,
                        decision: Some(raw_decision),
                        error_category: None,
                        request_id: ok.request_id,
                        resource_type: item.resource_type,
                        resource_id: item.resource_id,
                        principal_id: None,
                        shadow,
                        cache_hit: false,
                        policy_hits: ok.policy_hits,
                        diag_errors: ok.diag_errors,
                        masked: false,
                        policy_version: None,
                        batch_id: batch_id_trace.clone(),
                    });
                    (
                        idx_i32,
                        finalize_batch_decision(raw_decision),
                        None,
                        batch_id.clone(),
                    )
                },
                authz_bridge::BatchItemKind::Err(e) => {
                    // Item never reached Cedar. Fail-closed decision via
                    // `finalize_error` preserves the single-item contract;
                    // `error_category` surfaces the reason to consumers.
                    status::record_error_msg(&e.message);
                    push_trace(AuthorizationTrace {
                        timestamp: timestamp.clone(),
                        action: item.action,
                        duration_ms: total_ms,
                        decision: None,
                        error_category: Some(e.category),
                        request_id: String::new(),
                        resource_type: item.resource_type,
                        resource_id: item.resource_id,
                        principal_id: None,
                        shadow,
                        cache_hit: false,
                        policy_hits: vec![],
                        diag_errors: vec![e.message],
                        masked: false,
                        policy_version: None,
                        batch_id: batch_id_trace.clone(),
                    });
                    (
                        idx_i32,
                        finalize_error(&CedarlingError::RequestInvalid(e.category.to_string())),
                        Some(e.category.to_string()),
                        batch_id.clone(),
                    )
                },
            }
        })
        .collect()
}

fn execute_unsigned_batch(
    request_json: &str,
) -> Result<authz_bridge::BatchOutcome, CedarlingError> {
    let engine = engine::global_cedarling().map_err(|e| {
        extension_log::log_engine_failure(&e);
        status::record_error_msg(&e.to_string());
        CedarlingError::from(e)
    })?;
    authz_bridge::authorize_unsigned_batch_outcome(engine.as_ref(), request_json).map_err(|e| {
        let msg = e.to_string();
        extension_log::log_batch_bridge_failure(&msg);
        status::record_error_msg(&msg);
        CedarlingError::from(e)
    })
}

fn execute_multi_issuer_batch(
    request_json: &str,
) -> Result<authz_bridge::BatchOutcome, CedarlingError> {
    let engine = engine::global_cedarling().map_err(|e| {
        extension_log::log_engine_failure(&e);
        status::record_error_msg(&e.to_string());
        CedarlingError::from(e)
    })?;
    authz_bridge::authorize_multi_issuer_batch_outcome(engine.as_ref(), request_json).map_err(
        |e| {
            let msg = e.to_string();
            extension_log::log_batch_bridge_failure(&msg);
            status::record_error_msg(&msg);
            CedarlingError::from(e)
        },
    )
}

fn resolve_token_bundle(arg: Option<&str>) -> Option<String> {
    match arg {
        None => guc_config::tokens_utf8(),
        Some(s) if s.trim().is_empty() => guc_config::tokens_utf8(),
        Some(s) => Some(s.to_string()),
    }
}

/// Apply `cedarling.mode` to a cleanly evaluated decision.
///
/// In `shadow` mode we always return `true`; the raw decision has already been logged upstream
/// for trace purposes.
pub(crate) fn finalize_decision(decision: bool) -> bool {
    match guc_config::mode() {
        CedarlingMode::Shadow => true,
        CedarlingMode::Enforcement | CedarlingMode::Instrumentation => decision,
    }
}

/// Apply `cedarling.mode` + `cedarling.fail_mode` to an error path. Emits an audit entry when
/// fail-open takes effect and `cedarling.audit_fail_open` is true.
pub(crate) fn finalize_error(err: &CedarlingError) -> bool {
    if matches!(guc_config::mode(), CedarlingMode::Shadow) {
        return true;
    }
    match guc_config::fail_mode() {
        CedarlingFailMode::Closed => false,
        CedarlingFailMode::Open => {
            if guc_config::audit_fail_open() {
                extension_log::log_audit_fail_open(err);
            }
            true
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolve_token_prefers_argument() {
        assert_eq!(
            resolve_token_bundle(Some(r#"{"A":"b"}"#)),
            Some(r#"{"A":"b"}"#.to_string())
        );
    }
}
