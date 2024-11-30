use super::http_client::{HttpClient, HttpClientError};
use super::{KeyId, TrustedIssuerId};
use crate::common::policy_store::TrustedIssuer;
use jsonwebtoken::jwk::Jwk;
use jsonwebtoken::DecodingKey;
use serde::Deserialize;
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::fmt::Debug;
use std::sync::Arc;
use time::OffsetDateTime;

#[derive(Deserialize)]
struct OpenIdConfig {
    issuer: String,
    jwks_uri: String,
}

/// Represents a store of JSON Web Keys (JWK) used for decoding tokens.
///
/// This store maintains a collection of keys identified by a unique `KeyId`.
/// It also supports keys without an identifier for situations where a key ID is
/// not provided.
pub struct JwkStore {
    /// A unique identifier for the store.
    store_id: Arc<str>,
    /// The issuer response from the IDP.
    issuer: Option<Box<str>>,
    /// A map of keys indexed by their `KeyId`.
    keys: HashMap<KeyId, DecodingKey>,
    /// A collection of keys that do not have an associated ID.
    keys_without_id: Vec<DecodingKey>,
    /// The timestamp indicating when the store was last updated.
    last_updated: OffsetDateTime,
    /// From which TrustedIssuer this struct was built (if applicable).
    source_iss: Option<TrustedIssuer>,
}

// We cannot derive from Debug directly because DecodingKey does not implement Debug.
impl Debug for JwkStore {
    /// Formats the `JwkStore` for debugging purposes.
    ///
    /// This implementation displays the store's `store_id`, optional `issuer`,
    /// the list of `KeyId`s for stored keys, the count of keys without an ID,
    /// and the `last_updated` timestamp.
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("JwkStore")
            .field("store_id", &self.store_id)
            .field("issuer", &self.issuer)
            .field("keys", &self.keys.keys())
            .field("keys_without_id_count", &self.keys_without_id.len())
            .field("last_updated", &self.last_updated)
            .finish()
    }
}

// We cannot derive from PartialEq directly because DecodingKey does not implement
// PartialEq.
impl PartialEq for JwkStore {
    /// Compares two `JwkStore` instances for equality.
    ///
    /// Two `JwkStore` instances are considered equal if:
    /// - Their `store_id` values are the same.
    /// - Their `issuer` values are the same.
    /// - The set of key IDs in the `keys` map are the same.
    /// - The length of the `keys_without_id` collections are the same.
    /// - Their `last_updated` timestamps are the same.
    fn eq(&self, other: &Self) -> bool {
        self.store_id == other.store_id
            && self.issuer == other.issuer
            && self.keys.keys().collect::<HashSet<_>>() == other.keys.keys().collect::<HashSet<_>>()
            && self.keys_without_id.len() == other.keys_without_id.len()
            && self.last_updated == other.last_updated
    }
}

impl JwkStore {
    /// Creates a JwkStore from a [`serde_json::Value`]
    pub fn new_from_jwks_value(store_id: Arc<str>, jwks: Value) -> Result<Self, JwkStoreError> {
        let jwks = serde_json::from_value::<IntermediateJwks>(jwks)?;
        Self::new_from_jwks(store_id, jwks)
    }
    /// Creates a JwkStore from a [`String`]
    pub fn new_from_jwks_str(store_id: Arc<str>, jwks: &str) -> Result<Self, JwkStoreError> {
        let jwks = serde_json::from_str::<IntermediateJwks>(jwks)?;
        Self::new_from_jwks(store_id, jwks)
    }

