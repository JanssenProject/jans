// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore blob verifier — 10-step offline verification.
//!
//! Takes artifact bytes + Sigstore bundle JSON and produces a verified identity.
//! No network calls during `verify()`.

use sha2::{Digest, Sha256};

use crate::bundle::{BundleContent, ParsedBundle};
use crate::cert::Cert;
use crate::chain::validate_chain;
use crate::crypto::verify_ecdsa_p256_prehashed;
use crate::error::SigstoreVerificationError;
use crate::policy::VerificationPolicy;
use crate::sct::verify_sct;
use crate::tlog::{verify_body_consistency, verify_set_from_bundle};
use crate::trust_root::{SigstoreTrustRootRaw, TrustRoot};

/// Result of a successful verification.
#[derive(Debug, Clone)]
pub struct VerifiedSignature {
    /// The Subject Alternative Name from the signing certificate.
    pub subject_alternative_name: String,

    /// The OIDC issuer from the certificate's Fulcio extension.
    pub issuer: String,

    /// The Rekor `integratedTime` (UNIX epoch seconds) — verified via SET.
    pub verified_at: i64,
}

/// Offline Sigstore blob verifier.
///
/// No network calls during [`verify`](SigstoreBlobVerifier::verify).
/// All trust material is provided at construction time.
///
/// For swapping trust material without rebuilding, wrap in `Arc<ArcSwap<SigstoreBlobVerifier>>`
/// at the caller level.
pub struct SigstoreBlobVerifier {
    trust_root: TrustRoot,
}

impl SigstoreBlobVerifier {
    /// Construct a verifier from explicit trust root bytes.
    ///
    /// Returns an error if any PEM/DER data is malformed.
    pub fn new(trust_root_raw: SigstoreTrustRootRaw) -> Result<Self, SigstoreVerificationError> {
        let trust_root = trust_root_raw.parse()?;
        Ok(Self { trust_root })
    }

    /// Construct a verifier with public-good Sigstore keys embedded at compile time.
    ///
    /// Uses `include_bytes!` — zero network, zero filesystem at runtime.
    ///
    /// The embedded keys are validated at compile time by `build.rs`.
    /// This function cannot fail at runtime unless the compiled binary
    /// has been tampered with.
    ///
    /// # Panics
    ///
    /// Panics if the compiled binary has been tampered with and the embedded
    /// PEM keys no longer match what was validated at build time.
    #[must_use]
    pub fn with_static_trust_root() -> Self {
        let trust_root_raw = SigstoreTrustRootRaw::with_static_trust_root();
        // Safety: build.rs validates these PEM files at compile time.
        // A panic here indicates binary tampering, not a coding error.
        Self::new(trust_root_raw).expect("trust root keys validated at build time")
    }

    /// Verify that `artifact_bytes` was signed, producing `bundle_json`.
    ///
    /// This is the main entry point. It executes the 10-step verification algorithm
    /// as specified in the Sigstore client spec (§4 Verification).
    ///
    /// # Steps
    ///
    /// 1. Parse bundle JSON → extract mediaType, cert, signature, `tlog_entry`
    /// 2. Parse X.509 cert → pubkey, SAN, OIDC issuer, validity, SCT
    /// 3. SET verification → authenticate integratedTime via Rekor signature
    /// 4. Cert chain validation → Fulcio root (timestamp-anchored)
    /// 5. SCT verification → against CTFE public keys
    /// 6. Cert validity window → `not_before` ≤ integratedTime ≤ `not_after`
    /// 7. OIDC identity check → SAN + issuer match policy
    /// 8. Signature verification → SHA-256(artifact) verified against cert pubkey
    /// 9. Rekor entry consistency → body matches cert/sig/hash (CVE-2022-36056)
    /// 10. Offline inclusion proof → signed checkpoint authenticates the log root,
    ///     Merkle proof ties the entry to it (when the bundle carries a proof)
    pub fn verify(
        &self,
        artifact_bytes: &[u8],
        bundle_json: &[u8],
        policy: &VerificationPolicy,
    ) -> Result<VerifiedSignature, SigstoreVerificationError> {
        // Step 1: Parse bundle JSON
        let parsed = ParsedBundle::from_json(bundle_json)?;

        // Extract certificate (from bundle or caller-provided for legacy)
        let cert_b64 = parsed.certificate_base64().ok_or_else(|| {
            SigstoreVerificationError::InvalidBundleFormat {
                reason: "bundle does not contain a certificate".into(),
            }
        })?;
        let cert_der = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, cert_b64)
            .map_err(|e| SigstoreVerificationError::InvalidBundleFormat {
                reason: format!("failed to decode certificate: {e}"),
            })?;
        let cert = Cert::from_der(&cert_der)?;

