// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `testing::TestSigner` seen from outside the crate, as downstream tests use it.

use sigstore_verifier::testing::TestSigner;
use sigstore_verifier::{
    IdentityMatch, SigstoreBlobVerifier, SigstoreVerificationError, VerificationPolicy,
};

const ARTIFACT: &[u8] = b"policy-store.cjar bytes";

fn verifier(signer: &TestSigner) -> SigstoreBlobVerifier {
    SigstoreBlobVerifier::new(&signer.trust_root()).expect("signer trust root must parse")
}

#[test]
fn signed_artifact_verifies_with_expected_identity() {
    let signer = TestSigner::new();
    let bundle = signer.sign(ARTIFACT);

    let verified = verifier(&signer)
        .verify(ARTIFACT, &bundle, &TestSigner::policy())
        .expect("a bundle from the test signer must verify under its trust root");

    assert_eq!(
        verified.subject_alternative_name,
        TestSigner::IDENTITY,
        "verified SAN must be the signer's identity"
    );
    assert_eq!(
        verified.issuer,
        TestSigner::ISSUER,
        "verified issuer must be the signer's issuer"
    );
    assert_eq!(
        verified.verified_at,
        TestSigner::INTEGRATED_TIME,
        "verified time must be the signer's integratedTime"
    );
}

#[test]
fn modified_artifact_is_rejected() {
    let signer = TestSigner::new();
    let bundle = signer.sign(ARTIFACT);
    let mut tampered = ARTIFACT.to_vec();
    tampered[0] ^= 1;

    let err = verifier(&signer)
        .verify(&tampered, &bundle, &TestSigner::policy())
        .expect_err("a one-bit change to the artifact must fail verification");
    assert!(
        matches!(err, SigstoreVerificationError::SignatureMismatch { .. }),
        "expected SignatureMismatch, got: {err:?}"
    );
}

#[test]
fn wrong_identity_is_rejected() {
    let signer = TestSigner::new();
    let bundle = signer.sign(ARTIFACT);
    let policy = VerificationPolicy {
        cert_identity: IdentityMatch::Exact("https://github.com/other/repo".to_string()),
        cert_issuer: TestSigner::ISSUER.to_string(),
    };

    let err = verifier(&signer)
        .verify(ARTIFACT, &bundle, &policy)
        .expect_err("a SAN the policy does not name must fail verification");
    assert!(
        matches!(err, SigstoreVerificationError::PolicyViolation { .. }),
        "expected PolicyViolation, got: {err:?}"
    );
}

#[test]
fn static_trust_root_does_not_trust_the_test_signer() {
    let signer = TestSigner::new();
    let bundle = signer.sign(ARTIFACT);

    SigstoreBlobVerifier::with_static_trust_root()
        .verify(ARTIFACT, &bundle, &TestSigner::policy())
        .expect_err("test bundles must never verify under the public-good trust root");
}
