// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! X.509 certificate parsing and validation.
//!
//! Uses `x509-parser` for zero-copy, pure-Rust parsing.
//! Extracts pubkey, SAN, issuer extension, validity, SCT, `BasicConstraints`, EKU.

use p256::ecdsa::VerifyingKey;
use x509_parser::certificate::X509Certificate;
use x509_parser::prelude::*;

use crate::error::SigstoreVerificationError;

/// OID for the Fulcio OIDC issuer extension (v2).
const OID_ISSUER_V2: &str = "1.3.6.1.4.1.57264.1.8";

/// OID for the CT Precertificate SCTs extension.
const OID_SCT_LIST: &str = "1.3.6.1.4.1.11129.2.4.2";

/// OID for Extended Key Usage: code signing.
const OID_EKU_CODE_SIGNING: &str = "1.3.6.1.5.5.7.3.3";

/// A parsed X.509 certificate with extracted fields needed for Sigstore verification.
#[derive(Debug, Clone)]
pub struct Cert {
    /// The raw DER bytes of the certificate.
    pub der: Vec<u8>,

    /// The public key bytes (SEC1 uncompressed point for ECDSA P-256).
    pub pubkey_bytes: Vec<u8>,

    /// The full DER of this cert's `SubjectPublicKeyInfo`.
    /// Used as the SCT `issuer_key_hash` input (SHA-256 over the issuer SPKI).
    pub spki_der: Vec<u8>,

    /// Subject Alternative Names (URIs and email addresses).
    pub sans: Vec<String>,

    /// The OIDC issuer extracted from the Fulcio extension (OID 1.3.6.1.4.1.57264.1.8).
    pub issuer: Option<String>,

    /// Certificate validity: not before (UNIX epoch seconds).
    pub not_before: i64,

    /// Certificate validity: not after (UNIX epoch seconds).
    pub not_after: i64,

    /// The raw bytes of the SCT extension (if present).
    pub sct_extension: Option<Vec<u8>>,

    /// Whether this certificate is a CA (`BasicConstraints` CA:TRUE).
    pub is_ca: bool,

    /// The pathLen constraint from `BasicConstraints` (None if no constraint).
    pub path_len: Option<u32>,

    /// Whether the certificate has the code signing EKU.
    pub has_code_signing_eku: bool,

    /// Whether the certificate has the keyCertSign key usage.
    pub has_key_cert_sign: bool,

    /// The TBS certificate DER bytes (for chain validation).
    pub tbs_der: Vec<u8>,

    /// The signature value from the certificate (BIT STRING payload).
    pub signature_value: Vec<u8>,

    /// The certificate's `signatureAlgorithm` OID (dotted string), e.g.
    /// `1.2.840.10045.4.3.3` for ecdsa-with-SHA384. Determines the digest used
    /// when verifying this cert's signature against its issuer.
    pub signature_algorithm_oid: String,

    /// The issuer DN as string.
    pub issuer_dn: String,

    /// The subject DN as string.
    pub subject_dn: String,
}

impl Cert {
    /// Parse a DER-encoded X.509 certificate.
    pub fn from_der(der_bytes: &[u8]) -> Result<Self, SigstoreVerificationError> {
        let (_, cert) = X509Certificate::from_der(der_bytes).map_err(|e| {
            SigstoreVerificationError::CertificateParsing {
                reason: format!("DER parsing failed: {e}"),
            }
        })?;

        Ok(Self::from_parsed(&cert, der_bytes.to_vec()))
    }

    /// Parse a PEM-encoded X.509 certificate.
    pub fn from_pem(pem_bytes: &[u8]) -> Result<Self, SigstoreVerificationError> {
        let der_bytes = parse_pem_to_der(pem_bytes).ok_or_else(|| {
            SigstoreVerificationError::CertificateParsing {
                reason: "PEM parsing failed".into(),
            }
        })?;
        Self::from_der(&der_bytes)
    }

    fn from_parsed(cert: &X509Certificate, der: Vec<u8>) -> Self {
        let tbs = &cert.tbs_certificate;

        let subject_pki = &tbs.subject_pki;
        let pubkey_bytes = subject_pki.subject_public_key.data.to_vec();
        let spki_der = subject_pki.raw.to_vec();

        let sans = extract_sans(tbs);

        let issuer = extract_issuer_extension(tbs);

        let not_before = tbs.validity.not_before.timestamp();
        let not_after = tbs.validity.not_after.timestamp();

        let sct_extension = extract_sct_extension(tbs);

        let (is_ca, path_len) = extract_basic_constraints(tbs);

        let has_code_signing_eku = extract_eku_code_signing(tbs);

        let has_key_cert_sign = extract_key_usage_key_cert_sign(tbs);

        let issuer_dn = cert.issuer().to_string();
        let subject_dn = cert.subject().to_string();

        // TBS DER and signature value come straight from `x509-parser` — the
        // raw TBS bytes (what gets hashed for chain validation) and the
        // BIT STRING payload (unused-bits byte already stripped).
        let tbs_der = tbs.as_ref().to_vec();
        let signature_value = cert.signature_value.data.to_vec();
        let signature_algorithm_oid = cert.signature_algorithm.algorithm.to_id_string();

        Self {
            der,
            pubkey_bytes,
            spki_der,
            sans,
            issuer,
            not_before,
            not_after,
            sct_extension,
            is_ca,
            path_len,
            has_code_signing_eku,
            has_key_cert_sign,
            tbs_der,
            signature_value,
            signature_algorithm_oid,
            issuer_dn,
            subject_dn,
        }
    }
}

