// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `metadata.json` parse structs for format v1.

use chrono::{DateTime, Utc};
use serde::Deserialize;

use crate::common::policy_store::errors::ValidationError;
use crate::common::policy_store::formats::datetime;

/// Root of `metadata.json`. Same as v0 plus the required spec version;
/// unknown fields are still ignored.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct MetadataDoc {
    /// Dispatch already matched this against [`SPEC_VERSION`](super::SPEC_VERSION),
    /// so nothing reads it back outside tests.
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_all_fields() {
        let json = r#"{
            "policy_store_spec_version": 1,
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
        let timestamp = |value: &str| {
            DateTime::parse_from_rfc3339(value)
                .expect("valid timestamp")
                .with_timezone(&Utc)
        };

        assert_eq!(
            parse(json).expect("metadata with all fields should parse"),
            MetadataDoc {
                policy_store_spec_version: 1,
                cedar_version: "4.4.0".to_string(),
                policy_store: PolicyStoreInfoDoc {
                    id: "abc123def456".to_string(),
                    name: "test_store".to_string(),
                    description: Some("A test policy store".to_string()),
                    version: "1.0.0".to_string(),
                    created_date: Some(timestamp("2024-01-01T00:00:00Z")),
                    updated_date: Some(timestamp("2024-01-02T00:00:00Z")),
                },
            },
            "every metadata.json field should be parsed"
        );
    }

    #[test]
    fn requires_spec_version_and_name() {
        for json in [
            r#"{ "cedar_version": "4.4.0", "policy_store": { "name": "Test Store" } }"#,
            r#"{ "policy_store_spec_version": 1, "cedar_version": "4.4.0", "policy_store": {} }"#,
            "{ invalid json }",
        ] {
            let err = parse(json).expect_err("incomplete v1 metadata must fail");
            assert!(
                matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
                "Expected MetadataJsonParseFailed for {json}, got: {err:?}"
            );
        }
    }

    #[test]
    fn ignores_unknown_fields() {
        let json = r#"{
            "policy_store_spec_version": 1,
            "cedar_version": "4.4.0",
            "extra_root": true,
            "policy_store": { "name": "Test Store", "extra_info": 1 }
        }"#;

        parse(json).expect("v1 metadata must keep ignoring unknown fields");
    }
}
