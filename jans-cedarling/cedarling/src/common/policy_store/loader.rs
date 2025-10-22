// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};
use super::policy_parser::{ParsedPolicy, ParsedTemplate, PolicyParser};
use super::schema_parser::{ParsedSchema, SchemaParser};
use super::source::{PolicyStoreFormat, PolicyStoreSource};
use super::validator::MetadataValidator;
use super::vfs_adapter::VfsFileSystem;
use cedar_policy::{PolicySet, Schema};
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

    /// Parse and validate a Cedar schema from content.
    ///
    /// Uses the Cedar engine to parse and validate the schema syntax and structure.
    fn parse_schema(content: &str, filename: &str) -> Result<ParsedSchema, PolicyStoreError> {
        let parsed = SchemaParser::parse_schema(content, filename)?;
        SchemaParser::validate_schema(&parsed)?;
        Ok(parsed)
    }

    /// Extract the Cedar Schema object from a parsed schema.
    ///
    /// Returns the validated Cedar Schema that can be used for policy validation.
    fn get_schema(parsed: &ParsedSchema) -> Result<&Schema, PolicyStoreError> {
        Ok(&parsed.schema)
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
        let parsed_schema = PhysicalLoader::parse_schema(&loaded.schema, "schema.cedarschema");
        assert!(parsed_schema.is_ok());

        let parsed = parsed_schema.unwrap();
        assert_eq!(parsed.filename, "schema.cedarschema");
        assert_eq!(parsed.content, schema_content);

        // Validate the schema
        let validation = SchemaParser::validate_schema(&parsed);
        assert!(validation.is_ok());

        // Get the Cedar schema object
        let schema = PhysicalLoader::get_schema(&parsed);
        assert!(schema.is_ok());
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
        let parsed_schema = PhysicalLoader::parse_schema(&loaded.schema, "schema.cedarschema");
        assert!(parsed_schema.is_ok());

        // Parse policies
        let parsed_policies = PhysicalLoader::parse_policies(&loaded.policies);
        assert!(parsed_policies.is_ok());

        // Verify they work together
        let parsed_schema_result = parsed_schema.unwrap();
        let schema = PhysicalLoader::get_schema(&parsed_schema_result);
        assert!(schema.is_ok());

        let policy_set = PhysicalLoader::create_policy_set(parsed_policies.unwrap(), vec![]);
        assert!(policy_set.is_ok());
    }
}
