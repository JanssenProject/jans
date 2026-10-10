// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Custom (non-JWT) issuer configuration parsing for the directory/archive format.
//!
//! Mirrors [`trusted_issuer`](super::trusted_issuer) but for custom issuers: one
//! JSON file per issuer under `custom-issuers/`, converted into
//! [`CustomIssuerMetadata`] by [`CustomIssuerParser::create_map`]. The map key (issuer name, later sanitized into the
//! `context.tokens.{issuer}_{type}` id) is taken from an explicit `id` field or,
//! failing that, the filename with its `.json` suffix stripped.

use std::collections::{HashMap, HashSet};

use crate::common::policy_store::errors::CustomIssuerParseError;
use crate::common::policy_store::formats::file_id::id_from_filename;
use crate::common::policy_store::{CustomIssuerMetadata, CustomTokenMetadata};
use serde::Deserialize;
use serde_json::Value as JsonValue;

/// A custom issuer file body; unknown fields fail the load.
#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct CustomIssuerDoc {
    pub(crate) tokens_mappings: HashMap<String, CustomTokenDoc>,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct CustomTokenDoc {
    #[serde(default)]
    pub(crate) required: bool,
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
}

impl From<CustomIssuerDoc> for CustomIssuerMetadata {
    fn from(doc: CustomIssuerDoc) -> Self {
        Self {
            tokens_mappings: doc
                .tokens_mappings
                .into_iter()
                .map(|(mapping, token)| {
                    let token = CustomTokenMetadata {
                        required: token.required,
                        required_claims: token.required_claims,
                    };
                    (mapping, token)
                })
                .collect(),
        }
    }
}

/// A parsed custom issuer configuration with its resolved id and source filename.
#[derive(Debug, Clone)]
pub(crate) struct ParsedCustomIssuer {
    /// The issuer name/id (map key; sanitized downstream).
    pub(crate) id: String,
    /// The custom issuer configuration.
    pub(crate) meta: CustomIssuerDoc,
    /// Source filename.
    pub(crate) filename: String,
}

/// Parser for custom issuer configuration files.
pub(crate) struct CustomIssuerParser;

impl CustomIssuerParser {
    /// Parse a single custom issuer configuration from JSON content.
    ///
    /// Errors are returned as strings; the caller wraps them in
    /// [`ConversionError`](crate::common::policy_store::manager::ConversionError).
    pub(crate) fn parse(
        content: &str,
        filename: &str,
    ) -> Result<ParsedCustomIssuer, CustomIssuerParseError> {
        let json: JsonValue = serde_json::from_str(content).map_err(|source| {
            CustomIssuerParseError::InvalidJson {
                file: filename.to_string(),
                source,
            }
        })?;

        let obj = json
            .as_object()
            .ok_or_else(|| CustomIssuerParseError::NotAnObject {
                file: filename.to_string(),
            })?;

        // Resolve id from the "id" field, else derive from the filename.
        let id = obj.get("id").and_then(JsonValue::as_str).map_or_else(
            || id_from_filename(filename).to_string(),
            std::string::ToString::to_string,
        );

        // Drop the out-of-band `id` (consumed above) before deserializing:
        // `CustomIssuerDoc` denies unknown fields so a misspelled enforcement
        // knob fails the load, and `id` is the one legitimately-extra key.
        let mut body = obj.clone();
        body.remove("id");
        let meta: CustomIssuerDoc =
            serde_json::from_value(JsonValue::Object(body)).map_err(|source| {
                CustomIssuerParseError::InvalidBody {
                    id: id.clone(),
                    file: filename.to_string(),
                    source,
                }
            })?;

        if meta.tokens_mappings.is_empty() {
            return Err(CustomIssuerParseError::NoTokens {
                id,
                file: filename.to_string(),
            });
        }
        if meta.tokens_mappings.keys().any(String::is_empty) {
            return Err(CustomIssuerParseError::EmptyEntityTypeName {
                id,
                file: filename.to_string(),
            });
        }

        Ok(ParsedCustomIssuer {
            id,
            meta,
            filename: filename.to_string(),
        })
    }

