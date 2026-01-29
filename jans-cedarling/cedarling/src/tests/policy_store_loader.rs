// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Integration tests for the new policy store loader.
//!
//! These tests verify that:
//! - Directory-based policy stores load correctly and can be used for authorization
//! - Cedar Archive (.cjar) files load correctly and can be used for authorization
//! - Manifest validation works as expected (checksums, policy store ID matching)
//! - Error cases are handled properly at the API level
//!
//! The tests use the same `Cedarling` API and patterns as other integration tests,
//! ensuring the new loader paths work end-to-end.
//!
//! ## Platform Support
//!
//! - **Native platforms**: All tests run, including directory/file-based loading
//! - **WASM**: Tests using `CjarUrl` and `load_policy_store_archive_bytes` work,
//!   as they don't require filesystem access. Directory and file-based tests are
//!   skipped with `#[cfg(not(target_arch = "wasm32"))]`.

#[cfg(not(target_arch = "wasm32"))]
use std::fs;
#[cfg(not(target_arch = "wasm32"))]
use std::io::Read;

use serde_json::json;
#[cfg(not(target_arch = "wasm32"))]
use tempfile::TempDir;
use tokio::test;
#[cfg(not(target_arch = "wasm32"))]
use zip::read::ZipArchive;

use super::utils::*;
use crate::common::policy_store::test_utils::PolicyStoreTestBuilder;
use crate::tests::utils::cedarling_util::get_cedarling_with_callback;
use crate::tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request};
use crate::{BootstrapConfig, Cedarling, PolicyStoreConfig, PolicyStoreSource};

// ============================================================================
// Helper Functions
// ============================================================================

/// Creates a policy store builder configured for authorization testing.
///
/// This builder includes:
/// - A schema with User, Resource, and Action types
/// - A simple "allow-read" policy
/// - A "deny-write-guest" policy based on `user_type` attribute
fn create_authz_policy_store_builder() -> PolicyStoreTestBuilder {
    PolicyStoreTestBuilder::new("a1b2c3d4e5f6a7b8")
        .with_name("Integration Test Policy Store")
        .with_schema(
            r#"namespace TestApp {
    entity User {
        name: String,
        user_type: String,
    };
    entity Resource {
        name: String,
    };
    
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
    
    action "write" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
"#,
        )
        .with_policy(
            "allow-read",
            r#"@id("allow-read")
permit(
    principal,
    action == TestApp::Action::"read",
    resource
);"#,
        )
        .with_policy(
            "deny-write-guest",
            r#"@id("deny-write-guest")
forbid(
    principal,
    action == TestApp::Action::"write",
    resource
) when { principal.user_type == "guest" };"#,
        )
}

/// Extracts a zip archive to a temporary directory.
#[cfg(not(target_arch = "wasm32"))]
fn extract_archive_to_temp_dir(archive_bytes: &[u8]) -> TempDir {
    let temp_dir = TempDir::new().expect("Failed to create temp directory");
    let mut zip_archive =
        ZipArchive::new(std::io::Cursor::new(archive_bytes)).expect("Failed to read zip archive");

    for i in 0..zip_archive.len() {
        let mut file = zip_archive.by_index(i).expect("Failed to get zip entry");
        let file_path = temp_dir.path().join(file.name());

        if file.is_dir() {
            fs::create_dir_all(&file_path).expect("Failed to create directory");
        } else {
            if let Some(parent) = file_path.parent() {
                fs::create_dir_all(parent).expect("Failed to create parent directory");
            }
            let mut contents = Vec::new();
            file.read_to_end(&mut contents)
                .expect("Failed to read file contents");
            fs::write(&file_path, contents).expect("Failed to write file");
        }
    }

    temp_dir
}

