// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Test utilities for policy store testing.
//!
//! This module provides utilities for creating test policy stores programmatically,
//! including:
//! - `PolicyStoreTestBuilder` - Fluent builder for creating policy stores
//! - Test fixtures for valid and invalid policy stores
//! - Archive creation utilities for .cjar testing
//! - Performance testing utilities

#![allow(dead_code)] // Test utilities - not all methods used in every test

use super::errors::PolicyStoreError;
use chrono::Utc;
use sha2::{Digest, Sha256};
use std::collections::HashMap;
use std::io::{Cursor, Write};
use std::path::Path;
use zip::write::{ExtendedFileOptions, FileOptions};
use zip::{CompressionMethod, ZipWriter};

/// Builder for creating test policy stores programmatically.
///
/// # Example
///
/// ```ignore
/// let builder = PolicyStoreTestBuilder::new("test-store")
///     .with_schema(r#"namespace TestApp { entity User; }"#)
///     .with_policy("policy1", r#"@id("policy1") permit(principal, action, resource);"#)
///     .with_entity("users/alice", r#"{"uid":{"type":"User","id":"alice"},"attrs":{},"parents":[]}"#)
///     .with_trusted_issuer("issuer1", r#"{"issuer1":{"name":"Test","oidc_endpoint":"https://example.com"}}"#);
///
/// // Create directory structure
/// builder.build_directory(temp_dir.path())?;
///
/// // Or create archive
/// let archive_bytes = builder.build_archive()?;
/// ```
pub struct PolicyStoreTestBuilder {
    /// Store ID (hex string)
    pub id: String,
    /// Store name
    pub name: String,
    /// Store version
    pub version: String,
    /// Cedar version
    pub cedar_version: String,
    /// Description
    pub description: Option<String>,
    /// Schema content (Cedar schema format)
    pub schema: String,
    /// Policies: filename -> content
    pub policies: HashMap<String, String>,
    /// Templates: filename -> content
    pub templates: HashMap<String, String>,
    /// Entities: filename -> content
    pub entities: HashMap<String, String>,
    /// Trusted issuers: filename -> content
    pub trusted_issuers: HashMap<String, String>,
    /// Whether to generate manifest with checksums
    pub generate_manifest: bool,
    /// Additional files to include
    pub extra_files: HashMap<String, String>,
}

impl Default for PolicyStoreTestBuilder {
    fn default() -> Self {
        Self::new("test123456789")
    }
}

