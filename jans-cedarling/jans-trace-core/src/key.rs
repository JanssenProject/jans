// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Producer public keys as Lock registers them: JWK OKP Ed25519 (RFC 8037), with the
//! RFC 7638 thumbprint in TRACE digest form (decision R20).

use base64::Engine as _;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use ed25519_dalek::{Signature, SignatureError, VerifyingKey};
use serde_json::Value;
use thiserror::Error;

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use crate::json::{JsonError, parse_strict};
use crate::signature::SignatureValue;
use crate::{Digest, Limits};

/// Bytes or JWK do not describe a usable Ed25519 public key. `Display` includes the reason;
/// a public key is not secret, but the reason still names members and lengths, not values.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("public key is not a valid Ed25519 key: {reason}")]
pub struct PublicKeyError {
    reason: PublicKeyReason,
}

/// Private, so dependency error types never become part of the public API. Shown through
/// `Display` only, as for the signature and private-key reasons.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
enum PublicKeyReason {
    #[error("JWK is not a JSON object")]
    NotAnObject,
    #[error("JWK `kty` is not \"OKP\"")]
    Kty,
    #[error("JWK `crv` is not \"Ed25519\"")]
    Crv,
    #[error("JWK has a `d` member, so it is a private key")]
    PrivateMember,
    #[error("JWK `x` is missing or not a JSON string")]
    MissingX,
    #[error("{0}")]
    Base64(base64::DecodeError),
    #[error("key is {0} bytes, expected 32")]
    WrongLength(usize),
    #[error("bytes are not a compressed Edwards25519 point")]
    NotOnCurve,
    #[error("key has small order, so no signature would verify under it")]
    SmallOrder,
}

impl From<PublicKeyReason> for PublicKeyError {
    fn from(reason: PublicKeyReason) -> Self {
        Self { reason }
    }
}

impl PublicKeyError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

/// Why `PublicKey::from_jwk` rejected the JWK.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum JwkError {
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error(transparent)]
    InvalidPublicKey(#[from] PublicKeyError),
}

impl From<PublicKeyReason> for JwkError {
    fn from(reason: PublicKeyReason) -> Self {
        Self::InvalidPublicKey(reason.into())
    }
}

impl JwkError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

/// An Ed25519 key that can verify producer signatures.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct PublicKey(VerifyingKey);

impl PublicKey {
    /// Refuses small-order keys: `verify_strict` would reject every signature under them, so
    /// a registration error is better than silent verification failures later.
    /// Takes a slice and checks its length here, so every caller (bindings included) gets the
    /// same rule and message.
    pub fn from_bytes(bytes: &[u8]) -> Result<Self, PublicKeyError> {
        let bytes: &[u8; 32] = bytes
            .try_into()
            .map_err(|_| PublicKeyReason::WrongLength(bytes.len()))?;
        // `SignatureError` is opaque by design (its `Display` is "signature error"), and point
        // decompression is the only way `from_bytes` fails, so naming that cause loses nothing.
        let key = VerifyingKey::from_bytes(bytes)
            .map_err(|_: SignatureError| PublicKeyReason::NotOnCurve)?;
        if key.is_weak() {
            return Err(PublicKeyReason::SmallOrder.into());
        }
        Ok(Self(key))
    }

    /// Parses `{"kty":"OKP","crv":"Ed25519","x":"…"}`. Takes no `Limits`: the JWK comes
    /// from Lock's registry configuration, not from a client.
    pub fn from_jwk(jwk: &str) -> Result<Self, JwkError> {
        let Value::Object(members) = parse_strict(jwk, &Limits::UNBOUNDED)? else {
            return Err(PublicKeyReason::NotAnObject.into());
        };
        let text = |name: &str| match members.get(name) {
            Some(Value::String(value)) => Some(value.as_str()),
            _ => None,
        };
        if text("kty") != Some("OKP") {
            return Err(PublicKeyReason::Kty.into());
        }
        if text("crv") != Some("Ed25519") {
            return Err(PublicKeyReason::Crv.into());
        }
        // `d` makes it a private JWK, which must never sit in a public-key registry.
        if members.contains_key("d") {
            return Err(PublicKeyReason::PrivateMember.into());
        }
        let x = text("x").ok_or(PublicKeyReason::MissingX)?;
        let bytes = URL_SAFE_NO_PAD.decode(x).map_err(PublicKeyReason::Base64)?;
        Ok(Self::from_bytes(&bytes)?)
    }

    /// For keys known to be valid, such as one derived from a private key.
    pub(crate) fn from_verifying_key(key: VerifyingKey) -> Self {
        Self(key)
    }

    #[must_use]
    pub fn to_bytes(&self) -> [u8; 32] {
        self.0.to_bytes()
    }

