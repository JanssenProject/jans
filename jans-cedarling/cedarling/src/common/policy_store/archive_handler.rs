// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Archive VFS implementation for .cjar policy store archives.
//!
//! This module provides a VFS implementation backed by ZIP archives, enabling
//! policy stores to be distributed as single `.cjar` files. The implementation:
//!
//! - **Fully WASM-compatible** - `from_buffer()` works in both native and WASM
//! - Reads files on-demand from the archive (no extraction needed)
//! - Validates archive format and structure during construction
//! - Prevents path traversal attacks
//! - Provides full VfsFileSystem trait implementation
//!
//! # WASM Support
//!
//! Archives are **fully supported in WASM**:
//! - Use `ArchiveVfs::from_buffer()` with bytes you fetch (works now)
//! - Use `ArchiveSource::Url` with `load_policy_store()` (once URL fetching is implemented)
//! - Only `from_file()` is native-only (requires file system access)
use super::errors::ArchiveError;
use super::vfs_adapter::{DirEntry, VfsFileSystem};
use std::io::{Cursor, Read, Seek};
use std::path::Path;
use std::sync::Mutex;
use zip::ZipArchive;

/// VFS implementation backed by a ZIP archive.
///
/// This implementation reads files on-demand from a ZIP archive without extraction,
/// making it efficient and WASM-compatible. The archive is validated during construction
/// to ensure it's a valid .cjar file with no path traversal attempts.
///
/// # Thread Safety
///
/// This type is `Send + Sync` despite using `Mutex` because the `ZipArchive` is protected
/// by a mutex. Concurrent access is prevented by the Mutex locking mechanism.
///
/// # Generic Type Parameter
///
/// The generic type `T` must implement `Read + Seek` and represents the underlying
/// reader for the ZIP archive. Common types:
/// - `Cursor<Vec<u8>>` - For in-memory archives (WASM-compatible)
/// - `std::fs::File` - For file-based archives (native only)
#[derive(Debug)]
pub struct ArchiveVfs<T> {
    /// The ZIP archive reader (wrapped in Mutex for thread safety)
    archive: Mutex<ZipArchive<T>>,
}

impl<T> ArchiveVfs<T>
where
    T: Read + Seek,
{
    /// Create an ArchiveVfs from a reader.
    ///
    /// This method:
    /// 1. Validates the reader contains a valid ZIP archive
    /// 2. Checks for path traversal attempts
    /// 3. Validates archive structure
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - Reader does not contain a valid ZIP archive
    /// - Archive contains path traversal attempts
    /// - Archive is corrupted
    pub fn from_reader(reader: T) -> Result<Self, ArchiveError> {
        let mut archive = ZipArchive::new(reader).map_err(|e| ArchiveError::InvalidZipFormat {
            details: e.to_string(),
        })?;

        // Validate all file names for security
        for i in 0..archive.len() {
            let file = archive
                .by_index(i)
                .map_err(|e| ArchiveError::CorruptedEntry {
                    index: i,
                    details: e.to_string(),
                })?;

            let file_path = file.name();

            // Check for path traversal attempts
            if file_path.contains("..") || Path::new(file_path).is_absolute() {
                return Err(ArchiveError::PathTraversal {
                    path: file_path.to_string(),
                });
            }
        }

        Ok(Self {
            archive: Mutex::new(archive),
        })
    }
}

impl ArchiveVfs<std::fs::File> {
    /// Create an ArchiveVfs from a file path (native only).
    ///
    /// This method:
    /// 1. Validates the file has .cjar extension
    /// 2. Opens the file
    /// 3. Validates it's a valid ZIP archive
    /// 4. Checks for path traversal attempts
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - File extension is not .cjar
    /// - File cannot be read
    /// - Archive is not a valid ZIP
    /// - Archive contains path traversal attempts
    /// - Archive is corrupted
    pub fn from_file<P: AsRef<Path>>(path: P) -> Result<Self, ArchiveError> {
        let path = path.as_ref();

        // Validate extension
        if path.extension().and_then(|s| s.to_str()) != Some("cjar") {
            return Err(ArchiveError::InvalidExtension {
                expected: "cjar".to_string(),
                found: path
                    .extension()
                    .and_then(|s| s.to_str())
                    .unwrap_or("(none)")
                    .to_string(),
            });
        }

        let file = std::fs::File::open(path).map_err(|e| ArchiveError::CannotReadFile {
            path: path.display().to_string(),
            source: e,
        })?;

        Self::from_reader(file)
    }
}

