// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::{DateTime, Utc};
use serde_json::{json, Value};
use thiserror::Error;
use uuid::Uuid;

use crate::authz_bridge::{AuthorizeBridgeError, UnsignedBridgeError};
use crate::engine::EngineError;
use crate::guc_config::CedarlingLogLevelGuc;
use crate::resource::ResourceEntityDataError;
use crate::token_bundle::TokenBundleError;

/// One error type for the whole extension.
//
// Some variants are unused until their producers (policy versioning,
// schema validation, cache introspection, direct SPI surfaces). They're part of the
// public error API now so downstream `From` impls and match arms don't churn.
#[allow(dead_code)]
#[derive(Debug, Error)]
pub enum CedarlingError {
    /// A JWT / token bundle failed structural or semantic validation (format, `exp`, `nbf`,
    /// issuer, audience, cross-token consistency). Distinct from [`Self::TokenBundle`] which
    /// only covers JSON shape.
    #[error("token validation failed: {0}")]
    TokenValidation(String),

    /// The token bundle JSON could not be parsed into the expected `[{{mapping, payload}}]` or
    /// `{{mapping: jwt}}` shape.
    #[error("token bundle shape invalid: {0}")]
    TokenBundle(String),

    /// A resource / principal `EntityData` JSON could not be built (missing
    /// `cedar_entity_mapping`, bad JSON, non-object root, etc.).
    #[error("resource construction failed: {0}")]
    ResourceConstruction(String),

    /// The Cedar [`AuthorizeMultiIssuerRequest`](cedarling::AuthorizeMultiIssuerRequest)
    /// / [`RequestUnsigned`](cedarling::RequestUnsigned) failed its own pre-evaluation
    /// validation step (bad action UID, context not an object, etc.).
    #[error("request invalid: {0}")]
    RequestInvalid(String),

    /// Cedar policy evaluation itself returned an error (not a `Deny` — a hard failure).
    #[error("policy evaluation failed: {0}")]
    PolicyEvaluation(String),

    /// Loading or swapping the policy set failed.
    #[error("policy loading failed: {0}")]
    PolicyLoading(String),

    /// A Cedar schema validation check failed.
    #[error("schema validation failed: {0}")]
    SchemaValidation(String),

    /// The process-wide Cedarling engine could not be initialized or has a cached init failure.
    #[error("engine unavailable: {0}")]
    Engine(String),

    /// GUC or other extension configuration is invalid or missing.
    #[error("configuration invalid: {0}")]
    Configuration(String),

    /// The authorization-decision cache misbehaved (lock poisoned, etc.). Recoverable.
    #[error("cache error: {0}")]
    Cache(String),

    /// A JSON parse failed outside of a more specific context.
    #[error("JSON parse error: {0}")]
    JsonParsing(String),

    /// An SPI call or catalog query failed.
    #[error("database error: {0}")]
    Database(String),

    /// Cedar returned a clean `Deny`. This is the one variant that does **not** indicate a bug
    /// — it's the expected outcome when policy rules do not permit the request.
    #[error("authorization denied")]
    AuthorizationDenied,
}

/// Short, stable categorization string for a [`CedarlingError`]. Intended for metrics and
/// audit-log grouping — format should be treated as a stable API.
pub type ErrorCategory = &'static str;

/// Record emitted to the server log when a [`CedarlingError`] is surfaced from the authorize
/// path. Serializes to JSON via [`AuditLogEntry::to_json`].
#[derive(Debug, Clone)]
pub struct AuditLogEntry {
    pub error_id: Uuid,
    pub category: ErrorCategory,
    pub timestamp: DateTime<Utc>,
    pub detail: String,
    /// `fail_closed` or `fail_open`, depending on the effective `cedarling.fail_mode` at the
    /// time the error was surfaced. Filled in by the authorize path, not by this module.
    pub fail_mode_applied: &'static str,
}

impl AuditLogEntry {
    #[must_use]
    pub fn new(err: &CedarlingError, fail_mode_applied: &'static str) -> Self {
        Self {
            error_id: Uuid::new_v4(),
            category: err.category(),
            timestamp: Utc::now(),
            detail: err.to_string(),
            fail_mode_applied,
        }
    }

    #[must_use]
    pub fn to_json(&self) -> Value {
        json!({
            "error_id": self.error_id.to_string(),
            "category": self.category,
            "timestamp": self.timestamp.to_rfc3339(),
            "detail": self.detail,
            "fail_mode": self.fail_mode_applied,
        })
    }
}

