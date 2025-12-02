// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::path::Path;
use std::time::Duration;
use std::{fs, io};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{
    AgamaPolicyStore, ConversionError, PolicyStoreManager, PolicyStoreWithID,
};
use crate::http::{HttpClient, HttpClientError};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yml::Error),
    #[error("failed to fetch the policy store from the lock server")]
    FetchFromLockServer(#[from] HttpClientError),
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
    #[error("Failed to load policy store from {0}: {1}")]
    ParseFile(Box<Path>, io::Error),
    #[error("Failed to convert loaded policy store: {0}")]
    Conversion(#[from] ConversionError),
    #[error("Failed to load policy store from archive: {0}")]
    Archive(String),
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
        PolicyStoreSource::CjarFile(path) => load_policy_store_from_cjar_file(path).await?,
        PolicyStoreSource::CjarUrl(url) => load_policy_store_from_cjar_url(url).await?,
        PolicyStoreSource::Directory(path) => load_policy_store_from_directory(path).await?,
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
/// Uses the new directory structure format and converts it to the legacy format
/// for backward compatibility with existing code.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_cjar_file(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::DefaultPolicyStoreLoader;
    use crate::common::policy_store::archive_handler::ArchiveVfs;

    // Create an archive VFS from the file path
    let archive_vfs = ArchiveVfs::from_file(path)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to open archive: {}", e)))?;

    // Use the DefaultPolicyStoreLoader to load from the archive
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    let loaded = loader.load_directory(".").map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to load from archive: {}", e))
    })?;

    // Get the policy store ID from the metadata
    let store_id = loaded.metadata.policy_store.id.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
    })
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
async fn load_policy_store_from_cjar_file(
    _path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    Err(PolicyStoreLoadError::Archive(
        "Loading from file path is not supported in WASM. Use CjarUrl instead.".to_string(),
    ))
}

/// Loads the policy store from a Cedar Archive (.cjar) URL.
///
/// Downloads the archive and loads it using the new directory structure format,
/// then converts to legacy format for backward compatibility.
async fn load_policy_store_from_cjar_url(
    url: &str,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::DefaultPolicyStoreLoader;
    use crate::common::policy_store::archive_handler::ArchiveVfs;

    // Fetch the archive from the URL
    let client = HttpClient::new(3, Duration::from_secs(30))?;
    let bytes = client
        .get_bytes(url)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to fetch archive: {}", e)))?;

    // Create an archive VFS from the downloaded bytes
    let archive_vfs = ArchiveVfs::from_buffer(bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to open archive: {}", e)))?;

    // Use the DefaultPolicyStoreLoader to load from the archive
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    let loaded = loader.load_directory(".").map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to load from archive: {}", e))
    })?;

    // Get the policy store ID from the metadata
    let store_id = loaded.metadata.policy_store.id.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
    })
}

/// Loads the policy store from a directory structure.
///
/// Uses the new directory structure format (with manifest.json, policies/, etc.)
/// and converts to legacy format for backward compatibility.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_directory(
    path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::DefaultPolicyStoreLoader;
    use crate::common::policy_store::PhysicalVfs;

    // Create a physical VFS
    let physical_vfs = PhysicalVfs::new();

    // Use the DefaultPolicyStoreLoader to load from the directory
    let loader = DefaultPolicyStoreLoader::new(physical_vfs);
    let loaded = loader
        .load_directory(path.to_string_lossy().as_ref())
        .map_err(|e| {
            PolicyStoreLoadError::Directory(format!("Failed to load from directory: {}", e))
        })?;

    // Get the policy store ID from the metadata
    let store_id = loaded.metadata.policy_store.id.clone();

    // Convert to legacy format using PolicyStoreManager
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
    })
}

/// Loads the policy store from a directory structure.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
async fn load_policy_store_from_directory(
    _path: &Path,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    Err(PolicyStoreLoadError::Directory(
        "Loading from directory is not supported in WASM.".to_string(),
    ))
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
