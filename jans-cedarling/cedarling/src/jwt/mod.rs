/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # `JwtEngine`
//!
//! The `JwtEngine` is designed for managing JSON Web Tokens (JWTs) and provides the following functionalities:
//! - Fetching decoding keys from a JSON Web Key Set (JWKS) provided by Identity Providers (IDPs) and storing these keys.
//! - Extracting claims from JWTs for further processing and validation.
//! - Validating the signatures of JWTs to ensure their integrity and authenticity.
//! - Verifying the validity of JWTs based on claims such as expiration time and audience.

mod error;
mod http_client;
mod issuer;
mod jwk_store;
mod jwt_service_config;
mod new_key_service;
#[cfg(test)]
mod test;
#[cfg(test)]
mod test_utils;
mod token;
mod validator;

use crate::common::policy_store::TrustedIssuer;
use crate::{IdTokenTrustMode, NewJwtConfig};
use decoding_strategy::{open_id_storage::OpenIdStorage, DecodingStrategy};
use new_key_service::{NewKeyService, NewKeyServiceError};
use serde::de::DeserializeOwned;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use token::*;
use validator::{JwtValidator, JwtValidatorConfig, JwtValidatorError, ProcessedJwt};

pub use decoding_strategy::key_service::{HttpClient, KeyServiceError};
pub use decoding_strategy::ParseAlgorithmError;
pub use error::JwtServiceError;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;
pub(crate) mod decoding_strategy;

/// Type alias for Trusted Issuers' ID.
type TrustedIssuerId = Arc<str>;

/// Type alias for a Json Web Key ID (`kid`).
type KeyId = Box<str>;

#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum NewJwtServiceError {
    #[error("Failed to initialize JWT Service")]
    Init(#[from] NewJwtServiceInitError),
    #[error("Invalid Access Token")]
    InvalidAccessToken(#[source] JwtValidatorError),
    #[error("Invalid ID Token")]
    InvalidIdToken(#[source] JwtValidatorError),
    #[error("Invalid Userinfo Token")]
    InvalidUserinfoToken(#[source] JwtValidatorError),
    #[error("Validation failed: id_token audience does not match the access_token client_id. id_token.aud: {0:?}, access_token.client_id: {1:?}")]
    IdTokenAudienceMismatch(String, String),
    #[error("Validation failed: Userinfo token subject does not match the id_token subject. userinfo_token.sub: {0:?}, id_token.sub: {1:?}")]
    UserinfoSubMismatch(String, String),
    #[error(
        "Validation failed: Userinfo token audience ({0}) does not match the access_token client_id ({1})."
    )]
    UserinfoAudienceMismatch(String, String),
    #[error(
        "CEDARLING_ID_TOKEN_TRUST_MODE is set to 'Strict' but an Access Token was not provided."
    )]
    MissingAccessTokenInStrictMode,
    #[error("CEDARLING_ID_TOKEN_TRUST_MODE is set to 'Strict' but an ID Token was not provided.")]
    MissingIdTokenInStrictMode,
    #[error("CEDARLING_ID_TOKEN_TRUST_MODE is set to 'Strict', but the {0} is missing a required claim: {1}")]
    MissingClaimsInStrictMode(&'static str, &'static str),
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
}

#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum NewJwtServiceInitError {
    #[error("Failed to initialize Key Service for JwtService due to a conflictig config: both a local JWKS and trusted issuers was provided.")]
    ConflictingJwksConfig,
    #[error("Failed to initialize Key Service for JwtService due to a missing config: no local JWKS or trusted issuers was provided.")]
    MissingJwksConfig,
    #[error("Failed to initialize Key Service for JwtService: {0}")]
    KeyService(#[from] NewKeyServiceError),
    #[error("Encountered an unsupported algorithm in the config: {0}")]
    UnsupportedAlgorithm(String),
    #[error(transparent)]
    InitJwtValidator(#[from] JwtValidatorError),
}

pub struct NewJwtService {
    access_tkn_validator: JwtValidator,
    id_tkn_validator: JwtValidator,
    userinfo_tkn_validator: JwtValidator,
    id_token_trust_mode: IdTokenTrustMode,
}

#[allow(dead_code)]
pub struct ProcessJwtResult<'a> {
    pub access_token: Option<ProcessedJwt<'a>>,
    pub id_token: Option<ProcessedJwt<'a>>,
    pub userinfo_token: Option<ProcessedJwt<'a>>,
}

