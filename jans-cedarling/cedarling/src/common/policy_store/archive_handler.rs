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
//! - Provides full [`VfsFileSystem`] trait implementation
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
#[cfg(not(target_arch = "wasm32"))]
use std::path::Path;
use std::sync::Mutex;
use zip::ZipArchive;

/// Resource limits bounding what [`ArchiveVfs`] will decompress into memory.
/// A compressed `.cjar` can expand arbitrarily; these turn an OOM into a typed
/// [`ArchiveError`]. A limit of `0` disables that check.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
// The shared `max_` prefix marks these as caps rather than measurements, which
// is worth more at the use sites than satisfying `struct_field_names`.
#[allow(clippy::struct_field_names)]
pub(crate) struct ArchiveLimits {
    /// Maximum decompressed size of a single entry, in bytes.
    pub max_entry_size: u64,
    /// Maximum combined decompressed size of every entry, in bytes.
    pub max_total_size: u64,
    /// Maximum number of entries in the archive.
    pub max_entries: usize,
}

impl ArchiveLimits {
    /// Matches the 10 MB cap `StatusList::parse` applies to status lists.
    pub(crate) const DEFAULT_MAX_ENTRY_SIZE: u64 = 10 * 1024 * 1024;

    /// Derived rather than configured, so raising the per-entry cap scales the
    /// total with it instead of tripping a fixed ceiling.
    const TOTAL_SIZE_RATIO: u64 = 10;

    /// Also bounds the O(n) scans in `read_dir` / `is_directory_locked`.
    const MAX_ENTRIES: usize = 10_000;

    /// Build limits from a `CEDARLING_POLICY_STORE_MAX_FILE_SIZE` value. `0`
    /// disables both size caps; the entry-count cap always applies.
    pub(crate) fn from_max_file_size(max_entry_size: u64) -> Self {
        Self {
            max_entry_size,
            max_total_size: max_entry_size.saturating_mul(Self::TOTAL_SIZE_RATIO),
            max_entries: Self::MAX_ENTRIES,
        }
    }
}

impl Default for ArchiveLimits {
    fn default() -> Self {
        Self::from_max_file_size(Self::DEFAULT_MAX_ENTRY_SIZE)
    }
}

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
pub(super) struct ArchiveVfs<T> {
    /// The ZIP archive reader (wrapped in Mutex for thread safety)
    archive: Mutex<ZipArchive<T>>,
    /// Resource limits enforced at construction and on every `read_file`.
    limits: ArchiveLimits,
}

impl<T> ArchiveVfs<T>
where
    T: Read + Seek,
{
    /// Create an [`ArchiveVfs`] from a reader.
    ///
    /// This method:
    /// 1. Validates the reader contains a valid ZIP archive
    /// 2. Enforces `limits` on entry count and decompressed size
    /// 3. Checks for path traversal attempts
    /// 4. Validates archive structure
    ///
    /// # Errors
    ///
    /// Returns `ArchiveError` if:
    /// - Reader does not contain a valid ZIP archive
    /// - Archive contains path traversal attempts
    /// - Archive is corrupted
    /// - Archive exceeds any of the `limits`
    pub(super) fn from_reader(reader: T, limits: ArchiveLimits) -> Result<Self, ArchiveError> {
        let mut archive = ZipArchive::new(reader).map_err(|e| ArchiveError::InvalidZipFormat {
            details: e.to_string(),
        })?;

        // Checked before the loop: the count is a per-call cost multiplier for
        // `read_dir` / `is_directory_locked`, not just a memory concern.
        if limits.max_entries > 0 && archive.len() > limits.max_entries {
            return Err(ArchiveError::TooManyEntries {
                count: archive.len(),
                limit: limits.max_entries,
            });
        }

        let mut total_size: u64 = 0;

        // Validate all file names for security
        for i in 0..archive.len() {
            let file = archive
                .by_index(i)
                .map_err(|e| ArchiveError::CorruptedEntry {
                    index: i,
                    details: e.to_string(),
                })?;

            let file_name = file.name();

            // Explicitly reject absolute paths (Unix-style or Windows-style)
            // Note: enclosed_name() behavior is inconsistent across environments -
            // it may return Some("etc/passwd") on some systems and None on others.
            // We explicitly check for absolute paths to ensure consistent behavior.
            if file_name.starts_with('/') || file_name.starts_with('\\') {
                return Err(ArchiveError::PathTraversal {
                    path: file_name.to_string(),
                });
            }

            // Check for Windows-style absolute paths (e.g., "C:\", "D:/")
            if file_name.len() >= 2 {
                let mut chars = file_name.chars();
                if let (Some(first), Some(second)) = (chars.next(), chars.next())
                    && first.is_ascii_alphabetic()
                    && second == ':'
                {
                    return Err(ArchiveError::PathTraversal {
                        path: file_name.to_string(),
                    });
                }
            }

            // Use enclosed_name() to validate and normalize the path
            // This handles path traversal patterns like "../"
            let normalized = file.enclosed_name();
            if let Some(normalized_path) = normalized {
                // Additional check: ensure normalized path doesn't contain .. sequences
                let path_str = normalized_path.to_string_lossy();
                if path_str.contains("..") {
                    return Err(ArchiveError::PathTraversal {
                        path: file_name.to_string(),
                    });
                }
            } else {
                return Err(ArchiveError::PathTraversal {
                    path: file_name.to_string(),
                });
            }

            // Central-directory sizes are author-controlled and can understate
            // reality, so this is only a cheap fail-fast; `read_file` re-checks
            // against the real decompressed byte count.
            let declared_size = file.size();

            if limits.max_entry_size > 0 && declared_size > limits.max_entry_size {
                return Err(ArchiveError::EntrySizeExceeded {
                    path: file_name.to_string(),
                    limit: limits.max_entry_size,
                });
            }

            total_size = total_size.saturating_add(declared_size);
            if limits.max_total_size > 0 && total_size > limits.max_total_size {
                return Err(ArchiveError::ArchiveSizeExceeded {
                    limit: limits.max_total_size,
                });
            }
        }

        Ok(Self {
            archive: Mutex::new(archive),
            limits,
        })
    }
}

