// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE producer: everything Cedarling needs to sign `AUTHORIZATION_DECISION`
//! records. Cryptography lives in `jans-trace-core`.

mod chain;
#[cfg(not(target_arch = "wasm32"))]
mod key;
mod producer_id;
mod record;
mod recorder;
mod signing_kid;
mod status;
#[cfg(test)]
mod test_utils;

#[cfg(not(target_arch = "wasm32"))]
pub(crate) use key::ProducerKey;
pub use producer_id::{ParseProducerIdError, ProducerId};
pub(crate) use record::{DecisionFacts, DraftError, Outcome, TokenRef};
pub(crate) use recorder::{TraceRecorder, token_facts};
pub use signing_kid::{ParseTraceSigningKidError, TraceSigningKid};
pub(crate) use status::TraceRecordStatus;
