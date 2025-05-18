// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{DecodeJwtError, key_service::KeyServiceError, validator::JwtValidatorError};

#[derive(Debug, thiserror::Error)]
pub enum JwtProcessingError {
    #[error("Invalid token `{0}`: {1}")]
    InvalidToken(String, JwtValidatorError),
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
    #[error("error while trying to parse issuer from token: {0}")]
    GetIss(#[from] DecodeJwtError),
}

#[derive(Debug, thiserror::Error)]
pub enum JwtServiceInitError {
    #[error(
        "Failed to initialize Key Service for JwtService due to a conflictig config: both a local \
         JWKS and trusted issuers was provided."
    )]
    ConflictingJwksConfig,
    #[error(
        "Failed to initialize Key Service for JwtService due to a missing config: no local JWKS \
         or trusted issuers was provided."
    )]
    MissingJwksConfig,
    #[error("Failed to initialize Key Service: {0}")]
    KeyService(#[from] KeyServiceError),
    #[error("Encountered an unsupported algorithm in the config: {0}")]
    UnsupportedAlgorithm(String),
    #[error("Failed to initialize JwtValidator: {0}")]
    InitJwtValidator(#[from] JwtValidatorError),
    #[error("failed to parse the openid_configuration_endpoint for the trusted issuer `{0}`: {1}")]
    ParseOidcUrl(String, url::ParseError),
}
