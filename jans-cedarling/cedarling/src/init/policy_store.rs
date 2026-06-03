// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::path::Path;
use std::{fs, io};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;
use crate::common::policy_store::manager::PolicyStoreManager;
use crate::common::policy_store::{ConversionError, PolicyStoreWithID};
use crate::http::{HttpClient, HttpClientError};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    /// Failed to parse policy store from JSON string.
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    /// Failed to parse policy store from YAML string.
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yaml_ng::Error),
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

// LegacyAgamaPolicyStore contains the structure to accommodate several policies,
// and this code for now assumes that there is only ever one policy store,
// extract the first 'policy_stores' entry.
fn extract_first_policy_store(
    agama_policy_store: &LegacyAgamaPolicyStore,
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
            store: v.to_owned().into(), // Convert LegacyPolicyStore -> PolicyStore
            metadata: None,             // Legacy format doesn't include metadata
        })
        .next();

    match policy_store_option {
        Some(policy_store) => Ok(policy_store.clone()),
        None => Err(PolicyStoreLoadError::InvalidStore(
            "error retrieving first policy_stores element".into(),
        )),
    }
}

/// Outcome of [`load_policy_store`]: the parsed store plus, for URL-based
/// sources, both the body-hash and the parsed cache validators (`ETag`,
/// `Last-Modified`, `max-age` / `Expires`) captured from the bootstrap
/// response. The refresh worker seeds **both** `last_body_hash` and
/// `validators` with these values so the first periodic tick can:
///
/// - send a conditional GET that may return `304 Not Modified` (no body
///   downloaded at all — the optimal path), and
/// - failing that, still short-circuit on the body-hash compare when the
///   upstream returns a byte-identical body despite ignored conditional
///   headers.
///
/// Both are `None` / empty for non-URL sources (local files, inline JSON/YAML,
/// in-memory archive bytes) — the refresh worker doesn't spawn there anyway.
pub(crate) struct LoadedPolicyStore {
    pub store: PolicyStoreWithID,
    pub body_hash: Option<u64>,
    pub validators: crate::http::cache_headers::CacheHeadersState,
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) async fn load_policy_store(
    config: &PolicyStoreConfig,
    http_client: &HttpClient,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    let loaded = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            let agama_policy_store = serde_json::from_str::<LegacyAgamaPolicyStore>(policy_json)
                .map_err(PolicyStoreLoadError::ParseJson)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store)?,
                body_hash: None,
                validators: crate::http::cache_headers::CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            let agama_policy_store = serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store)?,
                body_hash: None,
                validators: crate::http::cache_headers::CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::LockServer(policy_store_uri) => {
            load_policy_store_from_lock_master(policy_store_uri, http_client).await?
        },
        PolicyStoreSource::FileJson(path) => {
            let policy_json = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store = serde_json::from_str::<LegacyAgamaPolicyStore>(&policy_json)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store)?,
                body_hash: None,
                validators: crate::http::cache_headers::CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::FileYaml(path) => {
            let policy_yaml = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store =
                serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(&policy_yaml)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store)?,
                body_hash: None,
                validators: crate::http::cache_headers::CacheHeadersState::default(),
            }
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::CjarFile(path) => LoadedPolicyStore {
            store: load_policy_store_from_cjar_file(path).await?,
            body_hash: None,
            validators: crate::http::cache_headers::CacheHeadersState::default(),
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::CjarFile(path) => LoadedPolicyStore {
            store: load_policy_store_from_cjar_file(path)?,
            body_hash: None,
            validators: crate::http::cache_headers::CacheHeadersState::default(),
        },
        PolicyStoreSource::CjarUrl(url) => {
            load_policy_store_from_cjar_url(url, http_client).await?
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::Directory(path) => LoadedPolicyStore {
            store: load_policy_store_from_directory(path).await?,
            body_hash: None,
            validators: crate::http::cache_headers::CacheHeadersState::default(),
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::Directory(path) => LoadedPolicyStore {
            store: load_policy_store_from_directory(path)?,
            body_hash: None,
            validators: crate::http::cache_headers::CacheHeadersState::default(),
        },
        PolicyStoreSource::ArchiveBytes(bytes) => LoadedPolicyStore {
            store: load_policy_store_from_archive_bytes(bytes)?,
            body_hash: None,
            validators: crate::http::cache_headers::CacheHeadersState::default(),
        },
    };

    Ok(loaded)
}

/// Loads the policy store from the Lock Master.
///
/// The URI is from the `CEDARLING_POLICY_STORE_URI` bootstrap property.
async fn load_policy_store_from_lock_master(
    uri: &str,
    http_client: &HttpClient,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    // Fetch via `get_with_retry` so we can capture both the response headers
    // (for seeding `RefreshState.validators` — `ETag` / `Last-Modified` /
    // `Cache-Control`) and the raw bytes (for seeding `last_body_hash`).
    // Magic-byte sniffing in the refresh worker handles the case where Lock
    // Server starts serving `.cjar` archives in the future.
    let response = http_client.get_with_retry(uri).await?;
    let status = response.status();
    let validators = crate::http::cache_headers::CacheHeadersState::from_headers(
        response.headers(),
        chrono::Utc::now(),
    );
    let bytes = response.bytes().await.map_err(|e| {
        http_utils::HttpRequestError::new(
            http_utils::HttpRequestReasonError::DecodeResponseBytes(e),
            Some(status),
        )
    })?;
    let store = parse_lock_master_bytes(&bytes)?;
    Ok(LoadedPolicyStore {
        store,
        body_hash: Some(crate::init::policy_store_refresh::body_hash(&bytes)),
        validators,
    })
}

/// Parses already-fetched Lock-Master JSON bytes into a [`PolicyStoreWithID`].
/// Used by the policy-store refresh worker, which performs the HTTP fetch itself
/// to be able to send conditional-request headers.
pub(crate) fn parse_lock_master_bytes(
    bytes: &[u8],
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let agama_policy_store: LegacyAgamaPolicyStore = serde_json::from_slice(bytes)?;
    extract_first_policy_store(&agama_policy_store)
}

/// Parses already-fetched `.cjar` archive bytes into a [`PolicyStoreWithID`].
/// On native targets the schema-parsing step is offloaded to a blocking thread;
/// on WASM it runs inline since `spawn_blocking` is unavailable.
#[cfg(not(target_arch = "wasm32"))]
pub(crate) async fn parse_cjar_bytes(
    bytes: &[u8],
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    let loaded = loader::load_policy_store_archive_bytes(bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    let legacy_store =
        tokio::task::spawn_blocking(move || PolicyStoreManager::convert_to_legacy(loaded))
            .await
            .map_err(|e| {
                PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}"))
            })??;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

/// Parses already-fetched `.cjar` archive bytes into a [`PolicyStoreWithID`] —
/// WASM build, single-threaded.
#[cfg(target_arch = "wasm32")]
pub(crate) async fn parse_cjar_bytes(
    bytes: &[u8],
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    let loaded = loader::load_policy_store_archive_bytes(bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
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

    // Convert to legacy format in a blocking task (schema parsing is CPU-heavy)
    let legacy_store =
        tokio::task::spawn_blocking(move || PolicyStoreManager::convert_to_legacy(loaded))
            .await
            .map_err(|e| {
                PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}"))
            })??;

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
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_cjar_url(
    url: &str,
    http_client: &HttpClient,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Fetch via `get_with_retry` to capture response headers (for the refresh
    // worker's initial `validators` seed) alongside the body bytes (for the
    // `last_body_hash` seed).
    let response = http_client.get_with_retry(url).await.map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to fetch archive: {e}"))
    })?;
    let status = response.status();
    let validators = crate::http::cache_headers::CacheHeadersState::from_headers(
        response.headers(),
        chrono::Utc::now(),
    );
    let bytes: Vec<u8> = response
        .bytes()
        .await
        .map_err(|e| {
            http_utils::HttpRequestError::new(
                http_utils::HttpRequestReasonError::DecodeResponseBytes(e),
                Some(status),
            )
        })?
        .to_vec();

    let body_hash = crate::init::policy_store_refresh::body_hash(&bytes);

    // Load from bytes (works in both native and WASM)
    let loaded = loader::load_policy_store_archive_bytes(&bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format in a blocking task (schema parsing is CPU-heavy)
    let legacy_store =
        tokio::task::spawn_blocking(move || PolicyStoreManager::convert_to_legacy(loaded))
            .await
            .map_err(|e| {
                PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}"))
            })??;

    Ok(LoadedPolicyStore {
        store: PolicyStoreWithID {
            id: store_id,
            store: legacy_store,
            metadata: Some(store_metadata),
        },
        body_hash: Some(body_hash),
        validators,
    })
}

/// Loads the policy store from a Cedar Archive (.cjar) URL.
/// WASM version - no `spawn_blocking` available.
#[cfg(target_arch = "wasm32")]
async fn load_policy_store_from_cjar_url(
    url: &str,
    http_client: &HttpClient,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Fetch via `get_with_retry` to capture response headers (for the refresh
    // worker's initial `validators` seed) alongside the body bytes (for the
    // `last_body_hash` seed).
    let response = http_client.get_with_retry(url).await.map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to fetch archive: {e}"))
    })?;
    let status = response.status();
    let validators = crate::http::cache_headers::CacheHeadersState::from_headers(
        response.headers(),
        chrono::Utc::now(),
    );
    let bytes: Vec<u8> = response
        .bytes()
        .await
        .map_err(|e| {
            http_utils::HttpRequestError::new(
                http_utils::HttpRequestReasonError::DecodeResponseBytes(e),
                Some(status),
            )
        })?
        .to_vec();

    let body_hash = crate::init::policy_store_refresh::body_hash(&bytes);

    // Load from bytes (works in both native and WASM)
    let loaded = loader::load_policy_store_archive_bytes(&bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format (WASM runs single-threaded, no spawn_blocking)
    let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;

    Ok(LoadedPolicyStore {
        store: PolicyStoreWithID {
            id: store_id,
            store: legacy_store,
            metadata: Some(store_metadata),
        },
        body_hash: Some(body_hash),
        validators,
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

    // Convert to legacy format in a blocking task (schema parsing is CPU-heavy)
    let legacy_store =
        tokio::task::spawn_blocking(move || PolicyStoreManager::convert_to_legacy(loaded))
            .await
            .map_err(|e| {
                PolicyStoreLoadError::Directory(format!("Conversion task panicked: {e}"))
            })??;

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
    use std::{path::Path, sync::LazyLock, time::Duration};

    use mockito::Server;

    use super::load_policy_store;
    use crate::{
        PolicyStoreConfig,
        http::{HttpClient, HttpClientConfig},
    };

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
        })
        .expect("http client should be constructed")
    });

    // NOTE: we probably don't need to test if the deserialization for JSON and YAML
    // works correctly anymore here since we already have tests for those in
    // src/common/policy_store/test.rs...

    #[tokio::test]
    async fn can_load_from_json_file() {
        load_policy_store(
            &PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileJson(
                    Path::new("../test_files/policy-store_generated.json").into(),
                ),
                ..Default::default()
            },
            &HTTP_CLIENT,
        )
        .await
        .expect("Should load policy store from JSON file");
    }

    #[tokio::test]
    async fn can_load_from_yaml_file() {
        load_policy_store(
            &PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileYaml(
                    Path::new("../test_files/policy-store_ok.yaml").into(),
                ),
                ..Default::default()
            },
            &HTTP_CLIENT,
        )
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

        load_policy_store(
            &PolicyStoreConfig {
                source: crate::PolicyStoreSource::LockServer(uri),
                ..Default::default()
            },
            &HTTP_CLIENT,
        )
        .await
        .expect("Should load policy store from Lock Master file");

        mock_endpoint.assert();
    }
}
