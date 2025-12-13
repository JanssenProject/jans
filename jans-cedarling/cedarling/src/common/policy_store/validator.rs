// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store metadata validation and parsing.

use super::errors::ValidationError;
use super::metadata::{PolicyStoreInfo, PolicyStoreMetadata};
use semver::Version;

/// Maximum allowed length for policy store description.
pub const DESCRIPTION_MAX_LENGTH: usize = 1000;

/// Validator for policy store metadata.
pub struct MetadataValidator;

impl MetadataValidator {
    /// Validate a PolicyStoreMetadata structure.
    ///
    /// Checks:
    /// - Cedar version format is valid
    /// - Policy store name is not empty
    /// - Policy store version is valid semantic version (if provided)
    /// - Policy store ID format is valid (if provided)
    pub fn validate(metadata: &PolicyStoreMetadata) -> Result<(), ValidationError> {
        // Validate cedar_version
        Self::validate_cedar_version(&metadata.cedar_version)?;

        // Validate policy_store fields
        Self::validate_policy_store_info(&metadata.policy_store)?;

        Ok(())
    }

    /// Validate Cedar version string.
    ///
    /// Expected format: "X.Y.Z" where X, Y, Z are integers
    /// Examples: "4.4.0", "3.0.1", "4.2.5"
    fn validate_cedar_version(version: &str) -> Result<(), ValidationError> {
        if version.is_empty() {
            return Err(ValidationError::EmptyCedarVersion);
        }

        // Parse as semantic version
        Version::parse(version).map_err(|e| ValidationError::InvalidCedarVersion {
            version: version.to_string(),
            details: e.to_string(),
        })?;

        Ok(())
    }

    /// Validate policy store info fields.
    fn validate_policy_store_info(info: &PolicyStoreInfo) -> Result<(), ValidationError> {
        // Validate name (required)
        if info.name.is_empty() {
            return Err(ValidationError::EmptyPolicyStoreName);
        }

        // Validate name length (reasonable limit)
        if info.name.len() > 255 {
            return Err(ValidationError::PolicyStoreNameTooLong {
                length: info.name.len(),
            });
        }

        // Validate ID format if provided (should be hex string or empty)
        if !info.id.is_empty() {
            Self::validate_policy_store_id(&info.id)?;
        }

        // Validate version format if provided (should be semantic version)
        if !info.version.is_empty() {
            Self::validate_policy_store_version(&info.version)?;
        }

        // Validate description length if provided
        if let Some(desc) = &info.description
            && desc.len() > DESCRIPTION_MAX_LENGTH
        {
            return Err(ValidationError::DescriptionTooLong {
                length: desc.len(),
                max_length: DESCRIPTION_MAX_LENGTH,
            });
        }

        // Validate timestamps ordering if both are provided
        if let (Some(created), Some(updated)) = (info.created_date, info.updated_date)
            && updated < created
        {
            return Err(ValidationError::InvalidTimestampOrdering);
        }

        Ok(())
    }

    /// Validate policy store ID format.
    ///
    /// Expected: Hexadecimal string (lowercase or uppercase)
    /// Examples: "abc123", "ABC123", "0123456789abcdef"
    fn validate_policy_store_id(id: &str) -> Result<(), ValidationError> {
        // Check if all characters are valid hex and length is 8-64 chars
        if !id.chars().all(|c| c.is_ascii_hexdigit()) || id.len() < 8 || id.len() > 64 {
            return Err(ValidationError::InvalidPolicyStoreId { id: id.to_string() });
        }

        Ok(())
    }

    /// Validate policy store version.
    ///
    /// Expected: Semantic version (X.Y.Z or X.Y.Z-prerelease+build)
    /// Examples: "1.0.0", "2.1.3", "1.0.0-alpha", "1.0.0-beta.1+build.123"
    fn validate_policy_store_version(version: &str) -> Result<(), ValidationError> {
        Version::parse(version).map_err(|e| ValidationError::InvalidPolicyStoreVersion {
            version: version.to_string(),
            details: e.to_string(),
        })?;

        Ok(())
    }

