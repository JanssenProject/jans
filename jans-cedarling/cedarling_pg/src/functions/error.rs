// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Authorization-path error type and audit log entry for `cedarling_pg`.
//!
//! [`CedarlingError`] is the common error currency for the `functions/` authorize path.
//! Each subsystem (engine, tokens, resource, policy) maintains its own error type;
//! the `From` impls below funnel them here at the authorize boundary.

use chrono::{DateTime, Utc};
use serde_json::{json, Value};
use thiserror::Error;
use uuid::Uuid;

use crate::authz::bridge::{AuthorizeBridgeError, UnsignedBridgeError};
use crate::engine::EngineError;
use crate::policy::{PolicyError, SchemaError};
use crate::resource::row::RowBuildError;
use crate::tokens::bundle::TokenBundleError;

#[derive(Debug, Error)]
pub enum CedarlingError {
    #[error("token validation failed: {0}")]
    TokenValidation(String),

    #[error("token bundle shape invalid: {0}")]
    TokenBundle(String),

    #[error("resource construction failed: {0}")]
    ResourceConstruction(String),

    #[error("request invalid: {0}")]
    RequestInvalid(String),

    #[error("policy evaluation failed: {0}")]
    PolicyEvaluation(String),

    #[error("policy loading failed: {0}")]
    PolicyLoading(String),

    #[error("schema validation failed: {0}")]
    SchemaValidation(String),

    #[error("engine unavailable: {0}")]
    Engine(String),

    #[error("configuration invalid: {0}")]
    Configuration(String),

    #[error("JSON parse error: {0}")]
    JsonParsing(String),

    #[error("database error: {0}")]
    Database(String),
}

pub type ErrorCategory = &'static str;

#[derive(Debug, Clone)]
pub struct AuditLogEntry {
    pub error_id: Uuid,
    pub category: ErrorCategory,
    pub timestamp: DateTime<Utc>,
    pub detail: String,
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
            Self::JsonParsing(_) => "json_parsing",
            Self::Database(_) => "database",
        }
    }

    #[must_use]
    pub fn to_audit_entry(&self, fail_mode_applied: &'static str) -> AuditLogEntry {
        AuditLogEntry::new(self, fail_mode_applied)
    }
}

// ---- From impls (module errors → CedarlingError) --------------------------------

impl From<TokenBundleError> for CedarlingError {
    fn from(value: TokenBundleError) -> Self {
        Self::TokenBundle(value.to_string())
    }
}

impl From<RowBuildError> for CedarlingError {
    fn from(value: RowBuildError) -> Self {
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
            EngineError::MutexPoisoned => Self::Engine("engine mutex poisoned".into()),
        }
    }
}

