// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE assertions as three states (core design §6.3–§6.5, §8):
//! `UnsignedAssertion` --> `sign` --> `SignedAssertion` --> `verify` --> `VerifiedAssertion`.
//!
//! Each state is reached only by parsing or by the previous step, and parsing checks
//! everything the state promises once: strict JSON, limits, identity members, the presence
//! or absence of the top-level `signature`. Later steps cannot fail on those again.

use serde_json::{Map, Value};
use thiserror::Error;

use crate::json::{JsonError, canonicalize_value, parse_strict, write_jcs_string};
use crate::limits::LimitKind;
use crate::signature::{ParseSignatureError, SignatureValue, SignerError, TraceSigner};
use crate::{Digest, Limits, SizeLimitError};

mod batch;
mod verify;

pub use batch::{ParseBatchError, batch_body, parse_batch};
pub use verify::{VerificationContext, VerifiedAssertion, VerifyError};

const SIGNATURE: &str = "signature";

/// One code for every rejection of the assertion's common structure (core design §10).
const INVALID_ASSERTION_SHAPE: &str = "INVALID_ASSERTION_SHAPE";

/// Bytes the top-level signature member adds to canonical JSON:
/// `,"signature":"<86 base64url characters>"`.
const SIGNATURE_MEMBER_BYTES: usize = ",\"signature\":\"\"".len() + 86;

/// The members Lock needs to select a key, read from strictly parsed bytes: after parsing,
/// any other JSON reader sees the same values, because duplicates were rejected.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AssertionIdentity {
    producer: String,
    kid: String,
    record_id: String,
}

impl AssertionIdentity {
    #[must_use]
    pub fn producer(&self) -> &str {
        &self.producer
    }

    #[must_use]
    pub fn kid(&self) -> &str {
        &self.kid
    }

    #[must_use]
    pub fn record_id(&self) -> &str {
        &self.record_id
    }

    fn read(members: &Map<String, Value>) -> Result<Self, BadIdentity> {
        let text = |name: &str, bad: BadIdentity| match members.get(name) {
            Some(Value::String(value)) if !value.is_empty() => Ok(value.clone()),
            _ => Err(bad),
        };
        Ok(Self {
            producer: text("producer", BadIdentity::Producer)?,
            kid: text("kid", BadIdentity::Kid)?,
            record_id: text("record_id", BadIdentity::RecordId)?,
        })
    }
}

/// Which identity member is absent or not a non-empty string.
enum BadIdentity {
    Producer,
    Kid,
    RecordId,
}

/// A producer's record before signing: strict JSON, an object with non-empty `producer`,
/// `kid` and `record_id`, and no top-level `signature`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UnsignedAssertion {
    identity: AssertionIdentity,
    members: CanonicalMembers,
    signing_input: String,
}

impl UnsignedAssertion {
    /// Also checks that the signed record will fit `limits`, so a record Lock would refuse
    /// for its size fails here, before it is signed and sent.
    pub fn parse(assertion: &str, limits: &Limits) -> Result<Self, ParseUnsignedAssertionError> {
        limits.check_input(assertion)?;
        let Value::Object(members) = parse_strict(assertion, limits)? else {
            return Err(ParseUnsignedAssertionError::NotAnObject);
        };
        if members.contains_key(SIGNATURE) {
            return Err(ParseUnsignedAssertionError::SignatureAlreadyPresent);
        }
        let identity = AssertionIdentity::read(&members)?;
        let members = CanonicalMembers::new(members)?;
        let signing_input = members.write(None);
        let signed_len = signing_input.len() + SIGNATURE_MEMBER_BYTES;
        for (kind, limit) in [
            (LimitKind::InputBytes, limits.max_input_bytes()),
            (LimitKind::CanonicalBytes, limits.max_canonical_bytes()),
        ] {
            if signed_len > limit {
                return Err(SizeLimitError::new(kind, limit).into());
            }
        }
        Ok(Self {
            identity,
            members,
            signing_input,
        })
    }

    #[must_use]
    pub fn identity(&self) -> &AssertionIdentity {
        &self.identity
    }

    /// The text the signature covers: JCS of the record (core design §6.3).
    #[must_use]
    pub fn signing_input(&self) -> &str {
        &self.signing_input
    }

