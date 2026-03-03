// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Manifest-based integrity validation for policy stores.
//!
//! This module provides functionality to validate the integrity of a policy store
//! using a manifest file that contains SHA-256 checksums for all files.

// This file is not compiled for WebAssembly targets.
// Because manifest validation relies on filesystem access which is not available in WASM.
#![cfg(not(target_arch = "wasm32"))]

use std::collections::HashSet;
use std::path::PathBuf;

use sha2::{Digest, Sha256};

use super::errors::{ManifestErrorType, PolicyStoreError};
use super::metadata::PolicyStoreManifest;
use super::vfs_adapter::VfsFileSystem;

/// Result of manifest validation with detailed information.
#[derive(Debug, Clone, PartialEq)]
pub(super) struct ManifestValidationResult {
    /// Whether validation passed (all required checks passed)
    pub is_valid: bool,
    /// Files that passed validation
    pub validated_files: Vec<String>,
    /// Files found in policy store but not listed in manifest (warnings)
    pub unlisted_files: Vec<String>,
    /// Errors encountered during validation
    pub errors: Vec<ManifestValidationError>,
}

/// Detailed error information for manifest validation failures.
#[derive(Debug, Clone, PartialEq)]
pub(super) struct ManifestValidationError {
    /// Type of error
    pub error_type: ManifestErrorType,
    /// File path related to the error (if applicable)
    pub file: Option<String>,
}

impl ManifestValidationResult {
    /// Create a new validation result.
    fn new() -> Self {
        Self {
            is_valid: true,
            validated_files: Vec::new(),
            unlisted_files: Vec::new(),
            errors: Vec::new(),
        }
    }

    /// Add an error to the validation result and mark as invalid.
    fn add_error(&mut self, error_type: ManifestErrorType, file: Option<String>) {
        self.is_valid = false;
        self.errors
            .push(ManifestValidationError { error_type, file });
    }

    /// Add a validated file.
    fn add_validated_file(&mut self, file: String) {
        self.validated_files.push(file);
    }

    /// Add an unlisted file (warning).
    fn add_unlisted_file(&mut self, file: String) {
        self.unlisted_files.push(file);
    }
}

impl Default for ManifestValidationResult {
    fn default() -> Self {
        Self::new()
    }
}

/// Manifest validator for policy store integrity validation.
pub(super) struct ManifestValidator<V: VfsFileSystem> {
    vfs: V,
    base_path: PathBuf,
}

impl<V: VfsFileSystem> ManifestValidator<V> {
    /// Create a new manifest validator.
    pub(super) fn new(vfs: V, base_path: PathBuf) -> Self {
        Self { vfs, base_path }
    }

    /// Load and parse the manifest file.
    pub(super) fn load_manifest(&self) -> Result<PolicyStoreManifest, PolicyStoreError> {
        let manifest_path = format!("{}/manifest.json", self.base_path.display());

        // Check if manifest exists
        if !self.vfs.exists(&manifest_path) {
            return Err(PolicyStoreError::ManifestError {
                err: ManifestErrorType::ManifestNotFound,
            });
        }

        // Read manifest content
        let content_bytes =
            self.vfs
                .read_file(&manifest_path)
                .map_err(|e| PolicyStoreError::FileReadError {
                    path: manifest_path.clone(),
                    source: e,
                })?;

        let content =
            String::from_utf8(content_bytes).map_err(|e| PolicyStoreError::FileReadError {
                path: manifest_path.clone(),
                source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
            })?;

        // Parse manifest JSON
        let manifest: PolicyStoreManifest =
            serde_json::from_str(&content).map_err(|e| PolicyStoreError::ManifestError {
                err: ManifestErrorType::ParseError(e.to_string()),
            })?;

        Ok(manifest)
    }

    /// Compute SHA-256 checksum for a file.
    ///
    /// Useful for manifest generation and file integrity verification in tests and tooling.
    #[cfg(test)]
    pub(super) fn compute_checksum(&self, file_path: &str) -> Result<String, PolicyStoreError> {
        let content_bytes =
            self.vfs
                .read_file(file_path)
                .map_err(|e| PolicyStoreError::FileReadError {
                    path: file_path.to_string(),
                    source: e,
                })?;

        let mut hasher = Sha256::new();
        hasher.update(&content_bytes);
        let result = hasher.finalize();
        Ok(format!("sha256:{}", hex::encode(result)))
    }

