// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Custom (non-JWT) issuer configuration parsing for the directory/archive format.
//!
//! Mirrors [`issuer_parser`](super::issuer_parser) but for custom issuers: one
//! JSON file per issuer under `custom-issuers/`, deserialized into
//! [`CustomIssuerMetadata`]. The map key (issuer name, later sanitized into the
//! `context.tokens.{issuer}_{type}` id) is taken from an explicit `id` field or,
//! failing that, the filename with its `.json` suffix stripped.

use std::collections::HashMap;

use super::CustomIssuerMetadata;
use serde_json::Value as JsonValue;

/// A parsed custom issuer configuration with its resolved id and source filename.
#[derive(Debug, Clone)]
pub(super) struct ParsedCustomIssuer {
    /// The issuer name/id (map key; sanitized downstream).
    pub id: String,
    /// The custom issuer configuration.
    pub meta: CustomIssuerMetadata,
    /// Source filename.
    pub filename: String,
}

/// Parser for custom issuer configuration files.
pub(super) struct CustomIssuerParser;

impl CustomIssuerParser {
    /// Parse a single custom issuer configuration from JSON content.
    ///
    /// Errors are returned as strings; the caller wraps them in
    /// [`ConversionError`](super::manager::ConversionError).
    pub(super) fn parse(content: &str, filename: &str) -> Result<ParsedCustomIssuer, String> {
        let json: JsonValue = serde_json::from_str(content)
            .map_err(|e| format!("invalid JSON in '{filename}': {e}"))?;

        let obj = json
            .as_object()
            .ok_or_else(|| format!("custom issuer file '{filename}' is not a JSON object"))?;

        // Resolve id from the "id" field, else derive from the filename.
        let id = obj.get("id").and_then(JsonValue::as_str).map_or_else(
            || {
                let stem = filename
                    .rfind('.')
                    .filter(|&dot| filename[dot..].eq_ignore_ascii_case(".json"))
                    .map_or(filename, |dot| &filename[..dot]);
                stem.to_string()
            },
            std::string::ToString::to_string,
        );

        // `CustomIssuerMetadata` ignores the extra `id` field (no deny_unknown).
        let meta: CustomIssuerMetadata = serde_json::from_value(json.clone())
            .map_err(|e| format!("invalid custom issuer '{id}' in '{filename}': {e}"))?;

        if meta.entity_type_name.is_empty() {
            return Err(format!(
                "custom issuer '{id}' in '{filename}' is missing required field: entity_type_name"
            ));
        }

        Ok(ParsedCustomIssuer {
            id,
            meta,
            filename: filename.to_string(),
        })
    }

    /// Reject duplicate issuer ids across files.
    pub(super) fn validate(issuers: &[ParsedCustomIssuer]) -> Result<(), Vec<String>> {
        let mut errors = Vec::new();
        let mut seen: HashMap<&str, &str> = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            if let Some(existing_file) = seen.get(parsed.id.as_str()) {
                errors.push(format!(
                    "Duplicate custom issuer ID '{}' found in files '{}' and '{}'",
                    parsed.id, existing_file, parsed.filename
                ));
            } else {
                seen.insert(&parsed.id, &parsed.filename);
            }
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }

