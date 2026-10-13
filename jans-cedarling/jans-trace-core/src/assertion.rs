// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Producer-side assertion operations: signing input, signature, content digest
//! (core design §6.3, §6.5, §8.1).

use serde_json::Value;
use thiserror::Error;

use crate::canonical::canonicalize_value;
use crate::json::{JsonError, parse_strict};
use crate::{
    Digest, Limits, ParseSignatureError, SignatureValue, SignerError, SizeLimitError, TraceSigner,
};

const SIGNATURE: &str = "signature";
const KID: &str = "kid";

/// One code for every rejection of the assertion's common structure (core design §10).
const INVALID_ASSERTION_SHAPE: &str = "INVALID_ASSERTION_SHAPE";

/// Why `assertion_signing_input` rejected its input.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum SigningInputError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
}

impl SigningInputError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject => INVALID_ASSERTION_SHAPE,
        }
    }
}

/// Why `sign_assertion` produced no signature.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum SignError {
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
    #[error("assertion to sign already has a top-level signature")]
    SignatureAlreadyPresent,
    #[error("assertion has no string `kid` member")]
    InvalidKid,
    /// Lock selects the verification key by the signed `kid`; a mismatch would make every
    /// such record unverifiable.
    #[error("assertion kid differs from the signer kid")]
    KidMismatch,
    #[error(transparent)]
    Signer(#[from] SignerError),
}

impl SignError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::Json(e) => e.code(),
            Self::NotAnObject | Self::InvalidKid => INVALID_ASSERTION_SHAPE,
            Self::SignatureAlreadyPresent => "SIGNATURE_ALREADY_PRESENT",
            Self::KidMismatch => "KID_MISMATCH",
            Self::Signer(e) => e.code(),
        }
    }
}

/// Why `compute_content_digest` rejected its input.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum ContentDigestError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
    #[error("assertion has no top-level signature")]
    MissingSignature,
    #[error(transparent)]
    InvalidSignatureEncoding(#[from] ParseSignatureError),
}

impl ContentDigestError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject => INVALID_ASSERTION_SHAPE,
            Self::MissingSignature => "MISSING_SIGNATURE",
            Self::InvalidSignatureEncoding(e) => e.code(),
        }
    }
}

/// Returns the bytes the producer signs: JCS of the assertion without its top-level
/// `signature`. Accepts signed input so verification tooling can rebuild the same bytes.
pub fn assertion_signing_input(
    assertion: &[u8],
    limits: &Limits,
) -> Result<Vec<u8>, SigningInputError> {
    limits.check_input(assertion)?;
    let Value::Object(mut members) = parse_strict(assertion)? else {
        return Err(SigningInputError::NotAnObject);
    };
    members.remove(SIGNATURE);
    Ok(canonicalize_value(&Value::Object(members))?)
}

/// Signs an unsigned assertion. The caller inserts the result as the top-level `signature`;
/// the input is not modified, so the caller keeps control of its own serialization.
///
/// Takes no `Limits`: the input is the producer's own record, not untrusted bytes.
pub fn sign_assertion(
    unsigned_assertion: &[u8],
    signer: &dyn TraceSigner,
) -> Result<SignatureValue, SignError> {
    let Value::Object(members) = parse_strict(unsigned_assertion)? else {
        return Err(SignError::NotAnObject);
    };
    if members.contains_key(SIGNATURE) {
        return Err(SignError::SignatureAlreadyPresent);
    }
    let Some(Value::String(kid)) = members.get(KID) else {
        return Err(SignError::InvalidKid);
    };
    if kid != signer.kid() {
        return Err(SignError::KidMismatch);
    }
    let input = canonicalize_value(&Value::Object(members))?;
    Ok(SignatureValue::from_bytes(signer.sign(&input)?))
}

/// Fingerprint of the complete signed assertion, signature included; Lock-derived metadata
/// never reaches this function, so it cannot affect the digest.
pub fn compute_content_digest(
    signed_assertion: &[u8],
    limits: &Limits,
) -> Result<Digest, ContentDigestError> {
    limits.check_input(signed_assertion)?;
    let Value::Object(members) = parse_strict(signed_assertion)? else {
        return Err(ContentDigestError::NotAnObject);
    };
    match members.get(SIGNATURE) {
        None => return Err(ContentDigestError::MissingSignature),
        Some(Value::String(text)) => {
            text.parse::<SignatureValue>()?;
        },
        Some(_) => return Err(ParseSignatureError.into()),
    }
    let canonical = canonicalize_value(&Value::Object(members))?;
    Ok(Digest::sha256(&canonical))
}

#[cfg(test)]
mod tests {
    use ed25519_dalek::{Signature, VerifyingKey};

    use super::{
        ContentDigestError, SignError, SigningInputError, assertion_signing_input,
        compute_content_digest, sign_assertion,
    };
    use crate::{
        Digest, Ed25519Signer, JsonError, Limits, SignatureValue, SignerError, TraceSigner,
        canonicalize_json,
    };

