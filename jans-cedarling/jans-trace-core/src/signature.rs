// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Ed25519 producer signatures and their unpadded base64url form (core design §6.4, §8.2).

use std::fmt;
use std::str::FromStr;

use base64::Engine as _;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use ed25519_dalek::pkcs8::DecodePrivateKey as _;
use ed25519_dalek::{Signer as _, SigningKey};
use thiserror::Error;

/// Text is not the unpadded base64url form of exactly 64 bytes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("signature is not unpadded base64url of 64 bytes")]
pub struct ParseSignatureError;

impl ParseSignatureError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_SIGNATURE_ENCODING"
    }
}

/// A signer could not produce a signature. Deliberately carries no detail, so key material
/// and HSM/KMS responses never travel with it; adapters log their own diagnostics.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("signer could not produce a signature")]
pub struct SignerError;

impl SignerError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "SIGNER_FAILURE"
    }
}

/// Bytes are not an unencrypted PKCS#8 PEM Ed25519 private key (RFC 8410). Carries no
/// detail, so no part of the key text travels with it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("private key is not an unencrypted PKCS#8 PEM Ed25519 key")]
pub struct PrivateKeyError;

impl PrivateKeyError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_PRIVATE_KEY"
    }
}

/// A 64-byte Ed25519 signature as carried in the top-level `signature` member.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SignatureValue([u8; 64]);

impl SignatureValue {
    pub(crate) fn from_bytes(bytes: [u8; 64]) -> Self {
        Self(bytes)
    }

    #[must_use]
    pub fn as_bytes(&self) -> &[u8; 64] {
        &self.0
    }
}

impl fmt::Display for SignatureValue {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&URL_SAFE_NO_PAD.encode(self.0))
    }
}

impl FromStr for SignatureValue {
    type Err = ParseSignatureError;

    /// Accepts only the canonical encoding, so one signature has exactly one text form.
    fn from_str(text: &str) -> Result<Self, Self::Err> {
        let bytes = URL_SAFE_NO_PAD
            .decode(text)
            .map_err(|_| ParseSignatureError)?;
        bytes.try_into().map(Self).map_err(|_| ParseSignatureError)
    }
}

/// Signs TRACE assertions. HSM and KMS adapters implement this outside the crate so the
/// core never handles their key material.
pub trait TraceSigner: Send + Sync {
    /// The `kid` Lock uses to select the verification key; must equal the assertion `kid`.
    fn kid(&self) -> &str;

    fn sign(&self, message: &[u8]) -> Result<[u8; 64], SignerError>;
}

/// In-memory Ed25519 signer for tests and deployments whose key comes from a mounted secret.
/// The secret is zeroized on drop.
pub struct Ed25519Signer {
    kid: String,
    key: SigningKey,
}

impl Ed25519Signer {
    #[must_use]
    pub fn from_secret_bytes(kid: impl Into<String>, secret: &[u8; 32]) -> Self {
        Self {
            kid: kid.into(),
            key: SigningKey::from_bytes(secret),
        }
    }

    /// Loads the key format `openssl genpkey -algorithm ed25519` writes (decision R20).
    /// Takes bytes, not a path: reading the file is the host's job.
    pub fn from_pkcs8_pem(kid: impl Into<String>, pem: &[u8]) -> Result<Self, PrivateKeyError> {
        let text = std::str::from_utf8(pem).map_err(|_| PrivateKeyError)?;
        let key = SigningKey::from_pkcs8_pem(text).map_err(|_| PrivateKeyError)?;
        Ok(Self {
            kid: kid.into(),
            key,
        })
    }

    #[must_use]
    pub fn public_key_bytes(&self) -> [u8; 32] {
        self.key.verifying_key().to_bytes()
    }
}

impl fmt::Debug for Ed25519Signer {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Ed25519Signer")
            .field("kid", &self.kid)
            .field("key", &"<redacted>")
            .finish()
    }
}

impl TraceSigner for Ed25519Signer {
    fn kid(&self) -> &str {
        &self.kid
    }

    fn sign(&self, message: &[u8]) -> Result<[u8; 64], SignerError> {
        Ok(self.key.sign(message).to_bytes())
    }
}

#[cfg(test)]
mod tests {
    use super::{
        Ed25519Signer, ParseSignatureError, PrivateKeyError, SignatureValue, SignerError,
        TraceSigner,
    };

    struct Rfc8032Case {
        secret: &'static str,
        public: &'static str,
        message: &'static [u8],
        signature: &'static str,
    }

    const TEST_1: Rfc8032Case = Rfc8032Case {
        secret: "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
        public: "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
        message: b"",
        signature: "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
    };

    const TEST_2: Rfc8032Case = Rfc8032Case {
        secret: "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
        public: "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
        message: &[0x72],
        signature: "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00",
    };

    fn signer(secret_hex: &str) -> Ed25519Signer {
        let secret: [u8; 32] = hex::decode(secret_hex)
            .expect("test secret is hex")
            .try_into()
            .expect("test secret is 32 bytes");
        Ed25519Signer::from_secret_bytes("key-test", &secret)
    }

