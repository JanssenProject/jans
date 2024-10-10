use jsonwebtoken as jwt;
use jsonwebtoken::DecodingKey;

/// Error type for JWT decoding
#[derive(thiserror::Error, Debug)]
#[non_exhaustive]
pub enum KeyServiceError {
    /// Key is not in the storage
    #[error("Item with the given key was not found: {0}")]
    KeyNotFound(Box<str>),

    #[error("A key with the same id already exists")]
    KeyAlreadyExists,

    #[error("Key could not be decoded: {0}")]
    DecodingError(jwt::errors::Error),

    #[error("Could not parse jwk: {0}")]
    ParsingError(jwt::errors::Error),
}

#[allow(dead_code)]
pub trait KeyService: Send + Sync {
    fn get_key(&self, kid: &str) -> Result<DecodingKey, KeyServiceError>;
}
