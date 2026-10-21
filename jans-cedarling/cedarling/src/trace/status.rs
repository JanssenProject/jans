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
