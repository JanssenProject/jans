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

#[cfg(test)]
mod test;

mod error;
mod jwt_service_config;
mod token;

use crate::common::policy_store::TrustedIssuer;
use decoding_strategy::{open_id_storage::OpenIdStorage, DecodingStrategy};
use serde::de::DeserializeOwned;
use token::*;

pub use decoding_strategy::key_service::{HttpClient, KeyServiceError};
pub use decoding_strategy::ParseAlgorithmError;
pub use error::JwtServiceError;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;
pub(crate) mod decoding_strategy;

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