    const KID: &str = "key-2026-01";

    const UNSIGNED: &str = r#"{
        "producer": "cedarling-fleet-1",
        "kid": "key-2026-01",
        "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
        "trace": { "event_kind": "AUTHORIZATION_DECISION", "signature": "nested stays signed" },
        "producer_chain": { "sequence_number": 1 }
    }"#;

    fn signer() -> Ed25519Signer {
        Ed25519Signer::from_secret_bytes(KID, &[7; 32])
    }

    /// Stands in for an HSM that is unreachable.
    struct FailingSigner;

    impl TraceSigner for FailingSigner {
        fn kid(&self) -> &str {
            KID
        }

        fn sign(&self, _message: &[u8]) -> Result<[u8; 64], SignerError> {
            Err(SignerError)
        }
    }

    fn signing_input(input: &[u8]) -> Result<Vec<u8>, SigningInputError> {
        assertion_signing_input(input, &Limits::default())
    }

    fn content_digest(input: &[u8]) -> Result<Digest, ContentDigestError> {
        compute_content_digest(input, &Limits::default())
    }

    fn canonical(input: &[u8]) -> Vec<u8> {
        canonicalize_json(input, &Limits::default()).expect("test input canonicalizes")
    }

    /// Appends a top-level `"signature": <raw_json>` member to an assertion object.
    fn with_signature(assertion: &str, raw_json: &str) -> String {
        let body = assertion
            .trim_end()
            .strip_suffix('}')
            .expect("test assertion is a JSON object");
        format!(r#"{body}, "signature": {raw_json}}}"#)
    }

    fn signed_assertion() -> String {
        let signature =
            sign_assertion(UNSIGNED.as_bytes(), &signer()).expect("well-formed assertion signs");
        with_signature(UNSIGNED, &format!("\"{signature}\""))
    }

    #[test]
    fn signature_verifies_over_canonical_signing_input() {
        let signer = signer();
        let signature =
            sign_assertion(UNSIGNED.as_bytes(), &signer).expect("well-formed assertion signs");
        let input = signing_input(UNSIGNED.as_bytes()).expect("signing input builds");
        let key = VerifyingKey::from_bytes(&signer.public_key_bytes()).expect("valid public key");
        key.verify_strict(&input, &Signature::from_bytes(signature.as_bytes()))
            .expect("signature verifies over JCS of the assertion without `signature`");
    }

    #[test]
    fn formatting_does_not_change_signature() {
        let compact = canonical(UNSIGNED.as_bytes());
        let pretty = sign_assertion(UNSIGNED.as_bytes(), &signer()).expect("pretty input signs");
        let canonical = sign_assertion(&compact, &signer()).expect("canonical input signs");
        assert_eq!(
            pretty, canonical,
            "Ed25519 is deterministic over canonical bytes"
        );
    }

    #[test]
    fn signing_input_drops_only_top_level_signature() {
        let signed = with_signature(UNSIGNED, r#""AAAA""#);
        let from_signed = signing_input(signed.as_bytes()).expect("signed assertion is accepted");
        let from_unsigned =
            signing_input(UNSIGNED.as_bytes()).expect("unsigned assertion is accepted");
        assert_eq!(
            from_signed, from_unsigned,
            "top-level signature is outside the signed scope"
        );
        let text = String::from_utf8(from_signed).expect("canonical JSON is UTF-8");
        assert!(
            text.contains(r#""signature":"nested stays signed""#),
            "nested `signature` member stays signed: {text}"
        );
    }

    #[test]
    fn sign_needs_only_kid_among_identity_members() {
        sign_assertion(br#"{"kid":"key-2026-01"}"#, &signer())
            .expect("producer and record_id are checked by verify and Lock, not by signing");
    }

    #[test]
    fn sign_rejects_assertion_that_already_has_signature() {
        let signed = with_signature(UNSIGNED, r#""AAAA""#);
        let err = sign_assertion(signed.as_bytes(), &signer())
            .expect_err("signing an already signed assertion is a producer bug");
        assert!(
            matches!(err, SignError::SignatureAlreadyPresent),
            "expected SignatureAlreadyPresent, got {err:?}"
        );
    }

    #[test]
    fn sign_rejects_kid_that_differs_from_signer() {
        let other = Ed25519Signer::from_secret_bytes("key-other", &[7; 32]);
        let err = sign_assertion(UNSIGNED.as_bytes(), &other)
            .expect_err("Lock would select the wrong verification key");
        assert!(
            matches!(err, SignError::KidMismatch),
            "expected KidMismatch, got {err:?}"
        );
    }

    #[test]
    fn sign_rejects_missing_or_mistyped_kid() {
        for input in [r#"{"producer":"p"}"#, r#"{"kid":7}"#] {
            let err = sign_assertion(input.as_bytes(), &signer())
                .expect_err("without a string kid Lock cannot select the key");
            assert!(
                matches!(err, SignError::InvalidKid),
                "expected InvalidKid for {input}, got {err:?}"
            );
        }
    }

    #[test]
    fn rejects_non_object_assertion() {
        let err = sign_assertion(b"[]", &signer()).expect_err("an array is not an assertion");
        assert!(
            matches!(err, SignError::NotAnObject),
            "sign: expected NotAnObject, got {err:?}"
        );
        let err = signing_input(b"[]").expect_err("an array is not an assertion");
        assert!(
            matches!(err, SigningInputError::NotAnObject),
            "signing input: expected NotAnObject, got {err:?}"
        );
        let err = content_digest(b"[]").expect_err("an array is not an assertion");
        assert!(
            matches!(err, ContentDigestError::NotAnObject),
            "content digest: expected NotAnObject, got {err:?}"
        );
    }

    #[test]
    fn rejects_duplicate_member_before_signing() {
        let input = r#"{"kid":"key-2026-01","kid":"key-2026-01"}"#;
        let err = sign_assertion(input.as_bytes(), &signer())
            .expect_err("duplicate members must be rejected before signing");
        assert!(
            matches!(err, SignError::Json(JsonError::DuplicateMember)),
            "expected Json(DuplicateMember), got {err:?}"
        );
    }

    #[test]
    fn sign_reports_signer_failure() {
        let err = sign_assertion(UNSIGNED.as_bytes(), &FailingSigner)
            .expect_err("a failing HSM must not yield a signature");
        assert!(
            matches!(err, SignError::Signer(SignerError)),
            "expected Signer, got {err:?}"
        );
    }

    #[test]
    fn content_digest_is_sha256_of_complete_canonical_assertion() {
        let signed = signed_assertion();
        let digest = content_digest(signed.as_bytes()).expect("signed assertion digests");
        assert_eq!(
            digest,
            Digest::sha256(&canonical(signed.as_bytes())),
            "digest covers the signature too"
        );

        let other_signature = SignatureValue::from_bytes([1; 64]);
        let other = with_signature(UNSIGNED, &format!("\"{other_signature}\""));
        let other_digest = content_digest(other.as_bytes()).expect("other signature digests");
        assert_ne!(
            other_digest, digest,
            "changing only the signature changes the digest"
        );
    }

    #[test]
    fn content_digest_ignores_formatting() {
        let signed = signed_assertion();
        let compact = canonical(signed.as_bytes());
        assert_eq!(
            content_digest(signed.as_bytes()).expect("pretty input digests"),
            content_digest(&compact).expect("compact input digests"),
            "formatting-only changes keep the digest"
        );
    }

    #[test]
    fn content_digest_requires_valid_signature() {
        let err = content_digest(UNSIGNED.as_bytes())
            .expect_err("content digest is defined only for signed assertions");
        assert!(
            matches!(err, ContentDigestError::MissingSignature),
            "expected MissingSignature, got {err:?}"
        );

        for raw in [r#""AAAA""#, "5"] {
            let input = with_signature(UNSIGNED, raw);
            let err = content_digest(input.as_bytes())
                .expect_err("signature must be 64 bytes of unpadded base64url");
            assert!(
                matches!(err, ContentDigestError::InvalidSignatureEncoding(_)),
                "expected InvalidSignatureEncoding for signature {raw}, got {err:?}"
            );
        }
    }

    #[test]
    fn untrusted_input_functions_enforce_the_limit() {
        let signed = signed_assertion();
        let limits = Limits::default().with_max_input_bytes(signed.len() - 1);
        let err = compute_content_digest(signed.as_bytes(), &limits)
            .expect_err("content digest input above the limit");
        assert!(
            matches!(err, ContentDigestError::SizeLimitExceeded(_)),
            "content digest: expected SizeLimitExceeded, got {err:?}"
        );
        let err = assertion_signing_input(signed.as_bytes(), &limits)
            .expect_err("signing input above the limit");
        assert!(
            matches!(err, SigningInputError::SizeLimitExceeded(_)),
            "signing input: expected SizeLimitExceeded, got {err:?}"
        );
    }

    #[test]
    fn shape_errors_share_the_assertion_shape_code() {
        let cases = [
            (SignError::NotAnObject.code(), "INVALID_ASSERTION_SHAPE"),
            (SignError::InvalidKid.code(), "INVALID_ASSERTION_SHAPE"),
            (
                SigningInputError::NotAnObject.code(),
                "INVALID_ASSERTION_SHAPE",
            ),
            (
                ContentDigestError::NotAnObject.code(),
                "INVALID_ASSERTION_SHAPE",
            ),
            (SignError::KidMismatch.code(), "KID_MISMATCH"),
            (
                SignError::SignatureAlreadyPresent.code(),
                "SIGNATURE_ALREADY_PRESENT",
            ),
            (SignError::Signer(SignerError).code(), "SIGNER_FAILURE"),
            (
                ContentDigestError::MissingSignature.code(),
                "MISSING_SIGNATURE",
            ),
        ];
        for (actual, expected) in cases {
            assert_eq!(actual, expected, "stable code from core design §10");
        }
    }
}
