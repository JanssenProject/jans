// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Custom (non-JWT) issuer file parsing for format v0.
//!
//! One JSON file per issuer under `custom-issuers/`. The issuer id is taken from
//! an explicit `id` field or, failing that, the filename without `.json`.

use std::collections::{HashMap, HashSet};

use crate::common::policy_store::formats::file_id::id_from_filename;
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

/// A parsed custom issuer configuration with its resolved id and source filename.
#[derive(Debug, Clone)]
pub(crate) struct ParsedCustomIssuer {
    /// The issuer name/id (map key; sanitized downstream).
    pub id: String,
    /// The custom issuer configuration.
    pub meta: CustomIssuerDoc,
    /// Source filename.
    pub filename: String,
}

/// Parser for custom issuer configuration files.
pub(crate) struct CustomIssuerParser;

impl CustomIssuerParser {
    /// Parse a single custom issuer configuration from JSON content.
    ///
    /// Errors are returned as strings; the caller wraps them in
    /// [`ConversionError`](crate::common::policy_store::manager::ConversionError).
    pub(crate) fn parse(content: &str, filename: &str) -> Result<ParsedCustomIssuer, String> {
        let json: JsonValue = serde_json::from_str(content)
            .map_err(|e| format!("invalid JSON in '{filename}': {e}"))?;

        let obj = json
            .as_object()
            .ok_or_else(|| format!("custom issuer file '{filename}' is not a JSON object"))?;

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
        let meta: CustomIssuerDoc = serde_json::from_value(JsonValue::Object(body))
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
    fn rejects_unknown_fields() {
        let err = CustomIssuerParser::parse(
            r#"{ "tokens_mappings": { "Acme::CustomToken": { "requiredd": true } } }"#,
            "acme.json",
        )
        .expect_err("a misspelled token setting must be rejected");
        assert!(
            err.contains("requiredd"),
            "error should name the field, got: {err}"
        );
    }
}
