// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::{Arc, RwLock};

use crate::LogWriter;
use crate::common::issuer_utils::IssClaim;
use crate::jwt::log_entry::JwtLogEntry;
use crate::log::Logger;

use super::http_utils::{GetFromUrl, HttpError, OpenIdConfig};
use jsonwebtoken::jwk::{Jwk, KeyAlgorithm};
use jsonwebtoken::{Algorithm, DecodingKey};
use serde::Deserialize;

#[derive(Debug, Hash, Eq, PartialEq)]
pub(crate) struct DecodingKeyInfo {
    pub issuer: Option<IssClaim>,
    pub kid: Option<String>,
    pub algorithm: Algorithm,
}

const MUTEX_POISONED_ERR: &str =
    "KeyService RwLock poisoned due to another thread panicking while holding the lock";

/// Manages JSON Web Keys (JWKs) used for decoding JWTs.
///
/// This structure is thread-safe.
///
/// ## Key Rotation
///
/// JWKS are automatically refreshed via a per-issuer background task that
/// periodically re-fetches keys from the issuer's `jwks_uri`. The refresh
/// interval is driven by (in priority order):
///
/// 1. `CEDARLING_JWKS_REFRESH_INTERVAL` bootstrap property
/// 2. `Cache-Control: max-age` from the JWKS HTTP response
/// 3. Hardcoded fallback of 3600 seconds (1 hour)
///
/// Additionally, an on-demand refresh is triggered when a token arrives with
/// an unknown `kid`. This signal is rate-limited per issuer
/// (`CEDARLING_JWKS_REFRESH_MIN_INTERVAL`, default 30 s) to prevent abuse.
#[derive(Default)]
pub(super) struct KeyService {
    keys: RwLock<HashMap<DecodingKeyInfo, Arc<DecodingKey>>>,
}