    /// Parse and validate metadata from JSON string.
    pub fn parse_and_validate(json: &str) -> Result<PolicyStoreMetadata, ValidationError> {
        // Parse JSON
        let metadata: PolicyStoreMetadata =
            serde_json::from_str(json).map_err(|e| ValidationError::MetadataJsonParseFailed {
                file: "metadata.json".to_string(),
                source: e,
            })?;

        // Validate
        Self::validate(&metadata)?;

        Ok(metadata)
    }
}

/// Accessor methods for policy store metadata.
impl PolicyStoreMetadata {
    /// Get the Cedar version.
    pub fn cedar_version(&self) -> &str {
        &self.cedar_version
    }

    /// Get the policy store ID.
    pub fn id(&self) -> &str {
        &self.policy_store.id
    }

    /// Get the policy store name.
    pub fn name(&self) -> &str {
        &self.policy_store.name
    }

    /// Get the policy store description.
    pub fn description(&self) -> Option<&str> {
        self.policy_store.description.as_deref()
    }

    /// Get the policy store version.
    pub fn version(&self) -> &str {
        &self.policy_store.version
    }

    /// Get the policy store version as a parsed semantic version.
    pub fn version_parsed(&self) -> Option<Version> {
        Version::parse(&self.policy_store.version).ok()
    }

    /// Get the policy store created date.
    pub fn created_date(&self) -> Option<chrono::DateTime<chrono::Utc>> {
        self.policy_store.created_date
    }

    /// Get the policy store updated date.
    pub fn updated_date(&self) -> Option<chrono::DateTime<chrono::Utc>> {
        self.policy_store.updated_date
    }

