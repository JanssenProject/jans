// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Test-only helpers for code that builds an [`super::HttpClient`].

use std::time::Duration;

use super::HttpClientConfig;

/// The default HTTP client settings with a 10 ms retry delay, so a test that
/// exercises retries finishes in milliseconds rather than 3 + 6 + 12 seconds.
pub(crate) fn fast_retries() -> HttpClientConfig {
    HttpClientConfig {
        retry_delay: Duration::from_millis(10),
        ..HttpClientConfig::default()
    }
}
