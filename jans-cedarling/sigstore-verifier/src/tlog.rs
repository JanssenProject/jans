// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Rekor transparency log verification.
//!
//! Verifies the Signed Entry Timestamp (SET) and checks that the log entry body
//! is consistent with the certificate, signature, and artifact hash
//! (preventing CVE-2022-36056 attacks).

use std::collections::BTreeMap;

use sha2::{Digest, Sha256};

use crate::bundle::{LegacyRekorBundle, TlogEntry};
use crate::cert::Cert;
use crate::crypto::verify_ecdsa_p256_prehashed;
use crate::error::SigstoreVerificationError;

/// Verify the SET (Signed Entry Timestamp) for a Sigstore bundle.
///
/// Steps:
/// 1. Decode `canonicalized_body` (base64 → JSON bytes)
/// 2. Construct `RekorPayload` with the decoded body, integratedTime, logIndex, logID
/// 3. RFC 8785 canonicalize the payload
/// 4. SHA-256 the canonicalized bytes
/// 5. Verify ECDSA signature against the Rekor key
pub fn verify_set_from_bundle(
    tlog_entry: &TlogEntry,
    rekor_key_bytes: &[u8],
) -> Result<i64, SigstoreVerificationError> {
    let integrated_time: i64 = tlog_entry.integrated_time.parse().map_err(|_| {
        SigstoreVerificationError::SetVerification {
            reason: "invalid integratedTime".into(),
        }
    })?;

    let log_index: i64 = tlog_entry.log_index.parse().map_err(|_| {
        SigstoreVerificationError::SetVerification {
            reason: "invalid logIndex".into(),
        }
    })?;

    let log_id = base64_to_hex(&tlog_entry.log_id.key_id)?;

    // The canonicalized_body is base64-encoded JSON bytes of the tlog entry body.
    // We need this as a base64 STRING for SET verification (Rekor signs over
    // the raw base64 string, not the decoded JSON).
    let body_b64 = tlog_entry
        .canonicalized_body
        .as_ref()
        .ok_or_else(|| SigstoreVerificationError::SetVerification {
            reason: "canonicalizedBody is missing".into(),
        })?;

    verify_set(
        body_b64,
        integrated_time,
        log_index,
        &log_id,
        &tlog_entry.inclusion_promise,
        rekor_key_bytes,
    )?;

    Ok(integrated_time)
}

/// Verify the SET for a legacy `RekorBundle`.
pub fn verify_set_legacy(
    legacy: &LegacyRekorBundle,
    rekor_key_bytes: &[u8],
) -> Result<i64, SigstoreVerificationError> {
    let inclusion_promise = crate::bundle::InclusionPromise {
        signed_entry_timestamp: legacy.signed_entry_timestamp.clone(),
    };

    // Pass the body as a base64 string — Rekor signs the raw base64, not decoded JSON.
    verify_set(
        &legacy.payload.body,
        legacy.payload.integrated_time,
        legacy.payload.log_index,
        &legacy.payload.log_id,
        &Some(inclusion_promise),
        rekor_key_bytes,
    )?;

    Ok(legacy.payload.integrated_time)
}

