// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Virtual File System (VFS) adapter for policy store loading.
//!
//! This module provides an abstraction layer over filesystem operations to enable:
//! - Native filesystem access on non-WASM platforms
//! - In-memory filesystem for testing and WASM environments
//! - Future support for archive (.cjar) loading
//!
//! The VFS abstraction allows the policy store loader to work uniformly across
//! different storage backends without changing the loading logic.

use std::io::{self, Read};
use vfs::{PhysicalFS, VfsPath};

#[cfg(test)]
use std::path::Path;

/// Represents a directory entry from VFS.
#[derive(Debug, Clone)]
pub struct DirEntry {
    /// The file name
    pub name: String,
    /// The full path
    pub path: String,
    /// Whether this is a directory
    pub is_dir: bool,
}

/// Trait for virtual filesystem operations.
///
/// This trait abstracts filesystem operations to enable testing and cross-platform support.
///
/// # Examples
///
/// Using `open_file` with `BufReader` for efficient reading:
///
/// ```text
/// use std::io::{BufRead, BufReader};
/// use crate::common::policy_store::vfs_adapter::{PhysicalVfs, VfsFileSystem};
///
/// let vfs = PhysicalVfs::new();
/// let reader = vfs.open_file("/path/to/file.txt")?;
/// let buf_reader = BufReader::new(reader);
///
/// for line in buf_reader.lines() {
///     println!("{}", line?);
/// }
/// ```
///
/// Using `read_file` for small files:
///
/// ```text
/// use crate::common::policy_store::vfs_adapter::{PhysicalVfs, VfsFileSystem};
///
/// let vfs = PhysicalVfs::new();
/// let content = vfs.read_file("/path/to/small-file.txt")?;
/// let text = String::from_utf8(content)?;
/// ```
pub trait VfsFileSystem: Send + Sync + 'static {
    /// Open a file and return a reader.
    ///
    /// This is the primary method for reading files, allowing callers to:
    /// - Read incrementally (memory efficient for large files)
    /// - Use standard I/O traits like `BufReader`
    /// - Control buffer sizes
    ///
    /// # Examples
    ///
    /// ```text
    /// use std::io::BufReader;
    /// use crate::common::policy_store::vfs_adapter::{PhysicalVfs, VfsFileSystem};
    ///
    /// let vfs = PhysicalVfs::new();
    /// let reader = vfs.open_file("/path/to/file.json")?;
    /// let buf_reader = BufReader::new(reader);
    ///
    /// // Can now use serde_json::from_reader, etc.
    /// ```
    fn open_file(&self, path: &str) -> io::Result<Box<dyn Read + Send>>;

    /// Read the entire contents of a file into memory.
    ///
    /// This is a convenience method that reads the entire file.
    /// For large files, consider using `open_file` instead.
    fn read_file(&self, path: &str) -> io::Result<Vec<u8>> {
        let mut reader = self.open_file(path)?;
        let mut buffer = Vec::new();
        reader.read_to_end(&mut buffer)?;
        Ok(buffer)
    }

    /// Read directory entries.
    fn read_dir(&self, path: &str) -> io::Result<Vec<DirEntry>>;

    /// Check if a path exists.
    fn exists(&self, path: &str) -> bool;

    /// Check if a path is a directory.
    fn is_dir(&self, path: &str) -> bool;

    /// Check if a path is a file.
    fn is_file(&self, path: &str) -> bool;
}

/// Physical filesystem implementation for native platforms.
///
/// Uses the actual filesystem via the `vfs::PhysicalFS` backend.
#[cfg(not(target_arch = "wasm32"))]
#[derive(Debug)]
pub struct PhysicalVfs {
    root: VfsPath,
}

#[cfg(not(target_arch = "wasm32"))]
impl PhysicalVfs {
    /// Create a new physical VFS rooted at the system root.
    pub fn new() -> Self {
        let root = PhysicalFS::new("/").into();
        Self { root }
    }

