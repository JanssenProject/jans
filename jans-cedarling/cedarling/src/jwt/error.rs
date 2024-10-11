/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::key_service::KeyServiceError;

/// Error type for JWT decoding
#[derive(thiserror::Error, Debug)]
#[non_exhaustive]
pub enum Error {
    /// Failed to parse the JWT
    #[error("error parsing the JWT: {0}")]
    ParsingError(#[source] jsonwebtoken::errors::Error),

    /// Attempted to validate a token, but no `KeyService` was provided
    #[error("`KeyService` is required but was not initialized")]
    KeyServiceNotFound,

    /// Key could not be retrieved from the `KeyService`
    #[error("could not get hold of a key from the KeyService: {0}")]
    MissingKey(#[from] KeyServiceError),

    /// Tried to validate a token signed with an unsupported algorithm
    #[error("the JWT is signed with an unsupported algorithm")]
    UnsupportedAlgorithm,

    /// The JWT is missing a `kid` header
    #[error("the JWT is missing a required header: {0}")]
    MissingRequiredHeader(String),

    /// Errors that shouldn't really happen but happened somehow
    #[error("an unexpected error occurred: {0}")]
    Unexpected(String),

    /// Failed to validate the JWT
    #[error("failed to validate JWT: {0}")]
    ValidationError(#[source] jsonwebtoken::errors::Error),
}
