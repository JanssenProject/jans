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
use super::metadata::PolicyStoreMetadata;
use super::schema_parser::{ParsedSchema, SchemaFile};
use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;

/// Load a policy store from a directory path.
///
/// This function uses `PhysicalVfs` to read from the local filesystem.
/// It is only available on native platforms (not WASM).
#[cfg(not(target_arch = "wasm32"))]
pub(crate) async fn load_policy_store_directory(
    path: &Path,
    strict: bool,
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
        loader.load_directory(&path_str, strict)
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
    _strict: bool,
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
    strict: bool,
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
        let loaded_directory = loader.load_directory(".", strict)?;

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

/// Load a policy store from a Cedar Archive (.cjar) file (WASM stub).
///
/// File-based archive loading is not supported in WASM environments.
/// Use `load_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub(crate) fn load_policy_store_archive(
    _path: &Path,
    _strict: bool,
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
    strict: bool,
) -> Result<LoadedPolicyStore, PolicyStoreError> {
    use super::archive_handler::ArchiveVfs;

    let archive_vfs = ArchiveVfs::from_buffer(bytes.to_owned())?;
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    loader.load_directory(".", strict)
}

/// A loaded policy store with all its components.
#[derive(Debug)]
pub(crate) struct LoadedPolicyStore {
    /// Policy store metadata
    pub metadata: PolicyStoreMetadata,
    /// Parsed schema (optional — absent when running without schema)
    pub schema: Option<ParsedSchema>,
    /// Whether a schema source (schema.cedarschema or schemas/ dir) was present
    /// in the store, regardless of whether it was loaded/parsed.
    /// Used to distinguish "explicitly no schema" from "schema skipped by
    /// strict_schema_validation=false" in log messages.
    pub schema_source_exists: bool,
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

/// Describes where a schema source was found in the policy store.
enum SchemaSource {
    /// `schema.cedarschema` exists at this path.
    SingleFile { path: String, schemas_dir: String },
    /// `schemas/` directory exists at this path.
    Directory(String),
    /// No schema source found.
    None { searched_file: String, searched_dir: String },
}

impl SchemaSource {
    fn exists(&self) -> bool {
        matches!(
            self,
            SchemaSource::SingleFile { .. } | SchemaSource::Directory(_)
        )
    }
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

