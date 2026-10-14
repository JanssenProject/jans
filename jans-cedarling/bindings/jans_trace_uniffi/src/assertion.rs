// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use jans_trace_core::assertion as core;
use thiserror::Error;

use crate::digest::Digest;
use crate::json::{JsonBody, json_variant};
use crate::key::PublicKey;
use crate::limits::{Limits, SizeLimitError};
use crate::signature::{Ed25519Signer, SignatureValue};

/// The members Lock selects a key by, read from strictly parsed bytes.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct AssertionIdentity {
    pub producer: String,
    pub kid: String,
    pub record_id: String,
}

impl From<&core::AssertionIdentity> for AssertionIdentity {
    fn from(identity: &core::AssertionIdentity) -> Self {
        Self {
            producer: identity.producer().to_owned(),
            kid: identity.kid().to_owned(),
            record_id: identity.record_id().to_owned(),
        }
    }
}

/// A producer's record before signing.
#[derive(Debug, uniffi::Object)]
pub struct UnsignedAssertion(core::UnsignedAssertion);

#[uniffi::export]
impl UnsignedAssertion {
    /// Also checks that the signed record will fit `limits`.
    #[uniffi::constructor]
    pub fn parse(
        assertion: &JsonBody,
        limits: &Limits,
    ) -> Result<Arc<Self>, ParseUnsignedAssertionError> {
        core::UnsignedAssertion::parse(&assertion.value, &limits.0)
            .map(|unsigned| Arc::new(Self(unsigned)))
            .map_err(Into::into)
    }

    pub fn identity(&self) -> AssertionIdentity {
        self.0.identity().into()
    }

    /// The text the signature covers: JCS of the record.
    pub fn signing_input(&self) -> String {
        self.0.signing_input().to_owned()
    }

    /// The signed record is the JCS form of the whole assertion, signature included.
    pub fn sign(&self, signer: &Ed25519Signer) -> Result<Arc<SignedAssertion>, SignError> {
        self.0
            .sign(&signer.0)
            .map(|signed| Arc::new(SignedAssertion(Arc::new(signed))))
            .map_err(Into::into)
    }
}

/// A record with a well-formed top-level `signature`, not yet verified. Shares the core value,
/// so joining records into a bulk body never copies their text.
#[derive(Debug, uniffi::Object)]
pub struct SignedAssertion(Arc<core::SignedAssertion>);

#[uniffi::export]
impl SignedAssertion {
    #[uniffi::constructor]
    pub fn parse(
        assertion: &JsonBody,
        limits: &Limits,
    ) -> Result<Arc<Self>, ParseSignedAssertionError> {
        core::SignedAssertion::parse(&assertion.value, &limits.0)
            .map(|signed| Arc::new(Self(Arc::new(signed))))
            .map_err(Into::into)
    }

    /// The record exactly as received or as `sign` produced it (core `as_str`). Store this;
    /// never serialize the record again.
    pub fn text(&self) -> String {
        self.0.as_str().to_owned()
    }

    pub fn identity(&self) -> AssertionIdentity {
        self.0.identity().into()
    }

    pub fn signature(&self) -> Arc<SignatureValue> {
        Arc::new(SignatureValue(*self.0.signature()))
    }

    /// JCS of the record without its top-level `signature`.
    pub fn signing_input(&self) -> String {
        self.0.signing_input().to_owned()
    }

    /// SHA-256 of JCS of the complete record, signature included.
    pub fn content_digest(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.content_digest()))
    }

    pub fn verify(
        &self,
        context: VerificationContext,
    ) -> Result<Arc<VerifiedAssertion>, VerifyError> {
        let context = core::VerificationContext {
            expected_producer: context.expected_producer,
            expected_kid: context.expected_kid,
            public_key: context.public_key.0,
        };
        self.0
            .verify(&context)
            .map(|verified| Arc::new(VerifiedAssertion(verified)))
            .map_err(Into::into)
    }
}

/// The registered key Lock selected by the record's identity, with the `producer` and `kid`
/// it is registered under.
#[derive(Debug, uniffi::Record)]
pub struct VerificationContext {
    pub expected_producer: String,
    pub expected_kid: String,
    pub public_key: Arc<PublicKey>,
}

/// A record whose signature verified under the expected key; receipts are built from it.
#[derive(Debug, uniffi::Object)]
pub struct VerifiedAssertion(pub(crate) core::VerifiedAssertion);

#[uniffi::export]
impl VerifiedAssertion {
    pub fn identity(&self) -> AssertionIdentity {
        self.0.identity().into()
    }

    pub fn content_digest(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.content_digest()))
    }

    /// SHA-256 of the exact bytes the signature covers.
    pub fn signature_input_sha256(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.signature_input_sha256()))
    }
}

/// One element of a bulk body: the parsed record or why it was rejected.
#[derive(Debug, uniffi::Object)]
pub struct BatchRecord(Result<Arc<core::SignedAssertion>, core::ParseSignedAssertionError>);

#[uniffi::export]
impl BatchRecord {
    pub fn assertion(&self) -> Result<Arc<SignedAssertion>, ParseSignedAssertionError> {
        match &self.0 {
            Ok(signed) => Ok(Arc::new(SignedAssertion(Arc::clone(signed)))),
            Err(error) => Err(error.clone().into()),
        }
    }
}