impl CedarlingError {
    /// Whether this error should result in a `Deny` decision when in fail-closed mode.
    ///
    /// Every variant returns `true`; kept as a method so future variants must opt out
    /// explicitly (the fail-safe invariant).
    #[must_use]
    pub const fn should_deny(&self) -> bool {
        match self {
            Self::TokenValidation(_)
            | Self::TokenBundle(_)
            | Self::ResourceConstruction(_)
            | Self::RequestInvalid(_)
            | Self::PolicyEvaluation(_)
            | Self::PolicyLoading(_)
            | Self::SchemaValidation(_)
            | Self::Engine(_)
            | Self::Configuration(_)
            | Self::Cache(_)
            | Self::JsonParsing(_)
            | Self::Database(_)
            | Self::AuthorizationDenied => true,
        }
    }

    /// Stable, short category string for metrics/audit grouping.
    #[must_use]
    pub const fn category(&self) -> ErrorCategory {
        match self {
            Self::TokenValidation(_) => "token_validation",
            Self::TokenBundle(_) => "token_bundle",
            Self::ResourceConstruction(_) => "resource_construction",
            Self::RequestInvalid(_) => "request_invalid",
            Self::PolicyEvaluation(_) => "policy_evaluation",
            Self::PolicyLoading(_) => "policy_loading",
            Self::SchemaValidation(_) => "schema_validation",
            Self::Engine(_) => "engine",
            Self::Configuration(_) => "configuration",
            Self::Cache(_) => "cache",
            Self::JsonParsing(_) => "json_parsing",
            Self::Database(_) => "database",
            Self::AuthorizationDenied => "authorization_denied",
        }
    }

    /// Recommended minimum log level for this error. Follows [`CedarlingLogLevelGuc`].
    #[must_use]
    pub const fn log_level(&self) -> CedarlingLogLevelGuc {
        match self {
            // Operator-visible subsystem failures → WARN: worth attention but not a crash.
            Self::Engine(_)
            | Self::PolicyEvaluation(_)
            | Self::PolicyLoading(_)
            | Self::SchemaValidation(_)
            | Self::Cache(_)
            | Self::Database(_) => CedarlingLogLevelGuc::Warn,
            // Caller-shape problems → INFO: expected in the wild, not a system health signal.
            Self::TokenValidation(_)
            | Self::TokenBundle(_)
            | Self::ResourceConstruction(_)
            | Self::RequestInvalid(_)
            | Self::JsonParsing(_)
            | Self::Configuration(_)
            | Self::AuthorizationDenied => CedarlingLogLevelGuc::Info,
        }
    }

    /// Build an [`AuditLogEntry`] for this error.
    #[must_use]
    pub fn to_audit_entry(&self, fail_mode_applied: &'static str) -> AuditLogEntry {
        AuditLogEntry::new(self, fail_mode_applied)
    }
}

// ---- From impls ------------------------------------------------------------
//
// Module-specific errors keep their own types for unit-testability, but the authorize path
// sees only [`CedarlingError`]. Each From impl redacts detail that could carry secrets.

impl From<TokenBundleError> for CedarlingError {
    fn from(value: TokenBundleError) -> Self {
        // `TokenBundleError` display strings describe *shape* (array vs object), not payload
        // content, so they are safe to include verbatim.
        Self::TokenBundle(value.to_string())
    }
}

impl From<ResourceEntityDataError> for CedarlingError {
    fn from(value: ResourceEntityDataError) -> Self {
        Self::ResourceConstruction(value.to_string())
    }
}

impl From<EngineError> for CedarlingError {
    fn from(value: EngineError) -> Self {
        match value {
            EngineError::BootstrapPathNotSet => {
                Self::Configuration("cedarling.bootstrap_config is not set".into())
            },
            EngineError::BootstrapLoad(e) => Self::Engine(format!("bootstrap load: {e}")),
            EngineError::CedarlingInit(e) => Self::Engine(format!("init: {e}")),
            EngineError::InitPreviouslyFailed(msg) => {
                Self::Engine(format!("previous init failure cached: {msg}"))
            },
            EngineError::MutexPoisoned => Self::Engine("engine mutex poisoned".into()),
        }
    }
}

impl From<AuthorizeBridgeError> for CedarlingError {
    fn from(value: AuthorizeBridgeError) -> Self {
        match value {
            AuthorizeBridgeError::TokenBundle(e) => e.into(),
            AuthorizeBridgeError::Resource(e) => e.into(),
            AuthorizeBridgeError::RequestInvalid(msg) => Self::RequestInvalid(msg),
            AuthorizeBridgeError::Authorize(e) => classify_cedar_authorize_error(&e),
        }
    }
}

impl From<UnsignedBridgeError> for CedarlingError {
    fn from(value: UnsignedBridgeError) -> Self {
        match value {
            UnsignedBridgeError::Principal(e) => {
                Self::ResourceConstruction(format!("principal: {e}"))
            },
            UnsignedBridgeError::Resource(e) => e.into(),
            UnsignedBridgeError::ContextParse(e) => Self::JsonParsing(format!("context: {e}")),
            UnsignedBridgeError::ContextNotObject => {
                Self::RequestInvalid("context must be a JSON object".into())
            },
            UnsignedBridgeError::Authorize(e) => classify_cedar_authorize_error(&e),
        }
    }
}

