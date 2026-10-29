// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::path::Path;
use std::{fs, io};

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::archive_handler::ArchiveLimits;
use crate::common::policy_store::errors::{PolicyStoreError, ValidationError};
use crate::common::policy_store::formats::{
    FormatReport, ParseStoreError, ParsedPolicyStore, ReadStoreError,
};
use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;
use crate::common::policy_store::loader;
use crate::common::policy_store::migration::MigrationError;
use crate::common::policy_store::validator::MetadataValidator;
use crate::common::policy_store::{ConversionError, PolicyStore, PolicyStoreWithID};
use crate::http::cache_headers::CacheHeadersState;
use crate::http::{HttpClient, HttpClientError};

use super::policy_store_signature::{
    ArchiveVerifier, SignatureError, VerifiedArchive, check_if_enabled,
};

// ZIP local-file-header magic bytes.
pub(super) const ZIP_MAGIC: [u8; 4] = [0x50, 0x4B, 0x03, 0x04];

#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    /// Legacy JSON policy store format is no longer supported.
    #[error(
        "Legacy JSON policy store format is no longer supported. Please migrate to the \
         folder-based policy store format (.cjar archive or directory)."
    )]
    LegacyJsonNotSupported,
    /// Failed to parse policy store from YAML string.
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_saphyr::Error),
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
    /// The store could not be migrated to the current spec version.
    #[error("Failed to migrate the policy store: {0}")]
    Migration(MigrationError),
    /// Failed to load policy store from archive.
    #[error("Failed to load policy store from archive: {0}")]
    Archive(String),
    /// Failed to load policy store from directory.
    #[error("Failed to load policy store from directory: {0}")]
    Directory(String),
    /// Policy store validation error.
    #[error("Policy store validation error: {0}")]
    Validation(#[from] ValidationError),
    /// The `.cjar` failed its Sigstore signature check.
    #[error(transparent)]
    Signature(#[from] SignatureError),
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
        .map(
            |(k, v)| -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
                let store = PolicyStore::try_from(v.to_owned()).map_err(ConversionError::from)?;

                let metadata = crate::common::policy_store::metadata::PolicyStoreMetadata {
                    cedar_version: agama_policy_store
                        .cedar_version
                        .strip_prefix('v')
                        .unwrap_or(&agama_policy_store.cedar_version)
                        .to_string(),
                    policy_store: crate::common::policy_store::metadata::PolicyStoreInfo {
                        id: k.clone(),
                        name: k.clone(),
                        version: v.version.clone().unwrap_or_default(),
                        description: None,
                        created_date: None,
                        updated_date: None,
                    },
                };

                Ok(PolicyStoreWithID::without_archive(
                    k.to_owned(),
                    store,
                    Some(metadata),
                ))
            },
        )
        .next()
        .transpose()?;

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
#[cfg_attr(test, derive(Debug))]
pub(crate) struct LoadedPolicyStore {
    pub store: PolicyStoreWithID,
    /// What the store's format reported; logged at bootstrap, then dropped.
    pub format: FormatReport,
    pub body_hash: Option<u64>,
    pub validators: CacheHeadersState,
    /// Signer of the archive, when its signature was checked.
    pub signed_by: Option<VerifiedArchive>,
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
    let verifier = ArchiveVerifier::from_config(&config.signature);
    load_verified_policy_store(
        config,
        http_client,
        strict_schema_validation,
        verifier.as_ref(),
    )
    .await
}

/// [`load_policy_store`] with the signature verifier supplied, so tests can use their own trust root.
async fn load_verified_policy_store(
    config: &PolicyStoreConfig,
    http_client: &HttpClient,
    strict_schema_validation: bool,
    verifier: Option<&ArchiveVerifier>,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    let limits = config.archive_limits();

    // Checked here rather than when decoding, so a `BootstrapConfig` built directly can't skip it.
    if verifier.is_some() && !config.source.is_archive() {
        return Err(SignatureError::UnsupportedSource.into());
    }

    let loaded = match &config.source {
        PolicyStoreSource::Yaml(policy_yaml) => {
            if crate::common::policy_store::is_json_content(policy_yaml) {
                return Err(PolicyStoreLoadError::LegacyJsonNotSupported);
            }
            let agama_policy_store = serde_saphyr::from_str::<LegacyAgamaPolicyStore>(policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            process_legacy_agama_store(&agama_policy_store, strict_schema_validation)?
        },
        PolicyStoreSource::LockServer(uri) | PolicyStoreSource::Uri(uri) => {
            load_policy_store_from_uri(uri, http_client, strict_schema_validation, limits, verifier)
                .await?
        },
        PolicyStoreSource::FileYaml(path) => {
            let policy_yaml = fs::read_to_string(path)
                .map_err(|e| PolicyStoreLoadError::ParseFile(path.clone().into(), e))?;
            if crate::common::policy_store::is_json_content(&policy_yaml) {
                return Err(PolicyStoreLoadError::LegacyJsonNotSupported);
            }
            let agama_policy_store = serde_saphyr::from_str::<LegacyAgamaPolicyStore>(&policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            process_legacy_agama_store(&agama_policy_store, strict_schema_validation)?
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::CjarFile(path) => {
            load_policy_store_from_cjar_file(
                path,
                http_client,
                strict_schema_validation,
                limits,
                verifier,
            )
            .await?
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::CjarFile(path) => {
            let parsed = load_policy_store_from_cjar_file(path, strict_schema_validation, limits)?;
            LoadedPolicyStore {
                store: parsed.store,
                format: parsed.format,
                body_hash: None,
                validators: CacheHeadersState::default(),
                signed_by: None,
            }
        },
        PolicyStoreSource::CjarUrl(url) => {
            load_policy_store_from_cjar_url(
                url,
                http_client,
                strict_schema_validation,
                limits,
                verifier,
            )
            .await?
        },
        #[cfg(not(target_arch = "wasm32"))]
        PolicyStoreSource::Directory(path) => {
            let parsed = load_policy_store_from_directory(path, strict_schema_validation).await?;
            LoadedPolicyStore {
                store: parsed.store,
                format: parsed.format,
                body_hash: None,
                validators: CacheHeadersState::default(),
                signed_by: None,
            }
        },
        #[cfg(target_arch = "wasm32")]
        PolicyStoreSource::Directory(path) => {
            let parsed = load_policy_store_from_directory(path, strict_schema_validation)?;
            LoadedPolicyStore {
                store: parsed.store,
                format: parsed.format,
                body_hash: None,
                validators: CacheHeadersState::default(),
                signed_by: None,
            }
        },
        PolicyStoreSource::ArchiveBytes(bytes) => {
            let signed_by = check_if_enabled(verifier, http_client, None, bytes).await?;
            // The config keeps its bytes, so the read takes a copy it can own.
            let parsed = parse_cjar_bytes(bytes.clone(), strict_schema_validation, limits).await?;
            LoadedPolicyStore {
                store: parsed.store,
                format: parsed.format,
                body_hash: None,
                validators: CacheHeadersState::default(),
                signed_by,
            }
        },
    };

    Ok(loaded)
}

fn process_legacy_agama_store(
    agama_policy_store: &LegacyAgamaPolicyStore,
    strict_schema_validation: bool,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    MetadataValidator::validate_legacy_store(agama_policy_store)
        .map_err(PolicyStoreLoadError::Validation)?;
    Ok(LoadedPolicyStore {
        store: extract_first_policy_store(agama_policy_store, strict_schema_validation)?,
        format: FormatReport::default(),
        body_hash: None,
        validators: CacheHeadersState::default(),
        signed_by: None,
    })
}

/// Loads the policy store from a URI with ZIP magic byte-based format detection.
async fn load_policy_store_from_uri(
    uri: &str,
    http_client: &HttpClient,
    strict_schema_validation: bool,
    limits: ArchiveLimits,
    verifier: Option<&ArchiveVerifier>,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    let response = http_client.get_with_retry(uri).await?;

    let validators = CacheHeadersState::from_headers(response.headers(), chrono::Utc::now());

    let bytes = http_client.read_response_capped(response).await?;
    let body_hash = crate::init::policy_store_refresh::body_hash(&bytes);

    if bytes.starts_with(&ZIP_MAGIC) {
        let signed_by = check_if_enabled(verifier, http_client, Some(uri), &bytes).await?;
        let parsed = parse_cjar_bytes(bytes, strict_schema_validation, limits).await?;
        return Ok(LoadedPolicyStore {
            store: parsed.store,
            format: parsed.format,
            body_hash: Some(body_hash),
            validators,
            signed_by,
        });
    }

    if crate::common::policy_store::is_json_bytes(&bytes) {
        return Err(PolicyStoreLoadError::LegacyJsonNotSupported);
    }

    Err(PolicyStoreLoadError::Archive(
        "Response body from URI is not a valid Cedar Archive (.cjar)".to_string(),
    ))
}

/// Parses `.cjar` archive bytes, fetched or supplied, into a [`PolicyStoreWithID`].
///
/// Takes the bytes by value so the archive is read in place rather than copied.
/// Every `.cjar` source, refresh included, passes through here, so every one gets
/// a `bundle_hash` of exactly the bytes that are parsed.
pub(crate) async fn parse_cjar_bytes(
    bytes: Vec<u8>,
    strict_schema_validation: bool,
    limits: ArchiveLimits,
) -> Result<ParsedPolicyStore, PolicyStoreLoadError> {
    read_store_offloaded(
        move || {
            loader::read_policy_store_archive_bytes(bytes, strict_schema_validation, limits)
                .map_err(|e| map_read_err(e, true))
        },
        PolicyStoreLoadError::Archive,
    )
    .await
}

/// Runs a whole directory / `.cjar` store read off the async executor.
///
/// Once the path or bytes are in hand, everything left is blocking I/O or CPU
/// work: unzipping, reading files, parsing the schema and policies, building
/// the runtime store. Running it as one blocking task keeps a large store from
/// stalling a runtime worker, which matters most for background refreshes that
/// share the runtime with authorization requests. WASM is single-threaded and
/// runs it inline.
///
/// `panic_err` picks the error variant reported if the blocking task panics.
#[cfg_attr(target_arch = "wasm32", allow(clippy::unused_async))]
async fn read_store_offloaded(
    read: impl FnOnce() -> Result<ParsedPolicyStore, PolicyStoreLoadError> + Send + 'static,
    panic_err: fn(String) -> PolicyStoreLoadError,
) -> Result<ParsedPolicyStore, PolicyStoreLoadError> {
    #[cfg(not(target_arch = "wasm32"))]
    {
        tokio::task::spawn_blocking(read)
            .await
            .map_err(|e| panic_err(format!("Policy store read task panicked: {e}")))?
    }
    #[cfg(target_arch = "wasm32")]
    {
        let _ = panic_err;
        read()
    }
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_cjar_file(
    path: &Path,
    http_client: &HttpClient,
    strict_schema_validation: bool,
    limits: ArchiveLimits,
    verifier: Option<&ArchiveVerifier>,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
    // Read whole, like every other `.cjar` source: the verified bytes, the
    // hashed bytes (`bundle_hash`) and the parsed bytes are the same bytes.
    let bytes = read_cjar_file_offloaded(path, limits).await?;
    let signed_by =
        check_if_enabled(verifier, http_client, Some(&path.to_string_lossy()), &bytes).await?;
    let parsed = parse_cjar_bytes(bytes, strict_schema_validation, limits).await?;
    Ok(LoadedPolicyStore {
        store: parsed.store,
        format: parsed.format,
        body_hash: None,
        validators: CacheHeadersState::default(),
        signed_by,
    })
}

/// [`read_cjar_file`] on the blocking pool, so a large archive does not stall a
/// runtime worker.
#[cfg(not(target_arch = "wasm32"))]
async fn read_cjar_file_offloaded(
    path: &Path,
    limits: ArchiveLimits,
) -> Result<Vec<u8>, PolicyStoreLoadError> {
    let path = path.to_path_buf();
    tokio::task::spawn_blocking(move || read_cjar_file(&path, limits))
        .await
        .map_err(|e| {
            PolicyStoreLoadError::Archive(format!("Policy store read task panicked: {e}"))
        })?
}

/// Reads a `.cjar` into memory, so the verified bytes are the parsed bytes.
#[cfg(not(target_arch = "wasm32"))]
fn read_cjar_file(path: &Path, limits: ArchiveLimits) -> Result<Vec<u8>, PolicyStoreLoadError> {
    let to_err = |e| PolicyStoreLoadError::ParseFile(path.into(), e);
    let len = fs::metadata(path).map_err(to_err)?.len();
    if limits.max_total_size != 0 && len > limits.max_total_size {
        return Err(PolicyStoreLoadError::Archive(format!(
            "{} is {len} bytes, over the {} byte limit",
            path.display(),
            limits.max_total_size
        )));
    }
    fs::read(path).map_err(to_err)
}

/// Loads the policy store from a Cedar Archive (.cjar) file.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
fn load_policy_store_from_cjar_file(
    path: &Path,
    strict_schema_validation: bool,
    limits: ArchiveLimits,
) -> Result<ParsedPolicyStore, PolicyStoreLoadError> {
    // Call the loader stub function to ensure it's used and the error variant is constructed
    match loader::read_policy_store_archive(path, strict_schema_validation, limits) {
        Err(e) => Err(PolicyStoreLoadError::Archive(format!(
            "Loading from file path is not supported in WASM. Use CjarUrl instead. Original error: {e}",
        ))),
        Ok(_) => unreachable!("WASM stub should always return an error"),
    }
}

/// Loads the policy store from a Cedar Archive (.cjar) URL.
///
/// Fetches the archive via HTTP (capturing response headers for the
/// `validators` seed and body bytes for the `body_hash` seed), then parses it
/// via [`parse_cjar_bytes`].
async fn load_policy_store_from_cjar_url(
    url: &str,
    http_client: &HttpClient,
    strict_schema_validation: bool,
    limits: ArchiveLimits,
    verifier: Option<&ArchiveVerifier>,
) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
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

    let signed_by = check_if_enabled(verifier, http_client, Some(url), &bytes).await?;
    let parsed = parse_cjar_bytes(bytes, strict_schema_validation, limits).await?;

    Ok(LoadedPolicyStore {
        store: parsed.store,
        format: parsed.format,
        body_hash: Some(body_hash),
        validators,
        signed_by,
    })
}

/// Loads the policy store from a directory structure.
#[cfg(not(target_arch = "wasm32"))]
async fn load_policy_store_from_directory(
    path: &Path,
    strict_schema_validation: bool,
) -> Result<ParsedPolicyStore, PolicyStoreLoadError> {
    let path = path.to_path_buf();
    read_store_offloaded(
        move || {
            loader::read_policy_store_directory(&path, strict_schema_validation)
                .map_err(|e| map_read_err(e, false))
        },
        PolicyStoreLoadError::Directory,
    )
    .await
}

/// Loads the policy store from a directory structure.
/// WASM version - file system access is not supported.
#[cfg(target_arch = "wasm32")]
fn load_policy_store_from_directory(
    path: &Path,
    strict_schema_validation: bool,
) -> Result<ParsedPolicyStore, PolicyStoreLoadError> {
    // Call the loader stub function to ensure it's used and the error variant is constructed
    match loader::read_policy_store_directory(path, strict_schema_validation) {
        Err(e) => Err(PolicyStoreLoadError::Directory(format!(
            "Loading from directory is not supported in WASM. Original error: {e}",
        ))),
        Ok(_) => unreachable!("WASM stub should always return an error"),
    }
}

/// Maps a failed directory / `.cjar` read; `is_archive` picks the variant for
/// failures that are not about the store's content.
fn map_read_err(e: ReadStoreError, is_archive: bool) -> PolicyStoreLoadError {
    match e {
        ReadStoreError::Read(PolicyStoreError::Validation(e))
        | ReadStoreError::Parse(ParseStoreError::Validation(e)) => {
            PolicyStoreLoadError::Validation(e)
        },
        ReadStoreError::Parse(ParseStoreError::Conversion(e)) => {
            PolicyStoreLoadError::Conversion(e)
        },
        ReadStoreError::Migration(e) => PolicyStoreLoadError::Migration(e),
        ReadStoreError::Read(PolicyStoreError::CedarParsing { file, detail }) => {
            PolicyStoreLoadError::InvalidStore(format!("Cedar parse error in {file}: {detail}"))
        },
        ReadStoreError::Read(PolicyStoreError::CedarSchemaError { file, err }) => {
            PolicyStoreLoadError::InvalidStore(format!("Cedar schema error in {file}: {err}"))
        },
        ReadStoreError::Read(e) => {
            if is_archive {
                PolicyStoreLoadError::Archive(e.to_string())
            } else {
                PolicyStoreLoadError::Directory(e.to_string())
            }
        },
    }
}

#[cfg(test)]
mod test {
    use std::{path::Path, sync::LazyLock, time::Duration};

    use base64::Engine;
    use mockito::Server;
    use serde_json::json;

    use super::{
        PolicyStoreLoadError, extract_first_policy_store, load_policy_store, read_store_offloaded,
    };
    use crate::common::policy_store::errors::ValidationError;
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
    fn test_extract_first_policy_store_legacy_id_matches_map_key() {
        let agama: LegacyAgamaPolicyStore = serde_json::from_value(make_full_legacy_json())
            .expect("valid legacy store with schema");
        let result = extract_first_policy_store(&agama, false).expect("should succeed with schema");
        assert_eq!(
            result.id, "test",
            "PolicyStoreWithID.id should equal map key"
        );
        assert_eq!(
            result
                .metadata
                .expect("legacy store should carry metadata")
                .policy_store
                .id,
            "test",
            "metadata.policy_store.id should equal map key"
        );
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

    #[test]
    fn cannot_configure_legacy_json_file() {
        use crate::{BootstrapConfig, BootstrapConfigRaw};
        let raw = BootstrapConfigRaw {
            policy_store_local_fn: Some("../test_files/policy-store_generated.json".to_string()),
            ..Default::default()
        };
        let err =
            BootstrapConfig::from_raw_config(&raw).expect_err("legacy JSON file must be rejected");
        assert!(
            matches!(
                err,
                crate::BootstrapConfigLoadingError::LegacyJsonNotSupported
            ),
            "expected LegacyJsonNotSupported, got {err:?}"
        );
    }

    #[test]
    fn cannot_configure_legacy_json_inline() {
        use crate::{BootstrapConfig, BootstrapConfigRaw};
        let raw = BootstrapConfigRaw {
            local_policy_store: Some("{}".to_string()),
            ..Default::default()
        };
        let err = BootstrapConfig::from_raw_config(&raw)
            .expect_err("legacy JSON inline must be rejected");
        assert!(
            matches!(
                err,
                crate::BootstrapConfigLoadingError::LegacyJsonNotSupported
            ),
            "expected LegacyJsonNotSupported, got {err:?}"
        );
    }

    #[tokio::test]
    async fn cannot_load_legacy_json_from_yaml_source() {
        let cases = [
            "{\"cedar_version\": \"v4.0.0\"}",
            "   \n  {\"cedar_version\": \"v4.0.0\"}",
        ];

        for case in cases {
            let err = load_policy_store(
                &PolicyStoreConfig {
                    source: PolicyStoreSource::Yaml(case.to_string()),
                    ..Default::default()
                },
                &HTTP_CLIENT,
                true,
            )
            .await
            .expect_err("legacy JSON via Yaml source must be rejected");

            assert!(
                matches!(err, super::PolicyStoreLoadError::LegacyJsonNotSupported),
                "expected LegacyJsonNotSupported for {case}, got {err:?}"
            );
        }
    }

    #[tokio::test]
    async fn cannot_load_legacy_json_from_file_yaml_source() {
        let err = load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::FileYaml(
                    Path::new("../test_files/policy-store_lock_master_ok.json").into(),
                ),
                ..Default::default()
            },
            &HTTP_CLIENT,
            true,
        )
        .await
        .expect_err("legacy JSON via FileYaml source must be rejected");

        assert!(
            matches!(err, super::PolicyStoreLoadError::LegacyJsonNotSupported),
            "expected LegacyJsonNotSupported, got {err:?}"
        );
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
    async fn rejects_legacy_json_from_lock_master() {
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

        let err = load_policy_store(
            &PolicyStoreConfig {
                source: crate::PolicyStoreSource::LockServer(uri),
                ..Default::default()
            },
            &HTTP_CLIENT,
            true,
        )
        .await
        .expect_err("legacy JSON from Lock Master must be rejected");

        assert!(
            matches!(err, super::PolicyStoreLoadError::LegacyJsonNotSupported),
            "expected LegacyJsonNotSupported, got {err:?}"
        );

        mock_endpoint.assert();
    }

    #[tokio::test]
    async fn rejects_legacy_json_from_uri() {
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

        let err = load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::Uri(uri),
                ..Default::default()
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect_err("legacy JSON from URI must be rejected");

        assert!(
            matches!(err, super::PolicyStoreLoadError::LegacyJsonNotSupported),
            "expected LegacyJsonNotSupported, got {err:?}"
        );

        mock_endpoint.assert();
    }

    #[tokio::test]
    async fn rejects_whitespace_prefixed_legacy_json_from_uri() {
        let mut mock_server = Server::new_async().await;

        let policy_store_json = format!(
            "  \n\t  {}",
            include_str!("../../../test_files/policy-store_lock_master_ok.json")
        );

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store-ws")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(policy_store_json)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store-ws", mock_server.url()).to_string();

        let err = load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::Uri(uri),
                ..Default::default()
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect_err("whitespace-prefixed legacy JSON from URI must be rejected");

        assert!(
            matches!(err, super::PolicyStoreLoadError::LegacyJsonNotSupported),
            "expected LegacyJsonNotSupported, got {err:?}"
        );

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
                ..Default::default()
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
                ..Default::default()
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect("Should load policy store from URI with octet-stream content-type");

        mock_endpoint.assert();
    }

    /// A `.cjar` served from a URI reports a broken store as the typed error,
    /// the same as `CjarUrl` does, rather than as a generic archive failure.
    #[tokio::test]
    async fn uri_archive_reports_typed_validation_errors() {
        let mut mock_server = Server::new_async().await;

        let archive_bytes = fixtures::minimal_valid()
            .with_spec_version(99)
            .build_archive()
            .expect("Should build test archive");

        let mock_endpoint = mock_server
            .mock("GET", "/policy-store")
            .with_status(200)
            .with_body(archive_bytes)
            .expect(1)
            .create();

        let uri = format!("{}/policy-store", mock_server.url());

        let err = load_policy_store(
            &PolicyStoreConfig {
                source: PolicyStoreSource::Uri(uri),
                ..Default::default()
            },
            &HTTP_CLIENT,
            false,
        )
        .await
        .expect_err("a store with an unsupported spec version must be rejected");

        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Validation(ValidationError::SpecVersionTooNew {
                    found: 99,
                    ..
                })
            ),
            "expected a typed SpecVersionTooNew, got: {err:?}"
        );
        mock_endpoint.assert();
    }

    /// The whole read runs on a blocking thread, never on the async thread
    /// that awaits it.
    #[tokio::test]
    async fn store_read_runs_off_the_awaiting_thread() {
        let awaiting = std::thread::current().id();
        let (tx, rx) = std::sync::mpsc::channel();

        let err = read_store_offloaded(
            move || {
                tx.send(std::thread::current().id())
                    .expect("the receiver outlives the read");
                Err(PolicyStoreLoadError::Archive("probe".to_string()))
            },
            PolicyStoreLoadError::Archive,
        )
        .await
        .expect_err("the probe read returns an error");

        assert!(
            matches!(&err, PolicyStoreLoadError::Archive(msg) if msg == "probe"),
            "the read's own result should come back unchanged, got: {err:?}"
        );
        let reading = rx.recv().expect("the read should have run");
        assert_ne!(
            reading, awaiting,
            "the read must not run on the thread that awaits it"
        );
    }

    #[tokio::test]
    async fn store_read_panic_is_reported_as_the_given_error() {
        let err = read_store_offloaded(
            || -> Result<_, PolicyStoreLoadError> { panic!("injected read panic") },
            PolicyStoreLoadError::Directory,
        )
        .await
        .expect_err("a panicking read must be reported as an error");

        assert!(
            matches!(&err, PolicyStoreLoadError::Directory(msg) if msg.contains("panicked")),
            "expected the panic as a Directory error, got: {err:?}"
        );
    }
}

#[cfg(test)]
mod signature_tests {
    use std::sync::LazyLock;
    use std::time::Duration;

    use mockito::{Server, ServerGuard};
    use sigstore_verifier::SigstoreBlobVerifier;
    use sigstore_verifier::testing::TestSigner;

    use super::*;
    use crate::bootstrap_config::{PolicyStoreSignature, SigstoreSigner};
    use crate::common::policy_store::test_utils::fixtures;
    use crate::http::HttpClientConfig;

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(1),
            request_timeout: Duration::from_secs(2),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed")
    });

    fn archive() -> Vec<u8> {
        fixtures::minimal_valid()
            .build_archive()
            .expect("test archive builds")
    }

    fn signature(identity: &str, bundle_uri: Option<String>) -> SigstoreSigner {
        SigstoreSigner {
            cert_identity: identity.to_string(),
            cert_oidc_issuer: TestSigner::ISSUER.to_string(),
            bundle_uri,
        }
    }

    fn verifier(signer: &TestSigner, signature: &SigstoreSigner) -> ArchiveVerifier {
        let trust = SigstoreBlobVerifier::new(&signer.trust_root()).expect("test trust root");
        ArchiveVerifier::new(signature, trust)
    }

    /// Serves `archive` at `/store.cjar` and, when given, `bundle` next to it.
    async fn serve(archive: Vec<u8>, bundle: Option<Vec<u8>>) -> ServerGuard {
        let mut server = Server::new_async().await;
        server
            .mock("GET", "/store.cjar")
            .with_body(archive)
            .create_async()
            .await;
        let bundle_mock = server.mock("GET", "/store.cjar.sigstore.json");
        match bundle {
            Some(bundle) => bundle_mock.with_body(bundle),
            None => bundle_mock.with_status(404),
        }
        .create_async()
        .await;
        server
    }

    async fn load_from_url(
        source: PolicyStoreSource,
        verifier: Option<&ArchiveVerifier>,
    ) -> Result<LoadedPolicyStore, PolicyStoreLoadError> {
        let config = PolicyStoreConfig {
            source,
            ..Default::default()
        };
        load_verified_policy_store(&config, &HTTP_CLIENT, false, verifier).await
    }

    #[tokio::test]
    async fn signed_store_loads_from_cjar_url_and_uri() {
        let signer = TestSigner::new();
        let archive = archive();
        let server = serve(archive.clone(), Some(signer.sign(&archive))).await;
        let verifier = verifier(&signer, &signature(TestSigner::IDENTITY, None));
        let url = format!("{}/store.cjar", server.url());

        for source in [
            PolicyStoreSource::CjarUrl(url.clone()),
            PolicyStoreSource::Uri(url.clone()),
        ] {
            let loaded = load_from_url(source, Some(&verifier))
                .await
                .expect("a correctly signed store must load");
            let signed_by = loaded
                .signed_by
                .expect("an enforced load reports its signer");
            assert_eq!(signed_by.identity, TestSigner::IDENTITY, "verified signer");
            assert_eq!(signed_by.sha256.len(), 64, "archive sha256 is hex encoded");
        }
    }

    #[tokio::test]
    async fn modified_store_is_rejected() {
        let signer = TestSigner::new();
        let archive = archive();
        let mut tampered = archive.clone();
        let last = tampered.len() - 1;
        tampered[last] ^= 1;
        let server = serve(tampered, Some(signer.sign(&archive))).await;
        let verifier = verifier(&signer, &signature(TestSigner::IDENTITY, None));

        let err = load_from_url(
            PolicyStoreSource::CjarUrl(format!("{}/store.cjar", server.url())),
            Some(&verifier),
        )
        .await
        .expect_err("a store changed after signing must not load");
        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Signature(SignatureError::Verification { .. })
            ),
            "expected a verification error, got {err:?}"
        );
    }

    #[tokio::test]
    async fn wrong_signer_is_rejected() {
        let signer = TestSigner::new();
        let archive = archive();
        let server = serve(archive.clone(), Some(signer.sign(&archive))).await;
        let verifier = verifier(&signer, &signature("someone-else@example.com", None));

        let err = load_from_url(
            PolicyStoreSource::Uri(format!("{}/store.cjar", server.url())),
            Some(&verifier),
        )
        .await
        .expect_err("a store signed by someone else must not load");
        assert!(
            err.to_string().contains("someone-else@example.com"),
            "error must name the expected signer, got: {err}"
        );
    }

    #[tokio::test]
    async fn missing_bundle_is_rejected_only_when_enforced() {
        let signer = TestSigner::new();
        let server = serve(archive(), None).await;
        let url = format!("{}/store.cjar", server.url());
        let verifier = verifier(&signer, &signature(TestSigner::IDENTITY, None));

        let err = load_from_url(PolicyStoreSource::CjarUrl(url.clone()), Some(&verifier))
            .await
            .expect_err("a store without a bundle must not load when enforced");
        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Signature(SignatureError::BundleFetch { ref source, .. })
                    if source.status_code() == Some(reqwest::StatusCode::NOT_FOUND)
            ),
            "expected a 404 bundle fetch error, got {err:?}"
        );

        load_from_url(PolicyStoreSource::CjarUrl(url), None)
            .await
            .expect("without enforcement a store loads with no bundle");
    }

    /// A config built directly, not decoded from raw properties, must not be able
    /// to enforce signatures on a store that can't be signed.
    #[tokio::test]
    async fn enforce_rejects_unsigned_sources_however_configured() {
        let dir = tempfile::tempdir().expect("temp dir");
        let sources = [
            PolicyStoreSource::Yaml("cedar_version: v4.0.0\npolicy_stores: {}".to_string()),
            PolicyStoreSource::FileYaml(dir.path().join("store.yaml")),
            PolicyStoreSource::Directory(dir.path().to_path_buf()),
        ];
        for source in sources {
            let config = PolicyStoreConfig {
                source: source.clone(),
                signature: PolicyStoreSignature::Enforce(signature(TestSigner::IDENTITY, None)),
                ..Default::default()
            };
            let err = load_policy_store(&config, &HTTP_CLIENT, false)
                .await
                .expect_err("enforce must reject a source that can't be signed");
            assert!(
                matches!(
                    err,
                    PolicyStoreLoadError::Signature(SignatureError::UnsupportedSource)
                ),
                "expected UnsupportedSource for {source:?}, got {err:?}"
            );
        }
    }

    #[tokio::test]
    async fn load_policy_store_enforces_configured_signature() {
        let signer = TestSigner::new();
        let archive = archive();
        let server = serve(archive.clone(), Some(signer.sign(&archive))).await;
        let config = PolicyStoreConfig {
            source: PolicyStoreSource::CjarUrl(format!("{}/store.cjar", server.url())),
            signature: PolicyStoreSignature::Enforce(signature(TestSigner::IDENTITY, None)),
            ..Default::default()
        };

        let err = load_policy_store(&config, &HTTP_CLIENT, false)
            .await
            .expect_err("the production trust root must reject a test-signed store");
        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Signature(SignatureError::Verification { .. })
            ),
            "expected a verification error, got {err:?}"
        );
    }

    #[tokio::test]
    async fn signed_store_loads_from_cjar_file() {
        let signer = TestSigner::new();
        let archive = archive();
        let dir = tempfile::tempdir().expect("temp dir");
        let path = dir.path().join("store.cjar");
        std::fs::write(&path, &archive).expect("write archive");
        let verifier = verifier(&signer, &signature(TestSigner::IDENTITY, None));
        let source = || PolicyStoreSource::CjarFile(path.clone());

        let err = load_from_url(source(), Some(&verifier))
            .await
            .expect_err("a local store without its bundle file must not load");
        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Signature(SignatureError::Bundle { .. })
            ),
            "expected a bundle error, got {err:?}"
        );

        std::fs::write(
            dir.path().join("store.cjar.sigstore.json"),
            signer.sign(&archive),
        )
        .expect("write bundle");
        load_from_url(source(), Some(&verifier))
            .await
            .expect("a local store with a valid sibling bundle must load");
    }

    #[tokio::test]
    async fn archive_bytes_need_a_bundle_uri() {
        let signer = TestSigner::new();
        let archive = archive();
        let source = || PolicyStoreSource::ArchiveBytes(archive.clone());

        let err = load_from_url(
            source(),
            Some(&verifier(&signer, &signature(TestSigner::IDENTITY, None))),
        )
        .await
        .expect_err("in-memory bytes have no default bundle location");
        assert!(
            matches!(
                err,
                PolicyStoreLoadError::Signature(SignatureError::NoBundleLocation)
            ),
            "expected NoBundleLocation, got {err:?}"
        );

        let dir = tempfile::tempdir().expect("temp dir");
        let bundle = dir.path().join("bundle.json");
        std::fs::write(&bundle, signer.sign(&archive)).expect("write bundle");
        let with_uri = signature(
            TestSigner::IDENTITY,
            Some(bundle.to_string_lossy().into_owned()),
        );
        load_from_url(source(), Some(&verifier(&signer, &with_uri)))
            .await
            .expect("in-memory bytes load with a configured bundle");
    }
}

