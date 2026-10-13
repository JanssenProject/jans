// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lock-side assertion checks: identity before key lookup, then signature verification
//! (core design §6.3–§6.5, §8; notes 13 §2).

use serde_json::{Map, Value};
use thiserror::Error;

use crate::canonical::canonicalize_value;
use crate::json::{JsonError, parse_strict};
use crate::{Digest, Limits, ParseSignatureError, PublicKey, SignatureValue, SizeLimitError};

const SIGNATURE: &str = "signature";

/// One code for every rejection of the assertion's common structure (core design §10).
const INVALID_ASSERTION_SHAPE: &str = "INVALID_ASSERTION_SHAPE";

/// The members Lock needs to select a key, read from strictly parsed bytes.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AssertionIdentity {
    pub producer: String,
    pub kid: String,
    pub record_id: String,
}

/// What Lock needs after a successful verification.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VerifiedAssertion {
    pub producer: String,
    pub kid: String,
    pub record_id: String,
    /// SHA-256 of the exact bytes the signature covers; lets tooling compare signing inputs.
    pub signature_input_sha256: Digest,
    pub content_digest: Digest,
}

/// The values Lock used to select the key. The signed members must equal them.
#[derive(Debug, Clone, Copy)]
pub struct VerificationContext<'a> {
    pub expected_producer: &'a str,
    pub expected_kid: &'a str,
    pub public_key: &'a PublicKey,
}

/// Why `parse_assertion_identity` rejected its input.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum IdentityError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
    #[error("assertion has no non-empty string `producer`")]
    InvalidProducer,
    #[error("assertion has no non-empty string `kid`")]
    InvalidKid,
    #[error("assertion has no non-empty string `record_id`")]
    InvalidRecordId,
}

impl IdentityError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject
            | Self::InvalidProducer
            | Self::InvalidKid
            | Self::InvalidRecordId => INVALID_ASSERTION_SHAPE,
        }
    }
}

/// Why `verify_assertion` rejected its input.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum VerifyError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
    #[error("assertion has no non-empty string `producer`")]
    InvalidProducer,
    #[error("assertion has no non-empty string `kid`")]
    InvalidKid,
    #[error("assertion has no non-empty string `record_id`")]
    InvalidRecordId,
    #[error("assertion has no top-level signature")]
    MissingSignature,
    #[error(transparent)]
    InvalidSignatureEncoding(#[from] ParseSignatureError),
    #[error("signed producer differs from the expected producer")]
    ProducerMismatch,
    #[error("signed kid differs from the selected key")]
    KidMismatch,
    #[error("Ed25519 signature does not verify")]
    SignatureInvalid,
}

impl VerifyError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject
            | Self::InvalidProducer
            | Self::InvalidKid
            | Self::InvalidRecordId => INVALID_ASSERTION_SHAPE,
            Self::MissingSignature => "MISSING_SIGNATURE",
            Self::InvalidSignatureEncoding(e) => e.code(),
            Self::ProducerMismatch => "PRODUCER_MISMATCH",
            Self::KidMismatch => "KID_MISMATCH",
            Self::SignatureInvalid => "SIGNATURE_INVALID",
        }
    }
}

/// Which identity member is absent or not a non-empty string.
enum BadIdentity {
    Producer,
    Kid,
    RecordId,
}

impl From<BadIdentity> for IdentityError {
    fn from(bad: BadIdentity) -> Self {
        match bad {
            BadIdentity::Producer => Self::InvalidProducer,
            BadIdentity::Kid => Self::InvalidKid,
            BadIdentity::RecordId => Self::InvalidRecordId,
        }
    }
}

impl From<BadIdentity> for VerifyError {
    fn from(bad: BadIdentity) -> Self {
        match bad {
            BadIdentity::Producer => Self::InvalidProducer,
            BadIdentity::Kid => Self::InvalidKid,
            BadIdentity::RecordId => Self::InvalidRecordId,
        }
    }
}

fn read_identity(members: &Map<String, Value>) -> Result<AssertionIdentity, BadIdentity> {
    let text = |name: &str, bad: BadIdentity| match members.get(name) {
        Some(Value::String(value)) if !value.is_empty() => Ok(value.clone()),
        _ => Err(bad),
    };
    Ok(AssertionIdentity {
        producer: text("producer", BadIdentity::Producer)?,
        kid: text("kid", BadIdentity::Kid)?,
        record_id: text("record_id", BadIdentity::RecordId)?,
    })
}