/// Creates a Cedarling instance from a directory path.
///
/// Disables default entity building (user, workload, roles) since we're using
/// a custom schema that doesn't include the Jans namespace types.
/// Uses a custom `principal_bool_operator` that checks for `TestApp::User` principal.
async fn get_cedarling_from_directory(path: std::path::PathBuf) -> Cedarling {
    use crate::JsonRule;

    get_cedarling_with_callback(PolicyStoreSource::Directory(path), |config| {
        // Disable default entity builders that expect Jans namespace types
        config.entity_builder_config.build_user = false;
        config.entity_builder_config.build_workload = false;
        config.authorization_config.use_user_principal = false;
        config.authorization_config.use_workload_principal = false;

        // Use a custom operator that checks for our TestApp::User principal
        config.authorization_config.principal_bool_operator = JsonRule::new(json!({
            "===": [{"var": "TestApp::User"}, "ALLOW"]
        }))
        .expect("Failed to create principal bool operator");
    })
    .await
}

/// Creates a Cedarling instance from an archive file path.
///
/// Disables default entity building (user, workload, roles) since we're using
/// a custom schema that doesn't include the Jans namespace types.
/// Uses a custom `principal_bool_operator` that checks for `TestApp::User` principal.
async fn get_cedarling_from_cjar_file(path: std::path::PathBuf) -> Cedarling {
    use crate::JsonRule;

    get_cedarling_with_callback(PolicyStoreSource::CjarFile(path), |config| {
        // Disable default entity builders that expect Jans namespace types
        config.entity_builder_config.build_user = false;
        config.entity_builder_config.build_workload = false;
        config.authorization_config.use_user_principal = false;
        config.authorization_config.use_workload_principal = false;

        // Use a custom operator that checks for our TestApp::User principal
        config.authorization_config.principal_bool_operator = JsonRule::new(json!({
            "===": [{"var": "TestApp::User"}, "ALLOW"]
        }))
        .expect("Failed to create principal bool operator");
    })
    .await
}

// ============================================================================
// Directory-Based Loading Tests
// ============================================================================

/// Test that a policy store loaded from a directory works for authorization.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_load_from_directory_and_authorize_success() {
    // Build archive and extract to temp directory

    use crate::tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request};
    let builder = create_authz_policy_store_builder();
    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Create Cedarling from directory
    let cedarling = get_cedarling_from_directory(temp_dir.path().to_path_buf()).await;

    // Create an authorization request
    let request = create_test_unsigned_request(
        "TestApp::Action::\"read\"",
        vec![
            create_test_principal(
                "TestApp::User",
                "user1",
                json!({"name": "Test User", "user_type": "admin"}),
            )
            .expect("Failed to create principal"),
        ],
        create_test_principal(
            "TestApp::Resource",
            "resource1",
            json!({"name": "Test Resource"}),
        )
        .expect("Failed to create resource"),
    );

    // Execute authorization
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("Authorization should succeed");

    // Verify the result - read action should be allowed
    assert!(
        result.decision,
        "Read action should be allowed by the allow-read policy"
    );
}

/// Test that write action is denied for guest users when loaded from directory.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_load_from_directory_deny_write_for_guest() {
    // Build archive and extract to temp directory
    let builder = create_authz_policy_store_builder();
    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Create Cedarling from directory
    let cedarling = get_cedarling_from_directory(temp_dir.path().to_path_buf()).await;

    // Create an authorization request for write action with guest user_type
    let request = create_test_unsigned_request(
        "TestApp::Action::\"write\"",
        vec![
            create_test_principal(
                "TestApp::User",
                "guest_user",
                json!({"name": "Guest User", "user_type": "guest"}),
            )
            .expect("Failed to create principal"),
        ],
        create_test_principal(
            "TestApp::Resource",
            "resource1",
            json!({"name": "Test Resource"}),
        )
        .expect("Failed to create resource"),
    );

    // Execute authorization
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("Authorization should succeed");

    // Verify the result - write action should be denied for guest
    assert!(
        !result.decision,
        "Write action should be denied for guest users by the deny-write-guest policy"
    );
}

// ============================================================================
// Archive (.cjar) Loading Tests
// ============================================================================

