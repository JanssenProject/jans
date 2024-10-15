/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::decoding_strategy::DecodingStrategy;
use super::traits::Decode;
use super::Error;
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
/// configuration.
impl JwtService {
    /// Creates a new instance of `JwtService` for testing purposes.
    ///
    /// This method allows for the creation of a `JwtService` with a specified
    /// decoding strategy, primarily used in unit tests to simulate different
    /// decoding scenarios.
    #[cfg(test)]
    pub fn new(decoding_strategy: DecodingStrategy) -> Self {
        Self { decoding_strategy }
    }

    /// Initializes a new `JwtService` instance based on the provided configuration.
    ///
    /// This method creates a `JwtService` with a decoding strategy determined by
    /// the specified `JwtConfig`. If the configuration is set to disabled,
    /// a decoding strategy without validation is used. If enabled, it initializes
    /// with validation based on the provided signature algorithms and a dependency
    /// injection container.
    pub fn new_with_container(
        dep_map: &di::DependencyMap,
        config: JwtConfig,
    ) -> Result<Self, Error> {
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

    /// Decodes and optionally validates a JWT token.
    ///
    /// The token will only be validated if the configuration is set to enabled
    /// in the bootstrap configuration. If validation is not enabled, decoding
    /// will proceed without validation.
    pub fn decode<T: DeserializeOwned>(&self, jwt: &str) -> Result<T, Error> {
        self.decoding_strategy.decode(jwt)
    }
}