/// Reads `producer`, `kid` and `record_id` so Lock can select the key before verifying.
/// Strict parsing proves the bytes free of duplicates, so any other reader sees the same
/// values afterwards.
pub fn parse_assertion_identity(
    assertion: &[u8],
    limits: &Limits,
) -> Result<AssertionIdentity, IdentityError> {
    limits.check_input(assertion)?;
    let Value::Object(members) = parse_strict(assertion, limits)? else {
        return Err(IdentityError::NotAnObject);
    };
    Ok(read_identity(&members)?)
}

/// Verifies the producer signature against the key Lock selected and returns the identity
/// and `content_digest` Lock stores.
pub fn verify_assertion(
    signed_assertion: &[u8],
    context: &VerificationContext<'_>,
    limits: &Limits,
) -> Result<VerifiedAssertion, VerifyError> {
    limits.check_input(signed_assertion)?;
    let Value::Object(mut members) = parse_strict(signed_assertion, limits)? else {
        return Err(VerifyError::NotAnObject);
    };
    let identity = read_identity(&members)?;
    let signature = match members.get(SIGNATURE) {
        None => return Err(VerifyError::MissingSignature),
        Some(Value::String(text)) => text.parse::<SignatureValue>()?,
        Some(_) => return Err(ParseSignatureError::not_a_string().into()),
    };
    if identity.producer != context.expected_producer {
        return Err(VerifyError::ProducerMismatch);
    }
    if identity.kid != context.expected_kid {
        return Err(VerifyError::KidMismatch);
    }
    let content_digest = Digest::sha256(&canonicalize_value(
        &Value::Object(members.clone()),
        limits,
    )?);
    members.remove(SIGNATURE);
    let signing_input = canonicalize_value(&Value::Object(members), limits)?;
    if !context.public_key.verify(&signing_input, &signature) {
        return Err(VerifyError::SignatureInvalid);
    }
    Ok(VerifiedAssertion {
        producer: identity.producer,
        kid: identity.kid,
        record_id: identity.record_id,
        signature_input_sha256: Digest::sha256(&signing_input),
        content_digest,
    })
}

#[cfg(test)]
mod tests {
    use super::{
        IdentityError, VerificationContext, VerifiedAssertion, VerifyError,
        parse_assertion_identity, verify_assertion,
    };
    use crate::limits::LimitKind;
    use crate::{
        Digest, Ed25519Signer, JsonError, Limits, ParseSignatureError, PublicKey, SignatureValue,
        SizeLimitError, assertion_signing_input, canonicalize_json, compute_content_digest,
        sign_assertion,
    };

    const PRODUCER: &str = "cedarling-fleet-1";
    const KID: &str = "key-2026-01";
    const RECORD_ID: &str = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41";

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

    fn public_key() -> PublicKey {
        PublicKey::from_bytes(&signer().public_key_bytes()).expect("signer key is valid")
    }

