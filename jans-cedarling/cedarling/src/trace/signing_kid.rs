// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::str::FromStr;

/// Lock's `ProducerKeyRegistry.MAX_KID_LENGTH`. Lock counts Java `String` length,
/// that is UTF-16 code units.
const MAX_LEN: usize = 255;

/// Explicit `kid` Lock selects the producer key by
/// (`CEDARLING_TRACE_SIGNING_KID`).
///
/// Build it with [`FromStr`], which rejects a value Lock would not accept, so a
/// [`TraceConfig`](crate::TraceConfig) never holds one.
///
/// ```
/// use cedarling::TraceSigningKid;
///
/// let kid: TraceSigningKid = "cedarling-fleet-1-2026-01".parse().expect("valid kid");
/// assert_eq!(kid.as_str(), "cedarling-fleet-1-2026-01");
/// ```
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TraceSigningKid(String);

impl TraceSigningKid {
    /// The `kid` as Lock receives it in every signed record.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl FromStr for TraceSigningKid {
    type Err = ParseTraceSigningKidError;

    fn from_str(kid: &str) -> Result<Self, Self::Err> {
        if kid.trim().is_empty() {
            return Err(ParseTraceSigningKidError::Blank);
        }
        let len = kid.encode_utf16().count();
        if len > MAX_LEN {
            return Err(ParseTraceSigningKidError::TooLong(len));
        }
        Ok(Self(kid.to_owned()))
    }
}

impl fmt::Display for TraceSigningKid {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

/// Why a value is not a valid [`TraceSigningKid`].
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum ParseTraceSigningKidError {
    /// Lock needs a non-empty `kid`; leave the property unset to use the key thumbprint.
    #[error("the kid is blank")]
    Blank,
    /// Longer than the 255 UTF-16 units Lock stores.
    #[error("the kid is {0} UTF-16 units long, more than 255")]
    TooLong(usize),
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn kid_parses() {
        let kid: TraceSigningKid = "fleet-key-1".parse().expect("plain kid should parse");
        assert_eq!(kid.as_str(), "fleet-key-1", "kid should keep its text");
        assert_eq!(
            kid.to_string(),
            "fleet-key-1",
            "Display should print the kid"
        );
    }

    #[test]
    fn blank_kid_is_error() {
        for blank in ["", "   "] {
            let err = blank
                .parse::<TraceSigningKid>()
                .expect_err("a blank kid should be rejected");
            assert!(
                matches!(err, ParseTraceSigningKidError::Blank),
                "expected Blank for {blank:?}, got {err:?}"
            );
        }
    }

    #[test]
    fn kid_of_255_units_is_ok() {
        "k".repeat(255)
            .parse::<TraceSigningKid>()
            .expect("a kid of 255 UTF-16 units should parse");
    }

    #[test]
    fn kid_over_255_units_is_error() {
        let err = "k"
            .repeat(256)
            .parse::<TraceSigningKid>()
            .expect_err("a kid of 256 UTF-16 units should be rejected");
        assert!(
            matches!(err, ParseTraceSigningKidError::TooLong(256)),
            "expected TooLong(256), got {err:?}"
        );
    }

    #[test]
    fn kid_length_counts_utf16_units() {
        // 128 astral characters are 128 chars but 256 UTF-16 units.
        let err = "😀"
            .repeat(128)
            .parse::<TraceSigningKid>()
            .expect_err("256 UTF-16 units should be rejected");
        assert!(
            matches!(err, ParseTraceSigningKidError::TooLong(256)),
            "expected TooLong(256) counted in UTF-16 units, got {err:?}"
        );
    }
}
