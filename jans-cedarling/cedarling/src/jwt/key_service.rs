// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;

use jsonwebtoken::DecodingKey;
use serde_json::{Value, json};

use super::TrustedIssuerId;
use super::jwk_store::{JwkStore, JwkStoreError};
use crate::common::policy_store::TrustedIssuer;
use crate::http::{HttpClient, HttpClientError};

pub struct DecodingKeyWithIss<'a> {
    /// The decoding key used to validate JWT signatures.
    pub key:     &'a DecodingKey,
    /// The Trusted Issuer where the Key was fetched.
    pub key_iss: Option<&'a TrustedIssuer>,
}

/// Manages Json Web Keys (JWK).
// TODO: periodically update the key stores to ensure keys are valid.
#[derive(Debug)]
pub struct KeyService {
    key_stores: HashMap<TrustedIssuerId, JwkStore>,
}

impl KeyService {
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
    pub fn new_from_str(key_stores: &str) -> Result<Self, KeyServiceError> {
        let parsed_stores = serde_json::from_str::<HashMap<String, Value>>(key_stores)
            .map_err(KeyServiceError::DecodeJwkStores)?;
        let mut key_stores = HashMap::new();
        for (iss_id, keys) in &parsed_stores {
            let iss_id = TrustedIssuerId::from(iss_id.as_str());
            let jwks = json!({"keys": keys});
            key_stores.insert(iss_id.clone(), JwkStore::new_from_jwks_value(iss_id, jwks)?);
        }
        Ok(Self { key_stores })
    }

    /// Loads key stores using a JSON string.
    ///
    /// Enables loading key stores from a local JSON file.
    pub fn new_from_trusted_issuers(
        trusted_issuers: &HashMap<String, TrustedIssuer>,
    ) -> Result<Self, KeyServiceError> {
        let http_client = HttpClient::new(3, Duration::from_secs(3))?;

        let mut key_stores = HashMap::new();
        for (iss_id, iss) in trusted_issuers.iter() {
            let iss_id: Arc<str> = iss_id.as_str().into();
            key_stores.insert(
                iss_id.clone(),
                JwkStore::new_from_trusted_issuer(iss_id, iss, &http_client)?,
            );
        }

        Ok(Self { key_stores })
    }

    /// Gets the decoding key with the given key ID from the store with it's Trusted Issuer.
    pub fn get_key(&self, key_id: &str) -> Option<DecodingKeyWithIss> {
        // PERF: We can add a reference of all the keys into a HashMap
        // so we do not need to loop through all of these.
        for store in self.key_stores.values() {
            if let Some(key) = store.get(key_id) {
                return Some(DecodingKeyWithIss {
                    key,
                    key_iss: store.source_iss(),
                });
            }
        }

        None
    }

    /// Returns a Vec containing a reference to all of the keys.
    ///
    /// Useful if the keys from the JWKS do not have a `kid` (Key ID).
    //  this is currently unused since we don't handle JWTs without `kid` yet.
    #[allow(dead_code)]
    pub fn get_keys(&self) -> Vec<&DecodingKey> {
        // PERF: We can cache the returned Vec so it doesn't
        // get created every time this function is called.
        let mut keys = Vec::new();
        self.key_stores
            .values()
            .for_each(|store| keys.extend(store.get_keys()));
        keys
    }
}

#[derive(thiserror::Error, Debug)]
pub enum KeyServiceError {
    #[error("Failed to decode JWK Stores from string: {0}")]
    DecodeJwkStores(serde_json::Error),
    #[error("Failed to make HTTP Request: {0}")]
    Http(#[from] HttpClientError),
    #[error("Failed to load JWKS: {0}")]
    JwkStoreError(#[from] JwkStoreError),
}

#[cfg(test)]
mod test {
    use std::collections::HashMap;

    use mockito::Server;
    use serde_json::json;

    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::key_service::KeyService;

    #[test]
    fn can_load_from_str() {
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3";
        let key_stores = json!({
            "some_issuer_id": [{
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee"
            }],
            "anorther_issuer_id": [{
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": "73e25f9789119c7875d58087a78ac23f5ef2eda3"
            }],
        });

        let key_service = KeyService::new_from_str(&key_stores.to_string())
            .expect("Should load KeyService from str");

        assert!(
            key_service.get_key(kid1).is_some(),
            "Expected to find a key"
        );
        assert!(
            key_service.get_key(kid2).is_some(),
            "Expected to find a key"
        );
        assert!(
            key_service.get_key("some unknown key id").is_none(),
            "Expected to not find a key"
        );
    }

    #[test]
    fn can_load_jwk_stores_from_multiple_trusted_issuers() {
        let kid1 = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
        let kid2 = "73e25f9789119c7875d58087a78ac23f5ef2eda3";

        let mut mock_server = Server::new();

        // Setup first OpenID config endpoint
        let openid_config_endpoint1 = mock_server
            .mock("GET", "/first/.well-known/openid-configuration")
            .with_status(200)
            .with_body(
                json!({
                    "issuer": mock_server.url(),
                    "jwks_uri": format!("{}/first/jwks", mock_server.url())
                })
                .to_string(),
            )
            .expect(1)
            .create();
        // Setup first JWKS endpoint
        let jwks_endpoint1 = mock_server
            .mock("GET", "/first/jwks")
            .with_status(200)
            .with_body(
                json!({
                "keys": [
                    {
                        "use": "sig",
                        "e": "AQAB",
                        "alg": "RS256",
                        "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKArc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                        "kty": "RSA",
                        "kid": kid1,
                    },
                ]
                }).to_string())
            .expect(1)
            .create();

        // Setup second OpenID config endpoint
        let openid_config_endpoint2 = mock_server
            .mock("GET", "/second/.well-known/openid-configuration")
            .with_status(200)
            .with_body(
                json!({
                    "issuer": mock_server.url(),
                    "jwks_uri": format!("{}/second/jwks", mock_server.url())
                })
                .to_string(),
            )
            .expect(1)
            .create();
        // Setup second JWKS endpoint
        let jwks_endpoint2 = mock_server
            .mock("GET", "/second/jwks")
            .with_status(200)
            .with_body(
                json!({
                "keys": [
                    {
                        "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                        "e": "AQAB",
                        "use": "sig",
                        "alg": "RS256",
                        "kty": "RSA",
                        "kid": kid2,
                    }

                ]}).to_string())
            .expect(1)
            .create();

        let key_service = KeyService::new_from_trusted_issuers(&HashMap::from([
            ("first".to_string(), TrustedIssuer {
                name: "First IDP".to_string(),
                description: "".to_string(),
                openid_configuration_endpoint: format!(
                    "{}/first/.well-known/openid-configuration",
                    mock_server.url()
                ),
                ..Default::default()
            }),
            ("second".to_string(), TrustedIssuer {
                name: "Second IDP".to_string(),
                description: "".to_string(),
                openid_configuration_endpoint: format!(
                    "{}/second/.well-known/openid-configuration",
                    mock_server.url()
                ),
                ..Default::default()
            }),
        ]))
        .expect("Should load KeyService from trusted issuers");

        assert!(
            key_service.get_key(kid1).is_some(),
            "Expected to find a key"
        );
        assert!(
            key_service.get_key(kid2).is_some(),
            "Expected to find a key"
        );
        assert!(
            key_service.get_key("some unknown key id").is_none(),
            "Expected to not find a key"
        );

        // Assert that each of the endpoints are only visited once.
        openid_config_endpoint1.assert();
        openid_config_endpoint2.assert();
        jwks_endpoint1.assert();
        jwks_endpoint2.assert();
    }
}
