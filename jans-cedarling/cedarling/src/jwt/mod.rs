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

mod issuers_store;
mod jwk_store;
mod key_service;
#[cfg(test)]
mod test_utils;
mod validator;

use crate::common::policy_store::TrustedIssuer;
use crate::{IdTokenTrustMode, JwtConfig};
use key_service::{KeyService, KeyServiceError};
use serde::de::DeserializeOwned;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use validator::{JwtValidator, JwtValidatorConfig, JwtValidatorError};

pub use jsonwebtoken::Algorithm;

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
    #[error("Validation failed: id_token audience does not match the access_token client_id. id_token.aud: {0:?}, access_token.client_id: {1:?}")]
    IdTokenAudienceMismatch(String, String),
    #[error("Validation failed: Userinfo token subject does not match the id_token subject. userinfo_token.sub: {0:?}, id_token.sub: {1:?}")]
    UserinfoSubMismatch(String, String),
    #[error(
        "Validation failed: Userinfo token audience ({0}) does not match the access_token client_id ({1})."
    )]
    UserinfoAudienceMismatch(String, String),
    #[error("CEDARLING_ID_TOKEN_TRUST_MODE is set to 'Strict', but the {0} is missing a required claim: {1}")]
    MissingClaimsInStrictMode(&'static str, &'static str),
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
}

#[derive(Debug, thiserror::Error)]
pub enum JwtServiceInitError {
    #[error("Failed to initialize Key Service for JwtService due to a conflictig config: both a local JWKS and trusted issuers was provided.")]
    ConflictingJwksConfig,
    #[error("Failed to initialize Key Service for JwtService due to a missing config: no local JWKS or trusted issuers was provided.")]
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
    id_token_trust_mode: IdTokenTrustMode,
}

impl JwtService {
    pub fn new(
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

    pub fn process_tokens<'a, A, I, U>(
        &'a self,
        access_token: &'a str,
        id_token: &'a str,
        userinfo_token: Option<&'a str>,
    ) -> Result<ProcessTokensResult<'a, A, I, U>, JwtProcessingError>
    where
        A: DeserializeOwned,
        I: DeserializeOwned,
        U: DeserializeOwned,
    {
        let access_token = self
            .access_tkn_validator
            .process_jwt(access_token)
            .map_err(JwtProcessingError::InvalidAccessToken)?;
        let id_token = self
            .id_tkn_validator
            .process_jwt(id_token)
            .map_err(JwtProcessingError::InvalidIdToken)?;
        let userinfo_token = userinfo_token
            .map(|jwt| self.userinfo_tkn_validator.process_jwt(jwt))
            .transpose()
            .map_err(JwtProcessingError::InvalidUserinfoToken)?;

        // Additional checks for STRICT MODE
        if self.id_token_trust_mode == IdTokenTrustMode::Strict {
            // Check if id_token.sub == access_token.client_id
            let id_tkn_aud =
                id_token
                    .claims
                    .get("aud")
                    .ok_or(JwtProcessingError::MissingClaimsInStrictMode(
                        "id_token", "aud",
                    ))?;
            let access_tkn_client_id = access_token.claims.get("client_id").ok_or(
                JwtProcessingError::MissingClaimsInStrictMode("access_token", "client_id"),
            )?;
            if id_tkn_aud != access_tkn_client_id {
                Err(JwtProcessingError::IdTokenAudienceMismatch(
                    serde_json::from_value::<String>(id_tkn_aud.clone())?,
                    serde_json::from_value::<String>(access_tkn_client_id.clone())?,
                ))?
            }

            // If userinfo token is present, check if:
            // 1. userinfo_token.sub == id_token.sub
            // 2. userinfo_token.aud == access_token.client_id
            if let Some(token) = &userinfo_token {
                let id_tkn_sub = id_token.claims.get("sub").ok_or(
                    JwtProcessingError::MissingClaimsInStrictMode("ID Token", "sub"),
                )?;
                let usrinfo_sub = token.claims.get("sub").ok_or(
                    JwtProcessingError::MissingClaimsInStrictMode("Userinfo Token", "sub"),
                )?;
                if usrinfo_sub != id_tkn_sub {
                    Err(JwtProcessingError::UserinfoSubMismatch(
                        serde_json::from_value::<String>(usrinfo_sub.clone())?,
                        serde_json::from_value::<String>(id_tkn_sub.clone())?,
                    ))?
                }

                let usrinfo_aud = token.claims.get("aud").ok_or(
                    JwtProcessingError::MissingClaimsInStrictMode("Userinfo Token", "aud"),
                )?;
                if usrinfo_aud != access_tkn_client_id {
                    Err(JwtProcessingError::UserinfoAudienceMismatch(
                        serde_json::from_value::<String>(usrinfo_aud.clone())?,
                        serde_json::from_value::<String>(access_tkn_client_id.clone())?,
                    ))?
                }
            }
        }

        let userinfo_token = match userinfo_token {
            Some(token) => token,
            None => unimplemented!("Having no userinfo token is not yet supported."),
        };

        Ok(ProcessTokensResult {
            access_token: serde_json::from_value::<A>(access_token.claims)?,
            id_token: serde_json::from_value::<I>(id_token.claims)?,
            userinfo_token: serde_json::from_value::<U>(userinfo_token.claims)?,
            // we just assume that all the tokens have the same issuer so we get the
            // issuer from the access token.
            // this behavior might be changed in future
            trusted_issuer: access_token.trusted_iss,
        })
    }
}

#[derive(Debug)]
pub struct ProcessTokensResult<'a, A, I, U> {
    pub access_token: A,
    pub id_token: I,
    pub userinfo_token: U,
    pub trusted_issuer: Option<&'a TrustedIssuer>,
}

#[cfg(test)]
mod test {
    use super::test_utils::*;
    use super::JwtService;
    use crate::IdTokenTrustMode;
    use crate::JwtConfig;
    use crate::TokenValidationConfig;
    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use serde_json::Value;
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
        .expect("Should create JwtService");

        jwt_service
            .process_tokens::<Value, Value, Value>(&access_tkn, &id_tkn, Some(&userinfo_tkn))
            .expect("Should process JWTs");
    }
}