impl PolicyStoreTestBuilder {
    /// Create a new builder with the given store ID.
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            id: id.into(),
            name: "Test Policy Store".to_string(),
            version: "1.0.0".to_string(),
            cedar_version: "4.4.0".to_string(),
            description: None,
            schema: Self::default_schema(),
            policies: HashMap::new(),
            templates: HashMap::new(),
            entities: HashMap::new(),
            trusted_issuers: HashMap::new(),
            generate_manifest: false,
            extra_files: HashMap::new(),
        }
    }

    /// Default minimal Cedar schema for testing.
    pub fn default_schema() -> String {
        r#"namespace TestApp {
    entity User;
    entity Resource;
    entity Role;
    
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
    
    action "write" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
"#
        .to_string()
    }

    /// Set the store name.
    pub fn with_name(mut self, name: impl Into<String>) -> Self {
        self.name = name.into();
        self
    }

    /// Set the store version.
    pub fn with_version(mut self, version: impl Into<String>) -> Self {
        self.version = version.into();
        self
    }

    /// Set the Cedar version.
    pub fn with_cedar_version(mut self, version: impl Into<String>) -> Self {
        self.cedar_version = version.into();
        self
    }

    /// Set the description.
    pub fn with_description(mut self, desc: impl Into<String>) -> Self {
        self.description = Some(desc.into());
        self
    }

    /// Set the schema content.
    pub fn with_schema(mut self, schema: impl Into<String>) -> Self {
        self.schema = schema.into();
        self
    }

    /// Add a policy file.
    ///
    /// # Arguments
    /// * `name` - Filename without .cedar extension (e.g., "policy1" or "auth/admin")
    /// * `content` - Cedar policy content with @id annotation
    pub fn with_policy(mut self, name: impl Into<String>, content: impl Into<String>) -> Self {
        self.policies.insert(name.into(), content.into());
        self
    }

    /// Add a template file.
    pub fn with_template(mut self, name: impl Into<String>, content: impl Into<String>) -> Self {
        self.templates.insert(name.into(), content.into());
        self
    }

    /// Add an entity file.
    ///
    /// # Arguments
    /// * `name` - Filename without .json extension (e.g., "users" or "roles/admin")
    /// * `content` - JSON entity content
    pub fn with_entity(mut self, name: impl Into<String>, content: impl Into<String>) -> Self {
        self.entities.insert(name.into(), content.into());
        self
    }

    /// Add a trusted issuer file.
    pub fn with_trusted_issuer(
        mut self,
        name: impl Into<String>,
        content: impl Into<String>,
    ) -> Self {
        self.trusted_issuers.insert(name.into(), content.into());
        self
    }

    /// Enable manifest generation with checksums.
    pub fn with_manifest(mut self) -> Self {
        self.generate_manifest = true;
        self
    }

    /// Add an extra file (for testing invalid structures).
    pub fn with_extra_file(mut self, path: impl Into<String>, content: impl Into<String>) -> Self {
        self.extra_files.insert(path.into(), content.into());
        self
    }

    /// Generate metadata.json content.
    pub fn build_metadata_json(&self) -> String {
        let mut metadata = serde_json::json!({
            "cedar_version": self.cedar_version,
            "policy_store": {
                "id": self.id,
                "name": self.name,
                "version": self.version
            }
        });

        if let Some(desc) = &self.description {
            metadata["policy_store"]["description"] = serde_json::Value::String(desc.clone());
        }

        serde_json::to_string_pretty(&metadata).unwrap()
    }

    /// Generate manifest.json content with computed checksums.
    fn build_manifest_json(&self, files: &HashMap<String, Vec<u8>>) -> String {
        let mut manifest_files = HashMap::new();

        for (path, content) in files {
            if path != "manifest.json" {
                let mut hasher = Sha256::new();
                hasher.update(content);
                let hash = hex::encode(hasher.finalize());

                manifest_files.insert(
                    path.clone(),
                    serde_json::json!({
                        "size": content.len(),
                        "checksum": format!("sha256:{}", hash)
                    }),
                );
            }
        }

        let manifest = serde_json::json!({
            "policy_store_id": self.id,
            "generated_date": Utc::now().to_rfc3339(),
            "files": manifest_files
        });

        serde_json::to_string_pretty(&manifest).unwrap()
    }

    /// Build all files as a HashMap (path -> content bytes).
    fn build_files(&self) -> HashMap<String, Vec<u8>> {
        let mut files: HashMap<String, Vec<u8>> = HashMap::new();

        // Add metadata.json
        files.insert(
            "metadata.json".to_string(),
            self.build_metadata_json().into_bytes(),
        );

        // Add schema.cedarschema
        files.insert(
            "schema.cedarschema".to_string(),
            self.schema.as_bytes().to_vec(),
        );

        // Add policies
        for (name, content) in &self.policies {
            let path = format!("policies/{}.cedar", name);
            files.insert(path, content.as_bytes().to_vec());
        }

        // Add templates
        for (name, content) in &self.templates {
            let path = format!("templates/{}.cedar", name);
            files.insert(path, content.as_bytes().to_vec());
        }

        // Add entities
        for (name, content) in &self.entities {
            let path = format!("entities/{}.json", name);
            files.insert(path, content.as_bytes().to_vec());
        }

        // Add trusted issuers
        for (name, content) in &self.trusted_issuers {
            let path = format!("trusted-issuers/{}.json", name);
            files.insert(path, content.as_bytes().to_vec());
        }

        // Generate manifest if requested (must be last before extra_files)
        if self.generate_manifest {
            let manifest_content = self.build_manifest_json(&files);
            files.insert("manifest.json".to_string(), manifest_content.into_bytes());
        }

        // Add extra files last, overwriting any generated files with the same path
        for (path, content) in &self.extra_files {
            files.insert(path.clone(), content.as_bytes().to_vec());
        }

        files
    }

    /// Build policy store as directory structure.
    ///
    /// Creates all necessary directories and files at the given path.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn build_directory(&self, base_path: &Path) -> Result<(), PolicyStoreError> {
        use std::fs;

        let files = self.build_files();

        // Create directories first
        let dirs: std::collections::HashSet<_> = files
            .keys()
            .filter_map(|p| {
                let path = Path::new(p);
                path.parent().map(|parent| parent.to_path_buf())
            })
            .filter(|p| !p.as_os_str().is_empty())
            .collect();

        for dir in dirs {
            let full_path = base_path.join(&dir);
            fs::create_dir_all(&full_path).map_err(|e| PolicyStoreError::FileReadError {
                path: full_path.display().to_string(),
                source: e,
            })?;
        }

        // Create required directories even if empty
        let policies_path = base_path.join("policies");
        if !policies_path.exists() {
            fs::create_dir_all(&policies_path).map_err(|e| PolicyStoreError::FileReadError {
                path: policies_path.display().to_string(),
                source: e,
            })?;
        }

        // Write files
        for (path, content) in files {
            let full_path = base_path.join(&path);
            fs::write(&full_path, content).map_err(|e| PolicyStoreError::FileReadError {
                path: full_path.display().to_string(),
                source: e,
            })?;
        }

        Ok(())
    }

    /// Build policy store as .cjar archive bytes.
    ///
    /// Returns the archive as a byte vector suitable for `ArchiveVfs::from_buffer()`.
    pub fn build_archive(&self) -> Result<Vec<u8>, PolicyStoreError> {
        let files = self.build_files();
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        for (path, content) in files {
            let options = FileOptions::<ExtendedFileOptions>::default()
                .compression_method(CompressionMethod::Deflated);
            zip.start_file(&path, options)
                .map_err(|e| PolicyStoreError::Io(std::io::Error::other(e)))?;
            zip.write_all(&content).map_err(PolicyStoreError::Io)?;
        }

        let cursor = zip
            .finish()
            .map_err(|e| PolicyStoreError::Io(std::io::Error::other(e)))?;
        Ok(cursor.into_inner())
    }

    /// Build policy store and write as .cjar file.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn build_archive_file(&self, path: &Path) -> Result<(), PolicyStoreError> {
        let bytes = self.build_archive()?;
        std::fs::write(path, bytes).map_err(|e| PolicyStoreError::FileReadError {
            path: path.display().to_string(),
            source: e,
        })
    }
}

