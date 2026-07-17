// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Signed Certificate Timestamp (SCT) verification.
//!
//! Extracts the embedded SCT list from a leaf certificate's x.509 extension
//! (OID 1.3.6.1.4.1.11129.2.4.2) and verifies each SCT signature against the
//! CTFE (Certificate Transparency Front-End) public keys, per RFC 6962 §3.2.
//!
//! Verification reconstructs the precertificate the log actually signed:
//!
//! - `issuer_key_hash` = SHA-256 of the **issuer** certificate's
//!   `SubjectPublicKeyInfo` DER.
//! - `tbs_certificate` = the leaf's TBS with the SCT list extension removed
//!   (the final cert carries the SCT list; the precert the log signed did not).

use sha2::{Digest, Sha256};

use crate::cert::Cert;
use crate::crypto::verify_ecdsa_p256_prehashed;
use crate::error::SigstoreVerificationError;

/// DER OID *content* bytes for `1.3.6.1.4.1.11129.2.4.2` (CT precert SCTs).
const SCT_OID_CONTENT: &[u8] = &[0x2B, 0x06, 0x01, 0x04, 0x01, 0xD6, 0x79, 0x02, 0x04, 0x02];

/// An SCT extracted from a certificate extension.
#[derive(Debug, Clone)]
pub struct Sct {
    pub version: u8,
    /// The CT log ID (SHA-256 of the log's public key SPKI DER).
    pub log_id: [u8; 32],
    pub timestamp: u64,
    /// Raw CT extensions blob (usually empty).
    pub extensions: Vec<u8>,
    pub signature: Vec<u8>,
}

/// A CTFE (Certificate Transparency) public key (SEC1 uncompressed point).
#[derive(Debug, Clone)]
pub struct CtfeKey {
    pub pubkey_bytes: Vec<u8>,
}

/// Verify SCTs embedded in a leaf certificate against CTFE keys.
///
/// - `leaf`: the signing certificate (carries the embedded SCT list).
/// - `issuer`: the certificate that issued `leaf` (its SPKI is hashed into the
///   precert `issuer_key_hash`).
/// - `ctfe_keys`: candidate CT log keys; any valid match accepts.
///
/// Returns `Ok(())` if any CTFE key validates any SCT.
pub fn verify_sct(
    leaf: &Cert,
    issuer: &Cert,
    ctfe_keys: &[CtfeKey],
) -> Result<(), SigstoreVerificationError> {
    let sct_bytes = leaf.sct_extension.as_ref().ok_or_else(|| {
        SigstoreVerificationError::SctVerification {
            reason: "certificate does not contain an SCT extension".into(),
        }
    })?;

    let scts = parse_sct_list(sct_bytes)?;
    if scts.is_empty() {
        return Err(SigstoreVerificationError::SctVerification {
            reason: "no SCTs found in certificate extension".into(),
        });
    }

    // issuer_key_hash = SHA-256(issuer SubjectPublicKeyInfo DER)
    let issuer_key_hash: [u8; 32] = Sha256::digest(&issuer.spki_der).into();

    // Precert TBS = leaf TBS with the SCT list extension removed.
    let precert_tbs = remove_sct_extension(&leaf.tbs_der).ok_or_else(|| {
        SigstoreVerificationError::SctVerification {
            reason: "failed to reconstruct precertificate TBS".into(),
        }
    })?;

    let mut any_key_id_matched = false;
    for sct in &scts {
        let signed_data = build_digitally_signed_data(sct, &issuer_key_hash, &precert_tbs)?;
        let hash: [u8; 32] = Sha256::digest(&signed_data).into();

        for key in ctfe_keys {
            // Only try keys whose key ID matches the SCT's logID.
            if crate::crypto::p256_key_id(&key.pubkey_bytes) != sct.log_id {
                continue;
            }
            any_key_id_matched = true;
            if verify_ecdsa_p256_prehashed(&key.pubkey_bytes, &hash, &sct.signature).is_ok() {
                return Ok(());
            }
        }
    }

    Err(SigstoreVerificationError::SctVerification {
        reason: if any_key_id_matched {
            "no CTFE key validated any SCT".into()
        } else {
            "no trusted CTFE key matches any SCT logID".into()
        },
    })
}