    /// The signed record is the JCS form of the whole assertion, signature included, so the
    /// producer sends exactly the text `content_digest` covers.
    pub fn sign(&self, signer: &dyn TraceSigner) -> Result<SignedAssertion, SignError> {
        if self.identity.kid != signer.kid() {
            return Err(SignError::KidMismatch {
                assertion: self.identity.kid.clone(),
                signer: signer.kid().to_owned(),
            });
        }
        let signature = SignatureValue::from_bytes(signer.sign(self.signing_input.as_bytes())?);
        let text = self.members.write(Some(&signature));
        Ok(SignedAssertion {
            content_digest: Digest::sha256(text.as_bytes()),
            text,
            identity: self.identity.clone(),
            signature,
            signing_input: self.signing_input.clone(),
        })
    }
}

/// A record with a well-formed top-level `signature`, not yet verified. Holds the text it
/// was parsed from (or the canonical text `sign` produced): that is what Lock stores, and
/// there is deliberately no way to serialize the record again.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SignedAssertion {
    text: String,
    identity: AssertionIdentity,
    signature: SignatureValue,
    signing_input: String,
    content_digest: Digest,
}

impl SignedAssertion {
    /// Parses an untrusted record once; identity, signature encoding, signing input and
    /// `content_digest` are all known afterwards.
    pub fn parse(assertion: &str, limits: &Limits) -> Result<Self, ParseSignedAssertionError> {
        limits.check_input(assertion)?;
        let Value::Object(mut members) = parse_strict(assertion, limits)? else {
            return Err(ParseSignedAssertionError::NotAnObject);
        };
        let identity = AssertionIdentity::read(&members)?;
        let signature = match members.remove(SIGNATURE) {
            None => return Err(ParseSignedAssertionError::MissingSignature),
            Some(Value::String(text)) => text.parse::<SignatureValue>()?,
            Some(_) => return Err(ParseSignatureError::not_a_string().into()),
        };
        let members = CanonicalMembers::new(members)?;
        let canonical = members.write(Some(&signature));
        // JCS can lengthen numbers (`1e20` becomes 21 digits), so the input bound is not enough.
        if canonical.len() > limits.max_canonical_bytes() {
            return Err(SizeLimitError::new(
                LimitKind::CanonicalBytes,
                limits.max_canonical_bytes(),
            )
            .into());
        }
        Ok(Self {
            text: assertion.to_owned(),
            identity,
            signature,
            signing_input: members.write(None),
            content_digest: Digest::sha256(canonical.as_bytes()),
        })
    }

    /// The record exactly as received or as `sign` produced it.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.text
    }

    #[must_use]
    pub fn identity(&self) -> &AssertionIdentity {
        &self.identity
    }

    #[must_use]
    pub fn signature(&self) -> &SignatureValue {
        &self.signature
    }

    /// JCS of the record without its top-level `signature`, for tooling that compares what
    /// was signed.
    #[must_use]
    pub fn signing_input(&self) -> &str {
        &self.signing_input
    }

    /// SHA-256 of JCS of the complete record, signature included (core design §6.5).
    #[must_use]
    pub fn content_digest(&self) -> Digest {
        self.content_digest
    }
}

/// Top-level members in JCS order, each already written as `"name":value`. Built once
/// while parsing, so writing the record with or without a signature cannot fail.
#[derive(Debug, Clone, PartialEq, Eq)]
struct CanonicalMembers(Vec<(String, String)>);

impl CanonicalMembers {
    fn new(members: Map<String, Value>) -> Result<Self, JsonError> {
        let mut written = members
            .into_iter()
            .map(|(name, value)| {
                let mut member = String::new();
                write_jcs_string(&mut member, &name);
                member.push(':');
                member.push_str(&canonicalize_value(&value, &Limits::UNBOUNDED)?);
                Ok((name, member))
            })
            .collect::<Result<Vec<_>, JsonError>>()?;
        written.sort_by(|(a, _), (b, _)| jcs_order(a, b));
        Ok(Self(written))
    }