    /// Appends a top-level `"signature": <raw_json>` member to an assertion object.
    fn with_signature(assertion: &str, raw_json: &str) -> String {
        let body = assertion
            .trim_end()
            .strip_suffix('}')
            .expect("test assertion is a JSON object");
        format!(r#"{body}, "signature": {raw_json}}}"#)
    }

    fn sign(unsigned: &str) -> String {
        let signature =
            sign_assertion(unsigned.as_bytes(), &signer()).expect("well-formed assertion signs");
        with_signature(unsigned, &format!("\"{signature}\""))
    }

    fn verify(signed: &str) -> Result<VerifiedAssertion, VerifyError> {
        verify_as(signed, PRODUCER, KID, &public_key())
    }

    fn verify_as(
        signed: &str,
        producer: &str,
        kid: &str,
        key: &PublicKey,
    ) -> Result<VerifiedAssertion, VerifyError> {
        let context = VerificationContext {
            expected_producer: producer,
            expected_kid: kid,
            public_key: key,
        };
        verify_assertion(signed.as_bytes(), &context, &Limits::default())
    }

    #[test]
    fn verifies_own_signature_and_reports_digests() {
        let signed = sign(UNSIGNED);
        let verified = verify(&signed).expect("own signature verifies");
        assert_eq!(verified.producer, PRODUCER, "signed producer");
        assert_eq!(verified.kid, KID, "signed kid");
        assert_eq!(verified.record_id, RECORD_ID, "signed record id");
        assert_eq!(
            verified.content_digest,
            compute_content_digest(signed.as_bytes(), &Limits::default())
                .expect("signed assertion digests"),
            "same content digest the producer computes"
        );
        let input = assertion_signing_input(signed.as_bytes(), &Limits::default())
            .expect("signing input builds");
        assert_eq!(
            verified.signature_input_sha256,
            Digest::sha256(&input),
            "digest of the exact bytes that were verified"
        );
    }

    #[test]
    fn reformatted_assertion_still_verifies() {
        let signed = sign(UNSIGNED);
        let compact = canonicalize_json(signed.as_bytes(), &Limits::default())
            .expect("signed assertion canonicalizes");
        let compact = String::from_utf8(compact).expect("canonical JSON is UTF-8");
        let verified = verify(&compact).expect("formatting is outside the signature");
        assert_eq!(
            verified.content_digest,
            verify(&signed).expect("original verifies").content_digest,
            "formatting does not change the content digest"
        );
    }

    struct ForgeryCase {
        name: &'static str,
        assertion: String,
        key: PublicKey,
    }

    #[test]
    fn rejects_mutated_or_foreign_signatures() {
        let signed = sign(UNSIGNED);
        let other_key = PublicKey::from_bytes(
            &Ed25519Signer::from_secret_bytes(KID, &[8; 32]).public_key_bytes(),
        )
        .expect("other key is valid");
        let foreign = {
            let other = sign(&UNSIGNED.replace("AUTHORIZATION_DECISION", "CAPABILITY_INVOKED"));
            let signature = other
                .rsplit_once("\"signature\": ")
                .expect("signed test assertion ends with its signature")
                .1
                .trim_end_matches('}');
            with_signature(UNSIGNED, signature)
        };
        let cases = [
            ForgeryCase {
                name: "signed field changed",
                assertion: signed.replace("AUTHORIZATION_DECISION", "AUTHORIZATION_DECISIOM"),
                key: public_key(),
            },
            ForgeryCase {
                name: "nested signature changed",
                assertion: signed.replace("nested stays signed", "nested stays signeD"),
                key: public_key(),
            },
            ForgeryCase {
                name: "sequence changed",
                assertion: signed.replace("\"sequence_number\": 1", "\"sequence_number\": 2"),
                key: public_key(),
            },
            ForgeryCase {
                name: "signature from another assertion",
                assertion: foreign,
                key: public_key(),
            },
            ForgeryCase {
                name: "other key",
                assertion: signed.clone(),
                key: other_key,
            },
        ];
        for case in cases {
            assert!(
                case.assertion != signed || case.key != public_key(),
                "{} differs from the original in bytes or key",
                case.name
            );
            let err = verify_as(&case.assertion, PRODUCER, KID, &case.key).expect_err(case.name);
            assert_eq!(
                err,
                VerifyError::SignatureInvalid,
                "only the signature check fails for {}",
                case.name
            );
        }
    }

    #[test]
    fn rejects_identity_other_than_expected() {
        let signed = sign(UNSIGNED);
        let err = verify_as(&signed, "cedarling-fleet-2", KID, &public_key())
            .expect_err("record of another producer");
        assert_eq!(err, VerifyError::ProducerMismatch, "producer is checked");
        let err = verify_as(&signed, PRODUCER, "key-other", &public_key())
            .expect_err("record signed under another kid");
        assert_eq!(err, VerifyError::KidMismatch, "kid is checked");
    }

    #[test]
    fn rejects_missing_or_malformed_signature() {
        let err = verify(UNSIGNED).expect_err("unsigned record");
        assert_eq!(err, VerifyError::MissingSignature, "no top-level signature");

        let err = verify(&with_signature(UNSIGNED, r#""AAAA""#)).expect_err("3-byte signature");
        assert_eq!(
            err,
            VerifyError::InvalidSignatureEncoding(
                "AAAA"
                    .parse::<SignatureValue>()
                    .expect_err("3 bytes are not a signature")
            ),
            "signature text is parsed with its reason"
        );

        let err = verify(&with_signature(UNSIGNED, "5")).expect_err("numeric signature");
        assert_eq!(
            err,
            VerifyError::InvalidSignatureEncoding(ParseSignatureError::not_a_string()),
            "a non-string signature is an encoding error"
        );
    }

    #[test]
    fn duplicate_kid_is_rejected_before_any_key_is_used() {
        // A non-strict reader could take the first `kid` and the verifier the second.
        let input = UNSIGNED.replace(
            "\"kid\": \"key-2026-01\",",
            "\"kid\": \"key-2026-01\", \"kid\": \"key-other\",",
        );
        let err = parse_assertion_identity(input.as_bytes(), &Limits::default())
            .expect_err("duplicate kid is ambiguous");
        assert_eq!(
            err,
            IdentityError::Json(JsonError::DuplicateMember),
            "identity rejects the duplicate"
        );
        let err = verify(&with_signature(&input, r#""AAAA""#)).expect_err("duplicate kid");
        assert_eq!(
            err,
            VerifyError::Json(JsonError::DuplicateMember),
            "verify rejects the duplicate before the signature"
        );
    }

    #[test]
    fn identity_reads_signed_and_unsigned_records_without_checking_the_signature() {
        for input in [
            UNSIGNED.to_owned(),
            with_signature(UNSIGNED, r#""not checked here""#),
        ] {
            let identity = parse_assertion_identity(input.as_bytes(), &Limits::default())
                .expect("identity members are well formed");
            assert_eq!(
                (
                    identity.producer.as_str(),
                    identity.kid.as_str(),
                    identity.record_id.as_str()
                ),
                (PRODUCER, KID, RECORD_ID),
                "identity members as signed"
            );
        }
    }

    struct ShapeCase {
        name: &'static str,
        input: String,
        identity_error: IdentityError,
        verify_error: VerifyError,
    }

    #[test]
    fn identity_members_must_be_non_empty_strings() {
        let cases = [
            ShapeCase {
                name: "producer missing",
                input: UNSIGNED.replace("\"producer\": \"cedarling-fleet-1\",", ""),
                identity_error: IdentityError::InvalidProducer,
                verify_error: VerifyError::InvalidProducer,
            },
            ShapeCase {
                name: "producer empty",
                input: UNSIGNED.replace("cedarling-fleet-1", ""),
                identity_error: IdentityError::InvalidProducer,
                verify_error: VerifyError::InvalidProducer,
            },
            ShapeCase {
                name: "kid number",
                input: UNSIGNED.replace("\"key-2026-01\"", "7"),
                identity_error: IdentityError::InvalidKid,
                verify_error: VerifyError::InvalidKid,
            },
            ShapeCase {
                name: "record_id missing",
                input: UNSIGNED.replace(&format!("\"record_id\": \"{RECORD_ID}\","), ""),
                identity_error: IdentityError::InvalidRecordId,
                verify_error: VerifyError::InvalidRecordId,
            },
        ];
        for case in cases {
            assert_ne!(case.input, UNSIGNED, "{} changes the input", case.name);
            let err = parse_assertion_identity(case.input.as_bytes(), &Limits::default())
                .expect_err(case.name);
            assert_eq!(err, case.identity_error, "identity: {}", case.name);
            let err = verify(&with_signature(&case.input, r#""AAAA""#)).expect_err(case.name);
            assert_eq!(err, case.verify_error, "verify: {}", case.name);
        }
    }

    #[test]
    fn assertion_must_be_an_object() {
        let err = parse_assertion_identity(b"[]", &Limits::default()).expect_err("array");
        assert_eq!(err, IdentityError::NotAnObject, "identity: array");
        let err = verify("[]").expect_err("array");
        assert_eq!(err, VerifyError::NotAnObject, "verify: array");
    }

    #[test]
    fn untrusted_input_limits_apply() {
        let signed = sign(UNSIGNED);
        let limit = signed.len() - 1;
        let limits = Limits::default().with_max_input_bytes(limit);
        let expected = SizeLimitError::new(LimitKind::InputBytes, limit);
        let err =
            parse_assertion_identity(signed.as_bytes(), &limits).expect_err("above the limit");
        assert_eq!(
            err,
            IdentityError::SizeLimitExceeded(expected),
            "identity: input size"
        );
        let context = VerificationContext {
            expected_producer: PRODUCER,
            expected_kid: KID,
            public_key: &public_key(),
        };
        let err =
            verify_assertion(signed.as_bytes(), &context, &limits).expect_err("above the limit");
        assert_eq!(
            err,
            VerifyError::SizeLimitExceeded(expected),
            "verify: input size"
        );
    }

    struct CodeCase {
        actual: &'static str,
        expected: &'static str,
    }

    #[test]
    fn codes_are_stable() {
        let cases = [
            CodeCase {
                actual: VerifyError::SignatureInvalid.code(),
                expected: "SIGNATURE_INVALID",
            },
            CodeCase {
                actual: VerifyError::ProducerMismatch.code(),
                expected: "PRODUCER_MISMATCH",
            },
            CodeCase {
                actual: VerifyError::KidMismatch.code(),
                expected: "KID_MISMATCH",
            },
            CodeCase {
                actual: VerifyError::MissingSignature.code(),
                expected: "MISSING_SIGNATURE",
            },
            CodeCase {
                actual: VerifyError::NotAnObject.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: VerifyError::InvalidKid.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: IdentityError::InvalidRecordId.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: IdentityError::Json(JsonError::DuplicateMember).code(),
                expected: "DUPLICATE_MEMBER",
            },
        ];
        for case in cases {
            assert_eq!(
                case.actual, case.expected,
                "stable code from core design §10"
            );
        }
    }
}
