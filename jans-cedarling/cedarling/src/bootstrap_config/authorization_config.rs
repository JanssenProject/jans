// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::json_rules::JsonRule;

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Configuration to specify authorization workflow.
/// - If we use user entity as principal.
/// - If we use workload entity as principal.
/// - What boolean operator we need to use when both is used
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AuthorizationConfig {
    /// When `enabled`, Cedar engine authorization is queried for a User principal.
    /// bootstrap property: `CEDARLING_USER_AUTHZ`
    pub use_user_principal: bool,

    /// When `enabled`, Cedar engine authorization is queried for a Workload principal.
    /// bootstrap property: `CEDARLING_WORKLOAD_AUTHZ`
    pub use_workload_principal: bool,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD`  when
    /// making authz (authorization) decisions.
    pub principal_bool_operator: JsonRule,

    /// List of claims to map from user entity, such as ["sub", "email", "username", ...]
    /// `CEDARLING_DECISION_LOG_USER_CLAIMS` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub decision_log_user_claims: Vec<String>,

    /// List of claims to map from user entity, such as ["client_id", "rp_id", ...]
    /// `CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub decision_log_workload_claims: Vec<String>,

    /// Token claims that will be used for decision logging.
    /// Default is jti, but perhaps some other claim is needed.
    /// `CEDARLING_DECISION_LOG_DEFAULT_JWT_ID` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub decision_log_default_jwt_id: String,

    /// Sets the validation level for ID tokens.
    ///
    /// The available levels are [`Always`], [`Never`], [`IfPresent`], and [`Strict`].
    ///
    /// # Strict Mode
    ///
    /// In `Strict` mode, the following conditions must be met for a token
    /// to be considered valid:
    ///
    /// - The `id_token`'s `aud` (audience) must match the `access_token`'s `client_id`
    /// - If a Userinfo token is present:
    ///     - Its `sub` (subject) must match the `id_token`'s `sub`.
    ///     - Its `aud` (audience) must match the `access_token`'s `client_id`.
    ///
    /// [`Always`]: IdTokenTrustMode::Always
    /// [`Never`]: IdTokenTrustMode::Never
    /// [`IfPresent`]: IdTokenTrustMode::IfPresent
    /// [`Strict`]: IdTokenTrustMode::Strict
    pub id_token_trust_mode: IdTokenTrustMode,
}

/// Raw authorization config
pub struct AuthorizationConfigRaw {
    /// Use user principal
    pub use_user_principal: bool,
    /// Use workload principal
    pub use_workload_principal: bool,
    /// Principal bool operator
    pub principal_bool_operator: JsonRule,
    /// Decision log default JWT ID
    pub decision_log_default_jwt_id: String,
    /// Decision log user claims
    pub decision_log_user_claims: Vec<String>,
    /// Decision log workload claims
    pub decision_log_workload_claims: Vec<String>,
}

/// ID token trust mode
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum IdTokenTrustMode {
    /// Always
    Always,
    /// Never
    Never,
    /// If present
    IfPresent,
    /// Strict
    Strict,
}

impl Default for IdTokenTrustMode {
    fn default() -> Self {
        Self::Strict
    }
}

impl Default for AuthorizationConfig {
    fn default() -> Self {
        Self {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::default(),
            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: Vec::new(),
            decision_log_workload_claims: Vec::new(),
            id_token_trust_mode: IdTokenTrustMode::Strict,
        }
    }
}

impl From<AuthorizationConfigRaw> for AuthorizationConfig {
    fn from(raw: AuthorizationConfigRaw) -> Self {
        Self {
            use_user_principal: raw.use_user_principal,
            use_workload_principal: raw.use_workload_principal,
            principal_bool_operator: raw.principal_bool_operator,
            decision_log_default_jwt_id: raw.decision_log_default_jwt_id,
            decision_log_user_claims: raw.decision_log_user_claims,
            decision_log_workload_claims: raw.decision_log_workload_claims,
            id_token_trust_mode: IdTokenTrustMode::Strict,
        }
    }
}

/// Token entity names
pub struct TokenEntityNames(pub HashMap<String, String>);

impl Default for TokenEntityNames {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), "Jans::Access_token".to_string()),
            ("id_token".to_string(), "Jans::Id_token".to_string()),
            (
                "userinfo_token".to_string(),
                "Jans::Userinfo_token".to_string(),
            ),
        ]))
    }
}

impl From<HashMap<String, String>> for TokenEntityNames {
    fn from(value: HashMap<String, String>) -> Self {
        Self(value)
    }
}

impl From<TokenEntityNames> for HashMap<String, String> {
    fn from(value: TokenEntityNames) -> Self {
        value.0
    }
}

/// Error when parsing [`IdTokenTrustMode`]
#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `IdTokenTrustMode`: {trust_mode}. should be `strict` or `never`")]
pub struct IdTknTrustModeParseError {
    trust_mode: String,
}