    /// Validate a single file against manifest entry.
    fn validate_file(
        &self,
        relative_path: &str,
        expected_checksum: &str,
        expected_size: u64,
    ) -> Result<(), ManifestErrorType> {
        let file_path = format!("{}/{}", self.base_path.display(), relative_path);

        // Check if file exists
        if !self.vfs.exists(&file_path) {
            return Err(ManifestErrorType::FileMissing {
                file: relative_path.to_string(),
            });
        }

        // Validate checksum format
        if !expected_checksum.starts_with("sha256:") {
            return Err(ManifestErrorType::InvalidChecksumFormat {
                file: relative_path.to_string(),
                checksum: expected_checksum.to_string(),
            });
        }

        // Read file content for size and checksum validation
        let content_bytes =
            self.vfs
                .read_file(&file_path)
                .map_err(|e| ManifestErrorType::FileReadError {
                    file: relative_path.to_string(),
                    error_message: format!("{e}"),
                })?;

        // Validate file size
        let actual_size = content_bytes.len() as u64;
        if actual_size != expected_size {
            return Err(ManifestErrorType::SizeMismatch {
                file: relative_path.to_string(),
                expected: expected_size,
                actual: actual_size,
            });
        }

        // Compute checksum from already-read content
        let mut hasher = Sha256::new();
        hasher.update(&content_bytes);
        let result = hasher.finalize();
        let actual_checksum = format!("sha256:{}", hex::encode(result));

        if actual_checksum != expected_checksum {
            return Err(ManifestErrorType::ChecksumMismatch {
                file: relative_path.to_string(),
                expected: expected_checksum.to_string(),
                actual: actual_checksum,
            });
        }

        Ok(())
    }

    /// Find all files in the policy store (excluding manifest.json).
    fn find_all_files(&self) -> Result<HashSet<String>, PolicyStoreError> {
        let mut files = HashSet::new();

        // Define directories to scan
        let dirs = vec![
            "policies",
            "templates",
            "schemas",
            "entities",
            "trusted-issuers",
        ];

        for dir in dirs {
            let dir_path = format!("{}/{}", self.base_path.display(), dir);
            if self.vfs.exists(&dir_path) && self.vfs.is_dir(&dir_path) {
                self.scan_directory(&dir_path, dir, &mut files)?;
            }
        }

        // Add metadata.json if it exists
        let metadata_path = format!("{}/metadata.json", self.base_path.display());
        if self.vfs.exists(&metadata_path) {
            files.insert("metadata.json".to_string());
        }

        // Add schema.cedarschema if it exists
        let schema_path = format!("{}/schema.cedarschema", self.base_path.display());
        if self.vfs.exists(&schema_path) {
            files.insert("schema.cedarschema".to_string());
        }

        Ok(files)
    }

    /// Recursively scan a directory for files.
    fn scan_directory(
        &self,
        dir_path: &str,
        relative_base: &str,
        files: &mut HashSet<String>,
    ) -> Result<(), PolicyStoreError> {
        let entries =
            self.vfs
                .read_dir(dir_path)
                .map_err(|e| PolicyStoreError::DirectoryReadError {
                    path: dir_path.to_string(),
                    source: e,
                })?;

        for entry in entries {
            let path = &entry.path;
            let file_name = &entry.name;

            if self.vfs.is_file(path) {
                let relative_path = format!("{relative_base}/{file_name}");
                files.insert(relative_path);
            } else if self.vfs.is_dir(path) {
                let new_relative_base = format!("{relative_base}/{file_name}");
                self.scan_directory(path, &new_relative_base, files)?;
            }
        }

        Ok(())
    }

