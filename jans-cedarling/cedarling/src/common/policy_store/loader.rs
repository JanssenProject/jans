// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loader with format detection and directory loading support.
//!
//! # Internal API Note
//!
//! This module is part of the internal implementation. External users should use the
//! `Cedarling` API with `BootstrapConfig` to load policy stores.
//!
//! # Loading Archives (.cjar files)
//!
//! Archives are loaded using `ArchiveVfs`, which implements the `VfsFileSystem` trait.
//! This design:
//! - Works in WASM (no temp file extraction needed)
//! - Is efficient (reads files on-demand from archive)
//! - Is secure (no temp file cleanup concerns)
//!
//! # Blocking
//!
//! Every function here is synchronous and does blocking I/O and CPU work
//! (unzipping, parsing, building the runtime store). Callers on an async
//! runtime run it off the executor as one blocking task.

use std::path::Path;

use super::archive_handler::{ArchiveLimits, ArchiveVfs};
use super::errors::{PolicyStoreError, ValidationError};
use super::formats::{self, ParsedPolicyStore, ReadStoreError};
use super::schema_parser::{ParsedSchema, SchemaFile};
use super::vfs_adapter::VfsFileSystem;

/// Read a policy store from a directory path.
///
/// This function uses `PhysicalVfs` to read from the local filesystem.
/// It is only available on native platforms (not WASM).
#[cfg(not(target_arch = "wasm32"))]
pub(crate) fn read_policy_store_directory(
    path: &Path,
    strict: bool,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    let path_str = path
        .to_str()
        .ok_or_else(|| PolicyStoreError::PathNotFound {
            path: path.display().to_string(),
        })?;

    let vfs = super::vfs_adapter::PhysicalVfs::new();
    read_from_vfs(&vfs, path_str, strict)
}

/// Read a policy store from a directory path (WASM stub).
///
/// Directory loading is not supported in WASM environments.
/// Use `read_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub(crate) fn read_policy_store_directory(
    _path: &Path,
    _strict: bool,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    Err(PolicyStoreError::from(super::errors::ArchiveError::WasmUnsupported).into())
}

/// Read a policy store from a Cedar Archive (.cjar) file.
///
/// This function uses `ArchiveVfs` to read from a zip archive.
/// It is only available on native platforms (not WASM).
#[cfg(not(target_arch = "wasm32"))]
pub(crate) fn read_policy_store_archive(
    path: &Path,
    strict: bool,
    limits: ArchiveLimits,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    let archive_vfs = ArchiveVfs::from_file(path, limits).map_err(PolicyStoreError::from)?;
    read_from_vfs(&archive_vfs, ".", strict)
}

/// Read a policy store from a Cedar Archive (.cjar) file (WASM stub).
///
/// File-based archive loading is not supported in WASM environments.
/// Use `read_policy_store_archive_bytes` instead.
#[cfg(target_arch = "wasm32")]
pub(crate) fn read_policy_store_archive(
    _path: &Path,
    _strict: bool,
    _limits: ArchiveLimits,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    Err(PolicyStoreError::from(super::errors::ArchiveError::WasmUnsupported).into())
}

/// Read a policy store from archive bytes.
///
/// Takes the bytes by value so the archive is read in place rather than
/// copied. This function is useful for:
/// - WASM environments where file system access is not available
/// - Loading archives fetched from URLs
/// - Loading archives from any byte source
pub(crate) fn read_policy_store_archive_bytes(
    bytes: Vec<u8>,
    strict: bool,
    limits: ArchiveLimits,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    let archive_vfs = ArchiveVfs::from_buffer(bytes, limits).map_err(PolicyStoreError::from)?;
    read_from_vfs(&archive_vfs, ".", strict)
}

/// Read a policy store rooted at `dir` from any VFS backend into its runtime
/// form. The store's spec version decides where its files live and how they
/// parse (see [`formats::read_policy_store`]).
pub(super) fn read_from_vfs(
    vfs: &dyn VfsFileSystem,
    dir: &str,
    strict: bool,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    formats::read_policy_store(&StoreFs::open(vfs, dir)?, strict)
}

