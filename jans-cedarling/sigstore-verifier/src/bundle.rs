// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore bundle JSON deserialization.
//!
//! Supports Sigstore protobuf-based bundle format (v0.1–v0.3 JSON).

use serde::Deserialize;

use crate::error::SigstoreVerificationError;

/// Supported Sigstore bundle media types.
#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub(crate) enum BundleVersion {
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
    pub(crate) fn from_media_type(s: &str) -> Option<Self> {
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
///
/// Built only through [`Bundle::from_json`], which resolves the wire form's
/// two optional content fields into exactly one [`BundleContent`].
#[derive(Debug, Clone)]
pub(crate) struct Bundle {
    /// The bundle media type (e.g., `application/vnd.dev.sigstore.bundle.v0.3+json`).
    pub(crate) media_type: String,

    /// The verification material (certificate + tlog entries).
    pub(crate) verification_material: VerificationMaterial,

    /// The signed content.
    pub(crate) content: BundleContent,
}

/// The wire form of a [`Bundle`].
///
/// `messageSignature` and `dsseEnvelope` are a protobuf `oneof` in
/// `sigstore_bundle.proto`, so exactly one may be present. They are
/// deserialized as two independent `Option`s — rather than one
/// `#[serde(flatten)]` enum — because a flattened externally-tagged enum
/// stops at the first variant key it finds and drops the rest: a bundle
/// carrying both would parse, with JSON key order silently deciding which
/// content got authenticated and the other never even checked for
/// consistency. Keeping both fields visible lets [`Bundle::from_json`]
/// reject that, and the "neither present" case, explicitly.
#[derive(Debug, Deserialize)]
struct RawBundle {
    #[serde(rename = "mediaType")]
    media_type: String,

    #[serde(rename = "verificationMaterial")]
    verification_material: VerificationMaterial,

    #[serde(rename = "messageSignature")]
    message_signature: Option<MessageSignatureContent>,

    #[serde(rename = "dsseEnvelope")]
    dsse_envelope: Option<DsseEnvelopeContent>,
}

/// The verification material within a Sigstore bundle.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct VerificationMaterial {
    /// The signing certificate in DER form (base64-encoded).
    pub(crate) certificate: Option<CertificateEntry>,

    /// Optional chain of additional certificates.
    #[serde(rename = "x509CertificateChain")]
    pub(crate) x509_certificate_chain: Option<CertificateChainEntry>,

    /// Rekor transparency log entries.
    #[serde(rename = "tlogEntries")]
    pub(crate) tlog_entries: Vec<TlogEntry>,
}

/// A single certificate entry (raw DER, base64-encoded).
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct CertificateEntry {
    /// Base64-encoded DER certificate bytes.
    #[serde(rename = "rawBytes")]
    pub(crate) raw_bytes: String,
}

/// A certificate chain entry.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct CertificateChainEntry {
    /// Base64-encoded DER certificates, root-first or leaf-first.
    pub(crate) certificates: Vec<CertificateEntry>,
}

/// A transparency log entry from the bundle.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct TlogEntry {
    /// The index of the log entry in the transparency log.
    #[serde(rename = "logIndex")]
    pub(crate) log_index: String,

    /// The log identifier (SHA-256 of the DER-encoded Rekor public key).
    #[serde(rename = "logId")]
    pub(crate) log_id: LogId,

    /// The UNIX timestamp when the entry was integrated into the log.
    #[serde(rename = "integratedTime")]
    pub(crate) integrated_time: String,

    /// The inclusion promise containing the Signed Entry Timestamp (SET).
    #[serde(rename = "inclusionPromise")]
    pub(crate) inclusion_promise: Option<InclusionPromise>,

    /// The inclusion proof (Merkle proof).
    #[serde(rename = "inclusionProof")]
    pub(crate) inclusion_proof: Option<InclusionProof>,

    /// The canonicalized body of the log entry (base64-encoded JSON bytes).
    #[serde(rename = "canonicalizedBody")]
    pub(crate) canonicalized_body: Option<String>,
}

/// The log ID (SHA-256 of the DER-encoded Rekor public key).
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct LogId {
    /// Base64-encoded key ID.
    #[serde(rename = "keyId")]
    pub(crate) key_id: String,
}

