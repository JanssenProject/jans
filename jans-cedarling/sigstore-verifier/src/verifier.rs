// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore blob verifier — 9-step offline verification.
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
use crate::tlog::{verify_body_consistency, verify_set_from_bundle, verify_set_legacy};
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
    /// This is the main entry point. It executes the 9-step verification algorithm
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
        let integrated_time = match &parsed {
            ParsedBundle::Sigstore(_bundle) => {
                let tlog_entry = parsed.tlog_entry().ok_or_else(|| {
                    SigstoreVerificationError::InvalidBundleFormat {
                        reason: "bundle has no tlog entries".into(),
                    }
                })?;
                // Try each Rekor key until one works
                let mut integrated_time = None;
                let mut last_err = None;
                for rekor_key in &self.trust_root.rekor_keys {
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
                        reason: "no Rekor keys provided".into(),
                    })
                })?
            },
            ParsedBundle::Legacy(legacy) => {
                let mut integrated_time = None;
                let mut last_err = None;
                for rekor_key in &self.trust_root.rekor_keys {
                    match verify_set_legacy(legacy, rekor_key) {
                        Ok(time) => {
                            integrated_time = Some(time);
                            break;
                        },
                        Err(e) => last_err = Some(e),
                    }
                }
                integrated_time.ok_or_else(|| {
                    last_err.unwrap_or_else(|| SigstoreVerificationError::SetVerification {
                        reason: "no Rekor keys provided".into(),
                    })
                })?
            },
        };

        // After step 3, integratedTime is TRUSTED

        // Step 4: Cert chain validation (timestamp-anchored on integratedTime)
        validate_chain(
            &cert,
            &self.trust_root.fulcio_intermediates,
            &self.trust_root.fulcio_roots,
            integrated_time,
        )?;

        // Step 5: SCT verification
        verify_sct(&cert, &self.trust_root.ctfe_keys)?;

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
        policy.verify(&cert.sans, Some(&issuer))?;

        // Step 8: Signature verification
        let artifact_digest: [u8; 32] = Sha256::digest(artifact_bytes).into();
        let artifact_digest_hex = artifact_digest
            .iter()
            .map(|b| format!("{b:02x}"))
            .collect::<String>();

        // Determine what to verify against based on content type.
        // Also capture DSSE envelope data for tlog body consistency check.
        let mut dsse_data: Option<(Vec<u8>, Vec<u8>)> = None;

        match &parsed {
            ParsedBundle::Sigstore(bundle) => match &bundle.content {
                BundleContent::MessageSignature { .. } => {
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
                    verify_ecdsa_p256_prehashed(
                        &cert.pubkey_bytes,
                        &Sha256::digest(&pae),
                        &signature,
                    )?;

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
            },
            ParsedBundle::Legacy(_) => {
                // Legacy: signature over SHA-256(artifact)
                verify_ecdsa_p256_prehashed(&cert.pubkey_bytes, &artifact_digest, &signature)?;
            },
        }

        // Step 9: Rekor entry consistency (CVE-2022-36056)
        if let Some(tlog_entry) = parsed.tlog_entry() {
            verify_body_consistency(
                tlog_entry,
                &cert,
                sig_b64,
                &artifact_digest_hex,
                dsse_data
                    .as_ref()
                    .map(|(env, pay)| (env.as_slice(), pay.as_slice())),
            )?;
        }

        // Success
        Ok(VerifiedSignature {
            subject_alternative_name: cert
                .sans
                .first()
                .cloned()
                .unwrap_or_else(|| "unknown".into()),
            issuer,
            verified_at: integrated_time,
        })
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
