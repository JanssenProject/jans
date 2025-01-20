// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::WorkloadBoolOp;
use serde::Deserialize;

/// Configuration to specify authorization workflow.
/// - If we use user entity as principal.
/// - If we use workload entity as principal.
/// - What boolean operator we need to use when both is used
#[derive(Debug, Clone, Default, PartialEq)]
pub struct AuthorizationConfig {
    /// When `enabled`, Cedar engine authorization is queried for a User principal.
    /// bootstrap property: `CEDARLING_USER_AUTHZ`
    pub use_user_principal: bool,

    /// When `enabled`, Cedar engine authorization is queried for a Workload principal.
    /// bootstrap property: `CEDARLING_WORKLOAD_AUTHZ`
    pub use_workload_principal: bool,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD` when
    /// making authz (authorization) decisions.
    ///
    /// # Available Operations
    /// - **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
    /// - **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.
    pub user_workload_operator: WorkloadBoolOp,

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

    /// Name of Cedar Context schema entity
    pub mapping_user: Option<String>,

    /// Name of Cedar Workload schema entity
    pub mapping_workload: Option<String>,

    /// Name of Cedar id_token schema entity
    pub mapping_id_token: Option<String>,

    /// Name of Cedar access_token schema entity
    pub mapping_access_token: Option<String>,

    /// Name of Cedar userinfo schema entity
    pub mapping_userinfo_token: Option<String>,

    /// Sets the validation level for ID tokens.
    ///
    /// The available levels are [`None`] and [`Strict`].
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
    /// [`None`]: IdTokenTrustMode::None
    /// [`Strict`]: IdTokenTrustMode::Strict
    pub id_token_trust_mode: IdTokenTrustMode,
}

/// Defines the level of validation for ID tokens.
#[derive(Debug, Clone, PartialEq, Default, Deserialize, Copy)]
#[serde(rename_all = "lowercase")]
pub enum IdTokenTrustMode {
    /// No validation is performed on the ID token.
    None,
    /// Strict validation of the ID token.
    ///
    /// In this mode, the following conditions must be met:
    ///
    /// - The `id_token`'s `aud` (audience) must match the `access_token`'s `client_id`.
    /// - If a Userinfo token is present:
    ///   - Its `sub` (subject) must match the `id_token`'s `sub`.
    ///   - Its `aud` must match the `access_token`'s `client_id`.
    #[default]
    Strict,
}

/// Error when parsing [`IdTokenTrustMode`]
#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `IdTokenTrustMode`: {trust_mode}. should be `strict` or `none`")]
pub struct IdTknTrustModeParseError {
    trust_mode: String,
}