        // Extract signature
        let sig_b64 = parsed.signature_base64().ok_or_else(|| {
            SigstoreVerificationError::InvalidBundleFormat {
                reason: "bundle does not contain a signature".into(),
            }
        })?;
        let signature = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, sig_b64)
            .map_err(|e| SigstoreVerificationError::InvalidBundleFormat {
                reason: format!("failed to decode signature: {e}"),
            })?;

        // Step 2: Extract cert fields (done during Cert::from_der)

        // Step 3: SET verification — authenticate integratedTime
        let tlog_entry = parsed.tlog_entry().ok_or_else(|| {
            SigstoreVerificationError::InvalidBundleFormat {
                reason: "bundle has no tlog entries".into(),
            }
        })?;
        // Bundle spec: media type v0.2+ requires an inclusion proof (with
        // checkpoint). v0.1 predates that and may be SET-only.
        if parsed.version() >= crate::bundle::BundleVersion::Bundle0_2
            && tlog_entry.inclusion_proof.is_none()
        {
            return Err(SigstoreVerificationError::InvalidBundleFormat {
                reason: "bundle v0.2+ requires a tlog inclusion proof".into(),
            });
        }
        // Select the trusted Rekor key(s) whose key ID (SHA-256 of SPKI DER)
        // matches the entry's logId, then verify the SET with those.
        let claimed_log_id = base64::Engine::decode(
            &base64::engine::general_purpose::STANDARD,
            &tlog_entry.log_id.key_id,
        )
        .map_err(|e| SigstoreVerificationError::SetVerification {
            reason: format!("failed to decode tlog logId: {e}"),
        })?;
        let integrated_time = {
            let mut integrated_time = None;
            let mut last_err = None;
            for rekor_key in self
                .trust_root
                .rekor_keys
                .iter()
                .filter(|k| crate::crypto::p256_key_id(k)[..] == claimed_log_id[..])
            {
                match verify_set_from_bundle(tlog_entry, rekor_key) {
                    Ok(time) => {
                        integrated_time = Some(time);
                        break;
                    },
                    Err(e) => last_err = Some(e),
                }
            }
            integrated_time.ok_or_else(|| {
                last_err.unwrap_or_else(|| SigstoreVerificationError::SetVerification {
                    reason: "no trusted Rekor key matches the tlog entry logId".into(),
                })
            })?
        };

        // After step 3, integratedTime is TRUSTED

        // Candidate intermediate pool = bundle-provided (x509CertificateChain,
        // v0.1/v0.2) + trust-root intermediates. Path building still anchors at a
        // trusted root, so accepting bundle intermediates does not weaken trust.
        let mut intermediates = self.trust_root.fulcio_intermediates.clone();
        for b64 in parsed.intermediate_certificates_base64() {
            let der = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, b64)
                .map_err(|e| SigstoreVerificationError::CertificateParsing {
                    reason: format!("failed to decode bundle intermediate: {e}"),
                })?;
            intermediates.push(Cert::from_der(&der)?);
        }

        // Step 4: Cert chain validation (timestamp-anchored on integratedTime)
        validate_chain(
            &cert,
            &intermediates,
            &self.trust_root.fulcio_roots,
            integrated_time,
        )?;

        // Step 5: SCT verification. The precert `issuer_key_hash` is computed
        // over the issuing CA's SPKI, so locate the cert that issued the leaf
        // (matched by DN — its signature was already checked in step 4).
        let issuer_cert = intermediates
            .iter()
            .chain(self.trust_root.fulcio_roots.iter())
            .find(|c| c.subject_dn == cert.issuer_dn)
            .ok_or_else(|| SigstoreVerificationError::SctVerification {
                reason: "issuer certificate for the leaf not found in trust root".into(),
            })?;
        verify_sct(&cert, issuer_cert, &self.trust_root.ctfe_keys)?;

        // Step 6: Cert validity window
        cert.check_validity(integrated_time)?;

        // Step 7: OIDC identity check
        let issuer = cert.issuer.clone().ok_or_else(|| {
            SigstoreVerificationError::PolicyViolation {
                reason:
                    "certificate does not contain OIDC issuer extension (OID 1.3.6.1.4.1.57264.1.8)"
                        .into(),
            }
        })?;
        let subject_alternative_name = policy.verify(&cert.sans, Some(&issuer))?;

        // Step 8: Signature verification
        let artifact_digest: [u8; 32] = Sha256::digest(artifact_bytes).into();
        let artifact_digest_hex = artifact_digest
            .iter()
            .map(|b| format!("{b:02x}"))
            .collect::<String>();

        // Determine what to verify against based on content type.
        // Also capture DSSE envelope data for tlog body consistency check.
        let mut dsse_data: Option<(Vec<u8>, Vec<u8>)> = None;

        let bundle = parsed.bundle();
        match &bundle.content {
            BundleContent::MessageSignature { message_digest, .. } => {
                // The `messageDigest` is an unauthenticated hint, but it must
                // be consistent with the artifact — reject a bundle claiming
                // a different digest than the one we compute and verify.
                if let Some(md) = message_digest {
                    let stated = base64::Engine::decode(
                        &base64::engine::general_purpose::STANDARD,
                        &md.digest,
                    )
                    .map_err(|e| SigstoreVerificationError::InvalidBundleFormat {
                        reason: format!("failed to decode messageDigest: {e}"),
                    })?;
                    if stated != artifact_digest {
                        return Err(SigstoreVerificationError::SignatureMismatch {
                            reason: "messageDigest does not match the artifact hash".into(),
                        });
                    }
                }
                // Signature over SHA-256(artifact)
                verify_ecdsa_p256_prehashed(&cert.pubkey_bytes, &artifact_digest, &signature)?;
            },
            BundleContent::DsseEnvelope {
                payload,
                payload_type,
                ..
            } => {
                // DSSE: verify signature over PAE(payloadType, payload)
                let payload_bytes =
                    base64::Engine::decode(&base64::engine::general_purpose::STANDARD, payload)
                        .map_err(|e| SigstoreVerificationError::InvalidBundleFormat {
                            reason: format!("failed to decode DSSE payload: {e}"),
                        })?;
                let pae = compute_pae(payload_type, &payload_bytes);
                verify_ecdsa_p256_prehashed(&cert.pubkey_bytes, &Sha256::digest(&pae), &signature)?;

                // Bind the envelope to THIS artifact: the statement's subject
                // digest must match, or verification is vacuous.
                verify_dsse_artifact_binding(payload_type, &payload_bytes, &artifact_digest_hex)?;

                // Compute canonical JSON of the DSSE envelope for tlog body check.
                // The Rekor `dsse` entry type stores envelopeHash = SHA-256 of this.
                // Format matches the sigstore protobuf DsseEnvelope canonical JSON.
                let envelope_value = serde_json::json!({
                    "payload": payload,
                    "payloadType": payload_type,
                    "signatures": bundle_content_signatures(bundle),
                });
                let envelope_json = serde_json::to_vec(&envelope_value).map_err(|e| {
                    SigstoreVerificationError::InvalidBundleFormat {
                        reason: format!("failed to serialize DSSE envelope: {e}"),
                    }
                })?;
                dsse_data = Some((envelope_json, payload_bytes));
            },
        }

        // Step 9: Rekor entry consistency (CVE-2022-36056)
        verify_body_consistency(
            tlog_entry,
            &cert,
            sig_b64,
            &artifact_digest_hex,
            dsse_data
                .as_ref()
                .map(|(env, pay)| (env.as_slice(), pay.as_slice())),
        )?;

        // Step 10: Offline Merkle inclusion proof + signed checkpoint.
        // When the bundle carries an inclusion proof, verify it: the signed
        // checkpoint authenticates the log's root hash, and the Merkle proof
        // ties this entry to that root. No network — the proof is embedded.
        if let Some(proof) = &tlog_entry.inclusion_proof {
            self.verify_inclusion_proof(tlog_entry, proof)?;
        }

        // Success
        Ok(VerifiedSignature {
            subject_alternative_name,
            issuer,
            verified_at: integrated_time,
        })
    }

    /// Verify a bundle's embedded Merkle inclusion proof and signed checkpoint.
    ///
    /// The checkpoint (signed by a trusted Rekor key) authenticates the log root
    /// hash and tree size; the Merkle proof ties the entry's canonicalized body
    /// to that root. Offline — the proof is carried in the bundle.
    fn verify_inclusion_proof(
        &self,
        tlog_entry: &crate::bundle::TlogEntry,
        proof: &crate::bundle::InclusionProof,
    ) -> Result<(), SigstoreVerificationError> {
        let b64 = base64::engine::general_purpose::STANDARD;

        let body_b64 = tlog_entry.canonicalized_body.as_ref().ok_or_else(|| {
            SigstoreVerificationError::RekorInconsistency {
                reason: "inclusion proof requires canonicalizedBody".into(),
            }
        })?;
        let entry_bytes = base64::Engine::decode(&b64, body_b64).map_err(|e| {
            SigstoreVerificationError::RekorInconsistency {
                reason: format!("failed to decode canonicalizedBody for inclusion proof: {e}"),
            }
        })?;

        let index: u64 = proof.log_index.parse().map_err(|_| {
            SigstoreVerificationError::RekorInconsistency {
                reason: "inclusion proof logIndex is not a number".into(),
            }
        })?;
        let tree_size: u64 = proof.tree_size.parse().map_err(|_| {
            SigstoreVerificationError::RekorInconsistency {
                reason: "inclusion proof treeSize is not a number".into(),
            }
        })?;
        let root = base64::Engine::decode(&b64, &proof.root_hash).map_err(|e| {
            SigstoreVerificationError::RekorInconsistency {
                reason: format!("inclusion proof rootHash is not valid base64: {e}"),
            }
        })?;
        let hashes: Vec<Vec<u8>> = proof
            .hashes
            .iter()
            .map(|h| base64::Engine::decode(&b64, h))
            .collect::<Result<_, _>>()
            .map_err(|e| SigstoreVerificationError::RekorInconsistency {
                reason: format!("inclusion proof hash is not valid base64: {e}"),
            })?;

        // The signed checkpoint must authenticate the root hash we prove against.
        let envelope = proof
            .checkpoint
            .as_ref()
            .map(|c| c.envelope.as_str())
            .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
                reason: "inclusion proof has no signed checkpoint".into(),
            })?;
        crate::tlog::verify_checkpoint(envelope, &self.trust_root.rekor_keys, &root, tree_size)?;

        crate::merkle::verify_inclusion(index, tree_size, &entry_bytes, &hashes, &root)
    }
}