// ============================================================================
// Test Fixtures
// ============================================================================

/// Pre-built test fixtures for common scenarios.
pub mod fixtures {
    use super::*;

    /// Creates a minimal valid policy store.
    pub fn minimal_valid() -> PolicyStoreTestBuilder {
        PolicyStoreTestBuilder::new("abc123def456").with_policy(
            "allow-all",
            r#"@id("allow-all")
permit(principal, action, resource);"#,
        )
    }

    /// Creates a policy store with multiple policies.
    pub fn with_multiple_policies(count: usize) -> PolicyStoreTestBuilder {
        let mut builder = PolicyStoreTestBuilder::new("multipolicy123");

        for i in 0..count {
            builder = builder.with_policy(
                format!("policy{}", i),
                format!(
                    r#"@id("policy{}")
permit(
    principal == TestApp::User::"user{}",
    action == TestApp::Action::"read",
    resource == TestApp::Resource::"res{}"
);"#,
                    i, i, i
                ),
            );
        }

        builder
    }

    /// Creates a policy store with multiple entities.
    pub fn with_multiple_entities(count: usize) -> PolicyStoreTestBuilder {
        let mut builder = PolicyStoreTestBuilder::new("multientity123").with_policy(
            "allow-all",
            r#"@id("allow-all") permit(principal, action, resource);"#,
        );

        // Create users
        let mut users = Vec::new();
        for i in 0..count {
            users.push(serde_json::json!({
                "uid": {"type": "TestApp::User", "id": format!("user{}", i)},
                "attrs": {
                    "name": format!("User {}", i),
                    "email": format!("user{}@example.com", i)
                },
                "parents": []
            }));
        }

        builder = builder.with_entity("users", serde_json::to_string_pretty(&users).unwrap());

        builder
    }

