// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{Deserialize, Serialize};

/// Per-call metadata that travels with an authorization request.
///
/// Kept apart from the Cedar inputs (`principal`, `action`, `resource`, `context`)
/// because none of it takes part in policy evaluation: the decision is the same
/// with or without options.
#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
pub struct AuthorizeOptions {
    /// TRACE correlation for this call. `None` means the host has no execution
    /// context, so no TRACE record can be emitted for the decision.
    #[serde(default)]
    pub trace: Option<TraceContext>,
}

impl AuthorizeOptions {
    /// Options carrying only a TRACE context.
    #[must_use]
    pub fn with_trace(trace: TraceContext) -> Self {
        Self { trace: Some(trace) }
    }
}

/// TRACE execution context, already normalized by the host.
///
/// Cedarling does not parse headers, tokens or baggage and never generates
/// `execution_id`: the execution initiator (gateway, PEP, host) owns the
/// `execution_id` + `execution_authority` pair. An empty mandatory field is
/// treated as a missing context, so it never blocks the decision.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct TraceContext {
    /// `trace_execution_id` issued by the execution initiator.
    pub execution_id: String,
    /// Authority that issued `execution_id`; always passed together with it.
    pub execution_authority: String,
    /// Stable id of the workload the decision is made for.
    pub workload_id: String,
    /// Instance of the workload, when the host distinguishes instances.
    #[serde(default)]
    pub workload_instance_id: Option<String>,
}

impl TraceContext {
    /// Context without `workload_instance_id`.
    #[must_use]
    pub fn new(execution_id: String, execution_authority: String, workload_id: String) -> Self {
        Self {
            execution_id,
            execution_authority,
            workload_id,
            workload_instance_id: None,
        }
    }
}
