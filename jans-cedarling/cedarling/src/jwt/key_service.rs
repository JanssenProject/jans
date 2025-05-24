// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use super::http_utils::*;
use jsonwebtoken::jwk::{Jwk, JwkSet, KeyAlgorithm};
use jsonwebtoken::{Algorithm, DecodingKey};

#[derive(Debug, Hash, Eq, PartialEq)]
pub struct DecodingKeyInfo {
    pub issuer: Option<String>,
    pub kid: Option<String>,
    pub algorithm: Algorithm,
}

/// Manages JSON Web Keys (JWKs) used for decoding JWTs.
///
/// ## TODO
///
/// We still need to figure out a reliable way to handle rotating out expired keys.
///
/// the Jans Auth Server adds a custom `exp` field to the JWK but it's not really
/// a standard approach yet as per [`RFC 7517 v41`] so some IDPs will might not follow
/// the same convention. Thus, we shouldn't rely on it yet for rotating keys.
///
/// A naive first solution might be to try fetching a new key if validation fails
/// but this could be abused if someone just kept sending invalid JWTs.
///
/// [`RFC 7517 v41`]: https://datatracker.ietf.org/doc/html/draft-ietf-jose-json-web-key-41
#[derive(Default)]
pub struct KeyService {
    keys: HashMap<DecodingKeyInfo, DecodingKey>,
}

impl KeyService {
    pub fn new() -> Self {
        Default::default()
    }

    /// Loads JWK stores from a string.
    ///
    /// This enables loading keystores via a local JSON file.
    ///
    /// # JWKS Schema
    ///
    /// The JSON must follow this schema:
    ///
    /// ```txt
    /// {
    ///     "trusted_issuer_id": [ ... ]
    ///     "another_trusted_issuer_id": [ ... ]
    /// }
    /// ```
    ///
    /// - Where keys are `Trusted Issuer IDs` assinged to each key store
    /// - and the values contains the JSON Web Keys as defined in [`RFC 7517`].
    ///
    /// [`RFC 7517`]: https://datatracker.ietf.org/doc/html/rfc7517
    pub fn insert_keys_from_str(&mut self, key_stores: &str) -> Result<(), KeyServiceError> {
        let parsed_stores = serde_json::from_str::<HashMap<String, Vec<Jwk>>>(key_stores)
            .map_err(InsertKeysError::DeserializeJwkStores)?;

        for (issuer, keys) in parsed_stores.into_iter() {
            for jwk in keys.into_iter() {
                let decoding_key =
                    DecodingKey::from_jwk(&jwk).map_err(InsertKeysError::BuildDecodingKey)?;
                let algorithm = get_algorithm(&jwk)
                    .map_err(InsertKeysError::UnsupportedKeyAlgorithm)?
                    .ok_or(InsertKeysError::UnspecifiedAlgorithm)?;
                let key_info = DecodingKeyInfo {
                    issuer: Some(issuer.clone()),
                    kid: jwk.common.key_id,
                    algorithm,
                };
                self.keys.insert(key_info, decoding_key);
            }
        }

        Ok(())
    }

    pub async fn get_keys(&mut self, openid_config: &OpenIdConfig) -> Result<(), KeyServiceError> {
        let jwks = JwkSet::get_from_url(&openid_config.jwks_uri)
            .await
            .map_err(KeyServiceError::GetJwks)?;

        for jwk in jwks.keys.into_iter() {
            let key = DecodingKey::from_jwk(&jwk).map_err(FetchKeysError::BuildDecodingKey)?;
            let algorithm = get_algorithm(&jwk)
                .map_err(FetchKeysError::UnsupportedKeyAlgorithm)?
                .ok_or(FetchKeysError::UnspecifiedAlgorithm)?;
            let key_info = DecodingKeyInfo {
                issuer: Some(openid_config.issuer.clone()),
                kid: jwk.common.key_id,
                algorithm,
            };
            self.keys.insert(key_info, key);
        }

        Ok(())
    }

    pub fn get_key(&self, key_info: &DecodingKeyInfo) -> Option<&DecodingKey> {
        self.keys.get(key_info)
    }

    pub fn has_keys(&self) -> bool {
        !self.keys.is_empty()
    }
}

#[inline]
fn get_algorithm(jwk: &Jwk) -> Result<Option<Algorithm>, KeyAlgorithm> {
    let Some(algorithm) = jwk.common.key_algorithm else {
        return Ok(None);
    };

    let algorithm = match algorithm {
        KeyAlgorithm::HS256 => Algorithm::HS256,
        KeyAlgorithm::HS384 => Algorithm::HS384,
        KeyAlgorithm::HS512 => Algorithm::HS512,
        KeyAlgorithm::ES256 => Algorithm::ES256,
        KeyAlgorithm::ES384 => Algorithm::ES384,
        KeyAlgorithm::RS256 => Algorithm::RS256,
        KeyAlgorithm::RS384 => Algorithm::RS384,
        KeyAlgorithm::RS512 => Algorithm::RS512,
        KeyAlgorithm::PS256 => Algorithm::PS256,
        KeyAlgorithm::PS384 => Algorithm::PS384,
        KeyAlgorithm::PS512 => Algorithm::PS512,
        KeyAlgorithm::EdDSA => Algorithm::EdDSA,
        alg => return Err(alg),
    };

    Ok(Some(algorithm))
}

