// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Certificate chain validation.
//!
//! Validates that a leaf certificate chains back to a trusted Fulcio root,
//! verifying signatures at each link and checking constraints.
//! Timestamp-anchored: validity is checked against the provided `integrated_time`
//! rather than the current wall clock.

use sha2::{Digest, Sha256, Sha384, Sha512};

use crate::cert::Cert;
use crate::crypto::{verify_ecdsa_p256_prehashed, verify_ecdsa_p384_prehashed};
use crate::error::SigstoreVerificationError;

/// ecdsa-with-SHA256.
const OID_ECDSA_SHA256: &str = "1.2.840.10045.4.3.2";
/// ecdsa-with-SHA384.
const OID_ECDSA_SHA384: &str = "1.2.840.10045.4.3.3";
/// ecdsa-with-SHA512.
const OID_ECDSA_SHA512: &str = "1.2.840.10045.4.3.4";

/// SEC1 uncompressed point length for P-256 (`04 || X || Y`).
const P256_POINT_LEN: usize = 65;
/// SEC1 uncompressed point length for P-384.
const P384_POINT_LEN: usize = 97;

/// Validate a certificate chain from leaf to root, anchored on `integrated_time`.
///
/// - `leaf`: the signing certificate from the bundle
/// - `intermediates`: optional intermediate CA certificates (from bundle chain or trust root)
/// - `roots`: trusted Fulcio root CAs
/// - `integrated_time`: the verified Rekor integratedTime (UNIX seconds)
///
/// Returns the root certificate that validated the chain on success.
pub fn validate_chain(
    leaf: &Cert,
    intermediates: &[Cert],
    roots: &[Cert],
    integrated_time: i64,
) -> Result<Cert, SigstoreVerificationError> {
    // Validate leaf constraints
    leaf.validate_leaf()?;

    // Validate root CA constraints
    for root in roots {
        root.validate_ca()?;
    }

    // Validate intermediate CA constraints
    for intermediate in intermediates {
        intermediate.validate_ca()?;
    }

    // Check chain length against pathLen constraints
    if let Some(path_len) = leaf.path_len
        && intermediates.len() as u32 > path_len {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: format!(
                    "pathLen constraint violated: leaf allows {}, but {} intermediates",
                    path_len,
                    intermediates.len()
                ),
            });
        }

    // Check pathLen constraints on intermediate CAs
    for (i, intermediate) in intermediates.iter().enumerate() {
        if let Some(path_len) = intermediate.path_len {
            let remaining = (intermediates.len() - i - 1) as u32;
            if remaining > path_len {
                return Err(SigstoreVerificationError::CertificateChain {
                    reason: format!(
                        "intermediate CA pathLen constraint violated: allows {path_len}, but {remaining} certs after it"
                    ),
                });
            }
        }
    }

    // Build the chain: leaf → intermediates → root
    let chain: Vec<&Cert> = std::iter::once(leaf)
        .chain(intermediates.iter())
        .collect();

    // For each root, try to validate the entire chain
    let mut last_err: Option<SigstoreVerificationError> = None;
    for root in roots {
        match try_chain_to_root(&chain, root, integrated_time) {
            Ok(()) => return Ok(root.clone()),
            Err(e) => last_err = Some(e),
        }
    }

    Err(last_err.unwrap_or_else(|| {
        SigstoreVerificationError::CertificateChain {
            reason: "no trusted root validated the certificate chain".into(),
        }
    }))
}

/// Attempt to validate the chain against a specific root.
fn try_chain_to_root(
    chain: &[&Cert],
    root: &Cert,
    integrated_time: i64,
) -> Result<(), SigstoreVerificationError> {
    // Check validity of all certs at integrated_time
    for cert in chain {
        cert.check_validity(integrated_time)?;
    }
    root.check_validity(integrated_time)?;

    // Verify signatures up the chain
    for i in 0..chain.len() {
        let child = chain[i];
        let parent: &Cert = if i + 1 < chain.len() {
            chain[i + 1]
        } else {
            root
        };

        verify_cert_signature(child, parent)?;
    }

    Ok(())
}

