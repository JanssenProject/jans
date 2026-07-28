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
pub(crate) fn verify_ecdsa_p256_prehashed(
    pubkey_bytes: &[u8],
    prehash: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    let verifying_key = VerifyingKey::from_sec1_bytes(pubkey_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid public key: {e}"),
        }
    })?;

    if let Ok(der_sig) = DerSignature::from_bytes(signature_bytes)
        && PrehashVerifier::verify_prehash(&verifying_key, prehash, &der_sig).is_ok()
    {
        return Ok(());
    }

    let raw_sig = Signature::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid signature format: {e}"),
        }
    })?;
    PrehashVerifier::verify_prehash(&verifying_key, prehash, &raw_sig).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA raw prehash verification failed: {e}"),
        }
    })
}

/// Compute the SHA-256 SPKI fingerprint (key ID) of a P-256 public key.
/// This is how Sigstore derives tlog / CT log key IDs.
///
/// `sec1_point` must be an uncompressed SEC1 point (65 bytes).
pub(crate) fn p256_key_id(sec1_point: &[u8]) -> Result<[u8; 32], SigstoreVerificationError> {
    use sha2::{Digest, Sha256};
    const P256_SPKI_PREFIX: &[u8] = &[
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08,
        0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
    ];
    if sec1_point.len() != 65 {
        return Err(SigstoreVerificationError::SignatureMismatch {
            reason: format!(
                "SEC1 uncompressed P-256 point must be 65 bytes, got {}",
                sec1_point.len()
            ),
        });
    }
    let mut der = P256_SPKI_PREFIX.to_vec();
    der.extend_from_slice(sec1_point);
    Ok(Sha256::digest(&der).into())
}

/// Verify an ECDSA **P-384** signature over pre-computed SHA-384 digest bytes.
///
/// Used for Fulcio certificate-chain links: the root and intermediate CAs are
/// P-384 and sign with `ecdsa-with-SHA384`. The public key is a SEC1 point
/// (uncompressed = 97 bytes); the signature may be DER or raw `r||s` (96 bytes).
pub(crate) fn verify_ecdsa_p384_prehashed(
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

    if let Ok(der_sig) = P384Der::from_bytes(signature_bytes)
        && PrehashVerifier::verify_prehash(&verifying_key, prehash, &der_sig).is_ok()
    {
        return Ok(());
    }

    let raw_sig = P384Sig::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid P-384 signature format: {e}"),
        }
    })?;
    PrehashVerifier::verify_prehash(&verifying_key, prehash, &raw_sig).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA P-384 raw prehash verification failed: {e}"),
        }
    })
}

#[cfg(test)]
mod tests {
    use p256::ecdsa::{Signature, SigningKey, signature::Signer};
    use sha2::{Digest, Sha256, Sha384};

    use super::*;

    fn signer() -> (SigningKey, Vec<u8>) {
        let sk = SigningKey::from_slice(&[7u8; 32]).expect("key generation from fixed seed");
        let pk = sk
            .verifying_key()
            .to_encoded_point(false)
            .as_bytes()
            .to_vec();
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

    use p384::ecdsa::{Signature as P384Signature, SigningKey as P384SigningKey};

    fn p384_signer() -> (P384SigningKey, Vec<u8>) {
        let sk = P384SigningKey::from_slice(&[7u8; 48])
            .expect("P-384 key generation from fixed seed");
        let pk = sk
            .verifying_key()
            .to_encoded_point(false)
            .as_bytes()
            .to_vec();
        (sk, pk)
    }

    #[test]
    fn verify_p384_accepts_der_signature() {
        let (sk, pk) = p384_signer();
        let msg = b"artifact contents";
        let digest: [u8; 48] = Sha384::digest(msg).into();
        let sig: P384Signature = sk.sign(msg);
        verify_ecdsa_p384_prehashed(&pk, &digest, sig.to_der().as_bytes())
            .expect("prehashed DER signature over correct digest must verify");
    }

    #[test]
    fn verify_p384_accepts_raw_signature() {
        let (sk, pk) = p384_signer();
        let msg = b"artifact contents";
        let digest: [u8; 48] = Sha384::digest(msg).into();
        let sig: P384Signature = sk.sign(msg);
        verify_ecdsa_p384_prehashed(&pk, &digest, &sig.to_bytes())
            .expect("prehashed raw r||s signature over correct digest must verify");
    }

    #[test]
    fn verify_p384_rejects_wrong_digest() {
        let (sk, pk) = p384_signer();
        let sig: P384Signature = sk.sign(b"original");
        let wrong_digest: [u8; 48] = Sha384::digest(b"different").into();
        verify_ecdsa_p384_prehashed(&pk, &wrong_digest, sig.to_der().as_bytes())
            .expect_err("prehashed signature over wrong digest must be rejected");
    }

    #[test]
    fn verify_p384_rejects_corrupted_signature() {
        let (sk, pk) = p384_signer();
        let digest: [u8; 48] = Sha384::digest(b"artifact contents").into();
        let sig: P384Signature = sk.sign(b"artifact contents");
        let mut sig = sig.to_bytes();
        sig[0] ^= 0xff;
        verify_ecdsa_p384_prehashed(&pk, &digest, &sig)
            .expect_err("corrupted signature must be rejected");
    }
}
