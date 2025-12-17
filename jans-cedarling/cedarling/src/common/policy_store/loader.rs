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
//!
//! ## Example: Loading an archive (native, internal)
//!
//! ```ignore
//! use cedarling::common::policy_store::archive_handler::ArchiveVfs;
//! use cedarling::common::policy_store::loader::DefaultPolicyStoreLoader;
//!
//! // Create archive VFS (validates format during construction)
//! let archive_vfs = ArchiveVfs::from_file("policy_store.cjar")?;
//!
//! // Create loader with archive VFS
//! let loader = DefaultPolicyStoreLoader::new(archive_vfs);
//!
//! // Load policy store from root directory of archive
//! let loaded = loader.load_directory(".")?;
//! ```
//!
//! ## Example: Loading archive in WASM (internal)
//!
//! ```ignore
//! use cedarling::common::policy_store::archive_handler::ArchiveVfs;
//! use cedarling::common::policy_store::loader::DefaultPolicyStoreLoader;
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
//! ```

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};

use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;
use std::path::Path;

/// Load a policy store from a directory path.
///
/// This function uses `PhysicalVfs` to read from the local filesystem.
/// It is only available on native platforms (not WASM).
///
/// # Example
///
/// ```text
/// use crate::common::policy_store::loader::load_policy_store_directory;
/// use std::path::Path;
///
/// async fn example() -> Result<(), Box<dyn std::error::Error>> {
///     let loaded = load_policy_store_directory(Path::new("./policy_store")).await?;
///     println!("Loaded store: {}", loaded.metadata.policy_store.name);
///     Ok(())
/// }
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
/// ```text
/// use crate::common::policy_store::loader::load_policy_store_archive;
/// use std::path::Path;
///
/// async fn example() -> Result<(), Box<dyn std::error::Error>> {
///     let loaded = load_policy_store_archive(Path::new("./policy_store.cjar")).await?;
///     println!("Loaded store: {}", loaded.metadata.policy_store.name);
///     Ok(())
/// }
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
    Err(PolicyStoreError::PathNotFound {
        path: "File-based archive loading not supported in WASM".to_string(),
    })
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
/// ```text
/// use crate::common::policy_store::loader::load_policy_store_archive_bytes;
///
/// async fn example() -> Result<(), Box<dyn std::error::Error>> {
///     // Fetch archive bytes from network, storage, etc.
///     let archive_bytes: Vec<u8> = fetch_from_network().await?;
///     let loaded = load_policy_store_archive_bytes(archive_bytes)?;
///     println!("Loaded store: {}", loaded.metadata.policy_store.name);
///     Ok(())
/// }
///
/// async fn fetch_from_network() -> Result<Vec<u8>, Box<dyn std::error::Error>> { Ok(vec![]) }
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

    /// Create a Cedar PolicySet from parsed policies and templates.
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