/// Test that a policy store loaded from a .cjar file works for authorization.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_load_from_cjar_file_and_authorize_success() {
    // Build archive
    let builder = create_authz_policy_store_builder();
    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");

    // Write archive to temp file
    let temp_dir = TempDir::new().expect("Failed to create temp directory");
    let archive_path = temp_dir.path().join("test_policy_store.cjar");
    fs::write(&archive_path, &archive).expect("Failed to write archive file");

    // Create Cedarling from archive file
    let cedarling = get_cedarling_from_cjar_file(archive_path).await;

    // Create an authorization request
    let request = create_test_unsigned_request(
        "TestApp::Action::\"read\"",
        vec![
            create_test_principal(
                "TestApp::User",
                "user1",
                json!({"name": "Test User", "user_type": "admin"}),
            )
            .expect("Failed to create principal"),
        ],
        create_test_principal(
            "TestApp::Resource",
            "resource1",
            json!({"name": "Test Resource"}),
        )
        .expect("Failed to create resource"),
    );

    // Execute authorization
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("Authorization should succeed");

    // Verify the result
    assert!(
        result.decision,
        "Read action should be allowed by the allow-read policy"
    );
}

// ============================================================================
// Manifest Validation Tests
// ============================================================================

/// Test that manifest validation detects checksum mismatches.
///
/// This test uses `load_policy_store_directory` which performs manifest validation.
/// An invalid checksum format in the manifest should cause initialization to fail.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_manifest_validation_invalid_checksum_format() {
    use super::utils::cedarling_util::get_config;
    use crate::common::policy_store::test_utils::fixtures;

    let mut builder = fixtures::minimal_valid();

    // Add manifest with invalid checksum format (missing sha256: prefix)
    builder.extra_files.insert(
        "manifest.json".to_string(),
        r#"{
            "policy_store_id": "abc123def456",
            "generated_date": "2024-01-01T00:00:00Z",
            "files": {
                "metadata.json": {
                    "size": 100,
                    "checksum": "invalid_format_no_sha256_prefix"
                }
            }
        }"#
        .to_string(),
    );

    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Attempt to create Cedarling - should fail due to invalid checksum format
    let config = get_config(PolicyStoreSource::Directory(temp_dir.path().to_path_buf()));

    let err = Cedarling::new(&config)
        .await
        .err()
        .expect("Cedarling initialization should fail with invalid checksum format");

    // Verify the error is a Directory error containing the checksum format message
    assert!(
        matches!(
            &err,
            crate::InitCedarlingError::ServiceConfig(
                crate::init::service_config::ServiceConfigError::PolicyStore(
                    crate::init::policy_store::PolicyStoreLoadError::Directory(msg)
                )
            ) if msg.contains("Invalid checksum format")
        ),
        "Expected Directory error with 'Invalid checksum format', got: {err:?}"
    );
}

/// Test that manifest validation detects policy store ID mismatches.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_manifest_validation_policy_store_id_mismatch() {
    use super::utils::cedarling_util::get_config;
    use crate::common::policy_store::test_utils::fixtures;

    let mut builder = fixtures::minimal_valid();

    // Add manifest with wrong policy_store_id (metadata has "abc123def456")
    builder.extra_files.insert(
        "manifest.json".to_string(),
        r#"{
            "policy_store_id": "wrong_id_12345",
            "generated_date": "2024-01-01T00:00:00Z",
            "files": {}
        }"#
        .to_string(),
    );

    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Attempt to create Cedarling - should fail due to ID mismatch
    let config = get_config(PolicyStoreSource::Directory(temp_dir.path().to_path_buf()));

    let err = Cedarling::new(&config)
        .await
        .err()
        .expect("Cedarling initialization should fail with policy store ID mismatch");

    // Verify the error is a Directory error containing the ID mismatch message
    assert!(
        matches!(
            &err,
            crate::InitCedarlingError::ServiceConfig(
                crate::init::service_config::ServiceConfigError::PolicyStore(
                    crate::init::policy_store::PolicyStoreLoadError::Directory(msg)
                )
            ) if msg.contains("Policy store ID mismatch")
        ),
        "Expected Directory error with 'Policy store ID mismatch', got: {err:?}"
    );
}

// ============================================================================
// Policy Store with Entities Tests
// ============================================================================