/// Core SET verification.
///
/// Constructs the `RekorPayload` and verifies the SET signature.
/// `body_b64` is the base64-encoded tlog entry body — Rekor signs over
/// the raw base64 string, not the decoded JSON object.
fn verify_set(
    body_b64: &str,
    integrated_time: i64,
    log_index: i64,
    log_id: &str,
    inclusion_promise: &Option<crate::bundle::InclusionPromise>,
    rekor_key_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    // Construct the RekorPayload — body is the base64 STRING per Rekor SET spec.
    let mut payload = BTreeMap::new();
    payload.insert("body".to_string(), serde_json::Value::String(body_b64.to_string()));
    payload.insert(
        "integratedTime".to_string(),
        serde_json::Value::Number(integrated_time.into()),
    );
    payload.insert(
        "logIndex".to_string(),
        serde_json::Value::Number(log_index.into()),
    );
    payload.insert(
        "logID".to_string(),
        serde_json::Value::String(log_id.to_string()),
    );

    // RFC 8785 canonicalize
    let canonicalized = serde_json_canonicalizer::to_vec(&payload).map_err(|e| {
        SigstoreVerificationError::SetVerification {
            reason: format!("RFC 8785 canonicalization failed: {e}"),
        }
    })?;

    // SHA-256 the canonicalized payload
    let hash: [u8; 32] = Sha256::digest(&canonicalized).into();

    // Get the SET signature
    let set_sig_b64 = inclusion_promise
        .as_ref()
        .map(|p| p.signed_entry_timestamp.as_str())
        .ok_or_else(|| SigstoreVerificationError::SetVerification {
            reason: "inclusion promise / SET is missing".into(),
        })?;

    let set_sig = base64::Engine::decode(
        &base64::engine::general_purpose::STANDARD,
        set_sig_b64,
    )
    .map_err(|e| SigstoreVerificationError::SetVerification {
        reason: format!("failed to decode SET signature: {e}"),
    })?;

    // Verify ECDSA signature
    verify_ecdsa_p256_prehashed(rekor_key_bytes, &hash, &set_sig).map_err(|_| {
        SigstoreVerificationError::SetVerification {
            reason: "SET signature verification failed".into(),
        }
    })
}

/// Verify that the Rekor log entry body is consistent with the cert, signature,
/// and artifact hash (preventing CVE-2022-36056).
///
/// `dsse_data` provides DSSE-specific fields for DSSE tlog entries:
/// - `.0`: canonical JSON bytes of the DSSE envelope (for envelopeHash)
/// - `.1`: raw payload bytes (for payloadHash)
pub fn verify_body_consistency(
    tlog_entry: &TlogEntry,
    cert: &Cert,
    signature_b64: &str,
    artifact_digest_hex: &str,
    dsse_data: Option<(&[u8], &[u8])>,
) -> Result<(), SigstoreVerificationError> {
    let canonicalized_body: Vec<u8> = tlog_entry
        .canonicalized_body
        .as_ref()
        .map(|b| {
            base64::Engine::decode(&base64::engine::general_purpose::STANDARD, b)
                .map_err(|e| SigstoreVerificationError::RekorInconsistency {
                    reason: format!("failed to decode canonicalizedBody: {e}"),
                })
        })
        .transpose()?
        .unwrap_or_default();

    let body: serde_json::Value =
        serde_json::from_slice(&canonicalized_body).map_err(|e| {
            SigstoreVerificationError::RekorInconsistency {
                reason: format!("failed to parse canonicalizedBody: {e}"),
            }
        })?;

    let kind = body
        .get("kind")
        .and_then(|v| v.as_str())
        .unwrap_or("unknown");

    match kind {
        "hashedrekord" => {
            verify_hashedrekord_body(&body, cert, signature_b64, artifact_digest_hex)?;
        }
        "dsse" => {
            let (envelope_json, payload_bytes) = dsse_data.ok_or_else(|| {
                SigstoreVerificationError::RekorInconsistency {
                    reason: "DSSE tlog entry requires DSSE data for verification".into(),
                }
            })?;
            verify_dsse_body(&body, cert, signature_b64, envelope_json, payload_bytes)?;
        }
        other => {
            return Err(SigstoreVerificationError::RekorInconsistency {
                reason: format!("unsupported tlog entry kind: {other}"),
            });
        }
    }

    Ok(())
}

