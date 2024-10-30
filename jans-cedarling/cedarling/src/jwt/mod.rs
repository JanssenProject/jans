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
mod jwt_service_config;
#[cfg(test)]
mod test;
mod token;

use decoding_strategy::{key_service, DecodingStrategy};
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
    /// Creates a new instance of `JwtService` for testing purposes.
    ///
    /// This constructor allows for the injection of a specific decoding strategy,
    /// facilitating unit testing by simulating different decoding and validation scenarios.
    /// It is useful for testing both successful and failing cases for various token types.
    #[cfg(test)]
    pub fn new(decoding_strategy: DecodingStrategy) -> Self {
        Self { decoding_strategy }
    }

    /// Initializes a new `JwtService` instance based on the provided configuration.
    pub(crate) fn new_with_config(config: JwtServiceConfig) -> Result<Self, InitError> {
        match config {
            JwtServiceConfig::WithoutValidation => {
                let decoding_strategy = DecodingStrategy::new_without_validation();
                Ok(Self { decoding_strategy })
            },
            JwtServiceConfig::WithValidation {
                supported_algs,
                trusted_idps,
            } => {
                let decoding_strategy =
                    DecodingStrategy::new_with_validation(supported_algs, trusted_idps)?;

                Ok(Self { decoding_strategy })
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
    /// 1. The `access_token` is decoded and validated first, with its `aud` (which is also the `client_id`)
    ///    stored for later use.
    /// 2. The `id_token` is then validated against the `access_token.aud` (client_id) and `access_token.iss` (issuer).
    /// 3. An error is returned if `id_token.aud` does not match `access_token.client_id`.
    ///
    /// # Returns
    /// A tuple containing the decoded claims for the `access_token` and `id_token`.
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
        // TODO: Improve error handling.
        // Current error show same error for each JWT token. It makes almost impossible to understand what is the problem.

        // extract claims without validation
        let access_token_claims = self.decoding_strategy.extract_claims(access_token)?;
        let id_token_claims = self.decoding_strategy.extract_claims(id_token)?;
        let userinfo_token_claims = self.decoding_strategy.extract_claims(userinfo_token)?;

        // validate access_token
        let access_token = self.decoding_strategy.decode::<AccessToken>(
            access_token,
            None::<String>, // TODO: validate issuer for access token
            None::<String>,
            false,
        )?;

        // validate the id_token against the access_token's `iss` and `aud`
        self.decoding_strategy.decode::<IdToken>(
            id_token,
            Some(&access_token.iss),
            Some(&access_token.aud),
            true,
        )?;

        // TODO: validate user info token
        Ok((access_token_claims, id_token_claims, userinfo_token_claims))
    }
}

/// Error type for initialization failures in the JWT service.
///
/// This enum represents errors that may occur during the initialization of
/// components within the JWT service, such as the decoding strategy and key service.
/// It provides a way to propagate errors from lower-level modules to the top level.
#[derive(thiserror::Error, Debug)]
pub enum InitError {
    /// An error occurred while initializing the decoding strategy.
    ///
    /// This error occurs when the `DecodingStrategy` failed to initialize,
    /// potentially due to issues such as invalid configuration, missing keys, or
    /// other initialization errors.
    #[error("Error initializing Decoding Strategy: {0}")]
    DecodingStrategy(#[from] decoding_strategy::Error),

    /// An error occurred while initializing the key service.
    ///
    /// This error occurs when the `KeyService` could not be initialized,
    /// which may happen due to problems like failing to retrieve necessary keys
    /// or encountering network errors while accessing a key provider.
    #[error("Error initializing Key Service: {0}")]
    KeyService(#[from] key_service::Error),
}

/// Error type for issues encountered in the JWT service.
///
/// This enum encapsulates various errors that may arise while decoding or
/// validating JWTs within the service. It serves as a central point for
/// handling errors related to JWT processing, allowing easy propagation
/// of errors from lower-level components.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// An error occurred during JWT decoding.
    ///
    /// This error occurs when the JWT could not be decoded properly,
    /// which may be due to issues such as malformed tokens or unsupported
    /// algorithms in the JWT header.
    #[error("Error decoding JWT: {0}")]
    Decoding(#[from] decoding_strategy::Error),

    /// An error from the key service occurred while processing JWTs.
    ///
    /// This error occurs when there was an issue related to key retrieval,
    /// management, or validation during JWT processing, originating from the
    /// `KeyService`.
    #[error("Key Service error: {0}")]
    KeyService(#[from] key_service::Error),
}
