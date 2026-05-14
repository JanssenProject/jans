// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Parse session token bundle JSON into Cedarling [`TokenInput`](cedarling::TokenInput) values.

use cedarling::TokenInput;
use serde_json::Value;
use thiserror::Error;

/// Invalid or unsupported token-bundle JSON.
#[derive(Debug, Error)]
pub enum TokenBundleError {
    #[error("token bundle JSON is empty or whitespace only")]
    Empty,
    #[error(transparent)]
    Serde(#[from] serde_json::Error),
    #[error("token bundle must be a JSON array of {{mapping,payload}} or an object of mapping string to JWT string")]
    UnsupportedShape,
    #[error("token bundle object entry {mapping:?} must be a JSON string JWT")]
    NonStringPayload {
        /// Mapping key that had a non-string value.
        mapping: String,
    },
}

/// Parses a token bundle used by `authorize_multi_issuer`.
///
/// Supported shapes:
/// - **Array:** `[{{"mapping":"Dolphin::Access_Token","payload":"<jwt>"}}, ...]` (matches [`TokenInput`] JSON).
/// - **Object:** `{{"Dolphin::Access_Token":"<jwt>", ...}}` (each key is the mapping, each value is the JWT string).
pub fn parse_token_inputs_from_json(json: &str) -> Result<Vec<TokenInput>, TokenBundleError> {
    let trimmed = json.trim();
    if trimmed.is_empty() {
        return Err(TokenBundleError::Empty);
    }
    let root: Value = serde_json::from_str(trimmed)?;
    parse_token_inputs_from_value(root)
}

fn parse_token_inputs_from_value(root: Value) -> Result<Vec<TokenInput>, TokenBundleError> {
    match root {
        Value::Array(items) => {
            let v: Vec<TokenInput> = serde_json::from_value(Value::Array(items))?;
            Ok(v)
        },
        Value::Object(map) => {
            let mut out = Vec::with_capacity(map.len());
            for (mapping, payload_val) in map {
                let Value::String(payload) = payload_val else {
                    return Err(TokenBundleError::NonStringPayload { mapping });
                };
                out.push(TokenInput::new(mapping, payload));
            }
            Ok(out)
        },
        _ => Err(TokenBundleError::UnsupportedShape),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_array_form() {
        let json = r#"[{"mapping":"Acme::Access_Token","payload":"eyJ"}]"#;
        let v = parse_token_inputs_from_json(json).expect("parse");
        assert_eq!(v.len(), 1);
        assert_eq!(v[0].mapping, "Acme::Access_Token");
        assert_eq!(v[0].payload, "eyJ");
    }

    #[test]
    fn parses_object_form() {
        let json = r#"{"Dolphin::Access_Token":"aa.bb.cc","Dolphin::Dolphin_Token":"xx.yy.zz"}"#;
        let v = parse_token_inputs_from_json(json).expect("parse");
        assert_eq!(v.len(), 2);
        let mut keys: Vec<_> = v.iter().map(|t| t.mapping.as_str()).collect();
        keys.sort_unstable();
        assert_eq!(keys, ["Dolphin::Access_Token", "Dolphin::Dolphin_Token"]);
    }

    #[test]
    fn rejects_primitives() {
        let err = parse_token_inputs_from_json("42").unwrap_err();
        assert!(matches!(err, TokenBundleError::UnsupportedShape));
    }

    #[test]
    fn rejects_object_with_non_string_payload() {
        let err = parse_token_inputs_from_json(r#"{"Acme::Access_Token":123}"#).unwrap_err();
        assert!(matches!(err, TokenBundleError::NonStringPayload { .. }));
    }

    /// Phase 8c — no JWT in logs: invalid token-bundle JSON must not echo the
    /// JWT body into the resulting error's Display. The mapping key is the
    /// only field a `NonStringPayload` error can include, and a raw JWT
    /// passed as the whole input is rejected by serde with a "expected value"
    /// message, not by quoting the input.
    #[test]
    fn errors_do_not_echo_raw_jwt_bytes() {
        let fake_jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsZWFreSJ9.sig_redacted";
        // Pass the raw JWT (not a bundle) — serde rejects it as invalid JSON.
        let err = parse_token_inputs_from_json(fake_jwt).unwrap_err();
        let msg = err.to_string();
        assert!(
            !msg.contains(fake_jwt),
            "token bundle error echoed JWT into message: {msg}"
        );
        assert!(
            !msg.contains("eyJ"),
            "token bundle error contains JWT prefix: {msg}"
        );
    }
}