/// One file read from a store: a policy, template, entity or issuer file.
///
/// All four are a name plus text; the directory a file came from is what gives
/// it meaning, so they share one type rather than four identical ones.
#[derive(Debug, Clone)]
pub(crate) struct StoreFile {
    /// File name, without the directory it was read from.
    pub name: String,
    /// File content as UTF-8 text.
    pub content: String,
}

/// Describes where a schema source was found in the policy store.
enum SchemaSource {
    /// The single schema file exists at this path.
    SingleFile { path: String },
    /// The schema directory exists at this path.
    Directory(String),
    /// No schema source found.
    None {
        searched_file: String,
        searched_dir: String,
    },
}

impl SchemaSource {
    fn exists(&self) -> bool {
        matches!(
            self,
            SchemaSource::SingleFile { .. } | SchemaSource::Directory(_)
        )
    }
}

/// The schema of a store, as read by [`StoreFs::schema`].
pub(super) struct LoadedSchema {
    /// Parsed schema (optional — absent when running without schema)
    pub(super) schema: Option<ParsedSchema>,
    /// Whether a schema source was present in the store, regardless of
    /// whether it was loaded/parsed.
    pub(super) source_exists: bool,
}

/// Read-only view of a store's files, rooted at the store's top directory.
///
/// Version modules reach the files only through this type: it owns path
/// joining, UTF-8 decoding, extension checks and the recursion limit, so a
/// spec version decides where its files live but not how they are read.
///
/// Holds the backend as `dyn` so that version modules stay free of the
/// backend type parameter.
pub(super) struct StoreFs<'a> {
    vfs: &'a dyn VfsFileSystem,
    root: &'a str,
}

impl<'a> StoreFs<'a> {
    /// Maximum directory recursion depth to prevent stack overflow from deeply
    /// nested `.cjar` archives or filesystem trees.
    const MAX_RECURSION_DEPTH: usize = 64;

    /// Opens the store rooted at `root`, which must be an existing directory.
    pub(super) fn open(
        vfs: &'a dyn VfsFileSystem,
        root: &'a str,
    ) -> Result<Self, PolicyStoreError> {
        if !vfs.exists(root) {
            return Err(PolicyStoreError::PathNotFound {
                path: root.to_string(),
            });
        }

        if !vfs.is_dir(root) {
            return Err(PolicyStoreError::NotADirectory {
                path: root.to_string(),
            });
        }

        Ok(Self { vfs, root })
    }

    /// Joins a store-relative path onto the root, handling "." correctly.
    fn path(&self, relative: &str) -> String {
        if self.root == "." || self.root.is_empty() {
            relative.to_string()
        } else {
            format!("{}/{relative}", self.root)
        }
    }

    /// Reads a file the store must contain.
    pub(super) fn read_required_file(&self, relative: &str) -> Result<String, PolicyStoreError> {
        let path = self.path(relative);
        if !self.vfs.exists(&path) {
            return Err(ValidationError::MissingRequiredFile {
                file: relative.to_string(),
            }
            .into());
        }
        self.read_text(&path)
    }

    /// Fails unless the store contains the directory.
    pub(super) fn require_dir(&self, relative: &str) -> Result<(), PolicyStoreError> {
        let path = self.path(relative);
        if !self.vfs.exists(&path) {
            return Err(ValidationError::MissingRequiredDirectory {
                directory: relative.to_string(),
            }
            .into());
        }

        if !self.vfs.is_dir(&path) {
            return Err(PolicyStoreError::NotADirectory { path });
        }

        Ok(())
    }

