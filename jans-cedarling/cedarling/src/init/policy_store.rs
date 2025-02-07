// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::path::Path;
use std::time::Duration;
use std::{fs, io};

use reqwest::Client;
use serde_json::{Value, json};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{AgamaPolicyStore, PolicyStoreWithID};
use crate::http::{HttpClient, HttpClientError};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yml::Error),
    #[error("failed to fetch the policy store from the given URI: {0}")]
    LoadFromUri(#[from] HttpClientError),
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
    #[error("Failed to load policy store from {0}: {1}")]
    ParseFile(Box<Path>, io::Error),
    #[error("failed to fetch the policy store from the lock server: {0}")]
    LoadFromLock(#[from] LoadFromLockError),
}

/// Errors that can occur when loading a policy store from the lock server.
#[derive(Debug, thiserror::Error)]
pub enum LoadFromLockError {
    #[error("failed to complete http request: {0}")]
    FailedRequest(#[source] reqwest::Error),
    #[error("bad request: {0}")]
    BadRequest(#[source] reqwest::Error),
    #[error("failed to deserialize response: {0}")]
    DeserializeResponse(#[source] reqwest::Error),
    #[error(
        "could not find `oauth_as_well_known` from the `/.well-known/lock-server-configuration` endpoint"
    )]
    MissingOidcEndpoint,
    #[error(
        "could not find `config_uri` from the `/.well-known/lock-server-configuration` endpoint"
    )]
    MissingConfigEndpoint,
    #[error(
        "could not find `registration_endpoint` from the `/.well-known/openid-configuration` endpoint"
    )]
    MissingDcrEndpoint,
    #[error(
        "could not find `token_endpoint` from the `/.well-known/openid-configuration` endpoint"
    )]
    MissingTokenEndpoint,
    #[error("could not find `client_id` from the DCA response")]
    MissingClientId,
    #[error("expected the JSON value, `{0}`, to be a string but got `{1:?}`")]
    NotAValidString(String, Value),
    #[error("could not find `access_token` from the token response")]
    MissingAccessToken,
}

// AgamaPolicyStore contains the structure to accommodate several policies,
// and this code for now assumes that there is only ever one policy store,
// extract the first 'policy_stores' entry.
fn extract_first_policy_store(
    agama_policy_store: &AgamaPolicyStore,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    if agama_policy_store.policy_stores.len() != 1 {
        return Err(PolicyStoreLoadError::InvalidStore(format!(
            "expected exactly one 'policy_stores' entry, but found {:?}",
            agama_policy_store.policy_stores.len()
        )));
    }

    // extract exactly the first policy store in the struct
    let policy_store_option = agama_policy_store
        .policy_stores
        .iter()
        .take(1)
        .map(|(k, v)| PolicyStoreWithID {
            id: k.to_owned(),
            store: v.to_owned(),
        })
        .next();

    match policy_store_option {
        Some(policy_store) => Ok(policy_store.clone()),
        None => Err(PolicyStoreLoadError::InvalidStore(
            "error retrieving first policy_stores element".into(),
        )),
    }
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) async fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let policy_store = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            let agama_policy_store = serde_json::from_str::<AgamaPolicyStore>(policy_json)
                .map_err(PolicyStoreLoadError::ParseJson)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            let agama_policy_store = serde_yml::from_str::<AgamaPolicyStore>(policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::LockMaster {
            ssa_jwt,
            config_uri,
            policy_store_id,
            jwks,
        } => load_policy_store_from_lock_master(ssa_jwt, config_uri, policy_store_id, jwks).await?,
        PolicyStoreSource::FileJson(path) => {
            let policy_json = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone(), e))?;
            let agama_policy_store = serde_json::from_str::<AgamaPolicyStore>(&policy_json)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::FileYaml(path) => {
            let policy_yaml = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone(), e))?;
            let agama_policy_store = serde_yml::from_str::<AgamaPolicyStore>(&policy_yaml)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::Uri(uri) => load_policy_store_from_uri(uri).await?,
    };

    Ok(policy_store)
}

