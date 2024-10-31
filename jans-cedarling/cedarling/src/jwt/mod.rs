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

mod decoding_strategy;
mod error;
mod jwt_service_config;
#[cfg(test)]
mod test;
mod token;

pub use decoding_strategy::{string_to_alg, ParseAlgorithmError};
use decoding_strategy::{DecodingArgs, DecodingStrategy};
pub use error::Error;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;
use serde::de::DeserializeOwned;
use token::*;

pub struct JwtService {
    decoding_strategy: DecodingStrategy,
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
            JwtServiceConfig::WithoutValidation => {
                let decoding_strategy = DecodingStrategy::new_without_validation();
                Self { decoding_strategy }
            },
            JwtServiceConfig::WithValidation {
                supported_algs,
                trusted_idps,
            } => {
                let decoding_strategy =
                    DecodingStrategy::new_with_validation(supported_algs, trusted_idps)
                        .expect("could not initialize decoding strategy with validation");
                Self { decoding_strategy }
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
    ) -> Result<(A, I, U), Error>
    where
        A: DeserializeOwned,
        I: DeserializeOwned,
        U: DeserializeOwned,
    {
        // extract claims without validation
        let access_token_claims =
            DecodingStrategy::extract_claims(access_token).map_err(Error::InvalidAccessToken)?;
        let id_token_claims =
            DecodingStrategy::extract_claims(id_token).map_err(Error::InvalidIdToken)?;
        let userinfo_token_claims = DecodingStrategy::extract_claims(userinfo_token)
            .map_err(Error::InvalidUserinfoToken)?;

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
            .map_err(Error::InvalidAccessToken)?;

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
            .map_err(Error::InvalidIdToken)?;

        // validate the `userinfo_token`.
        // - checks if userinfo_token.iss == access_token.iss
        // - checks if userinfo_token.aud == access_token.aud
        // - checks if userinfo_token.sub == access_token.sub
        // - checks if `nbf` has passed
        // - checks if token is not expired
        self.decoding_strategy
            .decode::<UserInfoToken>(DecodingArgs {
                jwt: userinfo_token,
                iss: Some(&access_token.iss),
                aud: Some(&access_token.aud),
                sub: Some(&id_token.sub),
                validate_nbf: true,
                validate_exp: true,
            })
            .map_err(Error::InvalidUserinfoToken)?;

        Ok((access_token_claims, id_token_claims, userinfo_token_claims))
    }
}
