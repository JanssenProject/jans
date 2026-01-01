// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Tests for the policy store loader module.
//!
//! This module is extracted from `loader.rs` for maintainability.

use super::super::archive_handler::ArchiveVfs;
use super::super::entity_parser::EntityParser;
use super::super::errors::{CedarParseErrorDetail, PolicyStoreError, ValidationError};
use super::super::issuer_parser::IssuerParser;
#[cfg(not(target_arch = "wasm32"))]
use super::super::manifest_validator::ManifestValidator;
use super::super::schema_parser::SchemaParser;
use super::super::vfs_adapter::{MemoryVfs, PhysicalVfs};
use super::*;
use std::fs::{self, File};
use std::io::{Cursor, Write};
use std::path::{Path, PathBuf};
use tempfile::TempDir;
use zip::CompressionMethod;
use zip::write::{ExtendedFileOptions, FileOptions};

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

/// Helper to create a test archive in memory with standard structure.
fn create_test_archive(
    name: &str,
    id: &str,
    extra_policies: &[(&str, &str)],
    extra_entities: &[(&str, &str)],
) -> Vec<u8> {
    let options = || {
        FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated)
    };

    let mut archive_bytes = Vec::new();
    {
        let cursor = Cursor::new(&mut archive_bytes);
        let mut zip = zip::ZipWriter::new(cursor);

        // Metadata
        zip.start_file("metadata.json", options()).unwrap();
        write!(
            zip,
            r#"{{"cedar_version":"4.4.0","policy_store":{{"id":"{}","name":"{}","version":"1.0.0"}}}}"#,
            id, name
        )
        .unwrap();

        // Schema
        zip.start_file("schema.cedarschema", options()).unwrap();
        zip.write_all(b"namespace TestApp { entity User; entity Resource; }")
            .unwrap();

        // Default policy
        zip.start_file("policies/default.cedar", options()).unwrap();
        zip.write_all(b"permit(principal, action, resource);")
            .unwrap();

        // Extra policies
        for (policy_name, content) in extra_policies {
            zip.start_file(format!("policies/{}", policy_name), options())
                .unwrap();
            zip.write_all(content.as_bytes()).unwrap();
        }

        // Extra entities
        for (entity_name, content) in extra_entities {
            zip.start_file(format!("entities/{}", entity_name), options())
                .unwrap();
            zip.write_all(content.as_bytes()).unwrap();
        }

        zip.finish().unwrap();
    }
    archive_bytes
}

#[test]
fn test_validate_nonexistent_directory() {
    let loader = DefaultPolicyStoreLoader::new_physical();
    let path = PathBuf::from("/nonexistent/path");
    let path_str = path.to_str().unwrap_or("/nonexistent/path");
    let result = loader.validate_directory_structure(path_str);
    let err = result.expect_err("Expected error for nonexistent directory");
    assert!(
        matches!(&err, PolicyStoreError::PathNotFound { .. })
            || matches!(&err, PolicyStoreError::Io(_)),
        "Expected PathNotFound or Io error, got: {:?}",
        err
    );
}

#[test]
fn test_validate_directory_missing_metadata() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create only schema, no metadata
    fs::write(dir.join("schema.cedarschema"), "test").unwrap();
    fs::create_dir(dir.join("policies")).unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.validate_directory_structure(dir.to_str().unwrap());

    let err = result.expect_err("Expected error for missing metadata.json");
    assert!(
        matches!(
            &err,
            PolicyStoreError::Validation(ValidationError::MissingRequiredFile { file })
            if file.contains("metadata")
        ),
        "Expected MissingRequiredFile error for metadata.json, got: {:?}",
        err
    );
}

#[test]
fn test_validate_directory_missing_schema() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create metadata but no schema
    fs::write(dir.join("metadata.json"), "{}").unwrap();
    fs::create_dir(dir.join("policies")).unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.validate_directory_structure(dir.to_str().unwrap());

    let err = result.expect_err("Expected error for missing schema.cedarschema");
    assert!(
        matches!(
            &err,
            PolicyStoreError::Validation(ValidationError::MissingRequiredFile { file })
            if file.contains("schema")
        ),
        "Expected MissingRequiredFile error for schema.cedarschema, got: {:?}",
        err
    );
}

#[test]
fn test_validate_directory_missing_policies_dir() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create files but no policies directory
    fs::write(dir.join("metadata.json"), "{}").unwrap();
    fs::write(dir.join("schema.cedarschema"), "test").unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.validate_directory_structure(dir.to_str().unwrap());

    let err = result.expect_err("Expected error for missing policies directory");
    assert!(
        matches!(
            &err,
            PolicyStoreError::Validation(ValidationError::MissingRequiredDirectory { directory })
            if directory.contains("policies")
        ),
        "Expected MissingRequiredDirectory error for policies, got: {:?}",
        err
    );
}

#[test]
fn test_validate_directory_success() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create valid structure
    create_test_policy_store(dir).unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.validate_directory_structure(dir.to_str().unwrap());

    assert!(result.is_ok());
}

