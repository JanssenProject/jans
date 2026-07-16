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
                Ok(CtfeKey {
                    pubkey_bytes: parse_ec_public_key_pem(pem)?,
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
/// Extracts the raw SEC1 public key point (`04 || X || Y`) from the PEM
/// `SubjectPublicKeyInfo` via `x509-parser` — the same parser used for
/// certificates, so there is a single DER code path.
fn parse_ec_public_key_pem(pem_bytes: &[u8]) -> Result<Vec<u8>, SigstoreVerificationError> {
    use x509_parser::prelude::FromDer;
    use x509_parser::x509::SubjectPublicKeyInfo;

    let der_bytes = crate::cert::parse_pem_to_der(pem_bytes).ok_or_else(|| {
        SigstoreVerificationError::CertificateParsing {
            reason: "PEM parsing failed for EC public key".into(),
        }
    })?;

    let (_, spki) = SubjectPublicKeyInfo::from_der(&der_bytes).map_err(|e| {
        SigstoreVerificationError::CertificateParsing {
            reason: format!("SPKI DER parsing failed: {e}"),
        }
    })?;

    // `subject_public_key.data` is the BIT STRING payload with the unused-bits
    // byte already stripped: for EC P-256 this is the SEC1 uncompressed point.
    let point = spki.subject_public_key.data.to_vec();
    if point.first() != Some(&0x04) {
        return Err(SigstoreVerificationError::CertificateParsing {
            reason: "expected EC uncompressed point (0x04) in SPKI".into(),
        });
    }
    Ok(point)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Regression: parsed Rekor/CTFE keys must load as real P-256 verifying
    /// keys. The previous hand-rolled SPKI parser left the BIT STRING
    /// unused-bits byte in place (66-byte `00 04 …`), which `from_sec1_bytes`
    /// rejects — silently breaking SET and SCT verification against the
    /// embedded keys.
    #[test]
    fn static_keys_load_as_p256_verifying_keys() {
        let raw = SigstoreTrustRootRaw::with_static_trust_root();
        let tr = raw.parse().expect("parse");
        assert!(!tr.rekor_keys.is_empty() && !tr.ctfe_keys.is_empty());
        for k in &tr.rekor_keys {
            assert_eq!(k.len(), 65, "SEC1 uncompressed point must be 65 bytes");
            p256::ecdsa::VerifyingKey::from_sec1_bytes(k)
                .expect("rekor key must load as P-256 VerifyingKey");
        }
        for k in &tr.ctfe_keys {
            p256::ecdsa::VerifyingKey::from_sec1_bytes(&k.pubkey_bytes)
                .expect("ctfe key must load as P-256 VerifyingKey");
        }
    }

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