#[derive(thiserror::Error, Debug)]
pub enum KeyServiceError {
    #[error("failed to retrieve openid configuration: {0}")]
    GetOpenIdConfig(#[source] HttpError),
    #[error("failed to retrieve the JWKS: {0}")]
    GetJwks(#[source] HttpError),
    #[error("failed to insert keys into the KeyService: {0}")]
    InsertKeys(#[from] InsertKeysError),
    #[error("failed to fetch keys for the KeyService: {0}")]
    FetchKeysError(#[from] FetchKeysError),
}

/// Errors encountered while inserting keys using strings
#[derive(thiserror::Error, Debug)]
pub enum InsertKeysError {
    #[error("failed to deserialize string into JWK stores: {0}")]
    DeserializeJwkStores(#[from] serde_json::Error),
    #[error("unsupported key algorithm: {0}")]
    UnsupportedKeyAlgorithm(KeyAlgorithm),
    #[error("failed to build decoding key: {0}")]
    BuildDecodingKey(#[from] jsonwebtoken::errors::Error),
    #[error("the key did not specify it's algorithm")]
    UnspecifiedAlgorithm,
}

/// Errors encountered while fetching keys remotely
#[derive(thiserror::Error, Debug)]
pub enum FetchKeysError {
    #[error("failed to get openid config: {0}")]
    GetOpenIdConfig(#[source] reqwest::Error),
    #[error("failed to deserialize openid config: {0}")]
    DeserializeOpenIdConfig(#[source] reqwest::Error),
    #[error("failed to get JWKS: {0}")]
    GetJwks(#[source] reqwest::Error),
    #[error("failed to deserialize JWKS: {0}")]
    DeserializeJwks(#[source] reqwest::Error),
    #[error("unsupported key algorithm: {0}")]
    UnsupportedKeyAlgorithm(KeyAlgorithm),
    #[error("failed to build decoding key: {0}")]
    BuildDecodingKey(#[from] jsonwebtoken::errors::Error),
    #[error("the key did not specify it's algorithm")]
    UnspecifiedAlgorithm,
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::jwt::test_utils::MockServer;
    use jsonwebtoken::Algorithm;
    use serde_json::json;

    #[test]
    fn can_insert_and_get_keys_from_str() {
        let iss1 = "http://some_issuer.com".to_string();
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee".to_string();
        let iss2 = "http://another_issuer.com".to_string();
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3".to_string();
        let key_stores = json!({
            &iss1: [{
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee"
            }],
            &iss2: [{
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": "73e25f9789119c7875d58087a78ac23f5ef2eda3"
            }],
        });
        let mut key_service = KeyService::default();

        key_service
            .insert_keys_from_str(&key_stores.to_string())
            .expect("insert keys");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(iss1),
                    kid: Some(kid1.clone()),
                    algorithm: Algorithm::RS256,
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(iss2),
                    kid: Some(kid2.clone()),
                    algorithm: Algorithm::RS256,
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some("some_unknown_iss".to_string()),
                    kid: Some(kid1),
                    algorithm: Algorithm::RS256,
                })
                .is_none(),
            "Expected to not find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: None,
                    kid: Some(kid2),
                    algorithm: Algorithm::HS256,
                })
                .is_none(),
            "Expected to not find a key"
        );
    }

    #[tokio::test]
    async fn can_load_jwk_stores_from_multiple_trusted_issuers() {
        let server1 = MockServer::new_with_defaults().await.unwrap();
        let (_key1, kid1) = server1.jwt_decoding_key_and_id().unwrap();

        let server2 = MockServer::new_with_defaults().await.unwrap();
        let (_key2, kid2) = server2.jwt_decoding_key_and_id().unwrap();

        let mut key_service = KeyService::default();

        key_service
            .get_keys(&server1.openid_config())
            .await
            .expect("fetch keys for issuer 1");
        key_service
            .get_keys(&server2.openid_config())
            .await
            .expect("fetch keys for issuer 2");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server1.issuer()),
                    kid: kid1,
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "expected to find a key from issuer 1"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server2.issuer()),
                    kid: kid2.clone(),
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "expected to find a key from issuer 2"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server2.issuer()),
                    kid: kid2,
                    algorithm: Algorithm::RS256,
                })
                .is_none(),
            "expected not to find a key with a different algorithm"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some("http://some_unknown_issuer.com".into()),
                    kid: None,
                    algorithm: Algorithm::HS256,
                })
                .is_none(),
            "expected to not find a key from an unknown issuer"
        );
    }
}
