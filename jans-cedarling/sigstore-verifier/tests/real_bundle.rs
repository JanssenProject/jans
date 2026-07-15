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

use sigstore_verifier::{IdentityMatch, SigstoreBlobVerifier, VerificationPolicy};

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

#[test]
fn real_bundle_wrong_identity_rejected() {
    let verifier = SigstoreBlobVerifier::with_static_trust_root();
    let policy = VerificationPolicy {
        cert_identity: IdentityMatch::Exact("https://github.com/attacker/repo".into()),
        cert_issuer: GHA_ISSUER.to_string(),
    };
    verifier
        .verify(ARTIFACT, BUNDLE, &policy)
        .expect_err("a mismatched identity must be rejected on the real bundle");
}

#[test]
fn real_bundle_tampered_artifact_rejected() {
    let verifier = SigstoreBlobVerifier::with_static_trust_root();
    let policy = VerificationPolicy {
        cert_identity: IdentityMatch::Exact(BEACON_SAN.to_string()),
        cert_issuer: GHA_ISSUER.to_string(),
    };
    let mut tampered = ARTIFACT.to_vec();
    tampered.extend_from_slice(b"tamper");
    verifier
        .verify(&tampered, BUNDLE, &policy)
        .expect_err("a modified artifact must not verify against the real bundle");
}
