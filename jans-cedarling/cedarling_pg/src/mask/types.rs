// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Strongly-typed [`MaskType`] enum and its `apply` implementation (M1–M3).

use sha2::{Digest, Sha256};

/// All supported masking strategies.
#[derive(Debug, Clone)]
pub(crate) enum MaskType {
    /// Replace with SQL NULL.
    Null,
    /// Replace with `"***REDACTED***"`.
    Redact,
    /// Pattern-based masking: `#` copies from the *right-aligned* tail of the original;
    /// `X` and `*` emit those literal characters; any other character is emitted verbatim.
    Partial { pattern: String },
    /// Deterministic integer in `[min, max]` derived from `SHA-256(salt ‖ value)`.
    Range { min: i64, max: i64 },
    /// SHA-256 + salt → 64-char lowercase hex. Requires `cedarling.mask_hash_salt` to be set.
    Hash,
    /// Always replace with a fixed string.
    Fixed { value: String },
    /// Internal fallback for unknown `mask_type` strings that bypass the CHECK constraint.
    /// Fail-closed by redacting the value.
    Identity,
}

impl MaskType {
    /// Construct from the two catalog columns (`mask_type`, `mask_value`).
    pub(crate) fn from_parts(mask_type: &str, mask_value: Option<&str>) -> Self {
        match mask_type.trim().to_ascii_lowercase().as_str() {
            "null" => MaskType::Null,
            "redact" => MaskType::Redact,
            "hash" => MaskType::Hash,
            "partial" => MaskType::Partial {
                pattern: mask_value.unwrap_or("****").to_string(),
            },
            "range" => {
                let bounds = mask_value.unwrap_or("0-0");
                let (min, max) = parse_range_bounds(bounds);
                MaskType::Range { min, max }
            },
            "fixed" => MaskType::Fixed {
                value: mask_value.unwrap_or("***").to_string(),
            },
            _ => MaskType::Identity,
        }
    }

    /// Apply this mask to `original` using `salt` for cryptographic operations.
    ///
    /// - `Null` → `None` (SQL NULL).
    /// - `Hash` with empty `salt` → `Some("[HASH_SALT_REQUIRED]")`. The caller should emit a
    ///   `WARNING` when this sentinel is returned.
    /// - All other variants → `Some(masked_string)`.
    pub(crate) fn apply(&self, original: Option<&str>, salt: &[u8]) -> Option<String> {
        let input = original.unwrap_or("");
        match self {
            MaskType::Null => None,
            MaskType::Redact | MaskType::Identity => Some("***REDACTED***".to_string()),
            MaskType::Fixed { value } => Some(value.clone()),
            MaskType::Partial { pattern } => Some(apply_partial(input, pattern)),
            MaskType::Hash => {
                if salt.is_empty() {
                    Some("[HASH_SALT_REQUIRED]".to_string())
                } else {
                    Some(sha256_hex(salt, input))
                }
            },
            MaskType::Range { min, max } => {
                let v = deterministic_range(salt, input, *min, *max);
                Some(v.to_string())
            },
        }
    }

    /// `true` when this variant produces an integer value — used by `cedarling_mask_row`
    /// to write a JSON `Number` rather than a `String`.
    pub(crate) fn preserves_numeric(&self) -> bool {
        matches!(self, MaskType::Range { .. })
    }
}

/// Pattern-walk left-to-right.
/// `#` pulls from the right-aligned tail of `original`; `X`/`*` emit themselves; other chars
/// are copied verbatim. Falls back to `'*'` when the original is too short.
fn apply_partial(original: &str, pattern: &str) -> String {
    let keep_count = pattern.chars().filter(|&c| c == '#').count();
    let orig_chars: Vec<char> = original.chars().collect();
    let keep_chars: Vec<char> = if keep_count == 0 {
        Vec::new()
    } else if orig_chars.len() >= keep_count {
        orig_chars[orig_chars.len() - keep_count..].to_vec()
    } else {
        let mut padded = vec!['*'; keep_count - orig_chars.len()];
        padded.extend_from_slice(&orig_chars);
        padded
    };

    let mut keep_iter = keep_chars.iter();
    let mut out = String::with_capacity(pattern.len());
    for pc in pattern.chars() {
        match pc {
            '#' => out.push(*keep_iter.next().unwrap_or(&'*')),
            'X' | '*' => out.push(pc),
            other => out.push(other),
        }
    }
    out
}