/// Test loading a policy store with pre-defined entities.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_load_directory_with_entities() {
    // Build a policy store with entities
    let builder = PolicyStoreTestBuilder::new("e1e2e3e4e5e6e7e8")
        .with_name("Entity Test Policy Store")
        .with_schema(
            r#"namespace TestApp {
    entity User {
        name: String,
        department: String,
    };
    entity Resource {
        name: String,
        owner: String,
    };
    
    action "access" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
"#,
        )
        .with_policy(
            "allow-same-department",
            r#"@id("allow-same-department")
permit(
    principal,
    action == TestApp::Action::"access",
    resource
);"#,
        )
        .with_entity(
            "users",
            serde_json::to_string(&json!([
                {
                    "uid": {"type": "TestApp::User", "id": "alice"},
                    "attrs": {
                        "name": "Alice",
                        "department": "engineering"
                    },
                    "parents": []
                }
            ]))
            .unwrap(),
        )
        .with_entity(
            "resources",
            serde_json::to_string(&json!([
                {
                    "uid": {"type": "TestApp::Resource", "id": "doc1"},
                    "attrs": {
                        "name": "Design Document",
                        "owner": "engineering"
                    },
                    "parents": []
                }
            ]))
            .unwrap(),
        );

    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Create Cedarling from directory
    let cedarling = get_cedarling_from_directory(temp_dir.path().to_path_buf()).await;

    // Create an authorization request
    let request = create_test_unsigned_request(
        "TestApp::Action::\"access\"",
        vec![
            create_test_principal(
                "TestApp::User",
                "alice",
                json!({"name": "Alice", "department": "engineering"}),
            )
            .expect("Failed to create principal"),
        ],
        create_test_principal(
            "TestApp::Resource",
            "doc1",
            json!({"name": "Design Document", "owner": "engineering"}),
        )
        .expect("Failed to create resource"),
    );

    // Execute authorization
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("Authorization should succeed");

    // Verify the result
    assert!(
        result.decision,
        "Access should be allowed by the allow-same-department policy"
    );
}

