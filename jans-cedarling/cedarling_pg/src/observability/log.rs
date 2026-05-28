// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` server logging for `cedarling_pg`, driven by [`crate::guc_config::CedarlingLogLevelGuc`].
//!
//! Messages intentionally avoid raw JWTs and token payloads: only static / structural diagnostics.

use pgrx::prelude::*;

use crate::authz::bridge::{MultiIssuerBridgeError, UnsignedBridgeError};
use crate::engine::EngineError;
use crate::functions::error::CedarlingError;
use crate::guc_config::{log_level, CedarlingLogLevelGuc};

#[inline]
const fn level_rank(level: CedarlingLogLevelGuc) -> u8 {
    match level {
        CedarlingLogLevelGuc::Debug => 0,
        CedarlingLogLevelGuc::Info => 1,
        CedarlingLogLevelGuc::Warn => 2,
        CedarlingLogLevelGuc::Error => 3,
    }
}

/// `true` when a message at `at` should be emitted for the current `cedarling.log_level` (minimum
/// severity: `debug` is most verbose, `error` is least).
#[must_use]
pub(crate) fn should_emit(at: CedarlingLogLevelGuc) -> bool {
    level_rank(at) >= level_rank(log_level())
}

/// Emit a diagnostic at `at` when enabled. Uses `WARNING` at most so authorization still returns a
/// boolean without raising `SQLSTATE` errors.
pub(crate) fn log_diagnostic(at: CedarlingLogLevelGuc, msg: &str) {
    if !should_emit(at) {
        return;
    }
    match at {
        CedarlingLogLevelGuc::Debug => debug1!("cedarling_pg: {}", msg),
        CedarlingLogLevelGuc::Info => log!("cedarling_pg: {}", msg),
        CedarlingLogLevelGuc::Warn | CedarlingLogLevelGuc::Error => {
            warning!("cedarling_pg: {}", msg);
        },
    }
}

pub(crate) fn log_engine_failure(err: &EngineError) {
    match err {
        EngineError::BootstrapPathNotSet => {
            log_diagnostic(
                CedarlingLogLevelGuc::Info,
                "Cedarling engine unavailable: cedarling.bootstrap_config is unset",
            );
        },
        EngineError::BootstrapLoad(e) => {
            if should_emit(CedarlingLogLevelGuc::Warn) {
                warning!("cedarling_pg: Cedarling bootstrap config load error: {e}");
            }
        },
        EngineError::CedarlingInit(e) => {
            if should_emit(CedarlingLogLevelGuc::Warn) {
                warning!("cedarling_pg: Cedarling initialization error: {e}");
            }
        },
        EngineError::MutexPoisoned => {
            log_diagnostic(
                CedarlingLogLevelGuc::Warn,
                "Cedarling engine internal error: engine mutex poisoned",
            );
        },
    }
}

pub(crate) fn log_multi_issuer_bridge_failure(err: &MultiIssuerBridgeError) {
    let (at, msg) = match err {
        MultiIssuerBridgeError::TokenBundle(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorized: token bundle JSON invalid or unsupported shape",
        ),
        MultiIssuerBridgeError::Resource(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorized: resource JSON invalid for EntityData",
        ),
        MultiIssuerBridgeError::RequestInvalid(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorized: request validation failed before Cedar evaluation",
        ),
        MultiIssuerBridgeError::Authorize(e) => classify_authorize_error(e),
    };
    log_diagnostic(at, msg);
}

pub(crate) fn log_unsigned_bridge_failure(err: &UnsignedBridgeError) {
    let (at, msg) = match err {
        UnsignedBridgeError::Principal(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorize_unsigned: principal entity JSON invalid",
        ),
        UnsignedBridgeError::Resource(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorize_unsigned: resource JSON invalid for EntityData",
        ),
        UnsignedBridgeError::ContextParse(_) => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorize_unsigned: context JSON parse error",
        ),
        UnsignedBridgeError::ContextNotObject => (
            CedarlingLogLevelGuc::Info,
            "cedarling_authorize_unsigned: context must be a JSON object",
        ),
        UnsignedBridgeError::Authorize(e) => classify_authorize_error(e),
    };
    log_diagnostic(at, msg);
}

/// Emit a structured audit entry at `WARN` when fail-open converts an error-induced deny into
/// an allow. Respects [`should_emit`] so it can be suppressed by `cedarling.log_level = error`.
pub(crate) fn log_audit_fail_open(err: &CedarlingError) {
    if !should_emit(CedarlingLogLevelGuc::Warn) {
        return;
    }
    let entry = err.to_audit_entry("fail_open");
    // Serialize to compact JSON so log collectors can parse it directly.
    warning!("cedarling_pg: audit {}", entry.to_json());
}

fn classify_authorize_error(
    err: &cedarling::AuthorizeError,
) -> (CedarlingLogLevelGuc, &'static str) {
    use cedarling::AuthorizeError;
    match err {
        AuthorizeError::ProcessTokens(_) => (
            CedarlingLogLevelGuc::Warn,
            "Cedarling authorize: JWT/token processing failed (details omitted from log)",
        ),
        AuthorizeError::Action(_) | AuthorizeError::IdentifierParsing(_) => (
            CedarlingLogLevelGuc::Info,
            "Cedarling authorize: invalid Cedar action or identifier",
        ),
        AuthorizeError::CreateContext(_)
        | AuthorizeError::RequestValidation(_)
        | AuthorizeError::InvalidPrincipal(_)
        | AuthorizeError::ValidateEntities(_)
        | AuthorizeError::EntitiesToJson(_)
        | AuthorizeError::BuildContext(_)
        | AuthorizeError::BuildEntity(_)
        | AuthorizeError::BuildUnsignedRoleEntity(_)
        | AuthorizeError::MultiIssuerValidation(_)
        | AuthorizeError::MultiIssuerEntity(_) => (
            CedarlingLogLevelGuc::Warn,
            "Cedarling authorize: request or entity build failed (see Cedarling docs for error kinds)",
        ),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn level_rank_orders_debug_most_verbose() {
        assert!(level_rank(CedarlingLogLevelGuc::Debug) < level_rank(CedarlingLogLevelGuc::Info));
        assert!(level_rank(CedarlingLogLevelGuc::Info) < level_rank(CedarlingLogLevelGuc::Warn));
        assert!(level_rank(CedarlingLogLevelGuc::Warn) < level_rank(CedarlingLogLevelGuc::Error));
    }
}
