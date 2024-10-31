/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::decoding_strategy::key_service;
use jsonwebtoken as jwt;

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
// clippy will complain a lot here to the point that we would have to name each variant to
// `Access`, `Id`, and `Userinfo` which is pretty unhelpful when reading the errors from
// a debug string
#[allow(clippy::enum_variant_names)]
pub enum JwtDecodingError {
    /// Error indicating that the provided access token is invalid.
    ///
    /// This occurs when the given access token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `access_token` is invalid or has failed validation: {0}")]
    InvalidAccessToken(#[source] TokenValidationError),
    /// Error indicating that the provided id token is invalid.
    ///
    /// This occurs when the given id token fails validation, such as when
    /// it has an invali d signature, has expired, or contains incorrect claims.
    #[error("The `id_token` is invalid or has failed validation: {0}")]
    InvalidIdToken(#[source] TokenValidationError),

    /// Error indicating that the provided userinfo token is invalid.
    ///
    /// This occurs when the given userinfo token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `userinfo_token` is invalid or has failed validation: {0}")]
    InvalidUserinfoToken(#[source] TokenValidationError),
}

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
pub enum TokenValidationError {
    /// Indicates an error from the key service when retrieving or handling keys.
    ///
    /// This occurs when the `KeyService` fails to retrieve a required key or encounters
    /// another key-related issue during JWT validation.
    #[error("Key service error: {0}")]
    KeyService(#[from] key_service::Error),

    /// Indicates an error encountered while parsing the JWT.
    ///
    /// This variant occurs when the provided JWT cannot be properly parsed.
    #[error("Error parsing the JWT: {0}")]
    Parsing(#[source] jwt::errors::Error),

    /// Indicates that a required header is missing from the JWT.
    ///
    /// The JWT specification requires certain headers (e.g., `kid` for key identification).
    /// This error is returned if any of those required headers are missing.
    #[error("The JWT is missing a required header: {0}")]
    MissingRequiredHeader(String),

    /// Indicates that the token was signed with an unsupported algorithm.
    ///
    /// This occurs when the JWT header specifies an algorithm that is not supported
    /// by the current validation configuration.
    #[error("The JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jwt::Algorithm),

    /// Indicates that the token failed the validation
    #[error("Token validation failed: {0}")]
    Validation(#[source] jwt::errors::Error),
}
