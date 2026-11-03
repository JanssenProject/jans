// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 8785 JSON Canonicalization Scheme over strictly parsed input.

use std::fmt::Write as _;

use serde_json::Value;
use thiserror::Error;

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use crate::json::{JsonError, NonCanonicalCause, NonCanonicalReason, parse_strict};
use crate::limits::LimitKind;
use crate::{Limits, SizeLimitError};

/// Why `canonicalize_json` rejected its input.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CanonicalizeError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
}

impl CanonicalizeError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

/// Returns the RFC 8785 canonical text of `input` after strict validation.
pub fn canonicalize_json(input: &str, limits: &Limits) -> Result<String, CanonicalizeError> {
    limits.check_input(input)?;
    let value = parse_strict(input, limits)?;
    Ok(canonicalize_value(&value, limits)?)
}

pub(crate) fn canonicalize_value(value: &Value, limits: &Limits) -> Result<String, JsonError> {
    // The canonicalizer refuses only values outside the JCS model; strict parsing already
    // rejects those, so this branch is not expected to run. It reports, not panics.
    let text = serde_json_canonicalizer::to_string(value).map_err(|e| canonicalizer_refused(&e))?;
    // JCS can lengthen numbers (`1e15` becomes 16 digits), so the input bound is not enough.
    if text.len() > limits.max_canonical_bytes() {
        return Err(JsonError::LimitExceeded(SizeLimitError::new(
            LimitKind::CanonicalBytes,
            limits.max_canonical_bytes(),
        )));
    }
    Ok(text)
}

/// Keeps the canonicalizer's message, so `{err}` says why it refused.
fn canonicalizer_refused(error: &serde_json::Error) -> JsonError {
    JsonError::NonCanonicalizable(NonCanonicalReason(NonCanonicalCause::Canonicalizer(
        error.to_string(),
    )))
}