/// Loads the policy store from the Lock Master.
async fn load_policy_store_from_lock_master(
    ssa_jwt: &Option<String>,
    config_uri: &str,
    policy_store_id: &str,
    jwks: &Option<String>,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    const SCOPE: &str = "cedarling";

    let client = Client::new();

    // GET request to the `/.well-known/lock-server-configuration` endpoint
    // for the shape of the response, see:
    // https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/refs/heads/main/jans-lock/lock-server.yaml
    let lock_server_config_resp = client
        .get(config_uri)
        .send()
        .await
        .map_err(LoadFromLockError::FailedRequest)?
        .error_for_status()
        .map_err(LoadFromLockError::BadRequest)?
        .json::<HashMap<String, String>>()
        .await
        .map_err(LoadFromLockError::DeserializeResponse)?;

    // this is the `/.well-known/openid-configuration` endpoint
    let idp_uri = lock_server_config_resp
        .get("oauth_as_well_known")
        .ok_or(LoadFromLockError::MissingOidcEndpoint)?;
    // this is the `/config` endpoint that will contain a policy store
    let policy_store_uri = lock_server_config_resp
        .get("config_uri")
        .ok_or(LoadFromLockError::MissingConfigEndpoint)?;

    let openid_config_resp = client
        .get(idp_uri)
        .send()
        .await
        .map_err(LoadFromLockError::FailedRequest)?
        .error_for_status()
        .map_err(LoadFromLockError::BadRequest)?
        .json::<HashMap<String, String>>()
        .await
        .map_err(LoadFromLockError::DeserializeResponse)?;

    // dcr stands for dynamic client registration
    // see: https://datatracker.ietf.org/doc/html/rfc7591
    let dcr_uri = openid_config_resp
        .get("registration_endpoint")
        .ok_or(LoadFromLockError::MissingDcrEndpoint)?;
    let token_uri = openid_config_resp
        .get("token_endpoint")
        .ok_or(LoadFromLockError::MissingTokenEndpoint)?;
    let jwks_uri = openid_config_resp.get("jwks_uri");

    let mut dca_payload = json!({
    "token_endpoint_auth_method": "client_secret_basic",
        "grant_types": ["client_credentials"],
        "client_name": "cedarling-test",
        "scope": SCOPE,
        "access_token_as_jwt": true,
        "software_version": "0.0.0",
    });
    if let Some(ssa_jwt) = ssa_jwt {
        dca_payload["software_statement"] = json!(ssa_jwt);
    }
    if let Some(jwks_uri) = jwks_uri {
        dca_payload["jwks_uri"] = json!(jwks_uri);
    } else if let Some(jwks) = jwks {
        dca_payload["jwks"] = json!(jwks);
    }

    let dca_resp = client
        .post(dcr_uri)
        .json(&dca_payload)
        .send()
        .await
        .map_err(LoadFromLockError::FailedRequest)?
        .error_for_status()
        .map_err(LoadFromLockError::BadRequest)?
        .json::<HashMap<String, Value>>()
        .await
        .map_err(LoadFromLockError::DeserializeResponse)?;

    // For requesting tokens,
    // see: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
    //
    // For requesting tokens response,
    // see: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.4
    let client_id = dca_resp
        .get("client_id")
        .ok_or(LoadFromLockError::MissingClientId)?
        .as_str()
        .ok_or_else(|| {
            LoadFromLockError::NotAValidString(
                "client_id".into(),
                dca_resp.get("client_id").unwrap().clone(),
            )
        })?;
    let client_secret = dca_resp
        .get("client_secret")
        .map(|x| {
            x.as_str().ok_or_else(|| {
                LoadFromLockError::NotAValidString(
                    "client_secret".into(),
                    dca_resp.get("client_secret").unwrap().clone(),
                )
            })
        })
        .transpose()?;
    let token_resp = client
        .post(token_uri)
        .basic_auth(client_id, client_secret)
        .form(&json!({
            "grant_type": "client_credentials",
            "client_id": "client_id",
            "scope": SCOPE,
        }))
        .send()
        .await
        .map_err(LoadFromLockError::FailedRequest)?
        .error_for_status()
        .map_err(LoadFromLockError::BadRequest)?
        .json::<HashMap<String, Value>>()
        .await
        .map_err(LoadFromLockError::DeserializeResponse)?;

    let access_token = token_resp
        .get("access_token")
        .ok_or(LoadFromLockError::MissingAccessToken)?
        .as_str()
        .ok_or_else(|| {
            LoadFromLockError::NotAValidString(
                "access_token".into(),
                token_resp.get("access_token").unwrap().clone(),
            )
        })?;

    let policy_store_resp = client
        .get(policy_store_uri)
        .bearer_auth(access_token)
        .query(&json!({
            "policy_store_format": "json",
            "policy_store_id": policy_store_id,
        }))
        .send()
        .await
        .map_err(LoadFromLockError::FailedRequest)?
        .error_for_status()
        .map_err(LoadFromLockError::BadRequest)?
        .json::<AgamaPolicyStore>()
        .await
        .map_err(LoadFromLockError::DeserializeResponse)?;

    extract_first_policy_store(&policy_store_resp)
}

/// Loads the policy store from a URI
///
/// The URI is from the `CEDARLING_POLICY_STORE_URI` bootstrap property.
async fn load_policy_store_from_uri(uri: &str) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let client = HttpClient::new(3, Duration::from_secs(3))?;
    let agama_policy_store = client.get(uri).await?.json::<AgamaPolicyStore>()?;
    extract_first_policy_store(&agama_policy_store)
}

#[cfg(test)]
mod test {
    use std::path::Path;

    use mockito::{Matcher, Server};
    use serde_json::json;
    use test_utils::token_claims::generate_token_using_claims;