/// Verify consistency for a hashedrekord tlog entry body.
fn verify_hashedrekord_body(
    body: &serde_json::Value,
    cert: &Cert,
    signature_b64: &str,
    artifact_digest_hex: &str,
) -> Result<(), SigstoreVerificationError> {
    let spec = body.get("spec").ok_or_else(|| {
        SigstoreVerificationError::RekorInconsistency {
            reason: "tlog body missing 'spec'".into(),
        }
    })?;

    // Check artifact hash
    let data_hash = spec
        .get("data")
        .and_then(|d| d.get("hash"))
        .and_then(|h| h.get("value"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "tlog body missing data.hash.value".into(),
        })?;

    if data_hash != artifact_digest_hex {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "artifact hash mismatch: tlog has '{data_hash}', expected '{artifact_digest_hex}'"
            ),
        });
    }

    // Check signature
    let tlog_sig = spec
        .get("signature")
        .and_then(|s| s.get("content"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "tlog body missing signature.content".into(),
        })?;

    if tlog_sig != signature_b64 {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: "tlog signature doesn't match bundle signature".into(),
        });
    }

    // Check public key / certificate
    let tlog_pubkey = spec
        .get("signature")
        .and_then(|s| s.get("publicKey"))
        .and_then(|pk| pk.get("content"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "tlog body missing signature.publicKey.content".into(),
        })?;

    // The publicKey.content in hashedrekord is base64-encoded PEM certificate
    let tlog_pubkey_bytes = base64::Engine::decode(
        &base64::engine::general_purpose::STANDARD,
        tlog_pubkey,
    )
    .map_err(|e| SigstoreVerificationError::RekorInconsistency {
        reason: format!("failed to decode tlog publicKey: {e}"),
    })?;

    // Compare the certificate. Rekor stores the cert as base64(PEM) in
    // publicKey.content, but some implementations use raw DER. Try both.
    // Parse PEM first (production Rekor), fall back to raw DER.
    let tlog_cert_der = crate::cert::parse_pem_to_der(&tlog_pubkey_bytes)
        .unwrap_or_else(|| tlog_pubkey_bytes.clone());

    if tlog_cert_der != cert.der {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: "tlog certificate doesn't match bundle certificate".into(),
        });
    }

    Ok(())
}

/// Verify consistency for a DSSE tlog entry body.
///
/// Checks (per the Rekor DSSE type v0.0.1):
/// 1. `envelopeHash` matches SHA-256(canonical JSON of the DSSE envelope)
/// 2. `payloadHash` matches SHA-256(raw payload bytes)
/// 3. The tlog signature matches the bundle signature
/// 4. The tlog verifier (cert) matches the bundle certificate
fn verify_dsse_body(
    body: &serde_json::Value,
    cert: &Cert,
    signature_b64: &str,
    envelope_json: &[u8],
    payload_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    let spec = body.get("spec").ok_or_else(|| {
        SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing 'spec'".into(),
        }
    })?;

    // 1. Verify envelopeHash
    let env_hash_algo = spec
        .get("envelopeHash")
        .and_then(|h| h.get("algorithm"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing envelopeHash.algorithm".into(),
        })?;
    if env_hash_algo != "sha256" {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "unsupported envelopeHash algorithm: expected sha256, got {env_hash_algo}"
            ),
        });
    }

    let actual_env_hash = spec
        .get("envelopeHash")
        .and_then(|h| h.get("value"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing envelopeHash.value".into(),
        })?;

    let expected_env_hash: String = {
        use sha2::{Digest, Sha256};
        let hash: [u8; 32] = Sha256::digest(envelope_json).into();
        hash.iter().map(|b| format!("{b:02x}")).collect()
    };

    if actual_env_hash != expected_env_hash {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "DSSE envelopeHash mismatch: tlog has '{actual_env_hash}', computed '{expected_env_hash}'"
            ),
        });
    }

    // 2. Verify payloadHash
    let payload_hash_algo = spec
        .get("payloadHash")
        .and_then(|h| h.get("algorithm"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing payloadHash.algorithm".into(),
        })?;
    if payload_hash_algo != "sha256" {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "unsupported payloadHash algorithm: expected sha256, got {payload_hash_algo}"
            ),
        });
    }

    let actual_payload_hash = spec
        .get("payloadHash")
        .and_then(|h| h.get("value"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing payloadHash.value".into(),
        })?;

    let expected_payload_hash: String = {
        use sha2::{Digest, Sha256};
        let hash: [u8; 32] = Sha256::digest(payload_bytes).into();
        hash.iter().map(|b| format!("{b:02x}")).collect()
    };

    if actual_payload_hash != expected_payload_hash {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "DSSE payloadHash mismatch: tlog has '{actual_payload_hash}', computed '{expected_payload_hash}'"
            ),
        });
    }

    // 3. Verify signature matches
    let tlog_sig_b64 = spec
        .get("signatures")
        .and_then(|s| s.as_array())
        .and_then(|arr| arr.first())
        .and_then(|sig| sig.get("signature"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing signatures[0].signature".into(),
        })?;

    if tlog_sig_b64 != signature_b64 {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog signature doesn't match bundle signature".into(),
        });
    }

    // 4. Verify verifier certificate matches
    let tlog_verifier_b64 = spec
        .get("signatures")
        .and_then(|s| s.as_array())
        .and_then(|arr| arr.first())
        .and_then(|sig| sig.get("verifier"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog body missing signatures[0].verifier".into(),
        })?;

    let verifier_pem = base64::Engine::decode(
        &base64::engine::general_purpose::STANDARD,
        tlog_verifier_b64,
    )
    .map_err(|e| SigstoreVerificationError::RekorInconsistency {
        reason: format!("failed to decode DSSE tlog verifier: {e}"),
    })?;

    if verifier_pem != cert.der {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: "DSSE tlog verifier certificate doesn't match bundle certificate".into(),
        });
    }

    Ok(())
}

