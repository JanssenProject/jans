// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jsonwebtoken::Algorithm;
use std::collections::HashSet;

/// The set of Bootstrap properties related to JWT validation.
#[derive(Debug, PartialEq)]
pub struct JwtConfig {
    /// A Json Web Key Store (JWKS) with public keys.
    ///
    /// If this is used, Cedarling will no longer try to fetch JWK Stores from
    /// a trustede identity provider and stick to using the local JWKS.
    pub jwks: Option<String>,
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
    /// Only tokens signed with algorithms in this list can be valid.
    pub signature_algorithms_supported: HashSet<Algorithm>,
}

/// Validation options related to JSON Web Tokens (JWT).
///
/// This struct provides the configuration for validating common JWT claims (`iss`,
/// `aud`, `sub`, `jti`, `exp`, `nbf`) across different types of JWTs.
///
/// The default configuration for Access Tokens, ID Tokens, and Userinfo Tokens
/// can be easily instantiated via the provided methods.
#[derive(Debug, Default, PartialEq)]
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
    /// Requires the `iat` claim to be present in the JWT.
    pub iat_validation: bool,
    /// Requires the `exp` claim to be present in the JWT and the current
    /// timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
    /// Requires the `nbf` claim to be present in the JWT.
    pub nbf_validation: bool,
}

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
        if self.iat_validation {
            req_claims.insert("iat".into());
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
    /// - `iss` (Issuer)
    /// - `jti` (JWT ID)
    /// - `exp` (Expiration)
    pub fn access_token() -> Self {
        Self {
            iss_validation: true,
            jti_validation: true,
            exp_validation: true,
            nbf_validation: false,
            aud_validation: false,
            sub_validation: false,
            iat_validation: false,
        }
    }

    /// Returns a default configuration for validating ID Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (Issuer)
    /// - `aud` (Audience)
    /// - `sub` (Subject)
    /// - `exp` (Expiration)
    pub fn id_token() -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            exp_validation: true,
            iat_validation: false,
            jti_validation: false,
            nbf_validation: false,
        }
    }

    /// Returns a default configuration for validating Userinfo Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer)
    /// - `aud` (audience)
    /// - `sub` (subject)
    /// - `exp` (expiration)
    pub fn userinfo_token() -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            exp_validation: true,
            jti_validation: false,
            iat_validation: false,
            nbf_validation: false,
        }
    }
}

impl Default for JwtConfig {
    /// Cedarling will use the strictest validation options by default.
    fn default() -> Self {
        Self {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: true,
            signature_algorithms_supported: HashSet::new(),
        }
    }
}

impl JwtConfig {
    /// Creates a new `JwtConfig` instance with validation turned off for all tokens.
    pub fn new_without_validation() -> Self {
        Self {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
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
