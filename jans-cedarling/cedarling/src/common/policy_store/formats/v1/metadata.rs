// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `metadata.json` parse structs for format v1.

use chrono::{DateTime, Utc};
use serde::Deserialize;

use crate::common::policy_store::errors::ValidationError;
use crate::common::policy_store::metadata::{PolicyStoreInfo, PolicyStoreMetadata};
use crate::common::policy_store::validator::MetadataValidator;

/// Deserializes an optional RFC 3339 timestamp.
mod datetime_option {
    use chrono::{DateTime, Utc};
    use serde::{Deserialize, Deserializer};

    pub(super) fn deserialize<'de, D>(deserializer: D) -> Result<Option<DateTime<Utc>>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let opt: Option<String> = Option::deserialize(deserializer)?;
        match opt {
            Some(s) => DateTime::parse_from_rfc3339(&s)
                .map(|dt| Some(dt.with_timezone(&Utc)))
                .map_err(serde::de::Error::custom),
            None => Ok(None),
        }
    }
}

/// Root of `metadata.json`. Unknown fields are ignored, as they always were in v1.
#[derive(Debug, Deserialize)]
struct MetadataDoc {
    cedar_version: String,
    policy_store: PolicyStoreInfoDoc,
}

#[derive(Debug, Deserialize)]
struct PolicyStoreInfoDoc {
    #[serde(default)]
    id: String,
    name: String,
    #[serde(default)]
    description: Option<String>,
    #[serde(default)]
    version: String,
    #[serde(default, with = "datetime_option")]
    created_date: Option<DateTime<Utc>>,
    #[serde(default, with = "datetime_option")]
    updated_date: Option<DateTime<Utc>>,
}

impl From<MetadataDoc> for PolicyStoreMetadata {
    fn from(doc: MetadataDoc) -> Self {
        let info = doc.policy_store;
        Self {
            cedar_version: doc.cedar_version,
            policy_store: PolicyStoreInfo {
                id: info.id,
                name: info.name,
                description: info.description,
                version: info.version,
                created_date: info.created_date,
                updated_date: info.updated_date,
            },
        }
    }
}

/// Parses `metadata.json` content and validates the result.
pub(crate) fn parse(json: &str) -> Result<PolicyStoreMetadata, ValidationError> {
    let doc: MetadataDoc =
        serde_json::from_str(json).map_err(|e| ValidationError::MetadataJsonParseFailed {
            file: "metadata.json".to_string(),
            source: e,
        })?;

    let metadata = PolicyStoreMetadata::from(doc);
    MetadataValidator::validate(&metadata)?;
    Ok(metadata)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_all_fields() {
        let json = r#"{
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

        let metadata = parse(json).expect("metadata with all fields should parse");
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
    fn ignores_unknown_fields() {
        let json = r#"{
            "cedar_version": "4.4.0",
            "extra_root": true,
            "policy_store": { "name": "Test Store", "extra_info": 1 }
        }"#;

        parse(json).expect("v1 metadata must keep ignoring unknown fields");
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

        let metadata = parse(json).expect("valid metadata should parse");
        assert_eq!(metadata.cedar_version, "4.4.0", "cedar_version mismatch");
        assert_eq!(metadata.policy_store.name, "Test Store", "name mismatch");
    }

    #[test]
    fn test_parse_and_validate_invalid_json() {
        let json = r"{ invalid json }";

        let err = parse(json).expect_err("Should fail on invalid JSON");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
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

        let err = parse(json).expect_err("Should fail on missing required field");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
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

        let err = parse(json).expect_err("Should fail on empty name validation");
        assert!(
            matches!(err, ValidationError::EmptyPolicyStoreName),
            "Expected EmptyPolicyStoreName, got: {err:?}"
        );
    }
}