impl From<serde_json::Error> for CedarlingError {
    fn from(value: serde_json::Error) -> Self {
        Self::JsonParsing(value.to_string())
    }
}

fn classify_cedar_authorize_error(err: &cedarling::AuthorizeError) -> CedarlingError {
    use cedarling::AuthorizeError;
    match err {
        // Token-processing failures are validation problems, even though Cedarling surfaces
        // them via `AuthorizeError`.
        AuthorizeError::ProcessTokens(_) => {
            CedarlingError::TokenValidation("JWT/token processing failed (details redacted)".into())
        },
        AuthorizeError::Action(_) | AuthorizeError::IdentifierParsing(_) => {
            CedarlingError::RequestInvalid("invalid Cedar action or identifier".into())
        },
        AuthorizeError::CreateContext(_)
        | AuthorizeError::RequestValidation(_)
        | AuthorizeError::InvalidPrincipal(_)
        | AuthorizeError::ValidateEntities(_)
        | AuthorizeError::EntitiesToJson(_)
        | AuthorizeError::BuildContext(_)
        | AuthorizeError::BuildEntity(_)
        | AuthorizeError::BuildUnsignedRoleEntity(_)
        | AuthorizeError::MultiIssuerValidation(_)
        | AuthorizeError::MultiIssuerEntity(_) => CedarlingError::PolicyEvaluation(
            "request or entity build failed during policy evaluation".into(),
        ),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_variant_denies_in_fail_closed() {
        // The fail-safe invariant: every variant must opt in to denial.
        for err in sample_errors() {
            assert!(
                err.should_deny(),
                "variant {err:?} must deny in fail-closed mode"
            );
        }
    }

    #[test]
    fn categories_are_stable_kebab_or_snake() {
        for err in sample_errors() {
            let cat = err.category();
            assert!(!cat.is_empty(), "category for {err:?} must not be empty");
            assert!(
                cat.chars()
                    .all(|c| c.is_ascii_lowercase() || c == '_' || c.is_ascii_digit()),
                "category {cat:?} for {err:?} must be snake_case ASCII"
            );
        }
    }

    #[test]
    fn log_levels_follow_expectation() {
        assert_eq!(
            CedarlingError::TokenValidation("x".into()).log_level(),
            CedarlingLogLevelGuc::Info
        );
        assert_eq!(
            CedarlingError::Engine("x".into()).log_level(),
            CedarlingLogLevelGuc::Warn
        );
        assert_eq!(
            CedarlingError::PolicyEvaluation("x".into()).log_level(),
            CedarlingLogLevelGuc::Warn
        );
        assert_eq!(
            CedarlingError::AuthorizationDenied.log_level(),
            CedarlingLogLevelGuc::Info
        );
    }

    #[test]
    fn audit_entry_is_json_safe() {
        let err = CedarlingError::TokenValidation("exp < now".into());
        let entry = err.to_audit_entry("fail_closed");
        let value = entry.to_json();
        assert_eq!(value["category"], "token_validation");
        assert_eq!(value["fail_mode"], "fail_closed");
        assert!(value["error_id"].is_string());
        assert!(value["timestamp"].is_string());
        assert_eq!(value["detail"], "token validation failed: exp < now");
    }

    #[test]
    fn from_token_bundle_error_redacts_to_shape_message() {
        let e: CedarlingError = TokenBundleError::Empty.into();
        assert!(matches!(e, CedarlingError::TokenBundle(_)));
    }

    #[test]
    fn from_resource_error_maps_to_resource_construction() {
        let e: CedarlingError = ResourceEntityDataError::Empty.into();
        assert!(matches!(e, CedarlingError::ResourceConstruction(_)));
    }

    #[test]
    fn from_engine_bootstrap_not_set_maps_to_configuration() {
        let e: CedarlingError = EngineError::BootstrapPathNotSet.into();
        assert!(matches!(e, CedarlingError::Configuration(_)));
    }

    fn sample_errors() -> Vec<CedarlingError> {
        vec![
            CedarlingError::TokenValidation("t".into()),
            CedarlingError::TokenBundle("t".into()),
            CedarlingError::ResourceConstruction("r".into()),
            CedarlingError::RequestInvalid("r".into()),
            CedarlingError::PolicyEvaluation("p".into()),
            CedarlingError::PolicyLoading("p".into()),
            CedarlingError::SchemaValidation("s".into()),
            CedarlingError::Engine("e".into()),
            CedarlingError::Configuration("c".into()),
            CedarlingError::Cache("c".into()),
            CedarlingError::JsonParsing("j".into()),
            CedarlingError::Database("d".into()),
            CedarlingError::AuthorizationDenied,
        ]
    }
}