    /// Reads and parses the schema from a single file, or failing that, from
    /// every `.cedarschema` file in a directory. With `strict`, a store that
    /// has neither is an error.
    pub(super) fn schema(
        &self,
        file: &str,
        dir: &str,
        strict: bool,
    ) -> Result<LoadedSchema, PolicyStoreError> {
        let source = self.resolve_schema_source(file, dir);
        let source_exists = source.exists();

        if strict
            && let SchemaSource::None {
                searched_file,
                searched_dir,
            } = source
        {
            return Err(ValidationError::MissingSchemaSource {
                searched_file,
                searched_dir,
            }
            .into());
        }

        Ok(LoadedSchema {
            schema: self.load_schema(&source, file)?,
            source_exists,
        })
    }

    /// Reads every `.cedar` file under a directory the store must contain,
    /// recursively.
    pub(super) fn cedar_files(&self, relative: &str) -> Result<Vec<StoreFile>, PolicyStoreError> {
        let mut files = Vec::new();
        self.load_cedar_files_recursive(&self.path(relative), &mut files, 0)?;
        Ok(files)
    }

    /// Like [`cedar_files`](Self::cedar_files), but an absent directory yields
    /// no files.
    pub(super) fn optional_cedar_files(
        &self,
        relative: &str,
    ) -> Result<Vec<StoreFile>, PolicyStoreError> {
        if !self.vfs.exists(&self.path(relative)) {
            return Ok(Vec::new());
        }
        self.cedar_files(relative)
    }

