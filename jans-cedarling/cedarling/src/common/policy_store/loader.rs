// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.

use super::errors::{PolicyStoreError, ValidationError};
use super::metadata::{PolicyStoreManifest, PolicyStoreMetadata};
use super::source::{PolicyStoreFormat, PolicyStoreSource};
use super::validator::MetadataValidator;
use std::fs;
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
pub struct DefaultPolicyStoreLoader;

impl DefaultPolicyStoreLoader {
    /// Create a new default policy store loader.
    pub fn new() -> Self {
        Self
    }

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
    fn validate_directory_structure(dir: &Path) -> Result<(), PolicyStoreError> {
        // Check if directory exists
        if !dir.exists() {
            return Err(PolicyStoreError::Io(std::io::Error::new(
                std::io::ErrorKind::NotFound,
                format!("Directory not found: {}", dir.display()),
            )));
        }

        if !dir.is_dir() {
            return Err(PolicyStoreError::Io(std::io::Error::new(
                std::io::ErrorKind::InvalidInput,
                format!("Path is not a directory: {}", dir.display()),
            )));
        }

        // Check for required files
        let metadata_path = dir.join("metadata.json");
        if !metadata_path.exists() {
            return Err(ValidationError::MissingRequiredFile {
                file: "metadata.json".to_string(),
            }
            .into());
        }

        let schema_path = dir.join("schema.cedarschema");
        if !schema_path.exists() {
            return Err(ValidationError::MissingRequiredFile {
                file: "schema.cedarschema".to_string(),
            }
            .into());
        }

        // Check for required directories
        let policies_dir = dir.join("policies");
        if !policies_dir.exists() {
            return Err(ValidationError::MissingRequiredDirectory {
                directory: "policies".to_string(),
            }
            .into());
        }

        if !policies_dir.is_dir() {
            return Err(PolicyStoreError::Io(std::io::Error::new(
                std::io::ErrorKind::InvalidInput,
                "policies path exists but is not a directory",
            )));
        }

        Ok(())
    }

