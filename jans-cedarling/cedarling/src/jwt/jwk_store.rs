use super::{
    http_client::{HttpClient, HttpClientError},
    TrustedIssuerId,
};
use crate::common::policy_store::TrustedIssuer;
use jsonwebtoken::jwk::JwkSet;
use serde::{de::Error, Deserialize};
use serde_json::Value;
use std::collections::HashMap;
use time::OffsetDateTime;

#[derive(Deserialize)]
struct OpenIdConfig {
    issuer: String,
    jwks_uri: String,
}

/// Enum representing a JSON Web Key Store (JWKS) source.
#[derive(Debug, PartialEq)]
#[allow(dead_code)]
pub enum JwkStore {
    /// A Local JWKS store loaded from a file or a string.
    Local {
        jwks: JwkSet,
        last_updated: OffsetDateTime,
    },
    /// OpenID-based JWKS store, including its configuration endpoint.
    OpenId {
        jwks: JwkSet,
        last_updated: OffsetDateTime,
        openid_config_endpoint: String,
        issuer: String,
    },
}

/// Loads a the JWK Stores from the provided HashMap of OpenID-based JWKS stores.
pub fn load_openid_stores(
    http_client: &HttpClient,
    mut trusted_issuers: HashMap<TrustedIssuerId, TrustedIssuer>,
) -> Result<HashMap<TrustedIssuerId, JwkStore>, JwksLoadingError> {
    let mut jwk_stores = HashMap::new();
    for (iss_id, iss_info) in trusted_issuers.drain() {
        // fetch openid configuration
        let response = http_client.get(&iss_info.openid_configuration_endpoint)?;
        let openid_config = response
            .json::<OpenIdConfig>()
            .map_err(|e| serde_json::Error::custom(e))?;

        // fetch jwks
        let response = http_client.get(&openid_config.jwks_uri)?;
        let jwks = response
            .json::<JwkSet>()
            .map_err(|e| serde_json::Error::custom(e))?;

        jwk_stores.insert(
            TrustedIssuerId::from(iss_id),
            JwkStore::OpenId {
                jwks,
                last_updated: OffsetDateTime::now_utc(),
                issuer: openid_config.issuer,
                openid_config_endpoint: iss_info.openid_configuration_endpoint,
            },
        );
    }

    Ok(jwk_stores)
}

/// Loads a local JWKS store.
pub fn load_local_store(
    jwks: String,
) -> Result<HashMap<TrustedIssuerId, JwkStore>, JwksLoadingError> {
    let parsed_stores = serde_json::from_str::<HashMap<String, Value>>(&jwks)?;

    let mut local_stores = HashMap::new();
    for (id, store) in parsed_stores {
        let jwks = serde_json::from_value::<JwkSet>(store).map_err(JwksLoadingError::Decoding)?;
        local_stores.insert(
            id,
            JwkStore::Local {
                jwks,
                last_updated: OffsetDateTime::now_utc(),
            },
        );
    }

    Ok(local_stores)
}