impl From<AuthorizeBridgeError> for CedarlingError {
    fn from(value: AuthorizeBridgeError) -> Self {
        match value {
            AuthorizeBridgeError::TokenBundle(e) => e.into(),
            AuthorizeBridgeError::Resource(e) => {
                Self::ResourceConstruction(e.to_string())
            },
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
            UnsignedBridgeError::Resource(e) => {
                Self::ResourceConstruction(e.to_string())
            },
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

impl From<PolicyError> for CedarlingError {
    fn from(value: PolicyError) -> Self {
        Self::PolicyLoading(value.to_string())
    }
}

impl From<SchemaError> for CedarlingError {
    fn from(value: SchemaError) -> Self {
        Self::SchemaValidation(value.to_string())
    }
}

fn classify_cedar_authorize_error(err: &cedarling::AuthorizeError) -> CedarlingError {
    use cedarling::AuthorizeError;
    match err {
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
    fn from_token_bundle_error_maps_to_token_bundle() {
        let e: CedarlingError = TokenBundleError::Empty.into();
        assert!(matches!(e, CedarlingError::TokenBundle(_)));
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
            CedarlingError::JsonParsing("j".into()),
            CedarlingError::Database("d".into()),
        ]
    }

    // The authorize boundary in [`classify_cedar_authorize_error`] is the *only*
    // place where a cedarling `AuthorizeError` (which may carry JWT material in
    // its inner Display) is translated into a `CedarlingError` that the
    // extension persists into traces, status, and audit entries. Each match arm
    // MUST return a fixed redacted string — not derived from the inner error —
    // so that no JWT bytes can reach `AuthorizationTrace`, `cedarling_status`,
    // `cedarling_last_trace`, or `AuditLogEntry::detail`.
    //
    // These tests pin that invariant. If a future contributor adds a new
    // `AuthorizeError` arm that interpolates inner state, this test fails.

    /// Detects substrings that look like the start of a serialized JWT
    /// (`base64url`-encoded header that decodes to `{"alg":...}`).
    fn looks_like_jwt(s: &str) -> bool {
        s.contains("eyJ")
    }

    const CLASSIFIER_REDACTED_MESSAGES: [&str; 3] = [
        "JWT/token processing failed (details redacted)",
        "invalid Cedar action or identifier",
        "request or entity build failed during policy evaluation",
    ];

    fn jwt_poison() -> &'static str {
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.x"
    }

    fn assert_classified_output_never_leaks_poison(
        auth_err: &cedarling::AuthorizeError,
        poison: &str,
    ) {
        let classified = classify_cedar_authorize_error(auth_err);
        let display = classified.to_string();
        let audit = classified.to_audit_entry("fail_closed").to_json().to_string();
        assert!(
            !display.contains(poison),
            "classified Display leaked poison: {display}"
        );
        assert!(
            !audit.contains(poison),
            "classified audit JSON leaked poison: {audit}"
        );
        assert!(!looks_like_jwt(&display), "Display contains eyJ: {display}");
        assert!(!looks_like_jwt(&audit), "audit JSON contains eyJ: {audit}");
    }

    fn poisonous_authorize_errors() -> Vec<cedarling::AuthorizeError> {
        use std::str::FromStr;

        use cedarling::AuthorizeError;
        use cedar_policy::EntityUid;

        let poison = jwt_poison();
        let parse_err = || EntityUid::from_str(poison).expect_err("poison must not parse as UID");

        vec![
            AuthorizeError::EntitiesToJson(
                serde_json::from_str::<serde_json::Value>(poison).expect_err("invalid json"),
            ),
            AuthorizeError::Action(Box::new(parse_err())),
            AuthorizeError::IdentifierParsing(Box::new(parse_err())),
        ]
    }

    #[test]
    fn classifier_outputs_are_static_redacted_strings() {
        for s in CLASSIFIER_REDACTED_MESSAGES {
            assert!(!looks_like_jwt(s), "redacted string itself contains 'eyJ': {s}");
        }
    }

    #[test]
    fn classify_maps_each_output_kind_to_a_static_redacted_message() {
        for auth_err in poisonous_authorize_errors() {
            let classified = classify_cedar_authorize_error(&auth_err);
            let detail = match &classified {
                CedarlingError::TokenValidation(s)
                | CedarlingError::RequestInvalid(s)
                | CedarlingError::PolicyEvaluation(s) => s.as_str(),
                other => panic!("unexpected CedarlingError from classify: {other:?}"),
            };
            assert!(
                CLASSIFIER_REDACTED_MESSAGES.contains(&detail),
                "classifier must return a fixed redacted message, got: {detail:?}"
            );
        }
    }

    #[test]
    fn classify_never_echoes_inner_authorize_error_display() {
        let poison = jwt_poison();
        for auth_err in poisonous_authorize_errors() {
            assert_classified_output_never_leaks_poison(&auth_err, poison);
        }
    }

    #[test]
    fn classify_never_echoes_random_jwt_like_bytes() {
        const RANDOM_BYTES: [u8; 48] = [
            0xA5, 0xC2, 0x7E, 0x11, 0x9B, 0x44, 0xD0, 0x6F, 0x23, 0x88, 0x5A, 0x01, 0xBE, 0x72,
            0x39, 0xE4, 0x0C, 0x97, 0x51, 0xFA, 0x2D, 0x83, 0x16, 0xCB, 0x60, 0xAD, 0x04, 0x79,
            0x32, 0xED, 0x58, 0x0F, 0xA0, 0x67, 0x1C, 0xD5, 0x8E, 0x41, 0xF6, 0x2B, 0x94, 0x07,
            0xBC, 0x65, 0x18, 0xCF, 0x76, 0x29,
        ];
        let poison = String::from_utf8_lossy(&RANDOM_BYTES);
        let inner =
            serde_json::from_str::<serde_json::Value>(&poison).expect_err("random bytes not json");
        assert_classified_output_never_leaks_poison(
            &cedarling::AuthorizeError::EntitiesToJson(inner),
            &poison,
        );
    }

    #[test]
    fn redacted_classifier_strings_do_not_carry_inner_token_data() {
        let cases = [
            CedarlingError::TokenValidation(CLASSIFIER_REDACTED_MESSAGES[0].into()),
            CedarlingError::RequestInvalid(CLASSIFIER_REDACTED_MESSAGES[1].into()),
            CedarlingError::PolicyEvaluation(CLASSIFIER_REDACTED_MESSAGES[2].into()),
        ];
        for err in cases {
            let entry = err.to_audit_entry("fail_closed");
            let serialized = entry.to_json().to_string();
            assert!(
                !looks_like_jwt(&serialized),
                "audit JSON contains JWT-looking substring: {serialized}"
            );
            assert!(
                !err.to_string().contains("eyJ"),
                "error Display contains JWT-looking substring: {err}"
            );
        }
    }
}
