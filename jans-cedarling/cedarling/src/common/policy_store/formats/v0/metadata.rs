// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `metadata.json` parse structs for format v0.

use chrono::{DateTime, Utc};
use serde::Deserialize;

use crate::common::policy_store::errors::ValidationError;
use crate::common::policy_store::formats::datetime;

/// Root of `metadata.json`. Unknown fields are ignored, as they always were in v0.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct MetadataDoc {
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

        let doc = parse(json).expect("v0 metadata with all fields should parse");
        assert_eq!(doc.cedar_version, "4.4.0", "cedar_version mismatch");
        assert_eq!(doc.policy_store.id, "abc123def456", "id mismatch");
        assert_eq!(
            doc.policy_store.description.as_deref(),
            Some("A test policy store"),
            "description mismatch"
        );
        assert_eq!(
            doc.policy_store.updated_date,
            Some(
                DateTime::parse_from_rfc3339("2024-01-02T00:00:00Z")
                    .expect("valid timestamp")
                    .with_timezone(&Utc)
            ),
            "updated_date should be parsed as RFC 3339"
        );
    }

    #[test]
    fn ignores_unknown_fields() {
        let json = r#"{
            "policy_store_spec_version": 1,
            "cedar_version": "4.4.0",
            "extra_root": true,
            "policy_store": { "name": "Test Store", "extra_info": 1 }
        }"#;

        parse(json).expect("v0 metadata must keep ignoring unknown fields");
    }

    #[test]
    fn missing_name_is_rejected() {
        let json = r#"{ "cedar_version": "4.4.0", "policy_store": { "id": "abc123def456" } }"#;

        let err = parse(json).expect_err("v0 metadata without a name must fail");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "Expected MetadataJsonParseFailed, got: {err:?}"
        );
    }
}