    /// Validate the entire policy store against the manifest.
    pub(crate) fn validate(&self, metadata_id: Option<&str>) -> ManifestValidationResult {
        let mut result = ManifestValidationResult::new();

        // Load manifest
        let manifest = match self.load_manifest() {
            Ok(m) => m,
            Err(PolicyStoreError::ManifestError { err }) => {
                result.add_error(err, None);
                return result;
            },
            Err(e) => {
                result.add_error(
                    ManifestErrorType::ParseError(e.to_string()),
                    Some("manifest.json".to_string()),
                );
                return result;
            },
        };

        // Validate policy store ID if metadata is provided
        if let Some(metadata_id) = metadata_id
            && manifest.policy_store_id != metadata_id
        {
            result.add_error(
                ManifestErrorType::PolicyStoreIdMismatch {
                    expected: manifest.policy_store_id.clone(),
                    actual: metadata_id.to_string(),
                },
                None,
            );
        }

        // Validate each file in manifest
        for (file_path, file_info) in &manifest.files {
            match self.validate_file(file_path, &file_info.checksum, file_info.size) {
                Ok(()) => {
                    result.add_validated_file(file_path.clone());
                },
                Err(err) => {
                    result.add_error(err, Some(file_path.clone()));
                },
            }
        }

        // Find unlisted files (files in policy store but not in manifest)
        match self.find_all_files() {
            Ok(all_files) => {
                let manifest_files: HashSet<String> = manifest.files.keys().cloned().collect();
                for file in all_files {
                    if !manifest_files.contains(&file) {
                        result.add_unlisted_file(file);
                    }
                }
            },
            Err(e) => {
                result.add_error(
                    ManifestErrorType::ParseError(format!("Failed to scan files: {e}")),
                    None,
                );
            },
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::super::metadata::FileInfo;
    use super::super::vfs_adapter::MemoryVfs;
    use super::*;
    use chrono::Utc;
    use std::collections::HashMap;

    #[test]
    fn test_compute_checksum() {
        let vfs = MemoryVfs::new();
        vfs.create_file("/test.txt", b"hello world")
            .expect("should create test file");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let checksum = validator
            .compute_checksum("/test.txt")
            .expect("should compute checksum");

        // Expected SHA-256 of "hello world"
        assert_eq!(
            checksum,
            "sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        );
    }

    #[test]
    fn test_load_manifest_not_found() {
        let vfs = MemoryVfs::new();
        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));

        let result = validator.load_manifest();
        assert!(matches!(
            result.expect_err("should fail when manifest not found"),
            PolicyStoreError::ManifestError {
                err: ManifestErrorType::ManifestNotFound
            }
        ));
    }

    #[test]
    fn test_load_manifest_success() {
        let vfs = MemoryVfs::new();

        let manifest_json = r#"{
            "policy_store_id": "test123",
            "generated_date": "2024-01-01T12:00:00Z",
            "files": {
                "metadata.json": {
                    "size": 100,
                    "checksum": "sha256:abc123"
                }
            }
        }"#;

        vfs.create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let manifest = validator.load_manifest().expect("should succeed");

        assert_eq!(manifest.policy_store_id, "test123");
        assert_eq!(manifest.files.len(), 1);
    }