/// Appends the RFC 8785 form of a JSON string (§3.2.2.2, the ECMAScript `JSON.stringify`
/// rules). Infallible, unlike the canonicalizer, so values built from already validated
/// parts hash without an error path; tests compare it with the canonicalizer.
pub(crate) fn write_jcs_string(out: &mut String, text: &str) {
    out.push('"');
    for character in text.chars() {
        match character {
            '"' => out.push_str(r#"\""#),
            '\\' => out.push_str(r"\\"),
            '\u{8}' => out.push_str(r"\b"),
            '\t' => out.push_str(r"\t"),
            '\n' => out.push_str(r"\n"),
            '\u{c}' => out.push_str(r"\f"),
            '\r' => out.push_str(r"\r"),
            control if u32::from(control) < 0x20 => {
                // Writing to a `String` cannot fail.
                let _ = write!(out, "\\u{:04x}", u32::from(control));
            },
            other => out.push(other),
        }
    }
    out.push('"');
}

#[cfg(test)]
mod tests {
    use super::{CanonicalizeError, canonicalize_json, canonicalizer_refused, write_jcs_string};
    use crate::ErrorCode;
    use crate::json::{JsonError, NonCanonicalCause, NonCanonicalReason};
    use crate::limits::LimitKind;
    use crate::{Limits, SizeLimitError};

    fn canonicalize(input: &str) -> Result<String, CanonicalizeError> {
        canonicalize_json(input, &Limits::default())
    }

    fn canonical_text(input: &str) -> String {
        canonicalize(input).expect("input is valid strict JSON")
    }

    #[test]
    fn string_writer_matches_the_canonicalizer_for_every_code_point() {
        let astral = ['\u{10000}', '\u{1F600}', '\u{10FFFF}'];
        let characters = (0..=0xFFFF_u32).filter_map(char::from_u32).chain(astral);
        for character in characters {
            let text = format!("a{character}b");
            let expected =
                serde_json_canonicalizer::to_string(&serde_json::Value::String(text.clone()))
                    .expect("strings always canonicalize");
            let mut actual = String::new();
            write_jcs_string(&mut actual, &text);
            assert_eq!(
                actual,
                expected,
                "JCS form of U+{:04X}",
                u32::from(character)
            );
        }
    }

    #[test]
    fn sorts_members_and_drops_whitespace() {
        assert_eq!(
            canonical_text(r#"{ "b": 1, "a": [true, null, "x"] }"#),
            r#"{"a":[true,null,"x"],"b":1}"#,
            "members are sorted and insignificant whitespace is removed"
        );
    }

    #[test]
    fn formats_numbers_like_ecmascript() {
        assert_eq!(
            canonical_text("[1.0, -0, 1e15, 0.000001, 1e-7]"),
            "[1,0,1000000000000000,0.000001,1e-7]",
            "numbers follow the RFC 8785 / ECMAScript serialization"
        );
    }

    #[test]
    fn accepts_integers_at_the_safe_boundary() {
        assert_eq!(
            canonical_text("[9007199254740991,-9007199254740991]"),
            "[9007199254740991,-9007199254740991]",
            "+/-(2^53-1) is exactly representable and must pass unchanged"
        );
    }

    #[test]
    fn rejects_integers_beyond_the_safe_boundary() {
        for input in [
            "[9007199254740992]",
            "[-9007199254740992]",
            "{\"n\":18446744073709551615}",
            "[9007199254740993.0]",
            "[9.007199254740993e15]",
            "[9007199254740991.6]",
            "[1e30]",
            "[-1e30]",
        ] {
            let err = canonicalize(input)
                .expect_err("integer outside +/-(2^53-1) would be silently rounded by JCS");
            assert_eq!(
                err,
                CanonicalizeError::Json(JsonError::NonCanonicalizable(NonCanonicalReason(
                    NonCanonicalCause::OutOfSafeRange
                ))),
                "reason for {input:?}"
            );
        }
    }

    struct NumberCase {
        input: &'static str,
        canonical: &'static str,
    }

    /// Inside +/-(2^53-1) a fraction or exponent is written as its nearest double, which is what
    /// Jackson, JavaScript and Python read too (RFC 8785 §3.2.2.3). Expected values come from
    /// Node's `JSON.stringify(JSON.parse(input))`.
    #[test]
    fn fractions_and_exponents_in_range_round_like_every_double_reader() {
        let cases = [
            NumberCase {
                input: "[9007199254740991.0]",
                canonical: "[9007199254740991]",
            },
            NumberCase {
                input: "[9.007199254740991e15]",
                canonical: "[9007199254740991]",
            },
            NumberCase {
                input: "[-9007199254740991.4]",
                canonical: "[-9007199254740991]",
            },
            NumberCase {
                input: "[1.00000000000000001]",
                canonical: "[1]",
            },
        ];
        for case in cases {
            assert_eq!(
                canonical_text(case.input),
                case.canonical,
                "{} is accepted and written as its nearest double",
                case.input
            );
        }
    }

    #[test]
    fn non_canonicalizable_display_names_the_reason_without_the_value() {
        let err = canonicalize("[9007199254740992]").expect_err("unsafe integer");
        assert_eq!(
            err.to_string(),
            "JSON value cannot be canonicalized without changing it: a number is outside \
             +/-(2^53-1), the range every JSON reader keeps exact",
            "`{{err}}` names the rule, never the record's number"
        );
    }

    #[test]
    fn a_canonicalizer_refusal_keeps_the_canonicalizer_message() {
        let refusal = serde_json::from_str::<serde_json::Value>("x")
            .expect_err("stands in for an error the canonicalizer would return");
        let message = refusal.to_string();
        let err = canonicalizer_refused(&refusal);
        assert_eq!(
            err,
            JsonError::NonCanonicalizable(NonCanonicalReason(NonCanonicalCause::Canonicalizer(
                message.clone()
            ))),
            "the message is kept, not discarded"
        );
        assert_eq!(
            err.to_string(),
            format!("JSON value cannot be canonicalized without changing it: {message}"),
            "`{{err}}` shows the canonicalizer's own message"
        );
    }

    #[test]
    fn rejects_duplicate_members_at_any_depth() {
        for input in [r#"{"a":1,"a":2}"#, r#"{"x":[{"k":1,"k":1}]}"#] {
            let err = canonicalize(input)
                .expect_err("duplicate members are ambiguous and must not be signed");
            assert!(
                matches!(err, CanonicalizeError::Json(JsonError::DuplicateMember)),
                "expected DuplicateMember for {input:?}, got {err:?}"
            );
        }
    }

    #[test]
    fn rejects_malformed_input() {
        let cases: [(&str, &str); 4] = [
            ("empty input", ""),
            ("trailing data", r#"{"a":1} {"#),
            ("byte order mark", "\u{FEFF}{}"),
            ("lone surrogate", r#"{"a":"\ud800"}"#),
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
        let err = canonicalize_json("{{{{", &Limits::default().with_max_input_bytes(3))
            .expect_err("oversized input must be rejected without parsing");
        assert!(
            err == CanonicalizeError::SizeLimitExceeded(SizeLimitError::new(
                LimitKind::InputBytes,
                3
            )),
            "expected the input-size limit of 3, got {err:?}"
        );
    }

    #[test]
    fn codes_are_stable() {
        let cases = [
            (
                JsonError::Invalid { line: 1, column: 1 },
                ErrorCode::InvalidJson,
            ),
            (JsonError::DuplicateMember, ErrorCode::DuplicateMember),
            (
                JsonError::NonCanonicalizable(NonCanonicalReason(
                    NonCanonicalCause::OutOfSafeRange,
                )),
                ErrorCode::NonCanonicalizableJson,
            ),
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

    /// Asserts that `input` under `limits` is rejected by exactly `expected`.
    fn assert_rejected(name: &str, input: &str, limits: Limits, expected: SizeLimitError) {
        let err = canonicalize_json(input, &limits).expect_err(name);
        assert_eq!(
            err,
            CanonicalizeError::Json(JsonError::LimitExceeded(expected)),
            "rejection for {name}"
        );
    }

    struct LimitCase {
        name: &'static str,
        limits: Limits,
        accepted: &'static str,
        rejected: &'static str,
        expected: SizeLimitError,
    }

    #[test]
    fn structural_limits_accept_the_bound_and_reject_beyond_it() {
        let defaults = Limits::default();
        let cases = [
            LimitCase {
                name: "depth",
                limits: defaults.with_max_depth(2),
                accepted: "[[1]]",
                rejected: "[[[1]]]",
                expected: SizeLimitError::new(LimitKind::Depth, 2),
            },
            LimitCase {
                name: "object depth",
                limits: defaults.with_max_depth(2),
                accepted: r#"{"a":{}}"#,
                rejected: r#"{"a":{"b":{}}}"#,
                expected: SizeLimitError::new(LimitKind::Depth, 2),
            },
            LimitCase {
                name: "members",
                limits: defaults.with_max_object_members(1),
                accepted: r#"{"a":1}"#,
                rejected: r#"{"a":1,"b":2}"#,
                expected: SizeLimitError::new(LimitKind::ObjectMembers, 1),
            },
            LimitCase {
                name: "elements",
                limits: defaults.with_max_array_elements(2),
                accepted: "[1,2]",
                rejected: "[1,2,3]",
                expected: SizeLimitError::new(LimitKind::ArrayElements, 2),
            },
            LimitCase {
                name: "string value",
                limits: defaults.with_max_string_bytes(3),
                accepted: r#"["abc"]"#,
                rejected: r#"["abcd"]"#,
                expected: SizeLimitError::new(LimitKind::StringBytes, 3),
            },
            LimitCase {
                name: "member name",
                limits: defaults.with_max_string_bytes(3),
                accepted: r#"{"abc":1}"#,
                rejected: r#"{"abcd":1}"#,
                expected: SizeLimitError::new(LimitKind::StringBytes, 3),
            },
            // Two-byte UTF-8 characters: the bound counts bytes, not characters.
            LimitCase {
                name: "string bytes",
                limits: defaults.with_max_string_bytes(3),
                accepted: "[\"é\"]",
                rejected: "[\"éé\"]",
                expected: SizeLimitError::new(LimitKind::StringBytes, 3),
            },
        ];
        for case in cases {
            canonicalize_json(case.accepted, &case.limits).expect(case.name);
            assert_rejected(case.name, case.rejected, case.limits, case.expected);
        }
    }

    #[test]
    fn canonical_output_limit_catches_number_expansion() {
        // 6 input bytes become `[1000000000000000]`, 18 canonical bytes.
        let limits = Limits::default().with_max_canonical_bytes(10);
        canonicalize_json("[1]", &limits).expect("short canonical output is accepted");
        assert_rejected(
            "expanded number",
            "[1e15]",
            limits,
            SizeLimitError::new(LimitKind::CanonicalBytes, 10),
        );
    }

    #[test]
    fn limit_code_is_size_limit_exceeded() {
        assert_eq!(
            JsonError::LimitExceeded(SizeLimitError::new(LimitKind::Depth, 1)).code(),
            ErrorCode::SizeLimitExceeded,
            "structural limits share the size-limit code from core design §10"
        );
    }
}
