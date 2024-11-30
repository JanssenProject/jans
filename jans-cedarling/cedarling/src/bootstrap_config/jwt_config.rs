/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::Algorithm;
use std::collections::HashSet;

/// The set of Bootstrap properties related to JWT validation.
#[allow(dead_code)]
pub struct NewJwtConfig {
    /// Check the signature for all the Json Web Tokens.
    ///
    /// This Requires the `iss` claim to be present in all the tokens and
    /// and the scheme must be `https`.
    ///
    /// This setting overrides the `iss` validation settings in the following:
    ///
    /// - `access_token_config`
    /// - `id_token_config`
    /// - `userinfo_token_config`
    pub jwt_sig_validation: bool,
    /// Whether to check the status of the JWT.
    ///
    /// On startup, the Cedarling will fetch and retreive the latest Status List
    /// JWT from the `.well-known/openid-configuration` via the `status_list_endpoint`
    /// claim and cache it.
    ///
    /// See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    pub jwt_status_validation: bool,
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
    /// Only tokens signed with algorithms in this list can be valid.
    pub signature_algorithms_supported: HashSet<Algorithm>,
    /// Validation options related to the Access token
    pub access_token_config: TokenValidationConfig,
    /// Validation options related to the Id token
    pub id_token_config: TokenValidationConfig,
    /// Validation options related to the Userinfo token
    pub userinfo_token_config: TokenValidationConfig,
}

/// Validation options related to JSON Web Tokens (JWT).
///
/// This struct provides the configuration for validating common JWT claims (`iss`,
/// `aud`, `sub`, `jti`, `exp`, `nbf`) across different types of JWTs.
///
/// The default configuration for Access Tokens, ID Tokens, and Userinfo Tokens
/// can be easily instantiated via the provided methods.
#[derive(Default)]
pub struct TokenValidationConfig {
    /// Requires the `iss` claim to be present in the JWT and the scheme
    /// must be `https`.
    pub iss_validation: bool,
    /// Requires the `aud` claim to be present in the JWT.
    pub aud_validation: bool,
    /// Requires the `sub` claim to be present in the JWT.
    pub sub_validation: bool,
    /// Requires the `jti` claim to be present in the JWT.
    pub jti_validation: bool,
    /// Requires the `exp` claim to be present in the JWT and the current
    /// timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
    /// Requires the `nbf` claim to be present in the JWT.
    pub nbf_validation: bool,
}

#[allow(dead_code)]
impl TokenValidationConfig {
    /// Collects all the required claims into a HashSet.
    pub fn required_claims(&self) -> HashSet<Box<str>> {
        let mut req_claims = HashSet::new();
        if self.iss_validation {
            req_claims.insert("iss".into());
        }
        if self.aud_validation {
            req_claims.insert("aud".into());
        }
        if self.sub_validation {
            req_claims.insert("sub".into());
        }
        if self.jti_validation {
            req_claims.insert("jti".into());
        }
        if self.exp_validation {
            req_claims.insert("exp".into());
        }
        if self.nbf_validation {
            req_claims.insert("nbf".into());
        }
        req_claims
    }

    /// Returns a default configuration for validating Access Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer) validation
    /// - `jti` (JWT ID) validation
    /// - `exp` (expiration) validation
    ///
    /// Claims like `aud` (audience) and `sub` (subject) are not required for
    /// Access Tokens.
    pub fn access_token() -> Self {
        Self {
            iss_validation: true,
            aud_validation: false,
            sub_validation: false,
            jti_validation: true,
            exp_validation: true,
            nbf_validation: false,
        }
    }

    /// Returns a default configuration for validating ID Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer) validation
    /// - `aud` (audience) validation
    /// - `sub` (subject) validation
    /// - `exp` (expiration) validation
    ///
    /// `jti` (JWT ID) and `nbf` (not before) are not required for ID Tokens.
    pub fn id_token() -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            jti_validation: false,
            exp_validation: true,
            nbf_validation: false,
        }
    }

    /// Returns a default configuration for validating Userinfo Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer) validation
    /// - `aud` (audience) validation
    /// - `sub` (subject) validation
    /// - `exp` (expiration) validation
    ///
    /// `jti` (JWT ID) and `nbf` (not before) are not required for Userinfo Tokens.
    pub fn userinfo_token() -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            jti_validation: false,
            exp_validation: true,
            nbf_validation: false,
        }
    }

    /// Enables the validation of the `iss` claim (issuer).
    fn validate_iss(mut self) -> Self {
        self.iss_validation = true;
        self
    }

    /// Enables the validation of the `aud` claim (audience).
    fn validate_aud(mut self) -> Self {
        self.aud_validation = true;
        self
    }

    /// Enables the validation of the `sub` claim (subject).
    fn validate_sub(mut self) -> Self {
        self.sub_validation = true;
        self
    }

    /// Enables the validation of the `jti` claim (JWT ID).
    fn validate_jti(mut self) -> Self {
        self.jti_validation = true;
        self
    }

    /// Enables the validation of the `exp` claim (expiration).
    fn validate_exp(mut self) -> Self {
        self.exp_validation = true;
        self
    }

    /// Enables the validation of the `nbf` claim (not before).
    fn validate_nbf(mut self) -> Self {
        self.nbf_validation = true;
        self
    }
}

/// Defines the level of validation for ID tokens.
#[derive(Debug, Clone, PartialEq, Default)]
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

impl Default for NewJwtConfig {
    /// Cedarling will use the strictest validation options by default.
    fn default() -> Self {
        Self {
            jwt_sig_validation: true,
            jwt_status_validation: true,
            id_token_trust_mode: IdTokenTrustMode::Strict,
            signature_algorithms_supported: HashSet::new(),
            access_token_config: TokenValidationConfig::access_token(),
            id_token_config: TokenValidationConfig::id_token(),
            userinfo_token_config: TokenValidationConfig::userinfo_token(),
        }
    }
}

#[allow(dead_code)]
impl NewJwtConfig {
    /// Creates a new `JwtConfig` instance with validation turned off for all tokens.
    pub fn new_without_validation() -> Self {
        Self {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            id_token_trust_mode: IdTokenTrustMode::None,
            signature_algorithms_supported: HashSet::new(),
            access_token_config: TokenValidationConfig::default(),
            id_token_config: TokenValidationConfig::default(),
            userinfo_token_config: TokenValidationConfig::default(),
        }
        .allow_all_algorithms()
    }

    /// Adds all supported algorithms to to `signature_algorithms_supported`.
    pub fn allow_all_algorithms(mut self) -> Self {
        self.signature_algorithms_supported = HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::HS384,
            Algorithm::HS512,
            Algorithm::ES256,
            Algorithm::ES384,
            Algorithm::RS256,
            Algorithm::RS384,
            Algorithm::RS512,
            Algorithm::PS256,
            Algorithm::PS384,
            Algorithm::PS512,
            Algorithm::EdDSA,
        ]);
        self
    }
}

/// A set of properties used to configure JWT in the `Cedarling` application.
#[derive(Debug, Clone, PartialEq)]
pub enum JwtConfig {
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Disabled` value.  
    /// Meaning no JWT validation and no controls if Cedarling will discard id_token without an access token with the corresponding client_id.
    Disabled,
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Enabled` value
    Enabled {
        /// `CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
        signature_algorithms: HashSet<Algorithm>,
    },
}