#[cfg(test)]
mod bundle_hash_tests {
    use std::fmt::Write as _;
    use std::io::Write as _;
    use std::sync::LazyLock;
    use std::time::Duration;

    use jans_trace_core::inputs::compute_bundle_hash;
    use mockito::Server;
    use sha2::{Digest as _, Sha256};

    use super::*;
    use crate::common::policy_store::test_utils::fixtures;
    use crate::http::HttpClientConfig;

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(1),
            request_timeout: Duration::from_secs(2),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed")
    });

    fn archive() -> Vec<u8> {
        fixtures::minimal_valid()
            .build_archive()
            .expect("test archive builds")
    }

    async fn load(source: PolicyStoreSource) -> PolicyStoreWithID {
        let config = PolicyStoreConfig {
            source,
            ..Default::default()
        };
        load_policy_store(&config, &HTTP_CLIENT, false)
            .await
            .expect("test policy store should load")
            .store
    }

    #[tokio::test]
    async fn cjar_bytes_store_has_bundle_hash() {
        let bytes = archive();
        let store = load(PolicyStoreSource::ArchiveBytes(bytes.clone())).await;
        let hash = store
            .bundle_hash()
            .expect("a store loaded from .cjar bytes should carry bundle_hash");
        assert_eq!(
            hash,
            compute_bundle_hash(&bytes),
            "bundle_hash should be the core hash of the archive bytes"
        );
        // Independent of core: `sha256sum store.cjar` with the TRACE prefix.
        let sha256sum = Sha256::digest(&bytes)
            .iter()
            .fold(String::new(), |mut hex, b| {
                let _ = write!(hex, "{b:02x}");
                hex
            });
        assert_eq!(
            hash.to_string(),
            format!("sha256:{sha256sum}"),
            "bundle_hash should be SHA-256 of the raw .cjar bytes"
        );
    }

    #[tokio::test]
    async fn cjar_file_and_url_hash_same_bytes_equally() {
        let bytes = archive();

        let mut file = tempfile::Builder::new()
            .suffix(".cjar")
            .tempfile()
            .expect("temp file should be created");
        file.write_all(&bytes).expect("archive should be written");
        let file_store = load(PolicyStoreSource::CjarFile(file.path().to_path_buf())).await;

        let mut server = Server::new_async().await;
        server
            .mock("GET", "/store.cjar")
            .with_body(bytes.clone())
            .create_async()
            .await;
        let url = format!("{}/store.cjar", server.url());
        let url_store = load(PolicyStoreSource::CjarUrl(url.clone())).await;
        let generic_uri_store = load(PolicyStoreSource::Uri(url)).await;

        let expected = Some(compute_bundle_hash(&bytes));
        assert_eq!(
            file_store.bundle_hash(),
            expected,
            "CjarFile should hash the file bytes"
        );
        assert_eq!(
            url_store.bundle_hash(),
            expected,
            "CjarUrl should hash the fetched bytes"
        );
        assert_eq!(
            generic_uri_store.bundle_hash(),
            expected,
            "Uri should hash the fetched bytes"
        );
    }

    #[tokio::test]
    async fn yaml_store_has_no_bundle_hash() {
        let store = load(PolicyStoreSource::Yaml(
            include_str!("../../../test_files/policy-store_ok.yaml").to_string(),
        ))
        .await;
        assert_eq!(
            store.bundle_hash(),
            None,
            "a YAML store has no archive, so no bundle_hash"
        );
    }
}
