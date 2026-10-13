// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SHA-256 digest in the TRACE text form `sha256:<64 lowercase hex>` (core design §6.1).

use std::fmt;
use std::str::FromStr;

use sha2::{Digest as _, Sha256};
use thiserror::Error;

const PREFIX: &str = "sha256:";

/// A SHA-256 digest; the text form is strict so equal digests always compare equal as text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Digest([u8; 32]);

/// Text is not `sha256:` followed by 64 lowercase hex characters.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("digest is not `sha256:` followed by 64 lowercase hex characters")]
pub struct InvalidDigest;

impl InvalidDigest {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_DIGEST"
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
    type Err = InvalidDigest;

    fn from_str(text: &str) -> Result<Self, Self::Err> {
        let hex = text.strip_prefix(PREFIX).ok_or(InvalidDigest)?.as_bytes();
        if hex.len() != 64 {
            return Err(InvalidDigest);
        }
        // Length is checked above, so the remainder is empty.
        let (pairs, _) = hex.as_chunks::<2>();
        let mut bytes = [0u8; 32];
        for (byte, [hi, lo]) in bytes.iter_mut().zip(pairs) {
            *byte = (lower_hex_value(*hi)? << 4) | lower_hex_value(*lo)?;
        }
        Ok(Self(bytes))
    }
}

fn lower_hex_value(c: u8) -> Result<u8, InvalidDigest> {
    match c {
        b'0'..=b'9' => Ok(c - b'0'),
        b'a'..=b'f' => Ok(c - b'a' + 10),
        _ => Err(InvalidDigest),
    }
}

#[cfg(test)]
mod tests {
    use super::{Digest, InvalidDigest};

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
    fn rejects_malformed_text() {
        let hex = &EMPTY_SHA256["sha256:".len()..];
        let cases = [
            String::new(),
            "sha256:".to_owned(),
            hex.to_owned(),
            format!("sha512:{hex}"),
            format!("sha256:{}", hex.to_uppercase()),
            format!("sha256:{}", &hex[1..]),
            format!("sha256:{hex}0"),
            format!("sha256:{}", "g".repeat(64)),
        ];
        for text in cases {
            let err = text
                .parse::<Digest>()
                .expect_err("malformed digest text must be rejected");
            assert_eq!(err, InvalidDigest, "expected InvalidDigest for {text:?}");
        }
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
            InvalidDigest.code(),
            "INVALID_DIGEST",
            "stable code from core design §10"
        );
    }
}
