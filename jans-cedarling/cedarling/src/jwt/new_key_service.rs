// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use jsonwebtoken::jwk::{Jwk, JwkSet, KeyAlgorithm};
use jsonwebtoken::{Algorithm, DecodingKey};
use reqwest::Client;
use serde::{Deserialize, Deserializer, de};
use url::Url;

use crate::common::policy_store::TrustedIssuer;

#[derive(Debug, Hash, Eq, PartialEq)]
struct DecodingKeyInfo {
    issuer: String,
    kid: Option<String>,
    algorithm: Option<Algorithm>,
}

#[derive(Deserialize)]
struct OpenIdConfig {
    issuer: String,
    #[serde(deserialize_with = "deserialize_url")]
    jwks_uri: Url,
}

pub fn deserialize_url<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = String::deserialize(deserializer)?;
    let url = Url::parse(&url_str)
        .map_err(|e| de::Error::custom(format!("invalid url '{url_str}': {e}")))?;
    Ok(url)
}

/// Manages JSON Web Keys (JWKs) used for decoding JWTs.
// TODO: some IDPs might rotate their keys so we need to figure out a way to keep
// the keys updated.
#[derive(Default)]
pub struct NewKeyService {
    keys: HashMap<DecodingKeyInfo, DecodingKey>,
}

impl NewKeyService {
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
    pub fn insert_keys_from_str(&mut self, key_stores: &str) -> Result<(), InsertKeysError> {
        let parsed_stores = serde_json::from_str::<HashMap<String, Vec<Jwk>>>(key_stores)?;

        for (issuer, keys) in parsed_stores.into_iter() {
            for jwk in keys.into_iter() {
                let decoding_key = DecodingKey::from_jwk(&jwk)?;
                let algorithm =
                    get_algorithm(&jwk).map_err(InsertKeysError::UnsupportedKeyAlgorithm)?;
                let key_info = DecodingKeyInfo {
                    issuer: issuer.clone(),
                    kid: jwk.common.key_id,
                    algorithm,
                };
                self.keys.insert(key_info, decoding_key);
            }
        }

        Ok(())
    }

    pub async fn fetch_keys_for_iss(&mut self, iss: &TrustedIssuer) -> Result<(), FetchKeysError> {
        let client = Client::new();

        let openid_config = client
            .get(iss.oidc_endpoint.as_str())
            .send()
            .await
            .map_err(FetchKeysError::GetOpenIdConfig)?
            .error_for_status()
            .map_err(FetchKeysError::GetOpenIdConfig)?
            .json::<OpenIdConfig>()
            .await
            .map_err(FetchKeysError::DeserializeOpenIdConfig)?;

        let jwks = client
            .get(openid_config.jwks_uri)
            .send()
            .await
            .map_err(FetchKeysError::GetJwks)?
            .error_for_status()
            .map_err(FetchKeysError::GetJwks)?
            .json::<JwkSet>()
            .await
            .map_err(FetchKeysError::DeserializeJwks)?;

        for jwk in jwks.keys.into_iter() {
            let key = DecodingKey::from_jwk(&jwk)?;
            let algorithm = get_algorithm(&jwk).map_err(FetchKeysError::UnsupportedKeyAlgorithm)?;
            let key_info = DecodingKeyInfo {
                issuer: openid_config.issuer.clone(),
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

/// Errors encountered while inserting keys using strings
#[derive(thiserror::Error, Debug)]
pub enum InsertKeysError {
    #[error("failed to deserialize string into JWK stores: {0}")]
    DeserializeJwkStores(#[from] serde_json::Error),
    #[error("unsupported key algorithm: {0}")]
    UnsupportedKeyAlgorithm(KeyAlgorithm),
    #[error("failed to build decoding key: {0}")]
    BuildDecodingKey(#[from] jsonwebtoken::errors::Error),
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
        let mut key_service = NewKeyService::default();

        key_service
            .insert_keys_from_str(&key_stores.to_string())
            .expect("insert keys");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: iss1,
                    kid: Some(kid1),
                    algorithm: Some(Algorithm::RS256),
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: iss2,
                    kid: Some(kid2),
                    algorithm: Some(Algorithm::RS256),
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: "some_unknown_iss".to_string(),
                    kid: Some("abc123".to_string()),
                    algorithm: Some(Algorithm::RS256),
                })
                .is_none(),
            "Expected to not find a key"
        );
    }

    #[tokio::test]
    async fn can_load_jwk_stores_from_multiple_trusted_issuers() {
        let server1 = MockServer::new_with_defaults().await.unwrap();
        let iss1 = server1.trusted_issuer();
        let (_key1, kid1) = server1.jwt_decoding_key_and_id().unwrap();

        let server2 = MockServer::new_with_defaults().await.unwrap();
        let iss2 = server2.trusted_issuer();
        let (_key2, kid2) = server2.jwt_decoding_key_and_id().unwrap();

        let mut key_service = NewKeyService::default();

        key_service
            .fetch_keys_for_iss(&iss1)
            .await
            .expect("fetch keys for issuer 1");
        key_service
            .fetch_keys_for_iss(&iss2)
            .await
            .expect("fetch keys for issuer 2");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: server1.issuer(),
                    kid: kid1,
                    algorithm: Some(Algorithm::HS256)
                })
                .is_some(),
            "expected to find a key from issuer 1"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: server2.issuer(),
                    kid: kid2.clone(),
                    algorithm: Some(Algorithm::HS256)
                })
                .is_some(),
            "expected to find a key from issuer 2"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: server2.issuer(),
                    kid: kid2,
                    algorithm: Some(Algorithm::RS256)
                })
                .is_none(),
            "expected not to find a key with a different algorithm"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: "http://some_unknown_issuer.com".into(),
                    kid: None,
                    algorithm: Some(Algorithm::HS256)
                })
                .is_none(),
            "expected to not find a key from an unknown issuer"
        );
    }
}