#[derive(thiserror::Error, Debug)]
pub enum JwksLoadingError {
    #[error("Failed to decode JWKS from string: {0}")]
    Decoding(#[from] serde_json::Error),
    #[error("Both a local keystore and a map of trusted issuers was provided.")]
    InvalidConfig,
    #[error("There is no method to load a keystore provided in the configs.")]
    MissingConfig,
    #[error("Failed to make HTTP Request: {0}")]
    Http(#[from] HttpClientError),
}

#[cfg(test)]
mod test {
    use super::{load_local_store, JwkStore};
    use crate::{
        common::policy_store::TrustedIssuer,
        jwt::{http_client::HttpClient, jwk_store::load_openid_stores},
    };
    use mockito::Server;
    use serde_json::json;
    use std::{collections::HashMap, time::Duration};
    use time::OffsetDateTime;

    #[test]
    fn can_load_local_stores() {
        let jwks_json = json!({
        "keys": [
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee"
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": "73e25f9789119c7875d58087a78ac23f5ef2eda3"
            }
        ]
        });

        let expected = HashMap::from([(
            "local".to_string(),
            JwkStore::Local {
                jwks: serde_json::from_value(jwks_json.clone())
                    .expect("Should deserialize Value into a JwkSet"),
                last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
            },
        )]);

        let jwks_local_json = json!({
            "local": jwks_json,
        });

        let result = load_local_store(jwks_local_json.to_string()).expect("Should load local jwks");

        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        let result = result
            .into_iter()
            .map(|(key, jwks)| match jwks {
                JwkStore::Local { jwks, .. } => (
                    key,
                    JwkStore::Local {
                        jwks,
                        last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
                    },
                ),
                JwkStore::OpenId { .. } => panic!("Expected JwkStore::Local"),
            })
            .collect();

        assert_eq!(
            expected, result,
            "Expected: {result:?}\nBut got: {expected:?}"
        );
    }

    #[test]
    fn can_load_openid_stores() {
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
        let jwks_json = json!({
        "keys": [
            {
                "use": "sig",
                "e": "AQAB",
                "alg": "RS256",
                "n": "4VI56fF0rcWHHVgHFLHrmEO5w8oN9gbSQ9TEQnlIKRg0zCtl2dLKtt0hC6WMrTA9cF7fnK4CLNkfV_Mytk-rydu2qRV_kah62v9uZmpbS5dcz5OMXmPuQdV8fDVIvscDK5dzkwD3_XJ2mzupvQN2reiYgce6-is23vwOyuT-n4vlxSqR7dWdssK5sj9mhPBEIlfbuKNykX5W6Rgu-DyuoKRc_aukWnLxWN-yoroP2IHYdCQm7Ol08vAXmrwMyDfvsmqdXUEx4om1UZ5WLf-JNaZp4lXhgF7Cur5066213jwpp4f_D3MyR-oa43fSa91gqp2berUgUyOWdYSIshABVQ",
                "kty": "RSA",
                "kid": "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee"
            },
            {
                "n": "tMXbmw7xEDVLLkAJdxpI-6pGywn0x9fHbD_mfgtFGZEs1LDjhDAJq6c-SoODeWQstjpetTgNqVCKOuU6zGyFPNtkDjhJqDW6THy06uJ8I85crILo3h-6NPclZ3bK9OzN5bIbzjbSvxrIM7ORZOlWzByOn5qGsMvI3aDrZ0lXNC1eCDWJpoJznG1fWcHYxbUy_CHDC3Cd26jX19aRALEEQU-y-wi9pv86qxEmrYMLsVN3__eWNNPkzxgf0eSOWFDv5_19YK7irYztqiwin6abxr9RHj3Qs21hpJ9A-YfsfmNkxmifgDeiTnXpZY8yfVTCJTtkgT7sjdU1lvhsMa4Z0w",
                "e": "AQAB",
                "use": "sig",
                "alg": "RS256",
                "kty": "RSA",
                "kid": "73e25f9789119c7875d58087a78ac23f5ef2eda3"
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
        let result = load_openid_stores(
            &http_client,
            HashMap::from([(
                "test_issuer".to_string(),
                TrustedIssuer {
                    name: "Test Issuer".to_string(),
                    description: "This is a test trusted issuer.".to_string(),
                    openid_configuration_endpoint: format!(
                        "{}/.well-known/openid-configuration",
                        mock_server.url()
                    ),
                    ..Default::default()
                },
            )]),
        )
        .expect("Should load jwks from jwks endpoint");

        // We edit the `last_updated` from the result so that the comparison
        // wont fail because of the timestamp.
        let result = result
            .into_iter()
            .map(|(key, jwks)| match jwks {
                JwkStore::OpenId {
                    jwks,
                    openid_config_endpoint,
                    ..
                } => (
                    key,
                    JwkStore::OpenId {
                        jwks,
                        last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
                        openid_config_endpoint,
                        issuer: mock_server.url(),
                    },
                ),
                JwkStore::Local { .. } => panic!("Expected JwkStore::OpenId"),
            })
            .collect();

        let expected = HashMap::from([(
            "test_issuer".to_string(),
            JwkStore::OpenId {
                jwks: serde_json::from_value(jwks_json.clone())
                    .expect("Should deserialize Value into a JwkSet."),
                last_updated: OffsetDateTime::from_unix_timestamp(0).unwrap(),
                openid_config_endpoint: format!(
                    "{}/.well-known/openid-configuration",
                    mock_server.url()
                ),
                issuer: mock_server.url(),
            },
        )]);

        assert_eq!(expected, result);

        // Assert that the mock endpoints only gets called once.
        openid_config_endpoint.assert();
        jwks_endpoint.assert();
    }
}
