// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::str::FromStr;

/// Upper bound of Lock's `TraceConstants.PRODUCER_PATTERN` (`jans-lock`).
const MAX_PRODUCER_ID_LEN: usize = 255;

/// Stable id of a TRACE producer, as Lock accepts it.
///
/// Lock matches `^[A-Za-z0-9][A-Za-z0-9._:/-]{0,254}$` and compares the value
/// byte-for-byte, so the id is validated at startup instead of producing records
/// Lock would reject.
///
/// Build it with [`FromStr`] (`str::parse`): there is no unchecked constructor,
/// so every `ProducerId` matches Lock's pattern.
///
/// ```
/// use cedarling::{ParseProducerIdError, ProducerId};
///
/// let id: ProducerId = "cedarling-fleet-1".parse().expect("valid producer id");
/// assert_eq!(id.as_str(), "cedarling-fleet-1");
///
/// let err = "My App".parse::<ProducerId>().expect_err("space is not allowed");
/// assert!(matches!(err, ParseProducerIdError::InvalidChar(' ')));
/// ```
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ProducerId(String);

impl ProducerId {
    /// The id exactly as configured.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for ProducerId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl FromStr for ProducerId {
    type Err = ParseProducerIdError;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        let mut chars = value.chars();
        let first = chars.next().ok_or(ParseProducerIdError::Empty)?;
        if !first.is_ascii_alphanumeric() {
            return Err(ParseProducerIdError::InvalidFirstChar(first));
        }
        if let Some(c) = chars.find(|c| !(c.is_ascii_alphanumeric() || "._:/-".contains(*c))) {
            return Err(ParseProducerIdError::InvalidChar(c));
        }
        // Only ASCII is left, so the byte length is the character count.
        if value.len() > MAX_PRODUCER_ID_LEN {
            return Err(ParseProducerIdError::TooLong(value.len()));
        }
        Ok(Self(value.to_string()))
    }
}

/// Why a string is not a valid [`ProducerId`].
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum ParseProducerIdError {
    /// The id is empty.
    #[error("producer id is empty")]
    Empty,
    /// The id does not start with an ASCII letter or digit.
    #[error("producer id must start with an ASCII letter or digit, found `{0}`")]
    InvalidFirstChar(char),
    /// The id has a character other than ASCII letters, digits and `._:/-`.
    #[error("producer id may contain only ASCII letters, digits and `._:/-`, found `{0}`")]
    InvalidChar(char),
    /// The id is longer than 255 characters.
    #[error("producer id has {0} characters, at most 255 are allowed")]
    TooLong(usize),
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn producer_id_rules() {
        for valid in [
            "cedarling-fleet-1",
            "a",
            "9",
            "urn:jans/pdp.v1_x",
            &"x".repeat(255),
        ] {
            let id = valid.parse::<ProducerId>();
            assert_eq!(
                id.as_ref().map(ProducerId::as_str),
                Ok(valid),
                "`{valid}` should be accepted and kept byte-for-byte"
            );
        }

        let parse_err = |value: &str| {
            value
                .parse::<ProducerId>()
                .expect_err("invalid producer id should be rejected")
        };
        assert!(
            matches!(parse_err(""), ParseProducerIdError::Empty),
            "empty id should be rejected as empty"
        );
        assert!(
            matches!(parse_err("-a"), ParseProducerIdError::InvalidFirstChar('-')),
            "id must start with a letter or digit"
        );
        assert!(
            matches!(parse_err("a b"), ParseProducerIdError::InvalidChar(' ')),
            "space is not allowed"
        );
        assert!(
            matches!(
                parse_err("caf\u{e9}"),
                ParseProducerIdError::InvalidChar('\u{e9}')
            ),
            "non-ASCII is not allowed"
        );
        assert!(
            matches!(
                parse_err(&"x".repeat(256)),
                ParseProducerIdError::TooLong(256)
            ),
            "256 characters is over the limit"
        );
    }
}
