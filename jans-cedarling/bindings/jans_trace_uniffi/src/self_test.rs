// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use thiserror::Error;

/// Known-answer checks for JCS, SHA-256, Ed25519 and the receipt hash. Run at startup; on
/// failure disable TRACE (fail closed).
#[uniffi::export]
pub fn self_test() -> Result<(), SelfTestError> {
    jans_trace_core::self_test::self_test().map_err(SelfTestError::SelfTestFailed)
}

#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum SelfTestError {
    #[error(transparent)]
    SelfTestFailed(jans_trace_core::self_test::SelfTestError),
}
