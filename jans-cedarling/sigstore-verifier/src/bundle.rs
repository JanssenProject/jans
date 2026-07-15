// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore bundle JSON deserialization.
//!
//! Supports both the protobuf-based Sigstore bundle format (v0.1–v0.3 JSON) and
//! the legacy cosign `RekorBundle` format.

use serde::Deserialize;

use crate::error::SigstoreVerificationError;

/// Supported Sigstore bundle media types.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum BundleVersion {
    /// `application/vnd.dev.sigstore.bundle+json;version=0.1`
    Bundle0_1,
    /// `application/vnd.dev.sigstore.bundle+json;version=0.2`
    Bundle0_2,
    /// `application/vnd.dev.sigstore.bundle+json;version=0.3` or
    /// `application/vnd.dev.sigstore.bundle.v0.3+json` (both denote v0.3).
    Bundle0_3,
}

impl BundleVersion {
    #[must_use]
    pub fn from_media_type(s: &str) -> Option<Self> {
        match s {
            "application/vnd.dev.sigstore.bundle+json;version=0.1" => Some(Self::Bundle0_1),
            "application/vnd.dev.sigstore.bundle+json;version=0.2" => Some(Self::Bundle0_2),
            "application/vnd.dev.sigstore.bundle+json;version=0.3"
            | "application/vnd.dev.sigstore.bundle.v0.3+json" => Some(Self::Bundle0_3),
            _ => None,
        }
    }
}

/// A parsed Sigstore protobuf bundle (v0.1–v0.3 JSON format).
#[derive(Debug, Clone, Deserialize)]
pub struct Bundle {
    /// The bundle media type (e.g., `application/vnd.dev.sigstore.bundle.v0.3+json`).
    #[serde(rename = "mediaType")]
    pub media_type: String,

    /// The verification material (certificate + tlog entries).
    #[serde(rename = "verificationMaterial")]
    pub verification_material: VerificationMaterial,

    /// The signed content.
    #[serde(flatten)]
    pub content: BundleContent,
}

/// The verification material within a Sigstore bundle.
#[derive(Debug, Clone, Deserialize)]
pub struct VerificationMaterial {
    /// The signing certificate in DER form (base64-encoded).
    pub certificate: Option<CertificateEntry>,

    /// Optional chain of additional certificates.
    #[serde(rename = "x509CertificateChain")]
    pub x509_certificate_chain: Option<CertificateChainEntry>,

    /// Rekor transparency log entries.
    #[serde(rename = "tlogEntries")]
    pub tlog_entries: Vec<TlogEntry>,
}

/// A single certificate entry (raw DER, base64-encoded).
#[derive(Debug, Clone, Deserialize)]
pub struct CertificateEntry {
    /// Base64-encoded DER certificate bytes.
    #[serde(rename = "rawBytes")]
    pub raw_bytes: String,
}

/// A certificate chain entry.
#[derive(Debug, Clone, Deserialize)]
pub struct CertificateChainEntry {
    /// Base64-encoded DER certificates, root-first or leaf-first.
    pub certificates: Vec<CertificateEntry>,
}

/// A transparency log entry from the bundle.
#[derive(Debug, Clone, Deserialize)]
pub struct TlogEntry {
    /// The index of the log entry in the transparency log.
    #[serde(rename = "logIndex")]
    pub log_index: String,

    /// The log identifier (SHA-256 of the DER-encoded Rekor public key).
    #[serde(rename = "logId")]
    pub log_id: LogId,

    /// The kind and version of the entry (e.g., `hashedrekord` v0.0.1).
    #[serde(rename = "kindVersion")]
    pub kind_version: KindVersion,

    /// The UNIX timestamp when the entry was integrated into the log.
    #[serde(rename = "integratedTime")]
    pub integrated_time: String,

    /// The inclusion promise containing the Signed Entry Timestamp (SET).
    #[serde(rename = "inclusionPromise")]
    pub inclusion_promise: Option<InclusionPromise>,

    /// The inclusion proof (Merkle proof).
    #[serde(rename = "inclusionProof")]
    pub inclusion_proof: Option<InclusionProof>,

    /// The canonicalized body of the log entry (base64-encoded JSON bytes).
    #[serde(rename = "canonicalizedBody")]
    pub canonicalized_body: Option<String>,
}

/// The log ID (SHA-256 of the DER-encoded Rekor public key).
#[derive(Debug, Clone, Deserialize)]
pub struct LogId {
    /// Base64-encoded key ID.
    #[serde(rename = "keyId")]
    pub key_id: String,
}

