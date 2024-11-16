/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::common::policy_store::TrustedIssuer;
use std::collections::HashMap;
type IssuerId = String;

/// The set of Bootstrap properties related to JWT validation.
#[allow(dead_code)]
pub struct NewJwtConfig {
    /// Local Json Web Key Store (JWKS) with public keys.
    pub local_jwks: Option<String>,
    /// An optional mapping of trusted issuers containing OpenID configuration or
    /// Identity Provider (IDP) information.
    ///
    /// Each entry in the map associates an `IssuerId` with a `TrustedIssuer` instance,
    /// representing metadata for the corresponding issuer. This metadata may include
    /// information retrieved from a `.well-known/openid-configuration` endpoint 
    /// for validating tokens and establishing trust with the issuer.
    pub trusted_issuers: Option<HashMap<IssuerId, TrustedIssuer>>,
    /// Check the signature for all the Json Web Tokens.
    ///
    /// This Requires the `iss` claim to be present in all the tokens and
    /// and the scheme must be `https`.
    ///
    /// This setting overrides the `iss` validation settings in the following:
    ///
    /// - [`AccessTokenValidationConfig`]
    /// - [`IdTokenValidationConfig`]
    /// - [`UserinfoTokenValidationConfig`]
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
    ///
    /// Set this to a [`Vec`] containing `'*'` to allow all algorithms.
    pub signature_algorithms_supported: Vec<String>,
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
    /// Returns a default configuration for validating Access Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer) validation
    /// - `jti` (JWT ID) validation
    /// - `exp` (expiration) validation
    ///
    /// Claims like `aud` (audience) and `sub` (subject) are not required for
    /// Access Tokens.
    fn access_token() -> Self {
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
    fn id_token() -> Self {
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
    fn userinfo_token() -> Self {
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

/// Validation options related to the Access token
pub struct AccessTokenValidationConfig {
    /// Requires the `iss` claim to be present in the Access token
    /// and the scheme must be `https`
    pub iss_validation: bool,
    /// Requires the `jti` claim to be present in the Access token
    pub jti_validation: bool,
    /// Requires the `exp` claim to be present in the Access token and
    /// the current timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
    /// Requires the `nbf` claim to be present in the Access token
    pub nbf_validation: bool,
}

/// Validation options related to the Id token
pub struct IdTokenValidationConfig {
    /// Requires the `iss` claim to be present in the Id token
    /// and the scheme must be `https`
    pub iss_validation: bool,
    /// Requires the `sub` claim to be present in the Access token
    pub sub_validation: bool,
    /// Requires the `iat` claim to be present in the Id token
    pub iat_validation: bool,
    /// Requires the `aud` claim to be present in the Id token
    pub aud_validation: bool,
    /// Requires the `exp` claim to be present in the Id token and
    /// the current timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
}

/// Validation options related to the Userinfo token
pub struct UserinfoTokenValidationConfig {
    /// Requires the `iss` claim to be present in the Userinfo token
    /// and the scheme must be `https`
    pub iss_validation: bool,
    /// Requires the `sub` claim to be present in the userinfo token
    pub sub_validation: bool,
    /// Requires the `aud` claim to be present in the userinfo token
    pub aud_validation: bool,
    /// Requires the `exp` claim to be present in the Userinfo token and
    /// the current timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
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
            local_jwks: None,
            trusted_issuers: None,
            jwt_sig_validation: true,
            jwt_status_validation: true,
            id_token_trust_mode: IdTokenTrustMode::Strict,
            signature_algorithms_supported: Vec::new(),
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
            local_jwks: None,
            trusted_issuers: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            id_token_trust_mode: IdTokenTrustMode::None,
            signature_algorithms_supported: vec!["*".into()],
            access_token_config: TokenValidationConfig::default(),
            id_token_config: TokenValidationConfig::default(),
            userinfo_token_config: TokenValidationConfig::default(),
        }
    }

    /// Allows all signature algorithms by setting the `signature_algorithms_supported`
    /// to `vec!["*"]`.
    ///
    /// This method clears the current list of supported algorithms and sets it to
    /// allow all algorithms.
    pub fn allow_all_algorithms(mut self) -> Self {
        self.signature_algorithms_supported.clear();
        self.signature_algorithms_supported.push("*".into());
        self
    }

    /// Sets the local JSON Web Key Set (JWKS) to be used for JWT signature validation.
    ///
    /// The JWKS is a collection of public keys used to validate the signature of
    /// incoming JWTs. This method sets the `local_jwks` field to a specific JWKS
    /// in the form of a JSON string.
    pub fn set_local_jwks(mut self, jwks: String) -> Self {
        self.local_jwks = Some(jwks);
        self
    }

    /// Sets the trusted issuers to be used for JWT signature validation.
    pub fn set_trusted_issuers(
        mut self,
        trusted_issuers: HashMap<IssuerId, TrustedIssuer>,
    ) -> Self {
        self.trusted_issuers = Some(trusted_issuers);
        self
    }
}

/// A set of properties used to configure JWT in the `Cedarling` application.
#[derive(Debug, Clone)]
pub enum JwtConfig {
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Disabled` value.  
    /// Meaning no JWT validation and no controls if Cedarling will discard id_token without an access token with the corresponding client_id.
    Disabled,
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Enabled` value
    Enabled {
        /// `CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
        signature_algorithms: Vec<String>,
    },
}