    /// Creates a policy store with entity hierarchy.
    pub fn with_entity_hierarchy() -> PolicyStoreTestBuilder {
        let roles = serde_json::json!([
            {
                "uid": {"type": "TestApp::Role", "id": "admin"},
                "attrs": {"level": 10},
                "parents": []
            },
            {
                "uid": {"type": "TestApp::Role", "id": "user"},
                "attrs": {"level": 1},
                "parents": []
            }
        ]);

        let users = serde_json::json!([
            {
                "uid": {"type": "TestApp::User", "id": "alice"},
                "attrs": {"name": "Alice"},
                "parents": [{"type": "TestApp::Role", "id": "admin"}]
            },
            {
                "uid": {"type": "TestApp::User", "id": "bob"},
                "attrs": {"name": "Bob"},
                "parents": [{"type": "TestApp::Role", "id": "user"}]
            }
        ]);

        PolicyStoreTestBuilder::new("hierarchy123")
            .with_schema(
                r#"namespace TestApp {
    entity User in [Role] {
        name: String,
    };
    entity Role {
        level: Long,
    };
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}"#,
            )
            .with_entity("roles", roles.to_string())
            .with_entity("users", users.to_string())
            .with_policy(
                "admin-access",
                r#"@id("admin-access")
permit(
    principal in TestApp::Role::"admin",
    action,
    resource
);"#,
            )
    }

    /// Creates a policy store with trusted issuers.
    pub fn with_trusted_issuers() -> PolicyStoreTestBuilder {
        let issuer = serde_json::json!({
            "test-issuer": {
                "name": "Test Issuer",
                "oidc_endpoint": "https://test.example.com/.well-known/openid-configuration",
                "token_metadata": {
                    "access_token": {
                        "user_id": "sub",
                        "required_claims": ["sub", "aud"]
                    },
                    "id_token": {
                        "user_id": "sub",
                        "required_claims": ["sub", "email"]
                    }
                }
            }
        });

        minimal_valid().with_trusted_issuer("test-issuer", issuer.to_string())
    }

    /// Creates a policy store with manifest for integrity testing.
    pub fn with_manifest() -> PolicyStoreTestBuilder {
        minimal_valid().with_manifest()
    }

    // ========================================================================
    // Invalid Fixtures
    // ========================================================================

    /// Creates a policy store with invalid metadata JSON.
    pub fn invalid_metadata_json() -> PolicyStoreTestBuilder {
        let mut builder = minimal_valid();
        builder
            .extra_files
            .insert("metadata.json".to_string(), "{ invalid json }".to_string());
        builder
    }

    /// Creates a policy store with missing required field in metadata.
    pub fn invalid_metadata_missing_name() -> PolicyStoreTestBuilder {
        let mut builder = minimal_valid();
        builder.name = "".to_string(); // Empty name is invalid
        builder
    }

    /// Creates a policy store with invalid Cedar schema.
    pub fn invalid_schema() -> PolicyStoreTestBuilder {
        PolicyStoreTestBuilder::new("invalidschema")
            .with_schema("this is not a valid cedar schema { } }")
            .with_policy(
                "allow",
                r#"@id("allow") permit(principal, action, resource);"#,
            )
    }

    /// Creates a policy store with invalid policy syntax.
    pub fn invalid_policy_syntax() -> PolicyStoreTestBuilder {
        PolicyStoreTestBuilder::new("invalidpolicy")
            .with_policy("bad-policy", "permit ( principal action resource );")
    }

    /// Creates a policy store with policy missing @id annotation.
    pub fn policy_missing_id() -> PolicyStoreTestBuilder {
        PolicyStoreTestBuilder::new("missingid123")
            .with_policy("no-id", "permit(principal, action, resource);")
    }

    /// Creates a policy store with duplicate entity UIDs.
    pub fn duplicate_entity_uids() -> PolicyStoreTestBuilder {
        let users1 = serde_json::json!([{
            "uid": {"type": "TestApp::User", "id": "alice"},
            "attrs": {},
            "parents": []
        }]);

        let users2 = serde_json::json!([{
            "uid": {"type": "TestApp::User", "id": "alice"},
            "attrs": {},
            "parents": []
        }]);

        minimal_valid()
            .with_entity("users1", users1.to_string())
            .with_entity("users2", users2.to_string())
    }

    /// Creates a policy store with invalid trusted issuer config.
    pub fn invalid_trusted_issuer() -> PolicyStoreTestBuilder {
        let issuer = serde_json::json!({
            "bad-issuer": {
                "name": "Missing OIDC endpoint"
                // Missing required oidc_endpoint field
            }
        });

        minimal_valid().with_trusted_issuer("bad-issuer", issuer.to_string())
    }
}

