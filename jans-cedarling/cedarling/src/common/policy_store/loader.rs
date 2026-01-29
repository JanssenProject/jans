// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.
//!
//! # Internal API Note
//!
//! This module is part of the internal implementation. External users should use the
//! `Cedarling` API with `BootstrapConfig` to load policy stores.
//!
//! # Loading Archives (.cjar files)
//!
//! Archives are loaded using `ArchiveVfs`, which implements the `VfsFileSystem` trait.
//! This design:
//! - Works in WASM (no temp file extraction needed)
//! - Is efficient (reads files on-demand from archive)
//! - Is secure (no temp file cleanup concerns)

use std::path::Path;

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};
use super::validator::MetadataValidator;
#[cfg(not(target_arch = "wasm32"))]
use super::vfs_adapter::PhysicalVfs;
use super::vfs_adapter::VfsFileSystem;

/// Load a policy store from a directory path.
///
/// This function uses `PhysicalVfs` to read from the local filesystem.
/// It is only available on native platforms (not WASM).
#[cfg(not(target_arch = "wasm32"))]
pub(crate) async fn load_policy_store_directory(
    path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    let path_str = path
        .to_str()
        .ok_or_else(|| PolicyStoreError::PathNotFound {
            path: path.display().to_string(),
        })?
        .to_string();

    // Offload blocking I/O operations to a blocking thread pool to avoid blocking the async runtime.
    // `load_directory` is intentionally synchronous because it performs blocking filesystem I/O.
    // Using `spawn_blocking` ensures these operations don't block the async executor.
    tokio::task::spawn_blocking(move || {
        // Use the PhysicalVfs-specific loader for directory-based stores.
        let loader = DefaultPolicyStoreLoader::new_physical();

        // Load all components from the directory.
        let loaded_directory = loader.load_directory(&path_str)?;

        // If a manifest is present, validate it against the physical filesystem.
        if let Some(ref manifest) = loaded_directory.manifest {
            DefaultPolicyStoreLoader::<PhysicalVfs>::validate_manifest(
                &path_str,
                &loaded_directory.metadata,
                manifest,
            )?;
        }

        Ok(loaded_directory)
    })
    .await
    .map_err(|e| {
        // If the blocking task panicked, convert to an IO error.
        // This should be rare and typically indicates a bug in the loader code.
        PolicyStoreError::Io(std::io::Error::other(format!(
            "Blocking task panicked: {e}"
        )))
    })?
}

/// Load a policy store from a directory path (WASM stub).
///
/// Directory loading is not supported in WASM environments.
/// Use `load_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub(crate) fn load_policy_store_directory(
    _path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    Err(super::errors::ArchiveError::WasmUnsupported.into())
}

/// Load a policy store from a Cedar Archive (.cjar) file.
///
/// This function uses `ArchiveVfs` to read from a zip archive.
/// It is only available on native platforms (not WASM).
#[cfg(not(target_arch = "wasm32"))]
pub(crate) async fn load_policy_store_archive(
    path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    let path = path.to_path_buf();

    // Offload blocking I/O operations to a blocking thread pool to avoid blocking the async runtime.
    // `load_directory` is intentionally synchronous because it performs blocking filesystem I/O
    // (reading from zip archive). Using `spawn_blocking` ensures these operations don't block
    // the async executor.
    tokio::task::spawn_blocking(move || {
        use super::archive_handler::ArchiveVfs;
        let archive_vfs = ArchiveVfs::from_file(&path)?;
        let loader = DefaultPolicyStoreLoader::new(archive_vfs);
        loader.load_directory(".")
    })
    .await
    .map_err(|e| {
        // If the blocking task panicked, convert to an IO error.
        // This should be rare and typically indicates a bug in the loader code.
        PolicyStoreError::Io(std::io::Error::other(format!(
            "Blocking task panicked: {e}"
        )))
    })?
}

/// Load a policy store from a Cedar Archive (.cjar) file (WASM stub).
///
/// File-based archive loading is not supported in WASM environments.
/// Use `load_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub(crate) fn load_policy_store_archive(
    _path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    Err(super::errors::ArchiveError::WasmUnsupported.into())
}

