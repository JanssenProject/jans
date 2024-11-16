use super::http_client::{HttpClient, HttpClientError};
use super::{KeyId, TrustedIssuerId};
use crate::common::policy_store::TrustedIssuer;
use jsonwebtoken::{jwk::JwkSet, DecodingKey};
use serde::de::Error;
use serde::Deserialize;
use std::collections::{HashMap, HashSet};
use std::fmt::Debug;
use std::rc::Rc;
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
    store_id: Rc<str>,
    /// Optional issuer associated with the store.
    issuer: Option<Box<str>>,
    /// A map of keys indexed by their `KeyId`.
    keys: HashMap<KeyId, DecodingKey>,
    /// A collection of keys that do not have an associated ID.
    keys_without_id: Vec<DecodingKey>,
    /// The timestamp indicating when the store was last updated.
    last_updated: OffsetDateTime,
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
    /// Creates a JwkStore from JwkSet
    pub fn new_from_jwkset(store_id: Rc<str>, jwks: &JwkSet) -> Result<Self, JwkStoreError> {
        let mut keys = HashMap::new();
        let mut keys_without_id = Vec::new();

        for key in jwks.keys.iter() {
            match &key.common.key_id {
                Some(kid) => {
                    keys.insert(
                        kid.as_str().into(),
                        DecodingKey::from_jwk(&key).map_err(JwkStoreError::DecodeJwk)?,
                    );
                },
                None => {
                    keys_without_id
                        .push(DecodingKey::from_jwk(&key).map_err(JwkStoreError::DecodeJwk)?);
                },
            }
        }

        Ok(JwkStore {
            store_id,
            issuer: None,
            keys,
            keys_without_id,
            last_updated: OffsetDateTime::now_utc(),
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
            .map_err(|e| serde_json::Error::custom(e))?;

        // fetch jwks
        let response = http_client.get(&openid_config.jwks_uri)?;
        let jwks = response
            .json::<JwkSet>()
            .map_err(|e| serde_json::Error::custom(e))?;

        let mut store = Self::new_from_jwkset(store_id, &jwks)?;
        store.issuer = Some(openid_config.issuer.into());

        Ok(store)
    }

    /// Retrieves a Decoding Key from the store
    pub fn get(&self, key_id: &str) -> Option<&DecodingKey> {
        self.keys.get(key_id)
    }

    /// Returns a &Vec of all the keys without a `kid` (Key ID).
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
    #[error("Failed to decode JWKS from string: {0}")]
    Decoding(#[from] serde_json::Error),
    #[error("Failed to make HTTP Request: {0}")]
    Http(#[from] HttpClientError),
    #[error("Failed to make HTTP Request: {0}")]
    DecodeJwk(#[from] jsonwebtoken::errors::Error),
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
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
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

        let jwkset = serde_json::from_value::<JwkSet>(json!({"keys": keys_json.clone()}))
            .expect("Should create JwkSet");

        let mut result =
            JwkStore::new_from_jwkset("test".into(), &jwkset).expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

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
            issuer: None,
            keys: expected_keys,
            keys_without_id: Vec::new(),
            last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
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
    fn can_load_jwk_stores_from_trusted_issuers() {
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
                    "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
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

        let mut result = JwkStore::new_from_trusted_issuer(
            "test".into(),
            &TrustedIssuer {
                name: "Test Trusted Issuer".to_string(),
                description: "This is a test trusted issuer".to_string(),
                openid_configuration_endpoint: format!(
                    "{}/.well-known/openid-configuration",
                    mock_server.url()
                ),
                ..Default::default()
            },
            &http_client,
        )
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
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
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

        let jwkset = serde_json::from_value::<JwkSet>(json!({"keys": keys_json.clone()}))
            .expect("Should create JwkSet");

        let mut result =
            JwkStore::new_from_jwkset("test".into(), &jwkset).expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

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
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
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

        let jwkset = serde_json::from_value::<JwkSet>(json!({"keys": keys_json.clone()}))
            .expect("Should create JwkSet");

        let mut result =
            JwkStore::new_from_jwkset("test".into(), &jwkset).expect("Should create JwkStore");
        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        result.last_updated = OffsetDateTime::from_unix_timestamp(0).unwrap();

        assert_eq!(result.get_keys().len(), 2, "Expected 2 keys");
    }
}