/// Compute the DSSE Pre-Authentication Encoding (PAE).
///
/// PAE = "`DSSEv1` <len(type)> <type> <len(payload)> <payload>"
/// See <https://github.com/secure-systems-lab/dsse/blob/master/protocol.md>
fn compute_pae(payload_type: &str, payload: &[u8]) -> Vec<u8> {
    let header = format!(
        "DSSEv1 {} {} {} ",
        payload_type.len(),
        payload_type,
        payload.len()
    );
    let mut result = header.into_bytes();
    result.extend_from_slice(payload);
    result
}

/// Bind a DSSE envelope to the artifact being verified.
///
/// The DSSE signature covers the payload, not the artifact — without this
/// check, `verify()` on a DSSE bundle would succeed for *any* artifact bytes.
/// Per the Sigstore client spec the payload must be an in-toto Statement and
/// one of its `subject[].digest.sha256` values must equal the artifact digest.
fn verify_dsse_artifact_binding(
    payload_type: &str,
    payload_bytes: &[u8],
    artifact_digest_hex: &str,
) -> Result<(), SigstoreVerificationError> {
    if payload_type != "application/vnd.in-toto+json" {
        return Err(SigstoreVerificationError::InvalidBundleFormat {
            reason: format!(
                "cannot bind DSSE payload of type '{payload_type}' to an artifact \
                 (only application/vnd.in-toto+json is supported)"
            ),
        });
    }

    let statement: serde_json::Value = serde_json::from_slice(payload_bytes).map_err(|e| {
        SigstoreVerificationError::InvalidBundleFormat {
            reason: format!("DSSE payload is not valid JSON: {e}"),
        }
    })?;

    let stmt_type = statement.get("_type").and_then(|v| v.as_str()).unwrap_or("");
    if stmt_type != "https://in-toto.io/Statement/v1"
        && stmt_type != "https://in-toto.io/Statement/v0.1"
    {
        return Err(SigstoreVerificationError::InvalidBundleFormat {
            reason: format!("DSSE payload is not an in-toto Statement (_type: '{stmt_type}')"),
        });
    }

    let subjects = statement
        .get("subject")
        .and_then(|s| s.as_array())
        .ok_or_else(|| SigstoreVerificationError::InvalidBundleFormat {
            reason: "in-toto Statement has no 'subject' array".into(),
        })?;

    let matched = subjects.iter().any(|s| {
        s.get("digest")
            .and_then(|d| d.get("sha256"))
            .and_then(|v| v.as_str())
            .is_some_and(|h| h.eq_ignore_ascii_case(artifact_digest_hex))
    });
    if !matched {
        return Err(SigstoreVerificationError::SignatureMismatch {
            reason: "no in-toto subject sha256 digest matches the artifact".into(),
        });
    }
    Ok(())
}