/// Load a policy store from archive bytes.
///
/// This function is useful for:
/// - WASM environments where file system access is not available
/// - Loading archives fetched from URLs
/// - Loading archives from any byte source
pub(crate) fn load_policy_store_archive_bytes(
    bytes: &[u8],
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    use super::archive_handler::ArchiveVfs;

    let archive_vfs = ArchiveVfs::from_buffer(bytes.to_owned())?;
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    let loaded_directory = loader.load_directory(".")?;

    // Validate manifest if present (same validation used for archive-backed loading)
    #[cfg(not(target_arch = "wasm32"))]
    if let Some(ref _manifest) = loaded_directory.manifest {
        use super::manifest_validator::ManifestValidator;
        use std::path::PathBuf;

        // Create a new ArchiveVfs instance for validation (ManifestValidator needs its own VFS)
        let validator_vfs = ArchiveVfs::from_buffer(bytes.to_vec())?;
        let validator = ManifestValidator::new(validator_vfs, PathBuf::from("."));
        let result = validator.validate(Some(&loaded_directory.metadata.policy_store.id));

        // If validation fails, return the first error
        if !result.is_valid
            && let Some(error) = result.errors.first()
        {
            return Err(PolicyStoreError::ManifestError {
                err: error.error_type.clone(),
            });
        }
    }

    Ok(loaded_directory)
}

/// A loaded policy store with all its components.
#[derive(Debug)]
pub(crate) struct LoadedPolicyStore {
    /// Policy store metadata
    pub metadata: PolicyStoreMetadata,
    /// Optional manifest for integrity checking
    pub manifest: Option<PolicyStoreManifest>,
    /// Raw schema content
    pub schema: String,
    /// Policy files content (filename -> content)
    pub policies: Vec<PolicyFile>,
    /// Template files content (filename -> content)
    pub templates: Vec<PolicyFile>,
    /// Entity files content (filename -> content)
    pub entities: Vec<EntityFile>,
    /// Trusted issuer files content (filename -> content)
    pub trusted_issuers: Vec<IssuerFile>,
}

/// A policy or template file.
#[derive(Debug, Clone)]
pub(crate) struct PolicyFile {
    /// File name
    pub name: String,
    /// File content
    pub content: String,
}

/// An entity definition file.
#[derive(Debug, Clone)]
pub(crate) struct EntityFile {
    /// File name
    pub name: String,
    /// JSON content
    pub content: String,
}

/// A trusted issuer configuration file.
#[derive(Debug, Clone)]
pub(crate) struct IssuerFile {
    /// File name
    pub name: String,
    /// JSON content
    pub content: String,
}

/// Default implementation of policy store loader.
///
/// Generic over a VFS implementation to support different storage backends:
/// - Physical filesystem for native platforms
/// - Memory filesystem for testing and WASM
/// - Archive filesystem for .cjar files
pub(super) struct DefaultPolicyStoreLoader<V: VfsFileSystem> {
    vfs: V,
}

impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Create a new policy store loader with the given VFS backend.
    pub(super) fn new(vfs: V) -> Self {
        Self { vfs }
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs> {
    /// Create a new policy store loader using the physical filesystem.
    ///
    /// This is a convenience constructor for native platforms.
    pub(super) fn new_physical() -> Self {
        Self::new(super::vfs_adapter::PhysicalVfs::new())
    }

    /// Validate the manifest file against the policy store contents.
    ///
    /// This method is only available for `PhysicalVfs` because:
    /// - It requires creating a new VFS instance for validation
    /// - Other VFS types (`MemoryVfs`, custom implementations) may not support cheap instantiation
    /// - WASM environments may not have filesystem access for validation
    ///
    /// Users of other VFS types should call `ManifestValidator::validate()` directly
    /// with their VFS instance if they need manifest validation.
    ///
    /// This method is public so it can be called explicitly when needed, following
    /// the Interface Segregation Principle.
    #[cfg(not(target_arch = "wasm32"))]
    pub(super) fn validate_manifest(
        dir: &str,
        metadata: &PolicyStoreMetadata,
        manifest: &PolicyStoreManifest,
    ) -> Result<(), PolicyStoreError> {
        Self::validate_manifest_with_logger(dir, metadata, manifest, None)
    }

    /// Validate the manifest file with optional logging for unlisted files.
    ///
    /// Same as `validate_manifest` but accepts an optional logger for structured logging.
    #[cfg(not(target_arch = "wasm32"))]
    fn validate_manifest_with_logger(
        dir: &str,
        metadata: &PolicyStoreMetadata,
        _manifest: &PolicyStoreManifest,
        logger: Option<crate::log::Logger>,
    ) -> Result<(), PolicyStoreError> {
        use super::log_entry::PolicyStoreLogEntry;
        use super::manifest_validator::ManifestValidator;
        use crate::log::interface::LogWriter;
        use std::path::PathBuf;

        // Create a new PhysicalVfs instance for validation
        let validator =
            ManifestValidator::new(super::vfs_adapter::PhysicalVfs::new(), PathBuf::from(dir));

        let result = validator.validate(Some(&metadata.policy_store.id));

        // If validation fails, return the first error
        if !result.is_valid
            && let Some(error) = result.errors.first()
        {
            return Err(PolicyStoreError::ManifestError {
                err: error.error_type.clone(),
            });
        }

        // Log unlisted files if any (informational - these files are allowed but not checksummed)
        if !result.unlisted_files.is_empty()
            && let Some(logger) = logger
        {
            logger.log_any(PolicyStoreLogEntry::info(format!(
                "Policy store contains {} unlisted file(s) not in manifest: {:?}",
                result.unlisted_files.len(),
                result.unlisted_files
            )));
        }

        Ok(())
    }
}

impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Helper to join paths, handling "." correctly.
    fn join_path(base: &str, file: &str) -> String {
        if base == "." || base.is_empty() {
            file.to_string()
        } else {
            format!("{base}/{file}")
        }
    }

    /// Validate directory structure for required files and directories.
    fn validate_directory_structure(&self, dir: &str) -> Result<(), PolicyStoreError> {
        // Check if directory exists
        if !self.vfs.exists(dir) {
            return Err(PolicyStoreError::PathNotFound {
                path: dir.to_string(),
            });
        }

        if !self.vfs.is_dir(dir) {
            return Err(PolicyStoreError::NotADirectory {
                path: dir.to_string(),
            });
        }

        // Ensure is_file method is used (prevents dead code warning in WASM)
        // This is a no-op check that ensures the trait method is called
        let _ = self.vfs.is_file(dir);

        // Check for required files
        let metadata_path = Self::join_path(dir, "metadata.json");
        if !self.vfs.exists(&metadata_path) {
            return Err(ValidationError::MissingRequiredFile {
                file: "metadata.json".to_string(),
            }
            .into());
        }

        let schema_path = Self::join_path(dir, "schema.cedarschema");
        if !self.vfs.exists(&schema_path) {
            return Err(ValidationError::MissingRequiredFile {
                file: "schema.cedarschema".to_string(),
            }
            .into());
        }

        // Check for required directories
        let policies_dir = Self::join_path(dir, "policies");
        if !self.vfs.exists(&policies_dir) {
            return Err(ValidationError::MissingRequiredDirectory {
                directory: "policies".to_string(),
            }
            .into());
        }

        if !self.vfs.is_dir(&policies_dir) {
            return Err(PolicyStoreError::NotADirectory {
                path: policies_dir.clone(),
            });
        }

        Ok(())
    }

    /// Load metadata from metadata.json file.
    fn load_metadata(&self, dir: &str) -> Result<PolicyStoreMetadata, PolicyStoreError> {
        let metadata_path = Self::join_path(dir, "metadata.json");
        let bytes = self.vfs.read_file(&metadata_path).map_err(|source| {
            PolicyStoreError::FileReadError {
                path: metadata_path.clone(),
                source,
            }
        })?;

        let content = String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
            path: metadata_path.clone(),
            source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
        })?;

        // Parse and validate metadata
        MetadataValidator::parse_and_validate(&content).map_err(PolicyStoreError::Validation)
    }

    /// Load optional manifest from manifest.json file.
    fn load_manifest(&self, dir: &str) -> Result<Option<PolicyStoreManifest>, PolicyStoreError> {
        let manifest_path = Self::join_path(dir, "manifest.json");
        if !self.vfs.exists(&manifest_path) {
            return Ok(None);
        }

        // Open file and parse JSON using from_reader for better performance
        let reader = self.vfs.open_file(&manifest_path).map_err(|source| {
            PolicyStoreError::FileReadError {
                path: manifest_path.clone(),
                source,
            }
        })?;

        let manifest =
            serde_json::from_reader(reader).map_err(|source| PolicyStoreError::JsonParsing {
                file: "manifest.json".to_string(),
                source,
            })?;

        Ok(Some(manifest))
    }

    /// Load schema from schema.cedarschema file.
    fn load_schema(&self, dir: &str) -> Result<String, PolicyStoreError> {
        let schema_path = Self::join_path(dir, "schema.cedarschema");
        let bytes =
            self.vfs
                .read_file(&schema_path)
                .map_err(|source| PolicyStoreError::FileReadError {
                    path: schema_path.clone(),
                    source,
                })?;

        String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
            path: schema_path.clone(),
            source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
        })
    }

    /// Load all policy files from policies directory.
    fn load_policies(&self, dir: &str) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let policies_dir = Self::join_path(dir, "policies");
        self.load_cedar_files(&policies_dir, "policy")
    }

    /// Load all template files from templates directory (if exists).
    fn load_templates(&self, dir: &str) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let templates_dir = Self::join_path(dir, "templates");
        if !self.vfs.exists(&templates_dir) {
            return Ok(Vec::new());
        }

        self.load_cedar_files(&templates_dir, "template")
    }

    /// Load all entity files from entities directory (if exists).
    fn load_entities(&self, dir: &str) -> Result<Vec<EntityFile>, PolicyStoreError> {
        let entities_dir = Self::join_path(dir, "entities");
        if !self.vfs.exists(&entities_dir) {
            return Ok(Vec::new());
        }

        self.load_json_files(&entities_dir, "entity")
    }

    /// Load all trusted issuer files from trusted-issuers directory (if exists).
    fn load_trusted_issuers(&self, dir: &str) -> Result<Vec<IssuerFile>, PolicyStoreError> {
        let issuers_dir = Self::join_path(dir, "trusted-issuers");
        if !self.vfs.exists(&issuers_dir) {
            return Ok(Vec::new());
        }

        let entries = self.vfs.read_dir(&issuers_dir).map_err(|source| {
            PolicyStoreError::DirectoryReadError {
                path: issuers_dir.clone(),
                source,
            }
        })?;

        let mut issuers = Vec::new();
        for entry in entries {
            if !entry.is_dir {
                // Validate .json extension
                if !entry.name.to_lowercase().ends_with(".json") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: entry.path.clone(),
                        expected: ".json".to_string(),
                        actual: Path::new(&entry.name)
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let bytes = self.vfs.read_file(&entry.path).map_err(|source| {
                    PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source,
                    }
                })?;

                let content =
                    String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
                    })?;

                issuers.push(IssuerFile {
                    name: entry.name,
                    content,
                });
            }
        }

        Ok(issuers)
    }

    /// Helper: Load all .cedar files from a directory, recursively scanning subdirectories.
    fn load_cedar_files(
        &self,
        dir: &str,
        _file_type: &str,
    ) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let mut files = Vec::new();
        self.load_cedar_files_recursive(dir, &mut files)?;
        Ok(files)
    }

    /// Recursive helper to load .cedar files from a directory and its subdirectories.
    fn load_cedar_files_recursive(
        &self,
        dir: &str,
        files: &mut Vec<PolicyFile>,
    ) -> Result<(), PolicyStoreError> {
        let entries =
            self.vfs
                .read_dir(dir)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: dir.to_string(),
                    source,
                })?;

        for entry in entries {
            if entry.is_dir {
                // Recursively scan subdirectories
                self.load_cedar_files_recursive(&entry.path, files)?;
            } else {
                // Validate .cedar extension
                if !entry.name.to_lowercase().ends_with(".cedar") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: entry.path.clone(),
                        expected: ".cedar".to_string(),
                        actual: Path::new(&entry.name)
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let bytes = self.vfs.read_file(&entry.path).map_err(|source| {
                    PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source,
                    }
                })?;

                let content =
                    String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
                    })?;

                files.push(PolicyFile {
                    name: entry.name,
                    content,
                });
            }
        }

        Ok(())
    }

    /// Helper: Load all .json files from a directory.
    fn load_json_files(
        &self,
        dir: &str,
        _file_type: &str,
    ) -> Result<Vec<EntityFile>, PolicyStoreError> {
        let entries =
            self.vfs
                .read_dir(dir)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: dir.to_string(),
                    source,
                })?;

        let mut files = Vec::new();
        for entry in entries {
            if !entry.is_dir {
                // Validate .json extension
                if !entry.name.to_lowercase().ends_with(".json") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: entry.path.clone(),
                        expected: ".json".to_string(),
                        actual: Path::new(&entry.name)
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let bytes = self.vfs.read_file(&entry.path).map_err(|source| {
                    PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source,
                    }
                })?;

                let content =
                    String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
                        path: entry.path.clone(),
                        source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
                    })?;

                files.push(EntityFile {
                    name: entry.name,
                    content,
                });
            }
        }

        Ok(files)
    }

    /// Load a directory-based policy store.
    ///
    /// This method is generic over the underlying `VfsFileSystem` and **does not**
    /// perform manifest validation. For backends that need manifest validation
    /// (e.g., `PhysicalVfs`), callers should use higher-level helpers such as
    /// `load_policy_store_directory` or call `validate_manifest` explicitly on
    /// `DefaultPolicyStoreLoader<PhysicalVfs>`.
    pub(super) fn load_directory(&self, dir: &str) -> Result<LoadedPolicyStore, PolicyStoreError> {
        // Validate structure first
        self.validate_directory_structure(dir)?;

        // Load all components
        let metadata = self.load_metadata(dir)?;
        let manifest = self.load_manifest(dir)?;

        let schema = self.load_schema(dir)?;
        let policies = self.load_policies(dir)?;
        let templates = self.load_templates(dir)?;
        let entities = self.load_entities(dir)?;
        let trusted_issuers = self.load_trusted_issuers(dir)?;

        Ok(LoadedPolicyStore {
            metadata,
            manifest,
            schema,
            policies,
            templates,
            entities,
            trusted_issuers,
        })
    }
}

