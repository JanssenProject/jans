// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Error types for policy store operations.

/// Cedar schema-specific errors.
#[derive(Debug, thiserror::Error)]
pub enum CedarSchemaErrorType {
    /// Schema file is empty
    #[error("Schema file is empty")]
    EmptySchema,

    /// Schema parsing failed
    #[error("Schema parsing failed: {0}")]
    ParseError(String),

    /// Schema validation failed
    #[error("Schema validation failed: {0}")]
    ValidationError(String),
}

/// Cedar entity-specific errors.
#[derive(Debug, thiserror::Error)]
pub enum CedarEntityErrorType {
    /// Failed to parse entity from JSON
    #[error("Failed to parse entity from JSON: {0}")]
    JsonParseError(String),

    /// Invalid entity type name
    #[error("Invalid entity type name '{0}': {1}")]
    InvalidTypeName(String, String),

    /// Invalid entity ID
    #[error("Invalid entity ID: {0}")]
    InvalidEntityId(String),

    /// Failed to create entity store
    #[error("Failed to create entity store: {0}")]
    EntityStoreCreation(String),
}

/// Trusted issuer-specific errors.
#[derive(Debug, thiserror::Error)]
pub enum TrustedIssuerErrorType {
    /// Trusted issuer file is not a JSON object
    #[error("Trusted issuer file must be a JSON object")]
    NotAnObject,

    /// Issuer configuration is not an object
    #[error("Issuer '{issuer_id}' must be a JSON object")]
    IssuerNotAnObject { issuer_id: String },

    /// Missing required field in issuer configuration
    #[error("Issuer '{issuer_id}': missing required field '{field}'")]
    MissingRequiredField { issuer_id: String, field: String },

    /// Invalid OIDC endpoint URL
    #[error("Issuer '{issuer_id}': invalid OIDC endpoint URL '{url}': {reason}")]
    InvalidOidcEndpoint {
        issuer_id: String,
        url: String,
        reason: String,
    },

    /// Token metadata is not an object
    #[error("Issuer '{issuer_id}': token_metadata must be a JSON object")]
    TokenMetadataNotAnObject { issuer_id: String },

    /// Token metadata entry is not an object
    #[error("Issuer '{issuer_id}': token_metadata.{token_type} must be a JSON object")]
    TokenMetadataEntryNotAnObject {
        issuer_id: String,
        token_type: String,
    },
}

/// Manifest validation-specific errors.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
#[cfg(not(target_arch = "wasm32"))]
pub enum ManifestErrorType {
    /// Manifest file not found
    #[error("Manifest file not found (manifest.json is required for integrity validation)")]
    ManifestNotFound,

    /// Manifest parsing failed
    #[error("Failed to parse manifest: {0}")]
    ParseError(String),

    /// File listed in manifest is missing from policy store
    #[error("File '{file}' is listed in manifest but not found in policy store")]
    FileMissing { file: String },

    /// Error reading file from policy store
    #[error("Failed to read file '{file}': {error_message}")]
    FileReadError { file: String, error_message: String },

    /// File checksum mismatch
    #[error("Checksum mismatch for '{file}': expected '{expected}', computed '{actual}'")]
    ChecksumMismatch {
        file: String,
        expected: String,
        actual: String,
    },

    /// Invalid checksum format
    #[error("Invalid checksum format for '{file}': expected 'sha256:<hex>', found '{checksum}'")]
    InvalidChecksumFormat { file: String, checksum: String },

    /// File size mismatch
    #[error("Size mismatch for '{file}': expected {expected} bytes, found {actual} bytes")]
    SizeMismatch {
        file: String,
        expected: u64,
        actual: u64,
    },

    /// Policy store ID mismatch
    #[error("Policy store ID mismatch: manifest expects '{expected}', metadata has '{actual}'")]
    PolicyStoreIdMismatch { expected: String, actual: String },
}

