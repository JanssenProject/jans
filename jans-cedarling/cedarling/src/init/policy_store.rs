// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::path::Path;
use std::{fs, io};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;
use crate::common::policy_store::manager::PolicyStoreManager;
use crate::common::policy_store::{ConversionError, PolicyStore, PolicyStoreWithID};
use crate::http::cache_headers::CacheHeadersState;
use crate::http::{HttpClient, HttpClientError};

// ZIP local-file-header magic bytes.
pub(super) const ZIP_MAGIC: [u8; 4] = [0x50, 0x4B, 0x03, 0x04];

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
    strict_schema_validation: bool,
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
        .map(|(k, v)| {
            let store: PolicyStore = v.to_owned().into();
            PolicyStoreWithID {
                id: k.to_owned(),
                store,
                metadata: None,
            }
        })
        .next();

    match policy_store_option {
        Some(policy_store) => {
            if strict_schema_validation && policy_store.schema.is_none() {
                return Err(PolicyStoreLoadError::InvalidStore(
                    "missing required schema in policy store".to_string(),
                ));
            }
            Ok(policy_store)
        },
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
    pub validators: CacheHeadersState,
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies. The
/// `strict_schema_validation` flag is forwarded to every parser path so that
/// the bootstrap-time invariant "policy store must declare a schema" is
/// enforced consistently regardless of which source the operator picked
/// (file, URL, archive, etc.).
pub(crate) async fn load_policy_store(
    config: &PolicyStoreConfig,
    http_client: &HttpClient,
    strict_schema_validation: bool,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    let loaded = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            let agama_policy_store = serde_json::from_str::<LegacyAgamaPolicyStore>(policy_json)
                .map_err(PolicyStoreLoadError::ParseJson)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store, strict_schema_validation)?,
                body_hash: None,
                validators: CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            let agama_policy_store = serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store, strict_schema_validation)?,
                body_hash: None,
                validators: CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::LockServer(policy_store_uri) => {
            load_policy_store_from_lock_master(
                policy_store_uri,
                http_client,
                strict_schema_validation,
            )
            .await?
        },
        PolicyStoreSource::FileJson(path) => {
            let policy_json = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store = serde_json::from_str::<LegacyAgamaPolicyStore>(&policy_json)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store, strict_schema_validation)?,
                body_hash: None,
                validators: CacheHeadersState::default(),
            }
        },
        PolicyStoreSource::FileYaml(path) => {
            let policy_yaml = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            let agama_policy_store =
                serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(&policy_yaml)?;
            LoadedPolicyStore {
                store: extract_first_policy_store(&agama_policy_store, strict_schema_validation)?,
                body_hash: None,
                validators: CacheHeadersState::default(),
            }
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::CjarFile(path) => LoadedPolicyStore {
            store: load_policy_store_from_cjar_file(path, strict_schema_validation).await?,
            body_hash: None,
            validators: CacheHeadersState::default(),
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::CjarFile(path) => LoadedPolicyStore {
            store: load_policy_store_from_cjar_file(path)?,
            body_hash: None,
            validators: CacheHeadersState::default(),
        },
        PolicyStoreSource::CjarUrl(url) => {
            load_policy_store_from_cjar_url(url, http_client, strict_schema_validation).await?
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::Directory(path) => LoadedPolicyStore {
            store: load_policy_store_from_directory(path, strict_schema_validation).await?,
            body_hash: None,
            validators: CacheHeadersState::default(),
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::Directory(path) => LoadedPolicyStore {
            store: load_policy_store_from_directory(path)?,
            body_hash: None,
            validators: CacheHeadersState::default(),
        },
        PolicyStoreSource::ArchiveBytes(bytes) => LoadedPolicyStore {
            store: load_policy_store_from_archive_bytes(bytes, strict_schema_validation)?,
            body_hash: None,
            validators: CacheHeadersState::default(),
        },
        PolicyStoreSource::Uri(uri) => {
            load_policy_store_from_uri(uri, http_client, strict_schema_validation).await?
        },
    };

    Ok(loaded)
}

/// Loads the policy store from a URI with ZIP magic byte-based format detection.
async fn load_policy_store_from_uri(
    uri: &str,
    http_client: &HttpClient,
    strict_schema_validation: bool,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    let response = http_client.get_with_retry(uri).await?;

    let validators = CacheHeadersState::from_headers(response.headers(), chrono::Utc::now());

    let bytes = http_client.read_response_capped(response).await?;
    let body_hash = crate::init::policy_store_refresh::body_hash(&bytes);

    if bytes.starts_with(&ZIP_MAGIC) {
        return Ok(LoadedPolicyStore {
            store: parse_cjar_bytes(&bytes, strict_schema_validation).await?,
            body_hash: Some(body_hash),
            validators,
        });
    }

    let store = parse_lock_master_bytes(&bytes, strict_schema_validation)?;
    Ok(LoadedPolicyStore {
        store,
        body_hash: Some(body_hash),
        validators,
    })
}

/// Loads the policy store from the Lock Master.
///
/// The URI is from the `CEDARLING_POLICY_STORE_URI` bootstrap property.
async fn load_policy_store_from_lock_master(
    uri: &str,
    http_client: &HttpClient,
    strict_schema_validation: bool,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    // Fetch via `get_with_retry` so we can capture both the response headers
    // (for seeding `RefreshState.validators` — `ETag` / `Last-Modified` /
    // `Cache-Control`) and the raw bytes (for seeding `last_body_hash`).
    // Magic-byte sniffing in the refresh worker handles the case where Lock
    // Server starts serving `.cjar` archives in the future.
    let response = http_client.get_with_retry(uri).await?;
    let validators = CacheHeadersState::from_headers(response.headers(), chrono::Utc::now());
    // Route through the client's capped reader so the bootstrap load honors
    // `CEDARLING_HTTP_MAX_RESPONSE_SIZE` — a multi-GB body on the very first
    // policy-store fetch shouldn't be able to exhaust memory.
    let bytes = http_client.read_response_capped(response).await?;
    let store = parse_lock_master_bytes(&bytes, strict_schema_validation)?;
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
    strict_schema_validation: bool,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    let agama_policy_store: LegacyAgamaPolicyStore = serde_json::from_slice(bytes)?;
    extract_first_policy_store(&agama_policy_store, strict_schema_validation)
}

/// Parses already-fetched `.cjar` archive bytes into a [`PolicyStoreWithID`].
/// The cfg gate around the `convert_to_legacy` step is localized via the
/// `convert_archive_to_legacy` helper so the load → metadata → convert flow
/// can be shared. Native callers offload the CPU-heavy conversion to a
/// blocking thread; WASM is single-threaded and calls it inline.
pub(crate) async fn parse_cjar_bytes(
    bytes: &[u8],
    strict_schema_validation: bool,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    let loaded = loader::load_policy_store_archive_bytes(bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    #[cfg(not(target_arch = "wasm32"))]
    let legacy_store = tokio::task::spawn_blocking(move || {
        convert_archive_to_legacy(loaded, strict_schema_validation)
    })
    .await
    .map_err(|e| PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}")))??;
    #[cfg(target_arch = "wasm32")]
    let legacy_store = convert_archive_to_legacy(loaded, strict_schema_validation)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

/// Synchronous shared helper: runs `PolicyStoreManager::convert_to_legacy`
/// and lifts the error into [`PolicyStoreLoadError`]. Always synchronous —
/// the platform-specific "offload to a blocking thread vs. run inline"
/// decision is made by callers, since WASM has no `spawn_blocking` and an
/// `async` wrapper there would have nothing to await.
fn convert_archive_to_legacy(
    loaded: crate::common::policy_store::loader::LoadedPolicyStore,
    strict_schema_validation: bool,
) -> Result<crate::common::policy_store::PolicyStore, PolicyStoreLoadError> {
    PolicyStoreManager::convert_to_legacy(loaded, strict_schema_validation).map_err(Into::into)
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
///
/// Uses the `load_policy_store_archive` function from the loader module
/// and converts to legacy format for backward compatibility.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_cjar_file(
    path: &Path,
    strict_schema_validation: bool,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    let loaded = loader::load_policy_store_archive(path)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format in a blocking task (schema parsing is CPU-heavy)
    let legacy_store = tokio::task::spawn_blocking(move || {
        convert_archive_to_legacy(loaded, strict_schema_validation)
    })
    .await
    .map_err(|e| PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}")))??;

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
/// Fetches the archive via HTTP (capturing response headers for the
/// `validators` seed and body bytes for the `body_hash` seed), loads it via
/// `load_policy_store_archive_bytes`, and converts to legacy format. The CPU-
/// heavy schema-parsing step in `convert_to_legacy` is shared via the
/// `convert_archive_to_legacy` helper; the only platform-specific concern is
/// whether to wrap that call in `tokio::task::spawn_blocking` (native) or
/// run it inline (WASM, which is single-threaded).
async fn load_policy_store_from_cjar_url(
    url: &str,
    http_client: &HttpClient,
    strict_schema_validation: bool,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Fetch via `get_with_retry` to capture response headers (for the refresh
    // worker's initial `validators` seed) alongside the body bytes (for the
    // `last_body_hash` seed).
    let response = http_client
        .get_with_retry(url)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to fetch archive: {e}")))?;
    let validators = CacheHeadersState::from_headers(response.headers(), chrono::Utc::now());
    // Cap-aware body read: `CEDARLING_HTTP_MAX_RESPONSE_SIZE` bounds the
    // archive download so an oversized `.cjar` URL can't exhaust memory.
    let bytes = http_client
        .read_response_capped(response)
        .await
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to read archive body: {e}")))?;

    let body_hash = crate::init::policy_store_refresh::body_hash(&bytes);

    let loaded = loader::load_policy_store_archive_bytes(&bytes)
        .map_err(|e| PolicyStoreLoadError::Archive(format!("Failed to load from archive: {e}")))?;

    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    #[cfg(not(target_arch = "wasm32"))]
    let legacy_store = tokio::task::spawn_blocking(move || {
        convert_archive_to_legacy(loaded, strict_schema_validation)
    })
    .await
    .map_err(|e| PolicyStoreLoadError::Archive(format!("Conversion task panicked: {e}")))??;
    #[cfg(target_arch = "wasm32")]
    let legacy_store = convert_archive_to_legacy(loaded, strict_schema_validation)?;

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
    strict_schema_validation: bool,
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
    let legacy_store = tokio::task::spawn_blocking(move || {
        convert_archive_to_legacy(loaded, strict_schema_validation)
    })
    .await
    .map_err(|e| PolicyStoreLoadError::Directory(format!("Conversion task panicked: {e}")))??;

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
    strict_schema_validation: bool,
) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
    use crate::common::policy_store::loader;

    // Load from bytes (works in both native and WASM)
    let loaded = loader::load_policy_store_archive_bytes(bytes).map_err(|e| {
        PolicyStoreLoadError::Archive(format!("Failed to load from archive bytes: {e}"))
    })?;

    // Get the policy store ID and metadata
    let store_id = loaded.metadata.policy_store.id.clone();
    let store_metadata = loaded.metadata.clone();

    // Convert to legacy format using the shared helper
    let legacy_store = convert_archive_to_legacy(loaded, strict_schema_validation)?;

    Ok(PolicyStoreWithID {
        id: store_id,
        store: legacy_store,
        metadata: Some(store_metadata),
    })
}

#[cfg(test)]
mod test {
    use std::{path::Path, sync::LazyLock, time::Duration};

    use base64::Engine;
    use mockito::Server;
    use serde_json::json;

    use super::{extract_first_policy_store, load_policy_store};
    use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;
    use crate::{
        PolicyStoreConfig, PolicyStoreSource,
        common::policy_store::test_utils::fixtures,
        http::{HttpClient, HttpClientConfig},
    };

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed")
    });

    // NOTE: we probably don't need to test if the deserialization for JSON and YAML
    // works correctly anymore here since we already have tests for those in
    // src/common/policy_store/test.rs...

    fn make_full_legacy_json() -> serde_json::Value {
        let schema = base64::prelude::BASE64_STANDARD.encode(
            r#"{
                "Jans": {
                    "entityTypes": {},
                    "actions": {}
                }
            }"#,
        );
        json!({
            "cedar_version": "v4.0.0",
            "policy_stores": {
                "test": {
                    "name": "test",
                    "schema": schema,
                    "policies": {}
                }
            }
        })
    }

    fn make_no_schema_legacy_json() -> serde_json::Value {
        json!({
            "cedar_version": "v4.0.0",
            "policy_stores": {
                "test": {
                    "name": "test",
                    "policies": {}
                }
            }
        })
    }

    fn make_null_schema_legacy_json() -> serde_json::Value {
        json!({
            "cedar_version": "v4.0.0",
            "policy_stores": {
                "test": {
                    "name": "test",
                    "schema": null,
                    "policies": {}
                }
            }
        })
    }

    #[test]
    fn test_extract_first_policy_store_with_schema_strict_true() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_full_legacy_json())
            .expect("valid legacy store with schema");
        let result = extract_first_policy_store(&agama, true);
        result.expect("should succeed with schema and strict=true");
    }

    #[test]
    fn test_extract_first_policy_store_with_schema_strict_false() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_full_legacy_json())
            .expect("valid legacy store with schema");
        let result = extract_first_policy_store(&agama, false);
        result.expect("should succeed with schema and strict=false");
    }

    #[test]
    fn test_extract_first_policy_store_missing_schema_strict_true() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_no_schema_legacy_json())
            .expect("valid legacy store without schema");
        let result = extract_first_policy_store(&agama, true);
        let err = result.expect_err("should error when schema missing and strict=true");
        assert!(
            err.to_string().contains("missing required schema"),
            "error should mention missing schema, got: {err}"
        );
    }

    #[test]
    fn test_extract_first_policy_store_missing_schema_strict_false() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_no_schema_legacy_json())
            .expect("valid legacy store without schema");
        let result = extract_first_policy_store(&agama, false);
        result.expect("should succeed when schema missing and strict=false");
    }

    #[test]
    fn test_extract_first_policy_store_null_schema_strict_true() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_null_schema_legacy_json())
            .expect("valid legacy store with null schema");
        let result = extract_first_policy_store(&agama, true);
        let err = result.expect_err("should error when schema is null and strict=true");
        assert!(
            err.to_string().contains("missing required schema"),
            "error should mention missing schema, got: {err}"
        );
    }

    #[test]
    fn test_extract_first_policy_store_null_schema_strict_false() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_null_schema_legacy_json())
            .expect("valid legacy store with null schema");
        let result = extract_first_policy_store(&agama, false);
        result.expect("should succeed when schema is null and strict=false");
    }

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
            true,
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
            true,
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
            true,
        )
        .await
        .expect("Should load policy store from Lock Master file");

        mock_endpoint.assert();
    }

    #[tokio::test]
    async fn can_load_from_uri_with_json_content_type() {
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
                source: PolicyStoreSource::Uri(uri),
                refresh_interval_secs: 0,
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect("Should load policy store from URI with JSON content-type");

        mock_endpoint.assert();
    }
    #[tokio::test]
    async fn can_load_from_uri_missing_content_type_uses_magic_bytes() {
        let mut mock_server = Server::new_async().await;

        let archive_bytes = fixtures::minimal_valid()
            .build_archive()
            .expect("Should build test archive");

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store")
            .with_status(200)
            .with_body(archive_bytes)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store", mock_server.url()).to_string();

        load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::Uri(uri),
                refresh_interval_secs: 0,
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect("Should load policy store from URI with magic bytes and no content-type");

        mock_endpoint.assert();
    }

    #[tokio::test]
    async fn can_load_from_uri_with_octet_stream_content_type() {
        let mut mock_server = Server::new_async().await;

        let archive_bytes = fixtures::minimal_valid()
            .build_archive()
            .expect("Should build test archive");

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store")
            .with_status(200)
            .with_header("content-type", "application/octet-stream")
            .with_body(archive_bytes)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store", mock_server.url()).to_string();

        load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::Uri(uri),
                refresh_interval_secs: 0,
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect("Should load policy store from URI with octet-stream content-type");

        mock_endpoint.assert();
    }
}
