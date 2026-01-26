// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::path::Path;
use std::time::Duration;
use std::{fs, io};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::manager::PolicyStoreManager;
use crate::common::policy_store::{AgamaPolicyStore, ConversionError, PolicyStoreWithID};
use crate::http::{HttpClient, HttpClientError};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    /// Failed to parse policy store from JSON string.
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    /// Failed to parse policy store from YAML string.
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yml::Error),
    /// Failed to fetch the policy store from the lock server.
    #[error("failed to fetch the policy store from the lock server")]
    FetchFromLockServer(#[from] HttpClientError),
    /// Invalid structure in the policy store.
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
    /// Failed to read or parse the policy store file.
    #[error("Failed to load policy store from {0}: {1}")]
    ParseFile(Box<Path>, io::Error),
    /// Failed to parse the policy store from file.
    #[error("Failed to convert loaded policy store: {0}")]
    Conversion(#[from] ConversionError),
    /// Failed to load policy store from archive.
    #[error("Failed to load policy store from archive: {0}")]
    Archive(String),
    /// Failed to load policy store from directory.
    #[error("Failed to load policy store from directory: {0}")]
    Directory(String),
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
            metadata: None, // Legacy format doesn't include metadata
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
///
/// # Errors
///
/// Returns [`PolicyStoreLoadError`] if:
/// - The policy store configuration is invalid
/// - JSON/YAML parsing fails
/// - No policy store is found in the configuration
/// - Policy store validation fails
pub async fn load_policy_store(
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
        PolicyStoreSource::LockServer(policy_store_uri) => {
            load_policy_store_from_lock_master(policy_store_uri).await?
        },
        PolicyStoreSource::FileJson(path) => {
            let policy_json = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store = serde_json::from_str::<AgamaPolicyStore>(&policy_json)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::FileYaml(path) => {
            let policy_yaml = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store = serde_yml::from_str::<AgamaPolicyStore>(&policy_yaml)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::CjarFile(path) => load_policy_store_from_cjar_file(path).await?,
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::CjarFile(path) => load_policy_store_from_cjar_file(path)?,
        PolicyStoreSource::CjarUrl(url) => load_policy_store_from_cjar_url(url).await?,
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::Directory(path) => load_policy_store_from_directory(path).await?,
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::Directory(path) => load_policy_store_from_directory(path)?,
        PolicyStoreSource::ArchiveBytes(bytes) => load_policy_store_from_archive_bytes(bytes)?,
    };

    Ok(policy_store)
}

/// Loads the policy store from the Lock Master.
///
/// The URI is from the `CEDARLING_POLICY_STORE_URI` bootstrap property.
async fn load_policy_store_from_lock_master(
    uri: &str,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let client = HttpClient::new(3, Duration::from_secs(3))?;
    let agama_policy_store = client.get(uri).await?.json::<AgamaPolicyStore>()?;
    extract_first_policy_store(&agama_policy_store)
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
///
/// Uses the `load_policy_store_archive` function from the loader module
/// and converts to legacy format for backward compatibility.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_cjar_file(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::{loader, manager::PolicyStoreManager};

    let loaded = loader::load_policy_store_archive(path)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
fn load_policy_store_from_cjar_file(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Call the loader stub function to ensure it's used and the error variant is constructed
    match loader::load_policy_store_archive(path) {
        Err(e) => Err(PolicyStoreLoadError::Archive(format!(
            "Loading from file path is not supported in WASM. Use CjarUrl instead. Original error: {e}",
        ))),
        Ok(_) => unreachable!("WASM stub should always return an error"),
    }
}

/// Loads the policy store from a Cedar Archive (.cjar) URL.
///
/// Fetches the archive via HTTP, loads it using `load_policy_store_archive_bytes`,
/// and converts to legacy format for backward compatibility.
async fn load_policy_store_from_cjar_url(
    url: &str,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Fetch the archive bytes via HTTP
    let client = HttpClient::new(3, Duration::from_secs(3))?;
    let bytes = client
        .get_bytes(url)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to fetch archive: {e}")))?;

    // Load from bytes (works in both native and WASM)
    let loaded = loader::load_policy_store_archive_bytes(&bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

/// Loads the policy store from a directory structure.
///
/// Uses the `load_policy_store_directory` function from the loader module
/// and converts to legacy format for backward compatibility.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_directory(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    let loaded = loader::load_policy_store_directory(path)
        .await
        .map_err(|e| {
            PolicyStoreLoadError::Directory(format!("Failed to load from directory: {e}"))
        })?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

/// Loads the policy store from a directory structure.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
fn load_policy_store_from_directory(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Call the loader stub function to ensure it's used and the error variant is constructed
    match loader::load_policy_store_directory(path) {
        Err(e) => Err(PolicyStoreLoadError::Directory(format!(
            "Loading from directory is not supported in WASM. Original error: {e}",
        ))),
        Ok(_) => unreachable!("WASM stub should always return an error"),
    }
}

/// Loads the policy store directly from archive bytes.
///
/// This is useful for:
/// - WASM environments with custom fetch logic (e.g., auth headers)
/// - Embedding archives in applications
/// - Loading from non-standard sources (databases, S3, etc.)
///
/// Works on all platforms including WASM.
fn load_policy_store_from_archive_bytes(
    bytes: &[u8],
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Load from bytes (works in both native and WASM)
    let loaded = loader::load_policy_store_archive_bytes(bytes).map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to load from archive bytes: {e}"))
    })?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
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
    async fn can_load_from_lock_master() {
        let mut mock_server = Server::new_async().await;

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
            source: crate::PolicyStoreSource::LockServer(uri),
        })
        .await
        .expect("Should load policy store from Lock Master file");

        mock_endpoint.assert();
    }
}
