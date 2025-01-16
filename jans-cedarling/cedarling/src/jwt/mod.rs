// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # `JwtEngine`
//!
//! The `JwtEngine` is designed for managing JSON Web Tokens (JWTs) and provides the following functionalities:
//! - Fetching decoding keys from a JSON Web Key Set (JWKS) provided by Identity Providers (IDPs) and storing these keys.
//! - Extracting claims from JWTs for further processing and validation.
//! - Validating the signatures of JWTs to ensure their integrity and authenticity.
//! - Verifying the validity of JWTs based on claims such as expiration time and audience.

mod issuers_store;
mod jwk_store;
mod key_service;
#[cfg(test)]
mod test_utils;
mod token;
mod validator;

use std::collections::{HashMap, HashSet};
use std::sync::Arc;

pub use jsonwebtoken::Algorithm;
use key_service::{KeyService, KeyServiceError};
pub use token::{Token, TokenClaimTypeError, TokenClaims, TokenStr};
use validator::{JwtValidator, JwtValidatorConfig, JwtValidatorError};

use crate::common::policy_store::TrustedIssuer;
use crate::{IdTokenTrustMode, JwtConfig};

/// Type alias for Trusted Issuers' ID.
type TrustedIssuerId = Arc<str>;

/// Type alias for a Json Web Key ID (`kid`).
type KeyId = Box<str>;

#[derive(Debug, thiserror::Error)]
pub enum JwtProcessingError {
    #[error("Invalid Access token: {0}")]
    InvalidAccessToken(#[source] JwtValidatorError),
    #[error("Invalid ID token: {0}")]
    InvalidIdToken(#[source] JwtValidatorError),
    #[error("Invalid Userinfo token: {0}")]
    InvalidUserinfoToken(#[source] JwtValidatorError),
    #[error(
        "Validation failed: id_token audience does not match the access_token client_id. \
         id_token.aud: {0:?}, access_token.client_id: {1:?}"
    )]
    IdTokenAudienceMismatch(String, String),
    #[error(
        "Validation failed: Userinfo token subject does not match the id_token subject. \
         userinfo_token.sub: {0:?}, id_token.sub: {1:?}"
    )]
    UserinfoSubMismatch(String, String),
    #[error(
        "Validation failed: Userinfo token audience ({0}) does not match the access_token \
         client_id ({1})."
    )]
    UserinfoAudienceMismatch(String, String),
    #[error(
        "CEDARLING_ID_TOKEN_TRUST_MODE is set to 'Strict', but the {0} is missing a required \
         claim: {1}"
    )]
    MissingClaimsInStrictMode(&'static str, &'static str),
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
}

#[derive(Debug, thiserror::Error)]
pub enum JwtServiceInitError {
    #[error(
        "Failed to initialize Key Service for JwtService due to a conflictig config: both a local \
         JWKS and trusted issuers was provided."
    )]
    ConflictingJwksConfig,
    #[error(
        "Failed to initialize Key Service for JwtService due to a missing config: no local JWKS \
         or trusted issuers was provided."
    )]
    MissingJwksConfig,
    #[error("Failed to initialize Key Service: {0}")]
    KeyService(#[from] KeyServiceError),
    #[error("Encountered an unsupported algorithm in the config: {0}")]
    UnsupportedAlgorithm(String),
    #[error("Failed to initialize JwtValidator: {0}")]
    InitJwtValidator(#[from] JwtValidatorError),
}

pub struct JwtService {
    access_tkn_validator: JwtValidator,
    id_tkn_validator: JwtValidator,
    userinfo_tkn_validator: JwtValidator,
    // TODO: implement the usage of this bootstrap property in
    // the authz module.
    #[allow(dead_code)]
    id_token_trust_mode: IdTokenTrustMode,
}

impl JwtService {
    pub async fn new(
        config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
    ) -> Result<Self, JwtServiceInitError> {
        let key_service: Arc<_> =
            match (&config.jwt_sig_validation, &config.jwks, &trusted_issuers) {
                // Case: no JWKS provided
                (true, None, None) => Err(JwtServiceInitError::MissingJwksConfig)?,
                // Case: Trusted issuers provided
                (true, None, Some(issuers)) => Some(
                    KeyService::new_from_trusted_issuers(issuers)
                        .await
                        .map_err(JwtServiceInitError::KeyService)?,
                ),
                // Case: Local JWKS provided
                (true, Some(jwks), None) => {
                    Some(KeyService::new_from_str(jwks).map_err(JwtServiceInitError::KeyService)?)
                },
                // Case: Both a local JWKS and trusted issuers were provided
                (true, Some(_), Some(_)) => Err(JwtServiceInitError::ConflictingJwksConfig)?,
                // Case: Signature validation is Off so no key service is needed.
                _ => None,
            }
            .into();

        // prepare shared configs
        let sig_validation: Arc<_> = config.jwt_sig_validation.into();
        let status_validation: Arc<_> = config.jwt_status_validation.into();
        let trusted_issuers: Arc<_> = trusted_issuers.clone().into();
        let algs_supported: Arc<HashSet<Algorithm>> =
            config.signature_algorithms_supported.clone().into();

        let access_tkn_validator = JwtValidator::new(
            JwtValidatorConfig {
                sig_validation: sig_validation.clone(),
                status_validation: status_validation.clone(),
                trusted_issuers: trusted_issuers.clone(),
                algs_supported: algs_supported.clone(),
                required_claims: config.access_token_config.required_claims(),
                validate_exp: config.access_token_config.exp_validation,
                validate_nbf: config.access_token_config.nbf_validation,
            },
            key_service.clone(),
        )?;

        let id_tkn_validator = JwtValidator::new(
            JwtValidatorConfig {
                sig_validation: sig_validation.clone(),
                status_validation: status_validation.clone(),
                trusted_issuers: trusted_issuers.clone(),
                algs_supported: algs_supported.clone(),
                required_claims: config.id_token_config.required_claims(),
                validate_exp: config.id_token_config.exp_validation,
                validate_nbf: config.id_token_config.nbf_validation,
            },
            key_service.clone(),
        )?;

        let userinfo_tkn_validator = JwtValidator::new(
            JwtValidatorConfig {
                sig_validation: sig_validation.clone(),
                status_validation: status_validation.clone(),
                trusted_issuers: trusted_issuers.clone(),
                algs_supported: algs_supported.clone(),
                required_claims: config.userinfo_token_config.required_claims(),
                validate_exp: config.userinfo_token_config.exp_validation,
                validate_nbf: config.userinfo_token_config.nbf_validation,
            },
            key_service.clone(),
        )?;

        Ok(Self {
            access_tkn_validator,
            id_tkn_validator,
            userinfo_tkn_validator,
            id_token_trust_mode: config.id_token_trust_mode,
        })
    }

    pub async fn process_token<'a>(
        &'a self,
        token: TokenStr<'a>,
    ) -> Result<Token<'a>, JwtProcessingError> {
        match token {
            TokenStr::Access(tkn_str) => {
                let token = self
                    .access_tkn_validator
                    .process_jwt(tkn_str)
                    .map_err(JwtProcessingError::InvalidAccessToken)?;
                let claims = serde_json::from_value::<TokenClaims>(token.claims)?;
                Ok(Token::new_access(claims, token.trusted_iss))
            },
            TokenStr::Id(tkn_str) => {
                let token = self
                    .id_tkn_validator
                    .process_jwt(tkn_str)
                    .map_err(JwtProcessingError::InvalidIdToken)?;
                let claims = serde_json::from_value::<TokenClaims>(token.claims)?;
                Ok(Token::new_id(claims, token.trusted_iss))
            },
            TokenStr::Userinfo(tkn_str) => {
                let token = self
                    .userinfo_tkn_validator
                    .process_jwt(tkn_str)
                    .map_err(JwtProcessingError::InvalidUserinfoToken)?;
                let claims = serde_json::from_value::<TokenClaims>(token.claims)?;
                Ok(Token::new_userinfo(claims, token.trusted_iss))
            },
        }
    }
}

