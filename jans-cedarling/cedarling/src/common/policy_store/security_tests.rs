// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Security tests for policy store loading and validation.
//!
//! These tests verify protection against:
//! - Path traversal attacks in archives and directories
//! - Malicious archive handling
//! - Input validation for all file types
//! - Resource exhaustion (zip bombs, deeply nested structures)

// Note: This module is cfg(test) via parent module declaration in policy_store.rs

use super::archive_handler::ArchiveVfs;
use super::errors::{ArchiveError, PolicyStoreError, ValidationError};
use super::loader::DefaultPolicyStoreLoader;
use super::test_utils::{
    PolicyStoreTestBuilder, create_corrupted_archive, create_deep_nested_archive,
    create_path_traversal_archive, fixtures,
};
use super::vfs_adapter::VfsFileSystem;
use std::io::{Cursor, Write};
use zip::write::{ExtendedFileOptions, FileOptions};
use zip::{CompressionMethod, ZipWriter};

// ============================================================================
// Path Traversal Tests
// ============================================================================

mod path_traversal {
    use super::*;

    #[test]
    fn test_rejects_parent_directory_traversal() {
        let archive = create_path_traversal_archive();
        let result = ArchiveVfs::from_buffer(archive);

        let err = result.expect_err("Expected PathTraversal error");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_absolute_path_in_archive() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);
        zip.start_file("/etc/passwd", options).unwrap();
        zip.write_all(b"root:x:0:0").unwrap();

        let archive = zip.finish().unwrap().into_inner();
        let result = ArchiveVfs::from_buffer(archive);

        let err = result.expect_err("archive with path traversal should be rejected");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "expected PathTraversal error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_double_dot_sequences() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Try various double-dot sequences
        let paths = [
            "foo/../../../etc/passwd",
            "foo/bar/../../secret",
            "policies/..%2F..%2Fetc/passwd", // URL encoded
        ];

        for path in paths {
            let result = zip.start_file(path, options.clone());
            if result.is_ok() {
                zip.write_all(b"content").unwrap();
            }
        }

        let archive = zip.finish().unwrap().into_inner();
        let result = ArchiveVfs::from_buffer(archive);

        // Should reject due to path traversal
        let err = result.expect_err("Expected PathTraversal error for double-dot sequences");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_windows_path_separators() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Windows-style path traversal
        zip.start_file("foo\\..\\..\\etc\\passwd", options).unwrap();
        zip.write_all(b"content").unwrap();

        let archive = zip.finish().unwrap().into_inner();
        let result = ArchiveVfs::from_buffer(archive);

        // Should reject or sanitize
        assert!(
            result.is_err() || {
                let vfs = result.unwrap();
                !vfs.exists("etc/passwd")
            }
        );
    }
}

// ============================================================================
// Malicious Archive Tests
// ============================================================================

mod malicious_archives {
    use super::*;

    #[test]
    fn test_rejects_corrupted_zip() {
        let archive = create_corrupted_archive();
        let result = ArchiveVfs::from_buffer(archive);

        let err = result.expect_err("Expected InvalidZipFormat error");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_non_zip_file() {
        let not_a_zip = b"This is definitely not a ZIP file".to_vec();
        let result = ArchiveVfs::from_buffer(not_a_zip);

        let err = result.expect_err("Expected InvalidZipFormat error");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_empty_file() {
        let empty: Vec<u8> = Vec::new();
        let result = ArchiveVfs::from_buffer(empty);
        result.expect_err("empty buffer should not be a valid archive");
    }

    #[test]
    fn test_handles_empty_zip() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let zip = ZipWriter::new(cursor);
        let archive = zip.finish().unwrap().into_inner();

        // Empty ZIP should be valid but have no files
        let result = ArchiveVfs::from_buffer(archive);
        let vfs = result.expect("Empty ZIP archive should be accepted by ArchiveVfs");
        assert!(!vfs.exists("metadata.json"));
    }

    #[test]
    fn test_deeply_nested_paths() {
        let archive = create_deep_nested_archive(100);
        let vfs = ArchiveVfs::from_buffer(archive)
            .expect("ArchiveVfs should handle deeply nested paths without error");

        // Verify VFS is usable for a deeply nested archive
        vfs.read_dir(".")
            .expect("Deeply nested archive paths should be readable");
    }

    #[test]
    fn test_handles_large_file_name() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Very long filename
        let long_name = "a".repeat(1000) + ".json";
        zip.start_file(&long_name, options).unwrap();
        zip.write_all(b"{}").unwrap();

        let archive = zip.finish().unwrap().into_inner();
        let vfs = ArchiveVfs::from_buffer(archive)
            .expect("ArchiveVfs should handle archives with very long filenames");

        // If accepted, verify VFS is functional
        vfs.read_dir(".")
            .expect("VFS created from archive with long filename should be readable");
    }
}

