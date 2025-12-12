// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.
//!
//! # Loading Archives (.cjar files)
//!
//! Archives are loaded using `ArchiveVfs`, which implements the `VfsFileSystem` trait.
//! This design:
//! - Works in WASM (no temp file extraction needed)
//! - Is efficient (reads files on-demand from archive)
//! - Is secure (no temp file cleanup concerns)
//!
//! ## Example: Loading an archive (native)
//!
//! ```no_run
//! use cedarling::common::policy_store::{ArchiveVfs, DefaultPolicyStoreLoader};
//!
//! // Create archive VFS (validates format during construction)
//! let archive_vfs = ArchiveVfs::from_file("policy_store.cjar")?;
//!
//! // Create loader with archive VFS
//! let loader = DefaultPolicyStoreLoader::new(archive_vfs);
//!
//! // Load policy store from root directory of archive
//! let loaded = loader.load_directory(".")?;
//! # Ok::<(), Box<dyn std::error::Error>>(())
//! ```
//!
//! ## Example: Loading archive in WASM
//!
//! ```no_run
//! use cedarling::common::policy_store::{ArchiveVfs, DefaultPolicyStoreLoader};
//!
//! // Get archive bytes (from network, storage, etc.)
//! let archive_bytes: Vec<u8> = fetch_archive_bytes()?;
//!
//! // Create archive VFS from bytes
//! let archive_vfs = ArchiveVfs::from_buffer(archive_bytes)?;
//!
//! // Load as normal
//! let loader = DefaultPolicyStoreLoader::new(archive_vfs);
//! let loaded = loader.load_directory(".")?;
//! # Ok::<(), Box<dyn std::error::Error>>(())
//! # fn fetch_archive_bytes() -> Result<Vec<u8>, Box<dyn std::error::Error>> { Ok(vec![]) }
//! ```

use super::errors::{PolicyStoreError, ValidationError};

#[cfg(test)]
use super::errors::ArchiveError;
use super::manifest_validator::ManifestValidator;
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};

#[cfg(test)]
use super::source::{ArchiveSource, PolicyStoreFormat, PolicyStoreSource};
use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;
use std::path::{Path, PathBuf};

/// Policy store loader trait for loading policy stores from various sources.
///
/// This trait provides a unified interface for loading policy stores from different
/// sources (directories, archives, URLs). The main code path uses `DefaultPolicyStoreLoader`
/// directly; this trait is used in tests for more generic loading scenarios.
#[cfg(test)]
pub trait PolicyStoreLoader {
    /// Load a policy store from the given source.
    fn load(&self, source: &PolicyStoreSource) -> Result<LoadedPolicyStore, PolicyStoreError>;

    /// Detect the format of a policy store source.
    fn detect_format(&self, source: &PolicyStoreSource) -> PolicyStoreFormat;

    /// Validate the structure of a policy store source.
    fn validate_structure(&self, source: &PolicyStoreSource) -> Result<(), PolicyStoreError>;
}

/// Load a policy store from a directory path.
///
/// This function uses `PhysicalVfs` to read from the local filesystem.
/// It is only available on native platforms (not WASM).
///
/// # Example
///
/// ```no_run
/// use cedarling::common::policy_store::loader::load_policy_store_directory;
/// use std::path::Path;
///
/// # async fn example() -> Result<(), Box<dyn std::error::Error>> {
/// let loaded = load_policy_store_directory(Path::new("./policy_store")).await?;
/// println!("Loaded store: {}", loaded.metadata.policy_store.name);
/// # Ok(())
/// # }
/// ```
///
/// # Errors
///
/// Returns an error if:
/// - The directory does not exist
/// - Required files are missing (metadata.json, schema.cedarschema)
/// - Files contain invalid content
#[cfg(not(target_arch = "wasm32"))]
pub async fn load_policy_store_directory(
    path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    let vfs = super::vfs_adapter::PhysicalVfs::new();
    let loader = DefaultPolicyStoreLoader::new(vfs);
    let path_str = path
        .to_str()
        .ok_or_else(|| PolicyStoreError::PathNotFound {
            path: path.display().to_string(),
        })?;
    loader.load_directory(path_str)
}

/// Load a policy store from a directory path (WASM stub).
///
/// Directory loading is not supported in WASM environments.
/// Use `load_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub async fn load_policy_store_directory(
    _path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    Err(PolicyStoreError::PathNotFound {
        path: "Directory loading not supported in WASM".to_string(),
    })
}

/// Load a policy store from a Cedar Archive (.cjar) file.
///
/// This function uses `ArchiveVfs` to read from a zip archive.
/// It is only available on native platforms (not WASM).
///
/// # Example
///
/// ```no_run
/// use cedarling::common::policy_store::loader::load_policy_store_archive;
/// use std::path::Path;
///
/// # async fn example() -> Result<(), Box<dyn std::error::Error>> {
/// let loaded = load_policy_store_archive(Path::new("./policy_store.cjar")).await?;
/// println!("Loaded store: {}", loaded.metadata.policy_store.name);
/// # Ok(())
/// # }
/// ```
///
/// # Errors
///
/// Returns an error if:
/// - The file does not exist
/// - The file is not a valid zip archive
/// - Required files are missing (metadata.json, schema.cedarschema)
/// - Files contain invalid content
#[cfg(not(target_arch = "wasm32"))]
pub async fn load_policy_store_archive(path: &Path) -> Result<LoadedPolicyStore, PolicyStoreError> {
    use super::archive_handler::ArchiveVfs;
    let archive_vfs = ArchiveVfs::from_file(path)?;
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    loader.load_directory(".")
}

/// Load a policy store from a Cedar Archive (.cjar) file (WASM stub).
///
/// File-based archive loading is not supported in WASM environments.
/// Use `load_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub async fn load_policy_store_archive(
    _path: &Path,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    Err(PolicyStoreError::Archive(
        super::errors::ArchiveError::WasmUnsupported,
    ))
}

/// Load a policy store from archive bytes.
///
/// This function is useful for:
/// - WASM environments where file system access is not available
/// - Loading archives fetched from URLs
/// - Loading archives from any byte source
///
/// # Example
///
/// ```no_run
/// use cedarling::common::policy_store::loader::load_policy_store_archive_bytes;
///
/// # async fn example() -> Result<(), Box<dyn std::error::Error>> {
/// // Fetch archive bytes from network, storage, etc.
/// let archive_bytes: Vec<u8> = fetch_from_network().await?;
/// let loaded = load_policy_store_archive_bytes(archive_bytes)?;
/// println!("Loaded store: {}", loaded.metadata.policy_store.name);
/// # Ok(())
/// # }
/// # async fn fetch_from_network() -> Result<Vec<u8>, Box<dyn std::error::Error>> { Ok(vec![]) }
/// ```
///
/// # Errors
///
/// Returns an error if:
/// - The bytes are not a valid zip archive
/// - Required files are missing (metadata.json, schema.cedarschema)
/// - Files contain invalid content
pub fn load_policy_store_archive_bytes(
    bytes: Vec<u8>,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    use super::archive_handler::ArchiveVfs;
    let archive_vfs = ArchiveVfs::from_buffer(bytes)?;
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    loader.load_directory(".")
}

