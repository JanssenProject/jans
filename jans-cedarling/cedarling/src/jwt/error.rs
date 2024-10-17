/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Failed to parse the JWT.
    #[error("error parsing the JWT: {0}")]
    ParsingError(#[source] jsonwebtoken::errors::Error),

    /// Key could not be retrieved from the `KeyService`.
    #[error("could not get hold of a key from the KeyService: {0}")]
    MissingKey(String),

    /// Tried to validate a token signed with an unsupported algorithm.
    #[error("the JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jsonwebtoken::Algorithm),

    /// The JWT is missing a `kid` header.
    #[error("the JWT is missing a required header: {0}")]
    MissingRequiredHeader(String),

    /// Failed to validate the JWT
    #[error("failed to validate JWT: {0}")]
    ValidationError(#[source] jsonwebtoken::errors::Error),
}

/// Error type for the creating JWT service.
//
// This structure is necessary to return only the errors specific to a certain scenario,
// rather than all possible errors that could occur in the module.
#[derive(thiserror::Error, Debug)]
pub enum CreateJwtServiceError {
    /// Config contains an unimplemented algorithm
    #[error("an algorithim defined in the config is not yet implemented: {0}")]
    UnimplementedAlgorithm(String),
}
