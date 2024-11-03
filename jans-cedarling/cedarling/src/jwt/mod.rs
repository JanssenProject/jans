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

use decoding_strategy::DecodingStrategy;
pub use decoding_strategy::{string_to_alg, ParseAlgorithmError};
pub use error::*;
pub use jsonwebtoken::Algorithm;
pub use jwt_service_config::*;
pub use token::*;

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
    /// 1. The `access_token` is decoded and validated first, with its `aud` (which is also the `client_id`)
    ///    stored for later use.
    /// 2. The `id_token.aud` is then validated against the `access_token.aud` and the `id_token.iss` with `access_token.iss`.
    /// 3. An error is returned if `id_token.aud` does not match `access_token.client_id` or
    ///    `id_token.iss` does not match with `access_token.iss`.
    /// 4. The `userinfo_token.client_id` is then validated against the `access_token.aud` and the `userinfo_token.sub` with `id_token.sub`.
    /// 5. An error is returned if `userinfo.client_id` does not match `access_token.aud` or
    ///    `userinfo.sub` does not match with `id_token.sub`.
    ///
    /// # Returns
    /// A tuple containing the decoded claims for the `access_token`, `id_token`, and
    /// `userinfo_token`.
    ///
    /// # Errors
    /// Returns an error if decoding or validation of either token fails.
    pub fn decode_tokens(
        &self,
        access_token: &str,
        id_token: &str,
        userinfo_token: &str,
    ) -> Result<(AccessToken, IdToken, UserinfoToken), JwtDecodingError> {
        // Validate the `access_token`.
        //
        // Context: This token is being used as proof of authentication (AuthN).
        // Validating the `iss` and `aud` claims can help ensure that the token is issued by a
        // trusted source and is intended for the client that is making the request.
        let access_token = self
            .decoding_strategy
            .decode::<AccessToken>(
                access_token,
                None::<String>,
                None::<String>,
                None::<String>,
                true,
                true,
            )
            .map_err(JwtDecodingError::InvalidAccessToken)?;

        // Validate the `id_token` against the `access_token`'s `iss` (issuer) and `aud` (audience).
        // This ensures that the `id_token` was issued by the same entity (`iss`) and intended for
        // the same audience (`aud`) as the `access_token`.
        let id_token = self
            .decoding_strategy
            .decode::<IdToken>(
                id_token,
                Some(&access_token.0.iss),
                Some(&access_token.0.aud),
                // we don't validate the `sub` (subject) here, as it is typically checked when
                // validating the `userinfo_token`. The `sub` claim identifies the end user.
                None::<String>,
                true,
                true,
            )
            .map_err(JwtDecodingError::InvalidIdToken)?;

        // validate the `userinfo_token`.
        // - The `aud` (audience) should match the `access_token`'s `aud` to ensure it was issued
        //   for the same client.
        // - The `iss` (issuer) should match the `access_token`'s `iss` to ensure it comes from
        //   the same trusted identity provider.
        // - We validate that the `sub` (subject) in the `userinfo_token` matches the `id_token`'s `sub`,
        //   confirming the tokens are referring to the same user.
        let userinfo_token = self
            .decoding_strategy
            .decode::<UserinfoToken>(
                userinfo_token,
                Some(&access_token.0.iss),
                Some(&access_token.0.aud),
                Some(&id_token.0.sub), // ensure that the `sub` is the same as with the id_token's sub
                false,                 // this token usually does not have an nbf field
                false,                 // this token usually does not have an exp field
            )
            .map_err(JwtDecodingError::InvalidUserinfoToken)?;

        Ok((access_token, id_token, userinfo_token))
    }
}
