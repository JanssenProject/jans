// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use crate::error_code::SelfTestError;

/// Known-answer checks for JCS, SHA-256, Ed25519 and the receipt hash. Run at startup; on
/// failure disable TRACE (fail closed).
#[uniffi::export]
pub fn self_test() -> Result<(), Arc<SelfTestError>> {
    jans_trace_core::self_test::self_test().map_err(SelfTestError::wrap)
}