    /// The JCS object, with the top-level `signature` member inserted in order when given.
    fn write(&self, signature: Option<&SignatureValue>) -> String {
        let signature_member = signature.map(|value| {
            let mut member = String::with_capacity(SIGNATURE_MEMBER_BYTES);
            write_jcs_string(&mut member, SIGNATURE);
            member.push(':');
            write_jcs_string(&mut member, &value.to_string());
            member
        });
        let mut pending = signature_member.as_deref();
        let mut out = String::from("{");
        let push = |out: &mut String, member: &str| {
            if out.len() > 1 {
                out.push(',');
            }
            out.push_str(member);
        };
        for (name, member) in &self.0 {
            if let Some(signature) = pending
                && jcs_order(SIGNATURE, name).is_lt()
            {
                push(&mut out, signature);
                pending = None;
            }
            push(&mut out, member);
        }
        if let Some(signature) = pending {
            push(&mut out, signature);
        }
        out.push('}');
        out
    }
}

/// RFC 8785 sorts member names by their UTF-16 code units, not by code points.
fn jcs_order(a: &str, b: &str) -> std::cmp::Ordering {
    a.encode_utf16().cmp(b.encode_utf16())
}

/// Why `UnsignedAssertion::parse` rejected its input.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum ParseUnsignedAssertionError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("assertion is not a JSON object")]
    NotAnObject,
    #[error("assertion to sign already has a top-level signature")]
    SignatureAlreadyPresent,
    #[error("assertion has no non-empty string `producer`")]
    InvalidProducer,
    #[error("assertion has no non-empty string `kid`")]
    InvalidKid,
    #[error("assertion has no non-empty string `record_id`")]
    InvalidRecordId,
}

impl ParseUnsignedAssertionError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject
            | Self::InvalidProducer
            | Self::InvalidKid
            | Self::InvalidRecordId => INVALID_ASSERTION_SHAPE,
            Self::SignatureAlreadyPresent => "SIGNATURE_ALREADY_PRESENT",
        }
    }
}

impl From<BadIdentity> for ParseUnsignedAssertionError {
    fn from(bad: BadIdentity) -> Self {
        match bad {
            BadIdentity::Producer => Self::InvalidProducer,
            BadIdentity::Kid => Self::InvalidKid,
            BadIdentity::RecordId => Self::InvalidRecordId,
        }
    }
}

/// Why `UnsignedAssertion::sign` produced no signed record.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum SignError {
    /// Lock selects the verification key by the signed `kid`; a mismatch would make every
    /// such record unverifiable.
    #[error("assertion kid `{assertion}` differs from the signer kid `{signer}`")]
    KidMismatch { assertion: String, signer: String },
    #[error(transparent)]
    Signer(#[from] SignerError),
}

impl SignError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::KidMismatch { .. } => "KID_MISMATCH",
            Self::Signer(e) => e.code(),
        }
    }
}

/// Why `SignedAssertion::parse` rejected its input.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum ParseSignedAssertionError {
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
}

impl ParseSignedAssertionError {
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
        }
    }
}

impl From<BadIdentity> for ParseSignedAssertionError {
    fn from(bad: BadIdentity) -> Self {
        match bad {
            BadIdentity::Producer => Self::InvalidProducer,
            BadIdentity::Kid => Self::InvalidKid,
            BadIdentity::RecordId => Self::InvalidRecordId,
        }
    }
}

#[cfg(test)]
mod tests {
    use ed25519_dalek::{Signature, VerifyingKey};

    use super::{
        ParseSignedAssertionError, ParseUnsignedAssertionError, SignError, SignedAssertion,
        UnsignedAssertion,
    };
    use crate::json::{JsonError, canonicalize_json};
    use crate::limits::LimitKind;
    use crate::signature::{
        Ed25519Signer, ParseSignatureError, SignatureValue, SignerError, TraceSigner,
    };
    use crate::{Digest, Limits, SizeLimitError};

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

    fn unsigned(text: &str) -> UnsignedAssertion {
        UnsignedAssertion::parse(text, &Limits::default()).expect("test assertion is well formed")
    }

    fn canonical(text: &str) -> String {
        canonicalize_json(text, &Limits::default()).expect("test input canonicalizes")
    }