fn sha256_hex(salt: &[u8], value: &str) -> String {
    use std::fmt::Write;
    let mut h = Sha256::new();
    h.update(salt);
    h.update(value.as_bytes());
    h.finalize().iter().fold(String::with_capacity(64), |mut out, b| {
        let _ = write!(out, "{b:02x}");
        out
    })
}

fn deterministic_range(salt: &[u8], value: &str, min: i64, max: i64) -> i64 {
    if min >= max {
        return min;
    }
    let mut h = Sha256::new();
    h.update(salt);
    h.update(value.as_bytes());
    let hash = h.finalize();
    let bytes: [u8; 8] = hash[24..32].try_into().unwrap_or([0u8; 8]);
    let hash_u64 = u64::from_be_bytes(bytes);
    // `min < max` was checked above, so `max - min + 1` is positive and fits u64.
    // The result of `hash_u64 % range` is in `0..range`, so adding it back to `min`
    // (as i128 to avoid wraparound) lands cleanly in `[min, max]`.
    let range = u64::try_from((i128::from(max) - i128::from(min)) + 1).unwrap_or(u64::MAX);
    let offset = i64::try_from(hash_u64 % range).unwrap_or(i64::MAX);
    min.saturating_add(offset)
}

/// Parse `"min-max"` range bounds, including signed integers on both sides.
fn parse_range_bounds(bounds: &str) -> (i64, i64) {
    let trimmed = bounds.trim();
    if trimmed.is_empty() {
        return (0, 0);
    }

    let bytes = trimmed.as_bytes();
    let mut scan_from = 0;
    if bytes.first().is_some_and(|b| *b == b'-' || *b == b'+') {
        scan_from = 1;
    }

    for (i, &b) in bytes.iter().enumerate().skip(scan_from) {
        if b != b'-' {
            continue;
        }
        let min_str = trimmed[..i].trim();
        let max_str = trimmed[i + 1..].trim();
        if let (Ok(min), Ok(max)) = (min_str.parse::<i64>(), max_str.parse::<i64>()) {
            return (min, max);
        }
    }

    (0, 0)
}

#[cfg(test)]
mod tests {
    use super::*;

    const SALT: &[u8] = b"cedarling-pg-test-salt";

    #[test]
    fn null_returns_none() {
        assert_eq!(MaskType::Null.apply(Some("v"), &[]), None);
        assert_eq!(MaskType::Null.apply(None, &[]), None);
    }

    #[test]
    fn redact_returns_sentinel() {
        assert_eq!(
            MaskType::Redact.apply(Some("any"), &[]),
            Some("***REDACTED***".to_string())
        );
    }

    #[test]
    fn fixed_returns_configured_value() {
        let m = MaskType::Fixed { value: "[PROTECTED]".to_string() };
        assert_eq!(m.apply(Some("secret"), &[]), Some("[PROTECTED]".to_string()));
    }

    #[test]
    fn identity_is_fail_closed_redaction() {
        assert_eq!(
            MaskType::Identity.apply(Some("keep"), &[]),
            Some("***REDACTED***".to_string())
        );
        assert_eq!(
            MaskType::Identity.apply(None, &[]),
            Some("***REDACTED***".to_string())
        );
    }

    #[test]
    fn partial_phone_keeps_last_four_digits() {
        let m = MaskType::Partial { pattern: "XXX-XXX-####".to_string() };
        assert_eq!(
            m.apply(Some("5558675309"), SALT),
            Some("XXX-XXX-5309".to_string())
        );
    }

    #[test]
    fn partial_cc_keeps_last_four() {
        let m = MaskType::Partial { pattern: "****-****-****-####".to_string() };
        assert_eq!(
            m.apply(Some("SYNTHCCARDNUM1234"), SALT),
            Some("****-****-****-1234".to_string()),
            "partial mask should keep the last four characters of a synthetic card number"
        );
    }

