// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trust root material — Fulcio roots, Rekor keys, CTFE keys.
//!
//! All trust material is provided by the caller (no network calls).
//! Multiple entries per field support key rotation.

use crate::cert::Cert;
use crate::error::SigstoreVerificationError;
use crate::sct::CtfeKey;

/// Raw PEM-encoded trust material.
/// the verifier tries all roots during chain building and accepts the one
/// that validates the leaf certificate.
#[derive(Debug, Clone)]
pub struct SigstoreTrustRootRaw {
    /// PEM-encoded Fulcio root CA certificates.
    pub fulcio_root_certs: Vec<Vec<u8>>,
    /// PEM-encoded Fulcio intermediate CA certificates.
    pub fulcio_intermediate_certs: Vec<Vec<u8>>,
    /// PEM-encoded Rekor signing keys (public keys).
    pub rekor_keys: Vec<Vec<u8>>,
    /// PEM-encoded CTFE (Certificate Transparency) public keys.
    pub ctfe_keys: Vec<Vec<u8>>,
}

/// Parsed trust root material ready for verification.
#[derive(Debug, Clone)]
pub struct TrustRoot {
    /// Parsed Fulcio root CAs.
    pub fulcio_roots: Vec<Cert>,
    /// Parsed Fulcio intermediate CAs.
    pub fulcio_intermediates: Vec<Cert>,
    /// Parsed Rekor public keys (raw SEC1 bytes).
    pub rekor_keys: Vec<Vec<u8>>,
    /// Parsed CTFE public keys.
    pub ctfe_keys: Vec<CtfeKey>,
}

impl SigstoreTrustRootRaw {
    /// Construct trust root with public-good Sigstore keys embedded at compile time.
    ///
    /// Uses `include_bytes!` — zero network, zero filesystem reads.
    /// The embedded PEM files are validated by `build.rs` at compile time.
    /// This function always returns a valid trust root.
    #[must_use]
    pub fn with_static_trust_root() -> Self {
        Self {
            fulcio_root_certs: vec![
                include_bytes!("trust/fulcio_root.pem").to_vec(),
            ],
            fulcio_intermediate_certs: vec![
                include_bytes!("trust/fulcio_intermediate.pem").to_vec(),
            ],
            rekor_keys: vec![
                include_bytes!("trust/rekor.pem").to_vec(),
            ],
            ctfe_keys: vec![
                include_bytes!("trust/ctfe.pem").to_vec(),
                include_bytes!("trust/ctfe_2021.pem").to_vec(),
            ],
        }
    }

    /// Parse the raw PEM trust material into [`TrustRoot`].
    pub fn parse(&self) -> Result<TrustRoot, SigstoreVerificationError> {
        let fulcio_roots: Vec<Cert> = self
            .fulcio_root_certs
            .iter()
            .map(|pem| Cert::from_pem(pem))
            .collect::<Result<Vec<_>, _>>()?;

        let fulcio_intermediates: Vec<Cert> = self
            .fulcio_intermediate_certs
            .iter()
            .map(|pem| Cert::from_pem(pem))
            .collect::<Result<Vec<_>, _>>()?;

        let rekor_keys: Vec<Vec<u8>> = self
            .rekor_keys
            .iter()
            .map(|pem| parse_ec_public_key_pem(pem))
            .collect::<Result<Vec<_>, _>>()?;

        let ctfe_keys: Vec<CtfeKey> = self
            .ctfe_keys
            .iter()
            
            .map(|pem| {
                let key_bytes = parse_ec_public_key_pem(pem)?;
                // Compute key ID: SHA-256 of DER-encoded SPKI
                let key_id = {
                    use sha2::{Digest, Sha256};
                    let hash: [u8; 32] = Sha256::digest(&key_bytes).into();
                    base64::Engine::encode(
                        &base64::engine::general_purpose::STANDARD,
                        hash,
                    )
                };
                Ok(CtfeKey {
                    key_id,
                    pubkey_bytes: key_bytes,
                })
            })
            .collect::<Result<Vec<_>, SigstoreVerificationError>>()?;

        Ok(TrustRoot {
            fulcio_roots,
            fulcio_intermediates,
            rekor_keys,
            ctfe_keys,
        })
    }
}

/// Parse a PEM-encoded EC (ECDSA P-256) public key.
///
/// Extracts the raw SEC1 public key bytes from PEM `SubjectPublicKeyInfo`.
fn parse_ec_public_key_pem(pem_bytes: &[u8]) -> Result<Vec<u8>, SigstoreVerificationError> {
    let der_bytes = crate::cert::parse_pem_to_der(pem_bytes).ok_or_else(|| {
        SigstoreVerificationError::CertificateParsing {
            reason: "PEM parsing failed for EC public key".into(),
        }
    })?;
    parse_ec_spki(&der_bytes)
}