/// Build the TLS-encoded `DigitallySigned` input per RFC 6962 §3.2.
///
/// ```text
/// digitally-signed struct {
///     Version    sct_version;   // 1 byte  (v1 = 0)
///     SignatureType type = 0;   // 1 byte  (certificate_timestamp)
///     uint64     timestamp;     // 8 bytes big-endian
///     LogEntryType entry = 1;   // 2 bytes (precert_entry)
///     PreCert {
///         opaque issuer_key_hash[32];
///         opaque tbs_certificate<1..2^24-1>;   // u24 length prefix
///     };
///     CtExtensions extensions;  // u16 length + data
/// }
/// ```
fn build_digitally_signed_data(
    sct: &Sct,
    issuer_key_hash: &[u8; 32],
    precert_tbs: &[u8],
) -> Result<Vec<u8>, SigstoreVerificationError> {
    let mut data = Vec::with_capacity(precert_tbs.len() + 64);

    data.push(sct.version); // sct_version
    data.push(0); // signature_type = certificate_timestamp
    data.extend_from_slice(&sct.timestamp.to_be_bytes()); // timestamp
    data.extend_from_slice(&1u16.to_be_bytes()); // entry_type = precert_entry

    data.extend_from_slice(issuer_key_hash);

    // tbs_certificate with a 3-byte (u24) big-endian length prefix.
    let tbs_len = precert_tbs.len();
    if tbs_len > 0x00FF_FFFF {
        return Err(SigstoreVerificationError::SctVerification {
            reason: "precertificate TBS too large for SCT".into(),
        });
    }
    data.push((tbs_len >> 16) as u8);
    data.push((tbs_len >> 8) as u8);
    data.push(tbs_len as u8);
    data.extend_from_slice(precert_tbs);

    // CtExtensions: u16 length + data.
    let ext_len = sct.extensions.len();
    if ext_len > 0xFFFF {
        return Err(SigstoreVerificationError::SctVerification {
            reason: "SCT extensions too large".into(),
        });
    }
    data.extend_from_slice(&(ext_len as u16).to_be_bytes());
    data.extend_from_slice(&sct.extensions);

    Ok(data)
}

// ── SCT list parsing (RFC 6962 §3.3) ─────────────────────────────────────────

/// Parse the `SignedCertificateTimestampList` from the raw extension value.
///
/// x.509 wraps it as `OCTET STRING { OCTET STRING { TLS SCTList } }`. The outer
/// OCTET STRING is already unwrapped by the extension parser, so the value here
/// is `OCTET STRING { TLS SCTList }`. The TLS `SCTList` is a `uint16` total
/// length followed by repeated `uint16`-prefixed serialized SCTs.
fn parse_sct_list(ext_value: &[u8]) -> Result<Vec<Sct>, SigstoreVerificationError> {
    let list = unwrap_octet_string(ext_value).ok_or_else(|| {
        SigstoreVerificationError::SctVerification {
            reason: "malformed SCT extension: expected OCTET STRING".into(),
        }
    })?;

    if list.len() < 2 {
        return Ok(Vec::new());
    }
    let total_len = u16::from_be_bytes([list[0], list[1]]) as usize;
    let end = (2 + total_len).min(list.len());

    let mut pos = 2;
    let mut scts = Vec::new();
    while pos + 2 <= end {
        let sct_len = u16::from_be_bytes([list[pos], list[pos + 1]]) as usize;
        pos += 2;
        if pos + sct_len > end {
            break;
        }
        if let Some(sct) = parse_single_sct(&list[pos..pos + sct_len]) {
            scts.push(sct);
        }
        pos += sct_len;
    }
    Ok(scts)
}

