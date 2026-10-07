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

use crate::cert::{Cert, EcCurve, SignatureAlgorithm};
use crate::crypto::{verify_ecdsa_p256_prehashed, verify_ecdsa_p384_prehashed};
use crate::error::SigstoreVerificationError;

/// Validate a certificate chain from leaf to root, anchored on `integrated_time`.
///
/// - `leaf`: the signing certificate from the bundle
/// - `intermediates`: optional intermediate CA certificates (from bundle chain or trust root)
/// - `roots`: trusted Fulcio root CAs
/// - `integrated_time`: the verified Rekor integratedTime (UNIX seconds)
///
/// Returns the leaf certificate's immediate issuer (the first intermediate
/// or root that signed it) on success.
pub(crate) fn validate_chain(
    leaf: &Cert,
    intermediates: &[Cert],
    roots: &[Cert],
    integrated_time: i64,
) -> Result<Cert, SigstoreVerificationError> {
    // Constraint checks. Only the intermediates that end up on the path matter
    // for the CA checks; validate them lazily during the walk instead.
    leaf.validate_leaf()?;
    leaf.check_validity(integrated_time)?;

    // Build the path from the leaf up to a trusted root, choosing each parent by
    // issuer/subject DN match + a verified signature. This selects the correct
    // issuer from the candidate pool (bundle-provided + trust-root intermediates)
    // rather than assuming the list is already the exact ordered path.
    let mut current = leaf;
    // `depth` = number of intermediate CAs already traversed below `current`.
    let mut depth: u32 = 0;
    let max_depth = u32::try_from(intermediates.len())
        .unwrap_or(u32::MAX)
        .saturating_add(1);
    // Track the first issuing cert (the leaf's immediate issuer).
    let mut leaf_issuer: Option<Cert> = None;

    loop {
        // Terminate: is `current` directly issued by a trusted root?
        if let Some(root) = roots.iter().find(|r| {
            r.subject_dn == current.issuer_dn && verify_cert_signature(current, r).is_ok()
        }) {
            root.validate_ca()?;
            root.check_validity(integrated_time)?;

            // RFC 5280 pathLenConstraint applies to the root too, not just
            // intermediates: it bounds how many subordinate CA certs may
            // follow it in the path. `depth` here is exactly that count —
            // the intermediates already traversed between the root and the
            // leaf, same quantity checked against each intermediate's own
            // path_len below.
            if let Some(path_len) = root.path_len
                && depth > path_len
            {
                return Err(SigstoreVerificationError::CertificateChain {
                    reason: format!(
                        "pathLen constraint violated: root allows {path_len} subordinate CA(s), but {depth} below it"
                    ),
                });
            }

            return Ok(leaf_issuer.unwrap_or_else(|| root.clone()));
        }

        // Otherwise step up through an intermediate that issued `current`.
        let parent = intermediates.iter().find(|i| {
            i.subject_dn == current.issuer_dn && verify_cert_signature(current, i).is_ok()
        });
        let Some(parent) = parent else {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: format!(
                    "no trusted path: nothing issues certificate with issuer DN '{}'",
                    current.issuer_dn
                ),
            });
        };

        parent.validate_ca()?;
        parent.check_validity(integrated_time)?;
        if leaf_issuer.is_none() {
            leaf_issuer = Some(parent.clone());
        }

        // RFC 5280 pathLenConstraint: an intermediate may have at most `path_len`
        // subordinate CA certs below it. `depth` counts intermediates already
        // traversed toward the leaf.
        if let Some(path_len) = parent.path_len
            && depth > path_len
        {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: format!(
                    "pathLen constraint violated: intermediate allows {path_len} subordinate CA(s), but {depth} below it"
                ),
            });
        }

        current = parent;
        depth += 1;
        if depth > max_depth {
            return Err(SigstoreVerificationError::CertificateChain {
                reason: "certificate chain exceeds maximum depth (possible loop)".into(),
            });
        }
    }
}

