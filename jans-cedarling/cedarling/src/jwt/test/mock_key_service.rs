/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use super::super::{traits::GetKey, Error};
use jsonwebtoken::{jwk::JwkSet, DecodingKey};

/// A mock implementation of the `KeyService` trait for testing purposes.
pub struct MockKeyService {
    keys: HashMap<Box<str>, DecodingKey>,
}

impl MockKeyService {
    /// Creates a new `MockKeyService` from a JSON string representation of JWKS.
    pub fn new_from_str(jwks_str: &str) -> Self {
        let jwks: JwkSet = serde_json::from_str(jwks_str).expect("failed to parse JWKS string");
        let mut keys = HashMap::new();
        for k in jwks.keys {
            let decoding_key = DecodingKey::from_jwk(&k).unwrap();
            keys.insert(k.common.key_id.unwrap().into(), decoding_key);
        }
        Self { keys }
    }
}

impl GetKey for MockKeyService {
    /// Retrieves a key from the JWKS using its key ID (kid).
    fn get_key(&self, kid: &str) -> Result<&DecodingKey, Error> {
        let key = self.keys.get(kid).ok_or(Error::MissingKey(kid.into()))?;
        Ok(key)
    }
}