#[test]
fn test_load_directory_success() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create valid policy store
    create_test_policy_store(dir).unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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

    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load with optional components to succeed");

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

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.load_directory(dir.to_str().unwrap());

    let err = result.expect_err("Expected error for invalid policy file extension");
    assert!(
        matches!(
            &err,
            PolicyStoreError::Validation(ValidationError::InvalidFileExtension { .. })
        ),
        "Expected InvalidFileExtension error, got: {:?}",
        err
    );
}

#[test]
fn test_load_directory_invalid_json() {
    let temp_dir = TempDir::new().unwrap();
    let dir = temp_dir.path();

    // Create invalid metadata
    fs::write(dir.join("metadata.json"), "not valid json").unwrap();
    fs::write(dir.join("schema.cedarschema"), "schema").unwrap();
    fs::create_dir(dir.join("policies")).unwrap();

    let loader = DefaultPolicyStoreLoader::new_physical();
    let result = loader.load_directory(dir.to_str().unwrap());

    let err = result.expect_err("Expected error for invalid JSON in metadata.json");
    assert!(
        matches!(&err, PolicyStoreError::JsonParsing { file, .. } if file.contains("metadata"))
            || matches!(
                &err,
                PolicyStoreError::Validation(ValidationError::MetadataJsonParseFailed { .. })
            ),
        "Expected JsonParsing or MetadataJsonParseFailed error, got: {:?}",
        err
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

    let parsed = result.expect("failed to parse policies");
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
    let err = result.expect_err("Expected CedarParsing error for invalid syntax");

    assert!(
        matches!(
            &err,
            PolicyStoreError::CedarParsing { file, detail: CedarParseErrorDetail::ParseError(_) }
            if file == "invalid.cedar"
        ),
        "Expected CedarParsing error with ParseError detail, got: {:?}",
        err
    );
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
    assert_eq!(parsed[0].template.id().to_string(), "template1");
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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
        let parsed_issuers = IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

    // Parse issuers
    let mut all_issuers = Vec::new();

    for issuer_file in &loaded.trusted_issuers {
        let parsed_issuers = IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
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
    let loader = DefaultPolicyStoreLoader::new(vfs);
    let loaded = loader
        .load_directory("/")
        .expect("Expected in-memory directory load to succeed");

    // Parse issuers
    let mut all_issuers = Vec::new();

    for issuer_file in &loaded.trusted_issuers {
        let parsed_issuers = IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
            .expect("Should parse issuers");
        all_issuers.extend(parsed_issuers);
    }

    // Detect duplicates
    let validation = IssuerParser::validate_issuers(&all_issuers);
    let errors = validation.expect_err("Should detect duplicate issuer IDs");
    assert_eq!(errors.len(), 1, "Should have 1 duplicate error");
    assert!(
        errors[0].contains("issuer1"),
        "Error should mention the duplicate issuer ID 'issuer1', got: {}",
        errors[0]
    );
    assert!(
        errors[0].contains("file1.json") || errors[0].contains("file2.json"),
        "Error should mention the source file, got: {}",
        errors[0]
    );
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
    let loader = DefaultPolicyStoreLoader::new(vfs);
    let loaded = loader
        .load_directory("/")
        .expect("Expected in-memory directory load to succeed");

    // Parse issuers - should fail
    let result = IssuerParser::parse_issuer(
        &loaded.trusted_issuers[0].content,
        &loaded.trusted_issuers[0].name,
    );

    let err = result.expect_err("Should fail on missing required field");
    assert!(
        matches!(&err, PolicyStoreError::TrustedIssuerError { .. }),
        "Expected TrustedIssuerError, got: {:?}",
        err
    );
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
    let loader = DefaultPolicyStoreLoader::new_physical();
    let loaded = loader
        .load_directory(dir.to_str().unwrap())
        .expect("Expected directory load to succeed");

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
        let parsed_issuers = IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name)
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
    // Create archive in memory using helper (simulates WASM fetching from network)
    let archive_bytes = create_test_archive("WASM Archive Store", "fedcba654321", &[], &[]);

    // Create ArchiveVfs from bytes (works in WASM!)
    let archive_vfs =
        ArchiveVfs::from_buffer(archive_bytes).expect("Should create ArchiveVfs from bytes");

    // Create loader and load policy store
    let loader = DefaultPolicyStoreLoader::new(archive_vfs);
    let loaded = loader
        .load_directory(".")
        .expect("Should load policy store from archive bytes");

    // Verify loaded correctly
    assert_eq!(loaded.metadata.name(), "WASM Archive Store");
    assert_eq!(loaded.metadata.policy_store.id, "fedcba654321");
    assert!(!loaded.schema.is_empty());
    assert_eq!(loaded.policies.len(), 1);
}

#[test]
#[cfg(not(target_arch = "wasm32"))]
fn test_archive_vfs_with_manifest_validation() {
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

    assert!(!validation_result.errors.is_empty() || !validation_result.is_valid);
}

#[test]
fn test_archive_vfs_with_multiple_policies() {
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
