// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE producer: everything Cedarling needs to sign `AUTHORIZATION_DECISION`
//! records. Cryptography lives in `jans-trace-core`.

mod chain;
mod emitter;
mod key;
mod producer_id;
mod record;
mod recorder;
mod signing_kid;
mod status;
#[cfg(test)]
pub(crate) mod test_utils;

pub use emitter::EmitterInitError;
pub(crate) use emitter::{EmitterHandle, EmitterParams, spawn_emitter};
pub(crate) use key::ProducerKey;
pub use producer_id::{ParseProducerIdError, ProducerId};
pub(crate) use record::{DecisionFacts, DraftError, Outcome, PolicyFacts, TokenRef};
pub(crate) use recorder::{TraceRecorder, token_facts};
pub use signing_kid::{ParseTraceSigningKidError, TraceSigningKid};
pub(crate) use status::{EmitterEvent, TraceRecordStatus};
