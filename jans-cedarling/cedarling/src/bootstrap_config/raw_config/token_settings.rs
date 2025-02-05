// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::super::TokenValidationConfig;
use super::super::authorization_config::TokenEntityNames;
use super::FeatureToggle;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Configuration for token-based entities, mapping token names to their
/// respective settings.
///
/// Each entry in this map associates a token identifier (String) with its
/// corresponding `TokenEntityConfig` settings. These settings define the
/// entity type and claim validation rules
#[derive(Debug, Deserialize, PartialEq, Clone, Serialize)]
pub struct TokenConfigs(HashMap<String, TokenConfig>);

/// Detailed configuration for a single token-based entity.
///
/// This struct defines the entity type name and the validation settings for
/// various claims associated with the token. Each claim validation setting is
/// represented as a `FeatureToggle`.
#[derive(Debug, Deserialize, PartialEq, Clone, Serialize)]
pub struct TokenConfig {
    /// The name of the Cedar entity type associated with this token.
    entity_type_name: String,
    #[serde(default, flatten)]
    claims: ClaimsValidationConfig,
}

#[derive(Debug, Deserialize, PartialEq, Clone, Default, Serialize)]
#[serde(default)]
pub struct ClaimsValidationConfig {
    /// Toggle for validating the `iss` (issuer) claim.
    iss: FeatureToggle,
    /// Toggle for validating the `sub` (subject) claim.
    sub: FeatureToggle,
    /// Toggle for validating the `aud` (audience) claim.
    aud: FeatureToggle,
    /// Toggle for validating the `exp` (expiration) claim.
    exp: FeatureToggle,
    /// Toggle for validating the `nbf` (not before) claim.
    nbf: FeatureToggle,
    /// Toggle for validating the `iat` (issued at) claim.
    iat: FeatureToggle,
    /// Toggle for validating the `jti` (JWT ID) claim.
    jti: FeatureToggle,
}

impl TokenConfigs {
    pub fn without_validation() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), TokenConfig {
                entity_type_name: "Access_token".to_string(),
                claims: ClaimsValidationConfig::default(),
            }),
            ("id_token".to_string(), TokenConfig {
                entity_type_name: "id_token".to_string(),
                claims: ClaimsValidationConfig::default(),
            }),
            ("userinfo_token".to_string(), TokenConfig {
                entity_type_name: "Userinfo_token".to_string(),
                claims: ClaimsValidationConfig::default(),
            }),
        ]))
    }
}

impl Default for TokenConfigs {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), TokenConfig {
                entity_type_name: "Access_token".to_string(),
                claims: ClaimsValidationConfig {
                    iss: FeatureToggle::Enabled,
                    sub: FeatureToggle::Disabled,
                    aud: FeatureToggle::Disabled,
                    exp: FeatureToggle::Enabled,
                    nbf: FeatureToggle::Disabled,
                    iat: FeatureToggle::Disabled,
                    jti: FeatureToggle::Enabled,
                },
            }),
            ("id_token".to_string(), TokenConfig {
                entity_type_name: "id_token".to_string(),
                claims: ClaimsValidationConfig {
                    iss: FeatureToggle::Enabled,
                    sub: FeatureToggle::Enabled,
                    aud: FeatureToggle::Enabled,
                    exp: FeatureToggle::Enabled,
                    nbf: FeatureToggle::Disabled,
                    iat: FeatureToggle::Disabled,
                    jti: FeatureToggle::Disabled,
                },
            }),
            ("userinfo_token".to_string(), TokenConfig {
                entity_type_name: "Userinfo_token".to_string(),
                claims: ClaimsValidationConfig {
                    iss: FeatureToggle::Enabled,
                    sub: FeatureToggle::Enabled,
                    aud: FeatureToggle::Enabled,
                    exp: FeatureToggle::Enabled,
                    nbf: FeatureToggle::Disabled,
                    iat: FeatureToggle::Disabled,
                    jti: FeatureToggle::Disabled,
                },
            }),
        ]))
    }
}

impl From<TokenConfig> for TokenValidationConfig {
    fn from(setting: TokenConfig) -> Self {
        Self {
            iss_validation: setting.claims.iss.into(),
            aud_validation: setting.claims.aud.into(),
            sub_validation: setting.claims.sub.into(),
            jti_validation: setting.claims.jti.into(),
            iat_validation: setting.claims.iat.into(),
            exp_validation: setting.claims.exp.into(),
            nbf_validation: setting.claims.nbf.into(),
        }
    }
}

impl From<TokenConfigs> for HashMap<String, TokenConfig> {
    fn from(value: TokenConfigs) -> Self {
        HashMap::from_iter(value.0)
    }
}

impl From<TokenConfigs> for TokenEntityNames {
    fn from(value: TokenConfigs) -> Self {
        Self(HashMap::from_iter(value.0.into_iter().map(
            |(tkn_name, config)| (tkn_name, config.entity_type_name),
        )))
    }
}

impl From<TokenConfigs> for HashMap<String, TokenValidationConfig> {
    fn from(value: TokenConfigs) -> Self {
        HashMap::from_iter(
            value
                .0
                .into_iter()
                .map(|(tkn_name, config)| (tkn_name, config.into())),
        )
    }
}
