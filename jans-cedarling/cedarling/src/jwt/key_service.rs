// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use crate::LogWriter;
use crate::jwt::log_entry::JwtLogEntry;
use crate::log::Logger;

use super::http_utils::*;
use jsonwebtoken::jwk::{Jwk, KeyAlgorithm};
use jsonwebtoken::{Algorithm, DecodingKey};
use serde::Deserialize;

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
                let algorithm = jwk
                    .common
                    .key_algorithm
                    .map(|alg| {
                        cast_to_algorithm(alg).map_err(InsertKeysError::UnsupportedKeyAlgorithm)
                    })
                    .transpose()?
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

    pub async fn get_keys_using_oidc(
        &mut self,
        openid_config: &OpenIdConfig,
        logger: &Option<Logger>,
    ) -> Result<(), KeyServiceError> {
        let jwks = JwkSet::get_from_url(&openid_config.jwks_uri)
            .await
            .map_err(KeyServiceError::GetJwks)?;

        let (keys, errs) = jwks.unwrap_keys();
        for err in errs.into_iter() {
            let err_msg = format!(
                "failed to deserialize a JWK from '{}': {}",
                openid_config.issuer, err,
            );
            logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::WARN)));
            continue;
        }

        for key in keys.into_iter() {
            // We will no support keys with unspecified algorithms
            let Some(key_algorithm) = key.common.key_algorithm else {
                let err_msg = format!(
                    "skipping a JWK with a missing algorithm specifier from '{}'",
                    openid_config.issuer,
                );
                logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::ERROR)));
                continue;
            };

            // We need to cast from `jsonwebtoken::jwk::KeyAlgorithm` into
            // `jsonwebtoken::Algorithm`
            let algorithm = match cast_to_algorithm(key_algorithm) {
                Ok(alg) => alg,
                Err(alg) => {
                    let err_msg = format!(
                        "skipping building a validation key for unsupported algorithm from '{}': {}",
                        openid_config.issuer, alg,
                    );
                    logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::WARN)));
                    continue;
                },
            };

            let decoding_key =
                DecodingKey::from_jwk(&key).map_err(FetchKeysError::BuildDecodingKey)?;

            let key_info = DecodingKeyInfo {
                issuer: Some(openid_config.issuer.clone()),
                kid: key.common.key_id,
                algorithm,
            };
            self.keys.insert(key_info, decoding_key);
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

/// An alternative implementation of [`jsonwebtoken::jwk::JwkSet`].
///
/// This struct allows us to iterate over each in in the JwkSet and handle deserializing
/// each one independently.
#[derive(Deserialize)]
pub struct JwkSet {
    keys: Vec<serde_json::Value>,
}

impl JwkSet {
    pub fn unwrap_keys(self) -> (Vec<Jwk>, Vec<serde_json::Error>) {
        let mut keys = Vec::new();
        let mut errs = Vec::new();

        for key in self.keys.into_iter() {
            let result = serde_json::from_value::<Jwk>(key);
            match result {
                Ok(jwk) => keys.push(jwk),
                Err(err) => errs.push(err),
            }
        }

        (keys, errs)
    }
}

/// Tries to Casts a [`jsonwebtoken::jwk::KeyAlgorithm`] into a [`jsonwebtoken::Algorithm`].
#[inline]
fn cast_to_algorithm(
    key_alg: jsonwebtoken::jwk::KeyAlgorithm,
) -> Result<jsonwebtoken::Algorithm, jsonwebtoken::jwk::KeyAlgorithm> {
    match key_alg {
        KeyAlgorithm::HS256 => Ok(Algorithm::HS256),
        KeyAlgorithm::HS384 => Ok(Algorithm::HS384),
        KeyAlgorithm::HS512 => Ok(Algorithm::HS512),
        KeyAlgorithm::ES256 => Ok(Algorithm::ES256),
        KeyAlgorithm::ES384 => Ok(Algorithm::ES384),
        KeyAlgorithm::RS256 => Ok(Algorithm::RS256),
        KeyAlgorithm::RS384 => Ok(Algorithm::RS384),
        KeyAlgorithm::RS512 => Ok(Algorithm::RS512),
        KeyAlgorithm::PS256 => Ok(Algorithm::PS256),
        KeyAlgorithm::PS384 => Ok(Algorithm::PS384),
        KeyAlgorithm::PS512 => Ok(Algorithm::PS512),
        KeyAlgorithm::EdDSA => Ok(Algorithm::EdDSA),
        key_alg => Err(key_alg),
    }
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
            .get_keys_using_oidc(&server1.openid_config(), &None)
            .await
            .expect("fetch keys for issuer 1");
        key_service
            .get_keys_using_oidc(&server2.openid_config(), &None)
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
