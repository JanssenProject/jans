// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Custom (non-JWT) issuer file parsing for format v0.
//!
//! One JSON file per issuer under `custom-issuers/`. The issuer id is taken from
//! an explicit `id` field or, failing that, the filename without `.json`.

use std::collections::{HashMap, HashSet};

use crate::common::policy_store::errors::CustomIssuerParseError;
use crate::common::policy_store::formats::file_id::id_from_filename;
use serde::Deserialize;
use serde_json::Value as JsonValue;

/// A custom issuer file body; unknown fields are ignored.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct CustomIssuerDoc {
    pub(crate) tokens_mappings: HashMap<String, CustomTokenDoc>,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct CustomTokenDoc {
    #[serde(default)]
    pub(crate) required: bool,
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
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
    /// Failures are [`CustomIssuerParseError`]; the caller wraps them in
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

        // Reachable from an explicit `"id": ""` and from a file named only
        // `.json`; an empty id would key the issuer map on "".
        if id.is_empty() {
            return Err(CustomIssuerParseError::EmptyId {
                file: filename.to_string(),
            });
        }

        // Drop the out-of-band `id` (consumed above) before deserializing.
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
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_minimal_issuer() {
        let parsed = CustomIssuerParser::parse(
            r#"{ "tokens_mappings": { "Acme::CustomToken": {} } }"#,
            "CustomKeys.json",
        )
        .expect("minimal v0 custom issuer should parse");
        assert_eq!(
            parsed.id, "CustomKeys",
            "id should be derived from the file name"
        );
        assert_eq!(
            parsed.meta.tokens_mappings["Acme::CustomToken"],
            CustomTokenDoc {
                required: false,
                required_claims: HashSet::new(),
            },
            "token settings should default when absent"
        );
    }

    #[test]
    fn ignores_unknown_fields() {
        let parsed = CustomIssuerParser::parse(
            r#"{ "tokens_mappings": { "Acme::CustomToken": { "vendor": true } }, "extra": 1 }"#,
            "acme.json",
        )
        .expect("unknown fields must not fail the load");
        assert!(
            parsed
                .meta
                .tokens_mappings
                .contains_key("Acme::CustomToken"),
            "known fields should still be read"
        );
    }
    /// A file named only `.json` leaves nothing to derive an id from.
    #[test]
    fn parse_rejects_id_derived_from_bare_extension() {
        let err = CustomIssuerParser::parse(
            r#"{ "tokens_mappings": { "Acme::CustomToken": {} } }"#,
            ".json",
        )
        .expect_err("a file named only '.json' must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::EmptyId { file } if file == ".json"),
            "expected EmptyId naming the file, got: {err:?}"
        );
    }

    #[test]
    fn parse_rejects_explicit_empty_id() {
        let content = r#"{ "id": "", "tokens_mappings": { "Acme::CustomToken": {} } }"#;
        let err = CustomIssuerParser::parse(content, "acme.json")
            .expect_err("an explicit empty id must be rejected");
        assert!(
            matches!(&err, CustomIssuerParseError::EmptyId { file } if file == "acme.json"),
            "expected EmptyId naming the file, got: {err:?}"
        );
    }
}
