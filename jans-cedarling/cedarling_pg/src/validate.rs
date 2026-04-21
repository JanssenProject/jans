// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Pure validation helpers for configuration values (unit-testable without `PostgreSQL`).

/// Returns `true` if `s` is acceptable for the `cedarling.tokens` GUC:
/// empty (after trim), or valid JSON (any value).
#[must_use]
pub fn tokens_json_is_valid(s: &str) -> bool {
    let trimmed = s.trim();
    if trimmed.is_empty() {
        return true;
    }
    serde_json::from_str::<serde_json::Value>(trimmed).is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tokens_empty_and_whitespace_ok() {
        assert!(tokens_json_is_valid(""), "empty string should be valid");
        assert!(
            tokens_json_is_valid("   \n\t  "),
            "whitespace-only should be valid"
        );
    }

    #[test]
    fn tokens_valid_json_ok() {
        assert!(tokens_json_is_valid(r#"{"access_token":"x"}"#));
        assert!(tokens_json_is_valid("[]"));
        assert!(tokens_json_is_valid("42"));
    }

    #[test]
    fn tokens_invalid_json_rejected() {
        assert!(
            !tokens_json_is_valid("not json"),
            "non-JSON text should be rejected"
        );
        assert!(
            !tokens_json_is_valid("{"),
            "truncated JSON should be rejected"
        );
    }
}