/// Extract signature objects from a DSSE bundle for envelope JSON serialization.
fn bundle_content_signatures(bundle: &crate::bundle::Bundle) -> Vec<serde_json::Value> {
    match &bundle.content {
        crate::bundle::BundleContent::DsseEnvelope { signatures, .. } => signatures
            .iter()
            .map(|s| {
                serde_json::json!({
                    "sig": s.sig,
                    "keyid": ""
                })
            })
            .collect(),
        crate::bundle::BundleContent::MessageSignature { .. } => vec![],
    }
}

#[cfg(test)]
mod dsse_binding_tests {
    use super::verify_dsse_artifact_binding;
    use crate::error::SigstoreVerificationError;

    const DIGEST: &str = "aabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccdd";
    const INTOTO: &str = "application/vnd.in-toto+json";

    fn statement(digest: &str) -> Vec<u8> {
        serde_json::to_vec(&serde_json::json!({
            "_type": "https://in-toto.io/Statement/v1",
            "subject": [
                { "name": "other", "digest": { "sha256": "00".repeat(32) } },
                { "name": "artifact", "digest": { "sha256": digest } }
            ],
            "predicateType": "https://slsa.dev/provenance/v1",
            "predicate": {}
        }))
        .unwrap()
    }

    #[test]
    fn matching_subject_digest_accepted() {
        verify_dsse_artifact_binding(INTOTO, &statement(DIGEST), DIGEST)
            .expect("statement whose subject digest matches the artifact must pass");
    }

