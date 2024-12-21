// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::thread::sleep;
use std::time::Duration;

use reqwest::blocking::Client;

use super::{HttpClientError, HttpGet, Response};

/// A wrapper around `reqwest::blocking::Client` providing HTTP request functionality
/// with retry logic.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs.
#[derive(Debug)]
pub struct BlockingHttpClient {
    client: reqwest::blocking::Client,
    max_retries: u32,
    retry_delay: Duration,
}

impl BlockingHttpClient {
    pub fn new(max_retries: u32, retry_delay: Duration) -> Result<Self, HttpClientError> {
        let client = Client::builder()
            .build()
            .map_err(HttpClientError::Initialization)?;

        Ok(Self {
            client,
            max_retries,
            retry_delay,
        })
    }
}

impl HttpGet for BlockingHttpClient {
    /// Sends a GET request to the specified URI with retry logic.
    ///
    /// This method will attempt to fetch the resource up to 3 times, with an increasing delay
    /// between each attempt.
    fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        // Fetch the JWKS from the jwks_uri
        let mut attempts = 0;
        let response = loop {
            match self.client.get(uri).send() {
                // Exit loop on success
                Ok(response) => break response,

                Err(e) if attempts < self.max_retries => {
                    attempts += 1;
                    // TODO: pass this message to the logger
                    eprintln!(
                        "Request failed (attempt {} of {}): {}. Retrying...",
                        attempts, self.max_retries, e
                    );
                    sleep(self.retry_delay * attempts);
                },
                // Exit if max retries exceeded
                Err(e) => return Err(HttpClientError::MaxHttpRetriesReached(e)),
            }
        };

        let response = response
            .error_for_status()
            .map_err(HttpClientError::HttpStatus)?;

        Ok(Response {
            text: response
                .text()
                .map_err(HttpClientError::DecodeResponseUtf8)?,
        })
    }
}
