// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE producer: everything Cedarling needs to sign `AUTHORIZATION_DECISION`
//! records. Cryptography lives in `jans-trace-core`.

mod producer_id;

pub use producer_id::{ParseProducerIdError, ProducerId};