impl KeyService {
    pub(super) fn new() -> Self {
        KeyService::default()
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
    pub(super) fn insert_keys_from_str(&self, key_stores: &str) -> Result<(), KeyServiceError> {
        let parsed_stores = serde_json::from_str::<HashMap<String, Vec<Jwk>>>(key_stores)
            .map_err(InsertKeysError::DeserializeJwkStores)?;

        let mut keys_guard = self.keys.write().expect(MUTEX_POISONED_ERR);

        for (issuer, keys) in parsed_stores
            .into_iter()
            .map(|(iss, keys)| (IssClaim::new(&iss), keys))
        {
            keys_guard.retain(|key_info, _| key_info.issuer.as_ref() != Some(&issuer));

            for jwk in keys {
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
                keys_guard.insert(key_info, Arc::new(decoding_key));
            }
        }

        Ok(())
    }

    pub(super) async fn get_keys_using_oidc(
        &self,
        openid_config: &OpenIdConfig,
        logger: Option<&Logger>,
    ) -> Result<(), KeyServiceError> {
        let jwks = JwkSet::get_from_url(&openid_config.jwks_uri)
            .await
            .map_err(KeyServiceError::GetJwks)?;

        self.insert_jwk_set(openid_config, logger, jwks)?;

        Ok(())
    }

    fn insert_jwk_set(
        &self,
        openid_config: &OpenIdConfig,
        logger: Option<&Logger>,
        jwks: JwkSet,
    ) -> Result<(), KeyServiceError> {
        let (keys, errs) = jwks.unwrap_keys();
        for err in errs {
            let err_msg = format!(
                "failed to deserialize a JWK from '{}': {}",
                openid_config.issuer.as_str(),
                err,
            );
            logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::WARN)));
        }

        let mut keys_guard = self.keys.write().expect(MUTEX_POISONED_ERR);

        keys_guard.retain(|key_info, _| key_info.issuer.as_ref() != Some(&openid_config.issuer));
        for parsed_key in keys {
            let key = parsed_key.jwk;

            // We will no support keys with unspecified algorithms
            let Some(key_algorithm) = key.common.key_algorithm else {
                let err_msg = format!(
                    "skipping a JWK with a missing algorithm specifier from '{}'",
                    openid_config.issuer.as_str(),
                );
                logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::ERROR)));
                continue;
            };

            // We need to cast from `jsonwebtoken::jwk::KeyAlgorithm` into
            // `jsonwebtoken::Algorithm`
            let Ok(algorithm) = cast_to_algorithm(key_algorithm) else {
                let alg_display = parsed_key.raw_algorithm.as_deref().unwrap_or("UNKNOWN");
                let err_msg = format!(
                    "skipping building a validation key for unsupported algorithm '{}' from '{}'",
                    alg_display,
                    openid_config.issuer.as_str(),
                );
                logger.log_any(JwtLogEntry::new(err_msg, Some(crate::LogLevel::WARN)));
                continue;
            };

            let decoding_key =
                DecodingKey::from_jwk(&key).map_err(FetchKeysError::BuildDecodingKey)?;

            let key_info = DecodingKeyInfo {
                issuer: Some(openid_config.issuer.clone()),
                kid: key.common.key_id,
                algorithm,
            };
            keys_guard.insert(key_info, Arc::new(decoding_key));
        }

        Ok(())
    }

    /// Re-fetches JWKS from the issuer's `jwks_uri`, inserts new keys, and
    /// returns the `Cache-Control: max-age` value from the HTTP response
    /// (if present) for use as the next refresh interval.
    pub(super) async fn refresh_keys_using_oidc(
        &self,
        openid_config: &OpenIdConfig,
        logger: Option<&Logger>,
    ) -> Result<Option<u64>, KeyServiceError> {
        let (jwks, max_age) = JwkSet::get_from_url_with_max_age(&openid_config.jwks_uri)
            .await
            .map_err(KeyServiceError::GetJwks)?;

        self.insert_jwk_set(openid_config, logger, jwks)?;

        Ok(max_age)
    }

    pub(super) fn get_key(&self, key_info: &DecodingKeyInfo) -> Option<Arc<DecodingKey>> {
        self.keys
            .read()
            .expect(MUTEX_POISONED_ERR)
            .get(key_info)
            .cloned()
    }

    pub(super) fn has_keys(&self) -> bool {
        !self.keys.read().expect(MUTEX_POISONED_ERR).is_empty()
    }
}

/// An alternative implementation of [`jsonwebtoken::jwk::JwkSet`].
///
/// This struct allows us to iterate over each item in the [`JwkSet`] and handle deserializing
/// each one independently.
#[derive(Deserialize)]
pub(super) struct JwkSet {
    keys: Vec<serde_json::Value>,
}

impl JwkSet {
    fn unwrap_keys(self) -> (Vec<ParsedJwk>, Vec<serde_json::Error>) {
        let mut keys = Vec::new();
        let mut errs = Vec::new();

        for raw_key in self.keys {
            let raw_algorithm = raw_key
                .get("alg")
                .and_then(|v| v.as_str())
                .map(str::to_owned);

            match serde_json::from_value::<Jwk>(raw_key) {
                Ok(jwk) => keys.push(ParsedJwk { jwk, raw_algorithm }),
                Err(err) => errs.push(err),
            }
        }

        (keys, errs)
    }
}

struct ParsedJwk {
    jwk: Jwk,
    raw_algorithm: Option<String>,
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
    use crate::jwt::test_utils::{MockServer, generate_jwks, generate_keypair_hs256};
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
        let key_service = KeyService::default();

