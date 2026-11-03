// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Offline signer for downstream tests, behind the `test-support` feature.
//!
//! [`TestSigner`] owns a synthetic Fulcio root, Rekor key and CTFE key, and
//! signs artifacts into v0.3 bundles that verify under
//! [`TestSigner::trust_root`] — never under the static public-good root.
//!
//! ```rust
//! use sigstore_verifier::SigstoreBlobVerifier;
//! use sigstore_verifier::testing::TestSigner;
//!
//! let signer = TestSigner::new();
//! let bundle = signer.sign(b"artifact");
//! let verifier = SigstoreBlobVerifier::new(&signer.trust_root()).expect("valid trust root");
//! verifier
//!     .verify(b"artifact", &bundle, &TestSigner::policy())
//!     .expect("bundle verifies under the signer's trust root");
//! ```

use p256::ecdsa::SigningKey;

use crate::policy::{IdentityMatch, VerificationPolicy};
use crate::test_support::{
    LeafOpts, der_to_pem, ec_pub_pem, make_leaf_with_real_sct, make_root, message_signature_bundle,
    rekor_key_id,
};
use crate::trust_root::SigstoreTrustRootRaw;

/// Signs artifacts as a fixed Fulcio identity, logged in a synthetic Rekor.
pub struct TestSigner {
    root_der: Vec<u8>,
    rekor_sk: SigningKey,
    ctfe_sk: SigningKey,
    leaf_der: Vec<u8>,
    leaf_sk: SigningKey,
}

impl TestSigner {
    /// SAN of the signing certificate.
    pub const IDENTITY: &'static str =
        "https://github.com/acme/app/.github/workflows/release.yml@refs/tags/v1";

    /// OIDC issuer of the signing certificate.
    pub const ISSUER: &'static str = "https://token.actions.githubusercontent.com";

    /// Rekor `integratedTime` stamped on every bundle; inside the leaf's
    /// 2021–2025 validity window.
    pub const INTEGRATED_TIME: i64 = 1_700_000_000;

    /// Creates a signer with a fresh root CA and leaf certificate.
    ///
    /// # Panics
    ///
    /// Panics if the fixed test keys or generated certificates fail to build,
    /// which would be a bug in this module.
    #[must_use]
    pub fn new() -> Self {
        let root = make_root("test-fulcio-root");
        let ctfe_sk = SigningKey::from_slice(&[5u8; 32]).expect("fixed CTFE test key");
        let rekor_sk = SigningKey::from_slice(&[3u8; 32]).expect("fixed Rekor test key");
        let ctfe_log_id =
            crate::crypto::p256_key_id(ctfe_sk.verifying_key().to_encoded_point(false).as_bytes())
                .expect("P-256 uncompressed point is 65 bytes");
        let opts = LeafOpts {
            san_uri: Some(Self::IDENTITY),
            oidc_issuer: Some(Self::ISSUER),
            ..LeafOpts::default()
        };
        let integrated_time =
            u64::try_from(Self::INTEGRATED_TIME).expect("INTEGRATED_TIME is positive");
        let (leaf, leaf_sk) =
            make_leaf_with_real_sct(&root, &opts, &ctfe_sk, &ctfe_log_id, integrated_time);
        Self {
            root_der: root.der,
            rekor_sk,
            ctfe_sk,
            leaf_der: leaf.der,
            leaf_sk,
        }
    }

    /// Trust root under which this signer's bundles verify.
    #[must_use]
    pub fn trust_root(&self) -> SigstoreTrustRootRaw {
        SigstoreTrustRootRaw {
            fulcio_root_certs: vec![der_to_pem(&self.root_der).into_bytes()],
            fulcio_intermediate_certs: vec![],
            rekor_keys: vec![ec_pub_pem(self.rekor_sk.verifying_key()).into_bytes()],
            ctfe_keys: vec![ec_pub_pem(self.ctfe_sk.verifying_key()).into_bytes()],
        }
    }

    /// Policy matching [`Self::IDENTITY`] and [`Self::ISSUER`] exactly.
    #[must_use]
    pub fn policy() -> VerificationPolicy {
        VerificationPolicy {
            cert_identity: IdentityMatch::Exact(Self::IDENTITY.to_string()),
            cert_issuer: Self::ISSUER.to_string(),
        }
    }

    /// Signs `artifact` and returns the bundle JSON, as `cosign sign-blob --bundle` writes it.
    #[must_use]
    pub fn sign(&self, artifact: &[u8]) -> Vec<u8> {
        message_signature_bundle(
            artifact,
            &self.leaf_der,
            &self.leaf_sk,
            &self.rekor_sk,
            &self.rekor_sk,
            &rekor_key_id(&self.rekor_sk),
            Self::INTEGRATED_TIME,
        )
    }
}

impl Default for TestSigner {
    fn default() -> Self {
        Self::new()
    }
}
