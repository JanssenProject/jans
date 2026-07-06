// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! ECDSA P-256 signature verification.
//!
//! Verify-only — no signing, no RNG. Pure Rust, WASM-compatible.

use ecdsa::signature::Verifier;
use p256::ecdsa::{DerSignature, Signature, VerifyingKey};

pub use p256::ecdsa::VerifyingKey as P256VerifyingKey;

use crate::error::SigstoreVerificationError;

/// Verify an ECDSA P-256 signature over raw message bytes.
///
/// The public key must be in SEC1 uncompressed point format (65 bytes)
/// or compressed format (33 bytes).
pub fn verify_ecdsa_p256_raw(
    pubkey_bytes: &[u8],
    message: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    let verifying_key = VerifyingKey::from_sec1_bytes(pubkey_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid public key: {e}"),
        }
    })?;

    // Try DER signature first (ASN.1 encoded)
    if let Ok(der_sig) = DerSignature::from_bytes(signature_bytes) {
        verifying_key.verify(message, &der_sig).map_err(|e| {
            SigstoreVerificationError::SignatureMismatch {
                reason: format!("ECDSA DER verification failed: {e}"),
            }
        })?;
        return Ok(());
    }

    // Try raw fixed-size signature (r || s, 64 bytes)
    let raw_sig = Signature::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid signature format: {e}"),
        }
    })?;
    verifying_key.verify(message, &raw_sig).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA raw verification failed: {e}"),
        }
    })
}

/// Verify an ECDSA P-256 signature over a pre-computed message.
///
/// The signature is over `SHA-256(message)` for prehash verification.
/// This is used for both `MessageSignature` and DSSE bundles.
pub fn verify_ecdsa_p256_prehash(
    pubkey_bytes: &[u8],
    message: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    verify_ecdsa_p256(pubkey_bytes, message, signature_bytes)
}

/// Verify a signature with flexible format detection.
///
/// Tries both DER (ASN.1) and raw (r||s) format automatically.
pub fn verify_ecdsa_p256(
    pubkey_bytes: &[u8],
    message: &[u8],
    signature_bytes: &[u8],
) -> Result<(), SigstoreVerificationError> {
    let verifying_key = VerifyingKey::from_sec1_bytes(pubkey_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid public key: {e}"),
        }
    })?;

    // Try DER format first
    if let Ok(der_sig) = DerSignature::from_bytes(signature_bytes) {
        return verifying_key.verify(message, &der_sig).map_err(|e| {
            SigstoreVerificationError::SignatureMismatch {
                reason: format!("ECDSA verification failed: {e}"),
            }
        });
    }

    // Try raw format
    let raw_sig = Signature::from_slice(signature_bytes).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("invalid signature format: {e}"),
        }
    })?;
    verifying_key.verify(message, &raw_sig).map_err(|e| {
        SigstoreVerificationError::SignatureMismatch {
            reason: format!("ECDSA verification failed: {e}"),
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use p256::ecdsa::{Signature, SigningKey, signature::Signer};
    use sha2::{Digest, Sha256};

    /// Fixed-key signer — `from_slice` avoids any RNG dependency.
    fn signer() -> (SigningKey, Vec<u8>) {
        let sk = SigningKey::from_slice(&[7u8; 32]).expect("key");
        let pk = sk.verifying_key().to_encoded_point(false).as_bytes().to_vec();
        (sk, pk)
    }

    #[test]
    fn verify_over_raw_message_succeeds() {
        // Sanity: the primitive itself works when handed the raw message.
        let (sk, pk) = signer();
        let msg = b"artifact contents";
        let sig: Signature = sk.sign(msg);
        assert!(verify_ecdsa_p256(&pk, msg, sig.to_der().as_bytes()).is_ok());
    }

    #[test]
    fn verify_accepts_signature_over_prehashed_digest() {
        // Every caller (SET, cert-chain, SCT, MessageSignature, DSSE) passes an
        // ALREADY-SHA-256'd digest as `message`. The ECDSA signature they check
        // has that exact digest as its signed prehash, so this MUST succeed.
        //
        // Currently FAILS: `verify_ecdsa_p256` calls `Verifier::verify`, which
        // hashes `message` a second time (SHA-256(digest)) — the double-hash bug.
        let (sk, pk) = signer();
        let msg = b"artifact contents";
        let digest: [u8; 32] = Sha256::digest(msg).into();
        let sig: Signature = sk.sign(msg); // signed prehash == SHA-256(msg) == digest
        let result = verify_ecdsa_p256(&pk, &digest, sig.to_der().as_bytes());
        assert!(
            result.is_ok(),
            "verify_ecdsa_p256 double-hashes: it rejects a valid signature when given \
             the prehash digest that every caller passes. Use PrehashVerifier::verify_prehash."
        );
    }

    #[test]
    fn verify_accepts_raw_fixed_size_signature() {
        // Rekor/cosign often emit raw r||s (64-byte) signatures, not DER.
        let (sk, pk) = signer();
        let msg = b"artifact contents";
        let sig: Signature = sk.sign(msg);
        assert!(verify_ecdsa_p256(&pk, msg, &sig.to_bytes()).is_ok());
    }

    #[test]
    fn verify_rejects_wrong_key() {
        let (sk, _) = signer();
        let other = SigningKey::from_slice(&[9u8; 32]).unwrap();
        let other_pk = other.verifying_key().to_encoded_point(false).as_bytes().to_vec();
        let sig: Signature = sk.sign(b"msg");
        assert!(verify_ecdsa_p256(&other_pk, b"msg", sig.to_der().as_bytes()).is_err());
    }

    #[test]
    fn verify_rejects_tampered_message() {
        let (sk, pk) = signer();
        let sig: Signature = sk.sign(b"original");
        assert!(verify_ecdsa_p256(&pk, b"tampered", sig.to_der().as_bytes()).is_err());
    }

    #[test]
    fn verify_rejects_empty_signature() {
        let (_, pk) = signer();
        assert!(verify_ecdsa_p256(&pk, b"msg", &[]).is_err());
    }
}