        // Check for required files
        let metadata_path = Self::join_path(dir, "metadata.json");
        if !self.vfs.exists(&metadata_path) {
            return Err(ValidationError::MissingRequiredFile {
                file: "metadata.json".to_string(),
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

    /// Resolve where the schema lives (or that it's absent), without any I/O
    /// beyond lightweight existence checks.
    fn resolve_schema_source(&self, dir: &str) -> SchemaSource {
        let schema_path = Self::join_path(dir, "schema.cedarschema");
        if self.vfs.exists(&schema_path) {
            return SchemaSource::SingleFile {
                path: schema_path,
                schemas_dir: Self::join_path(dir, "schemas"),
            };
        }
        let schemas_dir = Self::join_path(dir, "schemas");
        if self.vfs.exists(&schemas_dir) {
            return SchemaSource::Directory(schemas_dir);
        }
        SchemaSource::None {
            searched_file: schema_path,
            searched_dir: schemas_dir,
        }
    }

    /// Load and parse schema from a pre-resolved [`SchemaSource`].
    /// Returns `Ok(None)` when no schema source exists.
    fn load_schema(
        &self,
        source: &SchemaSource,
    ) -> Result<Option<ParsedSchema>, PolicyStoreError> {
        match source {
            SchemaSource::SingleFile { path, .. } => {
                let content = self
                    .read_schema_file_content(path)?
                    .ok_or_else(|| {
                        PolicyStoreError::Validation(ValidationError::MissingSchemaSource {
                            searched_file: path.clone(),
                            searched_dir: String::new(),
                        })
                    })?;
                Ok(Some(ParsedSchema::parse(&content, "schema.cedarschema")?))
            },
            SchemaSource::Directory(path) => self.load_schema_from_directory(path),
            SchemaSource::None { .. } => Ok(None),
        }
    }

    /// Read the raw content of a single schema file.
    /// Returns `None` if the file does not exist (no parsing).
    fn read_schema_file_content(&self, path: &str) -> Result<Option<String>, PolicyStoreError> {
        match self.vfs.read_file(path) {
            Ok(bytes) => {
                String::from_utf8(bytes)
                    .map(Some)
                    .map_err(|e| PolicyStoreError::FileReadError {
                        path: path.to_string(),
                        source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
                    })
            },
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(None),
            Err(source) => Err(PolicyStoreError::FileReadError {
                path: path.to_string(),
                source,
            }),
        }
    }

    /// Load schema from the schemas/ directory, combining all `.cedarschema` files.
    fn load_schema_from_directory(
        &self,
        path: &str,
    ) -> Result<Option<ParsedSchema>, PolicyStoreError> {
        if !self.vfs.exists(path) {
            return Ok(None);
        }
        if !self.vfs.is_dir(path) {
            return Err(PolicyStoreError::NotADirectory {
                path: path.to_string(),
            });
        }

        let entries =
            self.vfs
                .read_dir(path)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: path.to_string(),
                    source,
                })?;

        let raw_files = self.read_schema_files(entries)?;
        if raw_files.is_empty() {
            return Err(ValidationError::EmptySchemaDirectory {
                path: path.to_string(),
            }
            .into());
        }

        let parsed = Self::combine_schema_files(&raw_files)?;
        Ok(Some(parsed))
    }

    /// Read and validate all `.cedarschema` files from directory entries.
    fn read_schema_files(
        &self,
        entries: Vec<super::vfs_adapter::DirEntry>,
    ) -> Result<Vec<SchemaFile>, PolicyStoreError> {
        let mut files: Vec<SchemaFile> = Vec::new();
        for entry in entries {
            if entry.is_dir {
                continue;
            }
            if !entry.name.to_lowercase().ends_with(".cedarschema") {
                continue;
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
            files.push(SchemaFile {
                name: entry.name,
                content,
            });
        }
        files.sort_by(|a, b| a.name.cmp(&b.name));
        Ok(files)
    }

    /// Combine multiple `.cedarschema` files into a single parsed schema via
    /// [`ParsedSchema::parse_multiple`].
    fn combine_schema_files(raw_files: &[SchemaFile]) -> Result<ParsedSchema, PolicyStoreError> {
        ParsedSchema::parse_multiple(raw_files)
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
    /// When `strict` is `true`, a missing schema source raises `MissingSchemaSource`.
    /// When `false`, a missing schema is allowed (schemaless mode).
    ///
    /// This method is generic over the underlying `VfsFileSystem`.
    pub(super) fn load_directory(
        &self,
        dir: &str,
        strict: bool,
    ) -> Result<LoadedPolicyStore, PolicyStoreError> {
        // Validate structure first
        self.validate_directory_structure(dir)?;

        // Lightweight existence check (no parsing) — result reused by load_schema
        // and for the schema_source_exists log flag.
        let schema_source = self.resolve_schema_source(dir);
        let schema_source_exists = schema_source.exists();

        let metadata = self.load_metadata(dir)?;
        let schema = if strict {
            match &schema_source {
                SchemaSource::None { searched_file, searched_dir } => {
                    return Err(PolicyStoreError::Validation(
                        ValidationError::MissingSchemaSource {
                            searched_file: searched_file.clone(),
                            searched_dir: searched_dir.clone(),
                        },
                    ));
                },
                _ => self.load_schema(&schema_source)?,
            }
        } else {
            None
        };
        let policies = self.load_policies(dir)?;
        let templates = self.load_templates(dir)?;
        let entities = self.load_entities(dir)?;
        let trusted_issuers = self.load_trusted_issuers(dir)?;

        Ok(LoadedPolicyStore {
            metadata,
            schema,
            schema_source_exists,
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
        let mut parsed_policies = Vec::new();
        for file in policy_files {
            let parsed_list = policy_parser::PolicyParser::parse_policy(&file.content, &file.name)?;
            parsed_policies.extend(parsed_list);
        }
        Ok(parsed_policies)
    }

    /// Parse and validate Cedar templates from loaded template files.
    fn parse_templates(
        template_files: &[PolicyFile],
    ) -> Result<Vec<policy_parser::ParsedTemplate>, PolicyStoreError> {
        let mut parsed_templates = Vec::new();
        for file in template_files {
            let parsed_list =
                policy_parser::PolicyParser::parse_template(&file.content, &file.name)?;
            parsed_templates.extend(parsed_list);
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
