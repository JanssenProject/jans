use jsonwebtoken as jwt;
use jsonwebtoken::DecodingKey;

/// Custom error type for `KeyService` operations.
#[derive(thiserror::Error, Debug)]
#[non_exhaustive]
pub enum KeyServiceError {
    #[error("Item with the given key was not found: {0}")]
    KeyNotFound(Box<str>),

    #[error("A key with the same id already exists")]
    KeyAlreadyExists,

    #[error("Key could not be decoded: {0}")]
    DecodingError(jwt::errors::Error),

    #[error("Could not parse jwk: {0}")]
    ParsingError(jwt::errors::Error),
}

/// Trait for a service that manages cryptographic keys used for JWT operations.
pub trait KeyService: Send + Sync {
    /// Retrieves the decoding key associated with the specified key identifier (`kid`).
    fn get_key(&self, kid: &str) -> Result<DecodingKey, KeyServiceError>;
}