    /// Appends a top-level `"signature": <raw_json>` member to an assertion object.
    fn with_signature(assertion: &str, raw_json: &str) -> String {
        let body = assertion
            .trim_end()
            .strip_suffix('}')
            .expect("test assertion is a JSON object");
        format!(r#"{body}, "signature": {raw_json}}}"#)
    }

    #[test]
    fn signing_input_is_jcs_of_the_record() {
        assert_eq!(
            unsigned(UNSIGNED).signing_input(),
            canonical(UNSIGNED),
            "JCS of the assertion, which has no top-level signature"
        );
    }

    #[test]
    fn signature_verifies_over_the_signing_input() {
        let signed = unsigned(UNSIGNED)
            .sign(&signer())
            .expect("in-memory signer signs");
        let key = VerifyingKey::from_bytes(&signer().public_key_bytes()).expect("valid key");
        key.verify_strict(
            canonical(UNSIGNED).as_bytes(),
            &Signature::from_bytes(signed.signature().as_bytes()),
        )
        .expect("signature covers JCS of the assertion without `signature`");
    }

    #[test]
    fn signed_record_is_jcs_of_the_whole_assertion() {
        let signed = unsigned(UNSIGNED)
            .sign(&signer())
            .expect("in-memory signer signs");
        let expected = canonical(&with_signature(
            UNSIGNED,
            &format!("\"{}\"", signed.signature()),
        ));
        assert_eq!(
            signed.as_str(),
            expected,
            "sign produces the canonical record"
        );
        assert_eq!(
            signed.content_digest(),
            Digest::sha256(expected.as_bytes()),
            "content digest covers the signature too"
        );
        assert_eq!(
            signed.signing_input(),
            canonical(UNSIGNED),
            "signing input is kept"
        );
        assert_eq!(
            signed.identity(),
            unsigned(UNSIGNED).identity(),
            "identity is kept"
        );
    }

    #[test]
    fn formatting_does_not_change_the_signature() {
        let compact = canonical(UNSIGNED);
        let pretty = unsigned(UNSIGNED)
            .sign(&signer())
            .expect("pretty input signs");
        let compact = unsigned(&compact)
            .sign(&signer())
            .expect("compact input signs");
        assert_eq!(
            pretty, compact,
            "Ed25519 is deterministic over canonical bytes"
        );
    }

    #[test]
    fn hand_built_jcs_orders_members_like_the_canonicalizer() {
        // UTF-16 order puts U+10000 (surrogates D800 DC00) before U+FB01; code point order
        // would not. Names next to "signature" check where the signature member goes.
        let record = r#"{"producer":"p","kid":"key-2026-01","record_id":"r",
            "ﬁ":1,"𐀀":2,"sign":3,"signaturf":4,"Signature":5,"signaturd":6,"zz":[1.0,1e30]}"#;
        let signed = unsigned(record).sign(&signer()).expect("record signs");
        assert_eq!(signed.signing_input(), canonical(record), "signing input");
        assert_eq!(
            signed.as_str(),
            canonical(&with_signature(
                record,
                &format!("\"{}\"", signed.signature())
            )),
            "signed record"
        );
        let parsed =
            SignedAssertion::parse(signed.as_str(), &Limits::default()).expect("own record parses");
        assert_eq!(
            parsed, signed,
            "parsing the produced bytes gives the same record"
        );
    }