// ============================================================================
// Archive Test Utilities
// ============================================================================

/// Creates a test archive with path traversal attempt.
pub fn create_path_traversal_archive() -> Vec<u8> {
    let buffer = Vec::new();
    let cursor = Cursor::new(buffer);
    let mut zip = ZipWriter::new(cursor);

    let options = FileOptions::<ExtendedFileOptions>::default()
        .compression_method(CompressionMethod::Deflated);
    zip.start_file("../../../etc/passwd", options).unwrap();
    zip.write_all(b"malicious content").unwrap();

    zip.finish().unwrap().into_inner()
}

/// Creates a corrupted archive (invalid ZIP structure).
pub fn create_corrupted_archive() -> Vec<u8> {
    // Start with valid ZIP header but corrupt it
    let mut bytes = vec![0x50, 0x4B, 0x03, 0x04]; // ZIP local file header
    bytes.extend_from_slice(&[0xFF; 100]); // Corrupted data
    bytes
}

/// Creates a deeply nested archive for path length testing.
pub fn create_deep_nested_archive(depth: usize) -> Vec<u8> {
    let buffer = Vec::new();
    let cursor = Cursor::new(buffer);
    let mut zip = ZipWriter::new(cursor);

    let path = (0..depth).map(|_| "dir").collect::<Vec<_>>().join("/") + "/file.txt";

    let options = FileOptions::<ExtendedFileOptions>::default()
        .compression_method(CompressionMethod::Deflated);
    zip.start_file(&path, options).unwrap();
    zip.write_all(b"deep content").unwrap();

    zip.finish().unwrap().into_inner()
}

// ============================================================================
// Performance Testing Utilities
// ============================================================================