    /// Consolidate parsed issuers into a map keyed by id (first occurrence wins;
    /// duplicates are expected to be caught by [`validate`](Self::validate)).
    pub(super) fn create_map(
        issuers: Vec<ParsedCustomIssuer>,
    ) -> HashMap<String, CustomIssuerMetadata> {
        let mut map = HashMap::with_capacity(issuers.len());
        for parsed in issuers {
            if let std::collections::hash_map::Entry::Vacant(e) = map.entry(parsed.id.clone()) {
                e.insert(parsed.meta);
            }
        }
        map
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_minimal_derives_id_from_filename() {
        let content = r#"{ "entity_type_name": "Acme::CustomToken" }"#;
        let parsed = CustomIssuerParser::parse(content, "CustomKeys.json").unwrap();
        assert_eq!(
            parsed.id, "CustomKeys",
            "id should be derived from filename 'CustomKeys.json' by stripping the .json suffix"
        );
        assert_eq!(
            parsed.meta.entity_type_name, "Acme::CustomToken",
            "entity_type_name should be parsed from the JSON content"
        );
        assert!(
            !parsed.meta.required,
            "required should default to false when absent from the JSON content"
        );
        assert!(
            parsed.meta.required_claims.is_empty(),
            "required_claims should default to an empty set when absent from the JSON content"
        );
    }

    #[test]
    fn parse_derives_id_from_mixed_case_json_extension() {
        let content = r#"{ "entity_type_name": "Acme::CustomToken" }"#;
        let parsed = CustomIssuerParser::parse(content, "CustomKeys.JsOn").unwrap();
        assert_eq!(
            parsed.id, "CustomKeys",
            "id should strip the .json extension case-insensitively from 'CustomKeys.JsOn'"
        );
    }

    #[test]
    fn parse_full_reads_id_required_and_required_claims() {
        let content = r#"{
            "id": "acme",
            "entity_type_name": "Acme::CustomToken",
            "required": true,
            "required_claims": ["sub", "scope"]
        }"#;
        let parsed = CustomIssuerParser::parse(content, "ignored.json").unwrap();
        assert_eq!(
            parsed.id, "acme",
            "id should be taken from the explicit 'id' JSON field"
        );
        assert!(
            parsed.meta.required,
            "required flag should be true as set in the JSON content"
        );
        assert!(
            parsed.meta.required_claims.contains("sub"),
            "required_claims should contain the 'sub' claim"
        );
        assert!(
            parsed.meta.required_claims.contains("scope"),
            "required_claims should contain the 'scope' claim"
        );
    }

    #[test]
    fn parse_missing_entity_type_name_errors() {
        let content = r#"{ "required": true }"#;
        let err = CustomIssuerParser::parse(content, "bad.json").unwrap_err();
        assert!(err.contains("entity_type_name"), "got: {err}");
    }

    #[test]
    fn parse_invalid_json_errors() {
        let err = CustomIssuerParser::parse("{ not json }", "bad.json").unwrap_err();
        assert!(err.contains("invalid JSON"), "got: {err}");
    }

    #[test]
    fn parse_non_object_errors() {
        let err = CustomIssuerParser::parse("[]", "bad.json").unwrap_err();
        assert!(err.contains("not a JSON object"), "got: {err}");
    }

    #[test]
    fn validate_detects_duplicate_ids() {
        let issuers = vec![
            CustomIssuerParser::parse(r#"{ "id": "a", "entity_type_name": "M::T" }"#, "f1.json")
                .unwrap(),
            CustomIssuerParser::parse(r#"{ "id": "a", "entity_type_name": "M::U" }"#, "f2.json")
                .unwrap(),
        ];
        let errors = CustomIssuerParser::validate(&issuers).unwrap_err();
        assert_eq!(
            errors.len(),
            1,
            "validate should report exactly one duplicate-id error for two files sharing id 'a'"
        );
        assert!(
            errors[0].contains('a')
                && errors[0].contains("f1.json")
                && errors[0].contains("f2.json"),
            "duplicate error should mention id 'a' and both source files 'f1.json' and 'f2.json', got: {}",
            errors[0]
        );
    }

    #[test]
    fn create_map_keys_by_id() {
        let issuers = vec![
            CustomIssuerParser::parse(r#"{ "id": "a", "entity_type_name": "M::T" }"#, "f1.json")
                .unwrap(),
            CustomIssuerParser::parse(r#"{ "id": "b", "entity_type_name": "M::U" }"#, "f2.json")
                .unwrap(),
        ];
        let map = CustomIssuerParser::create_map(issuers);
        assert_eq!(
            map.len(),
            2,
            "map should contain one entry per parsed issuer id ('a' and 'b')"
        );
        assert_eq!(
            map.get("a").unwrap().entity_type_name,
            "M::T",
            "map entry for id 'a' should preserve its entity_type_name"
        );
        assert_eq!(
            map.get("b").unwrap().entity_type_name,
            "M::U",
            "map entry for id 'b' should preserve its entity_type_name"
        );
    }
}
