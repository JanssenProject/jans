// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Error types for policy store operations.

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
    #[error("JSON parsing error in {file}: {message}")]
    JsonParsing { file: String, message: String },

    /// YAML parsing error
    #[error("YAML parsing error in {file}: {message}")]
    YamlParsing { file: String, message: String },

    /// Cedar parsing error
    #[error("Cedar parsing error in {file}: {message}")]
    CedarParsing { file: String, message: String },
}

/// Validation errors for policy store components.
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum ValidationError {
    /// Invalid metadata
    #[error("Invalid metadata in file {file}: {message}")]
    InvalidMetadata { file: String, message: String },

    /// Invalid policy
    #[error("Invalid policy in file {file}{}: {message}", .line.map(|l| format!(" at line {}", l)).unwrap_or_default())]
    InvalidPolicy {
        file: String,
        line: Option<u32>,
        message: String,
    },

    /// Invalid template
    #[error("Invalid template in file {file}{}: {message}", .line.map(|l| format!(" at line {}", l)).unwrap_or_default())]
    InvalidTemplate {
        file: String,
        line: Option<u32>,
        message: String,
    },

    /// Invalid entity
    #[error("Invalid entity in file {file}: {message}")]
    InvalidEntity { file: String, message: String },

    /// Invalid trusted issuer
    #[error("Invalid trusted issuer in file {file}: {message}")]
    InvalidTrustedIssuer { file: String, message: String },

    /// Invalid schema
    #[error("Invalid schema in file {file}: {message}")]
    InvalidSchema { file: String, message: String },

    /// Manifest validation failed
    #[error("Manifest validation failed: {message}")]
    ManifestValidation { message: String },

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

    /// Invalid policy ID format
    #[error("Invalid policy ID format in {file}: {message}")]
    InvalidPolicyId { file: String, message: String },

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
    #[error("Policy store description too long in metadata.json: {length} chars (max 1000)")]
    DescriptionTooLong { length: usize },

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
    /// Invalid archive format
    #[error("Invalid archive format: {message}")]
    InvalidFormat { message: String },

    /// Archive extraction failed
    #[error("Failed to extract archive: {message}")]
    ExtractionFailed { message: String },

    /// Invalid archive structure
    #[error("Invalid archive structure: {message}")]
    InvalidStructure { message: String },

    /// Archive corruption detected
    #[error("Archive appears to be corrupted: {message}")]
    Corrupted { message: String },

    /// Path traversal attempt detected
    #[error("Potential path traversal detected in archive: {path}")]
    PathTraversal { path: String },
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

    /// Token signature validation failed
    #[error("Token signature validation failed for issuer {issuer}: {message}")]
    SignatureValidation { issuer: String, message: String },

    /// JWKS fetch failed
    #[error("Failed to fetch JWKS from endpoint {endpoint}: {message}")]
    JwksFetchFailed { endpoint: String, message: String },

    /// Invalid token format
    #[error("Invalid token format: {message}")]
    InvalidFormat { message: String },

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
        let err = ValidationError::InvalidMetadata {
            file: "metadata.json".to_string(),
            message: "missing field 'name'".to_string(),
        };
        assert_eq!(
            err.to_string(),
            "Invalid metadata in file metadata.json: missing field 'name'"
        );

        let err = ValidationError::InvalidPolicy {
            file: "policy1.cedar".to_string(),
            line: Some(42),
            message: "syntax error".to_string(),
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
        let err = ArchiveError::InvalidFormat {
            message: "not a zip file".to_string(),
        };
        assert_eq!(err.to_string(), "Invalid archive format: not a zip file");

        let err = ArchiveError::PathTraversal {
            path: "../../../etc/passwd".to_string(),
        };
        assert!(err.to_string().contains("path traversal"));
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
        let val_err = ValidationError::InvalidMetadata {
            file: "test.json".to_string(),
            message: "invalid".to_string(),
        };
        let ps_err: PolicyStoreError = val_err.into();
        assert!(ps_err.to_string().contains("Validation error"));
    }
}
