/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::{jwk::JwkSet, DecodingKey};

use crate::jwt::{KeyService, KeyServiceError};

/// A mock implementation of the `KeyService` trait for testing purposes.
pub struct MockKeyService {
    jwks: JwkSet,
}

impl MockKeyService {
    /// Creates a new `MockKeyService` from a JSON string representation of JWKS.
    pub fn new_from_str(jwks_str: &str) -> Self {
        let jwks: JwkSet = serde_json::from_str(jwks_str).expect("failed to parse JWKS string");
        Self { jwks }
    }
}

impl KeyService for MockKeyService {
    /// Retrieves a key from the JWKS using its key ID (kid).
    fn get_key(&self, kid: &str) -> Result<DecodingKey, KeyServiceError> {
        let key = self
            .jwks
            .find(&kid)
            .ok_or(KeyServiceError::KeyNotFound(kid.into()))?;

        Ok(DecodingKey::from_jwk(key).unwrap())
    }
}
