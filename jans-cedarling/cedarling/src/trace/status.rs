// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::Serialize;

/// What happened to the TRACE record of one decision, as the decision log and the
/// metrics report it. `Emitted` means handed to the emitter: whether Lock accepted
/// the record shows in the emitter's own logs and metrics.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum TraceRecordStatus {
    /// Handed to the emitter.
    Emitted,
    /// The host passed no TRACE context, or left a mandatory field empty.
    NotEmittedMissingContext,
    /// A value is longer, or a list larger, than Lock accepts.
    NotEmittedOverLimit,
    /// A value Lock rejects, or a policy store that cannot back a record.
    NotEmittedInvalidField,
    /// The emitter queue is full, or the emitter has stopped.
    NotEmittedQueueFull,
    /// There is no TRACE endpoint to send the record to.
    NotEmittedNoEndpoint,
}

impl TraceRecordStatus {
    /// Every status, each with its own counter.
    pub(crate) const ALL: [Self; 6] = [
        Self::Emitted,
        Self::NotEmittedMissingContext,
        Self::NotEmittedOverLimit,
        Self::NotEmittedInvalidField,
        Self::NotEmittedQueueFull,
        Self::NotEmittedNoEndpoint,
    ];

    /// Key of the status counter in the metrics `operational_stats`.
    pub(crate) fn metric_key(self) -> &'static str {
        match self {
            Self::Emitted => "trace.emitted",
            Self::NotEmittedMissingContext => "trace.not_emitted_missing_context",
            Self::NotEmittedOverLimit => "trace.not_emitted_over_limit",
            Self::NotEmittedInvalidField => "trace.not_emitted_invalid_field",
            Self::NotEmittedQueueFull => "trace.not_emitted_queue_full",
            Self::NotEmittedNoEndpoint => "trace.not_emitted_no_endpoint",
        }
    }
}

/// What the TRACE emitter did with records after the authorize path handed them
/// over, counted in the metrics.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum EmitterEvent {
    /// Lock accepted the record.
    Accepted,
    /// Lock refused the record for good, alone or with its whole request.
    Rejected,
    /// The record was given up after its retries, or discarded with its lost chain.
    Dropped,
    /// The draft could not be signed.
    SignFailed,
    /// A chain opened to replace one whose genesis record was lost.
    ChainOpened,
}

impl EmitterEvent {
    /// Every event, each with its own counter.
    pub(crate) const ALL: [Self; 5] = [
        Self::Accepted,
        Self::Rejected,
        Self::Dropped,
        Self::SignFailed,
        Self::ChainOpened,
    ];

    /// Key of the event counter in the metrics `operational_stats`.
    pub(crate) fn metric_key(self) -> &'static str {
        match self {
            Self::Accepted => "trace.accepted",
            Self::Rejected => "trace.rejected",
            Self::Dropped => "trace.dropped",
            Self::SignFailed => "trace.sign_failed",
            Self::ChainOpened => "trace.chains_opened",
        }
    }
}
