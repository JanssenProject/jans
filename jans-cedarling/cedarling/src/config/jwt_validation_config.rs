// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use jsonwebtoken::Algorithm;
use jsonwebtoken::jwk::JwkSet;
use serde::{Deserialize, Serialize};

/// Config specific to JWT validation behavior
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct JwtValidationConfig {
    /// JWKS file with public keys
    #[serde(alias = "CEDARLING_LOCAL_JWKS", default)]
    pub local_jwks: Option<JwkSet>,

    /// Toggles validatingto the signature JWTs.
    ///
    /// This requires an iss is present.
    #[serde(
        rename = "jwt_sig_validation",
        alias = "CEDARLING_JWT_SIG_VALIDATION",
        default
    )]
    pub sig_validation: FeatureToggle,

    /// Toggles checking the status of the JWT on startup.
    ///
    /// The latest Status List JWT will be retrieved from the
    /// `.well-known/openid-configuration` via the `status_list_endpoint` claim and
    /// cache it. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    #[serde(
        rename = "jwt_status_validation",
        alias = "CEDARLING_JWT_STATUS_VALIDATION",
        default
    )]
    pub status_validation: FeatureToggle,

    /// Cedarling will only accept tokens signed with these algorithms.
    #[serde(
        rename = "jwt_signature_algorithms_supported",
        alias = "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED",
        default
    )]
    pub signature_algorithms_supported: Vec<Algorithm>,

    /// Varying levels of validations based on the preference of the developer.
    ///
    /// # Strict Mode
    ///
    /// Strict mode requires:
    ///     1. id_token aud matches the access_token client_id;
    ///     2. if a Userinfo token is present, the sub matches the id_token, and that
    ///         the aud matches the access token client_id.
    #[serde(alias = "CEDARLING_ID_TOKEN_TRUST_MODE", default)]
    pub id_token_trust_mode: IdTokenTrustMode,
}

impl Default for JwtValidationConfig {
    fn default() -> Self {
        Self {
            local_jwks: None,
            sig_validation: FeatureToggle::Enabled,
            status_validation: FeatureToggle::Disabled,
            signature_algorithms_supported: vec![
                Algorithm::RS256,
                Algorithm::RS384,
                Algorithm::RS512,
                Algorithm::ES256,
                Algorithm::ES384,
                Algorithm::EdDSA,
            ],
            id_token_trust_mode: IdTokenTrustMode::None,
        }
    }
}

/// Defines the level of validation for ID tokens.
#[derive(Debug, Default, Deserialize, Serialize, PartialEq, Clone, Copy)]
#[serde(rename_all = "lowercase")]
pub enum IdTokenTrustMode {
    /// No validation is performed on the ID token.
    #[default]
    None,

    /// Strict validation of the ID token.
    ///
    /// In this mode, the following conditions must be met:
    ///
    /// - The `id_token`'s `aud` (audience) must match the `access_token`'s `client_id`.
    /// - If a Userinfo token is present:
    ///   - Its `sub` (subject) must match the `id_token`'s `sub`.
    ///   - Its `aud` must match the `access_token`'s `client_id`.
    Strict,
}