/// Convert a base64 (standard) encoded log ID to hex.
fn base64_to_hex(b64: &str) -> Result<String, SigstoreVerificationError> {
    let bytes = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, b64)
        .map_err(|e| SigstoreVerificationError::SetVerification {
            reason: format!("failed to decode logId: {e}"),
        })?;
    Ok(hex::encode(&bytes))
}

// hex module for encoding
mod hex {
    pub fn encode(bytes: &[u8]) -> String {
        bytes.iter().map(|b| format!("{b:02x}")).collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::bundle::{InclusionPromise, KindVersion, LogId};
    use crate::test_support::{LeafOpts, der_to_pem, make_leaf, make_root};
    use p256::ecdsa::{Signature, SigningKey, signature::Signer};
    use serde_json::json;

    fn b64(bytes: &[u8]) -> String {
        base64::Engine::encode(&base64::engine::general_purpose::STANDARD, bytes)
    }

    fn entry_with_body(body: &serde_json::Value) -> TlogEntry {
        TlogEntry {
            log_index: "1".into(),
            log_id: LogId { key_id: b64(&[0u8; 32]) },
            kind_version: KindVersion {
                kind: body.get("kind").and_then(|v| v.as_str()).unwrap_or("hashedrekord").into(),
                version: "0.0.1".into(),
            },
            integrated_time: "1700000000".into(),
            inclusion_promise: None,
            inclusion_proof: None,
            canonicalized_body: Some(b64(&serde_json::to_vec(body).unwrap())),
        }
    }

    /// Build a tlog entry whose SET is signed the way real Rekor signs it:
    /// ECDSA over the RFC-8785 canonical JSON of
    /// `{ body: <base64 STRING>, integratedTime, logIndex, logID: <hex> }`.
    fn signed_tlog_entry(body: &serde_json::Value, integrated_time: i64) -> (TlogEntry, Vec<u8>) {
        let rekor_sk = SigningKey::from_slice(&[3u8; 32]).unwrap();
        let rekor_pk = rekor_sk.verifying_key().to_encoded_point(false).as_bytes().to_vec();

        let body_b64 = b64(&serde_json::to_vec(body).unwrap());
        let log_index: i64 = 42;
        let log_id_raw = [0xABu8; 32];
        let log_id_hex: String = log_id_raw.iter().map(|b| format!("{b:02x}")).collect();

        // Rekor signs `body` as the base64 STRING, not the decoded object.
        let mut payload = std::collections::BTreeMap::new();
        payload.insert("body".to_string(), serde_json::Value::String(body_b64.clone()));
        payload.insert(
            "integratedTime".to_string(),
            serde_json::Value::Number(integrated_time.into()),
        );
        payload.insert("logIndex".to_string(), serde_json::Value::Number(log_index.into()));
        payload.insert("logID".to_string(), serde_json::Value::String(log_id_hex));
        let canonical = serde_json_canonicalizer::to_vec(&payload).unwrap();
        let set_sig: Signature = rekor_sk.sign(&canonical);

        let entry = TlogEntry {
            log_index: log_index.to_string(),
            log_id: LogId { key_id: b64(&log_id_raw) },
            kind_version: KindVersion { kind: "hashedrekord".into(), version: "0.0.1".into() },
            integrated_time: integrated_time.to_string(),
            inclusion_promise: Some(InclusionPromise {
                signed_entry_timestamp: b64(set_sig.to_der().as_bytes()),
            }),
            inclusion_proof: None,
            canonicalized_body: Some(body_b64),
        };
        (entry, rekor_pk)
    }

    #[test]
    fn valid_set_verifies_and_returns_integrated_time() {
        let body = json!({"kind":"hashedrekord","apiVersion":"0.0.1","spec":{}});
        let it = 1_700_000_000i64;
        let (entry, rekor_pk) = signed_tlog_entry(&body, it);
        let result = verify_set_from_bundle(&entry, &rekor_pk)
            .expect("a correctly-signed Rekor SET must verify");
        assert_eq!(
            result, it,
            "must return the authenticated integratedTime"
        );
    }

    #[test]
    fn set_signed_by_wrong_key_rejected() {
        let body = json!({"kind":"hashedrekord"});
        let (entry, _) = signed_tlog_entry(&body, 1);
        let wrong = SigningKey::from_slice(&[8u8; 32])
            .expect("key from seed")
            .verifying_key()
            .to_encoded_point(false)
            .as_bytes()
            .to_vec();
        verify_set_from_bundle(&entry, &wrong)
            .expect_err("SET signed by a different Rekor key must be rejected");
    }

    #[test]
    fn hashedrekord_consistency_accepts_matching_entry() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let cert = Cert::from_der(&leaf.der).unwrap();
        let sig_b64 = b64(b"a-signature");
        let artifact_hex: String = [0xAAu8; 32].iter().map(|b| format!("{b:02x}")).collect();
        let body = json!({
            "kind":"hashedrekord","apiVersion":"0.0.1",
            "spec":{
                "data":{"hash":{"algorithm":"sha256","value": artifact_hex}},
                "signature":{
                    "content": sig_b64,
                    "publicKey":{"content": b64(der_to_pem(&cert.der).as_bytes())}
                }
            }
        });
        let entry = entry_with_body(&body);
        verify_body_consistency(&entry, &cert, &sig_b64, &artifact_hex, None)
            .expect("consistency must accept an entry whose cert/sig/hash match the bundle");
    }