impl ArchiveVfs<Cursor<Vec<u8>>> {
    /// Create an ArchiveVfs from bytes (works in WASM and native).
    ///
    /// This method:
    /// 1. Validates the bytes form a valid ZIP archive
    /// 2. Checks for path traversal attempts
    /// 3. Validates archive structure
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - Bytes are not a valid ZIP archive
    /// - Archive contains path traversal attempts
    /// - Archive is corrupted
    pub fn from_buffer(buffer: Vec<u8>) -> Result<Self, ArchiveError> {
        let cursor = Cursor::new(buffer);
        Self::from_reader(cursor)
    }
}

impl<T> ArchiveVfs<T>
where
    T: Read + Seek,
{
    /// Normalize a path for archive lookup.
    ///
    /// Handles:
    /// - Converting absolute paths to relative
    /// - Removing leading slashes
    /// - Converting "." to ""
    /// - Normalizing path separators
    fn normalize_path(&self, path: &str) -> String {
        let path = path.trim_start_matches('/');
        if path == "." || path.is_empty() {
            String::new()
        } else {
            path.to_string()
        }
    }

    /// Check if a path exists in the archive (file or directory).
    fn path_exists(&self, path: &str) -> bool {
        let normalized = self.normalize_path(path);

        let mut archive = self.archive.lock().expect("mutex poisoned");

        // Check if it's a file
        if archive.by_name(&normalized).is_ok() {
            return true;
        }

        // Check if it's a directory by looking for entries that start with this prefix
        let dir_prefix = if normalized.is_empty() {
            String::new()
        } else {
            format!("{}/", normalized)
        };

        for i in 0..archive.len() {
            if let Ok(file) = archive.by_index(i) {
                let file_name = file.name();
                if file_name == normalized || file_name.starts_with(&dir_prefix) {
                    return true;
                }
            }
        }

        false
    }

    /// Check if a path is a directory in the archive.
    fn is_directory(&self, path: &str) -> bool {
        let normalized = self.normalize_path(path);
        let mut archive = self.archive.lock().expect("mutex poisoned");
        Self::is_directory_locked(&mut archive, &normalized)
    }

    /// Check if a path is a directory (with already-locked archive).
    /// This is a helper to avoid deadlocks when called from methods that already hold the lock.
    fn is_directory_locked(archive: &mut ZipArchive<T>, normalized: &str) -> bool {
        // Root is always a directory
        if normalized.is_empty() {
            return true;
        }

        // Check if there's an explicit directory entry
        let dir_path_with_slash = format!("{}/", normalized);
        if let Ok(file) = archive.by_name(&dir_path_with_slash) {
            return file.is_dir();
        }

        // Check if any files have this as a prefix (implicit directory)
        for i in 0..archive.len() {
            if let Ok(file) = archive.by_index(i) {
                let file_name = file.name();
                if file_name.starts_with(&format!("{}/", normalized)) {
                    return true;
                }
            }
        }

        false
    }
}