/// The kind and version of a tlog entry.
#[derive(Debug, Clone, Deserialize)]
pub struct KindVersion {
    /// The entry kind (e.g., `hashedrekord`, `dsse`).
    pub kind: String,

    /// The entry version (e.g., `0.0.1`).
    pub version: String,
}

/// The inclusion promise containing the Signed Entry Timestamp.
#[derive(Debug, Clone, Deserialize)]
pub struct InclusionPromise {
    /// Base64-encoded SET signature over the canonicalized body.
    #[serde(rename = "signedEntryTimestamp")]
    pub signed_entry_timestamp: String,
}

/// A Merkle inclusion proof.
#[derive(Debug, Clone, Deserialize)]
pub struct InclusionProof {
    /// The log index of the proof checkpoint.
    #[serde(rename = "logIndex")]
    pub log_index: String,

    /// The Merkle root hash (base64-encoded).
    #[serde(rename = "rootHash")]
    pub root_hash: String,

    /// The tree size at the time of the proof.
    #[serde(rename = "treeSize")]
    pub tree_size: String,

    /// The ordered hashes forming the Merkle audit path.
    pub hashes: Vec<String>,

    /// The signed checkpoint.
    pub checkpoint: Option<Checkpoint>,
}

/// A signed checkpoint from the transparency log.
#[derive(Debug, Clone, Deserialize)]
pub struct Checkpoint {
    /// The raw checkpoint envelope.
    pub envelope: String,
}

/// The content of a Sigstore bundle.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum BundleContent {
    /// A simple message signature (the `cosign sign-blob` case).
    #[serde(rename = "messageSignature")]
    MessageSignature {
        /// The digest of the artifact.
        #[serde(rename = "messageDigest")]
        message_digest: Option<MessageDigest>,

        /// Base64-encoded signature bytes.
        signature: String,
    },

    /// A DSSE envelope (in-toto attestation).
    #[serde(rename = "dsseEnvelope")]
    DsseEnvelope {
        /// Base64-encoded payload.
        payload: String,

        /// The payload type (e.g., `application/vnd.in-toto+json`).
        #[serde(rename = "payloadType")]
        payload_type: String,

        /// The signatures within the envelope.
        signatures: Vec<DsseSignature>,
    },
}

/// A message digest within a `MessageSignature`.
#[derive(Debug, Clone, Deserialize)]
pub struct MessageDigest {
    /// The hash algorithm (e.g., `SHA2_256`).
    pub algorithm: String,

    /// The hex-encoded digest value.
    pub digest: String,
}

/// A signature within a DSSE envelope.
#[derive(Debug, Clone, Deserialize)]
pub struct DsseSignature {
    /// Base64-encoded signature bytes.
    pub sig: String,
}

// ── Legacy cosign RekorBundle format ──────────────────────────────────────────

/// Legacy cosign `RekorBundle` format.
///
/// This is the format produced by `cosign sign-blob --bundle`.
/// It contains the SET and payload but not the certificate or signature,
/// which are provided separately.
#[derive(Debug, Clone, Deserialize)]
pub struct LegacyRekorBundle {
    /// Base64-encoded SET signature.
    #[serde(rename = "SignedEntryTimestamp")]
    pub signed_entry_timestamp: String,

    /// The Rekor payload.
    #[serde(rename = "Payload")]
    pub payload: LegacyRekorPayload,
}

/// The payload within a legacy `RekorBundle`.
#[derive(Debug, Clone, Deserialize)]
pub struct LegacyRekorPayload {
    /// Base64-encoded tlog entry body (JSON).
    pub body: String,

    /// The UNIX timestamp when the entry was integrated.
    #[serde(rename = "integratedTime")]
    pub integrated_time: i64,

    /// The index of the log entry.
    #[serde(rename = "logIndex")]
    pub log_index: i64,

    /// The hex-encoded log ID.
    #[serde(rename = "logID")]
    pub log_id: String,
}

// ── Parsing ───────────────────────────────────────────────────────────────────

/// Result of parsing a bundle JSON. Detects format automatically.
pub enum ParsedBundle {
    /// A protobuf-based Sigstore bundle (v0.1–v0.3).
    Sigstore(Bundle),
    /// A legacy cosign `RekorBundle`.
    Legacy(LegacyRekorBundle),
}

