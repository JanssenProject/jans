// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};
use super::source::{PolicyStoreFormat, PolicyStoreSource};
use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;
use std::path::Path;

/// Policy store loader trait for loading policy stores from various sources.
pub trait PolicyStoreLoader {
    /// Load a policy store from the given source.
    fn load(&self, source: &PolicyStoreSource) -> Result<LoadedPolicyStore, PolicyStoreError>;

    /// Detect the format of a policy store source.
    fn detect_format(&self, source: &PolicyStoreSource) -> PolicyStoreFormat;

    /// Validate the structure of a policy store source.
    fn validate_structure(&self, source: &PolicyStoreSource) -> Result<(), PolicyStoreError>;
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
/// - Archive filesystem for .cjar files (future)
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
}

impl<V: VfsFileSystem> DefaultPolicyStoreLoader<V> {
    /// Detect format based on source type and path characteristics.
    fn detect_format_internal(source: &PolicyStoreSource) -> PolicyStoreFormat {
        match source {
            PolicyStoreSource::Directory(_) => PolicyStoreFormat::Directory,
            PolicyStoreSource::Archive(path) => {
                // Check if file has .cjar extension
                if path.extension().and_then(|s| s.to_str()) == Some("cjar") {
                    PolicyStoreFormat::Archive
                } else {
                    // Assume archive format for any zip-like file
                    PolicyStoreFormat::Archive
                }
            },
            PolicyStoreSource::Legacy(_) => PolicyStoreFormat::Legacy,
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
        let metadata_path = format!("{}/metadata.json", dir);
        if !self.vfs.exists(&metadata_path) {
            return Err(ValidationError::MissingRequiredFile {
                file: "metadata.json".to_string(),
            }
            .into());
        }

        let schema_path = format!("{}/schema.cedarschema", dir);
        if !self.vfs.exists(&schema_path) {
            return Err(ValidationError::MissingRequiredFile {
                file: "schema.cedarschema".to_string(),
            }
            .into());
        }

        // Check for required directories
        let policies_dir = format!("{}/policies", dir);
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
        let metadata_path = format!("{}/metadata.json", dir);
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
        let manifest_path = format!("{}/manifest.json", dir);
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
        let schema_path = format!("{}/schema.cedarschema", dir);
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
        let policies_dir = format!("{}/policies", dir);
        self.load_cedar_files(&policies_dir, "policy")
    }

    /// Load all template files from templates directory (if exists).
    fn load_templates(&self, dir: &str) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let templates_dir = format!("{}/templates", dir);
        if !self.vfs.exists(&templates_dir) {
            return Ok(Vec::new());
        }

        self.load_cedar_files(&templates_dir, "template")
    }

    /// Load all entity files from entities directory (if exists).
    fn load_entities(&self, dir: &str) -> Result<Vec<EntityFile>, PolicyStoreError> {
        let entities_dir = format!("{}/entities", dir);
        if !self.vfs.exists(&entities_dir) {
            return Ok(Vec::new());
        }

        self.load_json_files(&entities_dir, "entity")
    }

    /// Load all trusted issuer files from trusted-issuers directory (if exists).
    fn load_trusted_issuers(&self, dir: &str) -> Result<Vec<IssuerFile>, PolicyStoreError> {
        let issuers_dir = format!("{}/trusted-issuers", dir);
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

    /// Helper: Load all .cedar files from a directory.
    fn load_cedar_files(
        &self,
        dir: &str,
        _file_type: &str,
    ) -> Result<Vec<PolicyFile>, PolicyStoreError> {
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

        Ok(files)
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
    fn load_directory(&self, dir: &str) -> Result<LoadedPolicyStore, PolicyStoreError> {
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

#[cfg(not(target_arch = "wasm32"))]
impl Default for DefaultPolicyStoreLoader<super::vfs_adapter::PhysicalVfs> {
    fn default() -> Self {
        Self::new_physical()
    }
}

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
            PolicyStoreSource::Archive(_) => {
                // TODO: Archive loading will be implemented
                todo!("Archive (.cjar) loading will use VFS + zip crate")
            },
            PolicyStoreSource::Legacy(_) => {
                // TODO: Legacy format integration will be handled
                todo!("Legacy format integration not yet implemented ")
            },
        }
    }

    fn detect_format(&self, source: &PolicyStoreSource) -> PolicyStoreFormat {
        Self::detect_format_internal(source)
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
            PolicyStoreSource::Archive(_) => {
                // TODO: Archive validation will be implemented
                todo!("Archive structure validation not yet implemented")
            },
            PolicyStoreSource::Legacy(_) => {
                // TODO: Legacy format validation will be handled
                todo!("Legacy format validation not yet implemented")
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::path::PathBuf;
    use tempfile::TempDir;

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
        let source = PolicyStoreSource::Archive(PathBuf::from("/path/to/store.cjar"));
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
}