    #[test]
    fn parsing_a_received_record_keeps_its_text() {
        let produced = unsigned(UNSIGNED).sign(&signer()).expect("record signs");
        let pretty = with_signature(UNSIGNED, &format!("\"{}\"", produced.signature()));
        let received =
            SignedAssertion::parse(&pretty, &Limits::default()).expect("pretty record parses");
        assert_eq!(received.as_str(), pretty, "the original text is stored");
        assert_eq!(
            received.content_digest(),
            produced.content_digest(),
            "formatting does not change the content digest"
        );
        let text = received.signing_input();
        assert!(
            text.contains(r#""signature":"nested stays signed""#)
                && !text.contains(&produced.signature().to_string()),
            "only the top-level signature leaves the signing input: {text}"
        );
    }

    struct UnsignedCase {
        name: &'static str,
        input: String,
        error: ParseUnsignedAssertionError,
    }

    #[test]
    fn unsigned_parse_rejects_records_that_cannot_be_signed() {
        let cases = [
            UnsignedCase {
                name: "array",
                input: "[]".to_owned(),
                error: ParseUnsignedAssertionError::NotAnObject,
            },
            UnsignedCase {
                name: "already signed",
                input: with_signature(UNSIGNED, r#""AAAA""#),
                error: ParseUnsignedAssertionError::SignatureAlreadyPresent,
            },
            UnsignedCase {
                name: "producer missing",
                input: UNSIGNED.replace("\"producer\": \"cedarling-fleet-1\",", ""),
                error: ParseUnsignedAssertionError::InvalidProducer,
            },
            UnsignedCase {
                name: "kid number",
                input: UNSIGNED.replace("\"key-2026-01\"", "7"),
                error: ParseUnsignedAssertionError::InvalidKid,
            },
            UnsignedCase {
                name: "record_id empty",
                input: UNSIGNED.replace("9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41", ""),
                error: ParseUnsignedAssertionError::InvalidRecordId,
            },
            UnsignedCase {
                name: "duplicate kid",
                input: UNSIGNED.replace(
                    "\"kid\": \"key-2026-01\",",
                    "\"kid\": \"key-2026-01\", \"kid\": \"key-2026-01\",",
                ),
                error: ParseUnsignedAssertionError::Json(JsonError::DuplicateMember),
            },
        ];
        for case in cases {
            assert_eq!(
                UnsignedAssertion::parse(&case.input, &Limits::default()),
                Err(case.error),
                "reason for {}",
                case.name
            );
        }
    }

    #[test]
    fn unsigned_parse_rejects_a_record_whose_signed_form_would_break_the_limits() {
        let signed_len = unsigned(UNSIGNED)
            .sign(&signer())
            .expect("record signs")
            .as_str()
            .len();
        for (limits, kind) in [
            (
                Limits::default().with_max_input_bytes(signed_len - 1),
                LimitKind::InputBytes,
            ),
            (
                Limits::default().with_max_canonical_bytes(signed_len - 1),
                LimitKind::CanonicalBytes,
            ),
        ] {
            assert!(
                UNSIGNED.len() < signed_len,
                "the unsigned input alone fits, so the projection is what fails"
            );
            assert_eq!(
                UnsignedAssertion::parse(UNSIGNED, &limits),
                Err(ParseUnsignedAssertionError::SizeLimitExceeded(
                    SizeLimitError::new(kind, signed_len - 1)
                )),
                "Lock would refuse the signed record for its size"
            );
        }
        let exact = Limits::default()
            .with_max_input_bytes(signed_len)
            .with_max_canonical_bytes(signed_len);
        UnsignedAssertion::parse(UNSIGNED, &exact)
            .expect("a signed record of exactly the limit is accepted");
    }

    #[test]
    fn sign_rejects_kid_that_differs_from_signer() {
        let other = Ed25519Signer::from_secret_bytes("key-other", &[7; 32]);
        assert_eq!(
            unsigned(UNSIGNED).sign(&other),
            Err(SignError::KidMismatch {
                assertion: KID.to_owned(),
                signer: "key-other".to_owned(),
            }),
            "Lock would select the wrong verification key"
        );
    }

    #[test]
    fn sign_reports_signer_failure() {
        assert_eq!(
            unsigned(UNSIGNED).sign(&FailingSigner),
            Err(SignError::Signer(SignerError)),
            "a failing HSM must not yield a signature"
        );
    }

    struct SignedCase {
        name: &'static str,
        input: String,
        error: ParseSignedAssertionError,
    }

    #[test]
    fn signed_parse_rejects_records_that_cannot_be_verified() {
        let valid = format!("\"{}\"", SignatureValue::from_bytes([0xFB; 64]));
        let cases = [
            SignedCase {
                name: "array",
                input: "[]".to_owned(),
                error: ParseSignedAssertionError::NotAnObject,
            },
            SignedCase {
                name: "unsigned",
                input: UNSIGNED.to_owned(),
                error: ParseSignedAssertionError::MissingSignature,
            },
            SignedCase {
                name: "3-byte signature",
                input: with_signature(UNSIGNED, r#""AAAA""#),
                error: ParseSignedAssertionError::InvalidSignatureEncoding(
                    "AAAA".parse::<SignatureValue>().expect_err("3 bytes"),
                ),
            },
            SignedCase {
                name: "padded signature",
                input: with_signature(
                    UNSIGNED,
                    &format!("\"{}==\"", SignatureValue::from_bytes([0xFB; 64])),
                ),
                error: ParseSignedAssertionError::InvalidSignatureEncoding(
                    format!("{}==", SignatureValue::from_bytes([0xFB; 64]))
                        .parse::<SignatureValue>()
                        .expect_err("padded"),
                ),
            },
            SignedCase {
                name: "numeric signature",
                input: with_signature(UNSIGNED, "5"),
                error: ParseSignedAssertionError::InvalidSignatureEncoding(
                    ParseSignatureError::not_a_string(),
                ),
            },
            SignedCase {
                name: "producer empty",
                input: with_signature(&UNSIGNED.replace("cedarling-fleet-1", ""), &valid),
                error: ParseSignedAssertionError::InvalidProducer,
            },
            SignedCase {
                name: "kid missing",
                input: with_signature(&UNSIGNED.replace("\"kid\": \"key-2026-01\",", ""), &valid),
                error: ParseSignedAssertionError::InvalidKid,
            },
            SignedCase {
                name: "record_id number",
                input: with_signature(
                    &UNSIGNED.replace("\"9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41\"", "7"),
                    &valid,
                ),
                error: ParseSignedAssertionError::InvalidRecordId,
            },
            // A non-strict reader could take the first `kid` and the verifier the second.
            SignedCase {
                name: "duplicate kid",
                input: with_signature(
                    &UNSIGNED.replace(
                        "\"kid\": \"key-2026-01\",",
                        "\"kid\": \"key-2026-01\", \"kid\": \"key-other\",",
                    ),
                    &valid,
                ),
                error: ParseSignedAssertionError::Json(JsonError::DuplicateMember),
            },
        ];
        for case in cases {
            assert_eq!(
                SignedAssertion::parse(&case.input, &Limits::default()),
                Err(case.error),
                "reason for {}",
                case.name
            );
        }
    }

    #[test]
    fn signed_parse_enforces_input_and_canonical_limits() {
        let signed = unsigned(UNSIGNED).sign(&signer()).expect("record signs");
        let pretty = with_signature(UNSIGNED, &format!("\"{}\"", signed.signature()));
        let input_limit = pretty.len() - 1;
        assert_eq!(
            SignedAssertion::parse(
                &pretty,
                &Limits::default().with_max_input_bytes(input_limit)
            ),
            Err(ParseSignedAssertionError::SizeLimitExceeded(
                SizeLimitError::new(LimitKind::InputBytes, input_limit)
            )),
            "input above the limit"
        );
        // `1e20` grows to 21 digits in JCS: within the input bound, beyond the canonical one.
        let grows = with_signature(
            &UNSIGNED.replace("\"sequence_number\": 1", "\"sequence_number\": 1e20"),
            &format!("\"{}\"", SignatureValue::from_bytes([0xFB; 64])),
        );
        let canonical_limit = grows.len() - 50;
        assert_eq!(
            SignedAssertion::parse(
                &grows,
                &Limits::default().with_max_canonical_bytes(canonical_limit)
            ),
            Err(ParseSignedAssertionError::SizeLimitExceeded(
                SizeLimitError::new(LimitKind::CanonicalBytes, canonical_limit)
            )),
            "canonical output above the limit"
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
                actual: ParseUnsignedAssertionError::NotAnObject.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: ParseUnsignedAssertionError::InvalidKid.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: ParseUnsignedAssertionError::SignatureAlreadyPresent.code(),
                expected: "SIGNATURE_ALREADY_PRESENT",
            },
            CodeCase {
                actual: ParseSignedAssertionError::InvalidRecordId.code(),
                expected: "INVALID_ASSERTION_SHAPE",
            },
            CodeCase {
                actual: ParseSignedAssertionError::MissingSignature.code(),
                expected: "MISSING_SIGNATURE",
            },
            CodeCase {
                actual: SignError::Signer(SignerError).code(),
                expected: "SIGNER_FAILURE",
            },
            CodeCase {
                actual: SignError::KidMismatch {
                    assertion: String::new(),
                    signer: String::new(),
                }
                .code(),
                expected: "KID_MISMATCH",
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