    #[test]
    fn matching_subject_digest_case_insensitive() {
        verify_dsse_artifact_binding(INTOTO, &statement(&DIGEST.to_uppercase()), DIGEST)
            .expect("hex digest comparison must be case-insensitive");
    }

    #[test]
    fn mismatched_subject_digest_rejected() {
        let err = verify_dsse_artifact_binding(INTOTO, &statement(&"11".repeat(32)), DIGEST)
            .expect_err("statement not covering the artifact must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::SignatureMismatch { .. }),
            "expected SignatureMismatch, got {err:?}"
        );
    }

    #[test]
    fn non_intoto_payload_type_rejected() {
        let err = verify_dsse_artifact_binding("application/json", &statement(DIGEST), DIGEST)
            .expect_err("non-in-toto payloadType cannot be bound to an artifact");
        assert!(
            matches!(err, SigstoreVerificationError::InvalidBundleFormat { .. }),
            "expected InvalidBundleFormat, got {err:?}"
        );
    }

    #[test]
    fn non_statement_payload_rejected() {
        let err = verify_dsse_artifact_binding(INTOTO, b"{\"_type\":\"something-else\"}", DIGEST)
            .expect_err("payload that is not an in-toto Statement must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::InvalidBundleFormat { .. }),
            "expected InvalidBundleFormat, got {err:?}"
        );
    }
}

#[cfg(test)]
mod e2e_tests {
    //! End-to-end tests driving the public `verify()` over a fully-assembled
    //! v0.3 bundle: real cert chain + embedded SCT + Rekor SET + hashedrekord
    //! tlog body + `MessageSignature`. Offline, deterministic, WASM-safe.

    use std::collections::BTreeMap;

    use p256::ecdsa::{
        Signature, SigningKey,
        signature::Signer,
        signature::hazmat::PrehashSigner,
    };
    use serde_json::json;
    use sha2::{Digest, Sha256};

    use super::*;
    use crate::cert::Cert;
    use crate::policy::IdentityMatch;
    use crate::test_support::{
        Ca, LeafOpts, der_to_pem, ec_pub_pem, make_leaf_with_real_sct, make_root,
    };

    fn b64(bytes: &[u8]) -> String {
        base64::Engine::encode(&base64::engine::general_purpose::STANDARD, bytes)
    }

    const ARTIFACT: &[u8] = b"hello sigstore end-to-end";
    const INTEGRATED_TIME: i64 = 1_700_000_000; // 2023-11-14, inside leaf validity

    /// The material an assembled bundle is built from — tweak fields for
    /// negative cases, then call [`Fixture::bundle_json`].
    struct Fixture {
        root: Ca,
        rekor_sk: SigningKey,
        ctfe_sk: SigningKey,
        leaf_cert: Cert,
        leaf_sk: SigningKey,
    }

    impl Fixture {
        fn new() -> Self {
            let root = make_root("fulcio-root");
            let ctfe_sk = SigningKey::from_slice(&[5u8; 32]).unwrap();
            let rekor_sk = SigningKey::from_slice(&[3u8; 32]).unwrap();
            let ctfe_log_id =
                crate::crypto::p256_key_id(ctfe_sk.verifying_key().to_encoded_point(false).as_bytes());
            let (leaf, leaf_sk) = make_leaf_with_real_sct(
                &root,
                &LeafOpts::default(),
                &ctfe_sk,
                &ctfe_log_id,
                INTEGRATED_TIME as u64,
            );
            let leaf_cert = Cert::from_der(&leaf.der).unwrap();
            Self { root, rekor_sk, ctfe_sk, leaf_cert, leaf_sk }
        }

        fn trust_root(&self) -> SigstoreTrustRootRaw {
            SigstoreTrustRootRaw {
                fulcio_root_certs: vec![der_to_pem(&self.root.der).into_bytes()],
                fulcio_intermediate_certs: vec![],
                rekor_keys: vec![ec_pub_pem(self.rekor_sk.verifying_key()).into_bytes()],
                ctfe_keys: vec![ec_pub_pem(self.ctfe_sk.verifying_key()).into_bytes()],
            }
        }