// ============================================================================
// Multiple Policies Tests
// ============================================================================
fn create_multiple_policy_store_builder() -> PolicyStoreTestBuilder {
    PolicyStoreTestBuilder::new("f1f2f3f4f5f6f7f8")
        .with_name("Multi-Policy Test Store")
        .with_schema(
            r#"namespace TestApp {
    entity User {
        user_role: String,
    };
    entity Resource;
    
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
    
    action "write" appliesTo {
        principal: [User],
        resource: [Resource]
    };
    
    action "delete" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}
"#,
        )
        .with_policy(
            "allow-read-all",
            r#"@id("allow-read-all")
permit(
    principal,
    action == TestApp::Action::"read",
    resource
);"#,
        )
        .with_policy(
            "allow-write-admin",
            r#"@id("allow-write-admin")
permit(
    principal,
    action == TestApp::Action::"write",
    resource
) when { principal.user_role == "admin" };"#,
        )
        .with_policy(
            "deny-delete-all",
            r#"@id("deny-delete-all")
forbid(
    principal,
    action == TestApp::Action::"delete",
    resource
);"#,
        )
}
/// Test loading a policy store with multiple policies and verifying correct policy evaluation.
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_load_directory_with_multiple_policies() {
    // Build a policy store with multiple policies
    let builder = create_multiple_policy_store_builder();

    let archive = builder
        .build_archive()
        .expect("Failed to build test archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Create Cedarling from directory
    let cedarling = get_cedarling_from_directory(temp_dir.path().to_path_buf()).await;

    // Test 1: Read should be allowed for any user
    let read_request = create_test_unsigned_request(
        "TestApp::Action::\"read\"",
        vec![
            create_test_principal("TestApp::User", "user1", json!({"user_role": "viewer"}))
                .expect("Failed to create principal"),
        ],
        create_test_principal("TestApp::Resource", "resource1", json!({}))
            .expect("Failed to create resource"),
    );

    let read_result = cedarling
        .authorize_unsigned(read_request)
        .await
        .expect("Read authorization should succeed");

    assert!(read_result.decision, "Read should be allowed for any user");

    // Test 2: Write should be allowed only for admin
    let write_admin_request = create_test_unsigned_request(
        "TestApp::Action::\"write\"",
        vec![
            create_test_principal("TestApp::User", "admin1", json!({"user_role": "admin"}))
                .expect("Failed to create principal"),
        ],
        create_test_principal("TestApp::Resource", "resource1", json!({}))
            .expect("Failed to create resource"),
    );

    let write_admin_result = cedarling
        .authorize_unsigned(write_admin_request)
        .await
        .expect("Write authorization should succeed");

    assert!(
        write_admin_result.decision,
        "Write should be allowed for admin"
    );

    // Test 3: Write should be denied for non-admin
    let write_viewer_request = create_test_unsigned_request(
        "TestApp::Action::\"write\"",
        vec![
            create_test_principal("TestApp::User", "user1", json!({"user_role": "viewer"}))
                .expect("Failed to create principal"),
        ],
        create_test_principal("TestApp::Resource", "resource1", json!({}))
            .expect("Failed to create resource"),
    );

    let write_viewer_result = cedarling
        .authorize_unsigned(write_viewer_request)
        .await
        .expect("Write authorization should succeed");

    assert!(
        !write_viewer_result.decision,
        "Write should be denied for non-admin"
    );

    // Test 4: Delete should be denied for everyone
    let delete_request = create_test_unsigned_request(
        "TestApp::Action::\"delete\"",
        vec![
            create_test_principal("TestApp::User", "admin1", json!({"user_role": "admin"}))
                .expect("Failed to create principal"),
        ],
        create_test_principal("TestApp::Resource", "resource1", json!({}))
            .expect("Failed to create resource"),
    );

    let delete_result = cedarling
        .authorize_unsigned(delete_request)
        .await
        .expect("Delete authorization should succeed");

    assert!(
        !delete_result.decision,
        "Delete should be denied for everyone"
    );
}

// ============================================================================
// Archive URL Tests (WASM-Compatible via CjarUrl)
// ============================================================================

/// Test loading a policy store from a URL using mockito.
///
/// This test is WASM-compatible as it uses HTTP to fetch the archive,
/// which works in both native and WASM environments.
#[test]
async fn test_load_from_cjar_url_and_authorize_success() {
    use crate::JsonRule;
    use mockito::Server;

    // Build archive bytes
    let builder = create_authz_policy_store_builder();
    let archive_bytes = builder
        .build_archive()
        .expect("Failed to build test archive");

    // Create mock server
    let mut server = Server::new_async().await;
    let mock = server
        .mock("GET", "/policy-store.cjar")
        .with_status(200)
        .with_header("content-type", "application/octet-stream")
        .with_body(archive_bytes)
        .create_async()
        .await;

    let cjar_url = format!("{}/policy-store.cjar", server.url());

    // Create Cedarling from CjarUrl
    let cedarling = get_cedarling_with_callback(PolicyStoreSource::CjarUrl(cjar_url), |config| {
        // Disable default entity builders that expect Jans namespace types
        config.entity_builder_config.build_user = false;
        config.entity_builder_config.build_workload = false;
        config.authorization_config.use_user_principal = false;
        config.authorization_config.use_workload_principal = false;

        // Use a custom operator that checks for our TestApp::User principal
        config.authorization_config.principal_bool_operator = JsonRule::new(json!({
            "===": [{"var": "TestApp::User"}, "ALLOW"]
        }))
        .expect("Failed to create principal bool operator");
    })
    .await;

    // Verify the mock was called
    mock.assert_async().await;

    // Create an authorization request
    let request = create_test_unsigned_request(
        "TestApp::Action::\"read\"",
        vec![
            create_test_principal(
                "TestApp::User",
                "user1",
                json!({"name": "Test User", "user_type": "admin"}),
            )
            .expect("Failed to create principal"),
        ],
        create_test_principal(
            "TestApp::Resource",
            "resource1",
            json!({"name": "Test Resource"}),
        )
        .expect("Failed to create resource"),
    );

    // Execute authorization
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("Authorization should succeed");

    // Verify the result
    assert!(
        result.decision,
        "Read action should be allowed when loading from CjarUrl"
    );
}

/// Test that `CjarUrl` handles HTTP errors gracefully.
/// The HTTP client retries on HTTP error status codes before failing.
#[test]
async fn test_cjar_url_handles_http_error() {
    use super::utils::cedarling_util::get_config;
    use mockito::Server;

    // Create mock server that returns 404
    // Note: The HTTP client will retry on HTTP errors, so expect multiple requests
    let mut server = Server::new_async().await;
    let mock = server
        .mock("GET", "/nonexistent.cjar")
        .with_status(404)
        .with_body("Not Found")
        .expect_at_least(1)
        .create_async()
        .await;

    let cjar_url = format!("{}/nonexistent.cjar", server.url());

    // Attempt to create Cedarling - should fail after retries
    let config = get_config(PolicyStoreSource::CjarUrl(cjar_url));

    let err = Cedarling::new(&config)
        .await
        .err()
        .expect("Cedarling initialization should fail after retries on 404 error");

    // Verify the mock was called at least once
    mock.assert_async().await;

    // Verify the error is an Archive error (max retries exceeded after HTTP errors)
    assert!(
        matches!(
            &err,
            crate::InitCedarlingError::ServiceConfig(
                crate::init::service_config::ServiceConfigError::PolicyStore(
                    crate::init::policy_store::PolicyStoreLoadError::Archive(_)
                )
            )
        ),
        "Expected Archive error after retries, got: {err:?}"
    );
}

/// Test loading archive from bytes directly using the loader function.
///
/// This tests the `load_policy_store_archive_bytes` function which is the
/// underlying mechanism used by `CjarUrl` and is WASM-compatible.
#[test]
async fn test_load_policy_store_archive_bytes_directly() {
    use crate::common::policy_store::loader::load_policy_store_archive_bytes;

    // Build archive bytes
    let builder = create_authz_policy_store_builder();
    let archive_bytes = builder
        .build_archive()
        .expect("Failed to build test archive");

    // Load directly using the bytes loader
    let loaded = load_policy_store_archive_bytes(&archive_bytes)
        .expect("Should load policy store from bytes");

    // Verify the loaded policy store
    assert_eq!(
        loaded.metadata.policy_store.id, "a1b2c3d4e5f6a7b8",
        "Policy store ID should match"
    );
    assert_eq!(
        loaded.metadata.policy_store.name, "Integration Test Policy Store",
        "Policy store name should match"
    );
    assert!(
        !loaded.policies.is_empty(),
        "Should have loaded at least one policy"
    );
    assert_eq!(loaded.policies.len(), 2, "Should have loaded 2 policies");

    // Verify policy content
    let policy_names: Vec<&str> = loaded.policies.iter().map(|p| p.name.as_str()).collect();
    assert!(
        policy_names.contains(&"allow-read.cedar"),
        "Should have allow-read policy"
    );
    assert!(
        policy_names.contains(&"deny-write-guest.cedar"),
        "Should have deny-write-guest policy"
    );
}

/// Test that invalid archive bytes are rejected.
#[test]
async fn test_load_policy_store_archive_bytes_invalid() {
    use crate::common::policy_store::loader::load_policy_store_archive_bytes;

    // Try to load invalid bytes
    let invalid_bytes = vec![0x00, 0x01, 0x02, 0x03];
    let err = load_policy_store_archive_bytes(&invalid_bytes)
        .expect_err("Should fail to load invalid archive bytes");

    // Verify the error is an Archive error (invalid zip format)
    assert!(
        matches!(
            err,
            crate::common::policy_store::errors::PolicyStoreError::Archive(_)
        ),
        "Expected Archive error for invalid bytes, got: {err:?}"
    );
}

// ============================================================================
// JWT Authorization Tests (using MockServer)
// ============================================================================

// Schema that works with JWT-based authorization
// Uses Jans namespace to match the default entity builder
const SCHEMA: &str = r#"namespace Jans {
    type Url = {"host": String, "path": String, "protocol": String};
    entity TrustedIssuer = {"issuer_entity_id": Url};
    entity Access_token = {
        aud: String,
        exp: Long,
        iat: Long,
        iss: TrustedIssuer,
        jti: String,
        client_id?: String,
        org_id?: String,
    };
    entity Id_token = {
        aud: Set<String>,
        exp: Long,
        iat: Long,
        iss: TrustedIssuer,
        jti: String,
        sub: String,
    };
    entity Userinfo_token = {
        country?: String,
        exp?: Long,
        iat?: Long,
        iss: TrustedIssuer,
        jti: String,
        sub: String,
        role?: Set<String>,
    };
    entity Workload {
        iss: TrustedIssuer,
        access_token: Access_token,
        client_id: String,
        org_id?: String,
    };
    entity User {
        userinfo_token: Userinfo_token,
        country?: String,
        role?: Set<String>,
        sub: String,
    };
    entity Role;
    entity Resource {
        org_id?: String,
        country?: String,
    };
    action "Read" appliesTo {
        principal: [Workload, User, Role],
        resource: [Resource],
        context: {}
    };
}
"#;

