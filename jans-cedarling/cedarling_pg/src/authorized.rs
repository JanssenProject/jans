// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-facing authorization for `RLS` and similar: [`cedarling_authorized`] (JWT / multi-issuer)
//! and [`cedarling_authorize_unsigned`] (pre-built principal + resource entities, no JWTs).

use pgrx::prelude::*;

use crate::authz_bridge;
use crate::authz_cache;
use crate::engine;
use crate::error::CedarlingError;
use crate::extension_log;
use crate::guc_config::{self, CedarlingFailMode, CedarlingMode};

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
/// [`CedarlingFailMode::Open`] (“fail open”), per `cedarling.fail_mode`. Shadow mode always
/// returns `true` regardless of error class.
///
/// **Logging:** structured messages go to the server log at or above [`guc_config::CedarlingLogLevelGuc`]
/// (`cedarling.log_level`). JWT material is never logged.
#[pg_extern]
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

    let ttl = guc_config::cache_ttl_seconds();
    let cache_key = authz_cache::multi_issuer_key(
        authz_cache::policy_segment_from_bootstrap_path(),
        token_str.as_str(),
        resource_trimmed,
        action_trimmed,
    );
    if let Some(decision) = authz_cache::global_cache().lookup(ttl, &cache_key) {
        return finalize_decision(decision);
    }

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            return finalize_error(&CedarlingError::from(e));
        },
    };

    match authz_bridge::authorize_multi_issuer_decision(
        engine.as_ref(),
        token_str.as_str(),
        resource_trimmed,
        action_trimmed,
    ) {
        Ok(decision) => {
            authz_cache::global_cache().store(ttl, cache_key, decision);
            finalize_decision(decision)
        },
        Err(e) => {
            extension_log::log_multi_issuer_bridge_failure(&e);
            finalize_error(&CedarlingError::from(e))
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
#[pg_extern]
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
    if let Some(decision) = authz_cache::global_cache().lookup(ttl, &cache_key) {
        return finalize_decision(decision);
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
            return finalize_error(&CedarlingError::from(e));
        },
    };

    let engine = match engine::global_cedarling() {
        Ok(e) => e,
        Err(e) => {
            extension_log::log_engine_failure(&e);
            return finalize_error(&CedarlingError::from(e));
        },
    };

    match authz_bridge::authorize_unsigned_decision_for_request(engine.as_ref(), request) {
        Ok(decision) => {
            authz_cache::global_cache().store(ttl, cache_key, decision);
            finalize_decision(decision)
        },
        Err(e) => {
            extension_log::log_unsigned_bridge_failure(&e);
            finalize_error(&CedarlingError::from(e))
        },
    }
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
fn finalize_decision(decision: bool) -> bool {
    match guc_config::mode() {
        CedarlingMode::Shadow => true,
        CedarlingMode::Enforcement | CedarlingMode::Instrumentation => decision,
    }
}

/// Apply `cedarling.mode` + `cedarling.fail_mode` to an error path. Emits an audit entry when
/// fail-open takes effect and `cedarling.audit_fail_open` is true.
fn finalize_error(err: &CedarlingError) -> bool {
    if matches!(guc_config::mode(), CedarlingMode::Shadow) {
        // Shadow mode: never let an error change observable behavior. The error was already
        // logged by the caller; don't raise further noise.
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