    #[test]
    fn hashedrekord_wrong_artifact_hash_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let cert = Cert::from_der(&leaf.der).unwrap();
        let sig_b64 = b64(b"a-signature");
        let logged_hex: String = [0xBBu8; 32].iter().map(|b| format!("{b:02x}")).collect();
        let our_hex: String = [0xAAu8; 32].iter().map(|b| format!("{b:02x}")).collect();
        let body = json!({
            "kind":"hashedrekord","apiVersion":"0.0.1",
            "spec":{
                "data":{"hash":{"algorithm":"sha256","value": logged_hex}},
                "signature":{"content": sig_b64, "publicKey":{"content": b64(&cert.der)}}
            }
        });
        let entry = entry_with_body(&body);
        let err = verify_body_consistency(&entry, &cert, &sig_b64, &our_hex, None)
            .expect_err("CVE-2022-36056: artifact-hash mismatch must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::RekorInconsistency { .. }),
            "artifact-hash mismatch must be a RekorInconsistency, got {err:?}"
        );
    }

    #[test]
    fn hashedrekord_wrong_signature_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let cert = Cert::from_der(&leaf.der).unwrap();
        let artifact_hex: String = [0xAAu8; 32].iter().map(|b| format!("{b:02x}")).collect();
        let body = json!({
            "kind":"hashedrekord","apiVersion":"0.0.1",
            "spec":{
                "data":{"hash":{"algorithm":"sha256","value": artifact_hex}},
                "signature":{"content": b64(b"logged-sig"), "publicKey":{"content": b64(&cert.der)}}
            }
        });
        let entry = entry_with_body(&body);
        let err = verify_body_consistency(
            &entry, &cert, &b64(b"bundle-sig"), &artifact_hex, None,
        )
        .expect_err("signature mismatch must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::RekorInconsistency { .. }),
            "signature mismatch must be a RekorInconsistency, got {err:?}"
        );
    }
}
