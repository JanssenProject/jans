// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Archive handling utilities for .cjar policy store archives.
//!
//! This module provides functionality to:
//! - Validate .cjar archive files
//! - Extract archives to temporary directories
//! - Detect and report archive corruption
//! - Manage temporary directory lifecycle with automatic cleanup

use super::errors::ArchiveError;
use std::fs::File;
use std::path::{Path, PathBuf};
use tempfile::TempDir;
use zip::ZipArchive;

/// Handler for .cjar archive extraction and validation.
///
/// Provides utilities to:
/// - Validate archive format and extension
/// - Extract archives to temporary directories
/// - Detect corruption and path traversal attempts
/// - Automatic cleanup of temporary directories
pub struct ArchiveHandler {
    /// Temporary directory for extraction (automatically cleaned up on drop)
    temp_dir: Option<TempDir>,
}

impl ArchiveHandler {
    /// Create a new archive handler.
    ///
    /// The handler does not create a temporary directory until extraction is performed.
    pub fn new() -> Self {
        Self { temp_dir: None }
    }

    /// Validate that a file is a valid .cjar archive.
    ///
    /// Checks:
    /// 1. File has .cjar extension
    /// 2. File exists and is readable
    /// 3. File is a valid ZIP archive
    /// 4. Archive is not corrupted
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - File extension is not .cjar
    /// - File cannot be opened
    /// - ZIP format is invalid
    /// - Archive is corrupted
    pub fn validate_archive(archive_path: &Path) -> Result<(), ArchiveError> {
        // Check extension
        if archive_path.extension().and_then(|s| s.to_str()) != Some("cjar") {
            return Err(ArchiveError::InvalidFormat {
                message: format!(
                    "File must have .cjar extension, found: {}",
                    archive_path
                        .extension()
                        .and_then(|s| s.to_str())
                        .unwrap_or("(none)")
                ),
            });
        }

        // Try to open as ZIP archive
        let file = File::open(archive_path).map_err(|e| ArchiveError::InvalidFormat {
            message: format!("Cannot open archive file: {}", e),
        })?;

        // Validate ZIP format
        let mut archive = ZipArchive::new(file).map_err(|e| ArchiveError::InvalidFormat {
            message: format!("Invalid ZIP format: {}", e),
        })?;

        // Basic corruption check - try to read file names
        for i in 0..archive.len() {
            let file = archive.by_index(i).map_err(|e| ArchiveError::Corrupted {
                message: format!("Cannot read file entry {}: {}", i, e),
            })?;

            // Check for path traversal attempts
            let file_path = file.name();
            if file_path.contains("..") || Path::new(file_path).is_absolute() {
                return Err(ArchiveError::PathTraversal {
                    path: file_path.to_string(),
                });
            }
        }

        Ok(())
    }

    /// Extract a .cjar archive to a temporary directory.
    ///
    /// The temporary directory is automatically cleaned up when this `ArchiveHandler`
    /// is dropped, unless `take_temp_dir()` is called to take ownership.
    ///
    /// # Returns
    ///
    /// Returns the path to the extracted contents.
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - Archive validation fails
    /// - Temporary directory creation fails
    /// - Extraction fails
    /// - Path traversal is detected
    pub fn extract_archive(&mut self, archive_path: &Path) -> Result<PathBuf, ArchiveError> {
        // Validate archive first
        Self::validate_archive(archive_path)?;

        // Create temporary directory
        let temp_dir = TempDir::new().map_err(|e| ArchiveError::ExtractionFailed {
            message: format!("Failed to create temporary directory: {}", e),
        })?;

        let temp_path = temp_dir.path().to_path_buf();

        // Open archive
        let file = File::open(archive_path).map_err(|e| ArchiveError::ExtractionFailed {
            message: format!("Failed to open archive: {}", e),
        })?;

        let mut archive = ZipArchive::new(file).map_err(|e| ArchiveError::ExtractionFailed {
            message: format!("Failed to read archive: {}", e),
        })?;

        // Extract all files
        for i in 0..archive.len() {
            let mut file = archive
                .by_index(i)
                .map_err(|e| ArchiveError::ExtractionFailed {
                    message: format!("Failed to read file entry {}: {}", i, e),
                })?;

            let file_path = file.name();

            // Security: prevent path traversal
            if file_path.contains("..") || Path::new(file_path).is_absolute() {
                return Err(ArchiveError::PathTraversal {
                    path: file_path.to_string(),
                });
            }

            let outpath = temp_path.join(file_path);

            if file.is_dir() {
                // Create directory
                std::fs::create_dir_all(&outpath).map_err(|e| ArchiveError::ExtractionFailed {
                    message: format!("Failed to create directory '{}': {}", outpath.display(), e),
                })?;
            } else {
                // Create parent directories if needed
                if let Some(parent) = outpath.parent() {
                    std::fs::create_dir_all(parent).map_err(|e| {
                        ArchiveError::ExtractionFailed {
                            message: format!(
                                "Failed to create parent directory '{}': {}",
                                parent.display(),
                                e
                            ),
                        }
                    })?;
                }

                // Extract file
                let mut outfile =
                    File::create(&outpath).map_err(|e| ArchiveError::ExtractionFailed {
                        message: format!("Failed to create file '{}': {}", outpath.display(), e),
                    })?;

                std::io::copy(&mut file, &mut outfile).map_err(|e| {
                    ArchiveError::ExtractionFailed {
                        message: format!("Failed to write file '{}': {}", outpath.display(), e),
                    }
                })?;
            }
        }

        // Store temp_dir to prevent premature cleanup
        self.temp_dir = Some(temp_dir);

        Ok(temp_path)
    }