// Test-only helper functions for parsing policies
// These are thin wrappers around PolicyParser for test convenience
#[cfg(test)]
use super::policy_parser;

#[cfg(test)]
impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Parse and validate Cedar policies from loaded policy files.
    fn parse_policies(
        policy_files: &[PolicyFile],
    ) -> Result<Vec<policy_parser::ParsedPolicy>, PolicyStoreError> {
        let mut parsed_policies = Vec::with_capacity(policy_files.len());
        for file in policy_files {
            let parsed = policy_parser::PolicyParser::parse_policy(&file.content, &file.name)?;
            parsed_policies.push(parsed);
        }
        Ok(parsed_policies)
    }

    /// Parse and validate Cedar templates from loaded template files.
    fn parse_templates(
        template_files: &[PolicyFile],
    ) -> Result<Vec<policy_parser::ParsedTemplate>, PolicyStoreError> {
        let mut parsed_templates = Vec::with_capacity(template_files.len());
        for file in template_files {
            let parsed = policy_parser::PolicyParser::parse_template(&file.content, &file.name)?;
            parsed_templates.push(parsed);
        }
        Ok(parsed_templates)
    }

    /// Create a Cedar `PolicySet` from parsed policies and templates.
    fn create_policy_set(
        policies: Vec<policy_parser::ParsedPolicy>,
        templates: Vec<policy_parser::ParsedTemplate>,
    ) -> Result<cedar_policy::PolicySet, PolicyStoreError> {
        policy_parser::PolicyParser::create_policy_set(policies, templates)
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl Default for DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs> {
    fn default() -> Self {
        Self::new_physical()
    }
}

#[cfg(test)]
#[path = "loader_tests.rs"]
mod tests;