    /// Helper to get a VfsPath from a string path.
    fn get_path(&self, path: &str) -> VfsPath {
        self.root.join(path).unwrap()
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl Default for PhysicalVfs {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl VfsFileSystem for PhysicalVfs {
    fn open_file(&self, path: &str) -> io::Result<Box<dyn Read + Send>> {
        let vfs_path = self.get_path(path);
        let file = vfs_path
            .open_file()
            .map_err(|e| io::Error::new(io::ErrorKind::NotFound, e))?;
        Ok(Box::new(file))
    }

    fn read_dir(&self, path: &str) -> io::Result<Vec<DirEntry>> {
        let vfs_path = self.get_path(path);
        let entries = vfs_path
            .read_dir()
            .map_err(|e| io::Error::new(io::ErrorKind::NotFound, e))?;

        let mut result = Vec::new();
        for entry in entries {
            let metadata = entry.metadata().map_err(io::Error::other)?;
            let filename = entry.filename();
            let full_path = entry.as_str().to_string();

            result.push(DirEntry {
                name: filename,
                path: full_path,
                is_dir: metadata.file_type == vfs::VfsFileType::Directory,
            });
        }

        Ok(result)
    }

    fn exists(&self, path: &str) -> bool {
        self.get_path(path).exists().unwrap_or(false)
    }

    fn is_dir(&self, path: &str) -> bool {
        self.get_path(path)
            .metadata()
            .map(|m| m.file_type == vfs::VfsFileType::Directory)
            .unwrap_or(false)
    }

    fn is_file(&self, path: &str) -> bool {
        self.get_path(path)
            .metadata()
            .map(|m| m.file_type == vfs::VfsFileType::File)
            .unwrap_or(false)
    }
}

/// In-memory filesystem implementation for testing.
///
/// Uses `vfs::MemoryFS` to store files in memory. This is useful for:
/// - Unit testing without touching the real filesystem
/// - Building policy stores programmatically in memory for tests
#[cfg(test)]
#[derive(Debug)]
pub struct MemoryVfs {
    root: VfsPath,
}

#[cfg(test)]
impl MemoryVfs {
    /// Create a new empty in-memory VFS.
    pub fn new() -> Self {
        let root = vfs::MemoryFS::new().into();
        Self { root }
    }

    /// Helper to get a VfsPath from a string path.
    fn get_path(&self, path: &str) -> VfsPath {
        self.root.join(path).unwrap()
    }

    /// Create a file with the given content.
    pub fn create_file(&self, path: &str, content: &[u8]) -> io::Result<()> {
        let vfs_path = self.get_path(path);

        // Create parent directories if needed
        if let Some(parent) = Path::new(path).parent()
            && !parent.as_os_str().is_empty()
        {
            let parent_str = parent.to_str().ok_or_else(|| {
                io::Error::new(io::ErrorKind::InvalidInput, "Invalid parent path")
            })?;
            self.create_dir_all(parent_str)?;
        }

        let mut file = vfs_path.create_file().map_err(io::Error::other)?;
        std::io::Write::write_all(&mut file, content)?;
        Ok(())
    }

    /// Create a directory and all of its parents.
    pub fn create_dir_all(&self, path: &str) -> io::Result<()> {
        let vfs_path = self.get_path(path);
        vfs_path.create_dir_all().map_err(io::Error::other)
    }
}

#[cfg(test)]
impl Default for MemoryVfs {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
impl VfsFileSystem for MemoryVfs {
    fn open_file(&self, path: &str) -> io::Result<Box<dyn Read + Send>> {
        let vfs_path = self.get_path(path);
        let file = vfs_path
            .open_file()
            .map_err(|e| io::Error::new(io::ErrorKind::NotFound, e))?;
        Ok(Box::new(file))
    }

    fn read_dir(&self, path: &str) -> io::Result<Vec<DirEntry>> {
        let vfs_path = self.get_path(path);
        let entries = vfs_path
            .read_dir()
            .map_err(|e| io::Error::new(io::ErrorKind::NotFound, e))?;

        let mut result = Vec::new();
        for entry in entries {
            let metadata = entry.metadata().map_err(io::Error::other)?;
            let filename = entry.filename();
            let full_path = entry.as_str().to_string();

            result.push(DirEntry {
                name: filename,
                path: full_path,
                is_dir: metadata.file_type == vfs::VfsFileType::Directory,
            });
        }

        Ok(result)
    }

    fn exists(&self, path: &str) -> bool {
        self.get_path(path).exists().unwrap_or(false)
    }

    fn is_dir(&self, path: &str) -> bool {
        self.get_path(path)
            .metadata()
            .map(|m| m.file_type == vfs::VfsFileType::Directory)
            .unwrap_or(false)
    }

    fn is_file(&self, path: &str) -> bool {
        self.get_path(path)
            .metadata()
            .map(|m| m.file_type == vfs::VfsFileType::File)
            .unwrap_or(false)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_memory_vfs_create_and_read_file() {
        let vfs = MemoryVfs::new();
        let content = b"test content";

        vfs.create_file("/test.txt", content).unwrap();

        assert!(vfs.exists("/test.txt"));
        assert!(vfs.is_file("/test.txt"));
        assert!(!vfs.is_dir("/test.txt"));

        let read_content = vfs.read_file("/test.txt").unwrap();
        assert_eq!(read_content, content);
    }

    #[test]
    fn test_memory_vfs_create_dir() {
        let vfs = MemoryVfs::new();

        vfs.create_dir_all("/test/nested/dir").unwrap();

        assert!(vfs.exists("/test"));
        assert!(vfs.is_dir("/test"));
        assert!(!vfs.is_file("/test"));

        assert!(vfs.exists("/test/nested/dir"));
        assert!(vfs.is_dir("/test/nested/dir"));
    }

    #[test]
    fn test_memory_vfs_read_dir() {
        let vfs = MemoryVfs::new();

        vfs.create_file("/test/file1.txt", b"content1").unwrap();
        vfs.create_file("/test/file2.txt", b"content2").unwrap();
        vfs.create_dir_all("/test/subdir").unwrap();

        let entries = vfs.read_dir("/test").unwrap();
        assert_eq!(entries.len(), 3);

        let names: Vec<String> = entries.iter().map(|e| e.name.clone()).collect();
        assert!(names.contains(&"file1.txt".to_string()));
        assert!(names.contains(&"file2.txt".to_string()));
        assert!(names.contains(&"subdir".to_string()));
    }

    #[test]
    fn test_memory_vfs_nonexistent_file() {
        let vfs = MemoryVfs::new();

        assert!(!vfs.exists("/nonexistent.txt"));
        let result = vfs.read_file("/nonexistent.txt");
        result.expect_err("Expected error when reading nonexistent file");
    }

    #[cfg(not(target_arch = "wasm32"))]
    #[test]
    fn test_physical_vfs_exists() {
        use std::fs;
        use tempfile::TempDir;

        let temp_dir = TempDir::new().unwrap();
        let test_file = temp_dir.path().join("test.txt");
        fs::write(&test_file, b"test").unwrap();

        let vfs = PhysicalVfs::new();
        let path_str = test_file.to_str().unwrap();

        assert!(vfs.exists(path_str));
        assert!(vfs.is_file(path_str));
        assert!(!vfs.is_dir(path_str));
    }

    #[cfg(not(target_arch = "wasm32"))]
    #[test]
    fn test_physical_vfs_read_file() {
        use std::fs;
        use tempfile::TempDir;

        let temp_dir = TempDir::new().unwrap();
        let test_file = temp_dir.path().join("test.txt");
        let content = b"test content";
        fs::write(&test_file, content).unwrap();

        let vfs = PhysicalVfs::new();
        let path_str = test_file.to_str().unwrap();

        let read_content = vfs.read_file(path_str).unwrap();
        assert_eq!(read_content, content);
    }

    #[cfg(not(target_arch = "wasm32"))]
    #[test]
    fn test_physical_vfs_read_dir() {
        use std::fs;
        use tempfile::TempDir;

        let temp_dir = TempDir::new().unwrap();
        let dir_path = temp_dir.path();

        fs::write(dir_path.join("file1.txt"), b"content1").unwrap();
        fs::write(dir_path.join("file2.txt"), b"content2").unwrap();
        fs::create_dir(dir_path.join("subdir")).unwrap();

        let vfs = PhysicalVfs::new();
        let path_str = dir_path.to_str().unwrap();

        let entries = vfs.read_dir(path_str).unwrap();
        assert_eq!(entries.len(), 3);

        let names: Vec<String> = entries.iter().map(|e| e.name.clone()).collect();
        assert!(names.contains(&"file1.txt".to_string()));
        assert!(names.contains(&"file2.txt".to_string()));
        assert!(names.contains(&"subdir".to_string()));
    }
}
