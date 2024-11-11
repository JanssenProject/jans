/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::key_service;
use jsonwebtoken as jwt;

/// Error type for JWT decoding_strategy operations.
///
/// This enum defines errors that can occur during the parsing, validation,
/// and processing of JWTs, including issues with the key service and unsupported
/// algorithms.
#[derive(thiserror::Error, Debug)]
pub enum JwtDecodingError {
    /// Error encountered while parsing the JWT.
    ///
    /// This error occurs when the provided JWT cannot be properly parsed,
    /// possibly due to malformed structure or invalid encoding.
    #[error("Error parsing the JWT: {0}")]
    Parsing(#[source] jwt::errors::Error),

    /// Missing required `kid` header in the JWT.
    ///
    /// The `kid` (Key ID) header is essential for identifying the correct key
    /// for JWT validation. Handling of JWTs without a `kid` is currently unsupported.
    #[error("The JWT is missing a required `kid` header: `kid`")]
    JwtMissingKeyId,

    /// Token signed with an unsupported algorithm.
    ///
    /// This error occurs when the JWT specifies a signing algorithm that is not
    /// supported by the current validation configuration, making it invalid.
    #[error("The JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jwt::Algorithm),

    /// Token failed validation.
    ///
    /// This error indicates that the token did not meet the necessary validation
    /// criteria, which may involve signature verification, claim checks, or other
    /// validation requirements.
    #[error("Token validation failed: {0}")]
    Validation(#[source] jwt::errors::Error),

    /// Error encountered in the Key Service.
    ///
    /// This error is returned when an issue arises with the Key Service, which
    /// manages keys for signing and verifying JWTs.
    #[error("There was an error with the Key Service: {0}")]
    KeyService(#[from] key_service::KeyServiceError),
}
