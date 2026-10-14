// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::sync::Arc;

use jans_trace_core::receipt as core;
use thiserror::Error;

use crate::assertion::VerifiedAssertion;
use crate::digest::Digest;

/// Profile id of Jans TRACE assertions.
#[uniffi::export]
pub fn assertion_profile() -> String {
    core::ASSERTION_PROFILE.to_owned()
}

/// Profile id of Lock receipts.
#[uniffi::export]
pub fn receipt_profile() -> String {
    core::RECEIPT_PROFILE.to_owned()
}

/// Sequences crossing back to Java are at most 2^53-1, so they always fit `i64`.
fn to_i64(value: u64) -> i64 {
    i64::try_from(value).unwrap_or(i64::MAX)
}

/// Lock's receipt time, always `YYYY-MM-DDTHH:MM:SS.sssZ` for a real UTC instant.
#[derive(Debug, PartialEq, Eq, Hash, uniffi::Object)]
#[uniffi::export(Display, Eq, Hash)]
pub struct ReceivedAt(core::ReceivedAt);

#[uniffi::export]
impl ReceivedAt {
    /// Formats Lock's clock reading.
    #[uniffi::constructor]
    pub fn from_unix_millis(unix_millis: i64) -> Result<Arc<Self>, ReceivedAtRangeError> {
        let millis = u64::try_from(unix_millis).map_err(|_| {
            ReceivedAtRangeError::InvalidReceipt(ReceivedAtRangeCause::BeforeEpoch(unix_millis))
        })?;
        core::ReceivedAt::from_unix_millis(millis)
            .map(|received_at| Arc::new(Self(received_at)))
            .map_err(|e| ReceivedAtRangeError::InvalidReceipt(ReceivedAtRangeCause::Core(e)))
    }

    /// Only text that formats back unchanged, so dates like February 30 are rejected.
    #[uniffi::constructor]
    pub fn parse(text: &str) -> Result<Arc<Self>, ParseReceivedAtError> {
        text.parse()
            .map(|received_at| Arc::new(Self(received_at)))
            .map_err(ParseReceivedAtError::InvalidReceipt)
    }
}

impl fmt::Display for ReceivedAt {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(f)
    }
}

/// Where a receipt sits in its evidence-domain chain.
#[derive(Debug, PartialEq, Eq, Hash, uniffi::Object)]
#[uniffi::export(Eq, Hash)]
pub struct ReceiptPosition(core::ReceiptPosition);

#[uniffi::export]
impl ReceiptPosition {
    /// The first receipt of an evidence domain: sequence 1 naming the genesis sentinel.
    #[uniffi::constructor]
    pub fn genesis() -> Arc<Self> {
        Arc::new(Self(core::ReceiptPosition::GENESIS))
    }

    /// Sequence 1 only with the sentinel, sequence 2..=2^53-1 only with a real predecessor.
    /// Named `of`, not `new`: Kotlin turns `new` into a primary constructor without `@Throws`,
    /// and Java cannot catch an undeclared checked exception.
    #[uniffi::constructor]
    pub fn of(
        sequence: i64,
        prev_receipt_hash: &Digest,
    ) -> Result<Arc<Self>, ReceiptPositionError> {
        let sequence = u64::try_from(sequence).map_err(|_| {
            ReceiptPositionError::InvalidReceipt(ReceiptPositionCause::Negative(sequence))
        })?;
        core::ReceiptPosition::new(sequence, prev_receipt_hash.0)
            .map(|position| Arc::new(Self(position)))
            .map_err(|e| ReceiptPositionError::InvalidReceipt(ReceiptPositionCause::Core(e)))
    }

    pub fn sequence(&self) -> i64 {
        to_i64(self.0.sequence())
    }

    pub fn prev_receipt_hash(&self) -> Arc<Digest> {
        Arc::new(Digest(*self.0.prev_receipt_hash()))
    }
}

/// The v1 field set hashed into a receipt. Read-only once built.
#[derive(Debug, PartialEq, Eq, uniffi::Object)]
#[uniffi::export(Eq)]
pub struct ReceiptCommitment(core::ReceiptCommitment);

/// A receipt as Lock stored it, read back for an audit; nothing here is verified yet.
#[derive(Debug, uniffi::Record)]
pub struct StoredReceipt {
    pub evidence_domain_id: String,
    pub position: Arc<ReceiptPosition>,
    pub received_at: Arc<ReceivedAt>,
    pub producer_id: String,
    pub record_id: String,
    pub content_digest: Arc<Digest>,
}

#[uniffi::export]
impl ReceiptCommitment {
    /// The commitment for a record just verified: producer, record id and content digest
    /// come from the verification.
    #[uniffi::constructor]
    pub fn for_assertion(
        evidence_domain_id: String,
        position: &ReceiptPosition,
        received_at: &ReceivedAt,
        assertion: &VerifiedAssertion,
    ) -> Arc<Self> {
        Arc::new(Self(core::ReceiptCommitment::for_assertion(
            evidence_domain_id,
            position.0,
            received_at.0.clone(),
            &assertion.0,
        )))
    }