    #[test]
    fn test_validate_file_missing() {
        let vfs = MemoryVfs::new();
        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));

        let result = validator.validate_file("missing.txt", "sha256:abc", 100);
        let err = result.expect_err("Expected FileMissing error for nonexistent file");
        assert!(
            matches!(err, ManifestErrorType::FileMissing { .. }),
            "Expected FileMissing error, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_file_invalid_checksum_format() {
        let vfs = MemoryVfs::new();
        vfs.create_file("/test.txt", b"hello")
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate_file("test.txt", "invalid_format", 5);

        let err = result.expect_err("Expected InvalidChecksumFormat error");
        assert!(
            matches!(err, ManifestErrorType::InvalidChecksumFormat { .. }),
            "Expected InvalidChecksumFormat error, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_file_size_mismatch() {
        let vfs = MemoryVfs::new();
        vfs.create_file("/test.txt", b"hello")
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate_file("test.txt", "sha256:abc", 100); // Wrong size

        let err = result.expect_err("Expected SizeMismatch error");
        assert!(
            matches!(err, ManifestErrorType::SizeMismatch { .. }),
            "Expected SizeMismatch error, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_file_checksum_mismatch() {
        let vfs = MemoryVfs::new();
        vfs.create_file("/test.txt", b"hello")
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate_file("test.txt", "sha256:wrongchecksum", 5);

        let err = result.expect_err("Expected ChecksumMismatch error");
        assert!(
            matches!(err, ManifestErrorType::ChecksumMismatch { .. }),
            "Expected ChecksumMismatch error, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_file_success() {
        let vfs = MemoryVfs::new();
        let content = b"hello world";
        vfs.create_file("/test.txt", content)
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));

        // Compute correct checksum
        let checksum = validator
            .compute_checksum("/test.txt")
            .expect("should succeed");

        let result = validator.validate_file("test.txt", &checksum, content.len() as u64);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_complete_policy_store_success() {
        let vfs = MemoryVfs::new();

        // Create metadata
        let metadata_content = b"{\"test\": \"data\"}";
        vfs.create_file("/metadata.json", metadata_content)
            .expect("should succeed");

        // Create policy
        let policy_content = b"permit(principal, action, resource);";
        vfs.create_file("/policies/policy1.cedar", policy_content)
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));

        // Compute checksums
        let metadata_checksum = validator
            .compute_checksum("/metadata.json")
            .expect("should succeed");
        let policy_checksum = validator
            .compute_checksum("/policies/policy1.cedar")
            .expect("should succeed");

        // Create manifest
        let mut files = HashMap::new();
        files.insert(
            "metadata.json".to_string(),
            FileInfo {
                size: metadata_content.len() as u64,
                checksum: metadata_checksum,
            },
        );
        files.insert(
            "policies/policy1.cedar".to_string(),
            FileInfo {
                size: policy_content.len() as u64,
                checksum: policy_checksum,
            },
        );

        let manifest = PolicyStoreManifest {
            policy_store_id: "test123".to_string(),
            generated_date: Utc::now(),
            files,
        };

        let manifest_json = serde_json::to_string(&manifest).expect("should succeed");
        validator
            .vfs
            .create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        // Validate
        let result = validator.validate(Some("test123"));
        assert!(result.is_valid);
        assert_eq!(result.validated_files.len(), 2);
        assert_eq!(result.errors.len(), 0);
    }

    #[test]
    fn test_validate_with_unlisted_files() {
        let vfs = MemoryVfs::new();

        // Create files
        let metadata_content = b"{\"test\": \"data\"}";
        let policy_content = b"permit(principal, action, resource);";

        vfs.create_file("/metadata.json", metadata_content)
            .expect("should succeed");
        vfs.create_file("/policies/policy1.cedar", policy_content)
            .expect("should succeed");
        vfs.create_file("/policies/extra_policy.cedar", policy_content)
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));

        // Create manifest with only metadata.json and policy1.cedar
        let metadata_checksum = validator
            .compute_checksum("/metadata.json")
            .expect("should succeed");
        let policy_checksum = validator
            .compute_checksum("/policies/policy1.cedar")
            .expect("should succeed");

        let mut files = HashMap::new();
        files.insert(
            "metadata.json".to_string(),
            FileInfo {
                size: metadata_content.len() as u64,
                checksum: metadata_checksum,
            },
        );
        files.insert(
            "policies/policy1.cedar".to_string(),
            FileInfo {
                size: policy_content.len() as u64,
                checksum: policy_checksum,
            },
        );

        let manifest = PolicyStoreManifest {
            policy_store_id: "test123".to_string(),
            generated_date: Utc::now(),
            files,
        };

        let manifest_json = serde_json::to_string(&manifest).expect("should succeed");
        validator
            .vfs
            .create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        // Validate
        let result = validator.validate(None);

        assert!(result.is_valid); // Still valid, but with warnings
        assert_eq!(result.validated_files.len(), 2);
        assert_eq!(result.unlisted_files.len(), 1);
        assert!(
            result
                .unlisted_files
                .contains(&"policies/extra_policy.cedar".to_string())
        );
    }

    #[test]
    fn test_validate_policy_store_id_mismatch() {
        let vfs = MemoryVfs::new();

        let manifest = PolicyStoreManifest {
            policy_store_id: "expected_id".to_string(),
            generated_date: Utc::now(),
            files: HashMap::new(),
        };

        let manifest_json = serde_json::to_string(&manifest).expect("should succeed");
        vfs.create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate(Some("wrong_id"));

        assert!(!result.is_valid);
        assert!(result.errors.iter().any(|e| matches!(
            &e.error_type,
            ManifestErrorType::PolicyStoreIdMismatch { .. }
        )));
    }

    #[test]
    fn test_validate_with_missing_file() {
        let vfs = MemoryVfs::new();

        // Create manifest with a file that doesn't exist
        let mut files = HashMap::new();
        files.insert(
            "missing.txt".to_string(),
            FileInfo {
                size: 100,
                checksum: "sha256:abc123".to_string(),
            },
        );

        let manifest = PolicyStoreManifest {
            policy_store_id: "test123".to_string(),
            generated_date: Utc::now(),
            files,
        };

        let manifest_json = serde_json::to_string(&manifest).expect("should succeed");
        vfs.create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate(None);

        assert!(!result.is_valid);
        assert!(
            result
                .errors
                .iter()
                .any(|e| matches!(&e.error_type, ManifestErrorType::FileMissing { .. }))
        );
    }

    #[test]
    fn test_validate_with_checksum_mismatch() {
        let vfs = MemoryVfs::new();

        vfs.create_file("/test.txt", b"actual content")
            .expect("should succeed");

        // Create manifest with wrong checksum
        let mut files = HashMap::new();
        files.insert(
            "test.txt".to_string(),
            FileInfo {
                size: 14,
                checksum: "sha256:wrongchecksum".to_string(),
            },
        );

        let manifest = PolicyStoreManifest {
            policy_store_id: "test123".to_string(),
            generated_date: Utc::now(),
            files,
        };

        let manifest_json = serde_json::to_string(&manifest).expect("should succeed");
        vfs.create_file("/manifest.json", manifest_json.as_bytes())
            .expect("should succeed");

        let validator = ManifestValidator::new(vfs, PathBuf::from("/"));
        let result = validator.validate(None);

        assert!(!result.is_valid);
        assert!(
            result
                .errors
                .iter()
                .any(|e| matches!(&e.error_type, ManifestErrorType::ChecksumMismatch { .. }))
        );
    }
}