    /// Reject duplicate issuer ids across files.
    pub(crate) fn validate(
        issuers: &[ParsedCustomIssuer],
    ) -> Result<(), Vec<CustomIssuerParseError>> {
        let mut errors = Vec::new();
        let mut seen: HashMap<&str, &str> = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            if let Some(existing_file) = seen.get(parsed.id.as_str()) {
                errors.push(CustomIssuerParseError::DuplicateId {
                    id: parsed.id.clone(),
                    first_file: (*existing_file).to_string(),
                    second_file: parsed.filename.clone(),
                });
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
    pub(crate) fn create_map(
        issuers: Vec<ParsedCustomIssuer>,
    ) -> HashMap<String, CustomIssuerMetadata> {
        let mut map = HashMap::with_capacity(issuers.len());
        for parsed in issuers {
            if let std::collections::hash_map::Entry::Vacant(e) = map.entry(parsed.id.clone()) {
                e.insert(parsed.meta.into());
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
        let parsed = CustomIssuerParser::parse(&one_token("Acme::CustomToken"), "CustomKeys.json")
            .expect("a minimal custom issuer should parse");
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
        let parsed = CustomIssuerParser::parse(&one_token("Acme::CustomToken"), "CustomKeys.JsOn")
            .expect("a mixed-case .json name should parse");
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
        let parsed = CustomIssuerParser::parse(content, "ignored.json")
            .expect("a custom issuer with an explicit id should parse");
        assert_eq!(
            parsed.id, "acme",
            "id should be taken from the explicit 'id' JSON field"
        );
        let token = parsed
            .meta
            .tokens_mappings
            .get("Acme::CustomToken")
            .expect("the issuer file should parse");
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
        let parsed = CustomIssuerParser::parse(content, "ignored.json")
            .expect("a custom issuer with an explicit id should parse");
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
        let err = CustomIssuerParser::parse(content, "bad.json")
            .expect_err("an issuer without tokens_mappings must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::InvalidBody { id, file, .. }
                if id == "acme" && file == "bad.json"),
            "expected InvalidBody naming the issuer and file, got: {err:?}"
        );
    }

    #[test]
    fn parse_empty_tokens_errors() {
        let content = r#"{ "tokens_mappings": {} }"#;
        let err = CustomIssuerParser::parse(content, "bad.json")
            .expect_err("an issuer declaring no tokens must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::NoTokens { file, .. } if file == "bad.json"),
            "expected NoTokens naming the file, got: {err:?}"
        );
    }

    #[test]
    fn parse_empty_entity_type_name_errors() {
        let content = r#"{ "tokens_mappings": { "": {} } }"#;
        let err = CustomIssuerParser::parse(content, "bad.json")
            .expect_err("an empty entity type name must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::EmptyEntityTypeName { file, .. }
                if file == "bad.json"),
            "expected EmptyEntityTypeName naming the file, got: {err:?}"
        );
    }

    #[test]
    fn parse_rejects_misspelled_required_knob() {
        let content = r#"{
            "tokens_mappings": { "Acme::CustomToken": { "requiredd": true } }
        }"#;
        let err = CustomIssuerParser::parse(content, "acme.json")
            .expect_err("a typo in an enforcement knob should fail the load");
        assert!(
            matches!(&err, CustomIssuerParseError::InvalidBody { file, source, .. }
                if file == "acme.json" && source.to_string().contains("requiredd")),
            "expected InvalidBody naming the misspelled knob, got: {err:?}"
        );
    }

    #[test]
    fn parse_rejects_unknown_issuer_field() {
        let content = r#"{
            "tokens_mappings": { "Acme::CustomToken": {} },
            "requireddd": true
        }"#;
        let err = CustomIssuerParser::parse(content, "acme.json")
            .expect_err("an unknown top-level issuer field should fail the load");
        assert!(
            matches!(&err, CustomIssuerParseError::InvalidBody { file, source, .. }
                if file == "acme.json" && source.to_string().contains("requireddd")),
            "expected InvalidBody naming the unknown field, got: {err:?}"
        );
    }

    #[test]
    fn parse_invalid_json_errors() {
        let err = CustomIssuerParser::parse("{ not json }", "bad.json")
            .expect_err("malformed JSON must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::InvalidJson { file, .. } if file == "bad.json"),
            "expected InvalidJson naming the file, got: {err:?}"
        );
    }

    #[test]
    fn parse_non_object_errors() {
        let err = CustomIssuerParser::parse("[]", "bad.json")
            .expect_err("a non-object issuer file must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::NotAnObject { file } if file == "bad.json"),
            "expected NotAnObject naming the file, got: {err:?}"
        );
    }

    #[test]
    fn validate_detects_duplicate_ids() {
        let issuers = vec![
            CustomIssuerParser::parse(
                r#"{ "id": "a", "tokens_mappings": { "M::T": {} } }"#,
                "f1.json",
            )
            .expect("the first issuer file should parse"),
            CustomIssuerParser::parse(
                r#"{ "id": "a", "tokens_mappings": { "M::U": {} } }"#,
                "f2.json",
            )
            .expect("the second issuer file should parse"),
        ];
        let errors = CustomIssuerParser::validate(&issuers)
            .expect_err("two files sharing an id must be rejected");
        assert_eq!(
            errors.len(),
            1,
            "validate should report exactly one duplicate-id error for two files sharing id 'a'"
        );
        assert!(
            matches!(&errors[0], CustomIssuerParseError::DuplicateId { id, first_file, second_file }
                if id == "a" && first_file == "f1.json" && second_file == "f2.json"),
            "expected DuplicateId naming the id and both files, got: {:?}",
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
            .expect("the first issuer file should parse"),
            CustomIssuerParser::parse(
                r#"{ "id": "b", "tokens_mappings": { "M::U": {} } }"#,
                "f2.json",
            )
            .expect("the second issuer file should parse"),
        ];
        let map = CustomIssuerParser::create_map(issuers);
        assert_eq!(
            map.len(),
            2,
            "map should contain one entry per parsed issuer id ('a' and 'b')"
        );
        assert!(
            map["a"].tokens_mappings.contains_key("M::T"),
            "map entry for id 'a' should preserve its declared token types"
        );
        assert!(
            map["b"].tokens_mappings.contains_key("M::U"),
            "map entry for id 'b' should preserve its declared token types"
        );
    }
}
