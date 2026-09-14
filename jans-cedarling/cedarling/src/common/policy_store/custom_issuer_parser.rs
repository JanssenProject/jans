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

        // Drop the out-of-band `id` (consumed above) before deserializing:
        // `CustomIssuerMetadata` denies unknown fields so a misspelled enforcement
        // knob fails the load, and `id` is the one legitimately-extra key.
        let mut body = obj.clone();
        body.remove("id");
        let meta: CustomIssuerMetadata = serde_json::from_value(JsonValue::Object(body))
            .map_err(|e| format!("invalid custom issuer '{id}' in '{filename}': {e}"))?;

        if meta.tokens_mappings.is_empty() {
            return Err(format!(
                "custom issuer '{id}' in '{filename}' declares no tokens"
            ));
        }
        if meta.tokens_mappings.keys().any(String::is_empty) {
            return Err(format!(
                "custom issuer '{id}' in '{filename}' has a token with an empty entity type name"
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

    /// A minimal single-token issuer body.
    fn one_token(entity_type_name: &str) -> String {
        format!(r#"{{ "tokens_mappings": {{ "{entity_type_name}": {{}} }} }}"#)
    }

    #[test]
    fn parse_minimal_derives_id_from_filename() {
        let parsed =
            CustomIssuerParser::parse(&one_token("Acme::CustomToken"), "CustomKeys.json").unwrap();
        assert_eq!(
            parsed.id, "CustomKeys",
            "id should be derived from filename 'CustomKeys.json' by stripping the .json suffix"
        );
        let token = parsed.meta.tokens_mappings.get("Acme::CustomToken").expect(
            "tokens_mappings should be keyed by the Cedar entity type name from the JSON content",
        );
        assert!(
            !token.required,
            "required should default to false when absent from the JSON content"
        );
        assert!(
            token.required_claims.is_empty(),
            "required_claims should default to an empty set when absent from the JSON content"
        );
    }

    #[test]
    fn parse_derives_id_from_mixed_case_json_extension() {
        let parsed =
            CustomIssuerParser::parse(&one_token("Acme::CustomToken"), "CustomKeys.JsOn").unwrap();
        assert_eq!(
            parsed.id, "CustomKeys",
            "id should strip the .json extension case-insensitively from 'CustomKeys.JsOn'"
        );
    }

    #[test]
    fn parse_full_reads_id_required_and_required_claims() {
        let content = r#"{
            "id": "acme",
            "tokens_mappings": {
                "Acme::CustomToken": {
                    "required": true,
                    "required_claims": ["sub", "scope"]
                }
            }
        }"#;
        let parsed = CustomIssuerParser::parse(content, "ignored.json").unwrap();
        assert_eq!(
            parsed.id, "acme",
            "id should be taken from the explicit 'id' JSON field"
        );
        let token = parsed
            .meta
            .tokens_mappings
            .get("Acme::CustomToken")
            .unwrap();
        assert!(
            token.required,
            "required flag should be true as set in the JSON content"
        );
        assert!(
            token.required_claims.contains("sub"),
            "required_claims should contain the 'sub' claim"
        );
        assert!(
            token.required_claims.contains("scope"),
            "required_claims should contain the 'scope' claim"
        );
    }

    #[test]
    fn parse_reads_several_tokens_for_one_issuer() {
        let content = r#"{
            "id": "acme",
            "tokens_mappings": {
                "Acme::DolphinToken": { "required": true },
                "Acme::WhaleToken": {}
            }
        }"#;
        let parsed = CustomIssuerParser::parse(content, "ignored.json").unwrap();
        assert_eq!(
            parsed.meta.tokens_mappings.len(),
            2,
            "one issuer should be able to declare several Cedar entity types"
        );
        assert!(
            parsed.meta.tokens_mappings["Acme::DolphinToken"].required,
            "required should be read per token, not shared across the issuer"
        );
        assert!(
            !parsed.meta.tokens_mappings["Acme::WhaleToken"].required,
            "a sibling token should keep its own default required=false"
        );
    }

    #[test]
    fn parse_missing_tokens_errors() {
        let content = r#"{ "id": "acme" }"#;
        let err = CustomIssuerParser::parse(content, "bad.json").unwrap_err();
        assert!(err.contains("tokens_mappings"), "got: {err}");
    }

    #[test]
    fn parse_empty_tokens_errors() {
        let content = r#"{ "tokens_mappings": {} }"#;
        let err = CustomIssuerParser::parse(content, "bad.json").unwrap_err();
        assert!(err.contains("declares no tokens"), "got: {err}");
    }

    #[test]
    fn parse_empty_entity_type_name_errors() {
        let content = r#"{ "tokens_mappings": { "": {} } }"#;
        let err = CustomIssuerParser::parse(content, "bad.json").unwrap_err();
        assert!(err.contains("empty entity type name"), "got: {err}");
    }

    #[test]
    fn parse_rejects_misspelled_required_knob() {
        let content = r#"{
            "tokens_mappings": { "Acme::CustomToken": { "requiredd": true } }
        }"#;
        let err = CustomIssuerParser::parse(content, "acme.json").unwrap_err();
        assert!(
            err.contains("requiredd") || err.contains("unknown field"),
            "a typo in an enforcement knob should fail the load, got: {err}"
        );
    }

    #[test]
    fn parse_rejects_unknown_issuer_field() {
        let content = r#"{
            "tokens_mappings": { "Acme::CustomToken": {} },
            "requireddd": true
        }"#;
        let err = CustomIssuerParser::parse(content, "acme.json").unwrap_err();
        assert!(
            err.contains("requireddd") || err.contains("unknown field"),
            "an unknown top-level issuer field should fail the load, got: {err}"
        );
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
            CustomIssuerParser::parse(
                r#"{ "id": "a", "tokens_mappings": { "M::T": {} } }"#,
                "f1.json",
            )
            .unwrap(),
            CustomIssuerParser::parse(
                r#"{ "id": "a", "tokens_mappings": { "M::U": {} } }"#,
                "f2.json",
            )
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
            CustomIssuerParser::parse(
                r#"{ "id": "a", "tokens_mappings": { "M::T": {} } }"#,
                "f1.json",
            )
            .unwrap(),
            CustomIssuerParser::parse(
                r#"{ "id": "b", "tokens_mappings": { "M::U": {} } }"#,
                "f2.json",
            )
            .unwrap(),
        ];
        let map = CustomIssuerParser::create_map(issuers);
        assert_eq!(
            map.len(),
            2,
            "map should contain one entry per parsed issuer id ('a' and 'b')"
        );
        assert!(
            map.get("a").unwrap().tokens_mappings.contains_key("M::T"),
            "map entry for id 'a' should preserve its declared token types"
        );
        assert!(
            map.get("b").unwrap().tokens_mappings.contains_key("M::U"),
            "map entry for id 'b' should preserve its declared token types"
        );
    }
}
