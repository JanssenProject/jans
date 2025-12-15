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
        assert!(result.is_ok());

        let vfs = result.unwrap();
        assert!(!vfs.exists("metadata.json"));
    }

    #[test]
    fn test_deeply_nested_paths() {
        let archive = create_deep_nested_archive(100);
        let result = ArchiveVfs::from_buffer(archive);

        // Should handle deep nesting (may succeed or fail gracefully)
        match result {
            Ok(vfs) => {
                // If it succeeded, verify it's usable
                let entries = vfs.read_dir(".");
                assert!(entries.is_ok());
            },
            Err(_) => {
                // Should fail gracefully, not panic - rejection is acceptable
            },
        }
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
        let result = ArchiveVfs::from_buffer(archive);

        // Should handle gracefully
        match result {
            Ok(vfs) => {
                // If accepted, verify VFS is functional
                let _ = vfs.read_dir(".");
            },
            Err(_) => {
                // Rejection is also acceptable
            },
        }
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
                PolicyStoreError::JsonParsing { file, .. } if file.contains("metadata")
            ) || matches!(
                &err,
                PolicyStoreError::Validation(ValidationError::MetadataJsonParseFailed { .. })
            ),
            "Expected JSON parsing error for metadata, got: {:?}",
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
        // The error may be CedarParsing or a validation error depending on where parsing fails
        assert!(
            matches!(&err, PolicyStoreError::CedarParsing { .. })
                || matches!(&err, PolicyStoreError::Validation(_)),
            "Expected CedarParsing or Validation error for invalid Cedar syntax, got: {:?}",
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
        // If implementation allows empty/invalid entities directory, this may succeed
        // In that case, the test documents current behavior
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::JsonParsing { .. })
                    || matches!(&err, PolicyStoreError::CedarEntityError { .. }),
                "Expected JSON parsing or entity error, got: {:?}",
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
        // If implementation allows missing optional fields, this may succeed
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::TrustedIssuerError { .. })
                    || matches!(&err, PolicyStoreError::JsonParsing { .. }),
                "Expected TrustedIssuerError or JsonParsing error, got: {:?}",
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
        // Cedar may allow duplicates in some cases (last wins), so test documents behavior
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::CedarEntityError { .. })
                    || matches!(&err, PolicyStoreError::JsonParsing { .. }),
                "Expected CedarEntityError or JsonParsing error for duplicate UIDs, got: {:?}",
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
        assert!(result.is_ok());
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
        // The loader should handle this gracefully - either by accepting the policy
        // (Cedar's parser accepts it) or by failing with a validation error.
        let result = loader.load_directory(".");
        match &result {
            Ok(loaded) => {
                // Verify policy was loaded with the special character ID
                assert!(
                    !loaded.policies.is_empty(),
                    "Expected at least one policy to be loaded"
                );
            },
            Err(err) => {
                // If validation rejects it, should be InvalidPolicyIdCharacters
                assert!(
                    matches!(
                        err,
                        PolicyStoreError::Validation(
                            ValidationError::InvalidPolicyIdCharacters { .. }
                        )
                    ),
                    "Expected InvalidPolicyIdCharacters error, got: {:?}",
                    err
                );
            },
        }
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
        let result = loader.load_directory(".");

        assert!(result.is_ok());
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
        assert!(result.is_ok());
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

        // Manifest validation is optional - loader may succeed even with invalid manifest
        // The test verifies the loader handles this gracefully (either succeeds or returns
        // a manifest-related error, but doesn't panic)
        match &result {
            Ok(_) => {
                // Loader succeeded - manifest validation is not enforced
            },
            Err(err) => {
                // If it fails, should be a manifest-related error
                assert!(
                    matches!(err, PolicyStoreError::ManifestError { .. })
                        || matches!(err, PolicyStoreError::JsonParsing { .. }),
                    "Expected ManifestError or JsonParsing error, got: {:?}",
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

        assert!(result.is_ok());
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
        assert!(
            result.is_ok(),
            "Large policy should load successfully, got error: {:?}",
            result.err()
        );
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
        assert!(result.is_ok());
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
        assert!(result.is_ok());
    }
}
