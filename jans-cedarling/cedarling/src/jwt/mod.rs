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
use decoding_strategy::{open_id_storage::OpenIdStorage, DecodingArgs, DecodingStrategy};
use new_key_service::{NewKeyService, NewKeyServiceError};
use serde::de::DeserializeOwned;
use std::collections::HashSet;
use std::rc::Rc;
use token::*;
use validator::{JwtValidator, JwtValidatorConfig, JwtValidatorError};

pub use decoding_strategy::key_service::{HttpClient, KeyServiceError};
pub use decoding_strategy::{string_to_alg, ParseAlgorithmError};
pub use error::JwtServiceError;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;
pub use validator::TokenClaims;
pub(crate) mod decoding_strategy;

/// Type alias for Trusted Issuers' ID.
type TrustedIssuerId = Rc<str>;

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
}

pub struct NewJwtService {
    access_tkn_validator: JwtValidator,
    id_tkn_validator: JwtValidator,
    userinfo_tkn_validator: JwtValidator,
    id_token_trust_mode: IdTokenTrustMode,
}

pub struct ProcessJwtResult {
    pub access_token: Option<TokenClaims>,
    pub id_token: Option<TokenClaims>,
    pub userinfo_token: Option<TokenClaims>,
}

#[allow(dead_code)]
impl NewJwtService {
    pub fn new_from_config(config: NewJwtConfig) -> Result<Self, NewJwtServiceError> {
        // prepare shared configs
        let sig_validation: Rc<_> = config.jwt_sig_validation.into();
        let status_validation: Rc<_> = config.jwt_status_validation.into();
        let trusted_issuers: Rc<_> = config.trusted_issuers.clone().into();
        let algs_supported: Rc<HashSet<Algorithm>> = config.signature_algorithms_supported.into();

        // prepare key service
        let key_service: Rc<_> = match (&config.local_jwks, &config.trusted_issuers) {
            // Case: no JWKS provided
            (None, None) => Err(NewJwtServiceInitError::MissingJwksConfig)?,
            // Case: Trusted issuers provided
            (None, Some(trusted_issuers)) => {
                NewKeyService::new_from_trusted_issuers(trusted_issuers)
                    .map_err(NewJwtServiceInitError::KeyService)?
            },
            // Case: Local JWKS provided
            (Some(jwks), None) => {
                NewKeyService::new_from_str(jwks).map_err(NewJwtServiceInitError::KeyService)?
            },
            // Case: Both a local JWKS and trusted issuers were provided
            (Some(_), Some(_)) => Err(NewJwtServiceInitError::ConflictingJwksConfig)?,
        }
        .into();

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
        );

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
        );

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
        );

        Ok(Self {
            access_tkn_validator,
            id_tkn_validator,
            userinfo_tkn_validator,
            id_token_trust_mode: config.id_token_trust_mode,
        })
    }

    pub fn process_tokens(
        &self,
        access_token: Option<&str>,
        id_token: Option<&str>,
        userinfo_token: Option<&str>,
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
                .get("aud")
                .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                    "id_token", "aud",
                ))?;
            let access_tkn_client_id = access_token
                .as_ref()
                .ok_or(NewJwtServiceError::MissingAccessTokenInStrictMode)?
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
            if let Some(claims) = &userinfo_token {
                let id_tkn_sub = id_token
                    .as_ref()
                    .ok_or(NewJwtServiceError::MissingIdTokenInStrictMode)?
                    .get("sub")
                    .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                        "ID Token", "sub",
                    ))?;
                let usrinfo_sub =
                    claims
                        .get("sub")
                        .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                            "Userinfo Token",
                            "sub",
                        ))?;
                if usrinfo_sub != id_tkn_sub {
                    Err(NewJwtServiceError::UserinfoSubMismatch(
                        serde_json::from_value::<String>(usrinfo_sub.clone())?,
                        serde_json::from_value::<String>(id_tkn_sub.clone())?,
                    ))?
                }

                let usrinfo_aud =
                    claims
                        .get("aud")
                        .ok_or(NewJwtServiceError::MissingClaimsInStrictMode(
                            "Userinfo Token",
                            "aud",
                        ))?;
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

    /// Decodes and validates both an `access_token` and an `id_token`.
    ///
    /// This method decodes both tokens, validates them according to the internal
    /// `DecodingStrategy`, and enforces token relationships, ensuring that the
    /// `id_token` is validated against claims from the `access_token`.
    ///
    /// # Token Validation Rules:
    /// - `access_token.iss` == `id_token.iss` == `userinfo_token.iss`
    /// - `access_token.aud` == `id_token.aud` == `userinfo_token.aud`
    /// - `id_token.sub` == `userinfo_token.sub`
    /// - token must not be expired.
    /// - token must not be used before the `nbf` timestamp.
    ///
    /// # Returns
    /// A tuple containing the decoded claims for the `access_token`, `id_token`, and
    /// `userinfo_token`.
    ///
    /// # Errors
    /// Returns an error if decoding or validation of either token fails.
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

        // Validate the `access_token`.
        //
        // - checks if `nbf` has passed
        // - checks if token is not expired
        //
        // Context: This token is being used as proof of authentication (AuthN).
        // Validating the  `aud` might not be needed because of this.
        //
        // TODO: validate the `iss` by checking if it's from a trusted issuer in the
        // `policy_store.json`.
        let access_token = self
            .decoding_strategy
            .decode::<AccessToken>(DecodingArgs {
                jwt: access_token,
                iss: None,
                aud: None,
                sub: None,
                validate_nbf: true,
                validate_exp: true,
            })
            .map_err(JwtServiceError::InvalidAccessToken)?;

        // Validate the `id_token`
        // - checks if id_token.iss == access_token.iss
        // - checks if id_token.aud == access_token.aud
        // - checks if `nbf` has passed
        // - checks if token is not expired
        let id_token = self
            .decoding_strategy
            .decode::<IdToken>(DecodingArgs {
                jwt: id_token,
                iss: Some(&access_token.iss),
                aud: Some(&access_token.aud),
                sub: None,
                validate_nbf: true,
                validate_exp: true,
            })
            .map_err(JwtServiceError::InvalidIdToken)?;

        // validate the `userinfo_token`.
        // - checks if userinfo_token.iss == access_token.iss
        // - checks if userinfo_token.aud == access_token.aud
        // - checks if userinfo_token.sub == access_token.sub
        self.decoding_strategy
            .decode::<UserInfoToken>(DecodingArgs {
                jwt: userinfo_token,
                // Getting next values from access token looks little strange for me
                // TODO: add comment here why we are doing in this way
                // We also need to check if `Userinfo token` not associated with a sub from the `id_token`
                // https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#cedarling-token-validation
                iss: Some(&access_token.iss),
                aud: Some(&access_token.aud),
                sub: Some(&id_token.sub),
                validate_nbf: false, // userinfo tokens do not have a `nbf` claim
                validate_exp: false, // userinfo tokens do not have an `exp` claim
            })
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
        let local_jwks = Some(json!({"test_idp": generate_jwks(&vec![keys]).keys}).to_string());

        let jwt_service = NewJwtService::new_from_config(NewJwtConfig {
            local_jwks,
            trusted_issuers: None,
            jwt_sig_validation: true,
            jwt_status_validation: false,
            id_token_trust_mode: IdTokenTrustMode::Strict,
            signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
            access_token_config: TokenValidationConfig::access_token(),
            id_token_config: TokenValidationConfig::id_token(),
            userinfo_token_config: TokenValidationConfig::userinfo_token(),
        })
        .expect("Should create JwtService");

        jwt_service
            .process_tokens(Some(&access_tkn), Some(&id_tkn), Some(&userinfo_tkn))
            .expect("Should process JWTs");
    }
}