fn prepare_cedarling_request(
    access_token: &str,
    id_token: &str,
    userinfo_token: &str,
) -> Result<Request, serde_json::Error> {
    Request::deserialize(json!({
        "tokens": {
            "access_token": access_token,
            "id_token": id_token,
            "userinfo_token": userinfo_token,
        },
        "action": "Jans::Action::\"Read\"",
        "resource": {
            "cedar_entity_mapping": {
                "entity_type": "Jans::Resource",
                "id": "resource1"
            },
            "org_id": "test_org",
            "country": "US"
        },
        "context": {},
    }))
}

fn create_jwt_trusted_issuer_json(oidc_endpoint: &str) -> String {
    format!(
        r#"{{
        "id": "mock_issuer",
        "name": "Jans",
        "description": "Test issuer for JWT validation",
        "configuration_endpoint": "{oidc_endpoint}",
        "token_metadata": {{
            "access_token": {{
                "entity_type_name": "Jans::Access_token",
                "workload_id": "client_id",
                "principal_mapping": ["Jans::Workload"]
            }},
            "id_token": {{
                "entity_type_name": "Jans::Id_token"
            }},
            "userinfo_token": {{
                "entity_type_name": "Jans::Userinfo_token",
                "user_id": "sub",
                "role_mapping": "role"
            }}
        }}
    }}"#
    )
}