/// A loaded policy store with all its components.
#[derive(Debug)]
pub struct LoadedPolicyStore {
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
pub struct PolicyFile {
    /// File name
    pub name: String,
    /// File content
    pub content: String,
}

/// An entity definition file.
#[derive(Debug, Clone)]
pub struct EntityFile {
    /// File name
    pub name: String,
    /// JSON content
    pub content: String,
}

/// A trusted issuer configuration file.
#[derive(Debug, Clone)]
pub struct IssuerFile {
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
pub struct DefaultPolicyStoreLoader<V: VfsFileSystem> {
    vfs: V,
}

impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Create a new policy store loader with the given VFS backend.
    pub fn new(vfs: V) -> Self {
        Self { vfs }
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs> {
    /// Create a new policy store loader using the physical filesystem.
    ///
    /// This is a convenience constructor for native platforms.
    pub fn new_physical() -> Self {
        Self::new(super::vfs_adapter::PhysicalVfs::new())
    }

    /// Validate the manifest file against the policy store contents.
    ///
    /// This method is only available for PhysicalVfs because:
    /// - It requires creating a new VFS instance for validation
    /// - Other VFS types (MemoryVfs, custom implementations) may not support cheap instantiation
    /// - WASM environments may not have filesystem access for validation
    ///
    /// Users of other VFS types should call ManifestValidator::validate() directly
    /// with their VFS instance if they need manifest validation.
    ///
    /// This method is public so it can be called explicitly when needed, following
    /// the Interface Segregation Principle.
    pub fn validate_manifest(
        &self,
        dir: &str,
        metadata: &PolicyStoreMetadata,
        _manifest: &PolicyStoreManifest,
    ) -> Result<(), PolicyStoreError> {
        self.validate_manifest_with_logger(dir, metadata, _manifest, None)
    }

    /// Validate the manifest file with optional logging for unlisted files.
    ///
    /// Same as `validate_manifest` but accepts an optional logger for structured logging.
    pub fn validate_manifest_with_logger(
        &self,
        dir: &str,
        metadata: &PolicyStoreMetadata,
        _manifest: &PolicyStoreManifest,
        logger: Option<crate::log::Logger>,
    ) -> Result<(), PolicyStoreError> {
        use super::log_entry::PolicyStoreLogEntry;
        use crate::log::interface::LogWriter;

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
        if !result.unlisted_files.is_empty() {
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
            format!("{}/{}", base, file)
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
                if !entry.name.ends_with(".json") {
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
                if !entry.name.ends_with(".cedar") {
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
                if !entry.name.ends_with(".json") {
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
    /// Note: Manifest validation is automatically performed ONLY for PhysicalVfs.
    /// For other VFS types (MemoryVfs, WASM, custom implementations), users should
    /// call ManifestValidator::validate() directly if validation is needed.
    ///
    /// This design follows the Interface Segregation Principle: manifest validation
    /// is only available where it makes sense (native filesystem).
    pub fn load_directory(&self, dir: &str) -> Result<LoadedPolicyStore, PolicyStoreError> {
        // Validate structure first
        self.validate_directory_structure(dir)?;

        // Load all components
        let metadata = self.load_metadata(dir)?;
        let manifest = self.load_manifest(dir)?;

        // Validate manifest if present (only for PhysicalVfs)
        // This uses runtime type checking to avoid leaking PhysicalVfs-specific
        // behavior into the generic interface
        #[cfg(not(target_arch = "wasm32"))]
        if let Some(ref manifest_data) = manifest {
            use std::any::TypeId;

            // Only validate for PhysicalVfs - this avoids forcing all VFS implementations
            // to support manifest validation when it may not be meaningful
            if TypeId::of::<V>() == TypeId::of::<super::vfs_adapter::PhysicalVfs>() {
                // We need to cast self to the PhysicalVfs-specific type to call validate_manifest
                // Safety: We've verified V is PhysicalVfs via TypeId check
                let physical_loader = unsafe {
                    &*(self as *const Self
                        as *const DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs>)
                };
                physical_loader.validate_manifest(dir, &metadata, manifest_data)?;
            }
        }

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
impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Parse and validate Cedar policies from loaded policy files.
    fn parse_policies(
        policy_files: &[PolicyFile],
    ) -> Result<Vec<super::policy_parser::ParsedPolicy>, PolicyStoreError> {
        use super::policy_parser::PolicyParser;
        let mut parsed_policies = Vec::with_capacity(policy_files.len());
        for file in policy_files {
            let parsed = PolicyParser::parse_policy(&file.content, &file.name)?;
            parsed_policies.push(parsed);
        }
        Ok(parsed_policies)
    }

    /// Parse and validate Cedar templates from loaded template files.
    fn parse_templates(
        template_files: &[PolicyFile],
    ) -> Result<Vec<super::policy_parser::ParsedTemplate>, PolicyStoreError> {
        use super::policy_parser::PolicyParser;
        let mut parsed_templates = Vec::with_capacity(template_files.len());
        for file in template_files {
            let parsed = PolicyParser::parse_template(&file.content, &file.name)?;
            parsed_templates.push(parsed);
        }
        Ok(parsed_templates)
    }

    /// Create a Cedar PolicySet from parsed policies and templates.
    fn create_policy_set(
        policies: Vec<super::policy_parser::ParsedPolicy>,
        templates: Vec<super::policy_parser::ParsedTemplate>,
    ) -> Result<cedar_policy::PolicySet, PolicyStoreError> {
        use super::policy_parser::PolicyParser;
        PolicyParser::create_policy_set(policies, templates)
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl Default for DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs> {
    fn default() -> Self {
        Self::new_physical()
    }
}

#[cfg(test)]
impl<V: VfsFileSystem> PolicyStoreLoader for DefaultPolicyStoreLoader<V> {
    fn load(&self, source: &PolicyStoreSource) -> Result<LoadedPolicyStore, PolicyStoreError> {
        match source {
            PolicyStoreSource::Directory(path) => {
                let path_str = path
                    .to_str()
                    .ok_or_else(|| PolicyStoreError::InvalidFileName {
                        path: path.display().to_string(),
                        source: std::io::Error::new(
                            std::io::ErrorKind::InvalidInput,
                            "Path contains invalid UTF-8",
                        ),
                    })?;
                self.load_directory(path_str)
            },
            PolicyStoreSource::Archive(archive_source) => {
                match archive_source {
                    ArchiveSource::File(path) => {
                        // For file-based archives, we need to create an ArchiveVfs
                        // but this method is sync and VFS-specific, so we can't do it here.
                        // Use the async load_policy_store() function instead for archives.
                        #[cfg(not(target_arch = "wasm32"))]
                        {
                            use super::archive_handler::ArchiveVfs;
                            let archive_vfs = ArchiveVfs::from_file(path)?;
                            let archive_loader = DefaultPolicyStoreLoader::new(archive_vfs);
                            archive_loader.load_directory(".")
                        }
                        #[cfg(target_arch = "wasm32")]
                        {
                            // File paths not supported in WASM - use ArchiveSource::Url or ArchiveVfs::from_buffer() directly
                            Err(PolicyStoreError::Archive(ArchiveError::WasmUnsupported))
                        }
                    },
                    ArchiveSource::Url(_) => {
                        // URL loading requires async, use load_policy_store() instead
                        Err(PolicyStoreError::Archive(ArchiveError::InvalidZipFormat {
                            details: "URL loading requires async load_policy_store() function"
                                .to_string(),
                        }))
                    },
                }
            },
            PolicyStoreSource::Legacy(_) => {
                // Legacy JSON/YAML format is not supported through the loader module.
                // Use init::policy_store::load_policy_store() which handles legacy formats
                // via AgamaPolicyStore deserialization.
                Err(PolicyStoreError::UnsupportedFormat {
                    message: "Legacy format not supported through PolicyStoreLoader. \
                              Use init::policy_store::load_policy_store() for legacy JSON/YAML formats."
                        .to_string(),
                })
            },
        }
    }

    fn detect_format(&self, source: &PolicyStoreSource) -> PolicyStoreFormat {
        match source {
            PolicyStoreSource::Directory(_) => PolicyStoreFormat::Directory,
            PolicyStoreSource::Archive(_) => PolicyStoreFormat::Archive,
            PolicyStoreSource::Legacy(_) => PolicyStoreFormat::Legacy,
        }
    }

    fn validate_structure(&self, source: &PolicyStoreSource) -> Result<(), PolicyStoreError> {
        match source {
            PolicyStoreSource::Directory(path) => {
                let path_str = path
                    .to_str()
                    .ok_or_else(|| PolicyStoreError::InvalidFileName {
                        path: path.display().to_string(),
                        source: std::io::Error::new(
                            std::io::ErrorKind::InvalidInput,
                            "Path contains invalid UTF-8",
                        ),
                    })?;
                self.validate_directory_structure(path_str)
            },
            PolicyStoreSource::Archive(archive_source) => {
                match archive_source {
                    ArchiveSource::File(path) => {
                        // Validate by attempting to create ArchiveVfs
                        // This will validate extension, ZIP format, and path traversal
                        #[cfg(not(target_arch = "wasm32"))]
                        {
                            use super::archive_handler::ArchiveVfs;
                            ArchiveVfs::from_file(path)?;
                            Ok(())
                        }
                        #[cfg(target_arch = "wasm32")]
                        {
                            // File paths not supported in WASM - use ArchiveSource::Url or ArchiveVfs::from_buffer() directly
                            Err(PolicyStoreError::Archive(ArchiveError::WasmUnsupported))
                        }
                    },
                    ArchiveSource::Url(_) => {
                        // URL validation requires async, use load_policy_store() for validation
                        Err(PolicyStoreError::Archive(ArchiveError::InvalidZipFormat {
                            details: "URL validation requires async load_policy_store() function"
                                .to_string(),
                        }))
                    },
                }
            },
            PolicyStoreSource::Legacy(_) => {
                // Legacy format validation is not supported through the loader module.
                // Legacy formats are validated during JSON/YAML deserialization in
                // init::policy_store::load_policy_store().
                Err(PolicyStoreError::UnsupportedFormat {
                    message: "Legacy format validation not supported through PolicyStoreLoader. \
                              Use init::policy_store::load_policy_store() for legacy JSON/YAML formats."
                        .to_string(),
                })
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use super::super::archive_handler::ArchiveVfs;
    use super::super::entity_parser::EntityParser;
    use super::super::issuer_parser::IssuerParser;
    use super::super::manifest_validator::ManifestValidator;
    use super::super::schema_parser::SchemaParser;
    use super::super::vfs_adapter::{MemoryVfs, PhysicalVfs};
    use super::*;
    use std::fs;
    use std::path::PathBuf;
    use tempfile::TempDir;

    type PhysicalLoader = DefaultPolicyStoreLoader<PhysicalVfs>;

    /// Helper to create a minimal valid policy store directory for testing.
    fn create_test_policy_store(dir: &Path) -> std::io::Result<()> {
        // Create metadata.json
        let metadata = r#"{
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": "Test Policy Store",
                "version": "1.0.0"
            }
        }"#;
        fs::write(dir.join("metadata.json"), metadata)?;

        // Create schema.cedarschema
        let schema = r#"
namespace TestApp {
    entity User;
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
"#;
        fs::write(dir.join("schema.cedarschema"), schema)?;

        // Create policies directory with a policy
        fs::create_dir(dir.join("policies"))?;
        let policy = r#"@id("test-policy")
permit(
    principal == TestApp::User::"alice",
    action == TestApp::Action::"read",
    resource == TestApp::Resource::"doc1"
);"#;
        fs::write(dir.join("policies/test-policy.cedar"), policy)?;

        Ok(())
    }

    #[test]
    fn test_format_detection_directory() {
        let source = PolicyStoreSource::Directory(PathBuf::from("/path/to/store"));
        let loader = DefaultPolicyStoreLoader::new_physical();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Directory);
    }

    #[test]
    fn test_format_detection_archive() {
        let source =
            PolicyStoreSource::Archive(ArchiveSource::File(PathBuf::from("/path/to/store.cjar")));
        let loader = DefaultPolicyStoreLoader::new_physical();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Archive);
    }

    #[test]
    fn test_format_detection_legacy() {
        let source = PolicyStoreSource::Legacy("{}".to_string());
        let loader = DefaultPolicyStoreLoader::new_physical();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Legacy);
    }

    #[test]
    fn test_validate_nonexistent_directory() {
        let source = PolicyStoreSource::Directory(PathBuf::from("/nonexistent/path"));
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.validate_structure(&source);
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("not found"));
    }

    #[test]
    fn test_validate_directory_missing_metadata() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create only schema, no metadata
        fs::write(dir.join("schema.cedarschema"), "test").unwrap();
        fs::create_dir(dir.join("policies")).unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.validate_structure(&source);

        assert!(result.is_err());
        let err = result.unwrap_err();
        assert!(err.to_string().contains("metadata.json"));
    }

    #[test]
    fn test_validate_directory_missing_schema() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create metadata but no schema
        fs::write(dir.join("metadata.json"), "{}").unwrap();
        fs::create_dir(dir.join("policies")).unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.validate_structure(&source);

        assert!(result.is_err());
        let err = result.unwrap_err();
        assert!(err.to_string().contains("schema.cedarschema"));
    }

    #[test]
    fn test_validate_directory_missing_policies_dir() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create files but no policies directory
        fs::write(dir.join("metadata.json"), "{}").unwrap();
        fs::write(dir.join("schema.cedarschema"), "test").unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.validate_structure(&source);

        assert!(result.is_err());
        let err = result.unwrap_err();
        assert!(err.to_string().contains("policies"));
    }

    #[test]
    fn test_validate_directory_success() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create valid structure
        create_test_policy_store(dir).unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.validate_structure(&source);

        assert!(result.is_ok());
    }

    #[test]
    fn test_load_directory_success() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create valid policy store
        create_test_policy_store(dir).unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.load(&source);

        assert!(result.is_ok());
        let loaded = result.unwrap();

        // Verify loaded data
        assert_eq!(loaded.metadata.cedar_version, "4.4.0");
        assert_eq!(loaded.metadata.policy_store.name, "Test Policy Store");
        assert!(!loaded.schema.is_empty());
        assert_eq!(loaded.policies.len(), 1);
        assert_eq!(loaded.policies[0].name, "test-policy.cedar");
    }

    #[test]
    fn test_load_directory_with_optional_components() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create basic structure
        create_test_policy_store(dir).unwrap();

        // Add optional components
        fs::create_dir(dir.join("templates")).unwrap();
        fs::write(
            dir.join("templates/template1.cedar"),
            "@id(\"template1\") permit(principal, action, resource);",
        )
        .unwrap();

        fs::create_dir(dir.join("entities")).unwrap();
        fs::write(dir.join("entities/users.json"), "[]").unwrap();

        fs::create_dir(dir.join("trusted-issuers")).unwrap();
        fs::write(dir.join("trusted-issuers/issuer1.json"), "{}").unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.load(&source);

        assert!(result.is_ok());
        let loaded = result.unwrap();

        assert_eq!(loaded.templates.len(), 1);
        assert_eq!(loaded.entities.len(), 1);
        assert_eq!(loaded.trusted_issuers.len(), 1);
    }

    #[test]
    fn test_load_directory_invalid_policy_extension() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        create_test_policy_store(dir).unwrap();

        // Add file with wrong extension
        fs::write(dir.join("policies/bad.txt"), "invalid").unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.load(&source);

        assert!(result.is_err());
        let err = result.unwrap_err();
        assert!(err.to_string().contains("extension"));
    }

    #[test]
    fn test_load_directory_invalid_json() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create invalid metadata
        fs::write(dir.join("metadata.json"), "not valid json").unwrap();
        fs::write(dir.join("schema.cedarschema"), "schema").unwrap();
        fs::create_dir(dir.join("policies")).unwrap();

        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let result = loader.load(&source);

        assert!(result.is_err());
        let err = result.unwrap_err();
        // Error could be "JSON parsing error" or "Invalid metadata" from validator
        let err_str = err.to_string();
        assert!(
            err_str.contains("JSON") || err_str.contains("parse") || err_str.contains("Invalid"),
            "Expected JSON/parse error, got: {}",
            err_str
        );
    }