impl Cert {
    /// Returns the ECDSA P-256 verifying key from the certificate's SPKI.
    pub fn verifying_key(&self) -> Result<VerifyingKey, SigstoreVerificationError> {
        VerifyingKey::from_sec1_bytes(&self.pubkey_bytes).map_err(|e| {
            SigstoreVerificationError::CertificateParsing {
                reason: format!("invalid public key in cert: {e}"),
            }
        })
    }
}

// ── Extension extraction helpers ────────────────────────────────────────────

fn extract_sans(tbs: &TbsCertificate) -> Vec<String> {
    let mut sans = Vec::new();
    if let Ok(Some(ext)) = tbs.subject_alternative_name() {
        // ext is BasicExtension<&SubjectAlternativeName>
        let value = &ext.value;
        for name in &value.general_names {
            match name {
                GeneralName::URI(uri) => sans.push(uri.to_string()),
                GeneralName::RFC822Name(email) => sans.push(email.to_string()),
                _ => {}
            }
        }
    }
    // Collect from extensions
    for ext in tbs.extensions() {
        if let ParsedExtension::SubjectAlternativeName(san) = ext.parsed_extension() {
            for name in &san.general_names {
                match name {
                    GeneralName::URI(uri) => sans.push(uri.to_string()),
                    GeneralName::RFC822Name(email) => sans.push(email.to_string()),
                    _ => {}
                }
            }
        }
    }
    sans
}

fn extract_issuer_extension(tbs: &TbsCertificate) -> Option<String> {
    for ext in tbs.extensions() {
        if ext.oid.to_id_string() == OID_ISSUER_V2 {
            // The extension value is a DER-encoded UTF8String
            if let Ok(value) = ext.value.parse_der_utf8string() {
                return Some(value);
            }
        }
    }
    None
}

fn extract_sct_extension(tbs: &TbsCertificate) -> Option<Vec<u8>> {
    for ext in tbs.extensions() {
        if ext.oid.to_id_string() == OID_SCT_LIST {
            return Some(ext.value.to_vec());
        }
    }
    None
}

fn extract_basic_constraints(tbs: &TbsCertificate) -> (bool, Option<u32>) {
    for ext in tbs.extensions() {
        if let ParsedExtension::BasicConstraints(bc) = ext.parsed_extension() {
            return (bc.ca, bc.path_len_constraint);
        }
    }
    (false, None)
}

fn extract_eku_code_signing(tbs: &TbsCertificate) -> bool {
    for ext in tbs.extensions() {
        if let ParsedExtension::ExtendedKeyUsage(eku) = ext.parsed_extension() {
            if eku.code_signing {
                return true;
            }
            // Also check by OID
            for oid in &eku.other {
                if oid.to_id_string() == OID_EKU_CODE_SIGNING {
                    return true;
                }
            }
        }
    }
    false
}

fn extract_key_usage_key_cert_sign(tbs: &TbsCertificate) -> bool {
    for ext in tbs.extensions() {
        if let ParsedExtension::KeyUsage(ku) = ext.parsed_extension() {
            return ku.key_cert_sign();
        }
    }
    false
}

/// Parse a `UTF8String` from DER-encoded extension bytes.
trait DerUtf8String {
    fn parse_der_utf8string(&self) -> Result<String, ()>;
}

impl DerUtf8String for [u8] {
    fn parse_der_utf8string(&self) -> Result<String, ()> {
        // The DER encoding of a UTF8String is: 0x0C <len> <bytes>
        if self.len() < 2 || self[0] != 0x0C {
            return Err(());
        }
        let len = self[1] as usize;
        if self.len() < 2 + len {
            return Err(());
        }
        String::from_utf8(self[2..2 + len].to_vec()).map_err(|_| ())
    }
}

// ── Cert validation checks ───────────────────────────────────────────────────

