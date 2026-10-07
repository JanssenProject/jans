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

/// Raw PEM-encoded trust material. The verifier tries all roots during chain
/// building and accepts the one that validates the leaf certificate.
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
pub(crate) struct TrustRoot {
    /// Parsed Fulcio root CAs.
    pub(crate) fulcio_roots: Vec<Cert>,
    /// Parsed Fulcio intermediate CAs.
    pub(crate) fulcio_intermediates: Vec<Cert>,
    /// Parsed Rekor public keys (raw SEC1 bytes).
    pub(crate) rekor_keys: Vec<Vec<u8>>,
    /// Parsed CTFE public keys.
    pub(crate) ctfe_keys: Vec<CtfeKey>,
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
            fulcio_root_certs: vec![include_bytes!("trust/fulcio_root.pem").to_vec()],
            fulcio_intermediate_certs: vec![
                include_bytes!("trust/fulcio_intermediate.pem").to_vec(),
            ],
            rekor_keys: vec![include_bytes!("trust/rekor.pem").to_vec()],
            ctfe_keys: vec![
                include_bytes!("trust/ctfe.pem").to_vec(),
                include_bytes!("trust/ctfe_2021.pem").to_vec(),
            ],
        }
    }

    /// Parse the raw PEM trust material into [`TrustRoot`].
    ///
    /// Fulcio root/intermediate CA certificates are also checked against the
    /// current wall-clock time: `build.rs` only guarantees they were valid
    /// (and not close to expiry) *at compile time* — a long-running process
    /// or an old build that's still being used can outlive that. A CA cert
    /// that has since expired is dropped rather than failing the whole
    /// parse, so that if this trust root ever holds multiple root/
    /// intermediate generations (key rotation), one aging out doesn't take
    /// down the still-valid ones. If filtering leaves no valid root (or no
    /// path from leaf to root), that surfaces later as the existing
    /// `CertificateChain` "no trusted root found" error at verification
    /// time — parsing itself doesn't need its own separate failure mode for
    /// this.
    ///
    /// Rekor/CTFE keys are bare public keys with no validity period
    /// embedded in this crate's PEM files, so they aren't checked here
    /// (tracked in the remediation plan: needs sourcing from the full
    /// `trusted_root.json`, which does carry a `validFor` window per key,
    /// instead of standalone PEMs).
    pub(crate) fn parse(&self) -> Result<TrustRoot, SigstoreVerificationError> {
        let all_fulcio_roots: Vec<Cert> = self
            .fulcio_root_certs
            .iter()
            .map(|pem| Cert::from_pem(pem))
            .collect::<Result<Vec<_>, _>>()?;

        let all_fulcio_intermediates: Vec<Cert> = self
            .fulcio_intermediate_certs
            .iter()
            .map(|pem| Cert::from_pem(pem))
            .collect::<Result<Vec<_>, _>>()?;

        let now = chrono::Utc::now().timestamp();
        let fulcio_roots = currently_valid(all_fulcio_roots, now);
        let fulcio_intermediates = currently_valid(all_fulcio_intermediates, now);

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

/// Drop certificates that aren't currently valid.
///
/// No error here even if everything gets filtered out: an empty result
/// (e.g. every embedded Fulcio root has expired) surfaces naturally as the
/// existing `CertificateChain` "no trusted root found" error once a chain
/// build is actually attempted, which already has test coverage — this
/// function doesn't need a second, earlier failure mode for the same
/// condition.
fn currently_valid(certs: impl IntoIterator<Item = Cert>, now: i64) -> Vec<Cert> {
    certs
        .into_iter()
        .filter(|cert| cert.check_validity(now).is_ok())
        .collect()
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
        assert!(
            !trust_root.fulcio_roots.is_empty(),
            "must have Fulcio roots"
        );
        assert!(!trust_root.rekor_keys.is_empty(), "must have Rekor keys");
        assert!(!trust_root.ctfe_keys.is_empty(), "must have CTFE keys");
    }

    #[test]
    fn static_trust_root_fulcio_root_is_ca() {
        let raw = SigstoreTrustRootRaw::with_static_trust_root();
        let trust_root = raw.parse().expect("must parse");
        for root in &trust_root.fulcio_roots {
            root.validate_ca().expect("Fulcio root must be a valid CA");
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

    #[test]
    fn currently_valid_drops_expired_keeps_valid() {
        use crate::cert::Cert;
        use crate::test_support::{make_root, make_root_expired};

        let expired = Cert::from_der(&make_root_expired("expired").der).expect("parse expired");
        let valid = Cert::from_der(&make_root("valid").der).expect("parse valid");
        let now = chrono::Utc::now().timestamp();

        let kept = currently_valid(vec![expired, valid], now);
        assert_eq!(kept.len(), 1, "only the still-valid cert should remain");
    }

    #[test]
    fn currently_valid_returns_empty_when_all_expired() {
        use crate::cert::Cert;
        use crate::test_support::make_root_expired;

        let expired = Cert::from_der(&make_root_expired("expired").der).expect("parse expired");
        let now = chrono::Utc::now().timestamp();

        let kept = currently_valid(vec![expired], now);
        assert!(
            kept.is_empty(),
            "an all-expired input should filter down to empty, not error \
             here — the empty-root case is exercised (and rejected) at \
             chain-validation time instead"
        );
    }
}
