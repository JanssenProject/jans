use super::key_service;
use jsonwebtoken as jwt;

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Indicates an error encountered while parsing the JWT.
    ///
    /// This variant occurs when the provided JWT cannot be properly parsed.
    #[error("Error parsing the JWT: {0}")]
    Parsing(#[source] jwt::errors::Error),

    /// Indicates that a required header is missing from the JWK.
    ///
    /// The `kid` header is used to identify which key should be used to validate
    /// Json Web Tokens. Currently, handling Json Web Keys
    /// without a `kid` is not supported.
    #[error("The JWK is missing a required header: {0}")]
    JwkMissingRequiredHeader(String),

    /// Indicates that the token was signed with an unsupported algorithm.
    ///
    /// This occurs when the JWT header specifies an algorithm that is not supported
    /// by the current validation configuration.
    #[error("The JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jwt::Algorithm),

    /// Indicates that the token failed the validation
    #[error("Token validation failed: {0}")]
    Validation(#[source] jwt::errors::Error),

    #[error("There was an error with the Key Service: {0}")]
    KeyService(#[from] key_service::Error),
}