    use super::load_policy_store;
    use crate::PolicyStoreConfig;

    // NOTE: we probably don't need to test if the deserialization for JSON and YAML
    // works correctly anymore here since we already have tests for those in
    // src/common/policy_store/test.rs...

    #[tokio::test]
    async fn can_load_from_json_file() {
        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::FileJson(
                Path::new("../test_files/policy-store_generated.json").into(),
            ),
        })
        .await
        .expect("Should load policy store from JSON file");
    }

    #[tokio::test]
    async fn can_load_from_yaml_file() {
        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok.yaml").into(),
            ),
        })
        .await
        .expect("Should load policy store from YAML file");
    }

    #[tokio::test]
    async fn can_load_from_uri() {
        let mut mock_server = Server::new_async().await;

        let policy_store_json =
            include_str!("../../../test_files/policy-store_ok.json").to_string();

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(policy_store_json)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store", mock_server.url()).to_string();

        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::Uri(uri),
        })
        .await
        .expect("Should load policy store from URI");

        mock_endpoint.assert();
    }

    // TODO: finish this test
    #[tokio::test]
    async fn can_load_from_lock_master() {
        let policy_store_id = "gICAgcHJpbmNpcGFsIGlz".to_string();
        let mut mock_lock_server = Server::new_async().await;
        let lock_policy_store_endpoint = "/config";
        let lock_policy_store_uri =
            format!("{}{}", mock_lock_server.url(), lock_policy_store_endpoint);
        let lock_config_endpoint = "/.well-known/lock-server-configuration";
        let lock_config_uri = format!("{}{}", mock_lock_server.url(), lock_config_endpoint);

        let mut mock_idp_server = Server::new_async().await;
        let idp_registration_endpoint = "/register";
        let idp_registration_uri =
            format!("{}{}", mock_idp_server.url(), idp_registration_endpoint);
        let idp_token_endpoint = "/token";
        let idp_token_uri = format!("{}{}", mock_idp_server.url(), idp_token_endpoint);
        let idp_conf_endpoint = "/.well-known/openid-configuration";
        let idp_conf_uri = format!("{}{}", mock_idp_server.url(), idp_conf_endpoint);

        let policy_store_json =
            include_str!("../../../test_files/policy-store_ok.json").to_string();

        let lock_config_endpoint = mock_lock_server
            .mock("GET", lock_config_endpoint)
            .with_status(200)
            .with_header("Content-Type", "application/json")
            .with_body(
                json!({
                    "config_uri": lock_policy_store_uri,
                    "oauth_as_well_known": idp_conf_uri,
                })
                .to_string(),
            )
            .expect(1)
            .create();
        let policy_store_endpoint = mock_lock_server
            .mock("GET", lock_policy_store_endpoint)
            .with_status(200)
            .with_header("Content-Type", "application/json")
            .with_body(policy_store_json)
            .match_query(Matcher::AllOf(vec![
                Matcher::UrlEncoded("policy_store_format".into(), "json".into()),
                Matcher::UrlEncoded("policy_store_id".into(), policy_store_id.clone()),
            ]))
            .expect(1)
            .create();
        let openid_conf_endpoint = mock_idp_server
            .mock("GET", idp_conf_endpoint)
            .with_status(200)
            .with_header("Content-Type", "application/json")
            .with_body(
                json!({
                    "registration_endpoint": idp_registration_uri,
                    "token_endpoint": idp_token_uri,
                })
                .to_string(),
            )
            .expect(1)
            .create();
        let registration_endpoint = mock_idp_server
            .mock("POST", idp_registration_endpoint)
            .with_status(201)
            .with_header("Content-Type", "application/json")
            .with_body(
                json!({"client_id": "test-client-id", "client_secret": "tes-client-secret"})
                    .to_string(),
            )
            .expect(1)
            .create();
        let token_endpoint = mock_idp_server
            .mock("POST", idp_token_endpoint)
            .with_status(200)
            .with_header("Content-Type", "application/json")
            .with_body(
                json!({
                    "access_token": generate_token_using_claims(json!({
                        "iss": mock_idp_server.url(),
                        "aud": "some-client-id",
                        "scope": "cedarling",
                    }))
                })
                .to_string(),
            )
            .expect(1)
            .create();

        let ssa_jwt = generate_token_using_claims(json!({
          "software_id": "4NRB1-0XZABZI9E6-5SM3R",
          "client_name": "Test Cedarling Client",
          "client_uri": "https://client.example.net/"
        }));

        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::LockMaster {
                ssa_jwt: Some(ssa_jwt),
                config_uri: lock_config_uri,
                policy_store_id,
                jwks: None,
            },
        })
        .await
        .expect("Should load policy store from URI");

        lock_config_endpoint.assert();
        policy_store_endpoint.assert();
        openid_conf_endpoint.assert();
        token_endpoint.assert();
        registration_endpoint.assert();
    }
}