    #[test]
    fn matches_rfc8032_vectors() {
        for (name, case) in [("TEST 1", TEST_1), ("TEST 2", TEST_2)] {
            let signer = signer(case.secret);
            assert_eq!(
                hex::encode(signer.public_key_bytes()),
                case.public,
                "public key derived for RFC 8032 {name}"
            );
            let signature = signer
                .sign(case.message)
                .expect("in-memory signer cannot fail");
            assert_eq!(
                hex::encode(signature),
                case.signature,
                "signature for RFC 8032 {name}"
            );
        }
    }

    #[test]
    fn debug_output_hides_secret_key() {
        let text = format!("{:?}", signer(TEST_1.secret));
        assert!(
            text.contains("key-test"),
            "kid is useful in diagnostics: {text}"
        );
        assert!(
            text.contains("redacted"),
            "secret key is never printed: {text}"
        );
    }

    #[test]
    fn signature_value_round_trips_as_unpadded_base64url() {
        // 0xFB bytes hit '+' and '/' in standard base64, so the URL alphabet is exercised.
        let value = SignatureValue::from_bytes([0xFB; 64]);
        let text = value.to_string();
        assert_eq!(
            text.len(),
            86,
            "64 bytes take 86 unpadded base64 characters"
        );
        assert!(
            !text.contains(['=', '+', '/']),
            "no padding and no standard-alphabet characters: {text}"
        );
        let parsed: SignatureValue = text.parse().expect("own output parses back");
        assert_eq!(parsed, value, "text form is lossless");
    }

    #[test]
    fn rejects_non_canonical_signature_text() {
        let valid = SignatureValue::from_bytes([0xFB; 64]).to_string();
        let cases = [
            ("padded", format!("{valid}==")),
            (
                "standard alphabet",
                valid.replace('-', "+").replace('_', "/"),
            ),
            ("truncated", valid[..85].to_owned()),
            ("66 bytes", format!("{valid}AA")),
            ("empty", String::new()),
            ("not base64", "!".repeat(86)),
        ];
        for (name, text) in cases {
            let err = text.parse::<SignatureValue>().expect_err(name);
            assert_eq!(
                err, ParseSignatureError,
                "expected ParseSignatureError for {name}"
            );
        }
    }

    #[test]
    fn codes_are_stable() {
        assert_eq!(
            ParseSignatureError.code(),
            "INVALID_SIGNATURE_ENCODING",
            "stable code from core design §10"
        );
        assert_eq!(
            SignerError.code(),
            "SIGNER_FAILURE",
            "stable code from core design §10"
        );
    }

    /// RFC 8032 TEST 1 secret as written by `openssl pkey` (RFC 8410 PKCS#8 v1).
    const TEST_1_PEM: &str = "-----BEGIN PRIVATE KEY-----\n\
        MC4CAQAwBQYDK2VwBCIEIJ1hsZ3v/VpguoRK9JLsLMREScVpezJpGXA7rAMcrn9g\n\
        -----END PRIVATE KEY-----\n";

    #[test]
    fn loads_unencrypted_pkcs8_pem() {
        let signer = Ed25519Signer::from_pkcs8_pem("key-2026-01", TEST_1_PEM.as_bytes())
            .expect("openssl-written Ed25519 key loads");
        assert_eq!(signer.kid(), "key-2026-01", "kid comes from the caller");
        assert_eq!(
            hex::encode(signer.public_key_bytes()),
            TEST_1.public,
            "same key as RFC 8032 TEST 1"
        );
        let signature = signer
            .sign(TEST_1.message)
            .expect("in-memory signer cannot fail");
        assert_eq!(
            hex::encode(signature),
            TEST_1.signature,
            "signs like RFC 8032 TEST 1"
        );
    }

    #[test]
    fn rejects_anything_but_an_unencrypted_ed25519_private_key() {
        let body = "MC4CAQAwBQYDK2VwBCIEIJ1hsZ3v/VpguoRK9JLsLMREScVpezJpGXA7rAMcrn9g";
        let cases: [(&str, Vec<u8>); 5] = [
            (
                "encrypted label",
                format!(
                    "-----BEGIN ENCRYPTED PRIVATE KEY-----\n{body}\n-----END ENCRYPTED PRIVATE KEY-----\n"
                )
                .into_bytes(),
            ),
            (
                "public key",
                b"-----BEGIN PUBLIC KEY-----\n\
                  MCowBQYDK2VwAyEA11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo=\n\
                  -----END PUBLIC KEY-----\n"
                    .to_vec(),
            ),
            ("not PEM", b"not a key".to_vec()),
            ("empty", Vec::new()),
            ("not UTF-8", vec![0xFF, 0xFE]),
        ];
        for (name, pem) in cases {
            let err = Ed25519Signer::from_pkcs8_pem("k", &pem).expect_err(name);
            assert_eq!(err, PrivateKeyError, "expected PrivateKeyError for {name}");
        }
    }

    #[test]
    fn private_key_code_is_stable() {
        assert_eq!(
            PrivateKeyError.code(),
            "INVALID_PRIVATE_KEY",
            "our code, not in §10"
        );
    }
}
