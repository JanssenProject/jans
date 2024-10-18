/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::decoding_strategy::DecodingStrategy;
use super::token::{AccessToken, IdToken};
use super::traits::{Decode, ExtractClaims};
use super::{CreateJwtServiceError, Error};
use crate::JwtConfig;
use serde::de::DeserializeOwned;

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
    /// This constructor is intended for unit testing, allowing the injection of a
    /// specific decoding strategy. By using this, tests can simulate both successful
    /// and failing decoding scenarios for different token types.
    #[cfg(test)]
    pub fn new(decoding_strategy: DecodingStrategy) -> Self {
        Self { decoding_strategy }
    }

    /// Initializes a new `JwtService` instance based on the provided configuration.
    ///
    /// This method is used to create a `JwtService`. JWT validation can be toggled via the
    /// provided `JwtConfig`.
    pub fn new_with_container(
        dep_map: &di::DependencyMap,
        config: JwtConfig,
    ) -> Result<Self, CreateJwtServiceError> {
        match config {
            JwtConfig::Disabled => {
                let decoding_strategy = DecodingStrategy::new_without_validation();
                Ok(Self { decoding_strategy })
            },
            JwtConfig::Enabled {
                signature_algorithms,
            } => {
                let decoding_strategy =
                    DecodingStrategy::new_with_validation(dep_map, &signature_algorithms)?;

                Ok(Self { decoding_strategy })
            },
        }
    }

    /// Decodes and validates an `access_token` and an `id_token`.
    ///
    /// This method decodes both the `access_token` and `id_token`, validating them according
    /// to the rules defined by the internal `DecodingStrategy`. The `access_token` is validated
    /// first, and its `iss` and `aud` claims are used to validate the `id_token`.
    ///
    /// Token Validation Rules:
    ///     1. The `access_token` is validated first, and its `aud` (which is also the `client_id`) is stored.
    ///     2. The `id_token` is validated against the `access_token.aud` (client_id) and `access_token.iss` (issuer).
    ///     3. Return an error if `id_token.aud != access_token.client_id`.
    pub fn decode_tokens<A, T>(
        &self,
        access_token_str: &str,
        id_token_str: &str,
    ) -> Result<(A, T), Error>
    where
        A: DeserializeOwned,
        T: DeserializeOwned,
    {
        let access_token_claims = self.decoding_strategy.extract_claims(access_token_str)?;
        let id_token_claims = self.decoding_strategy.extract_claims(id_token_str)?;

        // validate access_token
        let access_token = self.decoding_strategy.decode::<AccessToken>(
            access_token_str,
            None::<String>, // TODO: validate issuer for access token
            None::<String>,
            false,
        )?;

        // validate id_token
        // should error if `iss` and `aud` is different from `access_token`
        self.decoding_strategy.decode::<IdToken>(
            id_token_str,
            Some(&access_token.iss),
            Some(&access_token.aud),
            true,
        )?;

        Ok((access_token_claims, id_token_claims))
    }
}