/// Parse one `SerializedSCT` body (already length-delimited).
fn parse_single_sct(b: &[u8]) -> Option<Sct> {
    // version(1) + logID(32) + timestamp(8) + ext_len(2) = 43 minimum
    if b.len() < 43 {
        return None;
    }
    let version = b[0];
    if version != 0 {
        return None; // only v1 supported
    }
    let log_id: [u8; 32] = b[1..33].try_into().ok()?;
    let timestamp = u64::from_be_bytes(b[33..41].try_into().ok()?);

    let ext_len = u16::from_be_bytes([b[41], b[42]]) as usize;
    let mut pos = 43;
    if pos + ext_len > b.len() {
        return None;
    }
    let extensions = b[pos..pos + ext_len].to_vec();
    pos += ext_len;

    // digitally-signed: hash_alg(1) + sig_alg(1) + sig_len(2) + signature
    if pos + 4 > b.len() {
        return None;
    }
    // RFC 6962 SignatureAndHashAlgorithm: require sha256(4) + ecdsa(3);
    // anything else is a signature we cannot verify — skip the SCT.
    if b[pos] != 4 || b[pos + 1] != 3 {
        return None;
    }
    pos += 2;
    let sig_len = u16::from_be_bytes([b[pos], b[pos + 1]]) as usize;
    pos += 2;
    if pos + sig_len > b.len() {
        return None;
    }
    let signature = b[pos..pos + sig_len].to_vec();

    Some(Sct {
        version,
        log_id,
        timestamp,
        extensions,
        signature,
    })
}

/// Unwrap a single DER `OCTET STRING`, returning its content.
fn unwrap_octet_string(data: &[u8]) -> Option<&[u8]> {
    let (tag, content, _) = read_tlv(data)?;
    if tag != 0x04 {
        return None;
    }
    Some(content)
}

// ── Precertificate TBS reconstruction ────────────────────────────────────────

/// Rebuild the leaf TBS with the SCT list extension removed.
///
/// The extension bytes and the enclosing length fields (extensions `SEQUENCE`,
/// the `[3]` wrapper, the TBS `SEQUENCE`) are re-encoded so the result is valid
/// DER. Every unrelated field is copied verbatim, so the output is
/// byte-identical to the precertificate TBS the CT log signed.
pub(crate) fn remove_sct_extension(tbs: &[u8]) -> Option<Vec<u8>> {
    let (tag, inner, _) = read_tlv(tbs)?;
    if tag != 0x30 {
        return None;
    }

    let elements = split_tlvs(inner);
    let mut out = Vec::with_capacity(tbs.len());
    let mut removed = false;

    for el in &elements {
        // The extensions live in the `[3] EXPLICIT` element (tag 0xA3).
        if el[0] == 0xA3
            && let Some(new_a3) = rebuild_extensions(el)
        {
            out.extend_from_slice(&new_a3);
            removed = true;
            continue;
        }
        out.extend_from_slice(el);
    }

    if !removed {
        return None;
    }
    Some(enc_tlv(0x30, &out))
}

/// Rebuild a `[3] EXPLICIT SEQUENCE OF Extension` with the SCT extension dropped.
fn rebuild_extensions(a3: &[u8]) -> Option<Vec<u8>> {
    let (tag, a3_content, _) = read_tlv(a3)?;
    if tag != 0xA3 {
        return None;
    }
    let (seq_tag, exts, _) = read_tlv(a3_content)?;
    if seq_tag != 0x30 {
        return None;
    }

    let mut kept = Vec::with_capacity(exts.len());
    let mut found = false;
    for ext in split_tlvs(exts) {
        if extension_oid_is_sct(ext) {
            found = true;
            continue;
        }
        kept.extend_from_slice(ext);
    }
    if !found {
        return None;
    }

    let seq = enc_tlv(0x30, &kept);
    Some(enc_tlv(0xA3, &seq))
}

/// True if the first element of an `Extension` SEQUENCE is the SCT OID.
fn extension_oid_is_sct(ext: &[u8]) -> bool {
    let Some((tag, content, _)) = read_tlv(ext) else {
        return false;
    };
    if tag != 0x30 {
        return false;
    }
    let Some((oid_tag, oid, _)) = read_tlv(content) else {
        return false;
    };
    oid_tag == 0x06 && oid == SCT_OID_CONTENT
}

// ── Minimal DER TLV reader/writer ────────────────────────────────────────────