    /// Get the path to the temporary directory if extraction has been performed.
    pub fn temp_path(&self) -> Option<PathBuf> {
        self.temp_dir.as_ref().map(|d| d.path().to_path_buf())
    }

    /// Take ownership of the temporary directory, preventing automatic cleanup.
    ///
    /// This is useful if you need the extracted files to persist beyond the lifetime
    /// of the ArchiveHandler. The caller becomes responsible for cleanup.
    ///
    /// Returns `None` if no extraction has been performed.
    pub fn take_temp_dir(mut self) -> Option<TempDir> {
        self.temp_dir.take()
    }
}

impl Default for ArchiveHandler {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use zip::CompressionMethod;
    use zip::write::{ExtendedFileOptions, FileOptions};

    /// Helper to create a test .cjar archive
    fn create_test_cjar(
        path: &Path,
        files: Vec<(&str, &str)>,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let file = File::create(path)?;
        let mut zip = zip::ZipWriter::new(file);

        let options = FileOptions::<ExtendedFileOptions>::default()
            .compression_method(CompressionMethod::Deflated);

        for (name, content) in files {
            zip.start_file(name, options.clone())?;
            zip.write_all(content.as_bytes())?;
        }

        zip.finish()?;
        Ok(())
    }

    #[test]
    fn test_validate_archive_invalid_extension() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.zip");
        File::create(&archive_path).unwrap();

        let result = ArchiveHandler::validate_archive(&archive_path);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ArchiveError::InvalidFormat { .. }
        ));
    }

    #[test]
    fn test_validate_archive_not_zip() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        // Create a file that's not a ZIP
        let mut file = File::create(&archive_path).unwrap();
        file.write_all(b"This is not a ZIP file").unwrap();

        let result = ArchiveHandler::validate_archive(&archive_path);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ArchiveError::InvalidFormat { .. }
        ));
    }

    #[test]
    fn test_validate_archive_valid_cjar() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        create_test_cjar(&archive_path, vec![("metadata.json", "{}")]).unwrap();

        let result = ArchiveHandler::validate_archive(&archive_path);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_archive_path_traversal() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("malicious.cjar");

        // Create archive with path traversal attempt
        create_test_cjar(&archive_path, vec![("../../../etc/passwd", "malicious")]).unwrap();

        let result = ArchiveHandler::validate_archive(&archive_path);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ArchiveError::PathTraversal { .. }
        ));
    }

    #[test]
    fn test_extract_archive_success() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        create_test_cjar(
            &archive_path,
            vec![
                ("metadata.json", r#"{"version": "1.0"}"#),
                ("schema.cedarschema", "namespace Test;"),
                (
                    "policies/policy1.cedar",
                    "permit(principal, action, resource);",
                ),
            ],
        )
        .unwrap();

        let mut handler = ArchiveHandler::new();
        let extracted_path = handler.extract_archive(&archive_path).unwrap();

        // Verify files exist
        assert!(extracted_path.join("metadata.json").exists());
        assert!(extracted_path.join("schema.cedarschema").exists());
        assert!(extracted_path.join("policies/policy1.cedar").exists());

        // Verify content
        let metadata = std::fs::read_to_string(extracted_path.join("metadata.json")).unwrap();
        assert!(metadata.contains("1.0"));
    }

    #[test]
    fn test_extract_archive_with_directories() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        create_test_cjar(
            &archive_path,
            vec![
                ("metadata.json", "{}"),
                ("policies/policy1.cedar", "policy1"),
                ("policies/policy2.cedar", "policy2"),
                ("entities/users.json", "[]"),
            ],
        )
        .unwrap();

        let mut handler = ArchiveHandler::new();
        let extracted_path = handler.extract_archive(&archive_path).unwrap();

        // Verify directory structure
        assert!(extracted_path.join("policies").is_dir());
        assert!(extracted_path.join("entities").is_dir());
        assert!(extracted_path.join("policies/policy1.cedar").exists());
        assert!(extracted_path.join("policies/policy2.cedar").exists());
        assert!(extracted_path.join("entities/users.json").exists());
    }

    #[test]
    fn test_extract_archive_blocks_path_traversal() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("malicious.cjar");

        create_test_cjar(&archive_path, vec![("../../../etc/passwd", "malicious")]).unwrap();

        let mut handler = ArchiveHandler::new();
        let result = handler.extract_archive(&archive_path);

        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ArchiveError::PathTraversal { .. }
        ));
    }

    #[test]
    fn test_temp_dir_cleanup() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        create_test_cjar(&archive_path, vec![("metadata.json", "{}")]).unwrap();

        let extracted_path = {
            let mut handler = ArchiveHandler::new();
            let path = handler.extract_archive(&archive_path).unwrap();
            assert!(path.exists());
            path
        }; // handler dropped here

        // Temporary directory should be cleaned up
        assert!(!extracted_path.exists());
    }

    #[test]
    fn test_take_temp_dir_prevents_cleanup() {
        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        create_test_cjar(&archive_path, vec![("metadata.json", "{}")]).unwrap();

        let mut handler = ArchiveHandler::new();
        let _extracted_path = handler.extract_archive(&archive_path).unwrap();

        let temp_dir_handle = handler.take_temp_dir().unwrap();
        let temp_path = temp_dir_handle.path().to_path_buf();

        // Directory still exists because we took ownership
        assert!(temp_path.exists());

        // Clean up manually
        drop(temp_dir_handle);
        assert!(!temp_path.exists());
    }
}