/// Splits a bulk body into records and parses each one. The error is about the body as a
/// whole; each record carries its own result.
#[uniffi::export]
pub fn parse_batch(
    body: &JsonBody,
    limits: &Limits,
) -> Result<Vec<Arc<BatchRecord>>, ParseBatchError> {
    let records = core::parse_batch(&body.value, &limits.0)?;
    Ok(records
        .into_iter()
        .map(|record| Arc::new(BatchRecord(record.map(Arc::new))))
        .collect())
}

/// The bulk body for `records`: their own text joined into an array.
#[uniffi::export]
#[allow(
    clippy::needless_pass_by_value,
    reason = "UniFFI passes sequences by value; the records are shared, not consumed"
)]
pub fn batch_body(
    records: Vec<Arc<SignedAssertion>>,
    limits: &Limits,
) -> Result<String, SizeLimitError> {
    let shared: Vec<Arc<core::SignedAssertion>> =
        records.iter().map(|record| Arc::clone(&record.0)).collect();
    core::batch_body(&shared, &limits.0).map_err(SizeLimitError::SizeLimitExceeded)
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseUnsignedAssertionError {
    #[error(transparent)]
    InvalidJson(core::ParseUnsignedAssertionError),
    #[error(transparent)]
    DuplicateMember(core::ParseUnsignedAssertionError),
    #[error(transparent)]
    NonCanonicalizableJson(core::ParseUnsignedAssertionError),
    #[error(transparent)]
    SizeLimitExceeded(core::ParseUnsignedAssertionError),
    #[error(transparent)]
    InvalidAssertionShape(core::ParseUnsignedAssertionError),
    #[error(transparent)]
    SignatureAlreadyPresent(core::ParseUnsignedAssertionError),
}

impl From<core::ParseUnsignedAssertionError> for ParseUnsignedAssertionError {
    fn from(error: core::ParseUnsignedAssertionError) -> Self {
        use core::ParseUnsignedAssertionError as Core;
        match &error {
            Core::SizeLimitExceeded(_) => Self::SizeLimitExceeded(error),
            Core::Json(json) => json_variant!(json, error),
            Core::NotAnObject
            | Core::InvalidProducer
            | Core::InvalidKid
            | Core::InvalidRecordId => Self::InvalidAssertionShape(error),
            Core::SignatureAlreadyPresent => Self::SignatureAlreadyPresent(error),
        }
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum SignError {
    #[error(transparent)]
    KidMismatch(core::SignError),
    #[error(transparent)]
    SignerFailure(core::SignError),
}

impl From<core::SignError> for SignError {
    fn from(error: core::SignError) -> Self {
        match &error {
            core::SignError::KidMismatch { .. } => Self::KidMismatch(error),
            core::SignError::Signer(_) => Self::SignerFailure(error),
        }
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseSignedAssertionError {
    #[error(transparent)]
    InvalidJson(core::ParseSignedAssertionError),
    #[error(transparent)]
    DuplicateMember(core::ParseSignedAssertionError),
    #[error(transparent)]
    NonCanonicalizableJson(core::ParseSignedAssertionError),
    #[error(transparent)]
    SizeLimitExceeded(core::ParseSignedAssertionError),
    #[error(transparent)]
    InvalidAssertionShape(core::ParseSignedAssertionError),
    #[error(transparent)]
    MissingSignature(core::ParseSignedAssertionError),
    #[error(transparent)]
    InvalidSignatureEncoding(core::ParseSignedAssertionError),
}

impl From<core::ParseSignedAssertionError> for ParseSignedAssertionError {
    fn from(error: core::ParseSignedAssertionError) -> Self {
        use core::ParseSignedAssertionError as Core;
        match &error {
            Core::SizeLimitExceeded(_) => Self::SizeLimitExceeded(error),
            Core::Json(json) => json_variant!(json, error),
            Core::NotAnObject
            | Core::InvalidProducer
            | Core::InvalidKid
            | Core::InvalidRecordId => Self::InvalidAssertionShape(error),
            Core::MissingSignature => Self::MissingSignature(error),
            Core::InvalidSignatureEncoding(_) => Self::InvalidSignatureEncoding(error),
        }
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum VerifyError {
    #[error(transparent)]
    ProducerMismatch(core::VerifyError),
    #[error(transparent)]
    KidMismatch(core::VerifyError),
    #[error(transparent)]
    SignatureInvalid(core::VerifyError),
}

impl From<core::VerifyError> for VerifyError {
    fn from(error: core::VerifyError) -> Self {
        match &error {
            core::VerifyError::ProducerMismatch { .. } => Self::ProducerMismatch(error),
            core::VerifyError::KidMismatch { .. } => Self::KidMismatch(error),
            core::VerifyError::SignatureInvalid => Self::SignatureInvalid(error),
        }
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseBatchError {
    #[error(transparent)]
    InvalidJson(core::ParseBatchError),
    #[error(transparent)]
    DuplicateMember(core::ParseBatchError),
    #[error(transparent)]
    NonCanonicalizableJson(core::ParseBatchError),
    #[error(transparent)]
    SizeLimitExceeded(core::ParseBatchError),
    #[error(transparent)]
    InvalidBatch(core::ParseBatchError),
}

impl From<core::ParseBatchError> for ParseBatchError {
    fn from(error: core::ParseBatchError) -> Self {
        match &error {
            core::ParseBatchError::SizeLimitExceeded(_) => Self::SizeLimitExceeded(error),
            core::ParseBatchError::Json(json) => json_variant!(json, error),
            core::ParseBatchError::NotAnArray => Self::InvalidBatch(error),
        }
    }
}