/// Creates a large policy store for load testing.
///
/// # Arguments
/// * `policy_count` - Number of policies to generate
/// * `entity_count` - Number of entities to generate
/// * `issuer_count` - Number of trusted issuers to generate
pub fn create_large_policy_store(
    policy_count: usize,
    entity_count: usize,
    issuer_count: usize,
) -> PolicyStoreTestBuilder {
    let mut builder = PolicyStoreTestBuilder::new("loadtest123456");

    // Generate policies
    for i in 0..policy_count {
        builder = builder.with_policy(
            format!("policy{:06}", i),
            format!(
                r#"@id("policy{:06}")
permit(
    principal == TestApp::User::"user{:06}",
    action == TestApp::Action::"read",
    resource == TestApp::Resource::"resource{:06}"
) when {{
    principal has email && principal.email like "*@example.com"
}};"#,
                i,
                i % entity_count,
                i % 100
            ),
        );
    }

    // Generate entities in batches
    let batch_size = 1000;
    let entity_batches = (entity_count + batch_size - 1) / batch_size;

    for batch in 0..entity_batches {
        let start = batch * batch_size;
        let end = ((batch + 1) * batch_size).min(entity_count);

        let entities: Vec<_> = (start..end)
            .map(|i| {
                serde_json::json!({
                    "uid": {"type": "TestApp::User", "id": format!("user{:06}", i)},
                    "attrs": {
                        "name": format!("User {}", i),
                        "email": format!("user{}@example.com", i),
                        "department": format!("dept{}", i % 10)
                    },
                    "parents": []
                })
            })
            .collect();

        builder = builder.with_entity(
            format!("users_batch{:04}", batch),
            serde_json::to_string(&entities).unwrap(),
        );
    }

    // Generate trusted issuers
    for i in 0..issuer_count {
        let issuer = serde_json::json!({
            format!("issuer{}", i): {
                "name": format!("Issuer {}", i),
                "oidc_endpoint": format!("https://issuer{}.example.com/.well-known/openid-configuration", i),
                "token_metadata": {
                    "access_token": {
                        "user_id": "sub",
                        "required_claims": ["sub"]
                    }
                }
            }
        });
        builder = builder.with_trusted_issuer(format!("issuer{}", i), issuer.to_string());
    }

    builder
}

/// Memory statistics from an operation.
#[derive(Debug, Clone, Copy)]
pub struct MemoryStats {
    /// Total bytes allocated
    pub allocated: usize,
    /// Total bytes deallocated
    pub deallocated: usize,
    /// Net memory change (allocated - deallocated)
    pub net: isize,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_builder_creates_valid_metadata() {
        let builder = PolicyStoreTestBuilder::new("test123abc456")
            .with_name("My Test Store")
            .with_version("2.0.0")
            .with_description("A test store");

        let metadata_json = builder.build_metadata_json();
        let metadata: serde_json::Value = serde_json::from_str(&metadata_json).unwrap();

        assert_eq!(metadata["cedar_version"], "4.4.0");
        assert_eq!(metadata["policy_store"]["id"], "test123abc456");
        assert_eq!(metadata["policy_store"]["name"], "My Test Store");
        assert_eq!(metadata["policy_store"]["version"], "2.0.0");
        assert_eq!(metadata["policy_store"]["description"], "A test store");
    }

    #[test]
    fn test_builder_creates_archive() {
        let builder = fixtures::minimal_valid();
        let archive = builder.build_archive().unwrap();

        // Verify it's a valid ZIP
        assert!(archive.len() > 0);
        assert_eq!(&archive[0..2], &[0x50, 0x4B]); // ZIP magic number
    }

    #[test]
    fn test_fixture_with_multiple_policies() {
        let builder = fixtures::with_multiple_policies(10);
        assert_eq!(builder.policies.len(), 10);
    }

    #[test]
    fn test_fixture_with_multiple_entities() {
        let builder = fixtures::with_multiple_entities(100);
        assert_eq!(builder.entities.len(), 1); // All in one file
    }

    #[test]
    fn test_large_policy_store_creation() {
        let builder = create_large_policy_store(100, 1000, 5);
        assert_eq!(builder.policies.len(), 100);
        assert_eq!(builder.trusted_issuers.len(), 5);
    }

    #[test]
    fn test_path_traversal_archive() {
        let archive = create_path_traversal_archive();
        assert!(archive.len() > 0);
    }

    #[test]
    fn test_corrupted_archive() {
        let archive = create_corrupted_archive();
        assert!(archive.len() > 0);
    }

    #[test]
    fn test_deep_nested_archive() {
        let archive = create_deep_nested_archive(50);
        assert!(archive.len() > 0);
    }
}
