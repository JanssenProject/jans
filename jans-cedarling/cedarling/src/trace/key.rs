// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The producer's Ed25519 key and the `kid` Lock selects it by.

use jans_trace_core::Digest;
use jans_trace_core::signature::{Ed25519Signer, PrivateKeyError, SignerError, TraceSigner};

/// Signing key of this Cedarling instance with its `kid`.
///
/// Wraps [`Ed25519Signer`] because the default `kid` is the thumbprint of the
/// public key, which is only known after the key is parsed, while the core
/// signer takes its `kid` up front.
pub(crate) struct ProducerKey {
    kid: String,
    thumbprint: Digest,
    signer: Ed25519Signer,
}

impl ProducerKey {
    /// Parses an unencrypted PKCS#8 PEM key; no I/O, the caller reads the file.
    /// `kid` falls back to the thumbprint, the value an admin registers in Lock
    /// by default.
    pub(crate) fn from_pem(pem: &str, kid: Option<&str>) -> Result<Self, PrivateKeyError> {
        // `ProducerKey::kid` answers for the signer, so the inner `kid` is unused.
        let signer = Ed25519Signer::from_pkcs8_pem(String::new(), pem)?;
        let thumbprint = signer.public_key().thumbprint();
        let kid = kid.map_or_else(|| thumbprint.to_string(), str::to_owned);
        Ok(Self {
            kid,
            thumbprint,
            signer,
        })
    }

    /// RFC 7638 thumbprint of the public key, logged at startup for registration in Lock.
    pub(crate) fn thumbprint(&self) -> Digest {
        self.thumbprint
    }
}

impl TraceSigner for ProducerKey {
    fn kid(&self) -> &str {
        &self.kid
    }

    fn sign(&self, message: &[u8]) -> Result<[u8; 64], SignerError> {
        self.signer.sign(message)
    }
}

#[cfg(test)]
mod tests {
    use jans_trace_core::key::PublicKey;

    use super::*;

    /// RFC 8410 section 10.3 example key: public test vector, not a secret.
    const KEY_PEM: &str = include_str!("../../../test_files/trace/producer_ed25519.pem");
    const ENCRYPTED_PEM: &str = include_str!("../../../test_files/trace/encrypted_ed25519.pem");
    const RFC8410_PUBLIC_KEY: [u8; 32] = [
        0x19, 0xbf, 0x44, 0x09, 0x69, 0x84, 0xcd, 0xfe, 0x85, 0x41, 0xba, 0xc1, 0x67, 0xdc, 0x3b,
        0x96, 0xc8, 0x50, 0x86, 0xaa, 0x30, 0xb6, 0xb6, 0xcb, 0x0c, 0x5c, 0x38, 0xad, 0x70, 0x31,
        0x66, 0xe1,
    ];
    /// Body line of `KEY_PEM`; must never appear in an error.
    const KEY_BODY: &str = "MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp";

    fn rfc8410_thumbprint() -> Digest {
        PublicKey::from_bytes(&RFC8410_PUBLIC_KEY)
            .expect("RFC 8410 public key is valid")
            .thumbprint()
    }

    #[test]
    fn kid_defaults_to_thumbprint() {
        let key = ProducerKey::from_pem(KEY_PEM, None).expect("test key should parse");
        assert_eq!(
            key.kid(),
            key.thumbprint().to_string(),
            "kid should default to the thumbprint"
        );
        assert!(
            key.kid().starts_with("sha256:"),
            "thumbprint kid uses the TRACE digest form, got {}",
            key.kid()
        );
    }

    #[test]
    fn explicit_kid_is_used() {
        let key =
            ProducerKey::from_pem(KEY_PEM, Some("fleet-key-1")).expect("test key should parse");
        assert_eq!(key.kid(), "fleet-key-1", "explicit kid should be kept");
        assert_eq!(
            key.thumbprint(),
            rfc8410_thumbprint(),
            "an explicit kid does not change the thumbprint"
        );
    }

    #[test]
    fn thumbprint_matches_core() {
        let key = ProducerKey::from_pem(KEY_PEM, None).expect("test key should parse");
        assert_eq!(
            key.thumbprint(),
            rfc8410_thumbprint(),
            "thumbprint should be the RFC 7638 thumbprint of the RFC 8410 public key"
        );
    }

    #[test]
    fn garbage_is_invalid_key() {
        let err = ProducerKey::from_pem("not a key", None)
            .err()
            .expect("garbage should be rejected");
        assert_eq!(
            err.code(),
            jans_trace_core::ErrorCode::InvalidPrivateKey,
            "garbage should be an invalid private key"
        );
    }

    #[test]
    fn encrypted_pem_is_invalid_key() {
        let err = ProducerKey::from_pem(ENCRYPTED_PEM, None)
            .err()
            .expect("encrypted key should be rejected");
        assert_eq!(
            err.code(),
            jans_trace_core::ErrorCode::InvalidPrivateKey,
            "encrypted PKCS#8 should be an invalid private key"
        );
    }

    #[test]
    fn error_text_has_no_key_material() {
        // A real key body under a wrong label is rejected; the body must not leak.
        let pem = KEY_PEM.replace("PRIVATE KEY", "PUBLIC KEY");
        let err = ProducerKey::from_pem(&pem, None)
            .err()
            .expect("key under a wrong label should be rejected");
        let text = format!("{err} {err:?}");
        assert!(
            !text.contains(KEY_BODY),
            "error must not contain key material, got: {text}"
        );
    }

    #[test]
    fn signs_under_its_own_kid() {
        let key =
            ProducerKey::from_pem(KEY_PEM, Some("fleet-key-1")).expect("test key should parse");
        let signer: &dyn TraceSigner = &key;
        assert_eq!(
            signer.kid(),
            "fleet-key-1",
            "the trait reports the producer kid"
        );
        signer
            .sign(b"record")
            .expect("in-memory signer cannot fail");
    }
}