        fn policy() -> VerificationPolicy {
            VerificationPolicy {
                cert_identity: IdentityMatch::Exact(
                    LeafOpts::default().san_uri.unwrap().to_string(),
                ),
                cert_issuer: LeafOpts::default().oidc_issuer.unwrap().to_string(),
            }
        }

        /// Inclusion proof for a single-entry log containing `body_b64`'s bytes:
        /// root = RFC 6962 leaf hash, empty audit path, checkpoint signed by `rekor_sk`.
        fn inclusion_proof_value(&self, body_b64: &str) -> serde_json::Value {
            let body_bytes = base64::Engine::decode(
                &base64::engine::general_purpose::STANDARD,
                body_b64,
            )
            .unwrap();
            // RFC 6962 leaf hash: SHA-256(0x00 || entry).
            let mut h = Sha256::new();
            h.update([0x00]);
            h.update(&body_bytes);
            let root: [u8; 32] = h.finalize().into();

            // Signed note: body lines (origin, size, root) then "— <name> <b64>".
            let signed_text = format!("rekor.test \u{2014} log\n1\n{}\n", b64(&root));
            let note_hash: [u8; 32] = Sha256::digest(signed_text.as_bytes()).into();
            let note_sig: Signature =
                PrehashSigner::sign_prehash(&self.rekor_sk, &note_hash).unwrap();
            let key_id = crate::crypto::p256_key_id(
                self.rekor_sk.verifying_key().to_encoded_point(false).as_bytes(),
            );
            let mut sig_blob = key_id[..4].to_vec();
            sig_blob.extend_from_slice(note_sig.to_der().as_bytes());
            let envelope = format!("{signed_text}\n\u{2014} rekor.test {}\n", b64(&sig_blob));

            json!({
                "logIndex": "0",
                "rootHash": b64(&root),
                "treeSize": "1",
                "hashes": [],
                "checkpoint": { "envelope": envelope }
            })
        }

        /// Assemble the v0.3 bundle JSON over `artifact`, signing SET with
        /// `rekor_sk` (override to forge a bad SET), and stamping the tlog
        /// entry's logId/logID fields with `rekor_log_id`.
        fn bundle_json_with_log_id(
            &self,
            artifact: &[u8],
            rekor_sk: &SigningKey,
            rekor_log_id: &[u8; 32],
        ) -> Vec<u8> {
            let digest: [u8; 32] = Sha256::digest(artifact).into();
            let digest_hex: String = digest.iter().map(|b| format!("{b:02x}")).collect();

            let sig: Signature = self.leaf_sk.sign(artifact);
            let sig_b64 = b64(sig.to_der().as_bytes());

            // hashedrekord tlog body.
            let body = json!({
                "apiVersion": "0.0.1",
                "kind": "hashedrekord",
                "spec": {
                    "data": { "hash": { "algorithm": "sha256", "value": digest_hex } },
                    "signature": {
                        "content": sig_b64,
                        "publicKey": { "content": b64(der_to_pem(&self.leaf_cert.der).as_bytes()) }
                    }
                }
            });
            let body_b64 = b64(&serde_json::to_vec(&body).unwrap());

            // Rekor SET over the canonical payload (body as base64 STRING).
            let log_id_hex: String = rekor_log_id.iter().map(|b| format!("{b:02x}")).collect();
            let mut payload = BTreeMap::new();
            payload.insert("body".to_string(), json!(body_b64.clone()));
            payload.insert("integratedTime".to_string(), json!(INTEGRATED_TIME));
            payload.insert("logIndex".to_string(), json!(42));
            payload.insert("logID".to_string(), json!(log_id_hex));
            let canonical = serde_json_canonicalizer::to_vec(&payload).unwrap();
            let set_sig: Signature = rekor_sk.sign(&canonical);

            let bundle = json!({
                "mediaType": "application/vnd.dev.sigstore.bundle.v0.3+json",
                "verificationMaterial": {
                    "certificate": { "rawBytes": b64(&self.leaf_cert.der) },
                    "tlogEntries": [{
                        "logIndex": "42",
                        "logId": { "keyId": b64(rekor_log_id) },
                        "kindVersion": { "kind": "hashedrekord", "version": "0.0.1" },
                        "integratedTime": INTEGRATED_TIME.to_string(),
                        "inclusionPromise": {
                            "signedEntryTimestamp": b64(set_sig.to_der().as_bytes())
                        },
                        "inclusionProof": self.inclusion_proof_value(&body_b64),
                        "canonicalizedBody": body_b64
                    }]
                },
                "messageSignature": {
                    "messageDigest": { "algorithm": "SHA2_256", "digest": b64(&digest) },
                    "signature": sig_b64
                }
            });
            serde_json::to_vec(&bundle).unwrap()
        }