    /// Reads every `.json` file directly in a directory, if the store has it;
    /// subdirectories are skipped.
    pub(super) fn optional_json_files(
        &self,
        relative: &str,
    ) -> Result<Vec<StoreFile>, PolicyStoreError> {
        let dir = self.path(relative);
        if !self.vfs.exists(&dir) {
            return Ok(Vec::new());
        }

        let entries =
            self.vfs
                .read_dir(&dir)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: dir.clone(),
                    source,
                })?;

        let mut files = Vec::new();
        for entry in entries {
            if entry.is_dir {
                continue;
            }
            Self::check_extension(&entry, ".json")?;
            let content = self.read_text(&entry.path)?;
            files.push(StoreFile {
                name: entry.name,
                content,
            });
        }

        Ok(files)
    }

    /// Reads a file as UTF-8 text.
    fn read_text(&self, path: &str) -> Result<String, PolicyStoreError> {
        let bytes = self
            .vfs
            .read_file(path)
            .map_err(|source| PolicyStoreError::FileReadError {
                path: path.to_string(),
                source,
            })?;

        String::from_utf8(bytes).map_err(|e| PolicyStoreError::FileReadError {
            path: path.to_string(),
            source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
        })
    }

    fn check_extension(
        entry: &super::vfs_adapter::DirEntry,
        expected: &str,
    ) -> Result<(), PolicyStoreError> {
        if entry.name.to_lowercase().ends_with(expected) {
            return Ok(());
        }
        Err(ValidationError::InvalidFileExtension {
            file: entry.path.clone(),
            expected: expected.to_string(),
            actual: Path::new(&entry.name)
                .extension()
                .and_then(|s| s.to_str())
                .unwrap_or("(none)")
                .to_string(),
        }
        .into())
    }

    /// Resolve where the schema lives (or that it's absent), without any I/O
    /// beyond lightweight existence checks.
    fn resolve_schema_source(&self, file: &str, dir: &str) -> SchemaSource {
        let schema_path = self.path(file);
        if self.vfs.exists(&schema_path) {
            return SchemaSource::SingleFile { path: schema_path };
        }
        let schemas_dir = self.path(dir);
        if self.vfs.exists(&schemas_dir) {
            return SchemaSource::Directory(schemas_dir);
        }
        SchemaSource::None {
            searched_file: schema_path,
            searched_dir: schemas_dir,
        }
    }

    /// Load and parse schema from a pre-resolved [`SchemaSource`].
    /// Returns `Ok(None)` when no schema source exists.
    fn load_schema(
        &self,
        source: &SchemaSource,
        file_name: &str,
    ) -> Result<Option<ParsedSchema>, PolicyStoreError> {
        match source {
            SchemaSource::SingleFile { path, .. } => {
                let content = self.read_schema_file_content(path)?.ok_or_else(|| {
                    PolicyStoreError::Validation(ValidationError::MissingSchemaSource {
                        searched_file: path.clone(),
                        searched_dir: String::new(),
                    })
                })?;
                Ok(Some(ParsedSchema::parse(&content, file_name)?))
            },
            SchemaSource::Directory(path) => self.load_schema_from_directory(path),
            SchemaSource::None { .. } => Ok(None),
        }
    }

    /// Read the raw content of a single schema file.
    /// Returns `None` if the file does not exist (no parsing).
    fn read_schema_file_content(&self, path: &str) -> Result<Option<String>, PolicyStoreError> {
        match self.vfs.read_file(path) {
            Ok(bytes) => {
                String::from_utf8(bytes)
                    .map(Some)
                    .map_err(|e| PolicyStoreError::FileReadError {
                        path: path.to_string(),
                        source: std::io::Error::new(std::io::ErrorKind::InvalidData, e),
                    })
            },
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(None),
            Err(source) => Err(PolicyStoreError::FileReadError {
                path: path.to_string(),
                source,
            }),
        }
    }

    /// Load schema from the schema directory, combining all `.cedarschema` files.
    fn load_schema_from_directory(
        &self,
        path: &str,
    ) -> Result<Option<ParsedSchema>, PolicyStoreError> {
        if !self.vfs.exists(path) {
            return Ok(None);
        }
        if !self.vfs.is_dir(path) {
            return Err(PolicyStoreError::NotADirectory {
                path: path.to_string(),
            });
        }

        let entries =
            self.vfs
                .read_dir(path)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: path.to_string(),
                    source,
                })?;

        let raw_files = self.read_schema_files(entries)?;
        if raw_files.is_empty() {
            return Err(ValidationError::EmptySchemaDirectory {
                path: path.to_string(),
            }
            .into());
        }

        let parsed = ParsedSchema::parse_multiple(&raw_files)?;
        Ok(Some(parsed))
    }

    /// Read and validate all `.cedarschema` files from directory entries.
    fn read_schema_files(
        &self,
        entries: Vec<super::vfs_adapter::DirEntry>,
    ) -> Result<Vec<SchemaFile>, PolicyStoreError> {
        let mut files: Vec<SchemaFile> = Vec::new();
        for entry in entries {
            if entry.is_dir {
                continue;
            }
            if !entry.name.to_lowercase().ends_with(".cedarschema") {
                continue;
            }
            let content = self.read_text(&entry.path)?;
            files.push(SchemaFile {
                name: entry.name,
                content,
            });
        }
        files.sort_by(|a, b| a.name.cmp(&b.name));
        Ok(files)
    }

    /// Recursive helper to load .cedar files from a directory and its subdirectories.
    fn load_cedar_files_recursive(
        &self,
        dir: &str,
        files: &mut Vec<StoreFile>,
        depth: usize,
    ) -> Result<(), PolicyStoreError> {
        if depth > Self::MAX_RECURSION_DEPTH {
            return Err(PolicyStoreError::MaxDepthExceeded {
                path: dir.to_string(),
                max_depth: Self::MAX_RECURSION_DEPTH,
            });
        }

        let entries =
            self.vfs
                .read_dir(dir)
                .map_err(|source| PolicyStoreError::DirectoryReadError {
                    path: dir.to_string(),
                    source,
                })?;

        for entry in entries {
            if entry.is_dir {
                self.load_cedar_files_recursive(&entry.path, files, depth + 1)?;
            } else {
                Self::check_extension(&entry, ".cedar")?;
                let content = self.read_text(&entry.path)?;
                files.push(StoreFile {
                    name: entry.name,
                    content,
                });
            }
        }

        Ok(())
    }
}

#[cfg(test)]
#[path = "loader_tests.rs"]
mod tests;
