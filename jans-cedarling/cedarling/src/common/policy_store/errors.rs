// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Error types for policy store operations.

/// Cedar schema-specific errors.
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
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

    /// Namespace extraction failed
    #[error("Namespace extraction failed: {0}")]
    NamespaceError(String),
}

/// Cedar entity-specific errors.
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum CedarEntityErrorType {
    /// Failed to create entity
    #[error("Failed to create entity: {0}")]
    EntityCreationError(String),

    /// Failed to parse entity from JSON
    #[error("Failed to parse entity from JSON: {0}")]
    JsonParseError(String),

    /// No entity found after parsing
    #[error("No entity found after parsing")]
    NoEntityFound,

    /// Invalid entity UID format
    #[error("Invalid entity UID format: {0}")]
    InvalidUidFormat(String),

    /// Invalid entity type name
    #[error("Invalid entity type name '{0}': {1}")]
    InvalidTypeName(String, String),

    /// Invalid entity ID
    #[error("Invalid entity ID: {0}")]
    InvalidEntityId(String),

    /// Duplicate entity UID detected
    #[error("Duplicate entity UID '{uid}' found in '{file1}' and '{file2}'")]
    DuplicateUid {
        uid: String,
        file1: String,
        file2: String,
    },

    /// Parent entity not found in hierarchy
    #[error("Parent entity '{parent}' not found for entity '{child}'")]
    MissingParent { parent: String, child: String },

    /// Failed to create entity store
    #[error("Failed to create entity store: {0}")]
    EntityStoreCreation(String),
}

/// Trusted issuer-specific errors.
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
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

    /// Duplicate issuer ID detected
    #[error("Duplicate issuer ID '{issuer_id}' found in files '{file1}' and '{file2}'")]
    DuplicateIssuerId {
        issuer_id: String,
        file1: String,
        file2: String,
    },
}

/// Manifest validation-specific errors.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
#[allow(dead_code)]
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
#[allow(dead_code)]
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

    /// YAML parsing error
    #[error("YAML parsing error in '{file}'")]
    YamlParsing {
        file: String,
        #[source]
        source: Box<dyn std::error::Error + Send + Sync>,
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
    ManifestError { err: ManifestErrorType },

    /// Path not found
    #[error("Path not found: {path}")]
    PathNotFound { path: String },

    /// Path is not a directory
    #[error("Path is not a directory: {path}")]
    NotADirectory { path: String },

    /// Path is not a file
    #[error("Path is not a file: {path}")]
    NotAFile { path: String },

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

    /// Empty directory
    #[error("Directory is empty: {path}")]
    EmptyDirectory { path: String },

    /// Invalid file name
    #[error("Invalid file name in '{path}'")]
    InvalidFileName {
        path: String,
        #[source]
        source: std::io::Error,
    },

    /// Unsupported format - legacy format not supported through this loader
    #[error(
        "Unsupported format: legacy JSON/YAML format not supported through PolicyStoreLoader. Use init::policy_store::load_policy_store() instead"
    )]
    LegacyFormatNotSupported,
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
#[allow(dead_code)]
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

    /// Invalid policy - kept for compatibility but prefer specific errors
    #[error("Invalid policy in file {file}{}", .line.map(|l| format!(" at line {}", l)).unwrap_or_default())]
    InvalidPolicy { file: String, line: Option<u32> },

    /// Invalid template - kept for compatibility but prefer specific errors
    #[error("Invalid template in file {file}{}", .line.map(|l| format!(" at line {}", l)).unwrap_or_default())]
    InvalidTemplate { file: String, line: Option<u32> },

    /// Invalid entity - kept for compatibility
    #[error("Invalid entity in file {file}")]
    InvalidEntity { file: String },

    /// Invalid trusted issuer - kept for compatibility
    #[error("Invalid trusted issuer in file {file}")]
    InvalidTrustedIssuer { file: String },

    /// Invalid schema - kept for compatibility
    #[error("Invalid schema in file {file}")]
    InvalidSchema { file: String },

    /// Manifest validation failed - kept for compatibility
    #[error("Manifest validation failed")]
    ManifestValidation,

    /// File checksum mismatch
    #[error("Checksum mismatch for file {file}: expected {expected}, got {actual}")]
    ChecksumMismatch {
        file: String,
        expected: String,
        actual: String,
    },

    /// Missing required file
    #[error("Missing required file: {file}")]
    MissingRequiredFile { file: String },

    /// Missing required directory
    #[error("Missing required directory: {directory}")]
    MissingRequiredDirectory { directory: String },

    /// Duplicate entity UID
    #[error("Duplicate entity UID found: {uid} in files {file1} and {file2}")]
    DuplicateEntityUid {
        uid: String,
        file1: String,
        file2: String,
    },

    /// Missing @id() annotation
    #[error("Missing @id() annotation in {file}: {policy_type} must have an @id() annotation")]
    MissingIdAnnotation { file: String, policy_type: String },

    /// Invalid file extension
    #[error("Invalid file extension for {file}: expected {expected}, got {actual}")]
    InvalidFileExtension {
        file: String,
        expected: String,
        actual: String,
    },

    /// Duplicate policy ID
    #[error("Duplicate policy ID '{policy_id}' found in files {file1} and {file2}")]
    DuplicatePolicyId {
        policy_id: String,
        file1: String,
        file2: String,
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
#[allow(dead_code)]
pub enum ArchiveError {
    /// Invalid file extension (expected .cjar)
    #[error("Invalid file extension: expected '{expected}', found '{found}'")]
    InvalidExtension { expected: String, found: String },

    /// Cannot read archive file
    #[error("Cannot read archive file '{path}': {source}")]
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

    /// File path-based archive loading not supported in WASM
    #[error(
        "File path-based archive loading is not supported in WASM. Use ArchiveSource::Url for remote archives, or create an ArchiveVfs::from_buffer() directly with bytes you fetch. See module documentation for examples."
    )]
    WasmUnsupported,
}