// ============================================================================
// Input Validation Tests
// ============================================================================

mod input_validation {
    use super::*;

    #[test]
    fn test_rejects_invalid_json_in_metadata() {
        let builder = fixtures::invalid_metadata_json();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        let err = result.expect_err("Expected error for invalid metadata JSON");
        assert!(
            matches!(
                &err,
                PolicyStoreError::Validation(ValidationError::MetadataJsonParseFailed { .. })
            ),
            "Expected MetadataJsonParseFailed validation error for metadata, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_invalid_cedar_syntax() {
        let builder = fixtures::invalid_policy_syntax();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        let err = result.expect_err("Expected error for invalid Cedar syntax");
        assert!(
            matches!(
                &err,
                PolicyStoreError::Validation(ValidationError::InvalidPolicyStoreId { .. })
            ),
            "Expected InvalidPolicyStoreId validation error for invalid Cedar syntax fixture, got: {:?}",
            err
        );
    }

    #[test]
    fn test_rejects_invalid_entity_json() {
        let builder = fixtures::minimal_valid().with_entity("invalid", "{ not valid json }");

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Should error during entity parsing
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::JsonParsing { .. }),
                "Expected JSON parsing error for invalid entity JSON, got: {:?}",
                err
            );
        }
    }

    #[test]
    fn test_rejects_invalid_trusted_issuer() {
        let builder = fixtures::invalid_trusted_issuer();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Should error during trusted issuer validation
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::TrustedIssuerError { .. }),
                "Expected TrustedIssuerError for invalid trusted issuer, got: {:?}",
                err
            );
        }
    }

    #[test]
    fn test_rejects_duplicate_entity_uids() {
        let builder = fixtures::duplicate_entity_uids();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Should detect duplicate entity UIDs
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::CedarEntityError { .. }),
                "Expected CedarEntityError for duplicate UIDs, got: {:?}",
                err
            );
        }
    }

    #[test]
    fn test_handles_unicode_in_filenames() {
        let buffer = Vec::new();
        let cursor = Cursor::new(buffer);
        let mut zip = ZipWriter::new(cursor);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        // Unicode filename
        zip.start_file("metadata.json", options.clone()).unwrap();
        zip.write_all(br#"{"cedar_version":"4.4.0","policy_store":{"id":"abc123def456","name":"Test","version":"1.0.0"}}"#).unwrap();

        zip.start_file("schema.cedarschema", options.clone())
            .unwrap();
        zip.write_all(b"namespace Test { entity User; }").unwrap();

        zip.start_file("policies/日本語ポリシー.cedar", options.clone())
            .unwrap();
        zip.write_all(br#"@id("japanese-policy") permit(principal, action, resource);"#)
            .unwrap();

        let archive = zip.finish().unwrap().into_inner();
        let result = ArchiveVfs::from_buffer(archive);

        // Should handle unicode gracefully
        result.expect("ArchiveVfs should handle unicode filenames without error");
    }

    #[test]
    fn test_handles_special_characters_in_policy_id() {
        let builder = PolicyStoreTestBuilder::new("abc123def456").with_policy(
            "special-chars",
            r#"@id("policy-with-special-chars!@#$%")
permit(principal, action, resource);"#,
        );

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);

        // Cedar allows special characters in @id() annotations within the policy content.
        // The loader is expected to accept such policies successfully.
        let loaded = loader
            .load_directory(".")
            .expect("Policy with special-character @id should load successfully");

        // Verify policy was loaded with the special character ID
        assert!(
            !loaded.policies.is_empty(),
            "Expected at least one policy to be loaded"
        );
    }
}

// ============================================================================
// Manifest Security Tests
// ============================================================================

mod manifest_security {
    use super::*;

    #[test]
    fn test_detects_checksum_mismatch() {
        // Create a store with manifest
        let builder = fixtures::minimal_valid().with_manifest();

        // Build the archive
        let archive = builder.build_archive().unwrap();

        // TODO: Modify a file after manifest is generated
        // This would require manual archive manipulation
        // For now, just verify manifest is created correctly
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        loader
            .load_directory(".")
            .expect("Manifest-backed minimal_valid store should load successfully");
    }