impl ParsedBundle {
    /// Parse bundle JSON, auto-detecting the format.
    ///
    /// Tries Sigstore bundle format first (keyed on `mediaType`),
    /// then falls back to legacy `RekorBundle` format.
    pub fn from_json(json: &[u8]) -> Result<Self, SigstoreVerificationError> {
        // Try Sigstore bundle format first via mediaType detection
        if let Ok(bundle) = serde_json::from_slice::<Bundle>(json)
            && BundleVersion::from_media_type(&bundle.media_type).is_some() {
                return Ok(Self::Sigstore(bundle));
            }

        // Try legacy RekorBundle format
        let legacy: LegacyRekorBundle = serde_json::from_slice(json).map_err(|e| {
            SigstoreVerificationError::BundleParsing { source: e }
        })?;
        Ok(Self::Legacy(legacy))
    }

    /// Returns the certificate raw bytes (base64-encoded DER) from the bundle.
    #[must_use] 
    pub fn certificate_base64(&self) -> Option<&str> {
        match self {
            Self::Sigstore(bundle) => bundle.verification_material.certificate
                .as_ref()
                .map(|c| c.raw_bytes.as_str())
                .or_else(|| {
                    bundle.verification_material.x509_certificate_chain
                        .as_ref()
                        .and_then(|chain| chain.certificates.first())
                        .map(|c| c.raw_bytes.as_str())
                }),
            Self::Legacy(_) => {
                // Legacy bundles don't contain a cert — caller provides it separately
                None
            }
        }
    }

    /// Returns the intermediate certificates (base64 DER) carried in the
    /// bundle's `x509CertificateChain`, if any.
    ///
    /// For v0.1/v0.2 bundles the chain is `[leaf, intermediate...]`, so the
    /// leaf (index 0) is excluded here. v0.3 bundles use a single `certificate`
    /// and carry no intermediates (the verifier uses the trust root's).
    #[must_use]
    pub fn intermediate_certificates_base64(&self) -> Vec<&str> {
        match self {
            Self::Sigstore(bundle) => bundle
                .verification_material
                .x509_certificate_chain
                .as_ref()
                .map(|chain| {
                    chain
                        .certificates
                        .iter()
                        .skip(1)
                        .map(|c| c.raw_bytes.as_str())
                        .collect()
                })
                .unwrap_or_default(),
            Self::Legacy(_) => Vec::new(),
        }
    }

    /// Returns the signature (base64-encoded) from the bundle.
    #[must_use]
    pub fn signature_base64(&self) -> Option<&str> {
        match self {
            Self::Sigstore(bundle) => match &bundle.content {
                BundleContent::MessageSignature { signature, .. } => Some(signature.as_str()),
                BundleContent::DsseEnvelope { signatures, .. } => {
                    signatures.first().map(|s| s.sig.as_str())
                }
            },
            Self::Legacy(_) => {
                // Legacy bundles don't contain a signature — caller provides it separately
                None
            }
        }
    }

    /// Returns the tlog entry for Rekor verification.
    #[must_use] 
    pub fn tlog_entry(&self) -> Option<&TlogEntry> {
        match self {
            Self::Sigstore(bundle) => bundle.verification_material.tlog_entries.first(),
            Self::Legacy(_) => None,
        }
    }

    /// Returns the bundle version for Sigstore bundles.
    #[must_use] 
    pub fn bundle_version(&self) -> Option<BundleVersion> {
        match self {
            Self::Sigstore(bundle) => BundleVersion::from_media_type(&bundle.media_type),
            Self::Legacy(_) => None,
        }
    }
}

impl Bundle {
    /// Parse a Sigstore bundle from JSON bytes.
    pub fn from_json(json: &[u8]) -> Result<Self, SigstoreVerificationError> {
        let bundle: Bundle = serde_json::from_slice(json).map_err(|e| {
            SigstoreVerificationError::BundleParsing { source: e }
        })?;
        if BundleVersion::from_media_type(&bundle.media_type).is_none() {
            return Err(SigstoreVerificationError::InvalidBundleFormat {
                reason: format!("unsupported media type: {}", bundle.media_type),
            });
        }
        Ok(bundle)
    }
}

impl LegacyRekorBundle {
    /// Parse a legacy cosign `RekorBundle` from JSON bytes.
    pub fn from_json(json: &[u8]) -> Result<Self, SigstoreVerificationError> {
        serde_json::from_slice(json).map_err(|e| SigstoreVerificationError::BundleParsing {
            source: e,
        })
    }
}
