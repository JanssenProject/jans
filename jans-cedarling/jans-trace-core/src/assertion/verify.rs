// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lock-side verification of a parsed `SignedAssertion` (core design §6.3, §8).

use thiserror::Error;

use super::{AssertionIdentity, SignedAssertion};
use crate::Digest;
use crate::key::PublicKey;

/// The registered key Lock selected by the record's identity, with the `producer` and `kid`
/// it is registered under. The signed members must equal them (core design §6.3).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VerificationContext {
    pub expected_producer: String,
    pub expected_kid: String,
    pub public_key: PublicKey,
}

/// A record whose signature verified under the expected key. Only `SignedAssertion::verify`
/// creates one, so holding it proves the check happened; Lock builds receipts from it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VerifiedAssertion {
    identity: AssertionIdentity,
    content_digest: Digest,
    signature_input_sha256: Digest,
}

impl VerifiedAssertion {
    #[must_use]
    pub fn identity(&self) -> &AssertionIdentity {
        &self.identity
    }

    #[must_use]
    pub fn content_digest(&self) -> Digest {
        self.content_digest
    }

    /// SHA-256 of the exact bytes the signature covers; lets tooling compare signing inputs.
    #[must_use]
    pub fn signature_input_sha256(&self) -> Digest {
        self.signature_input_sha256
    }
}

impl SignedAssertion {
    /// Cheap identity checks run before the signature check.
    pub fn verify(&self, context: &VerificationContext) -> Result<VerifiedAssertion, VerifyError> {
        if self.identity.producer != context.expected_producer {
            return Err(VerifyError::ProducerMismatch {
                signed: self.identity.producer.clone(),
                expected: context.expected_producer.clone(),
            });
        }
        if self.identity.kid != context.expected_kid {
            return Err(VerifyError::KidMismatch {
                signed: self.identity.kid.clone(),
                expected: context.expected_kid.clone(),
            });
        }
        if !context
            .public_key
            .verify(self.signing_input.as_bytes(), &self.signature)
        {
            return Err(VerifyError::SignatureInvalid);
        }
        Ok(VerifiedAssertion {
            identity: self.identity.clone(),
            content_digest: self.content_digest,
            signature_input_sha256: Digest::sha256(self.signing_input.as_bytes()),
        })
    }
}

/// Why `SignedAssertion::verify` rejected the record. Producer and `kid` are public
/// identifiers, so both values are shown.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum VerifyError {
    #[error("signed producer `{signed}` differs from the expected producer `{expected}`")]
    ProducerMismatch { signed: String, expected: String },
    #[error("signed kid `{signed}` differs from the selected key `{expected}`")]
    KidMismatch { signed: String, expected: String },
    #[error("Ed25519 signature does not verify")]
    SignatureInvalid,
}

impl VerifyError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::ProducerMismatch { .. } => "PRODUCER_MISMATCH",
            Self::KidMismatch { .. } => "KID_MISMATCH",
            Self::SignatureInvalid => "SIGNATURE_INVALID",
        }
    }
}