        /// `bundle_json_with_log_id` using the trust-root key's own logId — the
        /// common case for tests that don't care about key-id selection.
        fn bundle_json(&self, artifact: &[u8], rekor_sk: &SigningKey) -> Vec<u8> {
            let id = crate::crypto::p256_key_id(
                self.rekor_sk.verifying_key().to_encoded_point(false).as_bytes(),
            );
            self.bundle_json_with_log_id(artifact, rekor_sk, &id)
        }

        /// Assemble a v0.3 DSSE bundle over an in-toto statement covering
        /// `artifact`. Mirrors the envelope JSON the verifier reconstructs
        /// (alphabetical keys, keyid "").
        fn dsse_bundle_json(&self, artifact: &[u8]) -> Vec<u8> {
            let digest: [u8; 32] = Sha256::digest(artifact).into();
            let digest_hex: String = digest.iter().map(|b| format!("{b:02x}")).collect();
            let payload_type = "application/vnd.in-toto+json";

            let payload = serde_json::to_vec(&json!({
                "_type": "https://in-toto.io/Statement/v1",
                "subject": [{ "name": "artifact", "digest": { "sha256": digest_hex } }],
                "predicateType": "https://slsa.dev/provenance/v1",
                "predicate": {}
            }))
            .unwrap();
            let payload_b64 = b64(&payload);

            // DSSE PAE signature by the leaf key.
            let pae = super::compute_pae(payload_type, &payload);
            let sig: Signature = self.leaf_sk.sign(&pae);
            let sig_b64 = b64(sig.to_der().as_bytes());

            // Envelope canonical JSON exactly as the verifier rebuilds it.
            let envelope_json = serde_json::to_vec(&json!({
                "payload": payload_b64,
                "payloadType": payload_type,
                "signatures": [{ "sig": sig_b64, "keyid": "" }],
            }))
            .unwrap();
            let env_hash_hex: String =
                Sha256::digest(&envelope_json).iter().map(|b| format!("{b:02x}")).collect();
            let payload_hash_hex: String =
                Sha256::digest(&payload).iter().map(|b| format!("{b:02x}")).collect();

            // Rekor dsse v0.0.1 body.
            let body = json!({
                "apiVersion": "0.0.1",
                "kind": "dsse",
                "spec": {
                    "envelopeHash": { "algorithm": "sha256", "value": env_hash_hex },
                    "payloadHash": { "algorithm": "sha256", "value": payload_hash_hex },
                    "signatures": [{
                        "signature": sig_b64,
                        "verifier": b64(&self.leaf_cert.der)
                    }]
                }
            });
            let body_b64 = b64(&serde_json::to_vec(&body).unwrap());

            // Rekor SET over the canonical payload.
            let rekor_log_id = crate::crypto::p256_key_id(
                self.rekor_sk.verifying_key().to_encoded_point(false).as_bytes(),
            );
            let log_id_hex: String = rekor_log_id.iter().map(|b| format!("{b:02x}")).collect();
            let mut set_payload = BTreeMap::new();
            set_payload.insert("body".to_string(), json!(body_b64.clone()));
            set_payload.insert("integratedTime".to_string(), json!(INTEGRATED_TIME));
            set_payload.insert("logIndex".to_string(), json!(42));
            set_payload.insert("logID".to_string(), json!(log_id_hex));
            let canonical = serde_json_canonicalizer::to_vec(&set_payload).unwrap();
            let set_sig: Signature = self.rekor_sk.sign(&canonical);

            let bundle = json!({
                "mediaType": "application/vnd.dev.sigstore.bundle.v0.3+json",
                "verificationMaterial": {
                    "certificate": { "rawBytes": b64(&self.leaf_cert.der) },
                    "tlogEntries": [{
                        "logIndex": "42",
                        "logId": { "keyId": b64(&rekor_log_id) },
                        "kindVersion": { "kind": "dsse", "version": "0.0.1" },
                        "integratedTime": INTEGRATED_TIME.to_string(),
                        "inclusionPromise": {
                            "signedEntryTimestamp": b64(set_sig.to_der().as_bytes())
                        },
                        "inclusionProof": self.inclusion_proof_value(&body_b64),
                        "canonicalizedBody": body_b64
                    }]
                },
                "dsseEnvelope": {
                    "payload": payload_b64,
                    "payloadType": payload_type,
                    "signatures": [{ "sig": sig_b64 }]
                }
            });
            serde_json::to_vec(&bundle).unwrap()
        }
    }