/// Errors that can occur during policy store operations.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreError {
    /// IO error during file operations
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    /// Validation error
    #[error("Validation error: {0}")]
    Validation(#[from] ValidationError),

    /// Archive handling error
    #[error("Archive error: {0}")]
    Archive(#[from] ArchiveError),

    /// JSON parsing error
    #[error("JSON parsing error in '{file}'")]
    JsonParsing {
        file: String,
        #[source]
        source: serde_json::Error,
    },

    /// Cedar parsing error
    #[error("Cedar parsing error in '{file}': {detail}")]
    CedarParsing {
        file: String,
        detail: CedarParseErrorDetail,
    },

    /// Cedar schema error
    #[error("Cedar schema error in '{file}': {err}")]
    CedarSchemaError {
        file: String,
        err: CedarSchemaErrorType,
    },

    /// Cedar entity error
    #[error("Cedar entity error in '{file}': {err}")]
    CedarEntityError {
        file: String,
        err: CedarEntityErrorType,
    },

    /// Trusted issuer error
    #[error("Trusted issuer error in '{file}': {err}")]
    TrustedIssuerError {
        file: String,
        err: TrustedIssuerErrorType,
    },

    /// Manifest validation error
    #[error("Manifest validation error: {err}")]
    #[cfg(not(target_arch = "wasm32"))]
    ManifestError { err: ManifestErrorType },

    /// Path not found
    #[error("Path not found: {path}")]
    PathNotFound { path: String },

    /// Path is not a directory
    #[error("Path is not a directory: {path}")]
    NotADirectory { path: String },

    /// Directory read error
    #[error("Failed to read directory '{path}'")]
    DirectoryReadError {
        path: String,
        #[source]
        source: std::io::Error,
    },

    /// File read error
    #[error("Failed to read file '{path}'")]
    FileReadError {
        path: String,
        #[source]
        source: std::io::Error,
    },
}

/// Details about Cedar parsing errors.
#[derive(Debug, Clone, thiserror::Error)]
pub enum CedarParseErrorDetail {
    /// Missing @id() annotation
    #[error("No @id() annotation found and could not derive ID from filename")]
    MissingIdAnnotation,

    /// Failed to parse Cedar policy or template
    #[error("{0}")]
    ParseError(String),

    /// Failed to add policy to policy set
    #[error("Failed to add policy to set: {0}")]
    AddPolicyFailed(String),

    /// Failed to add template to policy set
    #[error("Failed to add template to set: {0}")]
    AddTemplateFailed(String),
}

/// Validation errors for policy store components.
#[derive(Debug, thiserror::Error)]
pub enum ValidationError {
    /// Failed to parse metadata JSON
    #[error("Invalid metadata in file {file}: failed to parse JSON")]
    MetadataJsonParseFailed {
        file: String,
        #[source]
        source: serde_json::Error,
    },

    /// Invalid cedar version format in metadata
    #[error("Invalid metadata in file {file}: invalid cedar_version format")]
    MetadataInvalidCedarVersion {
        file: String,
        #[source]
        source: semver::Error,
    },

    /// Missing required file
    #[error("Missing required file: {file}")]
    MissingRequiredFile { file: String },

    /// Missing required directory
    #[error("Missing required directory: {directory}")]
    MissingRequiredDirectory { directory: String },

    /// Invalid file extension
    #[error("Invalid file extension for {file}: expected {expected}, got {actual}")]
    InvalidFileExtension {
        file: String,
        expected: String,
        actual: String,
    },

    /// Policy ID is empty
    #[error("Invalid policy ID format in {file}: Policy ID cannot be empty")]
    EmptyPolicyId { file: String },

    /// Policy ID contains invalid characters
    #[error(
        "Invalid policy ID format in {file}: Policy ID '{id}' contains invalid characters. Only alphanumeric, '_', '-', and ':' are allowed"
    )]
    InvalidPolicyIdCharacters { file: String, id: String },

    // Specific metadata validation errors
    /// Empty Cedar version
    #[error("Cedar version cannot be empty in metadata.json")]
    EmptyCedarVersion,

    /// Invalid Cedar version format
    #[error("Invalid Cedar version format in metadata.json: '{version}' - {details}")]
    InvalidCedarVersion { version: String, details: String },

    /// Empty policy store name
    #[error("Policy store name cannot be empty in metadata.json")]
    EmptyPolicyStoreName,

    /// Policy store name too long
    #[error("Policy store name too long in metadata.json: {length} chars (max 255)")]
    PolicyStoreNameTooLong { length: usize },

    /// Invalid policy store ID format
    #[error(
        "Invalid policy store ID format in metadata.json: '{id}' must be hexadecimal (8-64 chars)"
    )]
    InvalidPolicyStoreId { id: String },

    /// Invalid policy store version
    #[error("Invalid policy store version in metadata.json: '{version}' - {details}")]
    InvalidPolicyStoreVersion { version: String, details: String },

    /// Policy store description too long
    #[error(
        "Policy store description too long in metadata.json: {length} chars (max {max_length})"
    )]
    DescriptionTooLong { length: usize, max_length: usize },

    /// Invalid timestamp ordering
    #[error(
        "Invalid timestamp ordering in metadata.json: updated_date cannot be before created_date"
    )]
    InvalidTimestampOrdering,
}

/// Errors related to archive (.cjar) handling.
#[derive(Debug, thiserror::Error)]
pub enum ArchiveError {
    /// Invalid file extension (expected .cjar)
    #[error("Invalid file extension: expected '{expected}', found '{found}'")]
    #[cfg(not(target_arch = "wasm32"))]
    InvalidExtension { expected: String, found: String },

    /// Cannot read archive file
    #[error("Cannot read archive file '{path}': {source}")]
    #[cfg(not(target_arch = "wasm32"))]
    CannotReadFile {
        path: String,
        #[source]
        source: std::io::Error,
    },

    /// Invalid ZIP format
    #[error("Invalid ZIP archive format: {details}")]
    InvalidZipFormat { details: String },

    /// Corrupted archive entry
    #[error("Corrupted archive entry at index {index}: {details}")]
    CorruptedEntry { index: usize, details: String },

    /// Path traversal attempt detected
    #[error("Path traversal attempt detected in archive: '{path}'")]
    PathTraversal { path: String },

    /// Unsupported operation on this platform
    #[cfg(target_arch = "wasm32")]
    #[error("Archive operations are not supported on this platform")]
    WasmUnsupported,
}