    /// Creates a JwkStore from an [`IntermediateJwks`]
    fn new_from_jwks(store_id: Arc<str>, jwks: IntermediateJwks) -> Result<Self, JwkStoreError> {
        let mut keys = HashMap::new();
        let mut keys_without_id = Vec::new();

        for key in jwks.keys.into_iter() {
            let kid = key
                .get("kid")
                .map(|kid| serde_json::from_value::<String>(kid.clone()))
                .transpose()?;

            // try to create a key
            let key = match serde_json::from_value::<Jwk>(key) {
                Ok(key) => key,
                Err(e) => {
                    // if the error indicates an unknown variant,
                    // we can safely ignore it.
                    if e.to_string().contains("unknown variant") {
                        // TODO: pass this message to the logger
                        eprintln!(
                            "Encountered a JWK with an unsupported algorithm, ignoring it: {}",
                            e
                        );
                        continue;
                    } else {
                        Err(JwkStoreError::DecodeJwk(e))?
                    }
                },
            };

            match &kid {
                Some(kid) => {
                    keys.insert(
                        kid.as_str().into(),
                        DecodingKey::from_jwk(&key).map_err(JwkStoreError::CreateDecodingKey)?,
                    );
                },
                None => {
                    keys_without_id.push(
                        DecodingKey::from_jwk(&key).map_err(JwkStoreError::CreateDecodingKey)?,
                    );
                },
            }
        }

        Ok(JwkStore {
            store_id,
            issuer: None,
            keys,
            keys_without_id,
            last_updated: OffsetDateTime::now_utc(),
            source_iss: None,
        })
    }

    /// Creates a JwkStore by fetching the keys from the given [`TrustedIssuer`].
    pub fn new_from_trusted_issuer(
        store_id: TrustedIssuerId,
        issuer: &TrustedIssuer,
        http_client: &HttpClient,
    ) -> Result<Self, JwkStoreError> {
        // fetch openid configuration
        let response = http_client.get(&issuer.openid_configuration_endpoint)?;
        let openid_config = response
            .json::<OpenIdConfig>()
            .map_err(JwkStoreError::FetchOpenIdConfig)?;

        // fetch jwks
        let response = http_client.get(&openid_config.jwks_uri)?;

        let jwks = response.text().map_err(JwkStoreError::FetchJwks)?;

        let mut store = Self::new_from_jwks_str(store_id, &jwks)?;
        store.issuer = Some(openid_config.issuer.into());
        store.source_iss = Some(issuer.clone());

        Ok(store)
    }

    /// Returns a reference to the source [`TrustedIssuer`] this struct was built on.
    pub fn source_iss(&self) -> Option<&TrustedIssuer> {
        self.source_iss.as_ref()
    }

    /// Retrieves a Decoding Key from the store
    pub fn get(&self, key_id: &str) -> Option<&DecodingKey> {
        self.keys.get(key_id)
    }

    /// Returns a &Vec of all the keys without a `kid` (Key ID).
    #[allow(dead_code)]
    pub fn get_keys_without_id(&self) -> Vec<&DecodingKey> {
        self.keys_without_id.iter().collect()
    }

    /// Returns a Vec containing a reference to all of the keys.
    pub fn get_keys(&self) -> Vec<&DecodingKey> {
        // PERF: We can cache the returned Vec so it doesn't
        // get created every time this function is called.
        let mut keys = Vec::new();
        self.keys.values().for_each(|key| keys.push(key));
        self.keys_without_id.iter().for_each(|key| keys.push(key));
        keys
    }
}

