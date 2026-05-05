// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this file we define functions for serde `default` macro.

#[cfg(not(target_arch = "wasm32"))]
use crate::log::StdOutLoggerMode;
use crate::{HttpClientConfig, JwtConfig, lock_config::LockServiceConfig};

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

pub(super) fn default_log_channel_capacity() -> usize {
    LockServiceConfig::DEFAULT_CHANNEL_CAPACITY
}

pub(super) fn default_log_max_retries() -> u32 {
    LockServiceConfig::DEFAULT_LOG_MAX_RETRIES
}

#[cfg(not(target_arch = "wasm32"))]
pub(super) fn default_http_client_request_timeout_millis() -> u64 {
    u64::try_from(HttpClientConfig::DEFAULT_REQUEST_TIMEOUT.as_millis()).unwrap_or(u64::MAX)
}

pub(super) fn default_http_client_max_retries() -> u32 {
    HttpClientConfig::DEFAULT_MAX_RETRIES
}

pub(super) fn default_http_client_retry_delay() -> u64 {
    u64::try_from(HttpClientConfig::DEFAULT_RETRY_DELAY.as_millis()).unwrap_or(u64::MAX)
}
