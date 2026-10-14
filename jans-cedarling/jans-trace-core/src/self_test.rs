// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Known-answer checks a host runs at startup (core design §9): a broken build or platform
//! must disable TRACE instead of producing values no one else can reproduce.

use std::fmt;

use thiserror::Error;

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use crate::json::canonicalize_json;
use crate::key::PublicKey;
use crate::receipt::{ReceiptCommitment, ReceiptPosition, StoredReceipt};
use crate::signature::{Ed25519Signer, SignatureValue, TraceSigner};
use crate::{Digest, Limits};

/// A known answer differed. `Display` names the failed algorithms, so the operator sees
/// which part of the build or platform is broken; the host fails closed either way.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("jans-trace-core self-test failed: {reason}")]
pub struct SelfTestError {
    reason: FailedChecks,
}

impl SelfTestError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

/// Failed checks in the order they ran; never empty inside a `SelfTestError`.
#[derive(Debug, Clone, PartialEq, Eq)]
struct FailedChecks(Vec<Check>);

impl fmt::Display for FailedChecks {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        for (index, check) in self.0.iter().enumerate() {
            if index > 0 {
                f.write_str(", ")?;
            }
            write!(f, "{check}")?;
        }
        Ok(())
    }
}

/// One known answer per algorithm the outputs depend on.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
enum Check {
    #[error("canonicalization")]
    Canonicalization,
    #[error("sha256")]
    Sha256,
    #[error("ed25519")]
    Ed25519,
    #[error("receipt hash")]
    ReceiptHash,
}

impl Check {
    const ALL: [Self; 4] = [
        Self::Canonicalization,
        Self::Sha256,
        Self::Ed25519,
        Self::ReceiptHash,
    ];

    fn passes(self) -> bool {
        match self {
            Self::Canonicalization => check_canonicalization(),
            Self::Sha256 => check_sha256(),
            Self::Ed25519 => check_ed25519(),
            Self::ReceiptHash => check_receipt_hash(),
        }
    }
}

/// RFC 8032 §7.1 TEST 1.
const RFC8032_SECRET: [u8; 32] = [
    0x9d, 0x61, 0xb1, 0x9d, 0xef, 0xfd, 0x5a, 0x60, 0xba, 0x84, 0x4a, 0xf4, 0x92, 0xec, 0x2c, 0xc4,
    0x44, 0x49, 0xc5, 0x69, 0x7b, 0x32, 0x69, 0x19, 0x70, 0x3b, 0xac, 0x03, 0x1c, 0xae, 0x7f, 0x60,
];
const RFC8032_SIGNATURE: &str =
    "5VZDAMNgrHKQhuLMgG6CioSHfx645dl02HPgZSJJAVVfuIIVkKM7rMYeOXAc-bRr0lv18FlbviRlUUFDjnoQCw";

/// Runs every check, even after a failure, so the error lists all broken algorithms.
pub fn self_test() -> Result<(), SelfTestError> {
    let failed: Vec<Check> = Check::ALL
        .into_iter()
        .filter(|check| !check.passes())
        .collect();
    evaluate(&failed)
}

fn evaluate(failed: &[Check]) -> Result<(), SelfTestError> {
    if failed.is_empty() {
        return Ok(());
    }
    Err(SelfTestError {
        reason: FailedChecks(failed.to_vec()),
    })
}

fn check_canonicalization() -> bool {
    canonicalize_json(r#"{"b":[1.0,"é"],"a":1e-7}"#, &Limits::default())
        .is_ok_and(|text| text == "{\"a\":1e-7,\"b\":[1,\"é\"]}")
}

fn check_sha256() -> bool {
    Digest::sha256(b"abc").to_string()
        == "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
}

fn check_ed25519() -> bool {
    let Ok(signer) = Ed25519Signer::from_secret_bytes("self-test", &RFC8032_SECRET) else {
        return false;
    };
    let Ok(signature) = signer.sign(b"") else {
        return false;
    };
    let signature = SignatureValue::from_bytes(signature);
    signature.to_string() == RFC8032_SIGNATURE
        && PublicKey::from_bytes(&signer.public_key_bytes())
            .is_ok_and(|key| key.verify(b"", &signature) && !key.verify(b"x", &signature))
}

fn check_receipt_hash() -> bool {
    let Ok(received_at) = "2026-09-17T20:15:32.123Z".parse() else {
        return false;
    };
    let Ok(receipt) = ReceiptCommitment::from_stored(StoredReceipt {
        evidence_domain_id: "domain-1".to_owned(),
        position: ReceiptPosition::GENESIS,
        received_at,
        producer_id: "cedarling-fleet-1".to_owned(),
        record_id: "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41".to_owned(),
        content_digest: Digest::sha256(b"record"),
    }) else {
        return false;
    };
    receipt.receipt_hash().to_string()
        == "sha256:6f46f99849a5a88987dfdc81ce9b2c65aafc64f3e4a687fc78e93f38d1523908"
}

#[cfg(test)]
mod tests {
    use super::{Check, FailedChecks, SelfTestError, evaluate, self_test};
    use crate::ErrorCode;

    #[test]
    fn passes_on_a_correct_build() {
        self_test().expect("every known answer matches");
        for check in Check::ALL {
            assert!(check.passes(), "known answer for {check}");
        }
    }

    #[test]
    fn reports_every_failed_check_in_order() {
        let err = evaluate(&[Check::ReceiptHash, Check::Canonicalization])
            .expect_err("two known answers differ");
        assert_eq!(
            err,
            SelfTestError {
                reason: FailedChecks(vec![Check::ReceiptHash, Check::Canonicalization]),
            },
            "all failed checks are kept"
        );
        assert_eq!(
            err.to_string(),
            "jans-trace-core self-test failed: receipt hash, canonicalization",
            "`{{err}}` alone tells the operator which algorithms are broken"
        );
        assert!(
            std::error::Error::source(&err).is_none(),
            "the reason is already in Display, so a source chain would print it twice"
        );
        evaluate(&[]).expect("no failed check means success");
    }

    #[test]
    fn code_is_stable() {
        let err = evaluate(&[Check::Sha256]).expect_err("one known answer differs");
        assert_eq!(
            err.code(),
            ErrorCode::SelfTestFailed,
            "our code, not in core design §10"
        );
    }
}