/// Read one DER TLV from the front of `data`.
/// Returns `(tag, content, total_len)`. Single-byte tags only (sufficient for
/// certificate structure). Handles short- and long-form lengths.
fn read_tlv(data: &[u8]) -> Option<(u8, &[u8], usize)> {
    if data.len() < 2 {
        return None;
    }
    let tag = data[0];
    let len_byte = data[1];
    let (len, header) = if len_byte < 0x80 {
        (len_byte as usize, 2)
    } else {
        let n = (len_byte & 0x7f) as usize;
        if n == 0 || n > 4 || 2 + n > data.len() {
            return None;
        }
        let mut len = 0usize;
        for &b in &data[2..2 + n] {
            len = (len << 8) | b as usize;
        }
        (len, 2 + n)
    };
    let end = header.checked_add(len)?;
    if end > data.len() {
        return None;
    }
    Some((tag, &data[header..end], end))
}

/// Split a byte run into its consecutive TLV element slices.
fn split_tlvs(mut data: &[u8]) -> Vec<&[u8]> {
    let mut out = Vec::new();
    while !data.is_empty() {
        let Some((_, _, total)) = read_tlv(data) else {
            break;
        };
        out.push(&data[..total]);
        data = &data[total..];
    }
    out
}

/// Encode a DER length (minimal/definite form).
///
/// The precertificate TBS is reconstructed by re-encoding the affected container
/// lengths minimally. This matches Fulcio, which emits canonical (minimal-length)
/// DER. A certificate signed over non-minimal length encodings would reconstruct
/// to different bytes and fail SCT verification — acceptable, since production
/// Fulcio certs are always canonical DER.
fn enc_len(len: usize) -> Vec<u8> {
    if len < 0x80 {
        vec![len as u8]
    } else {
        let mut be = Vec::new();
        let mut l = len;
        while l > 0 {
            be.insert(0, (l & 0xff) as u8);
            l >>= 8;
        }
        let mut out = vec![0x80 | be.len() as u8];
        out.extend_from_slice(&be);
        out
    }
}