    /// Rebuilds a commitment from stored values, for re-hashing and re-linking a chain.
    #[uniffi::constructor]
    pub fn from_stored(stored: StoredReceipt) -> Result<Arc<Self>, StoredReceiptError> {
        core::ReceiptCommitment::from_stored(core::StoredReceipt {
            evidence_domain_id: stored.evidence_domain_id,
            position: stored.position.0,
            received_at: stored.received_at.0.clone(),
            producer_id: stored.producer_id,
            record_id: stored.record_id,
            content_digest: stored.content_digest.0,
        })
        .map(|commitment| Arc::new(Self(commitment)))
        .map_err(StoredReceiptError::InvalidReceipt)
    }

    pub fn evidence_domain_id(&self) -> String {
        self.0.evidence_domain_id().to_owned()
    }

    pub fn position(&self) -> Arc<ReceiptPosition> {
        Arc::new(ReceiptPosition(self.0.position()))
    }

    pub fn received_at(&self) -> Arc<ReceivedAt> {
        Arc::new(ReceivedAt(self.0.received_at().clone()))
    }

    pub fn producer_id(&self) -> String {
        self.0.producer_id().to_owned()
    }

    pub fn record_id(&self) -> String {
        self.0.record_id().to_owned()
    }

    pub fn content_digest(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.content_digest()))
    }

    /// `sha256(JCS(commitment))`.
    pub fn receipt_hash(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.receipt_hash()))
    }

    /// Checks that this receipt names `previous_receipt_hash` and returns its own hash, the
    /// next chain head.
    pub fn verify_link(
        &self,
        previous_receipt_hash: &Digest,
    ) -> Result<Arc<Digest>, ReceiptLinkError> {
        self.0
            .verify_link(&previous_receipt_hash.0)
            .map(|hash| Arc::new(Digest(hash)))
            .map_err(ReceiptLinkError::ReceiptLinkMismatch)
    }
}

/// The end of an evidence-domain chain verified so far.
#[derive(Debug, PartialEq, Eq, uniffi::Object)]
#[uniffi::export(Eq)]
pub struct ChainHead(core::ChainHead);

#[uniffi::export]
impl ChainHead {
    /// A chain with no receipts: sequence 0 and the genesis sentinel.
    #[uniffi::constructor]
    pub fn empty(evidence_domain_id: String) -> Arc<Self> {
        Arc::new(Self(core::ChainHead::empty(evidence_domain_id)))
    }

    /// Continues a walk from a head saved earlier. Trusts that state.
    #[uniffi::constructor]
    pub fn resume(
        evidence_domain_id: String,
        sequence: i64,
        hash: &Digest,
    ) -> Result<Arc<Self>, ChainHeadError> {
        let sequence = u64::try_from(sequence)
            .map_err(|_| ChainHeadError::InvalidReceipt(ChainHeadCause::Negative(sequence)))?;
        core::ChainHead::resume(evidence_domain_id, sequence, hash.0)
            .map(|head| Arc::new(Self(head)))
            .map_err(|e| ChainHeadError::InvalidReceipt(ChainHeadCause::Core(e)))
    }

    /// Checks that `receipt` continues this chain and returns the head after it.
    pub fn append(&self, receipt: &ReceiptCommitment) -> Result<Arc<Self>, ReceiptLinkError> {
        self.0
            .append(&receipt.0)
            .map(|head| Arc::new(Self(head)))
            .map_err(ReceiptLinkError::ReceiptLinkMismatch)
    }

    pub fn evidence_domain_id(&self) -> String {
        self.0.evidence_domain_id().to_owned()
    }

    /// 0 while the chain is empty.
    pub fn sequence(&self) -> i64 {
        to_i64(self.0.sequence())
    }

    /// The genesis sentinel while the chain is empty.
    pub fn hash(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.hash()))
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ReceivedAtRangeError {
    #[error(transparent)]
    InvalidReceipt(ReceivedAtRangeCause),
}

/// The core reason, or a clock reading the core never sees because it is negative.
#[derive(Debug, Error)]
pub enum ReceivedAtRangeCause {
    #[error(transparent)]
    Core(core::ReceivedAtRangeError),
    #[error("{0} ms is before 1970-01-01T00:00:00.000Z, the first receipt timestamp")]
    BeforeEpoch(i64),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseReceivedAtError {
    #[error(transparent)]
    InvalidReceipt(core::ParseReceivedAtError),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ReceiptPositionError {
    #[error(transparent)]
    InvalidReceipt(ReceiptPositionCause),
}

/// The core reason, or a sequence the core never sees because it is negative.
#[derive(Debug, Error)]
pub enum ReceiptPositionCause {
    #[error(transparent)]
    Core(core::ReceiptPositionError),
    #[error("receipt position is not valid in a v1 chain: sequence {0} is outside 1..=2^53-1")]
    Negative(i64),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum StoredReceiptError {
    #[error(transparent)]
    InvalidReceipt(core::StoredReceiptError),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ChainHeadError {
    #[error(transparent)]
    InvalidReceipt(ChainHeadCause),
}

/// The core reason, or a sequence the core never sees because it is negative.
#[derive(Debug, Error)]
pub enum ChainHeadCause {
    #[error(transparent)]
    Core(core::ChainHeadError),
    #[error("saved chain head is not valid: sequence {0} is outside 0..=2^53-1")]
    Negative(i64),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ReceiptLinkError {
    #[error(transparent)]
    ReceiptLinkMismatch(core::ReceiptLinkError),
}
