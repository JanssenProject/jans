use jsonwebtoken::{jwk::JwkSet, DecodingKey};

use crate::jwt::{KeyService, KeyServiceError};

/// An in-memory KV storage for DecodingKeys
pub struct MockKeyService {
    jwks: JwkSet,
}

impl MockKeyService {
    #[allow(dead_code)]
    pub fn new(jwks: JwkSet) -> Self {
        Self { jwks }
    }

    #[allow(dead_code)]
    pub fn new_from_str(jwks_str: &str) -> Self {
        let jwks: JwkSet = serde_json::from_str(jwks_str).expect("failed to parse JWKS string");
        Self { jwks }
    }
}

impl KeyService for MockKeyService {
    /// Gets a key from the store
    fn get_key(&self, kid: &str) -> Result<DecodingKey, KeyServiceError> {
        let key = self
            .jwks
            .find(&kid)
            .ok_or(KeyServiceError::KeyNotFound(kid.into()))?;

        Ok(DecodingKey::from_jwk(key).map_err(|e| KeyServiceError::DecodingError(e))?)
    }
}
