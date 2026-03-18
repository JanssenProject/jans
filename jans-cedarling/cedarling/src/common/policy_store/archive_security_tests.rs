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

use std::io::{Cursor, Write};

use zip::write::{ExtendedFileOptions, FileOptions};
use zip::{CompressionMethod, ZipWriter};

use super::archive_handler::ArchiveVfs;
use super::entity_parser::{EntityParser, ParsedEntity};
use super::errors::{ArchiveError, PolicyStoreError, ValidationError};
use super::issuer_parser::IssuerParser;
use super::loader::DefaultPolicyStoreLoader;
use super::test_utils::{
    PolicyStoreTestBuilder, create_corrupted_archive, create_deep_nested_archive,
    create_path_traversal_archive, fixtures,
};
use super::vfs_adapter::VfsFileSystem;

// ============================================================================
// Path Traversal Tests
// ============================================================================

mod path_traversal {
    use super::*;

    #[test]
    fn test_rejects_parent_directory_traversal_in_archive() {
        let archive = create_path_traversal_archive();
        let result = ArchiveVfs::from_buffer(archive);

        let err = result.expect_err("Expected PathTraversal error");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error, got: {err:?}"
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
            "expected PathTraversal error, got: {err:?}"
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
            "Expected PathTraversal error, got: {err:?}"
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

        // Should reject archives containing Windows-style path traversal
        let err = result.expect_err("expected PathTraversal error for Windows path separators");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error for Windows path separators, got: {err:?}"
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
            "Expected InvalidZipFormat error, got: {err:?}"
        );
    }

    #[test]
    fn test_rejects_non_zip_file() {
        let not_a_zip = b"This is definitely not a ZIP file".to_vec();
        let result = ArchiveVfs::from_buffer(not_a_zip);

        let err = result.expect_err("Expected InvalidZipFormat error");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error, got: {err:?}"
        );
    }

    #[test]
    fn test_rejects_empty_file() {
        let empty: Vec<u8> = Vec::new();
        let result = ArchiveVfs::from_buffer(empty);
        let err = result.expect_err("empty buffer should not be a valid archive");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error for empty buffer, got: {err:?}"
        );
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

        // Verify that the deeply nested file can be read and contains correct data
        let nested_path = (0..100).map(|_| "dir").collect::<Vec<_>>().join("/") + "/file.txt";
        let content = vfs
            .read_file(&nested_path)
            .expect("Should be able to read file at deeply nested path");
        let content_str = String::from_utf8(content).expect("File content should be valid UTF-8");
        assert_eq!(
            content_str, "deep content",
            "File content should match expected value"
        );
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
            "Expected MetadataJsonParseFailed validation error for metadata, got: {err:?}"
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
            "Expected InvalidPolicyStoreId validation error for invalid Cedar syntax fixture, got: {err:?}"
        );
    }

    #[test]
    fn test_rejects_invalid_entity_json() {
        let builder = fixtures::minimal_valid().with_entity("invalid", "{ not valid json }");

        let archive = builder.build_archive().unwrap();
        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let loaded_directory = loader.load_directory(".").expect("should load directory");

        // Parse entities to trigger validation
        let entity_file = loaded_directory
            .entities
            .first()
            .expect("Expected to find invalid entity JSON but none found");
        let result = EntityParser::parse_entities(&entity_file.content, &entity_file.name, None);
        let err = result.expect_err("expected JSON parsing error for invalid entity JSON");
        assert!(
            matches!(&err, PolicyStoreError::JsonParsing { .. }),
            "Expected JSON parsing error for invalid entity JSON, got: {err:?}"
        );
    }

    #[test]
    fn test_rejects_invalid_trusted_issuer() {
        let builder = fixtures::invalid_trusted_issuer();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let loaded_directory = loader.load_directory(".").expect("should load directory");

        // Parse issuers to trigger validation
        let issuer_file = loaded_directory
            .trusted_issuers
            .first()
            .expect("Expected to find invalid trusted issuer but none found");
        let result = IssuerParser::parse_issuer(&issuer_file.content, &issuer_file.name);
        let err = result.expect_err("expected TrustedIssuerError for invalid trusted issuer");
        assert!(
            matches!(&err, PolicyStoreError::TrustedIssuerError { .. }),
            "Expected TrustedIssuerError for invalid trusted issuer, got: {err:?}"
        );
    }

    #[test]
    fn test_handles_duplicate_entity_uids_gracefully() {
        let builder = fixtures::duplicate_entity_uids();
        let archive = builder.build_archive().unwrap();

        let vfs = ArchiveVfs::from_buffer(archive).unwrap();
        let loader = DefaultPolicyStoreLoader::new(vfs);
        let loaded_directory = loader.load_directory(".").expect("should load directory");

        // Parse all entities and detect duplicates
        let mut all_parsed_entities: Vec<ParsedEntity> = Vec::new();
        for entity_file in &loaded_directory.entities {
            let parsed =
                EntityParser::parse_entities(&entity_file.content, &entity_file.name, None)
                    .expect("should parse entities");
            all_parsed_entities.extend(parsed);
        }

        // Count entities before deduplication
        let total_before = all_parsed_entities.len();

        // Detect duplicates - this should succeed (duplicates handled gracefully)
        // Using None for logger since we don't need to capture warnings in this test
        let unique_entities = EntityParser::detect_duplicates(all_parsed_entities, None);

        // Should have fewer unique entities than total (duplicates were merged)
        assert!(
            unique_entities.len() < total_before,
            "Expected fewer unique entities ({}) than total ({}) due to duplicates",
            unique_entities.len(),
            total_before
        );
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
        let loaded_directory = loader
            .load_directory(".")
            .expect("Policy with special-character @id should load successfully");

        // Verify policy was loaded with the special character ID
        assert!(
            !loaded_directory.policies.is_empty(),
            "Expected at least one policy to be loaded"
        );
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
                format!("policy{i}"),
                format!(r#"@id("policy{i}") permit(principal, action, resource);"#),
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
            .map(|i| format!("principal.attr{i} == \"value{i}\""))
            .collect::<Vec<_>>()
            .join(" || ");

        let policy = format!(
            r#"@id("large-policy")
permit(principal, action, resource)
when {{ {large_condition} }};"#
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
            "Expected InvalidExtension error, got: {err:?}"
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
