// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Test-only helpers for code that builds an [`HttpClient`] or reads its answers.

use std::time::Duration;

use serde::Deserialize;

use super::{HttpClient, HttpClientConfig, HttpClientError};

/// The default HTTP client settings with a 10 ms retry delay, so a test that
/// exercises retries finishes in milliseconds rather than 3 + 6 + 12 seconds.
pub(crate) fn fast_retries() -> HttpClientConfig {
    HttpClientConfig {
        retry_delay: Duration::from_millis(10),
        ..HttpClientConfig::default()
    }
}

impl HttpClient {
    /// GET with retries, returning the body for the test to deserialize.
    pub(crate) async fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        let body = self.get_bytes(uri).await?;
        Ok(Response { body })
    }

    /// GET with retries, returning the raw body.
    pub(crate) async fn get_bytes(&self, uri: &str) -> Result<Vec<u8>, HttpClientError> {
        let response = self.get_with_retry(uri).await?;
        self.read_response_capped(response).await
    }
}

/// A response body a test deserializes when it needs it.
#[derive(Debug)]
pub(crate) struct Response {
    body: Vec<u8>,
}

impl Response {
    pub(crate) fn json<'a, T>(&'a self) -> Result<T, serde_json::Error>
    where
        T: Deserialize<'a>,
    {
        serde_json::from_slice::<'a, T>(&self.body)
    }
}