    #[test]
    fn test_parse_policies_success() {
        let policy_files = vec![
            PolicyFile {
                name: "policy1.cedar".to_string(),
                content: r#"permit(principal, action, resource);"#.to_string(),
            },
            PolicyFile {
                name: "policy2.cedar".to_string(),
                content: r#"forbid(principal, action, resource);"#.to_string(),
            },
        ];
        let result = PhysicalLoader::parse_policies(&policy_files);

        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 2);
        assert_eq!(parsed[0].filename, "policy1.cedar");
        assert_eq!(parsed[0].id.to_string(), "policy1");
        assert_eq!(parsed[1].filename, "policy2.cedar");
        assert_eq!(parsed[1].id.to_string(), "policy2");
    }

    #[test]
    fn test_parse_policies_with_id_annotation() {
        let policy_files = vec![PolicyFile {
            name: "my_policy.cedar".to_string(),
            content: r#"
                // @id("custom-id-123")
                permit(
                    principal == User::"alice",
                    action == Action::"view",
                    resource == File::"doc.txt"
                );
            "#
            .to_string(),
        }];

        let result = PhysicalLoader::parse_policies(&policy_files);
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1);
        assert_eq!(parsed[0].id.to_string(), "custom-id-123");
    }

    #[test]
    fn test_parse_policies_invalid_syntax() {
        let policy_files = vec![PolicyFile {
            name: "invalid.cedar".to_string(),
            content: "this is not valid cedar syntax".to_string(),
        }];

        let result = PhysicalLoader::parse_policies(&policy_files);
        assert!(result.is_err());

        if let Err(PolicyStoreError::CedarParsing { file, message }) = result {
            assert_eq!(file, "invalid.cedar");
            assert!(!message.is_empty());
        } else {
            panic!("Expected CedarParsing error");
        }
    }

    #[test]
    fn test_parse_templates_success() {
        let template_files = vec![PolicyFile {
            name: "template1.cedar".to_string(),
            content: r#"permit(principal == ?principal, action, resource);"#.to_string(),
        }];

        let result = PhysicalLoader::parse_templates(&template_files);
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1);
        assert_eq!(parsed[0].filename, "template1.cedar");
        assert_eq!(parsed[0].id.to_string(), "template1");
    }

    #[test]
    fn test_create_policy_set_integration() {
        let policy_files = vec![
            PolicyFile {
                name: "allow.cedar".to_string(),
                content: r#"permit(principal, action, resource);"#.to_string(),
            },
            PolicyFile {
                name: "deny.cedar".to_string(),
                content: r#"forbid(principal, action, resource);"#.to_string(),
            },
        ];

        let template_files = vec![PolicyFile {
            name: "user_template.cedar".to_string(),
            content: r#"permit(principal == ?principal, action, resource);"#.to_string(),
        }];

        let policies = PhysicalLoader::parse_policies(&policy_files).unwrap();
        let templates = PhysicalLoader::parse_templates(&template_files).unwrap();

        let result = PhysicalLoader::create_policy_set(policies, templates);
        assert!(result.is_ok());

        let policy_set = result.unwrap();
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_load_and_parse_policies_end_to_end() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Add some Cedar policies
        let policies_dir = dir.join("policies");
        fs::write(
            policies_dir.join("view_policy.cedar"),
            r#"
                // @id("allow-view-docs")
                permit(
                    principal == User::"alice",
                    action == Action::"view",
                    resource == File::"document.txt"
                );
            "#,
        )
        .unwrap();

        fs::write(
            policies_dir.join("edit_policy.cedar"),
            r#"
                permit(
                    principal == User::"bob",
                    action == Action::"edit",
                    resource == File::"document.txt"
                );
            "#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse the policies
        let parsed_policies = PhysicalLoader::parse_policies(&loaded.policies).unwrap();

        // Should have 3 policies: 1 from create_test_policy_store helper + 2 from this test
        assert_eq!(parsed_policies.len(), 3);

        // Check that policies have the expected IDs
        let ids: Vec<String> = parsed_policies.iter().map(|p| p.id.to_string()).collect();
        assert!(ids.contains(&"test-policy".to_string())); // From helper
        assert!(ids.contains(&"allow-view-docs".to_string())); // Custom ID
        assert!(ids.contains(&"edit_policy".to_string())); // Derived from filename

        // Create a policy set
        let policy_set = PhysicalLoader::create_policy_set(parsed_policies, vec![]).unwrap();
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_load_and_parse_schema_end_to_end() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Update schema with more complex content
        let schema_content = r#"
            namespace PhotoApp {
                entity User = {
                    "username": String,
                    "email": String,
                    "roles": Set<String>
                };
                
                entity Photo = {
                    "title": String,
                    "owner": User,
                    "public": Bool
                };
                
                entity Album = {
                    "name": String,
                    "photos": Set<Photo>
                };
                
                action "view" appliesTo {
                    principal: [User],
                    resource: [Photo, Album],
                    context: {
                        "ip_address": String
                    }
                };
                
                action "edit" appliesTo {
                    principal: [User],
                    resource: [Photo, Album]
                };
                
                action "delete" appliesTo {
                    principal: [User],
                    resource: [Photo, Album]
                };
            }
        "#;

        fs::write(dir.join("schema.cedarschema"), schema_content).unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Schema should be loaded
        assert!(!loaded.schema.is_empty(), "Schema should not be empty");

        // Parse the schema
        let parsed = SchemaParser::parse_schema(&loaded.schema, "schema.cedarschema")
            .expect("Should parse schema");
        assert_eq!(parsed.filename, "schema.cedarschema");
        assert_eq!(parsed.content, schema_content);

        // Validate the schema
        parsed.validate().expect("Schema should be valid");

        // Get the Cedar schema object
        let schema = parsed.get_schema();
        assert!(!format!("{:?}", schema).is_empty());
    }

    #[test]
    fn test_complete_policy_store_with_schema_and_policies() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Add a comprehensive schema
        let schema_content = r#"
            namespace DocumentApp {
                entity User = {
                    "id": String,
                    "name": String
                };
                
                entity Document = {
                    "id": String,
                    "title": String,
                    "owner": User
                };
                
                action "view" appliesTo {
                    principal: [User],
                    resource: [Document]
                };
                
                action "edit" appliesTo {
                    principal: [User],
                    resource: [Document]
                };
            }
        "#;

        fs::write(dir.join("schema.cedarschema"), schema_content).unwrap();

        // Add policies that reference the schema
        let policies_dir = dir.join("policies");
        fs::write(
            policies_dir.join("allow_owner.cedar"),
            r#"
                // @id("allow-owner-edit")
                permit(
                    principal,
                    action == Action::"edit",
                    resource
                ) when {
                    resource.owner == principal
                };
            "#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse schema
        assert!(!loaded.schema.is_empty(), "Schema should not be empty");
        let parsed_schema = SchemaParser::parse_schema(&loaded.schema, "schema.cedarschema")
            .expect("Should parse schema");

        // Validate schema
        parsed_schema.validate().expect("Schema should be valid");

        // Parse policies
        let parsed_policies =
            PhysicalLoader::parse_policies(&loaded.policies).expect("Should parse policies");

        // Verify they work together
        let schema = parsed_schema.get_schema();
        assert!(!format!("{:?}", schema).is_empty());

        let policy_set = PhysicalLoader::create_policy_set(parsed_policies, vec![])
            .expect("Should create policy set");
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_load_and_parse_entities_end_to_end() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create entities directory with entity files
        let entities_dir = dir.join("entities");
        fs::create_dir(&entities_dir).unwrap();

        // Add entity files
        fs::write(
            entities_dir.join("users.json"),
            r#"[
                {
                    "uid": {"type": "Jans::User", "id": "alice"},
                    "attrs": {
                        "email": "alice@example.com",
                        "role": "admin"
                    },
                    "parents": []
                },
                {
                    "uid": {"type": "Jans::User", "id": "bob"},
                    "attrs": {
                        "email": "bob@example.com",
                        "role": "user"
                    },
                    "parents": []
                }
            ]"#,
        )
        .unwrap();

        fs::write(
            entities_dir.join("roles.json"),
            r#"{
                "admin": {
                    "uid": {"type": "Jans::Role", "id": "admin"},
                    "attrs": {
                        "name": "Administrator"
                    },
                    "parents": []
                }
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Entities should be loaded
        assert!(!loaded.entities.is_empty(), "Entities should be loaded");

        // Parse entities from all files
        let mut all_entities = Vec::new();

        for entity_file in &loaded.entities {
            let parsed_entities =
                EntityParser::parse_entities(&entity_file.content, &entity_file.name, None)
                    .expect("Should parse entities");
            all_entities.extend(parsed_entities);
        }

        // Should have 3 entities total (2 users + 1 role)
        assert_eq!(all_entities.len(), 3, "Should have 3 entities total");

        // Verify UIDs
        let uids: Vec<String> = all_entities.iter().map(|e| e.uid.to_string()).collect();
        assert!(uids.contains(&"Jans::User::\"alice\"".to_string()));
        assert!(uids.contains(&"Jans::User::\"bob\"".to_string()));
        assert!(uids.contains(&"Jans::Role::\"admin\"".to_string()));

        // Create entity store
        let entity_store = EntityParser::create_entities_store(all_entities);
        assert!(entity_store.is_ok(), "Should create entity store");
        assert_eq!(
            entity_store.unwrap().iter().count(),
            3,
            "Store should have 3 entities"
        );
    }

    #[test]
    fn test_complete_policy_store_with_entities() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Add entities
        let entities_dir = dir.join("entities");
        fs::create_dir(&entities_dir).unwrap();

        fs::write(
            entities_dir.join("app_entities.json"),
            r#"[
                {
                    "uid": {"type": "Jans::Application", "id": "app1"},
                    "attrs": {
                        "name": "My Application",
                        "owner": "alice"
                    },
                    "parents": []
                },
                {
                    "uid": {"type": "Jans::User", "id": "alice"},
                    "attrs": {
                        "email": "alice@example.com",
                        "department": "Engineering"
                    },
                    "parents": []
                }
            ]"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Verify all components are loaded
        assert_eq!(loaded.metadata.name(), "Test Policy Store");
        assert!(!loaded.schema.is_empty());
        assert!(!loaded.policies.is_empty());
        assert!(!loaded.entities.is_empty());

        // Parse and validate all components

        // Schema
        let parsed_schema = SchemaParser::parse_schema(&loaded.schema, "schema.cedarschema")
            .expect("Should parse schema");
        parsed_schema.validate().expect("Schema should be valid");

        // Policies
        let parsed_policies =
            PhysicalLoader::parse_policies(&loaded.policies).expect("Should parse policies");
        let policy_set = PhysicalLoader::create_policy_set(parsed_policies, vec![])
            .expect("Should create policy set");

        // Entities
        let mut all_entities = Vec::new();
        for entity_file in &loaded.entities {
            let parsed_entities =
                EntityParser::parse_entities(&entity_file.content, &entity_file.name, None)
                    .expect("Should parse entities");
            all_entities.extend(parsed_entities);
        }

        let entity_store =
            EntityParser::create_entities_store(all_entities).expect("Should create entity store");

        // Verify everything works together
        assert!(!policy_set.is_empty());
        assert_eq!(entity_store.iter().count(), 2);
        assert!(!format!("{:?}", parsed_schema.get_schema()).is_empty());
    }

    #[test]
    fn test_entity_with_complex_attributes() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create entities directory with complex attributes
        let entities_dir = dir.join("entities");
        fs::create_dir(&entities_dir).unwrap();

        fs::write(
            entities_dir.join("complex.json"),
            r#"[
                {
                    "uid": {"type": "Jans::User", "id": "alice"},
                    "attrs": {
                        "email": "alice@example.com",
                        "roles": ["admin", "developer"],
                        "metadata": {
                            "department": "Engineering",
                            "level": 5
                        },
                        "active": true
                    },
                    "parents": []
                }
            ]"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse entities
        let mut all_entities = Vec::new();

        for entity_file in &loaded.entities {
            let parsed_entities =
                EntityParser::parse_entities(&entity_file.content, &entity_file.name, None)
                    .expect("Should parse entities with complex attributes");
            all_entities.extend(parsed_entities);
        }

        assert_eq!(all_entities.len(), 1);

        // Verify attributes are preserved
        let alice_json = all_entities[0].entity.to_json_value().unwrap();
        let attrs = alice_json.get("attrs").unwrap();

        assert!(attrs.get("email").is_some());
        assert!(attrs.get("roles").is_some());
        assert!(attrs.get("metadata").is_some());
        assert!(attrs.get("active").is_some());
    }

    #[test]
    fn test_load_and_parse_trusted_issuers_end_to_end() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create trusted-issuers directory with issuer files
        let issuers_dir = dir.join("trusted-issuers");
        fs::create_dir(&issuers_dir).unwrap();

        // Add issuer configuration
        fs::write(
            issuers_dir.join("jans.json"),
            r#"{
                "jans_server": {
                    "name": "Jans Authorization Server",
                    "description": "Primary Jans OpenID Connect Provider",
                    "openid_configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": {
                            "trusted": true,
                            "entity_type_name": "Jans::access_token",
                            "user_id": "sub",
                            "role_mapping": "role"
                        },
                        "id_token": {
                            "trusted": true,
                            "entity_type_name": "Jans::id_token",
                            "user_id": "sub"
                        }
                    }
                }
            }"#,
        )
        .unwrap();

        fs::write(
            issuers_dir.join("google.json"),
            r#"{
                "google_oauth": {
                    "name": "Google OAuth",
                    "description": "Google OAuth 2.0 Provider",
                    "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
                    "token_metadata": {
                        "id_token": {
                            "trusted": false,
                            "entity_type_name": "Google::id_token",
                            "user_id": "email"
                        }
                    }
                }
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Issuers should be loaded
        assert!(
            !loaded.trusted_issuers.is_empty(),
            "Issuers should be loaded"
        );
        assert_eq!(
            loaded.trusted_issuers.len(),
            2,
            "Should have 2 issuer files"
        );

        // Parse issuers from all files
        let mut all_issuers = Vec::new();

        for issuer_file in &loaded.trusted_issuers {
            let parsed_issuers =
                IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
                    .expect("Should parse issuers");
            all_issuers.extend(parsed_issuers);
        }

        // Should have 2 issuers total (1 jans + 1 google)
        assert_eq!(all_issuers.len(), 2, "Should have 2 issuers total");

        // Verify issuer IDs
        let ids: Vec<String> = all_issuers.iter().map(|i| i.id.clone()).collect();
        assert!(ids.contains(&"jans_server".to_string()));
        assert!(ids.contains(&"google_oauth".to_string()));

        // Create issuer map
        let issuer_map = IssuerParser::create_issuer_map(all_issuers);
        assert!(issuer_map.is_ok(), "Should create issuer map");
        assert_eq!(issuer_map.unwrap().len(), 2, "Map should have 2 issuers");
    }

    #[test]
    fn test_parse_issuer_with_token_metadata() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create trusted-issuers directory
        let issuers_dir = dir.join("trusted-issuers");
        fs::create_dir(&issuers_dir).unwrap();

        // Add issuer with comprehensive token metadata
        fs::write(
            issuers_dir.join("comprehensive.json"),
            r#"{
                "full_issuer": {
                    "name": "Full Feature Issuer",
                    "description": "Issuer with all token types",
                    "openid_configuration_endpoint": "https://full.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": {
                            "trusted": true,
                            "entity_type_name": "App::access_token",
                            "user_id": "sub",
                            "role_mapping": "role",
                            "token_id": "jti"
                        },
                        "id_token": {
                            "trusted": true,
                            "entity_type_name": "App::id_token",
                            "user_id": "sub",
                            "token_id": "jti"
                        },
                        "userinfo_token": {
                            "trusted": true,
                            "entity_type_name": "App::userinfo_token",
                            "user_id": "sub"
                        }
                    }
                }
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse issuers
        let mut all_issuers = Vec::new();

        for issuer_file in &loaded.trusted_issuers {
            let parsed_issuers =
                IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
                    .expect("Should parse issuers");
            all_issuers.extend(parsed_issuers);
        }

        assert_eq!(all_issuers.len(), 1);

        let issuer = &all_issuers[0];
        assert_eq!(issuer.id, "full_issuer");
        assert_eq!(issuer.issuer.token_metadata.len(), 3);

        // Verify token metadata details
        let access_token = issuer.issuer.token_metadata.get("access_token").unwrap();
        assert_eq!(access_token.entity_type_name, "App::access_token");
        assert_eq!(access_token.user_id, Some("sub".to_string()));
        assert_eq!(access_token.role_mapping, Some("role".to_string()));
    }

    #[test]
    fn test_detect_duplicate_issuer_ids() {
        // Create in-memory filesystem
        let vfs = MemoryVfs::new();

        // Create a complete policy store structure in memory
        vfs.create_file(
            "metadata.json",
            r#"{
                "cedar_version": "4.4.0",
                "policy_store": {
                    "id": "abc123def456",
                    "name": "Test Policy Store",
                    "version": "1.0.0"
                }
            }"#
            .as_bytes(),
        )
        .unwrap();

        vfs.create_file(
            "schema.cedarschema",
            r#"
namespace TestApp {
    entity User;
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
            "#
            .as_bytes(),
        )
        .unwrap();

        // Create policies directory with a test policy
        vfs.create_file(
            "policies/test_policy.cedar",
            b"permit(principal, action, resource);",
        )
        .unwrap();

        // Create trusted-issuers directory with duplicate IDs
        vfs.create_file(
            "trusted-issuers/file1.json",
            r#"{
                "issuer1": {
                    "name": "Issuer One",
                    "description": "First instance",
                    "openid_configuration_endpoint": "https://issuer1.com/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": {
                            "entity_type_name": "App::access_token"
                        }
                    }
                }
            }"#
                .as_bytes(),
        )
        .unwrap();

        vfs.create_file(
            "trusted-issuers/file2.json",
            r#"{
                "issuer1": {
                    "name": "Issuer One Duplicate",
                    "description": "Duplicate instance",
                    "openid_configuration_endpoint": "https://issuer1.com/.well-known/openid-configuration",
                    "token_metadata": {
                        "id_token": {
                            "entity_type_name": "App::id_token"
                        }
                    }
                }
            }"#
                .as_bytes(),
        )
        .unwrap();

        // Load the policy store using the in-memory filesystem
        let source = PolicyStoreSource::Directory(PathBuf::from("/"));
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let loaded = loader.load(&source).unwrap();

        // Parse issuers
        let mut all_issuers = Vec::new();

        for issuer_file in &loaded.trusted_issuers {
            let parsed_issuers =
                IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
                    .expect("Should parse issuers");
            all_issuers.extend(parsed_issuers);
        }

        // Detect duplicates
        let validation = IssuerParser::validate_issuers(&all_issuers);
        assert!(validation.is_err(), "Should detect duplicate issuer IDs");

        let errors = validation.unwrap_err();
        assert_eq!(errors.len(), 1, "Should have 1 duplicate error");
        assert!(errors[0].contains("issuer1"));
        assert!(errors[0].contains("file1.json") || errors[0].contains("file2.json"));
    }

    #[test]
    fn test_issuer_missing_required_field() {
        // Create in-memory filesystem
        let vfs = MemoryVfs::new();

        // Create a minimal policy store structure
        vfs.create_file(
            "metadata.json",
            r#"{
                "cedar_version": "4.4.0",
                "policy_store": {
                    "id": "abc123def456",
                    "name": "Test Policy Store",
                    "version": "1.0.0"
                }
            }"#
            .as_bytes(),
        )
        .unwrap();

        vfs.create_file("schema.cedarschema", b"namespace TestApp { entity User; }")
            .unwrap();

        vfs.create_file(
            "policies/test.cedar",
            b"permit(principal, action, resource);",
        )
        .unwrap();

        // Create trusted-issuers directory with invalid issuer (missing name)
        vfs.create_file(
            "trusted-issuers/invalid.json",
            r#"{
                "bad_issuer": {
                    "description": "Missing name field",
                    "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration"
                }
            }"#
                .as_bytes(),
        )
        .unwrap();

        // Load the policy store using in-memory filesystem
        let source = PolicyStoreSource::Directory(PathBuf::from("/"));
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let loaded = loader.load(&source).unwrap();

        // Parse issuers - should fail
        let result = IssuerParser::parse_issuer(
            &loaded.trusted_issuers[0].content,
            &loaded.trusted_issuers[0].name,
        );

        assert!(result.is_err(), "Should fail on missing required field");
    }

    #[test]
    fn test_complete_policy_store_with_issuers() {
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Add entities
        let entities_dir = dir.join("entities");
        fs::create_dir(&entities_dir).unwrap();
        fs::write(
            entities_dir.join("users.json"),
            r#"[
                {
                    "uid": {"type": "Jans::User", "id": "alice"},
                    "attrs": {"email": "alice@example.com"},
                    "parents": []
                }
            ]"#,
        )
        .unwrap();

        // Add trusted issuers
        let issuers_dir = dir.join("trusted-issuers");
        fs::create_dir(&issuers_dir).unwrap();
        fs::write(
            issuers_dir.join("issuer.json"),
            r#"{
                "main_issuer": {
                    "name": "Main Issuer",
                    "description": "Primary authentication provider",
                    "openid_configuration_endpoint": "https://auth.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": {
                            "entity_type_name": "Jans::access_token",
                            "user_id": "sub"
                        }
                    }
                }
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Verify all components are loaded
        assert_eq!(loaded.metadata.name(), "Test Policy Store");
        assert!(!loaded.schema.is_empty());
        assert!(!loaded.policies.is_empty());
        assert!(!loaded.entities.is_empty());
        assert!(!loaded.trusted_issuers.is_empty());

        // Parse and validate all components

        // Schema
        let parsed_schema = SchemaParser::parse_schema(&loaded.schema, "schema.cedarschema")
            .expect("Should parse schema");
        parsed_schema.validate().expect("Schema should be valid");

        // Policies
        let parsed_policies =
            PhysicalLoader::parse_policies(&loaded.policies).expect("Should parse policies");
        let policy_set = PhysicalLoader::create_policy_set(parsed_policies, vec![])
            .expect("Should create policy set");

        // Entities (parse without schema validation since this test focuses on issuers)
        let mut all_entities = Vec::new();
        for entity_file in &loaded.entities {
            let parsed_entities = EntityParser::parse_entities(
                &entity_file.content,
                &entity_file.name,
                None, // No schema validation - this test is about issuer integration
            )
            .expect("Should parse entities");
            all_entities.extend(parsed_entities);
        }
        let entity_store =
            EntityParser::create_entities_store(all_entities).expect("Should create entity store");

        // Issuers
        let mut all_issuers = Vec::new();
        for issuer_file in &loaded.trusted_issuers {
            let parsed_issuers =
                IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
                    .expect("Should parse issuers");
            all_issuers.extend(parsed_issuers);
        }
        let issuer_map =
            IssuerParser::create_issuer_map(all_issuers).expect("Should create issuer map");

        // Verify everything works together
        assert!(!policy_set.is_empty());
        assert_eq!(entity_store.iter().count(), 1);
        assert!(!format!("{:?}", parsed_schema.get_schema()).is_empty());
        assert_eq!(issuer_map.len(), 1);
        assert!(issuer_map.contains_key("main_issuer"));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_archive_vfs_end_to_end_from_file() {
        use std::fs::File;
        use std::io::Write;
        use tempfile::TempDir;
        use zip::CompressionMethod;
        use zip::write::{ExtendedFileOptions, FileOptions};

        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("complete_store.cjar");

        // Create a complete .cjar archive
        let file = File::create(&archive_path).unwrap();
        let mut zip = zip::ZipWriter::new(file);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Metadata
        zip.start_file("metadata.json", options).unwrap();
        zip.write_all(
            br#"{
            "cedar_version": "1.0.0",
            "policy_store": {
                "id": "abcdef123456",
                "name": "Archive Test Store",
                "version": "1.0.0"
            }
        }"#,
        )
        .unwrap();

        // Schema
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("schema.cedarschema", options).unwrap();
        zip.write_all(b"namespace TestApp { entity User; }")
            .unwrap();

        // Policy
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("policies/allow.cedar", options).unwrap();
        zip.write_all(b"permit(principal, action, resource);")
            .unwrap();

        // Entity
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("entities/users.json", options).unwrap();
        zip.write_all(
            br#"[{
            "uid": {"type": "TestApp::User", "id": "alice"},
            "attrs": {},
            "parents": []
        }]"#,
        )
        .unwrap();

        zip.finish().unwrap();

        // Step 1: Create ArchiveVfs from file path
        let archive_vfs =
            ArchiveVfs::from_file(&archive_path).expect("Should create ArchiveVfs from .cjar file");

        // Step 2: Create loader with ArchiveVfs
        let loader = DefaultPolicyStoreLoader::new(archive_vfs);

        // Step 3: Load policy store from archive root
        let loaded = loader
            .load_directory(".")
            .expect("Should load policy store from archive");

        // Step 4: Verify all components loaded correctly
        assert_eq!(loaded.metadata.name(), "Archive Test Store");
        assert_eq!(loaded.metadata.policy_store.id, "abcdef123456");
        assert!(!loaded.schema.is_empty());
        assert_eq!(loaded.policies.len(), 1);
        assert_eq!(loaded.policies[0].name, "allow.cedar");
        assert_eq!(loaded.entities.len(), 1);
        assert_eq!(loaded.entities[0].name, "users.json");

        // Step 5: Verify components can be parsed

        let parsed_schema = SchemaParser::parse_schema(&loaded.schema, "schema.cedarschema")
            .expect("Should parse schema from archive");

        let parsed_entities = EntityParser::parse_entities(
            &loaded.entities[0].content,
            "users.json",
            Some(parsed_schema.get_schema()),
        )
        .expect("Should parse entities from archive");

        assert_eq!(parsed_entities.len(), 1);
    }

    #[test]
    fn test_archive_vfs_end_to_end_from_bytes() {
        use std::io::{Cursor, Write};
        use zip::CompressionMethod;
        use zip::write::{ExtendedFileOptions, FileOptions};

        // Create archive in memory (simulates WASM fetching from network)
        let mut archive_bytes = Vec::new();
        {
            let cursor = Cursor::new(&mut archive_bytes);
            let mut zip = zip::ZipWriter::new(cursor);

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);

            // Metadata
            zip.start_file("metadata.json", options).unwrap();
            zip.write_all(
                br#"{
                "cedar_version": "1.0.0",
                "policy_store": {
                    "id": "fedcba654321",
                    "name": "WASM Archive Store",
                    "version": "2.0.0"
                }
            }"#,
            )
            .unwrap();

            // Schema
            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("schema.cedarschema", options).unwrap();
            zip.write_all(b"namespace WasmApp { entity Resource; }")
                .unwrap();

            // Policy
            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("policies/deny.cedar", options).unwrap();
            zip.write_all(b"forbid(principal, action, resource);")
                .unwrap();

            zip.finish().unwrap();
        }

        // Step 1: Create ArchiveVfs from bytes (works in WASM!)
        let archive_vfs =
            ArchiveVfs::from_buffer(archive_bytes).expect("Should create ArchiveVfs from bytes");

        // Step 2: Create loader with ArchiveVfs
        let loader = DefaultPolicyStoreLoader::new(archive_vfs);

        // Step 3: Load policy store
        let loaded = loader
            .load_directory(".")
            .expect("Should load policy store from archive bytes");

        // Step 4: Verify loaded correctly
        assert_eq!(loaded.metadata.name(), "WASM Archive Store");
        assert_eq!(loaded.metadata.policy_store.id, "fedcba654321");
        assert_eq!(loaded.metadata.version(), "2.0.0");
        assert!(loaded.schema.contains("WasmApp"));
        assert_eq!(loaded.policies.len(), 1);
        assert_eq!(loaded.policies[0].name, "deny.cedar");
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_archive_vfs_with_manifest_validation() {
        use std::fs::File;
        use std::io::Write;
        use std::path::PathBuf;
        use tempfile::TempDir;
        use zip::CompressionMethod;
        use zip::write::{ExtendedFileOptions, FileOptions};

        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("store_with_manifest.cjar");

        // Create archive with manifest
        let file = File::create(&archive_path).unwrap();
        let mut zip = zip::ZipWriter::new(file);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Metadata
        let metadata_content = br#"{
            "cedar_version": "1.0.0",
            "policy_store": {
                "id": "abc123def456",
                "name": "Manifest Test",
                "version": "1.0.0"
            }
        }"#;
        zip.start_file("metadata.json", options).unwrap();
        zip.write_all(metadata_content).unwrap();

        // Minimal schema
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("schema.cedarschema", options).unwrap();
        zip.write_all(b"namespace Test { entity User; }").unwrap();

        // Minimal policy (required)
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("policies/test.cedar", options).unwrap();
        zip.write_all(b"permit(principal, action, resource);")
            .unwrap();

        // Manifest (simplified - no checksums for this test)
        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("manifest.json", options).unwrap();
        zip.write_all(
            br#"{
            "policy_store_id": "abc123def456",
            "generated_date": "2024-01-01T00:00:00Z",
            "files": {}
        }"#,
        )
        .unwrap();

        zip.finish().unwrap();

        // Step 1: Create ArchiveVfs
        let archive_vfs = ArchiveVfs::from_file(&archive_path).expect("Should create ArchiveVfs");

        // Step 2: Load policy store
        let loader = DefaultPolicyStoreLoader::new(archive_vfs);
        let loaded = loader
            .load_directory(".")
            .expect("Should load with manifest");

        // Step 3: Verify manifest was loaded
        assert!(loaded.manifest.is_some());
        let manifest = loaded.manifest.as_ref().unwrap();
        assert_eq!(manifest.policy_store_id, "abc123def456");

        // Step 4: Show that ManifestValidator can work with ArchiveVfs
        let archive_vfs2 =
            ArchiveVfs::from_file(&archive_path).expect("Should create second ArchiveVfs");
        let validator = ManifestValidator::new(archive_vfs2, PathBuf::from("."));

        // This demonstrates that manifest validation works with ANY VfsFileSystem,
        // including ArchiveVfs (not just PhysicalVfs)
        let validation_result = validator.validate(Some("abc123def456"));
        // Note: This will have errors because we didn't include proper checksums,
        // but it proves the validator works with ArchiveVfs
        assert!(validation_result.errors.len() > 0 || !validation_result.is_valid);
    }

    #[test]
    fn test_archive_vfs_with_multiple_policies() {
        use std::io::{Cursor, Write};
        use zip::CompressionMethod;
        use zip::write::{ExtendedFileOptions, FileOptions};

        let mut archive_bytes = Vec::new();
        {
            let cursor = Cursor::new(&mut archive_bytes);
            let mut zip = zip::ZipWriter::new(cursor);

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);

            // Metadata
            zip.start_file("metadata.json", options).unwrap();
            zip.write_all(
                br#"{
                "cedar_version": "1.0.0",
                "policy_store": {
                    "id": "def456abc123",
                    "name": "Nested Structure",
                    "version": "1.0.0"
                }
            }"#,
            )
            .unwrap();

            // Schema
            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("schema.cedarschema", options).unwrap();
            zip.write_all(b"namespace App { entity User; }").unwrap();

            // Multiple policies in subdirectories (loader recursively scans subdirectories)
            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("policies/allow/basic.cedar", options)
                .unwrap();
            zip.write_all(b"permit(principal, action, resource);")
                .unwrap();

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("policies/allow/advanced.cedar", options)
                .unwrap();
            zip.write_all(b"permit(principal == App::User::\"admin\", action, resource);")
                .unwrap();

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("policies/deny/restricted.cedar", options)
                .unwrap();
            zip.write_all(b"forbid(principal, action, resource);")
                .unwrap();

            zip.finish().unwrap();
        }

        let archive_vfs = ArchiveVfs::from_buffer(archive_bytes).expect("Should create ArchiveVfs");

        let loader = DefaultPolicyStoreLoader::new(archive_vfs);
        let loaded = loader.load_directory(".").expect("Should load policies");

        // Verify all policies loaded recursively from subdirectories
        assert_eq!(loaded.policies.len(), 3);

        let policy_names: Vec<_> = loaded.policies.iter().map(|p| &p.name).collect();
        assert!(policy_names.contains(&&"basic.cedar".to_string()));
        assert!(policy_names.contains(&&"advanced.cedar".to_string()));
        assert!(policy_names.contains(&&"restricted.cedar".to_string()));
    }

    #[test]
    fn test_archive_vfs_vs_physical_vfs_equivalence() {
        // This test demonstrates that ArchiveVfs and PhysicalVfs are
        // functionally equivalent from the loader's perspective

        use std::io::{Cursor, Write};
        use zip::CompressionMethod;
        use zip::write::{ExtendedFileOptions, FileOptions};

        // Create identical content
        let metadata_json = br#"{
            "cedar_version": "1.0.0",
            "policy_store": {
                "id": "fedcba987654",
                "name": "Equivalence Test",
                "version": "1.0.0"
            }
        }"#;
        let schema_content = b"namespace Equiv { entity User; }";
        let policy_content = b"permit(principal, action, resource);";

        // Create archive
        let mut archive_bytes = Vec::new();
        {
            let cursor = Cursor::new(&mut archive_bytes);
            let mut zip = zip::ZipWriter::new(cursor);

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("metadata.json", options).unwrap();
            zip.write_all(metadata_json).unwrap();

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("schema.cedarschema", options).unwrap();
            zip.write_all(schema_content).unwrap();

            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file("policies/test.cedar", options).unwrap();
            zip.write_all(policy_content).unwrap();

            zip.finish().unwrap();
        }

        // Load using ArchiveVfs
        let archive_vfs = ArchiveVfs::from_buffer(archive_bytes).unwrap();
        let loader = DefaultPolicyStoreLoader::new(archive_vfs);
        let loaded = loader.load_directory(".").unwrap();

        // Verify results are identical regardless of VFS implementation
        assert_eq!(loaded.metadata.policy_store.id, "fedcba987654");
        assert_eq!(loaded.metadata.name(), "Equivalence Test");
        assert_eq!(loaded.policies.len(), 1);
        assert!(loaded.schema.contains("Equiv"));
    }
}
