/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{AgamaPolicyStore, PolicyStoreWithID};
use crate::http::{HttpClient, HttpClientError};
use std::path::Path;
use std::time::Duration;
use std::{fs, io};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yml::Error),
    #[error("failed to fetch the policy store from the lock master")]
    FetchFromLockMaster(#[from] HttpClientError),
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
    #[error("Failed to load policy store from {0}: {1}")]
    ParseFile(Box<Path>, io::Error),
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
pub(crate) fn load_policy_store(
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
        PolicyStoreSource::LockMaster(policy_store_uri) => {
            load_policy_store_from_lock_master(policy_store_uri)?
        },
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
    };

    Ok(policy_store)
}

/// Loads the policy store from the Lock Master.
///
/// The URI is from the `CEDARLING_POLICY_STORE_URI` bootstrap property.
fn load_policy_store_from_lock_master(
    uri: &str,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let client = HttpClient::new(3, Duration::from_secs(3))?;
    let agama_policy_store = client.get(uri)?.json::<AgamaPolicyStore>()?;
    extract_first_policy_store(&agama_policy_store)
}

#[cfg(test)]
mod test {
    use std::path::Path;

    use mockito::Server;

    use super::load_policy_store;
    use crate::PolicyStoreConfig;

    // NOTE: we probably don't need to test if the deserialization for JSON and YAML
    // works correctly anymore here since we already have tests for those in
    // src/common/policy_store/test.rs...

    #[test]
    fn can_load_from_json_file() {
        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::FileJson(
                Path::new("../test_files/policy-store_generated.json").into(),
            ),
        })
        .expect("Should load policy store from JSON file");
    }

    #[test]
    fn can_load_from_yaml_file() {
        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok.yaml").into(),
            ),
        })
        .expect("Should load policy store from YAML file");
    }

    #[test]
    fn can_load_from_lock_master() {
        let mut mock_server = Server::new();

        let policy_store_json =
            include_str!("../../../test_files/policy-store_lock_master_ok.json").to_string();

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(policy_store_json)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store", mock_server.url()).to_string();

        load_policy_store(&PolicyStoreConfig {
            source: crate::PolicyStoreSource::LockMaster(uri),
        })
        .expect("Should load policy store from Lock Master file");

        mock_endpoint.assert();
    }
}