    /// RFC 7638 thumbprint, written as a TRACE digest rather than base64url so every hash in
    /// TRACE has one form.
    #[must_use]
    pub fn thumbprint(&self) -> Digest {
        let x = URL_SAFE_NO_PAD.encode(self.0.to_bytes());
        // RFC 7638 input: required members only, sorted, no whitespace.
        Digest::sha256(format!(r#"{{"crv":"Ed25519","kty":"OKP","x":"{x}"}}"#).as_bytes())
    }

    /// Strict verification: rejects non-canonical and small-order signature components.
    pub(crate) fn verify(&self, message: &[u8], signature: &SignatureValue) -> bool {
        self.0
            .verify_strict(message, &Signature::from_bytes(signature.as_bytes()))
            .is_ok()
    }
}

#[cfg(test)]
mod tests {
    use base64::DecodeError;
    use base64::Engine as _;
    use base64::engine::general_purpose::URL_SAFE_NO_PAD;

    use super::{JwkError, PublicKey, PublicKeyError, PublicKeyReason};
    use crate::ErrorCode;
    use crate::json::JsonError;
    use crate::signature::{Ed25519Signer, SignatureValue, TraceSigner};

    /// RFC 8037 A.2 public key (the RFC 8032 TEST 1 key).
    const X: &str = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";

    fn jwk(members: &str) -> String {
        format!("{{{members}}}")
    }

    fn rfc8037_key() -> PublicKey {
        PublicKey::from_jwk(&jwk(&format!(r#""kty":"OKP","crv":"Ed25519","x":"{X}""#)))
            .expect("RFC 8037 example JWK is valid")
    }

    #[test]
    fn thumbprint_matches_rfc8037_example() {
        let thumbprint = rfc8037_key().thumbprint();
        assert_eq!(
            URL_SAFE_NO_PAD.encode(thumbprint.as_bytes()),
            "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k",
            "RFC 8037 A.3 thumbprint bytes"
        );
        assert_eq!(
            thumbprint.to_string(),
            "sha256:90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89",
            "TRACE text form of the same bytes (decision R20)"
        );
    }

    #[test]
    fn jwk_metadata_members_do_not_change_the_key() {
        let with_metadata = PublicKey::from_jwk(&jwk(&format!(
            r#""kid":"key-2026-01","use":"sig","alg":"EdDSA","kty":"OKP","crv":"Ed25519","x":"{X}""#
        )))
        .expect("kid, use and alg are allowed");
        assert_eq!(
            with_metadata,
            rfc8037_key(),
            "only kty, crv and x define the key"
        );
    }

    #[test]
    fn jwk_and_raw_bytes_give_the_same_key() {
        let signer =
            Ed25519Signer::from_secret_bytes("k", &[7; 32]).expect("test secret is 32 bytes");
        let raw = PublicKey::from_bytes(&signer.public_key_bytes()).expect("signer key is valid");
        let x = URL_SAFE_NO_PAD.encode(signer.public_key_bytes());
        let from_jwk =
            PublicKey::from_jwk(&jwk(&format!(r#""kty":"OKP","crv":"Ed25519","x":"{x}""#)))
                .expect("JWK of the signer key is valid");
        assert_eq!(from_jwk, raw, "both forms describe one key");
        assert_eq!(
            raw.to_bytes(),
            signer.public_key_bytes(),
            "raw bytes round-trip"
        );
    }

    #[test]
    fn verifies_only_matching_signatures() {
        let signer =
            Ed25519Signer::from_secret_bytes("k", &[7; 32]).expect("test secret is 32 bytes");
        let key = PublicKey::from_bytes(&signer.public_key_bytes()).expect("signer key is valid");
        let signature =
            SignatureValue::from_bytes(signer.sign(b"message").expect("in-memory signer"));
        assert!(key.verify(b"message", &signature), "own signature verifies");
        assert!(
            !key.verify(b"messagf", &signature),
            "other message does not verify"
        );
    }

    struct JwkCase {
        name: &'static str,
        jwk: String,
        reason: PublicKeyReason,
    }

    #[test]
    fn rejects_invalid_jwks_with_their_reason() {
        let cases = [
            JwkCase {
                name: "array",
                jwk: "[]".to_owned(),
                reason: PublicKeyReason::NotAnObject,
            },
            JwkCase {
                name: "wrong kty",
                jwk: jwk(&format!(r#""kty":"EC","crv":"Ed25519","x":"{X}""#)),
                reason: PublicKeyReason::Kty,
            },
            JwkCase {
                name: "missing kty",
                jwk: jwk(&format!(r#""crv":"Ed25519","x":"{X}""#)),
                reason: PublicKeyReason::Kty,
            },
            JwkCase {
                name: "wrong crv",
                jwk: jwk(&format!(r#""kty":"OKP","crv":"X25519","x":"{X}""#)),
                reason: PublicKeyReason::Crv,
            },
            JwkCase {
                name: "private key",
                jwk: jwk(&format!(
                    r#""kty":"OKP","crv":"Ed25519","x":"{X}","d":"AAAA""#
                )),
                reason: PublicKeyReason::PrivateMember,
            },
            JwkCase {
                name: "missing x",
                jwk: jwk(r#""kty":"OKP","crv":"Ed25519""#),
                reason: PublicKeyReason::MissingX,
            },
            JwkCase {
                name: "numeric x",
                jwk: jwk(r#""kty":"OKP","crv":"Ed25519","x":7"#),
                reason: PublicKeyReason::MissingX,
            },
            JwkCase {
                name: "padded x",
                jwk: jwk(&format!(r#""kty":"OKP","crv":"Ed25519","x":"{X}=""#)),
                reason: PublicKeyReason::Base64(DecodeError::InvalidPadding),
            },
            JwkCase {
                name: "standard alphabet x",
                jwk: jwk(&format!(
                    r#""kty":"OKP","crv":"Ed25519","x":"{}""#,
                    X.replace('_', "/")
                )),
                reason: PublicKeyReason::Base64(DecodeError::InvalidByte(13, b'/')),
            },
            JwkCase {
                name: "short x",
                jwk: jwk(r#""kty":"OKP","crv":"Ed25519","x":"AAAA""#),
                reason: PublicKeyReason::WrongLength(3),
            },
            // y = 2 has no x on the curve: (y^2 - 1) / (d*y^2 + 1) is not a square mod p.
            JwkCase {
                name: "not a curve point",
                jwk: jwk(&format!(
                    r#""kty":"OKP","crv":"Ed25519","x":"{}""#,
                    URL_SAFE_NO_PAD.encode(point_with_y(2))
                )),
                reason: PublicKeyReason::NotOnCurve,
            },
        ];
        for case in cases {
            let err = PublicKey::from_jwk(&case.jwk).expect_err(case.name);
            assert_eq!(
                err,
                JwkError::InvalidPublicKey(PublicKeyError {
                    reason: case.reason
                }),
                "reason for {}",
                case.name
            );
        }
    }

    #[test]
    fn rejects_duplicate_jwk_members() {
        let err = PublicKey::from_jwk(&jwk(&format!(
            r#""kty":"OKP","crv":"Ed25519","x":"{X}","x":"{X}""#
        )))
        .expect_err("two `x` members are ambiguous");
        assert_eq!(
            err,
            JwkError::Json(JsonError::DuplicateMember),
            "duplicates are caught by the strict parser"
        );
    }

    /// Compressed Edwards encoding: little-endian y, sign of x in the top bit (here 0).
    fn point_with_y(y: u8) -> [u8; 32] {
        let mut bytes = [0u8; 32];
        bytes[0] = y;
        bytes
    }

    #[test]
    fn rejects_bytes_that_are_not_a_usable_key() {
        assert_eq!(
            PublicKey::from_bytes(&point_with_y(2)).expect_err("y = 2 is not on the curve"),
            PublicKeyError {
                reason: PublicKeyReason::NotOnCurve
            },
            "decompression failure is named"
        );
        // y = 1 is the identity point: a weak key no honest signer produces.
        assert_eq!(
            PublicKey::from_bytes(&point_with_y(1)).expect_err("weak keys are refused"),
            PublicKeyError {
                reason: PublicKeyReason::SmallOrder
            },
            "small-order key is named"
        );
    }

    #[test]
    fn display_carries_the_reason_once() {
        let err = PublicKey::from_jwk(&jwk(r#""kty":"OKP","crv":"Ed25519","x":"AAAA""#))
            .expect_err("3 bytes are not an Ed25519 key");
        assert_eq!(
            err.to_string(),
            "public key is not a valid Ed25519 key: key is 3 bytes, expected 32",
            "`{{err}}` alone tells the operator what is wrong"
        );
        assert!(
            std::error::Error::source(&err).is_none(),
            "the reason is already in Display, so a source chain would print it twice"
        );
    }

    #[test]
    fn codes_are_stable() {
        let key_err = PublicKey::from_bytes(&point_with_y(1)).expect_err("weak key");
        assert_eq!(
            key_err.code(),
            ErrorCode::InvalidPublicKey,
            "stable code from core design §10"
        );
        assert_eq!(
            JwkError::InvalidPublicKey(key_err).code(),
            ErrorCode::InvalidPublicKey,
            "wrapping keeps the code"
        );
        assert_eq!(
            JwkError::Json(JsonError::DuplicateMember).code(),
            ErrorCode::DuplicateMember,
            "JSON failures keep their own code"
        );
    }

    #[test]
    fn from_bytes_refuses_a_wrong_length() {
        for length in [0, 31, 33] {
            assert_eq!(
                PublicKey::from_bytes(&vec![9; length]),
                Err(PublicKeyError {
                    reason: PublicKeyReason::WrongLength(length)
                }),
                "{length} bytes are not an Ed25519 public key"
            );
        }
    }
}