/// Verify that `parent` signed `child`.
///
/// Checks issuer/subject DN match, then verifies the signature over
/// `digest(child.tbs_der)` using the parent's public key. The digest is
/// selected from the child certificate's `signatureAlgorithm`
/// (`ecdsa-with-SHA256`, `ecdsa-with-SHA384`, or `ecdsa-with-SHA512`).
fn verify_cert_signature(child: &Cert, parent: &Cert) -> Result<(), SigstoreVerificationError> {
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
    let digest = match &child.signature_algorithm {
        SignatureAlgorithm::EcdsaSha256 => Sha256::digest(&child.tbs_der).to_vec(),
        SignatureAlgorithm::EcdsaSha384 => Sha384::digest(&child.tbs_der).to_vec(),
        SignatureAlgorithm::EcdsaSha512 => Sha512::digest(&child.tbs_der).to_vec(),
        SignatureAlgorithm::Other(oid) => {
            return Err(SigstoreVerificationError::UnsupportedAlgorithm {
                algorithm: format!("certificate signatureAlgorithm OID {oid}"),
            });
        },
    };

    let verify = match parent.curve {
        Some(EcCurve::P256) => verify_ecdsa_p256_prehashed,
        Some(EcCurve::P384) => verify_ecdsa_p384_prehashed,
        None => {
            return Err(SigstoreVerificationError::UnsupportedAlgorithm {
                algorithm: "issuer public key is not id-ecPublicKey on P-256/P-384".into(),
            });
        },
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
    use crate::test_support::{
        LeafOpts, make_intermediate, make_intermediate_p384, make_leaf, make_root,
        make_root_constrained, make_root_p384,
    };

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
    fn p384_leaf_intermediate_root_chain_validates() {
        // Real Fulcio root + intermediate are P-384 (ecdsa-with-SHA384); the
        // leaf's own key stays P-256 (make_leaf always uses keypair()), same
        // as production. Exercises verify_cert_signature's P-384 dispatch
        // for both chain links through an actual signature-verifying walk —
        // crypto.rs only unit-tests the P-384 primitive in isolation.
        let root = make_root_p384("fulcio-root-p384");
        let inter = make_intermediate_p384("fulcio-intermediate-p384", None, &root);
        let leaf = make_leaf(&inter, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let inter_cert = Cert::from_der(&inter.der).expect("parse intermediate");
        let root_cert = Cert::from_der(&root.der).expect("parse root");
        assert_eq!(
            inter_cert.curve,
            Some(EcCurve::P384),
            "intermediate must be recognized as P-384"
        );
        assert_eq!(
            root_cert.curve,
            Some(EcCurve::P384),
            "root must be recognized as P-384"
        );
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[inter_cert], &[root_cert], it)
            .expect("P-384 leaf -> intermediate -> root chain must validate");
    }

    #[test]
    fn self_signed_leaf_not_chaining_to_root_rejected() {
        let attacker = make_root("attacker-root");
        let real_root = make_root("fulcio-root");
        let leaf = make_leaf(&attacker, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let root_cert = Cert::from_der(&real_root.der).expect("parse root");
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[], &[root_cert], it)
            .expect_err("leaf not chaining to a trusted root must be rejected");
    }

    #[test]
    fn intermediate_selected_from_pool_regardless_of_order() {
        // Path builder must pick the correct issuer by DN even when the pool
        // contains unrelated intermediates in arbitrary order.
        let root = make_root("fulcio-root");
        let inter = make_intermediate("fulcio-intermediate", None, &root);
        let noise = make_intermediate("unrelated-intermediate", None, &make_root("other-root"));
        let leaf = make_leaf(&inter, &LeafOpts::default());

        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let inter_cert = Cert::from_der(&inter.der).expect("parse intermediate");
        let noise_cert = Cert::from_der(&noise.der).expect("parse unrelated intermediate");
        let root_cert = Cert::from_der(&root.der).expect("parse root");
        let it = anchor(&leaf_cert);

        // Noise first, real intermediate second — builder must still find the path.
        validate_chain(&leaf_cert, &[noise_cert, inter_cert], &[root_cert], it)
            .expect("path builder selects the correct issuer from the candidate pool");
    }

    #[test]
    fn wrong_root_rejected() {
        let root_a = make_root("root-a");
        let root_b = make_root("root-b");
        let leaf = make_leaf(&root_a, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).expect("parse leaf");
        let root_b_cert = Cert::from_der(&root_b.der).expect("parse root-b");
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
    fn root_path_len_zero_rejects_any_intermediate() {
        // Root's pathLenConstraint=0 means "no subordinate CA certs may
        // follow me" — a leaf chaining through one intermediate must be
        // rejected, even though the intermediate itself imposes no
        // constraint of its own.
        let root = make_root_constrained("fulcio-root", 0);
        let inter = make_intermediate("fulcio-intermediate", None, &root);
        let leaf = make_leaf(&inter, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let inter_cert = Cert::from_der(&inter.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let it = anchor(&leaf_cert);
        let err = validate_chain(&leaf_cert, &[inter_cert], &[root_cert], it)
            .expect_err("root pathLen=0 must reject a chain with an intermediate below it");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from root pathLen check, got {err:?}"
        );
    }

    #[test]
    fn root_path_len_one_allows_single_intermediate() {
        let root = make_root_constrained("fulcio-root", 1);
        let inter = make_intermediate("fulcio-intermediate", None, &root);
        let leaf = make_leaf(&inter, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let inter_cert = Cert::from_der(&inter.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let it = anchor(&leaf_cert);
        validate_chain(&leaf_cert, &[inter_cert], &[root_cert], it)
            .expect("root pathLen=1 must allow exactly one intermediate below it");
    }

    #[test]
    fn intermediate_path_len_zero_rejects_deeper_intermediate() {
        // Same as root_path_len_zero_rejects_any_intermediate, but the
        // pathLen=0 constraint is on the *intermediate* (chain.rs:104-108),
        // not the root (already covered separately at chain.rs:66-74).
        let root = make_root("fulcio-root");
        let inter_constrained = make_intermediate("fulcio-intermediate-pl0", Some(0), &root);
        let inter2 = make_intermediate("fulcio-intermediate-2", None, &inter_constrained);
        let leaf = make_leaf(&inter2, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let inter_constrained_cert = Cert::from_der(&inter_constrained.der).unwrap();
        let inter2_cert = Cert::from_der(&inter2.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let it = anchor(&leaf_cert);
        let err = validate_chain(
            &leaf_cert,
            &[inter_constrained_cert, inter2_cert],
            &[root_cert],
            it,
        )
        .expect_err("intermediate pathLen=0 must reject another intermediate below it");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from intermediate pathLen check, got {err:?}"
        );
    }

    #[test]
    fn chain_depth_exceeding_candidate_pool_rejected() {
        // A self-signed CA fed in as a candidate "intermediate" (not a
        // trusted root) is its own issuer/subject match, so the path
        // builder would loop on it forever without the max-depth guard
        // (chain.rs:113-116). With one candidate, max_depth = 2: the loop
        // revisits it a 3rd time and must bail out instead of looping.
        let looping_ca = make_root("looping-ca");
        let leaf = make_leaf(&looping_ca, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let looping_ca_cert = Cert::from_der(&looping_ca.der).unwrap();
        let it = anchor(&leaf_cert);
        let err = validate_chain(&leaf_cert, &[looping_ca_cert], &[], it)
            .expect_err("a self-referential candidate pool must not loop forever");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from the max-depth guard, got {err:?}"
        );
    }

    #[test]
    fn issuer_subject_dn_mismatch_rejected() {
        let root_a = make_root("root-a");
        let root_b = make_root("root-b");
        let leaf = make_leaf(&root_a, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_b_cert = Cert::from_der(&root_b.der).unwrap();
        let err = verify_cert_signature(&leaf_cert, &root_b_cert)
            .expect_err("issuer DN not matching parent subject DN must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from DN mismatch, got {err:?}"
        );
    }

    #[test]
    fn empty_child_signature_value_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let mut leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        leaf_cert.signature_value.clear();
        let err = verify_cert_signature(&leaf_cert, &root_cert)
            .expect_err("empty signature value must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from empty signature, got {err:?}"
        );
    }

    #[test]
    fn child_unsupported_signature_algorithm_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let mut leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        leaf_cert.signature_algorithm = SignatureAlgorithm::Other("1.2.840.113549.1.1.11".into());
        let err = verify_cert_signature(&leaf_cert, &root_cert)
            .expect_err("unsupported child signatureAlgorithm must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::UnsupportedAlgorithm { .. }),
            "must be UnsupportedAlgorithm from unrecognized OID, got {err:?}"
        );
    }

    #[test]
    fn parent_without_recognized_curve_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let mut root_cert = Cert::from_der(&root.der).unwrap();
        root_cert.curve = None;
        let err = verify_cert_signature(&leaf_cert, &root_cert)
            .expect_err("parent without a recognized EC curve must be rejected");
        assert!(
            matches!(err, SigstoreVerificationError::UnsupportedAlgorithm { .. }),
            "must be UnsupportedAlgorithm from missing curve, got {err:?}"
        );
    }

    #[test]
    fn tampered_child_signature_rejected() {
        let root = make_root("fulcio-root");
        let leaf = make_leaf(&root, &LeafOpts::default());
        let mut leaf_cert = Cert::from_der(&leaf.der).unwrap();
        let root_cert = Cert::from_der(&root.der).unwrap();
        let last = leaf_cert.signature_value.len() - 1;
        leaf_cert.signature_value[last] ^= 0xFF;
        let err = verify_cert_signature(&leaf_cert, &root_cert)
            .expect_err("a tampered signature must fail cryptographic verification");
        assert!(
            matches!(err, SigstoreVerificationError::CertificateChain { .. }),
            "must be CertificateChain from signature verification failure, got {err:?}"
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
