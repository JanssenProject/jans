// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store source and format types.

use std::path::PathBuf;

/// Source of a policy store, supporting multiple input formats.
#[derive(Debug, Clone)]
#[allow(dead_code)]
pub enum PolicyStoreSource {
    /// Directory structure format (for development)
    Directory(PathBuf),
    /// Compressed archive format (.cjar file for distribution)
    /// Can be a file path or a URL
    Archive(ArchiveSource),
    /// Legacy JSON/YAML format (backward compatibility)
    Legacy(String),
}

/// Source for archive-based policy stores.
#[derive(Debug, Clone)]
#[allow(dead_code)]
pub enum ArchiveSource {
    /// Local file path
    File(PathBuf),
    /// Remote URL (HTTP/HTTPS)
    Url(String),
}

/// Format of a policy store.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[allow(dead_code)]
pub enum PolicyStoreFormat {
    /// Directory structure format
    Directory,
    /// Compressed .cjar archive format
    Archive,
    /// Legacy format
    Legacy,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_policy_store_source_variants() {
        let dir_source = PolicyStoreSource::Directory(PathBuf::from("/path/to/store"));
        let archive_file_source =
            PolicyStoreSource::Archive(ArchiveSource::File(PathBuf::from("/path/to/store.cjar")));
        let archive_url_source = PolicyStoreSource::Archive(ArchiveSource::Url(
            "https://example.com/store.cjar".to_string(),
        ));
        let legacy_source = PolicyStoreSource::Legacy("{}".to_string());

        // Verify we can create all variants
        match dir_source {
            PolicyStoreSource::Directory(path) => {
                assert_eq!(path.to_str().unwrap(), "/path/to/store")
            },
            _ => panic!("Expected Directory variant"),
        }

        match archive_file_source {
            PolicyStoreSource::Archive(ArchiveSource::File(path)) => {
                assert_eq!(path.to_str().unwrap(), "/path/to/store.cjar")
            },
            _ => panic!("Expected Archive File variant"),
        }

        match archive_url_source {
            PolicyStoreSource::Archive(ArchiveSource::Url(url)) => {
                assert_eq!(url, "https://example.com/store.cjar")
            },
            _ => panic!("Expected Archive Url variant"),
        }

        match legacy_source {
            PolicyStoreSource::Legacy(content) => assert_eq!(content, "{}"),
            _ => panic!("Expected Legacy variant"),
        }
    }

    #[test]
    fn test_policy_store_format_enum() {
        assert_eq!(PolicyStoreFormat::Directory, PolicyStoreFormat::Directory);
        assert_ne!(PolicyStoreFormat::Directory, PolicyStoreFormat::Archive);
        assert_ne!(PolicyStoreFormat::Archive, PolicyStoreFormat::Legacy);
    }
}
