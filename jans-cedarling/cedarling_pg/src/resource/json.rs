// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Parse row / session resource JSON into Cedarling [`EntityData`](cedarling::EntityData).
//!
//! The canonical JSON shape matches [`cedarling::EntityData`] serialization (see
//! `cedarling::authz::request` and multi-issuer tests): a `cedar_entity_mapping` object with
//! `entity_type` and `id`, plus additional top-level keys that become entity attributes via
//! `#[serde(flatten)]`. This module does **not** run inside `PostgreSQL` SPI transaction control;
//! it is pure Rust for use from a future SQL bridge (PR5+).

use cedarling::EntityData;
use thiserror::Error;

/// Failure to build [`EntityData`] from JSON (wrong shape, invalid JSON, or empty input).
#[derive(Debug, Error)]
pub enum ResourceEntityDataError {
    /// Trimmed input was empty (SQL `NULL` should be handled by the caller before calling here).
    #[error("resource JSON is empty or whitespace only")]
    Empty,
    /// JSON parse error, or JSON that does not match the `EntityData` schema.
    #[error(transparent)]
    Serde(#[from] serde_json::Error),
}

/// Parses UTF-8 JSON text into [`EntityData`] using the same rules as [`EntityData::from_json`].
///
/// Leading / trailing whitespace is stripped. Empty or whitespace-only strings return
/// [`ResourceEntityDataError::Empty`].
pub fn resource_entity_data_from_json_str(
    json: &str,
) -> Result<EntityData, ResourceEntityDataError> {
    let trimmed = json.trim();
    if trimmed.is_empty() {
        return Err(ResourceEntityDataError::Empty);
    }
    Ok(EntityData::from_json(trimmed)?)
}

/// Parses a [`serde_json::Value`] (e.g. from `jsonb`) into [`EntityData`].
pub fn resource_entity_data_from_json_value(
    value: serde_json::Value,
) -> Result<EntityData, ResourceEntityDataError> {
    Ok(serde_json::from_value::<EntityData>(value)?)
}

#[cfg(test)]
mod tests {
    use super::*;
    use cedarling::CedarEntityMapping;
    use serde_json::json;

    #[test]
    fn parses_acme_style_fixture() {
        let s = json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "ApprovedDolphinFoods"
            },
            "name": "Approved Dolphin Foods"
        })
        .to_string();

        let got = resource_entity_data_from_json_str(&s).expect("valid resource JSON");
        assert_eq!(
            got.cedar_mapping,
            CedarEntityMapping {
                entity_type: "Acme::Resource".to_string(),
                id: "ApprovedDolphinFoods".to_string(),
            }
        );
        assert_eq!(
            got.attributes.get("name").cloned(),
            Some(json!("Approved Dolphin Foods"))
        );
    }

    #[test]
    fn parses_nested_attribute_objects_like_http_request() {
        let s = json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::HTTP_Request",
                "id": "some_request"
            },
            "header": {"Accept": "test"},
            "url": {"host": "protected.host", "protocol": "http", "path": "/protected"}
        })
        .to_string();

        let got = resource_entity_data_from_json_str(&s).expect("valid resource JSON");
        assert_eq!(got.cedar_mapping.entity_type, "Jans::HTTP_Request");
        assert_eq!(got.cedar_mapping.id, "some_request");
        assert!(got.attributes.contains_key("header"));
        assert!(got.attributes.contains_key("url"));
    }

    #[test]
    fn from_json_value_matches_from_str() {
        let value = json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "R1"
            },
            "count": 3
        });
        let from_value = resource_entity_data_from_json_value(value.clone()).expect("from_value");
        let from_str = resource_entity_data_from_json_str(&value.to_string()).expect("from_str");
        assert_eq!(from_value, from_str);
    }

    #[test]
    fn empty_and_whitespace_rejected() {
        assert!(matches!(
            resource_entity_data_from_json_str(""),
            Err(ResourceEntityDataError::Empty)
        ));
        assert!(matches!(
            resource_entity_data_from_json_str("  \n\t  "),
            Err(ResourceEntityDataError::Empty)
        ));
    }

    #[test]
    fn invalid_json_rejected() {
        let err = resource_entity_data_from_json_str("not json").unwrap_err();
        assert!(matches!(err, ResourceEntityDataError::Serde(_)));
    }

    #[test]
    fn valid_json_wrong_shape_rejected() {
        // Valid JSON object but not an `EntityData` document
        let err = resource_entity_data_from_json_str(r#"{"foo":1}"#).unwrap_err();
        assert!(matches!(err, ResourceEntityDataError::Serde(_)));
    }

    #[test]
    fn json_null_root_rejected() {
        let err = resource_entity_data_from_json_str("null").unwrap_err();
        assert!(matches!(err, ResourceEntityDataError::Serde(_)));
    }

    #[test]
    fn json_array_root_rejected() {
        let err =
            resource_entity_data_from_json_str(r#"[{"cedar_entity_mapping":{}}]"#).unwrap_err();
        assert!(matches!(err, ResourceEntityDataError::Serde(_)));
    }

    #[test]
    fn extra_top_level_keys_become_attributes() {
        let got = resource_entity_data_from_json_str(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "T::R",
                    "id": "id1"
                },
                "custom": "x",
                "n": 7
            })
            .to_string(),
        )
        .expect("parse");
        assert_eq!(got.attributes.len(), 2);
        assert_eq!(got.attributes.get("custom"), Some(&json!("x")));
        assert_eq!(got.attributes.get("n"), Some(&json!(7)));
    }

    #[test]
    fn mapping_only_no_attributes_ok() {
        let got = resource_entity_data_from_json_str(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "T::R",
                    "id": "only"
                }
            })
            .to_string(),
        )
        .expect("parse");
        assert!(got.attributes.is_empty());
    }

    #[test]
    fn from_value_null_rejected() {
        let err = resource_entity_data_from_json_value(serde_json::Value::Null).unwrap_err();
        assert!(matches!(err, ResourceEntityDataError::Serde(_)));
    }
}