        key_service
            .insert_keys_from_str(&key_stores.to_string())
            .expect("insert keys");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(IssClaim::new(&iss1)),
                    kid: Some(kid1.clone()),
                    algorithm: Algorithm::RS256,
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(IssClaim::new(&iss2)),
                    kid: Some(kid2.clone()),
                    algorithm: Algorithm::RS256,
                })
                .is_some(),
            "Expected to find a key"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(IssClaim::new("some_unknown_iss")),
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

        let key_service = KeyService::default();

        key_service
            .get_keys_using_oidc(&server1.openid_config(), None)
            .await
            .expect("fetch keys for issuer 1");
        key_service
            .get_keys_using_oidc(&server2.openid_config(), None)
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
                    issuer: Some(IssClaim::new("http://some_unknown_issuer.com")),
                    kid: None,
                    algorithm: Algorithm::HS256,
                })
                .is_none(),
            "expected to not find a key from an unknown issuer"
        );
    }

    #[tokio::test]
    async fn refresh_returns_max_age_from_cache_control_header() {
        let keys = generate_keypair_hs256(Some("cache_test_key")).unwrap();
        let mut server = mockito::Server::new_async().await;

        let jwks_body =
            json!({"keys": generate_jwks(std::slice::from_ref(&keys)).keys}).to_string();

        server
            .mock("GET", "/jwks")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_header("cache-control", "public, max-age=300")
            .with_body(&jwks_body)
            .expect(1)
            .create();

        let openid_config = OpenIdConfig {
            issuer: IssClaim::new(&server.url()),
            jwks_uri: url::Url::parse(&(server.url() + "/jwks")).unwrap(),
            status_list_endpoint: None,
        };

        let key_service = KeyService::default();
        let max_age = key_service
            .refresh_keys_using_oidc(&openid_config, None)
            .await
            .expect("refresh with cache-control header should succeed");

        assert_eq!(
            max_age,
            Some(300),
            "should parse max-age=300 from Cache-Control header"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(IssClaim::new(&server.url())),
                    kid: Some("cache_test_key".to_string()),
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "key should be loaded via refresh"
        );
    }

    #[tokio::test]
    async fn refresh_returns_none_when_cache_control_header_missing() {
        let keys = generate_keypair_hs256(Some("cache_test_key")).unwrap();
        let mut server = mockito::Server::new_async().await;

        let jwks_body =
            json!({"keys": generate_jwks(std::slice::from_ref(&keys)).keys}).to_string();

        server
            .mock("GET", "/jwks")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(&jwks_body)
            .expect(1)
            .create();

        let openid_config = OpenIdConfig {
            issuer: IssClaim::new(&server.url()),
            jwks_uri: url::Url::parse(&(server.url() + "/jwks")).unwrap(),
            status_list_endpoint: None,
        };

        let key_service = KeyService::default();
        let max_age = key_service
            .refresh_keys_using_oidc(&openid_config, None)
            .await
            .expect("refresh without cache-control header should succeed");

        assert_eq!(
            max_age, None,
            "max_age should be None when Cache-Control header is missing"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(IssClaim::new(&server.url())),
                    kid: Some("cache_test_key".to_string()),
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "key should be loaded via refresh"
        );
    }

    #[tokio::test]
    async fn refresh_removes_old_keys_after_rotation() {
        let mut server = MockServer::new_with_defaults().await.unwrap();
        let (_, initial_kid) = server.jwt_decoding_key_and_id().unwrap();

        let openid_config = server.openid_config();
        let key_service = KeyService::default();

        key_service
            .get_keys_using_oidc(&openid_config, None)
            .await
            .expect("initial key fetch should succeed");

        let initial_kid = initial_kid.unwrap();
        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server.issuer()),
                    kid: Some(initial_kid.clone()),
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "initial key should be present"
        );

        server
            .rotate_signing_key_hs256("rotated_key")
            .expect("should rotate signing key");

        key_service
            .refresh_keys_using_oidc(&server.openid_config(), None)
            .await
            .expect("refresh should succeed");

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server.issuer()),
                    kid: Some(initial_kid),
                    algorithm: Algorithm::HS256,
                })
                .is_none(),
            "old key should be removed after refresh"
        );

        assert!(
            key_service
                .get_key(&DecodingKeyInfo {
                    issuer: Some(server.issuer()),
                    kid: Some("rotated_key".to_string()),
                    algorithm: Algorithm::HS256,
                })
                .is_some(),
            "new key should be present after refresh"
        );
    }
}
