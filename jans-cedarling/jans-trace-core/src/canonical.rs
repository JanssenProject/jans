// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 8785 JSON Canonicalization Scheme over strictly parsed input.

use serde_json::Value;
use thiserror::Error;

use crate::json::{JsonError, parse_strict};
use crate::{Limits, SizeLimitExceeded};

/// Why `canonicalize_json` rejected its input.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum CanonicalizeError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitExceeded),
    #[error(transparent)]
    Json(#[from] JsonError),
}

impl CanonicalizeError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
        }
    }
}

/// Returns the RFC 8785 canonical bytes of `input` after strict validation.
pub fn canonicalize_json(input: &[u8], limits: &Limits) -> Result<Vec<u8>, CanonicalizeError> {
    limits.check_input(input)?;
    let value = parse_strict(input)?;
    Ok(canonicalize_value(&value)?)
}

pub(crate) fn canonicalize_value(value: &Value) -> Result<Vec<u8>, JsonError> {
    // The canonicalizer refuses only values outside the JCS model; strict parsing already
    // rejects those, so this maps an unreachable case to its honest meaning, not a panic.
    serde_json_canonicalizer::to_vec(value).map_err(|_| JsonError::NonCanonicalizable)
}

#[cfg(test)]
mod tests {
    use super::{CanonicalizeError, canonicalize_json};
    use crate::{JsonError, Limits};

    fn canonicalize(input: &[u8]) -> Result<Vec<u8>, CanonicalizeError> {
        canonicalize_json(input, &Limits::default())
    }

    fn canonical_text(input: &[u8]) -> String {
        let bytes = canonicalize(input).expect("input is valid strict JSON");
        String::from_utf8(bytes).expect("canonical JSON is UTF-8")
    }

    #[test]
    fn sorts_members_and_drops_whitespace() {
        assert_eq!(
            canonical_text(br#"{ "b": 1, "a": [true, null, "x"] }"#),
            r#"{"a":[true,null,"x"],"b":1}"#,
            "members are sorted and insignificant whitespace is removed"
        );
    }

    #[test]
    fn formats_numbers_like_ecmascript() {
        assert_eq!(
            canonical_text(b"[1.0, -0, 1e30, 0.000001, 1e-7]"),
            "[1,0,1e+30,0.000001,1e-7]",
            "numbers follow the RFC 8785 / ECMAScript serialization"
        );
    }

    #[test]
    fn accepts_integers_at_the_safe_boundary() {
        assert_eq!(
            canonical_text(b"[9007199254740991,-9007199254740991]"),
            "[9007199254740991,-9007199254740991]",
            "+/-(2^53-1) is exactly representable and must pass unchanged"
        );
    }

    #[test]
    fn rejects_integers_beyond_the_safe_boundary() {
        for input in [
            &b"[9007199254740992]"[..],
            b"[-9007199254740992]",
            b"{\"n\":18446744073709551615}",
        ] {
            let err = canonicalize(input)
                .expect_err("integer outside +/-(2^53-1) would be silently rounded by JCS");
            assert!(
                matches!(err, CanonicalizeError::Json(JsonError::NonCanonicalizable)),
                "expected NonCanonicalizable for {:?}, got {err:?}",
                String::from_utf8_lossy(input)
            );
        }
    }

    #[test]
    fn rejects_duplicate_members_at_any_depth() {
        for input in [&br#"{"a":1,"a":2}"#[..], br#"{"x":[{"k":1,"k":1}]}"#] {
            let err = canonicalize(input)
                .expect_err("duplicate members are ambiguous and must not be signed");
            assert!(
                matches!(err, CanonicalizeError::Json(JsonError::DuplicateMember)),
                "expected DuplicateMember for {:?}, got {err:?}",
                String::from_utf8_lossy(input)
            );
        }
    }

    #[test]
    fn rejects_malformed_input() {
        let cases: [(&str, &[u8]); 5] = [
            ("empty input", b""),
            ("trailing data", br#"{"a":1} {"#),
            ("byte order mark", b"\xEF\xBB\xBF{}"),
            ("invalid UTF-8", b"{\"a\":\"\xFF\"}"),
            ("lone surrogate", br#"{"a":"\ud800"}"#),
        ];
        for (name, input) in cases {
            let err = canonicalize(input).expect_err(name);
            assert!(
                matches!(err, CanonicalizeError::Json(JsonError::Invalid { .. })),
                "expected JsonError::Invalid for {name}, got {err:?}"
            );
        }
    }

    #[test]
    fn rejects_input_above_the_limit_before_parsing() {
        // Invalid JSON proves the size check runs first: parsing would report `Invalid`.
        let err = canonicalize_json(b"{{{{", &Limits::default().with_max_input_bytes(3))
            .expect_err("oversized input must be rejected without parsing");
        assert!(
            matches!(err, CanonicalizeError::SizeLimitExceeded(e) if e.limit() == 3),
            "expected SizeLimitExceeded with limit 3, got {err:?}"
        );
    }

    #[test]
    fn codes_are_stable() {
        let cases = [
            (JsonError::Invalid { line: 1, column: 1 }, "INVALID_JSON"),
            (JsonError::DuplicateMember, "DUPLICATE_MEMBER"),
            (JsonError::NonCanonicalizable, "NON_CANONICALIZABLE_JSON"),
        ];
        for (error, code) in cases {
            assert_eq!(error.code(), code, "stable code for {error:?}");
            assert_eq!(
                CanonicalizeError::Json(error).code(),
                code,
                "wrapping keeps the code"
            );
        }
    }
}