#[allow(dead_code)]
impl NewJwtService {
    pub fn new(
        config: &NewJwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
    ) -> Result<Self, NewJwtServiceInitError> {
        let key_service: Arc<_> =
            match (&config.jwt_sig_validation, &config.jwks, &trusted_issuers) {
                // Case: no JWKS provided
                (true, None, None) => Err(NewJwtServiceInitError::MissingJwksConfig)?,
                // Case: Trusted issuers provided
                (true, None, Some(issuers)) => Some(
                    NewKeyService::new_from_trusted_issuers(issuers)
                        .map_err(NewJwtServiceInitError::KeyService)?,
                ),
                // Case: Local JWKS provided
                (true, Some(jwks), None) => Some(
                    NewKeyService::new_from_str(jwks)
                        .map_err(NewJwtServiceInitError::KeyService)?,
                ),
                // Case: Both a local JWKS and trusted issuers were provided
                (true, Some(_), Some(_)) => Err(NewJwtServiceInitError::ConflictingJwksConfig)?,
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

    pub fn process_tokens<'a>(
        &'a self,
        access_token: Option<&'a str>,
        id_token: Option<&'a str>,
        userinfo_token: Option<&'a str>,
    ) -> Result<ProcessJwtResult, NewJwtServiceError> {
        let access_token = access_token
            .map(|jwt| self.access_tkn_validator.process_jwt(jwt))
            .transpose()
            .map_err(NewJwtServiceError::InvalidAccessToken)?;
        let id_token = id_token
            .map(|jwt| self.id_tkn_validator.process_jwt(jwt))
            .transpose()
            .map_err(NewJwtServiceError::InvalidIdToken)?;
        let userinfo_token = userinfo_token
            .map(|jwt| self.userinfo_tkn_validator.process_jwt(jwt))
            .transpose()
            .map_err(NewJwtServiceError::InvalidUserinfoToken)?;

        // Additional checks for STRICT MODE
        if self.id_token_trust_mode == IdTokenTrustMode::Strict {
            // Check if id_token.sub == access_token.client_id
            let id_tkn_aud = id_token
                .as_ref()
                .ok_or(NewJwtServiceError::MissingIdTokenInStrictMode)?
                .claims
                .get("aud")
                .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                    "id_token", "aud",
                ))?;
            let access_tkn_client_id = access_token
                .as_ref()
                .ok_or(NewJwtServiceError::MissingAccessTokenInStrictMode)?
                .claims
                .get("client_id")
                .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                    "access_token",
                    "client_id",
                ))?;
            if id_tkn_aud != access_tkn_client_id {
                Err(NewJwtServiceError::IdTokenAudienceMismatch(
                    serde_json::from_value::<String>(id_tkn_aud.clone())?,
                    serde_json::from_value::<String>(access_tkn_client_id.clone())?,
                ))?
            }

            // If userinfo token is present, check if:
            // 1. userinfo_token.sub == id_token.sub
            // 2. userinfo_token.aud == access_token.client_id
            if let Some(token) = &userinfo_token {
                let id_tkn_sub = id_token
                    .as_ref()
                    .ok_or(NewJwtServiceError::MissingIdTokenInStrictMode)?
                    .claims
                    .get("sub")
                    .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                        "ID Token", "sub",
                    ))?;
                let usrinfo_sub = token.claims.get("sub").ok_or(
                    NewJwtServiceError::MissingClaimsInStrictMode("Userinfo Token", "sub"),
                )?;
                if usrinfo_sub != id_tkn_sub {
                    Err(NewJwtServiceError::UserinfoSubMismatch(
                        serde_json::from_value::<String>(usrinfo_sub.clone())?,
                        serde_json::from_value::<String>(id_tkn_sub.clone())?,
                    ))?
                }

                let usrinfo_aud = token.claims.get("aud").ok_or(
                    NewJwtServiceError::MissingClaimsInStrictMode("Userinfo Token", "aud"),
                )?;
                if usrinfo_aud != access_tkn_client_id {
                    Err(NewJwtServiceError::UserinfoAudienceMismatch(
                        serde_json::from_value::<String>(usrinfo_aud.clone())?,
                        serde_json::from_value::<String>(access_tkn_client_id.clone())?,
                    ))?
                }
            }
        }

        Ok(ProcessJwtResult {
            access_token,
            id_token,
            userinfo_token,
        })
    }
}

pub struct JwtService {
    decoding_strategy: DecodingStrategy,
    open_id_storage: OpenIdStorage,
}

