// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};
use super::policy_parser::{ParsedPolicy, ParsedTemplate, PolicyParser};
use super::source::{PolicyStoreFormat, PolicyStoreSource};
use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;
use cedar_policy::PolicySet;
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

    /// Parse and validate Cedar policies from loaded policy files.
    ///
    /// Extracts policy IDs from @id annotations or filenames and validates syntax.
    fn parse_policies(policy_files: &[PolicyFile]) -> Result<Vec<ParsedPolicy>, PolicyStoreError> {
        let mut parsed_policies = Vec::with_capacity(policy_files.len());

        for file in policy_files {
            let parsed = PolicyParser::parse_policy(&file.content, &file.name)?;
            parsed_policies.push(parsed);
        }

        Ok(parsed_policies)
    }

    /// Parse and validate Cedar templates from loaded template files.
    ///
    /// Extracts template IDs from @id annotations or filenames and validates
    /// syntax including slot definitions.
    fn parse_templates(
        template_files: &[PolicyFile],
    ) -> Result<Vec<ParsedTemplate>, PolicyStoreError> {
        let mut parsed_templates = Vec::with_capacity(template_files.len());

        for file in template_files {
            let parsed = PolicyParser::parse_template(&file.content, &file.name)?;
            parsed_templates.push(parsed);
        }

        Ok(parsed_templates)
    }

    /// Create a Cedar PolicySet from parsed policies and templates.
    ///
    /// Validates no ID conflicts and that all policies/templates can be added.
    fn create_policy_set(
        policies: Vec<ParsedPolicy>,
        templates: Vec<ParsedTemplate>,
    ) -> Result<PolicySet, PolicyStoreError> {
        PolicyParser::create_policy_set(policies, templates)
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
    use super::super::schema_parser::SchemaParser;
    use super::*;
    use std::fs;
    use std::path::PathBuf;
    use tempfile::TempDir;

    type PhysicalLoader = DefaultPolicyStoreLoader<super::super::vfs_adapter::PhysicalVfs>;

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
        use super::super::entity_parser::EntityParser;
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
        use super::super::entity_parser::EntityParser;
        use super::super::schema_parser::SchemaParser;

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
        use super::super::entity_parser::EntityParser;
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
        use super::super::issuer_parser::IssuerParser;
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
        use super::super::issuer_parser::IssuerParser;
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
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create trusted-issuers directory with duplicate IDs
        let issuers_dir = dir.join("trusted-issuers");
        fs::create_dir(&issuers_dir).unwrap();

        fs::write(
            issuers_dir.join("file1.json"),
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
            }"#,
        )
        .unwrap();

        fs::write(
            issuers_dir.join("file2.json"),
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
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse issuers
        use super::super::issuer_parser::IssuerParser;
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
        let temp_dir = TempDir::new().unwrap();
        let dir = temp_dir.path();

        // Create a complete policy store structure
        let _ = create_test_policy_store(dir);

        // Create trusted-issuers directory with invalid issuer
        let issuers_dir = dir.join("trusted-issuers");
        fs::create_dir(&issuers_dir).unwrap();

        fs::write(
            issuers_dir.join("invalid.json"),
            r#"{
                "bad_issuer": {
                    "description": "Missing name field",
                    "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration"
                }
            }"#,
        )
        .unwrap();

        // Load the policy store
        let source = PolicyStoreSource::Directory(dir.to_path_buf());
        let loader = DefaultPolicyStoreLoader::new_physical();
        let loaded = loader.load(&source).unwrap();

        // Parse issuers - should fail
        use super::super::issuer_parser::IssuerParser;
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
        use super::super::entity_parser::EntityParser;
        use super::super::issuer_parser::IssuerParser;
        use super::super::schema_parser::SchemaParser;

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
            let parsed_entities = EntityParser::parse_entities(
                &entity_file.content,
                &entity_file.name,
                parsed_schema,
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
}