/// Verify that `parent` signed `child`.
///
/// Checks issuer/subject DN match, then verifies the signature over
/// SHA-256(child.tbs_der) using the parent's public key.
fn verify_cert_signature(
    child: &Cert,
    parent: &Cert,
) -> Result<(), SigstoreVerificationError> {
    // Check that the child's issuer DN matches the parent's subject DN
    if child.issuer_dn != parent.subject_dn {
        return Err(SigstoreVerificationError::CertificateChain {
            reason: format!(
                "issuer/subject mismatch: child issuer '{}' != parent subject '{}'",
                child.issuer_dn, parent.subject_dn
            ),
        });
    }

    if child.signature_value.is_empty() {
        return Err(SigstoreVerificationError::CertificateChain {
            reason: "child certificate has no signature value".into(),
        });
    }

    // The digest is chosen by the child's signatureAlgorithm; the curve is the
    // signer's (parent's) key. Fulcio root + intermediate are P-384 / SHA-384;
    // synthetic test chains are P-256 / SHA-256.
    let digest = match child.signature_algorithm_oid.as_str() {
        OID_ECDSA_SHA256 => Sha256::digest(&child.tbs_der).to_vec(),
        OID_ECDSA_SHA384 => Sha384::digest(&child.tbs_der).to_vec(),
        OID_ECDSA_SHA512 => Sha512::digest(&child.tbs_der).to_vec(),
        other => {
            return Err(SigstoreVerificationError::UnsupportedAlgorithm {
                algorithm: format!("certificate signatureAlgorithm OID {other}"),
            });
        }
    };

    let verify = match parent.pubkey_bytes.len() {
        P256_POINT_LEN => verify_ecdsa_p256_prehashed,
        P384_POINT_LEN => verify_ecdsa_p384_prehashed,
        n => {
            return Err(SigstoreVerificationError::UnsupportedAlgorithm {
                algorithm: format!("issuer public key of {n} bytes (not P-256/P-384)"),
            });
        }
    };

    verify(&parent.pubkey_bytes, &digest, &child.signature_value).map_err(|_| {
        SigstoreVerificationError::CertificateChain {
            reason: "certificate signature verification failed".into(),
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::test_support::{LeafOpts, make_intermediate, make_leaf, make_root};

    /// A timestamp inside every synthetic cert's validity window.
    fn anchor(leaf: &Cert) -> i64 {
        i64::midpoint(leaf.not_before, leaf.not_after)
    }

    #[test]
    fn valid_leaf_to_root_chain_validates() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let root_cert = Cert::from_der(&root.der).expect("parse root");
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[], &[root_cert], it)
            .expect("a leaf correctly signed by the trusted root must validate");
    }

    #[test]
    fn valid_leaf_intermediate_root_chain_validates() {
        let root = make_root("fulcio-root");
        let inter = make_intermediate("fulcio-intermediate", None, &root);
        let leaf = make_leaf(&inter, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let inter_cert = Cert::from_der(&inter.der).expect("parse intermediate");
        let root_cert = Cert::from_der(&root.der).expect("parse root");
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[inter_cert], &[root_cert], it)
            .expect("leaf -> intermediate -> root must validate");
    }

    #[test]
    fn self_signed_leaf_not_chaining_to_root_rejected() {
        let attacker = make_root("attacker-root");
        let real_root = make_root("fulcio-root");
        let leaf = make_leaf(&attacker, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&real_root.der).unwrap();
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[], &[root_cert], it)
            .expect_err("leaf not chaining to a trusted root must be rejected");
    }

    #[test]
    fn wrong_root_rejected() {
        let root_a = make_root("root-a");
        let root_b = make_root("root-b");
        let leaf = make_leaf(&root_a, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_b_cert = Cert::from_der(&root_b.der).unwrap();
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[], &[root_b_cert], it)
            .expect_err("a different root must not validate the chain");
    }

    #[test]
    fn expired_leaf_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let it = leaf_cert.not_after + 10_000;
        let err = validate_chain(&leaf_cert, &[], &[root_cert], it)
            .expect_err("integratedTime past not_after must reject the leaf");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateExpired { .. }),
            "must be CertificateExpired, got {err:?}"
        );
    }

    #[test]
    fn leaf_missing_eku_rejected_before_signature() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(
            &root,
            &LeafOpts {
                code_signing_eku: false,
                ..LeafOpts::default()
            },
        );
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let it = anchor(&leaf_cert);
        let err = validate_chain(&leaf_cert, &[], &[root_cert], it)
            .expect_err("leaf without code-signing EKU must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from leaf validation, got {err:?}"
        );
    }
}