/// Parse a DER-encoded `SubjectPublicKeyInfo` for an EC P-256 key.
///
/// SPKI structure: SEQUENCE { `AlgorithmIdentifier`, BIT STRING (public key) }
/// The BIT STRING contains: 00 04 || X (32 bytes) || Y (32 bytes)
fn parse_ec_spki(der: &[u8]) -> Result<Vec<u8>, SigstoreVerificationError> {
    if der.is_empty() || der[0] != 0x30 {
        return Err(SigstoreVerificationError::CertificateParsing {
            reason: "SPKI must start with SEQUENCE tag".into(),
        });
    }

    // Get content of outer SEQUENCE
    let inner = der_tlv_value(der, 0).map(|(val, _)| val)
        .ok_or_else(|| SigstoreVerificationError::CertificateParsing {
            reason: "failed to parse SPKI SEQUENCE".into(),
        })?;

    // inner = AlgorithmIdentifier SEQUENCE + BIT STRING
    // Skip the AlgorithmIdentifier
    let after_algo = der_tlv_value(inner, 0)
        .map(|(_, consumed)| &inner[consumed..])
        .ok_or_else(|| SigstoreVerificationError::CertificateParsing {
            reason: "failed to skip AlgorithmIdentifier SEQUENCE".into(),
        })?;

    // Parse BIT STRING
    if after_algo.is_empty() || after_algo[0] != 0x03 {
        return Err(SigstoreVerificationError::CertificateParsing {
            reason: "expected BIT STRING after AlgorithmIdentifier".into(),
        });
    }

    let bit_string_content = der_tlv_value(after_algo, 0)
        .map(|(val, _)| val)
        .ok_or_else(|| SigstoreVerificationError::CertificateParsing {
            reason: "failed to parse BIT STRING".into(),
        })?;

    // bit_string_content starts with unused bits byte (0x00) then 0x04 point marker
    if bit_string_content.len() < 3 || bit_string_content[0] != 0x00 || bit_string_content[1] != 0x04 {
        return Err(SigstoreVerificationError::CertificateParsing {
            reason: "expected EC uncompressed point in SPKI bit string".into(),
        });
    }

    Ok(bit_string_content.to_vec())
}

/// Parse a DER TLV at `offset`. Returns `Some((value_slice, next_offset))`.
fn der_tlv_value(data: &[u8], offset: usize) -> Option<(&[u8], usize)> {
    if offset >= data.len() {
        return None;
    }
    let tag = data[offset];
    if tag == 0x30 || tag == 0x03 || tag == 0x06 || tag == 0x04 {
        // SEQUENCE, BIT STRING, OID, OCTET STRING
        let val_offset = offset + 1;
        if val_offset >= data.len() {
            return None;
        }
        let (val, consumed) = der_read_length(data, val_offset)?;
        Some((val, consumed))
    } else {
        None
    }
}

/// Read DER length at `offset`. Returns `Some((value_slice, end_offset))`.
fn der_read_length(data: &[u8], offset: usize) -> Option<(&[u8], usize)> {
    if offset >= data.len() {
        return None;
    }
    let byte = data[offset];
    if byte < 0x80 {
        let len = byte as usize;
        let start = offset + 1;
        if start + len > data.len() {
            return None;
        }
        Some((&data[start..start + len], start + len))
    } else {
        let num_bytes = (byte & 0x7f) as usize;
        if num_bytes == 0 || num_bytes > 4 || offset + 1 + num_bytes > data.len() {
            return None;
        }
        let mut len = 0usize;
        for i in 0..num_bytes {
            len = (len << 8) | data[offset + 1 + i] as usize;
        }
        let start = offset + 1 + num_bytes;
        if start + len > data.len() {
            return None;
        }
        Some((&data[start..start + len], start + len))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn static_trust_root_parses_without_panic() {
        let raw = SigstoreTrustRootRaw::with_static_trust_root();
        let trust_root = raw.parse().expect("embedded trust root must parse");
        assert!(!trust_root.fulcio_roots.is_empty(), "must have Fulcio roots");
        assert!(!trust_root.rekor_keys.is_empty(), "must have Rekor keys");
        assert!(!trust_root.ctfe_keys.is_empty(), "must have CTFE keys");
    }

    #[test]
    fn static_trust_root_fulcio_root_is_ca() {
        let raw = SigstoreTrustRootRaw::with_static_trust_root();
        let trust_root = raw.parse().expect("must parse");
        for root in &trust_root.fulcio_roots {
            root.validate_ca()
                .expect("Fulcio root must be a valid CA");
        }
    }

    #[test]
    fn static_trust_root_intermediate_is_ca() {
        let raw = SigstoreTrustRootRaw::with_static_trust_root();
        let trust_root = raw.parse().expect("must parse");
        for intermediate in &trust_root.fulcio_intermediates {
            intermediate
                .validate_ca()
                .expect("Fulcio intermediate must be a valid CA");
        }
    }
}