#[cfg(test)]
mod tests {
    use super::{VerificationContext, VerifiedAssertion, VerifyError};
    use crate::assertion::{SignedAssertion, UnsignedAssertion};
    use crate::key::PublicKey;
    use crate::signature::Ed25519Signer;
    use crate::{Digest, Limits};

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
        Ed25519Signer::from_secret_bytes(KID, &[7; 32]).expect("test secret is 32 bytes")
    }

    fn public_key() -> PublicKey {
        PublicKey::from_bytes(&signer().public_key_bytes()).expect("signer key is valid")
    }

    fn sign(unsigned: &str) -> SignedAssertion {
        UnsignedAssertion::parse(unsigned, &Limits::default())
            .expect("test assertion is well formed")
            .sign(&signer())
            .expect("in-memory signer signs")
    }

    /// Appends a top-level `"signature": "<value>"` member to a pretty-printed assertion.
    fn with_signature(assertion: &str, signature: &str) -> String {
        let body = assertion
            .trim_end()
            .strip_suffix('}')
            .expect("test assertion is a JSON object");
        format!(r#"{body}, "signature": "{signature}"}}"#)
    }

    fn received(bytes: &str) -> SignedAssertion {
        SignedAssertion::parse(bytes, &Limits::default()).expect("received record is well formed")
    }

    fn context(producer: &str, kid: &str, key: PublicKey) -> VerificationContext {
        VerificationContext {
            expected_producer: producer.to_owned(),
            expected_kid: kid.to_owned(),
            public_key: key,
        }
    }

    fn verify(signed: &SignedAssertion) -> Result<VerifiedAssertion, VerifyError> {
        signed.verify(&context(PRODUCER, KID, public_key()))
    }

    #[test]
    fn verifies_own_signed_record_and_reports_digests() {
        let signed = sign(UNSIGNED);
        let verified = verify(&signed).expect("own signature verifies");
        assert_eq!(verified.identity().producer(), PRODUCER, "signed producer");
        assert_eq!(verified.identity().kid(), KID, "signed kid");
        assert_eq!(
            verified.identity().record_id(),
            RECORD_ID,
            "signed record id"
        );
        assert_eq!(
            verified.content_digest(),
            signed.content_digest(),
            "same content digest the producer computed"
        );
        assert_eq!(
            verified.signature_input_sha256(),
            Digest::sha256(signed.signing_input().as_bytes()),
            "digest of the exact bytes that were verified"
        );
    }

    #[test]
    fn verifies_received_bytes_in_any_formatting() {
        let produced = sign(UNSIGNED);
        let pretty = received(&with_signature(UNSIGNED, &produced.signature().to_string()));
        let verified = verify(&pretty).expect("formatting is outside the signature");
        assert_eq!(
            verified.content_digest(),
            produced.content_digest(),
            "formatting does not change the content digest"
        );
        let canonical = received(produced.as_str());
        verify(&canonical).expect("the producer's canonical text verifies as received");
    }

    struct ForgeryCase {
        name: &'static str,
        assertion: SignedAssertion,
        key: PublicKey,
    }

    #[test]
    fn rejects_mutated_or_foreign_signatures() {
        let produced = sign(UNSIGNED);
        let signature = produced.signature().to_string();
        let pretty = with_signature(UNSIGNED, &signature);
        let other_key = PublicKey::from_bytes(
            &Ed25519Signer::from_secret_bytes(KID, &[8; 32])
                .expect("test secret is 32 bytes")
                .public_key_bytes(),
        )
        .expect("other key is valid");
        let foreign = sign(&UNSIGNED.replace("AUTHORIZATION_DECISION", "CAPABILITY_INVOKED"))
            .signature()
            .to_string();
        let cases = [
            ForgeryCase {
                name: "signed field changed",
                assertion: received(
                    &pretty.replace("AUTHORIZATION_DECISION", "AUTHORIZATION_DECISIOM"),
                ),
                key: public_key(),
            },
            ForgeryCase {
                name: "nested signature changed",
                assertion: received(&pretty.replace("nested stays signed", "nested stays signeD")),
                key: public_key(),
            },
            ForgeryCase {
                name: "sequence changed",
                assertion: received(
                    &pretty.replace("\"sequence_number\": 1", "\"sequence_number\": 2"),
                ),
                key: public_key(),
            },
            ForgeryCase {
                name: "signature from another assertion",
                assertion: received(&with_signature(UNSIGNED, &foreign)),
                key: public_key(),
            },
            ForgeryCase {
                name: "other key",
                assertion: produced.clone(),
                key: other_key,
            },
        ];
        for case in cases {
            assert!(
                case.assertion.signing_input() != produced.signing_input()
                    || case.assertion.signature() != produced.signature()
                    || case.key != public_key(),
                "{} differs from the original",
                case.name
            );
            let err = case
                .assertion
                .verify(&context(PRODUCER, KID, case.key))
                .expect_err(case.name);
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
        assert_eq!(
            signed.verify(&context("cedarling-fleet-2", KID, public_key())),
            Err(VerifyError::ProducerMismatch {
                signed: PRODUCER.to_owned(),
                expected: "cedarling-fleet-2".to_owned(),
            }),
            "producer is checked"
        );
        assert_eq!(
            signed.verify(&context(PRODUCER, "key-other", public_key())),
            Err(VerifyError::KidMismatch {
                signed: KID.to_owned(),
                expected: "key-other".to_owned(),
            }),
            "kid is checked"
        );
    }

    #[test]
    fn display_names_both_values_of_a_mismatch() {
        let err = sign(UNSIGNED)
            .verify(&context(PRODUCER, "key-other", public_key()))
            .expect_err("kid differs");
        assert_eq!(
            err.to_string(),
            "signed kid `key-2026-01` differs from the selected key `key-other`",
            "`{{err}}` alone tells the operator what is wrong"
        );
    }

    struct CodeCase {
        error: VerifyError,
        expected: &'static str,
    }

    #[test]
    fn codes_are_stable() {
        let mismatch = |name: &str| (name.to_owned(), "other".to_owned());
        let (signed, expected) = mismatch("x");
        let cases = [
            CodeCase {
                error: VerifyError::SignatureInvalid,
                expected: "SIGNATURE_INVALID",
            },
            CodeCase {
                error: VerifyError::ProducerMismatch {
                    signed: signed.clone(),
                    expected: expected.clone(),
                },
                expected: "PRODUCER_MISMATCH",
            },
            CodeCase {
                error: VerifyError::KidMismatch { signed, expected },
                expected: "KID_MISMATCH",
            },
        ];
        for case in cases {
            assert_eq!(
                case.error.code(),
                case.expected,
                "stable code from core design §10"
            );
        }
    }
}
