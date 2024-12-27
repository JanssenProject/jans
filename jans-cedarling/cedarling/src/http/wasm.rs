// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

use super::{HttpClientError, HttpGet, Response};

/// A wrapper around `reqwest::blocking::Client` providing HTTP request functionality
/// with retry logic.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs.
#[derive(Debug)]
pub struct WasmHttpClient {
    _max_retries: u32,
    _retry_delay: Duration,
}

impl WasmHttpClient {
    pub fn new(_max_retries: u32, _retry_delay: Duration) -> Result<Self, HttpClientError> {
        todo!();
    }
}

impl HttpGet for WasmHttpClient {
    /// Sends a GET request to the specified URI with retry logic.
    ///
    /// This method will attempt to fetch the resource up to 3 times, with an increasing delay
    /// between each attempt.
    fn get(&self, _uri: &str) -> Result<Response, HttpClientError> {
        todo!()
    }
}