#[derive(thiserror::Error, Debug)]
pub enum JwkStoreError {
    #[error("Failed to fetch OpenIdConfig remote server: {0}")]
    FetchOpenIdConfig(#[source] reqwest::Error),
    #[error("Failed to fetch JWKS from remote server: {0}")]
    FetchJwks(#[source] reqwest::Error),
    #[error("Failed to make HTTP Request: {0}")]
    Http(#[from] HttpClientError),
    #[error("Failed to create Decoding Key from JWK: {0}")]
    CreateDecodingKey(#[from] jsonwebtoken::errors::Error),
    #[error("Failed to decode JWK: {0}")]
    DecodeJwk(#[from] serde_json::Error),
}

/// A simple struct to deserialize a collection of JWKs (JSON Web Keys).
///
/// This struct holds the raw keys in a vector of `serde_json::Value`, allowing
/// the keys to be processed or validated individually. It is primarily used
/// as an intermediary step for deserialization before iterating over each key
/// to check for errors or unsupported algorithms and skipping any invalid keys.
#[derive(Deserialize)]
struct IntermediateJwks {
    keys: Vec<Value>,
}

#[cfg(test)]
mod test {
    use crate::{
        common::policy_store::TrustedIssuer,
        jwt::{http_client::HttpClient, jwk_store::JwkStore},
    };
    use jsonwebtoken::{jwk::JwkSet, DecodingKey};
    use mockito::Server;
    use serde_json::json;
    use std::{collections::HashMap, time::Duration};
    use time::OffsetDateTime;

    #[test]
    fn can_load_from_jwkset() {
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3";
        let keys_json = json!([
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": kid1,
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": kid2,
            }
        ]);

        let jwks_json = json!({"keys": keys_json});
        let mut result = JwkStore::new_from_jwks_str("test".into(), &jwks_json.to_string())
            .expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        let expected_jwkset =
            serde_json::from_value::<JwkSet>(jwks_json).expect("Should create JwkSet");
        let expected_keys = expected_jwkset
            .keys
            .iter()
            .filter_map(|key| match &key.common.key_id {
                Some(key_id) => Some((
                    key_id.as_str().into(),
                    DecodingKey::from_jwk(key).expect("Should create DecodingKey from Jwk"),
                )),
                None => None,
            })
            .collect::<HashMap<Box<str>, DecodingKey>>();

        let expected = JwkStore {
            store_id: "test".into(),
            issuer: None,
            keys: expected_keys,
            keys_without_id: Vec::new(),
            last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
            source_iss: None,
        };

        assert_eq!(expected, result);

        // Asserts so we can check if using `get` works as expected
        assert!(
            result.get(kid1).is_some(),
            "Expected to find key with id: {kid1}"
        );
        assert!(
            result.get(kid2).is_some(),
            "Expected to find key with id: {kid2}"
        );
        assert!(
            result.get("unknown key id").is_none(),
            "Expected to find None"
        );
    }

    #[test]
    fn can_load_from_trusted_issuers() {
        let mut mock_server = Server::new();

        // Setup OpenId config endpoint
        let openid_config_json = json!({
            "issuer": mock_server.url(),
            "jwks_uri": format!("{}/jwks", mock_server.url())
        });
        let openid_config_endpoint = mock_server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(200)
            .with_body(openid_config_json.to_string())
            .expect(1)
            .create();

        // Setup JWKS endpoint
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3";
        let jwks_json = json!({
            "keys": [
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": kid1,
            },
            {
            "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
            "e": "AQAB",
            "use": "sig",
            "alg": "RS256",
            "kty": "RSA",
            "kid": kid2,
        }

        ]
        });
        let jwks_endpoint = mock_server
            .mock("GET", "/jwks")
            .with_status(200)
            .with_body(jwks_json.to_string())
            .expect(1)
            .create();

        let http_client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient");

        let source_iss = TrustedIssuer {
            name: "Test Trusted Issuer".to_string(),
            description: "This is a test trusted issuer".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                mock_server.url()
            ),
            ..Default::default()
        };

        let mut result =
            JwkStore::new_from_trusted_issuer("test".into(), &source_iss, &http_client)
                .expect("Should load JwkStore from Trusted Issuer");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        let jwkset =
            serde_json::from_value::<JwkSet>(jwks_json).expect("Should create JwkSet from Value");
        let expected_keys = jwkset
            .keys
            .iter()
            .filter_map(|key| match &key.common.key_id {
                Some(key_id) => Some((
                    key_id.as_str().into(),
                    DecodingKey::from_jwk(key).expect("Should create DecodingKey from Jwk"),
                )),
                None => None,
            })
            .collect::<HashMap<Box<str>, DecodingKey>>();
        let expected = JwkStore {
            store_id: "test".into(),
            issuer: Some(mock_server.url().into()),
            keys: expected_keys,
            keys_without_id: Vec::new(),
            last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
            source_iss: Some(source_iss),
        };

        assert_eq!(expected, result);

        // Asserts so we can check if using `get` works as expected
        assert!(
            result.get(kid1).is_some(),
            "Expected to find key with id: {kid1}"
        );
        assert!(
            result.get(kid2).is_some(),
            "Expected to find key with id: {kid2}"
        );
        assert!(
            result.get("unknown key id").is_none(),
            "Expected to find None"
        );

        // Assert that the mock endpoints only gets called once.
        openid_config_endpoint.assert();
        jwks_endpoint.assert();
    }

    #[test]
    fn can_load_keys_without_ids() {
        let keys_json = json!([
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
            }
        ]);

        let jwks_json = json!({"keys": keys_json.clone()});
        let mut result = JwkStore::new_from_jwks_str("test".into(), &jwks_json.to_string())
            .expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        let jwkset = serde_json::from_value::<JwkSet>(jwks_json).expect("Should create JwkSet");
        let expected_keys = jwkset
            .keys
            .iter()
            .map(|key| DecodingKey::from_jwk(key).expect("Should create DecodingKey from Jwk"))
            .collect::<Vec<DecodingKey>>();

        let expected = JwkStore {
            store_id: "test".into(),
            issuer: None,
            keys: HashMap::new(),
            keys_without_id: expected_keys,
            last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
            source_iss: None,
        };

        assert_eq!(expected, result);
    }

    #[test]
    fn can_get_a_reference_to_all_keys() {
        let keys_json = json!([
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": "some_random_key_id",
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
            }
        ]);

        let jwks_json = json!({"keys": keys_json.clone()});

        let mut result = JwkStore::new_from_jwks_str("test".into(), &jwks_json.to_string())
            .expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        assert_eq!(result.get_keys().len(), 2, "Expected 2 keys");
    }