impl ArchiveVfs<std::fs::File> {
    /// Create an [`ArchiveVfs`] from a file path (native only).
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
    /// - Archive exceeds any of the `limits`
    #[cfg(not(target_arch = "wasm32"))]
    pub(super) fn from_file<P: AsRef<Path>>(
        path: P,
        limits: ArchiveLimits,
    ) -> Result<Self, ArchiveError> {
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

        Self::from_reader(file, limits)
    }
}

impl ArchiveVfs<Cursor<Vec<u8>>> {
    /// Create an [`ArchiveVfs`] from bytes (works in WASM and native).
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
    /// - Archive exceeds any of the `limits`
    pub(super) fn from_buffer(
        buffer: Vec<u8>,
        limits: ArchiveLimits,
    ) -> Result<Self, ArchiveError> {
        let cursor = Cursor::new(buffer);
        Self::from_reader(cursor, limits)
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
    /// - Removing leading "./" prefix
    /// - Converting "." to ""
    /// - Normalizing path separators
    fn normalize_path(path: &str) -> String {
        let path = path.trim_start_matches('/');
        let path = path.strip_prefix("./").unwrap_or(path);
        if path == "." || path.is_empty() {
            String::new()
        } else {
            path.to_string()
        }
    }

    /// Check if a path exists in the archive (file or directory).
    fn path_exists(&self, path: &str) -> Result<bool, std::io::Error> {
        let normalized = Self::normalize_path(path);

        let mut archive = self
            .archive
            .lock()
            .map_err(|e| std::io::Error::other(format!("archive mutex poisoned: {e}")))?;

        // Check if it's a file
        if archive.by_name(&normalized).is_ok() {
            return Ok(true);
        }

        // Check if it's a directory by looking for entries that start with this prefix
        let dir_prefix = if normalized.is_empty() {
            String::new()
        } else {
            format!("{normalized}/")
        };

        for i in 0..archive.len() {
            if let Ok(file) = archive.by_index(i) {
                let file_name = file.name();
                if file_name == normalized || file_name.starts_with(&dir_prefix) {
                    return Ok(true);
                }
            }
        }

        Ok(false)
    }

    /// Check if a path is a file in the archive.
    #[cfg(test)]
    pub(super) fn is_file(&self, path: &str) -> bool {
        let normalized = Self::normalize_path(path);
        let Ok(mut archive) = self.archive.lock() else {
            return false;
        };

        if let Ok(file) = archive.by_name(&normalized) {
            return file.is_file();
        }

        false
    }

    /// Check if a path is a directory in the archive.
    fn is_directory(&self, path: &str) -> Result<bool, std::io::Error> {
        let normalized = Self::normalize_path(path);
        let mut archive = self
            .archive
            .lock()
            .map_err(|e| std::io::Error::other(format!("archive mutex poisoned: {e}")))?;
        Ok(Self::is_directory_locked(&mut archive, &normalized))
    }

    /// Check if a path is a directory (with already-locked archive).
    /// This is a helper to avoid deadlocks when called from methods that already hold the lock.
    fn is_directory_locked(archive: &mut ZipArchive<T>, normalized: &str) -> bool {
        // Root is always a directory
        if normalized.is_empty() {
            return true;
        }

        // Check if there's an explicit directory entry
        let dir_path_with_slash = format!("{normalized}/");
        if let Ok(file) = archive.by_name(&dir_path_with_slash) {
            return file.is_dir();
        }

        // Check if any files have this as a prefix (implicit directory)
        for i in 0..archive.len() {
            if let Ok(file) = archive.by_index(i) {
                let file_name = file.name();
                if file_name.starts_with(&format!("{normalized}/")) {
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
        let normalized = Self::normalize_path(path);

        let mut archive = self
            .archive
            .lock()
            .map_err(|e| std::io::Error::other(format!("archive mutex poisoned: {e}")))?;

        let mut file = archive.by_name(&normalized).map_err(|e| {
            std::io::Error::new(
                std::io::ErrorKind::NotFound,
                format!("File not found in archive: {path}: {e}"),
            )
        })?;

        let max_entry_size = self.limits.max_entry_size;
        let mut contents = Vec::new();

        if max_entry_size == 0 {
            file.read_to_end(&mut contents)?;
            return Ok(contents);
        }

        // One byte past the cap is enough to detect a central directory that
        // understated this entry and slipped past the check in `from_reader`.
        file.take(max_entry_size.saturating_add(1))
            .read_to_end(&mut contents)?;

        if contents.len() as u64 > max_entry_size {
            return Err(std::io::Error::other(ArchiveError::EntrySizeExceeded {
                path: path.to_string(),
                limit: max_entry_size,
            }));
        }

        Ok(contents)
    }

    fn exists(&self, path: &str) -> bool {
        self.path_exists(path).unwrap_or(false)
    }

    fn is_dir(&self, path: &str) -> bool {
        self.is_directory(path).unwrap_or(false)
    }

    fn read_dir(&self, path: &str) -> Result<Vec<DirEntry>, std::io::Error> {
        let normalized = Self::normalize_path(path);
        let prefix = if normalized.is_empty() {
            String::new()
        } else {
            format!("{normalized}/")
        };

        let mut archive = self
            .archive
            .lock()
            .map_err(|e| std::io::Error::other(format!("archive mutex poisoned: {e}")))?;
        let mut seen = std::collections::HashSet::new();
        let mut entry_paths = Vec::new();

        // First pass: collect all unique entry paths
        for i in 0..archive.len() {
            let file = archive.by_index(i).map_err(|e| {
                std::io::Error::other(format!("Failed to read archive entry {i}: {e}"))
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
                    format!("{prefix}{child_name}")
                };

                entry_paths.push((child_name.to_string(), entry_path));
            }
        }

        // Second pass: check if each path is a directory
        let mut entries = Vec::new();
        for (name, entry_path) in entry_paths {
            let entry_path_normalized = Self::normalize_path(&entry_path);
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
    pub(super) fn create_test_archive(files: Vec<(&str, &str)>) -> Vec<u8> {
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

    /// Helper to create a test .cjar archive with zero-filled entries of the
    /// given sizes, which deflate to almost nothing — the shape of a zip bomb.
    pub(super) fn create_archive_with_sizes(files: Vec<(&str, usize)>) -> Vec<u8> {
        let mut buffer = Vec::new();
        {
            let cursor = Cursor::new(&mut buffer);
            let mut zip = zip::ZipWriter::new(cursor);

            for (name, size) in files {
                let options = FileOptions::<ExtendedFileOptions>::default()
                    .compression_method(CompressionMethod::Deflated);
                zip.start_file(name, options).unwrap();
                zip.write_all(&vec![0u8; size]).unwrap();
            }

            zip.finish().unwrap();
        }
        buffer
    }

    #[test]
    fn test_from_buffer_valid_archive() {
        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        let _result = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default())
            .expect("expect ArchiveVfs initialized correctly from buffer");
    }

    #[test]
    fn test_from_buffer_invalid_zip() {
        let bytes = b"This is not a ZIP file".to_vec();
        let result = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default());
        let err = result.expect_err("Expected InvalidZipFormat error for non-ZIP data");
        assert!(
            matches!(err, ArchiveError::InvalidZipFormat { .. }),
            "Expected InvalidZipFormat error, got: {err:?}"
        );
    }

    #[test]
    fn test_from_buffer_path_traversal() {
        let bytes = create_test_archive(vec![("../../../etc/passwd", "malicious")]);
        let result = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default());
        let err = result.expect_err("Expected PathTraversal error for malicious path");
        assert!(
            matches!(err, ArchiveError::PathTraversal { .. }),
            "Expected PathTraversal error, got: {err:?}"
        );
    }

    #[test]
    fn test_read_file_success() {
        let bytes = create_test_archive(vec![
            ("metadata.json", r#"{"version":"1.0"}"#),
            ("schema.cedarschema", "namespace Test;"),
        ]);
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

        let content = vfs.read_file("metadata.json").unwrap();
        assert_eq!(String::from_utf8(content).unwrap(), r#"{"version":"1.0"}"#);

        let content = vfs.read_file("schema.cedarschema").unwrap();
        assert_eq!(String::from_utf8(content).unwrap(), "namespace Test;");
    }

    #[test]
    fn test_read_file_not_found() {
        let bytes = create_test_archive(vec![("metadata.json", "{}")]);
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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

        let result = ArchiveVfs::from_file(&archive_path, ArchiveLimits::default());
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

        ArchiveVfs::from_file(&archive_path, ArchiveLimits::default())
            .expect("should load valid .cjar file");
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
        let vfs = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default()).unwrap();

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

#[cfg(test)]
mod limit_tests {
    use super::tests::{create_archive_with_sizes, create_test_archive};
    use super::*;

    /// Keeps the boundary tests cheap while exercising the same code paths as
    /// the 10 MB production default.
    const SMALL_LIMIT: u64 = 4096;

    /// `usize` view of [`SMALL_LIMIT`], for the byte-count helpers.
    fn small_limit_bytes() -> usize {
        usize::try_from(SMALL_LIMIT).expect("SMALL_LIMIT fits in usize")
    }

    fn small_limits() -> ArchiveLimits {
        ArchiveLimits::from_max_file_size(SMALL_LIMIT)
    }

    #[test]
    fn test_zip_bomb_entry_rejected_at_default_limit() {
        // ~11 MB of zeros deflates to a few KB: a tiny archive that was
        // previously `read_to_end`'d straight into memory.
        let oversized = usize::try_from(ArchiveLimits::DEFAULT_MAX_ENTRY_SIZE + 1)
            .expect("default cap fits in usize");
        let bytes = create_archive_with_sizes(vec![("metadata.json", oversized)]);
        assert!(
            bytes.len() < 100 * 1024,
            "test archive should be small on disk to model a zip bomb, was {} bytes",
            bytes.len()
        );

        let err = ArchiveVfs::from_buffer(bytes, ArchiveLimits::default())
            .expect_err("Expected EntrySizeExceeded for a zip-bomb entry");
        assert!(
            matches!(err, ArchiveError::EntrySizeExceeded { .. }),
            "Expected EntrySizeExceeded, got: {err:?}"
        );
    }

    #[test]
    fn test_entry_at_exact_limit_is_accepted() {
        let bytes = create_archive_with_sizes(vec![("metadata.json", small_limit_bytes())]);

        let vfs = ArchiveVfs::from_buffer(bytes, small_limits())
            .expect("An entry of exactly the limit must be accepted");

        let contents = vfs
            .read_file("metadata.json")
            .expect("read_file must also accept an entry of exactly the limit");
        assert_eq!(contents.len(), small_limit_bytes());
    }

    #[test]
    fn test_entry_one_byte_over_limit_is_rejected() {
        let bytes = create_archive_with_sizes(vec![("metadata.json", small_limit_bytes() + 1)]);

        let err = ArchiveVfs::from_buffer(bytes, small_limits())
            .expect_err("Expected EntrySizeExceeded one byte past the limit");
        match err {
            ArchiveError::EntrySizeExceeded { path, limit } => {
                assert_eq!(path, "metadata.json");
                assert_eq!(limit, SMALL_LIMIT);
            },
            other => panic!("Expected EntrySizeExceeded, got: {other:?}"),
        }
    }

    #[test]
    fn test_total_archive_size_exceeded() {
        // Every entry sits under the per-entry cap; only the sum trips the
        // total, which `from_max_file_size` derives as 10x the per-entry cap.
        let limits = small_limits();
        let entry_count = usize::try_from(limits.max_total_size / SMALL_LIMIT + 1)
            .expect("entry count fits in usize");
        let names: Vec<String> = (0..entry_count).map(|i| format!("file{i}.json")).collect();
        let bytes = create_archive_with_sizes(
            names
                .iter()
                .map(|n| (n.as_str(), small_limit_bytes()))
                .collect(),
        );

        let err = ArchiveVfs::from_buffer(bytes, limits)
            .expect_err("Expected ArchiveSizeExceeded when the entries sum past the total");
        match err {
            ArchiveError::ArchiveSizeExceeded { limit } => {
                assert_eq!(limit, limits.max_total_size);
            },
            other => panic!("Expected ArchiveSizeExceeded, got: {other:?}"),
        }
    }

    #[test]
    fn test_total_archive_size_at_exact_limit_is_accepted() {
        let limits = small_limits();
        let entry_count = usize::try_from(limits.max_total_size / SMALL_LIMIT)
            .expect("entry count fits in usize");
        let names: Vec<String> = (0..entry_count).map(|i| format!("file{i}.json")).collect();
        let bytes = create_archive_with_sizes(
            names
                .iter()
                .map(|n| (n.as_str(), small_limit_bytes()))
                .collect(),
        );

        ArchiveVfs::from_buffer(bytes, limits)
            .expect("An archive totalling exactly the limit must be accepted");
    }

    #[test]
    fn test_too_many_entries_is_rejected() {
        let limits = ArchiveLimits {
            max_entries: 4,
            ..ArchiveLimits::default()
        };
        let names: Vec<String> = (0..5).map(|i| format!("file{i}.json")).collect();
        let bytes = create_test_archive(names.iter().map(|n| (n.as_str(), "{}")).collect());

        let err = ArchiveVfs::from_buffer(bytes, limits)
            .expect_err("Expected TooManyEntries past the entry-count cap");
        match err {
            ArchiveError::TooManyEntries { count, limit } => {
                assert_eq!(count, 5);
                assert_eq!(limit, 4);
            },
            other => panic!("Expected TooManyEntries, got: {other:?}"),
        }
    }

    #[test]
    fn test_entry_count_at_exact_limit_is_accepted() {
        let limits = ArchiveLimits {
            max_entries: 5,
            ..ArchiveLimits::default()
        };
        let names: Vec<String> = (0..5).map(|i| format!("file{i}.json")).collect();
        let bytes = create_test_archive(names.iter().map(|n| (n.as_str(), "{}")).collect());

        ArchiveVfs::from_buffer(bytes, limits)
            .expect("An archive with exactly the entry limit must be accepted");
    }

    #[test]
    fn test_zero_disables_size_limits() {
        // `0` is the "no cap" sentinel, matching the HTTP response cap.
        let limits = ArchiveLimits::from_max_file_size(0);
        assert_eq!(limits.max_total_size, 0);

        let bytes = create_archive_with_sizes(vec![("metadata.json", 64 * 1024)]);
        let vfs = ArchiveVfs::from_buffer(bytes, limits)
            .expect("Size caps must be disabled when the limit is 0");
        assert_eq!(vfs.read_file("metadata.json").unwrap().len(), 64 * 1024);
    }

    #[test]
    fn test_read_file_rejects_entry_whose_declared_size_lies() {
        // Rewrite the central directory's recorded size to 1 byte so the
        // construction-time check passes, then confirm `read_file` still
        // refuses to buffer the real payload.
        let real_size = small_limit_bytes() + 1;
        let mut bytes = create_archive_with_sizes(vec![("metadata.json", real_size)]);
        let truthful = u32::try_from(real_size)
            .expect("test entry size fits in u32")
            .to_le_bytes();
        let lie = 1u32.to_le_bytes();

        let mut patched = 0;
        for i in 0..bytes.len().saturating_sub(4) {
            if bytes[i..i + 4] == truthful {
                bytes[i..i + 4].copy_from_slice(&lie);
                patched += 1;
            }
        }
        assert!(
            patched >= 2,
            "expected to patch the local header and central directory size fields, patched {patched}"
        );

        let vfs = ArchiveVfs::from_buffer(bytes, small_limits())
            .expect("A understated declared size must pass the cheap header check");

        let err = vfs
            .read_file("metadata.json")
            .expect_err("read_file must reject an entry that decompresses past the cap");
        let source = err
            .get_ref()
            .and_then(|e| e.downcast_ref::<ArchiveError>())
            .expect("io::Error must carry the typed ArchiveError");
        assert!(
            matches!(source, ArchiveError::EntrySizeExceeded { .. }),
            "Expected EntrySizeExceeded, got: {source:?}"
        );
    }
}
