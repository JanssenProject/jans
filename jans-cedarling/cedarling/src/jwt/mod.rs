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

use decoding_strategy::{open_id_storage::OpenIdStorage, DecodingArgs, DecodingStrategy};
use serde::de::DeserializeOwned;
use token::*;

pub use decoding_strategy::key_service::{HttpClient, KeyServiceError};
pub use decoding_strategy::{string_to_alg, ParseAlgorithmError};
pub use error::JwtServiceError;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;

use crate::common::policy_store::TrustedIssuerMetadata;
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
    pub trusted_issuer: Option<&'a TrustedIssuerMetadata>,
}