/// Errors related to JWT token validation.
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum TokenError {
    /// Token from untrusted issuer
    #[error("Token from untrusted issuer: {issuer}")]
    UntrustedIssuer { issuer: String },

    /// Missing required claim
    #[error("Missing required claim '{claim}' in token from issuer {issuer}")]
    MissingRequiredClaim { claim: String, issuer: String },

    /// Token signature validation failed - invalid signature
    #[error("Token signature validation failed for issuer {issuer}: invalid signature")]
    InvalidSignature { issuer: String },

    /// Token signature validation failed - no matching key
    #[error("Token signature validation failed for issuer {issuer}: no matching key found")]
    NoMatchingKey { issuer: String },

    /// Token signature validation failed - algorithm mismatch
    #[error("Token signature validation failed for issuer {issuer}: algorithm mismatch")]
    AlgorithmMismatch { issuer: String },

    /// JWKS fetch failed - network error
    #[error("Failed to fetch JWKS from endpoint {endpoint}: network error")]
    JwksFetchNetworkError { endpoint: String },

    /// JWKS fetch failed - invalid response
    #[error("Failed to fetch JWKS from endpoint {endpoint}: invalid response format")]
    JwksFetchInvalidResponse { endpoint: String },

    /// Invalid token format - missing header
    #[error("Invalid token format: missing header")]
    MissingHeader,

    /// Invalid token format - malformed JWT
    #[error("Invalid token format: malformed JWT structure")]
    MalformedJwt,

    /// Token expired
    #[error("Token has expired")]
    Expired,

    /// Token not yet valid
    #[error("Token is not yet valid")]
    NotYetValid,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_validation_error_messages() {
        let err = ValidationError::EmptyPolicyId {
            file: "policy.cedar".to_string(),
        };
        assert!(err.to_string().contains("Policy ID cannot be empty"));
        assert!(err.to_string().contains("policy.cedar"));

        let err = ValidationError::InvalidPolicy {
            file: "policy1.cedar".to_string(),
            line: Some(42),
        };
        assert!(err.to_string().contains("policy1.cedar"));
        assert!(err.to_string().contains("at line 42"));

        let err = ValidationError::MissingRequiredFile {
            file: "schema.cedarschema".to_string(),
        };
        assert_eq!(err.to_string(), "Missing required file: schema.cedarschema");
    }

    #[test]
    fn test_archive_error_messages() {
        let err = ArchiveError::InvalidZipFormat {
            details: "not a zip file".to_string(),
        };
        assert_eq!(
            err.to_string(),
            "Invalid ZIP archive format: not a zip file"
        );

        let err = ArchiveError::PathTraversal {
            path: "../../../etc/passwd".to_string(),
        };
        assert!(err.to_string().contains("Path traversal"));
        assert!(err.to_string().contains("../../../etc/passwd"));
    }

    #[test]
    fn test_token_error_messages() {
        let err = TokenError::UntrustedIssuer {
            issuer: "https://evil.com".to_string(),
        };
        assert_eq!(
            err.to_string(),
            "Token from untrusted issuer: https://evil.com"
        );

        let err = TokenError::MissingRequiredClaim {
            claim: "sub".to_string(),
            issuer: "https://issuer.com".to_string(),
        };
        assert!(err.to_string().contains("sub"));
        assert!(err.to_string().contains("https://issuer.com"));
    }

    #[test]
    fn test_policy_store_error_from_io() {
        let io_err = std::io::Error::new(std::io::ErrorKind::NotFound, "file not found");
        let ps_err: PolicyStoreError = io_err.into();
        assert!(ps_err.to_string().contains("IO error"));
    }

    #[test]
    fn test_policy_store_error_from_validation() {
        let val_err = ValidationError::EmptyPolicyId {
            file: "test.cedar".to_string(),
        };
        let ps_err: PolicyStoreError = val_err.into();
        assert!(ps_err.to_string().contains("Validation error"));
    }
}