    /// Check if this policy store is compatible with a given Cedar version.
    pub fn is_compatible_with_cedar(
        &self,
        required_version: &Version,
    ) -> Result<bool, ValidationError> {
        let store_version = Version::parse(&self.cedar_version).map_err(|e| {
            ValidationError::MetadataInvalidCedarVersion {
                file: "metadata.json".to_string(),
                source: e,
            }
        })?;

        // Check if the store version is compatible with the required version
        if store_version.major == required_version.major {
            return Ok(store_version >= *required_version);
        }

        Ok(false)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::{DateTime, Utc};

    #[test]
    fn test_validate_valid_metadata() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "abc123def456".to_string(),
                name: "Test Policy Store".to_string(),
                description: Some("A test policy store".to_string()),
                version: "1.0.0".to_string(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_minimal_metadata() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.0.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Minimal Store".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_empty_cedar_version() {
        let metadata = PolicyStoreMetadata {
            cedar_version: String::new(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::EmptyCedarVersion
        ));
    }

    #[test]
    fn test_validate_invalid_cedar_version() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "invalid.version".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::InvalidCedarVersion { .. }
        ));
    }

    #[test]
    fn test_validate_empty_name() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: String::new(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::EmptyPolicyStoreName
        ));
    }

    #[test]
    fn test_validate_name_too_long() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "a".repeat(256),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::PolicyStoreNameTooLong { length: 256 }
        ));
    }

    #[test]
    fn test_validate_invalid_id_format() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "invalid-id-with-dashes".to_string(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::InvalidPolicyStoreId { .. }
        ));
    }

    #[test]
    fn test_validate_id_too_short() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "abc123".to_string(), // Only 6 chars, need 8+
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::InvalidPolicyStoreId { .. }
        ));
    }

    #[test]
    fn test_validate_valid_hex_id() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "0123456789abcdef".to_string(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_invalid_version() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: "not.a.version".to_string(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::InvalidPolicyStoreVersion { .. }
        ));
    }

    #[test]
    fn test_validate_valid_semver() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: "1.2.3-alpha.1+build.456".to_string(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_description_too_long() {
        let over_limit = DESCRIPTION_MAX_LENGTH + 1;
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: Some("a".repeat(over_limit)),
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::DescriptionTooLong { length, max_length }
                if length == over_limit && max_length == DESCRIPTION_MAX_LENGTH
        ));
    }

    #[test]
    fn test_validate_timestamp_ordering() {
        let created = DateTime::parse_from_rfc3339("2024-01-02T00:00:00Z")
            .unwrap()
            .with_timezone(&Utc);
        let updated = DateTime::parse_from_rfc3339("2024-01-01T00:00:00Z")
            .unwrap()
            .with_timezone(&Utc);

        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: Some(created),
                updated_date: Some(updated),
            },
        };

        let result = MetadataValidator::validate(&metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            ValidationError::InvalidTimestampOrdering
        ));
    }

    #[test]
    fn test_parse_and_validate_valid_json() {
        let json = r#"{
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": "Test Store",
                "version": "1.0.0"
            }
        }"#;

        let result = MetadataValidator::parse_and_validate(json);
        assert!(result.is_ok());
        let metadata = result.unwrap();
        assert_eq!(metadata.cedar_version, "4.4.0");
        assert_eq!(metadata.policy_store.name, "Test Store");
    }

    #[test]
    fn test_parse_and_validate_invalid_json() {
        let json = r#"{ invalid json }"#;

        let result = MetadataValidator::parse_and_validate(json);
        let err = result.expect_err("Should fail on invalid JSON");
        assert!(matches!(
            err,
            ValidationError::MetadataJsonParseFailed { .. }
        ));
    }

    #[test]
    fn test_parse_and_validate_missing_required_field() {
        // Missing the 'name' field entirely - should fail during JSON deserialization
        let json = r#"{
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456"
            }
        }"#;

        let result = MetadataValidator::parse_and_validate(json);
        let err = result.expect_err("Should fail on missing required field");
        assert!(matches!(
            err,
            ValidationError::MetadataJsonParseFailed { .. }
        ));
    }

    #[test]
    fn test_parse_and_validate_empty_name_validation() {
        // Empty name field - should pass JSON parsing but fail validation
        let json = r#"{
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": ""
            }
        }"#;

        let result = MetadataValidator::parse_and_validate(json);
        let err = result.expect_err("Should fail on empty name validation");
        assert!(matches!(err, ValidationError::EmptyPolicyStoreName));
    }

    #[test]
    fn test_accessor_methods() {
        let created = DateTime::parse_from_rfc3339("2024-01-01T00:00:00Z")
            .unwrap()
            .with_timezone(&Utc);

        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "abc123def456".to_string(),
                name: "Test Store".to_string(),
                description: Some("Test description".to_string()),
                version: "1.2.3".to_string(),
                created_date: Some(created),
                updated_date: None,
            },
        };

        assert_eq!(metadata.cedar_version(), "4.4.0");
        assert_eq!(metadata.id(), "abc123def456");
        assert_eq!(metadata.name(), "Test Store");
        assert_eq!(metadata.description(), Some("Test description"));
        assert_eq!(metadata.version(), "1.2.3");
        assert!(metadata.version_parsed().is_some());
        assert_eq!(metadata.version_parsed().unwrap().to_string(), "1.2.3");
        assert!(metadata.created_date().is_some());
        assert!(metadata.updated_date().is_none());
    }

    #[test]
    fn test_cedar_version_compatibility_same_version() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let is_compatible = metadata
            .is_compatible_with_cedar(&Version::new(4, 4, 0))
            .expect("Should successfully check compatibility");
        assert!(is_compatible);
    }

    #[test]
    fn test_cedar_version_compatibility_newer_minor() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.5.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let is_compatible = metadata
            .is_compatible_with_cedar(&Version::new(4, 4, 0))
            .expect("Should successfully check compatibility");
        assert!(is_compatible);
    }

    #[test]
    fn test_cedar_version_compatibility_different_major() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let is_compatible = metadata
            .is_compatible_with_cedar(&Version::new(3, 0, 0))
            .expect("Should successfully check compatibility");
        assert!(!is_compatible);
    }

    #[test]
    fn test_cedar_version_compatibility_older_minor() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.3.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: String::new(),
                name: "Test".to_string(),
                description: None,
                version: String::new(),
                created_date: None,
                updated_date: None,
            },
        };

        let is_compatible = metadata
            .is_compatible_with_cedar(&Version::new(4, 4, 0))
            .expect("Should successfully check compatibility");
        assert!(!is_compatible);
    }
}