    /// Load metadata from metadata.json file.
    fn load_metadata(dir: &Path) -> Result<PolicyStoreMetadata, PolicyStoreError> {
        let metadata_path = dir.join("metadata.json");
        let content = fs::read_to_string(&metadata_path).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!("Failed to read metadata.json: {}", e),
            ))
        })?;

        // Parse and validate metadata
        MetadataValidator::parse_and_validate(&content).map_err(PolicyStoreError::Validation)
    }

    /// Load optional manifest from manifest.json file.
    fn load_manifest(dir: &Path) -> Result<Option<PolicyStoreManifest>, PolicyStoreError> {
        let manifest_path = dir.join("manifest.json");
        if !manifest_path.exists() {
            return Ok(None);
        }

        // Open file and parse JSON directly from reader
        let file = fs::File::open(&manifest_path).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!("Failed to open manifest.json: {}", e),
            ))
        })?;

        let manifest =
            serde_json::from_reader(file).map_err(|e| PolicyStoreError::JsonParsing {
                file: "manifest.json".to_string(),
                message: e.to_string(),
            })?;

        Ok(Some(manifest))
    }

    /// Load schema from schema.cedarschema file.
    fn load_schema(dir: &Path) -> Result<String, PolicyStoreError> {
        let schema_path = dir.join("schema.cedarschema");
        fs::read_to_string(&schema_path).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!("Failed to read schema.cedarschema: {}", e),
            ))
        })
    }

    /// Load all policy files from policies directory.
    fn load_policies(dir: &Path) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let policies_dir = dir.join("policies");
        Self::load_cedar_files(&policies_dir, "policy")
    }

    /// Load all template files from templates directory (if exists).
    fn load_templates(dir: &Path) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let templates_dir = dir.join("templates");
        if !templates_dir.exists() {
            return Ok(Vec::new());
        }

        Self::load_cedar_files(&templates_dir, "template")
    }

    /// Load all entity files from entities directory (if exists).
    fn load_entities(dir: &Path) -> Result<Vec<EntityFile>, PolicyStoreError> {
        let entities_dir = dir.join("entities");
        if !entities_dir.exists() {
            return Ok(Vec::new());
        }

        Self::load_json_files(&entities_dir, "entity")
    }

    /// Load all trusted issuer files from trusted-issuers directory (if exists).
    fn load_trusted_issuers(dir: &Path) -> Result<Vec<IssuerFile>, PolicyStoreError> {
        let issuers_dir = dir.join("trusted-issuers");
        if !issuers_dir.exists() {
            return Ok(Vec::new());
        }

        let entries = fs::read_dir(&issuers_dir).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!(
                    "Failed to read trusted-issuers directory at '{}': {}",
                    issuers_dir.display(),
                    e
                ),
            ))
        })?;

        let mut issuers = Vec::new();
        for entry in entries {
            let entry = entry.map_err(|e| {
                PolicyStoreError::Io(std::io::Error::new(
                    e.kind(),
                    "Failed to read directory entry",
                ))
            })?;

            let path = entry.path();
            if path.is_file() {
                // Validate .json extension
                if path.extension().and_then(|s| s.to_str()) != Some("json") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: path.display().to_string(),
                        expected: ".json".to_string(),
                        actual: path
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let content = fs::read_to_string(&path).map_err(|e| {
                    PolicyStoreError::Io(std::io::Error::new(
                        e.kind(),
                        format!("Failed to read issuer file: {}", path.display()),
                    ))
                })?;

                issuers.push(IssuerFile {
                    name: path
                        .file_name()
                        .and_then(|s| s.to_str())
                        .unwrap_or("unknown")
                        .to_string(),
                    content,
                });
            }
        }

        Ok(issuers)
    }

    /// Helper: Load all .cedar files from a directory.
    fn load_cedar_files(dir: &Path, file_type: &str) -> Result<Vec<PolicyFile>, PolicyStoreError> {
        let entries = fs::read_dir(dir).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!(
                    "Failed to read {} directory at '{}': {}",
                    file_type,
                    dir.display(),
                    e
                ),
            ))
        })?;

        let mut files = Vec::new();
        for entry in entries {
            let entry = entry.map_err(|e| {
                PolicyStoreError::Io(std::io::Error::new(
                    e.kind(),
                    "Failed to read directory entry",
                ))
            })?;

            let path = entry.path();
            if path.is_file() {
                // Validate .cedar extension
                if path.extension().and_then(|s| s.to_str()) != Some("cedar") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: path.display().to_string(),
                        expected: ".cedar".to_string(),
                        actual: path
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let content = fs::read_to_string(&path).map_err(|e| {
                    PolicyStoreError::Io(std::io::Error::new(
                        e.kind(),
                        format!("Failed to read {} file: {}", file_type, path.display()),
                    ))
                })?;

                files.push(PolicyFile {
                    name: path
                        .file_name()
                        .and_then(|s| s.to_str())
                        .unwrap_or("unknown")
                        .to_string(),
                    content,
                });
            }
        }

        Ok(files)
    }

    /// Helper: Load all .json files from a directory.
    fn load_json_files(dir: &Path, file_type: &str) -> Result<Vec<EntityFile>, PolicyStoreError> {
        let entries = fs::read_dir(dir).map_err(|e| {
            PolicyStoreError::Io(std::io::Error::new(
                e.kind(),
                format!(
                    "Failed to read {} directory at '{}': {}",
                    file_type,
                    dir.display(),
                    e
                ),
            ))
        })?;

        let mut files = Vec::new();
        for entry in entries {
            let entry = entry.map_err(|e| {
                PolicyStoreError::Io(std::io::Error::new(
                    e.kind(),
                    "Failed to read directory entry",
                ))
            })?;

            let path = entry.path();
            if path.is_file() {
                // Validate .json extension
                if path.extension().and_then(|s| s.to_str()) != Some("json") {
                    return Err(ValidationError::InvalidFileExtension {
                        file: path.display().to_string(),
                        expected: ".json".to_string(),
                        actual: path
                            .extension()
                            .and_then(|s| s.to_str())
                            .unwrap_or("(none)")
                            .to_string(),
                    }
                    .into());
                }

                let content = fs::read_to_string(&path).map_err(|e| {
                    PolicyStoreError::Io(std::io::Error::new(
                        e.kind(),
                        format!("Failed to read {} file: {}", file_type, path.display()),
                    ))
                })?;

                files.push(EntityFile {
                    name: path
                        .file_name()
                        .and_then(|s| s.to_str())
                        .unwrap_or("unknown")
                        .to_string(),
                    content,
                });
            }
        }

        Ok(files)
    }

    /// Load a directory-based policy store.
    fn load_directory(dir: &Path) -> Result<LoadedPolicyStore, PolicyStoreError> {
        // Validate structure first
        Self::validate_directory_structure(dir)?;

        // Load all components
        let metadata = Self::load_metadata(dir)?;
        let manifest = Self::load_manifest(dir)?;
        let schema = Self::load_schema(dir)?;
        let policies = Self::load_policies(dir)?;
        let templates = Self::load_templates(dir)?;
        let entities = Self::load_entities(dir)?;
        let trusted_issuers = Self::load_trusted_issuers(dir)?;

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

impl Default for DefaultPolicyStoreLoader {
    fn default() -> Self {
        Self::new()
    }
}

impl PolicyStoreLoader for DefaultPolicyStoreLoader {
    fn load(&self, source: &PolicyStoreSource) -> Result<LoadedPolicyStore, PolicyStoreError> {
        match source {
            PolicyStoreSource::Directory(path) => Self::load_directory(path),
            PolicyStoreSource::Archive(_) => {
                // TODO: Archive loading will be implemented
                todo!("Archive (.cjar) loading not yet implemented ")
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
            PolicyStoreSource::Directory(path) => Self::validate_directory_structure(path),
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
        let loader = DefaultPolicyStoreLoader::new();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Directory);
    }

    #[test]
    fn test_format_detection_archive() {
        let source = PolicyStoreSource::Archive(PathBuf::from("/path/to/store.cjar"));
        let loader = DefaultPolicyStoreLoader::new();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Archive);
    }

    #[test]
    fn test_format_detection_legacy() {
        let source = PolicyStoreSource::Legacy("{}".to_string());
        let loader = DefaultPolicyStoreLoader::new();
        assert_eq!(loader.detect_format(&source), PolicyStoreFormat::Legacy);
    }

    #[test]
    fn test_validate_nonexistent_directory() {
        let source = PolicyStoreSource::Directory(PathBuf::from("/nonexistent/path"));
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
        let loader = DefaultPolicyStoreLoader::new();
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