impl<T> VfsFileSystem for ArchiveVfs<T>
where
    T: Read + Seek + Send + Sync + 'static,
{
    fn read_file(&self, path: &str) -> Result<Vec<u8>, std::io::Error> {
        let normalized = self.normalize_path(path);

        let mut archive = self.archive.lock().expect("mutex poisoned");

        let mut file = archive.by_name(&normalized).map_err(|e| {
            std::io::Error::new(
                std::io::ErrorKind::NotFound,
                format!("File not found in archive: {}: {}", path, e),
            )
        })?;

        let mut contents = Vec::new();
        file.read_to_end(&mut contents)?;

        Ok(contents)
    }

    fn exists(&self, path: &str) -> bool {
        self.path_exists(path)
    }

    fn is_dir(&self, path: &str) -> bool {
        self.is_directory(path)
    }

    fn is_file(&self, path: &str) -> bool {
        let normalized = self.normalize_path(path);
        let mut archive = self.archive.lock().expect("mutex poisoned");

        if let Ok(file) = archive.by_name(&normalized) {
            return file.is_file();
        }

        false
    }

    fn read_dir(&self, path: &str) -> Result<Vec<DirEntry>, std::io::Error> {
        let normalized = self.normalize_path(path);
        let prefix = if normalized.is_empty() {
            String::new()
        } else {
            format!("{}/", normalized)
        };

        let mut archive = self.archive.lock().expect("mutex poisoned");
        let mut seen = std::collections::HashSet::new();
        let mut entry_paths = Vec::new();

        // First pass: collect all unique entry paths
        for i in 0..archive.len() {
            let file = archive.by_index(i).map_err(|e| {
                std::io::Error::other(format!("Failed to read archive entry {}: {}", i, e))
            })?;

            let file_name = file.name();

            // Check if this file is in the requested directory
            if file_name.starts_with(&prefix) || (prefix.is_empty() && !file_name.contains('/')) {
                let relative = if prefix.is_empty() {
                    file_name
                } else {
                    &file_name[prefix.len()..]
                };

                // Get the immediate child name (first component)
                let child_name = if let Some(slash_pos) = relative.find('/') {
                    &relative[..slash_pos]
                } else {
                    relative
                };

                // Skip empty names and deduplicate
                if child_name.is_empty() || !seen.insert(child_name.to_string()) {
                    continue;
                }

                // Determine the full path for this entry
                let entry_path = if prefix.is_empty() {
                    child_name.to_string()
                } else {
                    format!("{}{}", prefix, child_name)
                };

                entry_paths.push((child_name.to_string(), entry_path));
            }
        }

        // Second pass: check if each path is a directory
        let mut entries = Vec::new();
        for (name, entry_path) in entry_paths {
            let entry_path_normalized = self.normalize_path(&entry_path);
            let is_directory = Self::is_directory_locked(&mut archive, &entry_path_normalized);

            entries.push(DirEntry {
                name,
                path: entry_path,
                is_dir: is_directory,
            });
        }

        Ok(entries)
    }

    fn open_file(&self, path: &str) -> Result<Box<dyn Read + Send>, std::io::Error> {
        let bytes = self.read_file(path)?;
        Ok(Box::new(Cursor::new(bytes)))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use zip::CompressionMethod;
    use zip::write::{ExtendedFileOptions, FileOptions};

    /// Helper to create a test .cjar archive in memory
    fn create_test_archive(files: Vec<(&str, &str)>) -> Vec<u8> {
        let mut buffer = Vec::new();
        {
            let cursor = Cursor::new(&mut buffer);
            let mut zip = zip::ZipWriter::new(cursor);

            for (name, content) in files {
                let options = FileOptions::<ExtendedFileOptions>::default()
                    .compression_method(CompressionMethod::Deflated);
                zip.start_file(name, options).unwrap();
                zip.write_all(content.as_bytes()).unwrap();
            }

            zip.finish().unwrap();
        }
        buffer
    }

    #[test]
    fn test_from_buffer_valid_archive() {
        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        let _result = ArchiveVfs::from_buffer(bytes)
            .expect("expect ArchiveVfs initialized correctly from buffer");
    }

    #[test]
    fn test_from_buffer_invalid_zip() {
        let bytes = b"This is not a ZIP file".to_vec();
        let result = ArchiveVfs::from_buffer(bytes);
        let err = result.expect_err("Expected InvalidZipFormat error for non-ZIP data");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_from_buffer_path_traversal() {
        let bytes = create_test_archive(vec![("../../../etc/passwd", "malicious")]);
        let result = ArchiveVfs::from_buffer(bytes);
        let err = result.expect_err("Expected PathTraversal error for malicious path");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_read_file_success() {
        let bytes = create_test_archive(vec![
            ("metadata.json", r#"{"version":"1.0"}"#),
            ("schema.cedarschema", "namespace Test;"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        let content = vfs.read_file("metadata.json").unwrap();
        assert_eq!(String::from_utf8(content).unwrap(), r#"{"version":"1.0"}"#);

        let content = vfs.read_file("schema.cedarschema").unwrap();
        assert_eq!(String::from_utf8(content).unwrap(), "namespace Test;");
    }

    #[test]
    fn test_read_file_not_found() {
        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        let result = vfs.read_file("nonexistent.json");
        let err = result.expect_err("Expected error for nonexistent file");
        assert!(
            matches!(err, std::io::Error { .. }),
            "Expected IO error for file not found"
        );
    }

    #[test]
    fn test_exists() {
        let bytes = create_test_archive(vec![
            ("metadata.json", "{}"),
            ("policies/policy1.cedar", "permit();"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        assert!(vfs.exists("metadata.json"));
        assert!(vfs.exists("policies/policy1.cedar"));
        assert!(vfs.exists("policies")); // directory
        assert!(!vfs.exists("nonexistent.json"));
    }

    #[test]
    fn test_is_file() {
        let bytes = create_test_archive(vec![
            ("metadata.json", "{}"),
            ("policies/policy1.cedar", "permit();"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        assert!(vfs.is_file("metadata.json"));
        assert!(vfs.is_file("policies/policy1.cedar"));
        assert!(!vfs.is_file("policies"));
        assert!(!vfs.is_file("nonexistent.json"));
    }

    #[test]
    fn test_is_dir() {
        let bytes = create_test_archive(vec![
            ("metadata.json", "{}"),
            ("policies/policy1.cedar", "permit();"),
            ("policies/policy2.cedar", "forbid();"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        assert!(vfs.is_dir("."));
        assert!(vfs.is_dir("policies"));
        assert!(!vfs.is_dir("metadata.json"));
        assert!(!vfs.is_dir("nonexistent"));
    }

    #[test]
    fn test_read_dir_root() {
        let bytes = create_test_archive(vec![
            ("metadata.json", "{}"),
            ("schema.cedarschema", "namespace Test;"),
            ("policies/policy1.cedar", "permit();"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        let entries = vfs.read_dir(".").unwrap();
        assert_eq!(entries.len(), 3);

        let names: Vec<_> = entries.iter().map(|e| e.name.as_str()).collect();
        assert!(names.contains(&"metadata.json"));
        assert!(names.contains(&"schema.cedarschema"));
        assert!(names.contains(&"policies"));
    }

    #[test]
    fn test_read_dir_subdirectory() {
        let bytes = create_test_archive(vec![
            ("policies/policy1.cedar", "permit();"),
            ("policies/policy2.cedar", "forbid();"),
            ("policies/nested/policy3.cedar", "deny();"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        let entries = vfs.read_dir("policies").unwrap();
        assert_eq!(entries.len(), 3);

        let names: Vec<_> = entries.iter().map(|e| e.name.as_str()).collect();
        assert!(names.contains(&"policy1.cedar"));
        assert!(names.contains(&"policy2.cedar"));
        assert!(names.contains(&"nested"));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_from_file_path_invalid_extension() {
        use tempfile::TempDir;

        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.zip");

        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        std::fs::write(&archive_path, bytes).unwrap();

        let result = ArchiveVfs::from_file(&archive_path);
        assert!(matches!(
            result.expect_err("should fail"),
            ArchiveError::InvalidExtension { .. }
        ));
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_from_file_path_success() {
        use tempfile::TempDir;

        let temp_dir = TempDir::new().unwrap();
        let archive_path = temp_dir.path().join("test.cjar");

        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        std::fs::write(&archive_path, bytes).unwrap();

        let result = ArchiveVfs::from_file(&archive_path);
        assert!(result.is_ok());
    }

    #[test]
    fn test_complex_directory_structure() {
        let bytes = create_test_archive(vec![
            ("metadata.json", "{}"),
            ("policies/allow/policy1.cedar", "permit();"),
            ("policies/allow/policy2.cedar", "permit();"),
            ("policies/deny/policy3.cedar", "forbid();"),
            ("entities/users/admin.json", "{}"),
            ("entities/users/regular.json", "{}"),
            ("entities/groups/admins.json", "{}"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes).unwrap();

        // Test root
        let root_entries = vfs.read_dir(".").unwrap();
        assert_eq!(root_entries.len(), 3); // metadata.json, policies, entities

        // Test policies directory
        let policies_entries = vfs.read_dir("policies").unwrap();
        assert_eq!(policies_entries.len(), 2); // allow, deny

        // Test nested allow directory
        let allow_entries = vfs.read_dir("policies/allow").unwrap();
        assert_eq!(allow_entries.len(), 2); // policy1.cedar, policy2.cedar
    }
}
