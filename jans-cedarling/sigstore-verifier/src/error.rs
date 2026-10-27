// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

/// Errors returned by the Sigstore verification process.
#[derive(Debug, thiserror::Error)]
pub enum SigstoreVerificationError {
    /// The bundle JSON could not be parsed or has an unknown format.
    #[error("bundle parsing failed: {source}")]
    BundleParsing {
        #[source]
        source: serde_json::Error,
    },

    /// The bundle is missing required fields or has an unsupported media type.
    #[error("invalid bundle format: {reason}")]
    InvalidBundleFormat { reason: String },

    /// The X.509 certificate could not be parsed from DER/PEM.
    #[error("certificate parsing failed: {reason}")]
    CertificateParsing { reason: String },

    /// The certificate chain could not be validated to a trusted root.
    #[error("certificate chain validation failed: {reason}")]
    CertificateChain { reason: String },

    /// The SCT (Signed Certificate Timestamp) verification failed.
    #[error("SCT verification failed: {reason}")]
    SctVerification { reason: String },

    /// The bundle identity does not match the verification policy.
    #[error("policy violation: {reason}")]
    PolicyViolation { reason: String },

    /// The artifact signature does not verify against the certificate's public key.
    #[error("signature mismatch: {reason}")]
    SignatureMismatch { reason: String },

    /// The Rekor log entry data is malformed, missing an expected field, of an
    /// unrecognized/unsupported shape, or otherwise fails to parse — distinct
    /// from [`RekorInconsistency`](Self::RekorInconsistency): this is a
    /// structural problem with the data itself, not a well-formed value that
    /// fails a cryptographic or equality check against another well-formed
    /// value. Retrying with different input (e.g. a differently-shaped
    /// bundle) may help; a caller should generally treat this as "reject the
    /// input", not "possible tampering".
    #[error("Rekor entry malformed: {reason}")]
    RekorMalformed { reason: String },

    /// The Rekor log entry body is well-formed but inconsistent with the
    /// certificate, signature, or artifact hash (see CVE-2022-36056), or a
    /// checkpoint/inclusion-proof value that should cryptographically match
    /// another independently-derived value doesn't. Unlike
    /// [`RekorMalformed`](Self::RekorMalformed), this is the "possible
    /// tampering" signal — the data parsed fine, but a check that must hold
    /// for a genuine, untampered bundle failed.
    #[error("Rekor entry inconsistency: {reason}")]
    RekorInconsistency { reason: String },

    /// The SET (Signed Entry Timestamp) verification against the Rekor key failed.
    #[error("SET verification failed: {reason}")]
    SetVerification { reason: String },

    /// The signing certificate was expired or not yet valid at the time of signing.
    #[error("certificate expired or not yet valid: {reason}")]
    CertificateExpired { reason: String },

    /// The signature or certificate uses an unsupported algorithm.
    #[error("unsupported algorithm: {algorithm}")]
    UnsupportedAlgorithm { algorithm: String },
}
