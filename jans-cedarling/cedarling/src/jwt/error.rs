/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::sync::Arc;

use super::key_service::KeyServiceError;

/// Error type for JWT decoding
#[derive(thiserror::Error, Debug)]
#[non_exhaustive]
pub enum DecodeJwtError {
    /// JWT is malformed
    #[error("Malformed JWT provided")]
    MalformedJWT,

    /// Invalid JWT signature
    #[error("Invalid JWT signature")]
    InvalidSignature,

    /// JWT contains an invalid key
    #[error("Invalid key used in JWT")]
    InvalidKey,

    /// JWT signature expired
    #[error("JWT signature has expired")]
    ExpiredSignature,

    /// JWT contains an invalid issuer
    #[error("Invalid issuer in JWT")]
    InvalidIssuer,

    /// JWT contains an invalid audience
    #[error("Invalid audience in JWT")]
    InvalidAudience,

    /// Unmarshaled JWT payload base64 value is not valid UTF-8
    #[error("Unable to convert decoded base64 JWT value to string: {0}")]
    UnableToString(#[from] std::string::FromUtf8Error),

    /// Base64 value in payload is malformed
    #[error("Unable to decode base64 JWT value: {0}")]
    UnableToDecodeBase64(String),

    /// Unable to parse JSON in unmarshaled JWT payload string value
    #[error("Unable to parse JWT JSON data: {0}")]
    UnableToParseJson(Arc<serde_json::Error>),

    /// Placeholder error for unhandled
    #[error("An unhandled error occurred: {0}")]
    Unhandled(String),

    /// Errors that shouldn't really happen but happened somehow
    #[error("An unexpected error occurred: {0}")]
    Unexpected(String),

    /// Happens when trying to validate a token without a `KeyService`
    #[error("Could not get hold of a key")]
    KeyServiceNotFound,

    /// Happens when a key could not be retrieved from the `KeyService`
    #[error("Could not get hold of a key from the KeyService: {0}")]
    KeyNotFound(#[from] KeyServiceError),

    /// Happens when a key could not be retrieved from the `KeyService`
    #[error("The token is missing the `kid` (Key ID) in its headers")]
    MissingKeyId,

    /// Happens when a key could not be retrieved from the `KeyService`
    #[error("Error validating the token: {0}")]
    ValidationError(String),
}

impl Into<DecodeJwtError> for jsonwebtoken::errors::Error {
    fn into(self) -> DecodeJwtError {
        match self.kind() {
            jsonwebtoken::errors::ErrorKind::InvalidToken => DecodeJwtError::MalformedJWT,
            jsonwebtoken::errors::ErrorKind::InvalidSignature => DecodeJwtError::InvalidSignature,
            jsonwebtoken::errors::ErrorKind::InvalidEcdsaKey => DecodeJwtError::InvalidKey,
            jsonwebtoken::errors::ErrorKind::InvalidRsaKey(_) => DecodeJwtError::InvalidKey,
            jsonwebtoken::errors::ErrorKind::ExpiredSignature => DecodeJwtError::ExpiredSignature,
            jsonwebtoken::errors::ErrorKind::InvalidIssuer => DecodeJwtError::InvalidIssuer,
            jsonwebtoken::errors::ErrorKind::InvalidAudience => DecodeJwtError::InvalidAudience,
            jsonwebtoken::errors::ErrorKind::Base64(decode_error) => {
                DecodeJwtError::UnableToDecodeBase64(decode_error.to_string())
            },

            jsonwebtoken::errors::ErrorKind::Json(arc) => {
                DecodeJwtError::UnableToParseJson(arc.clone())
            },
            jsonwebtoken::errors::ErrorKind::Utf8(err) => {
                DecodeJwtError::UnableToString(err.clone())
            },
            _ => DecodeJwtError::Unexpected(self.to_string()),
        }
    }
}
