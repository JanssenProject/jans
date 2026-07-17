// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! ECDSA P-256 signature verification.
//!
//! Verify-only — no signing, no RNG. Pure Rust, WASM-compatible.
//!
//! Single API:
//!
//! - [`verify_ecdsa_p256_prehashed`] — for pre-computed SHA-256 digests.
//!   Used by SET, cert-chain, SCT, and bundle signature verification.
//! - [`verify_ecdsa_p384_prehashed`] — for P-384 pre-computed SHA-384 digests.

use ecdsa::signature::hazmat::PrehashVerifier;
use p256::ecdsa::{DerSignature, Signature, VerifyingKey};

use crate::error::SigstoreVerificationError;

/// Verify an ECDSA P-256 signature over pre-computed SHA-256 digest bytes.
///
/// The `message` is `SHA-256(original_data)`. This is the path used by
/// SET, certificate chain, SCT, and bundle signature verification.
///
/// The public key must be in SEC1 uncompressed point format (65 bytes)
/// or compressed format (33 bytes). Signature can be DER (ASN.1) or
/// raw r||s (64 bytes).
pub fn verify_ecdsa_p256_prehashed(
    pubkey_bytes: &[u8],
    prehash: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    let verifying_key = VerifyingKey::from_sec1_bytes(pubkey_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid public key: {e}"),
        }
    })?;

    if let Ok(der_sig) = DerSignature::from_bytes(signature_bytes) {
        PrehashVerifier::verify_prehash(&verifying_key, prehash, &der_sig)
            .map_err(|e| SigstoreVerificationError::SignatureMismatch {
                reason: format!("ECDSA DER prehash verification failed: {e}"),
            })?;
        return Ok(());
    }

    let raw_sig = Signature::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid signature format: {e}"),
        }
    })?;
    PrehashVerifier::verify_prehash(&verifying_key, prehash, &raw_sig)
        .map_err(|e| SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA raw prehash verification failed: {e}"),
        })
}

/// SHA-256 of the P-256 `SubjectPublicKeyInfo` DER reconstructed from a SEC1
/// uncompressed point. This is how Sigstore derives tlog / CT log key IDs.
pub(crate) fn p256_key_id(sec1_point: &[u8]) -> [u8; 32] {
    use sha2::{Digest, Sha256};
    const P256_SPKI_PREFIX: &[u8] = &[
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08,
        0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
    ];
    let mut der = P256_SPKI_PREFIX.to_vec();
    der.extend_from_slice(sec1_point);
    Sha256::digest(&der).into()
}

/// Verify an ECDSA **P-384** signature over pre-computed SHA-384 digest bytes.
///
/// Used for Fulcio certificate-chain links: the root and intermediate CAs are
/// P-384 and sign with `ecdsa-with-SHA384`. The public key is a SEC1 point
/// (uncompressed = 97 bytes); the signature may be DER or raw `r||s` (96 bytes).
pub fn verify_ecdsa_p384_prehashed(
    pubkey_bytes: &[u8],
    prehash: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    use p384::ecdsa::{DerSignature as P384Der, Signature as P384Sig, VerifyingKey as P384Key};

    let verifying_key = P384Key::from_sec1_bytes(pubkey_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid P-384 public key: {e}"),
        }
    })?;

    if let Ok(der_sig) = P384Der::from_bytes(signature_bytes) {
        PrehashVerifier::verify_prehash(&verifying_key, prehash, &der_sig)
            .map_err(|e| SigstoreVerificationError::SignatureMismatch {
                reason: format!("ECDSA P-384 DER prehash verification failed: {e}"),
            })?;
        return Ok(());
    }

    let raw_sig = P384Sig::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid P-384 signature format: {e}"),
        }
    })?;
    PrehashVerifier::verify_prehash(&verifying_key, prehash, &raw_sig)
        .map_err(|e| SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA P-384 raw prehash verification failed: {e}"),
        })
}

#[cfg(test)]
mod tests {
    use super::*;
    use p256::ecdsa::{Signature, SigningKey, signature::Signer};
    use sha2::{Digest, Sha256};

    fn signer() -> (SigningKey, Vec<u8>) {
        let sk = SigningKey::from_slice(&[7u8; 32]).expect("key generation from fixed seed");
        let pk = sk.verifying_key().to_encoded_point(false).as_bytes().to_vec();
        (sk, pk)
    }

    #[test]
    fn verify_accepts_signature_over_prehashed_digest() {
        let (sk, pk) = signer();
        let msg = b"artifact contents";
        let digest: [u8; 32] = Sha256::digest(msg).into();
        // sign(msg) hashes the message internally, producing a signature
        // over SHA-256(msg). verify_prehashed should accept that same digest.
        let sig: Signature = sk.sign(msg);
        verify_ecdsa_p256_prehashed(&pk, &digest, sig.to_der().as_bytes())
            .expect("prehashed signature over correct digest must verify");
    }

    #[test]
    fn verify_prehashed_rejects_wrong_digest() {
        let (sk, pk) = signer();
        let sig: Signature = sk.sign(b"original");
        let wrong_digest: [u8; 32] = Sha256::digest(b"different").into();
        verify_ecdsa_p256_prehashed(&pk, &wrong_digest, sig.to_der().as_bytes())
            .expect_err("prehashed signature over wrong digest must be rejected");
    }

}