    #[test]
    fn can_gracefully_handle_unsupported_algs_from_jwks() {
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3";
        let keys_json = json!([
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": kid1,
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": kid2,
            },
            {
                "kty": "EC",
                "use": "sig",
                "key_ops_type": [],
                "crv": "P-521",
                "kid": "connect_190362b7-efca-4674-9cb7-21b428cb682a_sig_es512",
                "x5c": [
                    "MIICBjCCAWegAwIBAgIhALe16fd76pin3igeUTiLhGW01wkEMVzBsmGdXVtYpeZuMAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQxMDE5MTg1NzMyWhcNMjQxMDIxMTk1NzMxWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQAf4TdXH7umWW64g1w8+UZ0NhyRm6rWsRGL7E+bvS2cY+K6UPThM7/xy9nTs73Pw8OT26oUhBz1oM9Jhs0Qy/veXMAvgHuUeIT6CBV3aHr4osWFAnGwoh0pjd1NOU3TN+ms1ttcD1qyJcZxLOhvFr3VZ7/7p5gSOaY1MwEEG2Ka/itQTujJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBjAAwgYgCQgGBq8DEjIF1SwqFos+2mHA6XFO+pZfx9HESd8dUZxN3yA5yf1oFxhUCbviQeOCeATAITuEfSIIL8hAQ4uzQc7JYhgJCAfB8/JGumVAnU/3lx2aHVl8hpSXn/f2107VN4ld46dwy3r48Ioo8dfjN2dH0BOKNg2ddYPiORfrpI9Y/WF7vI4UT"
                ],
                "x": "f4TdXH7umWW64g1w8-UZ0NhyRm6rWsRGL7E-bvS2cY-K6UPThM7_xy9nTs73Pw8OT26oUhBz1oM9Jhs0Qy_veXM",
                "y": "vgHuUeIT6CBV3aHr4osWFAnGwoh0pjd1NOU3TN-ms1ttcD1qyJcZxLOhvFr3VZ7_7p5gSOaY1MwEEG2Ka_itQTs",
                "exp": 2729540651438u64,
                "alg": "ES512"
            }
        ]);
        let jwks_string = json!({"keys": keys_json.clone()}).to_string();

        let mut result = JwkStore::new_from_jwks_str("test".into(), &jwks_string)
            .expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        let expected_jwkset = serde_json::from_value::<JwkSet>(json!({"keys": [
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": kid1,
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": kid2,
            },
        ]}))
            .expect("Should deserialize JWKS");
        let expected_keys = expected_jwkset
            .keys
            .iter()
            .filter_map(|key| match &key.common.key_id {
                Some(key_id) => Some((
                    key_id.as_str().into(),
                    DecodingKey::from_jwk(key).expect("Should create DecodingKey from Jwk"),
                )),
                None => None,
            })
            .collect::<HashMap<Box<str>, DecodingKey>>();

        let expected = JwkStore {
            store_id: "test".into(),
            issuer: None,
            keys: expected_keys,
            keys_without_id: Vec::new(),
            last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
            source_iss: None,
        };

        assert_eq!(expected, result);

        // Asserts so we can check if using `get` works as expected
        assert!(
            result.get(kid1).is_some(),
            "Expected to find key with id: {kid1}"
        );
        assert!(
            result.get(kid2).is_some(),
            "Expected to find key with id: {kid2}"
        );
        assert!(
            result.get("unknown key id").is_none(),
            "Expected to find None"
        );
    }
}