/// The inclusion promise containing the Signed Entry Timestamp.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct InclusionPromise {
    /// Base64-encoded SET signature over the canonicalized body.
    #[serde(rename = "signedEntryTimestamp")]
    pub(crate) signed_entry_timestamp: String,
}

/// A Merkle inclusion proof.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct InclusionProof {
    /// The log index of the proof checkpoint.
    #[serde(rename = "logIndex")]
    pub(crate) log_index: String,

    /// The Merkle root hash (base64-encoded).
    #[serde(rename = "rootHash")]
    pub(crate) root_hash: String,

    /// The tree size at the time of the proof.
    #[serde(rename = "treeSize")]
    pub(crate) tree_size: String,

    /// The ordered hashes forming the Merkle audit path.
    pub(crate) hashes: Vec<String>,

    /// The signed checkpoint.
    pub(crate) checkpoint: Option<Checkpoint>,
}

/// A signed checkpoint from the transparency log.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct Checkpoint {
    /// The raw checkpoint envelope.
    pub(crate) envelope: String,
}

/// The content of a Sigstore bundle: exactly one of the two shapes.
#[derive(Debug, Clone)]
pub(crate) enum BundleContent {
    /// A simple message signature (the `cosign sign-blob` case).
    MessageSignature {
        /// The digest of the artifact.
        message_digest: Option<MessageDigest>,

        /// Base64-encoded signature bytes.
        signature: String,
    },

    /// A DSSE envelope (in-toto attestation).
    DsseEnvelope {
        /// Base64-encoded payload.
        payload: String,

        /// The payload type (e.g., `application/vnd.in-toto+json`).
        payload_type: String,

        /// The signatures within the envelope.
        signatures: Vec<DsseSignature>,
    },
}

/// Wire form of [`BundleContent::MessageSignature`].
#[derive(Debug, Deserialize)]
struct MessageSignatureContent {
    /// The digest of the artifact.
    #[serde(rename = "messageDigest")]
    message_digest: Option<MessageDigest>,

    /// Base64-encoded signature bytes.
    signature: String,
}

/// Wire form of [`BundleContent::DsseEnvelope`].
#[derive(Debug, Deserialize)]
struct DsseEnvelopeContent {
    /// Base64-encoded payload.
    payload: String,

    /// The payload type (e.g., `application/vnd.in-toto+json`).
    #[serde(rename = "payloadType")]
    payload_type: String,

    /// The signatures within the envelope.
    signatures: Vec<DsseSignature>,
}

/// A message digest within a `MessageSignature`.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct MessageDigest {
    /// The base64-encoded digest value.
    pub(crate) digest: String,
}

/// A signature within a DSSE envelope.
#[derive(Debug, Clone, Deserialize)]
pub(crate) struct DsseSignature {
    /// Base64-encoded signature bytes.
    pub(crate) sig: String,

    /// Optional key identifier for the signing key. Per the DSSE spec this
    /// may be present and non-empty; it must be echoed back verbatim when
    /// reconstructing the canonical envelope JSON for the Rekor tlog body
    /// consistency check, or that check spuriously fails for any bundle
    /// whose producer set a real `keyid`.
    #[serde(default)]
    pub(crate) keyid: Option<String>,
}

// ── Parsing ───────────────────────────────────────────────────────────────────

/// A parsed, media-type-validated Sigstore bundle (v0.1–v0.3).
#[derive(Debug)]
pub(crate) struct ParsedBundle(pub(crate) Bundle);

impl ParsedBundle {
    /// Parse and validate a Sigstore bundle from JSON bytes.
    ///
    /// Rejects anything that is not a recognised Sigstore bundle media type.
    pub(crate) fn from_json(json: &[u8]) -> Result<Self, SigstoreVerificationError> {
        Ok(Self(Bundle::from_json(json)?))
    }

    /// The underlying bundle.
    #[must_use]
    pub(crate) fn bundle(&self) -> &Bundle {
        &self.0
    }

