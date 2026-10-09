// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `metadata.json` parse structs for format v2.

use chrono::{DateTime, Utc};
use serde::Deserialize;

use crate::common::policy_store::errors::ValidationError;
use crate::common::policy_store::formats::datetime;
use crate::common::policy_store::metadata::{PolicyStoreInfo, PolicyStoreMetadata};
use crate::common::policy_store::validator::MetadataValidator;

/// Root of `metadata.json`. Same as v1 plus the required spec version;
/// unknown fields are still ignored.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct MetadataDoc {
    pub(crate) policy_store_spec_version: u32,
    pub(crate) cedar_version: String,
    pub(crate) policy_store: PolicyStoreInfoDoc,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct PolicyStoreInfoDoc {
    #[serde(default)]
    pub(crate) id: String,
    pub(crate) name: String,
    #[serde(default)]
    pub(crate) description: Option<String>,
    #[serde(default)]
    pub(crate) version: String,
    #[serde(default, deserialize_with = "datetime::deserialize_option")]
    pub(crate) created_date: Option<DateTime<Utc>>,
    #[serde(default, deserialize_with = "datetime::deserialize_option")]
    pub(crate) updated_date: Option<DateTime<Utc>>,
}

/// Parses `metadata.json` content.
pub(crate) fn parse(json: &str) -> Result<MetadataDoc, ValidationError> {
    serde_json::from_str(json).map_err(|e| ValidationError::MetadataJsonParseFailed {
        file: "metadata.json".to_string(),
        source: e,
    })
}

/// Converts parsed metadata into the runtime type and validates it.
pub(crate) fn into_runtime(doc: MetadataDoc) -> Result<PolicyStoreMetadata, ValidationError> {
    let info = doc.policy_store;
    let metadata = PolicyStoreMetadata {
        cedar_version: doc.cedar_version,
        policy_store: PolicyStoreInfo {
            id: info.id,
            name: info.name,
            description: info.description,
            version: info.version,
            created_date: info.created_date,
            updated_date: info.updated_date,
        },
    };
    MetadataValidator::validate(&metadata)?;
    Ok(metadata)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn parse_and_convert(json: &str) -> Result<PolicyStoreMetadata, ValidationError> {
        parse(json).and_then(into_runtime)
    }

    #[test]
    fn parses_all_fields() {
        let json = r#"{
            "policy_store_spec_version": 2,
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": "test_store",
                "description": "A test policy store",
                "version": "1.0.0",
                "created_date": "2024-01-01T00:00:00Z",
                "updated_date": "2024-01-02T00:00:00Z"
            }
        }"#;

        let created = DateTime::parse_from_rfc3339("2024-01-01T00:00:00Z")
            .expect("valid timestamp")
            .with_timezone(&Utc);
        let updated = DateTime::parse_from_rfc3339("2024-01-02T00:00:00Z")
            .expect("valid timestamp")
            .with_timezone(&Utc);

        let metadata = parse_and_convert(json).expect("metadata with all fields should parse");
        assert_eq!(
            metadata,
            PolicyStoreMetadata {
                cedar_version: "4.4.0".to_string(),
                policy_store: PolicyStoreInfo {
                    id: "abc123def456".to_string(),
                    name: "test_store".to_string(),
                    description: Some("A test policy store".to_string()),
                    version: "1.0.0".to_string(),
                    created_date: Some(created),
                    updated_date: Some(updated),
                },
            },
            "every metadata.json field should map onto the runtime metadata"
        );
    }

    #[test]
    fn requires_spec_version() {
        let json = r#"{
            "cedar_version": "4.4.0",
            "policy_store": { "name": "Test Store" }
        }"#;

        let err = parse(json).expect_err("v2 metadata without the spec version must fail");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
    }

    #[test]
    fn ignores_unknown_fields() {
        let json = r#"{
            "policy_store_spec_version": 2,
            "cedar_version": "4.4.0",
            "extra_root": true,
            "policy_store": { "name": "Test Store", "extra_info": 1 }
        }"#;

        parse_and_convert(json).expect("v2 metadata must keep ignoring unknown fields");
    }

    #[test]
    fn test_parse_and_validate_valid_json() {
        let json = r#"{
            "policy_store_spec_version": 2,
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": "Test Store",
                "version": "1.0.0"
            }
        }"#;

        let metadata = parse_and_convert(json).expect("valid metadata should parse");
        assert_eq!(metadata.cedar_version, "4.4.0", "cedar_version mismatch");
        assert_eq!(metadata.policy_store.name, "Test Store", "name mismatch");
    }

    #[test]
    fn test_parse_and_validate_invalid_json() {
        let json = r"{ invalid json }";

        let err = parse_and_convert(json).expect_err("Should fail on invalid JSON");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_and_validate_missing_required_field() {
        // Missing the 'name' field entirely - should fail during JSON deserialization
        let json = r#"{
            "policy_store_spec_version": 2,
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456"
            }
        }"#;

        let err = parse_and_convert(json).expect_err("Should fail on missing required field");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_and_validate_empty_name_validation() {
        // Empty name field - should pass JSON parsing but fail validation
        let json = r#"{
            "policy_store_spec_version": 2,
            "cedar_version": "4.4.0",
            "policy_store": {
                "id": "abc123def456",
                "name": ""
            }
        }"#;

        let err = parse_and_convert(json).expect_err("Should fail on empty name validation");
        assert!(
            matches!(err, ValidationError::EmptyPolicyStoreName),
            "Expected EmptyPolicyStoreName, got: {err:?}"
        );
    }
}