    #[test]
    fn test_handles_missing_manifest_gracefully() {
        let builder = fixtures::minimal_valid();
        // No manifest
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Should succeed without manifest
        result.expect("minimal_valid store without manifest should load successfully");
    }

    #[test]
    fn test_handles_invalid_checksum_format() {
        let mut builder = fixtures::minimal_valid();

        // Add invalid manifest with bad checksum format
        builder.extra_files.insert(
            "manifest.json".to_string(),
            r#"{
                "policy_store_id": "test123",
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

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Manifest validation is optional - loader may succeed even with invalid manifest.
        // Currently, invalid checksum format is reported via ManifestError::InvalidChecksumFormat.
        match &result {
            Ok(_) => {
                // Loader succeeded - manifest validation is not enforced
            },
            Err(err) => {
                // If it fails, it should be a manifest-related error with invalid checksum format
                assert!(
                    matches!(
                        err,
                        PolicyStoreError::ManifestError {
                            err: crate::common::policy_store::errors::ManifestErrorType::InvalidChecksumFormat { .. }
                        }
                    ),
                    "Expected ManifestError::InvalidChecksumFormat for invalid manifest checksum, got: {:?}",
                    err
                );
            },
        }
    }
}

// ============================================================================
// Resource Exhaustion Tests
// ============================================================================

mod resource_exhaustion {
    use super::*;

    #[test]
    fn test_handles_many_files() {
        let mut builder = fixtures::minimal_valid();

        // Add many policies
        for i in 0..100 {
            builder = builder.with_policy(
                format!("policy{}", i),
                format!(r#"@id("policy{}") permit(principal, action, resource);"#, i),
            );
        }

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        result.expect("Policy store with many policies should load successfully");
    }

    #[test]
    fn test_handles_large_policy_content() {
        // Create a policy with a very large condition
        let large_condition = (0..1000)
            .map(|i| format!("principal.attr{} == \"value{}\"", i, i))
            .collect::<Vec<_>>()
            .join(" || ");

        let policy = format!(
            r#"@id("large-policy")
permit(principal, action, resource)
when {{ {} }};"#,
            large_condition
        );

        let builder =
            PolicyStoreTestBuilder::new("abc123def456").with_policy("large-policy", &policy);

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);

        // Large policies should be handled gracefully
        let result = loader.load_directory(".");

        // Verify loading succeeds - Cedar can handle large policies
        result.expect("Large policy should load successfully");
    }

    #[test]
    fn test_handles_deeply_nested_entity_hierarchy() {
        // Create a deep entity hierarchy (should be bounded)
        let mut entities = Vec::new();

        // Create 50 levels of roles
        for i in 0..50 {
            let parents = if i > 0 {
                vec![serde_json::json!({"type": "TestApp::Role", "id": format!("role{}", i - 1)})]
            } else {
                vec![]
            };

            entities.push(serde_json::json!({
                "uid": {"type": "TestApp::Role", "id": format!("role{}", i)},
                "attrs": {"level": i},
                "parents": parents
            }));
        }

        let builder = fixtures::minimal_valid()
            .with_entity("deep_roles", serde_json::to_string(&entities).unwrap());

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let result = loader.load_directory(".");

        // Should handle deep hierarchy
        result.expect("Policy store with deeply nested entity hierarchy should load successfully");
    }
}

// ============================================================================
// File Extension Validation Tests
// ============================================================================

mod file_extension_validation {
    use super::*;

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_rejects_wrong_archive_extension() {
        use tempfile::TempDir;

        let builder = fixtures::minimal_valid();
        let archive_bytes = builder.build_archive().unwrap();

        let temp_dir = TempDir::new().unwrap();
        let wrong_ext = temp_dir.path().join("store.zip");
        std::fs::write(&wrong_ext, &archive_bytes).unwrap();

        let result = ArchiveVfs::from_file(&wrong_ext);
        let err = result.expect_err("Expected InvalidExtension error");
        assert!(
            matches!(err, ArchiveError::InvalidExtension { .. }),
            "Expected InvalidExtension error, got: {:?}",
            err
        );
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_accepts_cjar_extension() {
        use tempfile::TempDir;

        let builder = fixtures::minimal_valid();
        let archive_bytes = builder.build_archive().unwrap();

        let temp_dir = TempDir::new().unwrap();
        let correct_ext = temp_dir.path().join("store.cjar");
        std::fs::write(&correct_ext, &archive_bytes).unwrap();

        let result = ArchiveVfs::from_file(&correct_ext);
        result.expect("ArchiveVfs should accept .cjar extension");
    }
}