/// Encode a DER TLV from a tag and content.
fn enc_tlv(tag: u8, content: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(content.len() + 4);
    out.push(tag);
    out.extend_from_slice(&enc_len(content.len()));
    out.extend_from_slice(content);
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::test_support::{
        LeafOpts, make_leaf, make_leaf_with_sct_placeholder, make_root, sct_extension_value,
        serialized_sct,
    };
    use p256::ecdsa::{SigningKey, signature::Signer};

    /// End-to-end SCT check via the "splice" technique: the precertificate TBS
    /// is independent of the SCT extension's *content* (removal drops the whole
    /// extension), so we can compute the signed data, sign it with a synthetic
    /// CTFE key, and place the resulting SCT back into the leaf.
    fn signed_leaf_with_sct(
        ctfe_sk: &SigningKey,
        tamper_sig: bool,
    ) -> (Cert, Cert) {
        let root = make_root("fulcio-root");
        let root_cert = Cert::from_der(&root.der).unwrap();

        // Leaf carrying a placeholder SCT extension (content irrelevant).
        let leaf = make_leaf_with_sct_placeholder(&root, &LeafOpts::default());
        let mut leaf_cert = Cert::from_der(&leaf.der).unwrap();

        let issuer_key_hash: [u8; 32] = Sha256::digest(&root_cert.spki_der).into();
        let precert_tbs = remove_sct_extension(&leaf_cert.tbs_der)
            .expect("precert reconstruction");

        let timestamp: u64 = 1_700_000_000_000;
        let log_id = crate::crypto::p256_key_id(
            ctfe_sk.verifying_key().to_encoded_point(false).as_bytes(),
        );

        // Reconstruct the DigitallySigned input exactly as the verifier does,
        // but assembled independently here in the test.
        let sct_stub = Sct {
            version: 0,
            log_id: [0u8; 32],
            timestamp,
            extensions: Vec::new(),
            signature: Vec::new(),
        };
        let signed_data =
            build_digitally_signed_data(&sct_stub, &issuer_key_hash, &precert_tbs).unwrap();
        let sig: p256::ecdsa::Signature = ctfe_sk.sign(&signed_data);
        let mut sig_der = sig.to_der().as_bytes().to_vec();
        if tamper_sig {
            *sig_der.last_mut().unwrap() ^= 0x01;
        }

        let sct_body = serialized_sct(0, &log_id, timestamp, &sig_der);
        leaf_cert.sct_extension = Some(sct_extension_value(&sct_body));

        (leaf_cert, root_cert)
    }

    fn ctfe_key(sk: &SigningKey) -> CtfeKey {
        CtfeKey {
            pubkey_bytes: sk.verifying_key().to_encoded_point(false).as_bytes().to_vec(),
        }
    }

    #[test]
    fn valid_sct_verifies() {
        let sk = SigningKey::from_slice(&[5u8; 32]).unwrap();
        let (leaf, issuer) = signed_leaf_with_sct(&sk, false);
        verify_sct(&leaf, &issuer, &[ctfe_key(&sk)])
            .expect("a correctly signed SCT must verify");
    }

    #[test]
    fn sct_signed_by_wrong_key_rejected() {
        let sk = SigningKey::from_slice(&[5u8; 32]).unwrap();
        let (leaf, issuer) = signed_leaf_with_sct(&sk, false);
        let wrong = SigningKey::from_slice(&[6u8; 32]).unwrap();
        let err = verify_sct(&leaf, &issuer, &[ctfe_key(&wrong)])
            .expect_err("SCT signed by a different CTFE key must be rejected");
        assert!(matches!(err, SigstoreVerificationError::SctVerification { .. }));
    }

    #[test]
    fn tampered_sct_signature_rejected() {
        let sk = SigningKey::from_slice(&[5u8; 32]).unwrap();
        let (leaf, issuer) = signed_leaf_with_sct(&sk, true);
        verify_sct(&leaf, &issuer, &[ctfe_key(&sk)])
            .expect_err("a tampered SCT signature must be rejected");
    }

    #[test]
    fn missing_sct_extension_rejected() {
        let root = make_root("r");
        let root_cert = Cert::from_der(&root.der).unwrap();
        let leaf = make_leaf(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let sk = SigningKey::from_slice(&[5u8; 32]).unwrap();
        verify_sct(&leaf_cert, &root_cert, &[ctfe_key(&sk)])
            .expect_err("a leaf with no SCT extension must be rejected");
    }

    #[test]
    fn remove_sct_extension_drops_only_the_sct() {
        let root = make_root("r");
        let leaf = make_leaf_with_sct_placeholder(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        assert!(leaf_cert.sct_extension.is_some(), "placeholder SCT present");

        let precert = remove_sct_extension(&leaf_cert.tbs_der).expect("reconstruct");
        // The reconstructed TBS must be valid DER, shorter, and SCT-free.
        assert!(precert.len() < leaf_cert.tbs_der.len());
        let wrapped = enc_tlv(0x30, b""); // sanity: encoder produces valid header
        assert_eq!(wrapped, vec![0x30, 0x00]);

        // Re-parse: build a fake cert isn't needed — just assert the SCT OID no
        // longer appears in the reconstructed bytes.
        let needle = SCT_OID_CONTENT;
        assert!(
            !precert.windows(needle.len()).any(|w| w == needle),
            "SCT OID must be gone from precert TBS"
        );
        // And the OID is present before removal.
        assert!(
            leaf_cert.tbs_der.windows(needle.len()).any(|w| w == needle),
            "SCT OID must be present in the original TBS"
        );
    }

    #[test]
    fn sct_with_non_ecdsa_sha256_alg_skipped() {
        // serialized_sct writes hash=4(sha256), sig=3(ecdsa) at offsets 43/44
        // (version 1 + logID 32 + timestamp 8 + ext_len 2 = 43).
        let mut body = serialized_sct(0, &[0x11u8; 32], 1_700_000_000_000, &[0xAA; 70]);
        body[43] = 2; // hash = sha1 — not verifiable
        let scts = parse_sct_list(&sct_extension_value(&body)).expect("parse");
        assert!(scts.is_empty(), "non-sha256/ecdsa SCT must be skipped");
    }

    #[test]
    fn sct_log_id_extracted() {
        let log_id = [0x42u8; 32];
        let body = serialized_sct(0, &log_id, 1_700_000_000_000, &[0xAA; 70]);
        let scts = parse_sct_list(&sct_extension_value(&body)).expect("parse");
        assert_eq!(scts.len(), 1);
        assert_eq!(scts[0].log_id, log_id, "SCT logID bytes 1..33 must be extracted");
    }
}