fn create_jwt_cedarling_config(
    policy_store_source: PolicyStoreSource,
    jwt_sig_validation: bool,
) -> BootstrapConfig {
    use crate::{
        AuthorizationConfig, BootstrapConfig, EntityBuilderConfig, JsonRule, JwtConfig, LogConfig,
        LogTypeConfig,
    };

    BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut(crate::log::StdOutLoggerMode::Immediate),
            log_level: crate::LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: policy_store_source,
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation,
            jwt_status_validation: false,
            ..Default::default()
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: true,
            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: vec![],
            decision_log_workload_claims: vec!["client_id".to_string()],
            id_token_trust_mode: crate::IdTokenTrustMode::Never,
            principal_bool_operator: JsonRule::new(json!({
                "===": [{"var": "Jans::Workload"}, "ALLOW"]
            }))
            .expect("Failed to create principal bool operator"),
        },
        entity_builder_config: EntityBuilderConfig {
            build_user: false,
            build_workload: true,
            ..Default::default()
        },
        lock_config: None,
        max_default_entities: None,
        max_base64_size: None,
    }
}

/// Test the `authorize` method with signed JWTs loaded from a directory-based policy store.
///
/// This test verifies the full flow:
/// 1. Create a policy store with a trusted issuer pointing to `MockServer`
/// 2. `MockServer` provides OIDC config and JWKS endpoints
/// 3. Generate signed JWTs using `MockServer`
/// 4. Call `authorize` with the signed tokens
#[test]
#[cfg(not(target_arch = "wasm32"))]
async fn test_authorize_with_jwt_from_directory() {
    use crate::jwt::test_utils::MockServer;

    // Create mock server for OIDC/JWKS
    let mut mock_server = MockServer::new_with_defaults()
        .await
        .expect("Failed to create mock server");

    let issuer_url = mock_server.issuer();
    let oidc_endpoint = format!("{issuer_url}/.well-known/openid-configuration");

    // Create trusted issuer JSON that points to mock server
    // Uses "Jans" as the issuer name to match the default entity builder namespace
    let trusted_issuer_json = create_jwt_trusted_issuer_json(&oidc_endpoint);

    // Build the policy store
    let builder = PolicyStoreTestBuilder::new("a1b2c3d4e5f6a7b8")
        .with_name("JWT Test Policy Store")
        .with_schema(SCHEMA)
        .with_policy(
            "allow-workload-read",
            r#"@id("allow-workload-read")
permit(
    principal is Jans::Workload,
    action == Jans::Action::"Read",
    resource is Jans::Resource
)when{
    principal.access_token.org_id == resource.org_id
};"#,
        )
        .with_trusted_issuer("mock_issuer", trusted_issuer_json);

    let archive = builder.build_archive().expect("Failed to build archive");
    let temp_dir = extract_archive_to_temp_dir(&archive);

    // Generate signed tokens using MockServer
    let access_token = mock_server
        .generate_token_with_hs256sig(
            &mut json!({
                "org_id": "test_org",
                "jti": "access_jti",
                "client_id": "test_client",
                "aud": "test_aud",
                "exp": chrono::Utc::now().timestamp() + 3600,
                "iat": chrono::Utc::now().timestamp(),
            }),
            None,
        )
        .expect("Failed to generate access token");

    let id_token = mock_server
        .generate_token_with_hs256sig(
            &mut json!({
                "jti": "id_jti",
                "aud": ["test_aud"],
                "sub": "test_user",
                "exp": chrono::Utc::now().timestamp() + 3600,
                "iat": chrono::Utc::now().timestamp(),
            }),
            None,
        )
        .expect("Failed to generate id token");

    let userinfo_token = mock_server
        .generate_token_with_hs256sig(
            &mut json!({
                "jti": "userinfo_jti",
                "sub": "test_user",
                "country": "US",
                "role": ["Admin"],
                "exp": chrono::Utc::now().timestamp() + 3600,
                "iat": chrono::Utc::now().timestamp(),
            }),
            None,
        )
        .expect("Failed to generate userinfo token");

    // Configure Cedarling with JWT validation enabled
    let config = create_jwt_cedarling_config(
        PolicyStoreSource::Directory(temp_dir.path().to_path_buf()),
        true,
    );

    let cedarling = crate::Cedarling::new(&config)
        .await
        .expect("Cedarling should initialize with JWT-enabled config");

    // Create authorization request with signed JWTs
    let request = prepare_cedarling_request(&access_token, &id_token, &userinfo_token)
        .expect("Request should be deserialized");

    // Execute authorization with valid signed tokens
    let result = cedarling
        .authorize(request)
        .await
        .expect("Authorization should succeed with valid JWTs");

    assert!(
        result.decision,
        "Read action should be allowed for workload with matching org_id"
    );

    // Prove JWT validation is enforced: tampered token should fail
    // Create a request with an invalid/tampered access token
    let tampered_token = format!("{access_token}.tampered");
    let invalid_request = prepare_cedarling_request(&tampered_token, &id_token, &userinfo_token)
        .expect("Request should be deserialized");

    let invalid_result = cedarling.authorize(invalid_request).await;
    let err = invalid_result
        .expect_err("Authorization should fail with tampered JWT when validation is enabled");
    // Tampered JWT should result in a JWT validation error
    assert!(
        matches!(&err, crate::authz::AuthorizeError::ProcessTokens(_)),
        "Expected JWT processing error for tampered token, got: {err:?}"
    );
}