/// A service for handling JSON Web Tokens (JWT).
///
/// The `JwtService` struct provides functionality to decode and optionally validate
/// JWTs based on a specified decoding strategy. It can be configured to either
/// perform validation or to decode without validation, depending on the provided
/// configuration. It is an internal module used by other components of the library.
impl JwtService {
    /// Initializes a new `JwtService` instance based on the provided configuration.
    pub(crate) fn new_with_config(config: JwtServiceConfig) -> Self {
        match config {
            JwtServiceConfig::WithoutValidation { trusted_idps } => {
                let decoding_strategy = DecodingStrategy::new_without_validation();
                Self {
                    decoding_strategy,
                    open_id_storage: OpenIdStorage::new(trusted_idps),
                }
            },
            JwtServiceConfig::WithValidation {
                supported_algs,
                trusted_idps,
            } => {
                let decoding_strategy = DecodingStrategy::new_with_validation(
                    supported_algs,
                    // TODO: found the way to use `OpenIdStorage` in the decoding strategy.
                    // Or use more suitable structure
                    trusted_idps
                        .iter()
                        .map(|v| v.trusted_issuer.clone())
                        .collect(),
                )
                // TODO: remove expect here and all data should be already in the `JwtServiceConfig`
                .expect("could not initialize decoding strategy with validation");
                Self {
                    decoding_strategy,
                    open_id_storage: OpenIdStorage::new(trusted_idps),
                }
            },
        }
    }

    /// Decodes and validates an `access_token`, `id_token`, and `userinfo_token`.
    ///
    /// # Token Validation Rules:
    /// - token signature must be valid
    /// - token must not be expired.
    /// - token must not be used before the `nbf` timestamp.
    ///
    /// # Returns
    /// A tuple containing the decoded claims for the `access_token`, `id_token`, and
    /// `userinfo_token`.
    ///
    /// # Errors
    /// Returns an error if decoding or validation of either any token fails.
    pub fn decode_tokens<A, I, U>(
        &self,
        access_token: &str,
        id_token: &str,
        userinfo_token: &str,
    ) -> Result<DecodeTokensResult<A, I, U>, JwtServiceError>
    where
        A: DeserializeOwned,
        I: DeserializeOwned,
        U: DeserializeOwned,
    {
        // extract claims without validation
        let access_token_claims = DecodingStrategy::extract_claims(access_token)
            .map_err(JwtServiceError::InvalidAccessToken)?;
        let id_token_claims =
            DecodingStrategy::extract_claims(id_token).map_err(JwtServiceError::InvalidIdToken)?;
        let userinfo_token_claims = DecodingStrategy::extract_claims(userinfo_token)
            .map_err(JwtServiceError::InvalidUserinfoToken)?;

        // Validate the access_token's signature and optionally, exp and nbf.
        let access_token = self
            .decoding_strategy
            .decode::<AccessToken>(access_token)
            .map_err(JwtServiceError::InvalidAccessToken)?;

        // Validate the id_token's signature and optionally, exp and nbf.
        self.decoding_strategy
            .decode::<IdToken>(id_token)
            .map_err(JwtServiceError::InvalidIdToken)?;

        // validate the userinfo_token's signature and optionally, exp and nbf.
        self.decoding_strategy
            .decode::<UserInfoToken>(userinfo_token)
            .map_err(JwtServiceError::InvalidUserinfoToken)?;

        // assume that all tokens has the same `iss` (issuer) so we get config only for one JWT token
        // this behavior can be changed in future
        let trusted_issuer = self
            .open_id_storage
            .get(access_token.iss.as_str())
            .map(|config| &config.trusted_issuer);

        Ok(DecodeTokensResult {
            access_token: access_token_claims,
            id_token: id_token_claims,
            userinfo_token: userinfo_token_claims,
            trusted_issuer,
        })
    }
}

#[derive(Debug)]
pub struct DecodeTokensResult<'a, A, I, U> {
    pub access_token: A,
    pub id_token: I,
    pub userinfo_token: U,

    pub trusted_issuer: Option<&'a TrustedIssuer>,
}

#[cfg(test)]
mod new_test {
    use super::test_utils::*;
    use super::NewJwtService;
    use crate::IdTokenTrustMode;
    use crate::NewJwtConfig;
    use crate::TokenValidationConfig;
    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use std::collections::HashSet;

    #[test]
    pub fn can_validate_tokens() {
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

        let jwt_service = NewJwtService::new(
            &NewJwtConfig {
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
        .expect("Should create JwtService");

        jwt_service
            .process_tokens(Some(&access_tkn), Some(&id_tkn), Some(&userinfo_tkn))
            .expect("Should process JWTs");
    }
}
