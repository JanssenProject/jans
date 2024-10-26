/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::decoding_strategy::key_service;

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Indicates an error encountered while parsing the JWT.
    ///
    /// This variant occurs when the provided JWT cannot be properly parsed.
    #[error("Error parsing the JWT: {0}")]
    Parsing(#[source] jsonwebtoken::errors::Error),

    /// Indicates that the token was signed with an unsupported algorithm.
    ///
    /// This occurs when the JWT header specifies an algorithm that is not supported
    /// by the current validation configuration.
    #[error("The JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jsonwebtoken::Algorithm),

    /// Indicates that a required header is missing from the JWT.
    ///
    /// The JWT specification requires certain headers (e.g., `kid` for key identification).
    /// This error is returned if any of those required headers are missing.
    #[error("The JWT is missing a required header: {0}")]
    MissingRequiredHeader(String),

    /// Indicates failure during the validation of the JWT.
    ///
    /// This occurs when JWT validation fails due to issues such as an invalid signature,
    /// claim validation failure, or other validation errors.
    #[error("Failed to validate the JWT: {0}")]
    Validation(String),

    /// Indicates an unsupported algorithm defined in the configuration.
    ///
    /// This variant indicates that the configuration specifies an algorithm that
    /// is not yet implemented or recognized by the JWT service.
    #[error("An algorithm defined in the configuration is not yet implemented: {0}")]
    UnimplementedAlgorithm(Box<str>),

    /// Indicates an error from the key service when retrieving or handling keys.
    ///
    /// This occurs when the `KeyService` fails to retrieve a required key or encounters
    /// another key-related issue during JWT validation.
    #[error("Key service error: {0}")]
    KeyService(#[from] key_service::Error),

    /// Error indicating that the provided access token is invalid.
    ///
    /// This occurs when the given access token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `access_token` is invalid or has failed validation: {0}")]
    InvalidAccessToken(#[source] Box<Self>),

    /// Error indicating that the provided id token is invalid.
    ///
    /// This occurs when the given id token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `id_token` is invalid or has failed validation: {0}")]
    InvalidIdToken(#[source] Box<Self>),

    /// Error indicating that the provided userinfo token is invalid.
    ///
    /// This occurs when the given userinfo token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `userinfo_token` is invalid or has failed validation: {0}")]
    InvalidUserinfoToken(#[source] Box<Self>),
}
