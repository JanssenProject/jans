// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cross-implementation parity: verify a real public-good Sigstore bundle
//! produced against production Fulcio/Rekor/CTFE, using the embedded static
//! trust root — no network.
//!
//! Fixtures are from the sigstore-conformance suite (`happy-path-v0.3`):
//! a v0.3 bundle (`certificate` + `messageSignature` + `hashedrekord` + SET)
//! over the committed artifact `a.txt`, signed by the conformance OIDC beacon.

use sigstore_verifier::{
    IdentityMatch, SigstoreBlobVerifier, SigstoreVerificationError, VerificationPolicy,
};

/// Verify `bundle` over `artifact` and assert it fails with a specific error
/// variant — not merely that it fails (which could mask the wrong rejection).
fn assert_rejected_with(
    bundle: &[u8],
    artifact: &[u8],
    policy: &VerificationPolicy,
    want: fn(&SigstoreVerificationError) -> bool,
    what: &str,
) {
    let verifier = SigstoreBlobVerifier::with_static_trust_root();
    match verifier.verify(artifact, bundle, policy) {
        Ok(_) => panic!("expected rejection ({what}), but verification succeeded"),
        Err(e) if want(&e) => {}
        Err(e) => panic!("expected {what}, but got a different error: {e:?}"),
    }
}

const ARTIFACT: &[u8] = include_bytes!("fixtures/a.txt");
const BUNDLE: &[u8] = include_bytes!("fixtures/happy-path-v0.3.sigstore.json");

const BEACON_SAN: &str = "https://github.com/sigstore-conformance/extremely-dangerous-public-oidc-beacon/.github/workflows/extremely-dangerous-oidc-beacon.yml@refs/heads/main";
const GHA_ISSUER: &str = "https://token.actions.githubusercontent.com";

#[test]
fn real_public_good_bundle_verifies_against_static_trust_root() {
    let verifier = SigstoreBlobVerifier::with_static_trust_root();
    let policy = VerificationPolicy {
        cert_identity: IdentityMatch::Exact(BEACON_SAN.to_string()),
        cert_issuer: GHA_ISSUER.to_string(),
    };

    let result = verifier
        .verify(ARTIFACT, BUNDLE, &policy)
        .expect("a real public-good v0.3 bundle must verify against the embedded trust root");

    assert_eq!(result.subject_alternative_name, BEACON_SAN);
    assert_eq!(result.issuer, GHA_ISSUER);
    assert!(result.verified_at > 0, "integratedTime must be set");
}

fn beacon_policy() -> VerificationPolicy {
    VerificationPolicy {
        cert_identity: IdentityMatch::Exact(BEACON_SAN.to_string()),
        cert_issuer: GHA_ISSUER.to_string(),
    }
}

#[test]
fn real_bundle_wrong_identity_rejected() {
    let policy = VerificationPolicy {
        cert_identity: IdentityMatch::Exact("https://github.com/attacker/repo".into()),
        cert_issuer: GHA_ISSUER.to_string(),
    };
    assert_rejected_with(
        BUNDLE,
        ARTIFACT,
        &policy,
        |e| matches!(e, SigstoreVerificationError::PolicyViolation { .. }),
        "PolicyViolation for a mismatched identity",
    );
}

#[test]
fn real_bundle_tampered_artifact_rejected() {
    let mut tampered = ARTIFACT.to_vec();
    tampered.extend_from_slice(b"tamper");
    // The messageDigest no longer matches the (tampered) artifact.
    assert_rejected_with(
        BUNDLE,
        &tampered,
        &beacon_policy(),
        |e| matches!(e, SigstoreVerificationError::SignatureMismatch { .. }),
        "SignatureMismatch for a modified artifact",
    );
}

// Real sigstore-conformance negative fixtures — each corrupts one part of the
// transparency-log evidence; all must be rejected for the *right* reason.

#[test]
fn real_bundle_corrupted_inclusion_proof_rejected() {
    assert_rejected_with(
        include_bytes!("fixtures/inclusion-proof-corrupted-hash.sigstore.json"),
        ARTIFACT,
        &beacon_policy(),
        |e| matches!(e, SigstoreVerificationError::RekorInconsistency { .. }),
        "RekorInconsistency for a bit-flipped Merkle proof hash",
    );
}

#[test]
fn real_bundle_invalid_checkpoint_signature_rejected() {
    assert_rejected_with(
        include_bytes!("fixtures/invalid-checkpoint-signature.sigstore.json"),
        ARTIFACT,
        &beacon_policy(),
        |e| matches!(e, SigstoreVerificationError::RekorInconsistency { .. }),
        "RekorInconsistency for an invalid checkpoint signature",
    );
}

#[test]
fn real_bundle_checkpoint_wrong_roothash_rejected() {
    assert_rejected_with(
        include_bytes!("fixtures/checkpoint-wrong-roothash.sigstore.json"),
        ARTIFACT,
        &beacon_policy(),
        |e| matches!(e, SigstoreVerificationError::RekorInconsistency { .. }),
        "RekorInconsistency for a checkpoint root hash not matching the proof",
    );
}

#[test]
fn real_bundle_message_digest_mismatch_rejected() {
    assert_rejected_with(
        include_bytes!("fixtures/message-digest-mismatch.sigstore.json"),
        ARTIFACT,
        &beacon_policy(),
        |e| matches!(e, SigstoreVerificationError::SignatureMismatch { .. }),
        "SignatureMismatch for a messageDigest inconsistent with the artifact",
    );
}
