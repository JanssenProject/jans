// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Hashes of decision inputs that are not the record itself: the policy store and tokens
//! (notes 14, decisions R21, R22).

use crate::Digest;

/// `bundle_hash` of a policy store: SHA-256 of the `.cjar` exactly as the loader received it.
/// Hashing the artifact rather than its files keeps the value independent of what the
/// loader reads, and lets Lock verify it by hashing the archive it distributes.
#[must_use]
pub fn compute_bundle_hash(archive: &[u8]) -> Digest {
    Digest::sha256(archive)
}

/// Identity of a token without `jti`: SHA-256 of the UTF-8 text of the token exactly as
/// received, compact JWT or not. Tokens travel as text (RFC 6749 tokens, compact JWTs), so the
/// input is text too and no binding can pick a wrong charset. Every producer uses this one
/// function, so `(issuer, fingerprint)` correlates.
#[must_use]
pub fn token_fingerprint(token: &str) -> Digest {
    Digest::sha256(token.as_bytes())
}

#[cfg(test)]
mod tests {
    use super::{compute_bundle_hash, token_fingerprint};

    #[test]
    fn bundle_hash_is_sha256_of_the_archive_bytes() {
        assert_eq!(
            compute_bundle_hash(b"abc").to_string(),
            "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "SHA-256 of the raw archive, FIPS 180-2 `abc` vector"
        );
    }

    #[test]
    fn bundle_hash_changes_with_any_archive_byte() {
        // A repacked archive differs in bytes even when its files do not.
        assert_ne!(
            compute_bundle_hash(b"PK\x03\x04 one packing"),
            compute_bundle_hash(b"PK\x03\x04 another packing"),
            "the hash identifies the artifact, so different bytes are a different bundle"
        );
    }

    #[test]
    fn token_fingerprint_is_sha256_of_the_token_as_received() {
        assert_eq!(
            token_fingerprint("eyJhbGciOiJub25lIn0.e30.").to_string(),
            "sha256:4badc4f0c891b654107a33eb52b94a3d31ef3bd9dbba322958019ad1a9cc07bb",
            "whole compact token, not only its payload (notes 14 §2)"
        );
    }
}