impl Cert {
    /// Validate that the leaf certificate is not a CA and has the code signing EKU.
    pub fn validate_leaf(&self) -> Result<(), SigstoreVerificationError> {
        if self.is_ca {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: "leaf certificate must not be a CA (BasicConstraints CA:false)".into(),
            });
        }

        if !self.has_code_signing_eku {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: "leaf certificate must have code signing EKU (1.3.6.1.5.5.7.3.3)".into(),
            });
        }

        Ok(())
    }

    /// Validate that a trust anchor / CA certificate has CA:true and keyCertSign.
    pub fn validate_ca(&self) -> Result<(), SigstoreVerificationError> {
        if !self.is_ca {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: "CA certificate must have BasicConstraints CA:true".into(),
            });
        }

        if !self.has_key_cert_sign {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: "CA certificate must have KeyUsage keyCertSign".into(),
            });
        }

        Ok(())
    }

    /// Check that the certificate is valid at `integrated_time` (UNIX seconds).
    pub fn check_validity(&self, integrated_time: i64) -> Result<(), SigstoreVerificationError> {
        if integrated_time < self.not_before {
            return Err(SigstoreVerificationError::CertificateExpired {
                reason: format!(
                    "certificate not yet valid at signing time: not_before={}, integrated_time={}",
                    self.not_before, integrated_time
                ),
            });
        }
        if integrated_time > self.not_after {
            return Err(SigstoreVerificationError::CertificateExpired {
                reason: format!(
                    "certificate expired at signing time: not_after={}, integrated_time={}",
                    self.not_after, integrated_time
                ),
            });
        }
        Ok(())
    }
}

/// Simple PEM-to-DER conversion without relying on the `pem` crate.
///
/// Extracts the base64 content between `-----BEGIN ...-----` and `-----END ...-----`.
pub(crate) fn parse_pem_to_der(pem_bytes: &[u8]) -> Option<Vec<u8>> {
    let input = std::str::from_utf8(pem_bytes).ok()?;
    let mut in_body = false;
    let mut b64 = String::new();
    for line in input.lines() {
        if line.starts_with("-----BEGIN ") {
            in_body = true;
            continue;
        }
        if line.starts_with("-----END ") {
            break;
        }
        if in_body {
            b64.push_str(line.trim());
        }
    }
    base64::Engine::decode(&base64::engine::general_purpose::STANDARD, b64.as_bytes()).ok()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::test_support::{Ca, LeafOpts, make_leaf, make_root};

    fn leaf_and_root() -> (Cert, Cert, Ca) {
        let root = make_root("test-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let root_cert = Cert::from_der(&root.der).expect("parse root");
        (leaf_cert, root_cert, root)
    }

    #[test]
    fn leaf_fields_extracted() {
        let (leaf, _, _) = leaf_and_root();
        assert!(
            leaf.sans.iter().any(|s| s.contains("github.com/acme/app")),
            "URI SAN must be extracted, got {:?}",
            leaf.sans
        );
        assert_eq!(
            leaf.issuer.as_deref(),
            Some("https://token.actions.githubusercontent.com"),
            "OIDC issuer extension (1.3.6.1.4.1.57264.1.8) must be extracted"
        );
        assert!(leaf.has_code_signing_eku, "code-signing EKU must be detected");
        assert!(!leaf.is_ca, "leaf must not be a CA");
    }

    #[test]
    fn leaf_validates() {
        let (leaf, _, _) = leaf_and_root();
        leaf.validate_leaf().expect("well-formed leaf must validate");
    }

    #[test]
    fn root_is_recognized_as_ca() {
        let (_, root, _) = leaf_and_root();
        assert!(root.is_ca);
        assert!(root.has_key_cert_sign);
        root.validate_ca().expect("root must validate as CA");
    }

    #[test]
    fn leaf_without_code_signing_eku_rejected() {
        let root = make_root("r");
        let leaf = make_leaf(
            &root,
            &LeafOpts {
                code_signing_eku: false,
                ..LeafOpts::default()
            },
        );
        let cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let err = cert
            .validate_leaf()
            .expect_err("leaf lacking code-signing EKU must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from EKU check, got {err:?}"
        );
    }

    #[test]
    fn leaf_marked_ca_rejected() {
        let root = make_root("r");
        let leaf = make_leaf(&root, &LeafOpts { is_ca: true, ..LeafOpts::default() });
        let cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let err = cert
            .validate_leaf()
            .expect_err("a CA:true leaf must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from CA check, got {err:?}"
        );
    }

    #[test]
    fn validity_window_enforced() {
        let (leaf, _, _) = leaf_and_root();
        let mid = i64::midpoint(leaf.not_before, leaf.not_after);
        leaf.check_validity(mid).expect("valid within window");
        let err = leaf
            .check_validity(leaf.not_before - 1)
            .expect_err("must reject a timestamp before not_before");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateExpired { .. }),
            "must be CertificateExpired, got {err:?}"
        );
        let err = leaf
            .check_validity(leaf.not_after + 1)
            .expect_err("must reject a timestamp after not_after");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateExpired { .. }),
            "must be CertificateExpired, got {err:?}"
        );
    }

    #[test]
    fn issuer_absent_when_extension_missing() {
        let root = make_root("r");
        let leaf = make_leaf(&root, &LeafOpts { oidc_issuer: None, ..LeafOpts::default() });
        let cert = Cert::from_der(&leaf.der).unwrap();
        assert!(cert.issuer.is_none(), "no OIDC issuer ext => issuer is None");
    }
}

