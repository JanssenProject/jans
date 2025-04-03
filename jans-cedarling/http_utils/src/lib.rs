// software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is used by cedarling to make HTTP requests with retry logic that is both
//! native and WASM compatible.
//!
//! # Usage
//!
//! Usage involves initializing a [`SenderWithBackoff`] with a [`Backoff`].
//!
//! ```no_run
//! use std::error::Error;
//! use http_utils::{Sender, Backoff, HttpRequestError};
//! use serde::Deserialize;
//!
//! #[derive(Deserialize)]
//! struct OidcResponse {
//!     jwks_uri: String,
//! }
//!
//! async fn make_request() -> Result<OidcResponse, HttpRequestError>
//! {
//!     let client = reqwest::Client::new();
//!     let mut sender = Sender::new(Backoff::default_exponential());
//!
//!     let deserialized_response: OidcResponse = sender.send(|| {
//!         client.get("https://test.jans.io/.well-known/openid-configuration")
//!     }).await?;
//!
//!     Ok(deserialized_response)
//! }
//! ```

mod backoff;

pub use backoff::Backoff;

use reqwest::RequestBuilder;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum HttpRequestError {
    #[error("max retries exceeded")]
    MaxRetriesExceeded,
    #[error("failed to deserialize response to JSON: {0}")]
    DeserializeToJson(#[source] reqwest::Error),
}

/// Sends an HTTP request with backoff retry logic.
pub struct Sender {
    backoff: Backoff,
}

impl Sender {
    pub fn new(backoff: Backoff) -> Self {
        Self { backoff }
    }

    /// Sends an HTTP request with retry logic then deserializes the JSON response to a
    /// struct.
    ///
    /// This function attempts to send a request using the provided [`RequestBuilder`]
    /// generator. If the request fails (e.g., due to network errors or non-success HTTP
    /// status codes), it will retry the request with an exponentially increasing delay
    /// between attempts. The function returns the successfully parsed JSON response or
    /// an error if all retries fail.
    ///
    /// # Notes
    /// - The function retries on both network failures and HTTP error responses.
    /// - The `RequestBuilder` must be **re-created** for each attempt because it cannot be reused.
    pub async fn send<T, F>(&mut self, mut request: F) -> Result<T, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
        T: serde::de::DeserializeOwned,
    {
        let backoff = &mut self.backoff;
        backoff.reset();

        loop {
            let response = match request().send().await {
                Ok(resp) => resp,
                Err(err) => {
                    eprintln!("failed to complete HTTP request: {err}");
                    backoff
                        .snooze()
                        .await
                        .map_err(|_| HttpRequestError::MaxRetriesExceeded)?;
                    continue;
                },
            };

            let response = match response.error_for_status() {
                Ok(resp) => resp,
                Err(err) => {
                    eprintln!("received an HTTP error response: {err}");
                    backoff
                        .snooze()
                        .await
                        .map_err(|_| HttpRequestError::MaxRetriesExceeded)?;
                    continue;
                },
            };

            let response = response
                .json::<T>()
                .await
                .map_err(HttpRequestError::DeserializeToJson)?;

            return Ok(response);
        }
    }
}