    /// Returns the certificate raw bytes (base64-encoded DER) from the bundle.
    #[must_use]
    pub(crate) fn certificate_base64(&self) -> Option<&str> {
        let vm = &self.0.verification_material;
        vm.certificate
            .as_ref()
            .map(|c| c.raw_bytes.as_str())
            .or_else(|| {
                vm.x509_certificate_chain
                    .as_ref()
                    .and_then(|chain| chain.certificates.first())
                    .map(|c| c.raw_bytes.as_str())
            })
    }

    /// Returns the intermediate certificates (base64 DER) carried in the
    /// bundle's `x509CertificateChain`, if any.
    ///
    /// For v0.1/v0.2 bundles the chain is `[leaf, intermediate...]`, so the
    /// leaf (index 0) is excluded here. v0.3 bundles use a single `certificate`
    /// and carry no intermediates (the verifier uses the trust root's).
    #[must_use]
    pub(crate) fn intermediate_certificates_base64(&self) -> Vec<&str> {
        self.0
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
            .unwrap_or_default()
    }

    /// Returns the signature (base64-encoded) from the bundle.
    #[must_use]
    pub(crate) fn signature_base64(&self) -> Option<&str> {
        match &self.0.content {
            BundleContent::MessageSignature { signature, .. } => Some(signature.as_str()),
            BundleContent::DsseEnvelope { signatures, .. } => {
                signatures.first().map(|s| s.sig.as_str())
            },
        }
    }

    /// Returns every tlog entry carried by the bundle, for Rekor verification.
    ///
    /// `tlogEntries` is `repeated` in `sigstore_bundle.proto` with no
    /// cardinality constraint, so a bundle may legitimately carry more than
    /// one. All of them are returned — and the verifier checks all of them —
    /// because an entry that is parsed but not verified would still travel
    /// onward inside a bundle this crate has declared verified.
    ///
    /// May be empty; the caller establishes non-emptiness where it needs a
    /// specific entry.
    #[must_use]
    pub(crate) fn tlog_entries(&self) -> &[TlogEntry] {
        &self.0.verification_material.tlog_entries
    }

    /// The bundle's media-type version (validated during `from_json`).
    pub(crate) fn version(&self) -> Result<BundleVersion, SigstoreVerificationError> {
        BundleVersion::from_media_type(&self.0.media_type).ok_or_else(|| {
            SigstoreVerificationError::InvalidBundleFormat {
                reason: format!("unsupported media type: {}", self.0.media_type),
            }
        })
    }
}

impl Bundle {
    /// Parse a Sigstore bundle from JSON bytes, rejecting unknown media types
    /// and any bundle that does not carry exactly one content field.
    pub(crate) fn from_json(json: &[u8]) -> Result<Self, SigstoreVerificationError> {
        let raw: RawBundle = serde_json::from_slice(json)
            .map_err(|e| SigstoreVerificationError::BundleParsing { source: e })?;
        if BundleVersion::from_media_type(&raw.media_type).is_none() {
            return Err(SigstoreVerificationError::InvalidBundleFormat {
                reason: format!("unsupported media type: {}", raw.media_type),
            });
        }

        // `messageSignature` and `dsseEnvelope` are a `oneof`: neither an
        // ambiguous bundle nor a contentless one may reach verification. Both
        // are rejected here rather than downstream, so no later step has to
        // pick between two candidate contents.
        let content = match (raw.message_signature, raw.dsse_envelope) {
            (Some(msg), None) => BundleContent::MessageSignature {
                message_digest: msg.message_digest,
                signature: msg.signature,
            },
            (None, Some(dsse)) => BundleContent::DsseEnvelope {
                payload: dsse.payload,
                payload_type: dsse.payload_type,
                signatures: dsse.signatures,
            },
            (Some(_), Some(_)) => {
                return Err(SigstoreVerificationError::InvalidBundleFormat {
                    reason: "bundle carries both messageSignature and dsseEnvelope; \
                             exactly one is allowed"
                        .into(),
                });
            },
            (None, None) => {
                return Err(SigstoreVerificationError::InvalidBundleFormat {
                    reason: "bundle carries neither messageSignature nor dsseEnvelope".into(),
                });
            },
        };

        Ok(Self {
            media_type: raw.media_type,
            verification_material: raw.verification_material,
            content,
        })
    }
}