    #[test]
    fn partial_email_constant_output() {
        let m = MaskType::Partial { pattern: "***@***.com".to_string() };
        assert_eq!(
            m.apply(Some("alice@example.com"), SALT),
            Some("***@***.com".to_string())
        );
    }

    #[test]
    fn partial_short_input_pads_with_asterisk() {
        let m = MaskType::Partial { pattern: "####".to_string() };
        // "ab" has only 2 chars, pattern wants 4 → pad to "**ab"
        assert_eq!(m.apply(Some("ab"), SALT), Some("**ab".to_string()));
    }

    #[test]
    fn partial_no_hash_chars_returns_literal_pattern() {
        let m = MaskType::Partial { pattern: "REDACTED".to_string() };
        assert_eq!(m.apply(Some("anything"), SALT), Some("REDACTED".to_string()));
    }

    #[test]
    fn hash_without_salt_returns_sentinel() {
        assert_eq!(
            MaskType::Hash.apply(Some("value"), &[]),
            Some("[HASH_SALT_REQUIRED]".to_string())
        );
    }

    #[test]
    fn hash_with_salt_returns_64_char_lowercase_hex() {
        let hex = MaskType::Hash.apply(Some("value"), SALT).unwrap();
        assert_eq!(hex.len(), 64);
        assert!(hex.chars().all(|c| c.is_ascii_hexdigit()));
    }

    #[test]
    fn hash_is_deterministic() {
        let a = MaskType::Hash.apply(Some("v"), SALT);
        let b = MaskType::Hash.apply(Some("v"), SALT);
        assert_eq!(a, b);
    }

    #[test]
    fn hash_differs_for_different_values() {
        let a = MaskType::Hash.apply(Some("alice"), SALT);
        let b = MaskType::Hash.apply(Some("bob"), SALT);
        assert_ne!(a, b);
    }

    #[test]
    fn range_within_bounds() {
        let m = MaskType::Range { min: 50_000, max: 150_000 };
        let n: i64 = m.apply(Some("alice"), SALT).unwrap().parse().unwrap();
        assert!((50_000..=150_000).contains(&n), "range out of bounds: {n}");
    }

    #[test]
    fn range_is_deterministic() {
        let m = MaskType::Range { min: 1, max: 1_000 };
        assert_eq!(m.apply(Some("alice"), SALT), m.apply(Some("alice"), SALT));
    }

    #[test]
    fn range_equal_min_max_returns_min() {
        let m = MaskType::Range { min: 42, max: 42 };
        let n: i64 = m.apply(Some("x"), SALT).unwrap().parse().unwrap();
        assert_eq!(n, 42);
    }

    #[test]
    fn from_parts_null() {
        assert!(matches!(MaskType::from_parts("null", None), MaskType::Null));
    }

    #[test]
    fn from_parts_partial_with_pattern() {
        let m = MaskType::from_parts("partial", Some("XXX-XXX-####"));
        assert!(matches!(m, MaskType::Partial { ref pattern } if pattern == "XXX-XXX-####"));
    }

    #[test]
    fn from_parts_range_parses_bounds() {
        let m = MaskType::from_parts("range", Some("50000-150000"));
        assert!(matches!(m, MaskType::Range { min: 50_000, max: 150_000 }));
    }

    #[test]
    fn from_parts_unknown_becomes_identity() {
        assert!(matches!(MaskType::from_parts("bogus_type", None), MaskType::Identity));
    }

    #[test]
    fn parse_range_bounds_negative_min() {
        let (min, max) = parse_range_bounds("-100-100");
        assert_eq!(min, -100, "negative min should parse");
        assert_eq!(max, 100, "positive max should parse");
    }

    #[test]
    fn parse_range_bounds_negative_max() {
        let (min, max) = parse_range_bounds("-10--5");
        assert_eq!(min, -10, "negative min should parse when max is also negative");
        assert_eq!(max, -5, "negative max should parse");
    }
}
