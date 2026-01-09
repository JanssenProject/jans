// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this file we define functions for serde `default` macro.

use crate::JwtConfig;
#[cfg(not(target_arch = "wasm32"))]
use crate::log::StdOutLoggerMode;

pub(super) fn default_jti() -> String {
    "jti".to_string()
}

pub(super) fn default_true() -> bool {
    true
}

pub(super) fn default_token_cache_capacity() -> usize {
    JwtConfig::default().token_cache_capacity
}

#[cfg(not(target_arch = "wasm32"))]
pub(super) fn default_stdout_timeout_millis() -> u64 {
    StdOutLoggerMode::DEFAULT_FLUSH_TIMEOUT_MILLIS
}

#[cfg(not(target_arch = "wasm32"))]
pub(super) fn default_stdout_buffer_limit() -> usize {
    StdOutLoggerMode::DEFAULT_BUFFER_LIMIT
}