#[cfg(test)]
mod test {
    use std::collections::HashSet;

    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use test_utils::assert_eq;
    use tokio::test;

    use super::test_utils::*;
    use super::{JwtService, Token, TokenClaims, TokenStr};
    use crate::{IdTokenTrustMode, JwtConfig, TokenValidationConfig};

    #[test]
    pub async fn can_validate_token() {
        // Generate token
        let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
        let access_tkn_claims = json!({
            "iss": "https://accounts.test.com",
            "sub": "some_sub",
            "jti": 1231231231,
            "exp": u64::MAX,
            "client_id": "test123",
        });
        let access_tkn = generate_token_using_claims(&access_tkn_claims, &keys)
            .expect("Should generate access token");
        let id_tkn_claims = json!({
            "iss": "https://accounts.test.com",
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let id_tkn =
            generate_token_using_claims(&id_tkn_claims, &keys).expect("Should generate id token");
        let userinfo_tkn_claims = json!({
            "iss": "https://accounts.test.com",
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let userinfo_tkn = generate_token_using_claims(&userinfo_tkn_claims, &keys)
            .expect("Should generate userinfo token");

        // Prepare JWKS
        let local_jwks = json!({"test_idp": generate_jwks(&vec![keys]).keys}).to_string();

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: Some(local_jwks),
                jwt_sig_validation: true,
                jwt_status_validation: false,
                id_token_trust_mode: IdTokenTrustMode::Strict,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                access_token_config: TokenValidationConfig::access_token(),
                id_token_config: TokenValidationConfig::id_token(),
                userinfo_token_config: TokenValidationConfig::userinfo_token(),
            },
            None,
        )
        .await
        .expect("Should create JwtService");

        // Test access_token
        let access_tkn = jwt_service
            .process_token(TokenStr::Access(&access_tkn))
            .await
            .expect("Should process access_token");
        let expected_claims = serde_json::from_value::<TokenClaims>(access_tkn_claims)
            .expect("Should create expected access_token claims");
        assert_eq!(access_tkn, Token::new_access(expected_claims.into(), None));

        // Test id_token
        let id_tkn = jwt_service
            .process_token(TokenStr::Id(&id_tkn))
            .await
            .expect("Should process id_token");
        let expected_claims = serde_json::from_value::<TokenClaims>(id_tkn_claims)
            .expect("Should create expected id_token claims");
        assert_eq!(id_tkn, Token::new_id(expected_claims, None));

        // Test userinfo_token
        let userinfo_tkn = jwt_service
            .process_token(TokenStr::Userinfo(&userinfo_tkn))
            .await
            .expect("Should process userinfo_token");
        let expected_claims = serde_json::from_value::<TokenClaims>(userinfo_tkn_claims)
            .expect("Should create expected userinfo_token claims");
        assert_eq!(userinfo_tkn, Token::new_userinfo(expected_claims, None));
    }
}
