// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SHA-256 digest in the TRACE text form `sha256:<64 lowercase hex>` (core design §6.1).

use std::fmt;
use std::str::FromStr;

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use sha2::{Digest as _, Sha256};
use thiserror::Error;

const PREFIX: &str = "sha256:";

/// A SHA-256 digest; the text form is strict so equal digests always compare equal as text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Digest([u8; 32]);

/// Text is not `sha256:` followed by 64 lowercase hex characters. `Display` includes the
/// reason; a digest is public, so naming the offending character is safe.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("digest is not `sha256:` followed by 64 lowercase hex characters: {reason}")]
pub struct ParseDigestError {
    reason: DigestReason,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
enum DigestReason {
    #[error("text does not start with `sha256:`")]
    MissingPrefix,
    #[error("{0} hex characters after the prefix, expected 64")]
    WrongLength(usize),
    #[error("byte 0x{byte:02x} at offset {offset} is not a lowercase hex digit")]
    NotLowerHex { offset: usize, byte: u8 },
}

impl From<DigestReason> for ParseDigestError {
    fn from(reason: DigestReason) -> Self {
        Self { reason }
    }
}

impl ParseDigestError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

impl Digest {
    /// Predecessor of the first link in a producer chain or a receipt chain. A protocol
    /// constant, deliberately not the hash of any input.
    pub const GENESIS: Self = Self([0; 32]);

    pub(crate) fn sha256(data: &[u8]) -> Self {
        let mut bytes = [0u8; 32];
        bytes.copy_from_slice(&Sha256::digest(data));
        Self(bytes)
    }

    #[must_use]
    pub fn as_bytes(&self) -> &[u8; 32] {
        &self.0
    }
}

impl fmt::Display for Digest {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(PREFIX)?;
        self.0.iter().try_for_each(|byte| write!(f, "{byte:02x}"))
    }
}

impl FromStr for Digest {
    type Err = ParseDigestError;

    fn from_str(text: &str) -> Result<Self, Self::Err> {
        let hex = text
            .strip_prefix(PREFIX)
            .ok_or(DigestReason::MissingPrefix)?
            .as_bytes();
        if hex.len() != 64 {
            return Err(DigestReason::WrongLength(hex.len()).into());
        }
        // Length is checked above, so the remainder is empty.
        let (pairs, _) = hex.as_chunks::<2>();
        let mut bytes = [0u8; 32];
        for (index, (byte, [hi, lo])) in bytes.iter_mut().zip(pairs).enumerate() {
            let offset = PREFIX.len() + 2 * index;
            *byte = (lower_hex_value(*hi, offset)? << 4) | lower_hex_value(*lo, offset + 1)?;
        }
        Ok(Self(bytes))
    }
}

/// `offset` is the position of `c` in the whole digest text, for the error.
fn lower_hex_value(c: u8, offset: usize) -> Result<u8, DigestReason> {
    match c {
        b'0'..=b'9' => Ok(c - b'0'),
        b'a'..=b'f' => Ok(c - b'a' + 10),
        _ => Err(DigestReason::NotLowerHex { offset, byte: c }),
    }
}

#[cfg(test)]
mod tests {
    use super::{Digest, DigestReason, ParseDigestError};
    use crate::ErrorCode;

    const EMPTY_SHA256: &str =
        "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    #[test]
    fn formats_sha256_with_prefix_and_lowercase_hex() {
        assert_eq!(
            Digest::sha256(b"").to_string(),
            EMPTY_SHA256,
            "SHA-256 of empty input in the TRACE text form"
        );
    }

    #[test]
    fn round_trips_through_text() {
        let digest = Digest::sha256(b"abc");
        let parsed: Digest = digest.to_string().parse().expect("own output parses back");
        assert_eq!(parsed, digest, "text form is lossless");
    }

    #[test]
    fn rejects_malformed_text_with_its_reason() {
        let hex = &EMPTY_SHA256["sha256:".len()..];
        let cases = [
            (String::new(), DigestReason::MissingPrefix),
            ("sha256:".to_owned(), DigestReason::WrongLength(0)),
            (hex.to_owned(), DigestReason::MissingPrefix),
            (format!("sha512:{hex}"), DigestReason::MissingPrefix),
            (
                format!("sha256:{}", hex.to_uppercase()),
                DigestReason::NotLowerHex {
                    offset: 7,
                    byte: b'E',
                },
            ),
            (
                format!("sha256:{}", &hex[1..]),
                DigestReason::WrongLength(63),
            ),
            (format!("sha256:{hex}0"), DigestReason::WrongLength(65)),
            (
                format!("sha256:{}g", &hex[..63]),
                DigestReason::NotLowerHex {
                    offset: 70,
                    byte: b'g',
                },
            ),
        ];
        for (text, reason) in cases {
            let err = text
                .parse::<Digest>()
                .expect_err("malformed digest text must be rejected");
            assert_eq!(err, ParseDigestError { reason }, "reason for {text:?}");
        }
    }

    #[test]
    fn display_carries_the_reason() {
        let err = "sha256:abc"
            .parse::<Digest>()
            .expect_err("3 hex characters are not a SHA-256 digest");
        assert_eq!(
            err.to_string(),
            "digest is not `sha256:` followed by 64 lowercase hex characters: \
             3 hex characters after the prefix, expected 64",
            "`{{err}}` alone tells the operator what is wrong"
        );
    }

    #[test]
    fn genesis_is_the_all_zero_sentinel() {
        assert_eq!(
            Digest::GENESIS.to_string(),
            format!("sha256:{}", "0".repeat(64)),
            "genesis sentinel from core design §7.3"
        );
        assert_ne!(
            Digest::GENESIS.to_string(),
            EMPTY_SHA256,
            "sentinel is not the SHA-256 of empty input"
        );
    }

    #[test]
    fn code_is_stable() {
        assert_eq!(
            "x".parse::<Digest>().expect_err("not a digest").code(),
            ErrorCode::InvalidDigest,
            "stable code from core design §10"
        );
    }
}