    #[test]
    fn full_flow_valid_bundle_verifies() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).expect("trust root");
        let bundle = fx.bundle_json(ARTIFACT, &fx.rekor_sk);

        let result = verifier
            .verify(ARTIFACT, &bundle, &Fixture::policy())
            .expect("a fully valid bundle must pass all 10 steps");

        assert_eq!(result.issuer, LeafOpts::default().oidc_issuer.unwrap());
        assert_eq!(result.subject_alternative_name, LeafOpts::default().san_uri.unwrap());
        assert_eq!(result.verified_at, INTEGRATED_TIME);
    }

    #[test]
    fn v01_bundle_without_inclusion_proof_still_verifies() {
        // v0.1 predates the mandatory-proof rule: SET-only must stay accepted.
        // Locks the `>= Bundle0_2` boundary of the inclusion-proof gate.
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        let mut bundle: serde_json::Value =
            serde_json::from_slice(&fx.bundle_json(ARTIFACT, &fx.rekor_sk)).unwrap();
        bundle["mediaType"] = "application/vnd.dev.sigstore.bundle+json;version=0.1".into();
        bundle["verificationMaterial"]["tlogEntries"][0]
            .as_object_mut()
            .unwrap()
            .remove("inclusionProof");

        verifier
            .verify(ARTIFACT, &serde_json::to_vec(&bundle).unwrap(), &Fixture::policy())
            .expect("a SET-only v0.1 bundle must verify without an inclusion proof");
    }

    #[test]
    fn wrong_identity_policy_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        let bundle = fx.bundle_json(ARTIFACT, &fx.rekor_sk);

        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/attacker/evil".into()),
            cert_issuer: LeafOpts::default().oidc_issuer.unwrap().to_string(),
        };
        let err = verifier
            .verify(ARTIFACT, &bundle, &policy)
            .expect_err("a bundle signed by a different identity must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::PolicyViolation { .. }),
            "expected PolicyViolation, got {err:?}"
        );
    }

    #[test]
    fn tampered_artifact_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        let bundle = fx.bundle_json(ARTIFACT, &fx.rekor_sk);

        let err = verifier
            .verify(b"a different artifact", &bundle, &Fixture::policy())
            .expect_err("verifying a different artifact against the bundle must fail");
        assert!(
            matches!(
                err,
                SigstoreVerificationError::SignatureMismatch { .. }
                    | SigstoreVerificationError::RekorInconsistency { .. }
            ),
            "expected signature/rekor failure, got {err:?}"
        );
    }

    #[test]
    fn set_forged_with_wrong_rekor_key_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        // Sign the SET with a key the trust root does not know.
        let forged = SigningKey::from_slice(&[9u8; 32]).unwrap();
        let bundle = fx.bundle_json(ARTIFACT, &forged);

        let err = verifier
            .verify(ARTIFACT, &bundle, &Fixture::policy())
            .expect_err("a SET signed by an untrusted Rekor key must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::SetVerification { .. }),
            "expected SetVerification, got {err:?}"
        );
    }

    #[test]
    fn unknown_rekor_log_id_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        // Valid SET signature, but the bundle claims a logId no trusted key has.
        let bundle = fx.bundle_json_with_log_id(ARTIFACT, &fx.rekor_sk, &[0xEE; 32]);
        let err = verifier
            .verify(ARTIFACT, &bundle, &Fixture::policy())
            .expect_err("a logId matching no trusted Rekor key must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::SetVerification { .. }),
            "expected SetVerification, got {err:?}"
        );
    }

    #[test]
    fn v03_bundle_without_inclusion_proof_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        let mut bundle: serde_json::Value =
            serde_json::from_slice(&fx.bundle_json(ARTIFACT, &fx.rekor_sk)).unwrap();
        bundle["verificationMaterial"]["tlogEntries"][0]
            .as_object_mut()
            .unwrap()
            .remove("inclusionProof");
        let err = verifier
            .verify(ARTIFACT, &serde_json::to_vec(&bundle).unwrap(), &Fixture::policy())
            .expect_err("a v0.3 bundle without an inclusion proof must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::InvalidBundleFormat { .. }),
            "expected InvalidBundleFormat, got {err:?}"
        );
    }

    #[test]
    fn dsse_bundle_over_matching_artifact_verifies() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        let bundle = fx.dsse_bundle_json(ARTIFACT);
        let result = verifier
            .verify(ARTIFACT, &bundle, &Fixture::policy())
            .expect("a valid DSSE bundle whose statement covers the artifact must verify");
        assert_eq!(result.verified_at, INTEGRATED_TIME);
    }

    #[test]
    fn dsse_bundle_over_different_artifact_rejected() {
        let fx = Fixture::new();
        let verifier = SigstoreBlobVerifier::new(fx.trust_root()).unwrap();
        // Bundle attests ARTIFACT; verify a different blob against it.
        let bundle = fx.dsse_bundle_json(ARTIFACT);
        let err = verifier
            .verify(b"totally different artifact", &bundle, &Fixture::policy())
            .expect_err("a DSSE bundle must not verify an artifact its statement does not cover");
        assert!(
            matches!(err, SigstoreVerificationError::SignatureMismatch { .. }),
            "expected SignatureMismatch from artifact binding, got {err:?}"
        );
    }
}
