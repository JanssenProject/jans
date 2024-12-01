/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use reqwest::blocking::Client;
use serde::Deserialize;
use std::{thread::sleep, time::Duration};

/// A wrapper around `reqwest::blocking::Client` providing HTTP request functionality
/// with retry logic.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs.
#[derive(Debug)]
pub struct HttpClient {
    #[cfg(not(target_arch = "wasm32"))]
    client: reqwest::blocking::Client,
    max_retries: u32,
    retry_delay: Duration,
}

impl HttpClient {
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

    /// Sends a GET request to the specified URI with retry logic.
    ///
    /// This method will attempt to fetch the resource up to 3 times, with an increasing delay
    /// between each attempt.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
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

    /// Sends a GET request to the specified URI with retry logic.
    ///
    /// This method will attempt to fetch the resource up to 3 times, with an increasing delay
    /// between each attempt.
    #[cfg(target_arch = "wasm32")]
    pub fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        todo!()
    }
}

#[derive(Debug)]
pub struct Response {
    text: String,
}

impl Response {
    pub fn text(&self) -> &str {
        &self.text
    }

    pub fn json<'a, T>(&'a self) -> Result<T, serde_json::Error>
    where
        T: Deserialize<'a>,
    {
        serde_json::from_str::<'a, T>(&self.text)
    }
}

/// Error type for the HttpClient
#[derive(thiserror::Error, Debug)]
pub enum HttpClientError {
    /// Indicates failure to initialize the HTTP client.
    #[error("Failed to initilize HTTP client: {0}")]
    Initialization(#[source] reqwest::Error),
    /// Indicates an HTTP error response received from an endpoint.
    #[error("Received error HTTP status: {0}")]
    HttpStatus(#[source] reqwest::Error),
    /// Indicates a failure to reach the endpoint after 3 attempts.
    #[error("Could not reach endpoint after trying 3 times: {0}")]
    MaxHttpRetriesReached(#[source] reqwest::Error),
    /// Indicates a failure decode the response body to UTF-8
    #[error("Failed to decode the server's response to UTF-8: {0}")]
    DecodeResponseUtf8(#[source] reqwest::Error),
}

#[cfg(test)]
mod test {
    use crate::jwt::http_client::HttpClientError;

    use super::HttpClient;
    use mockito::Server;
    use serde_json::json;
    use std::time::Duration;
    use test_utils::assert_eq;

    #[test]
    fn can_fetch() {
        let mut mock_server = Server::new();

        let expected = json!({
            "issuer": mock_server.url(),
            "jwks_uri": &format!("{}/jwks", mock_server.url()),
        });

        let mock_endpoint = mock_server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(expected.to_string())
            .expect(1)
            .create();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");

        let response = client
            .get(&format!(
                "{}/.well-known/openid-configuration",
                mock_server.url()
            ))
            .expect("Should get response")
            .json::<serde_json::Value>()
            .expect("Should deserialize JSON response.");

        assert_eq!(
            response, expected,
            "Expected: {expected:?}\nBut got: {response:?}"
        );

        mock_endpoint.assert();
    }

    #[test]
    fn errors_when_max_http_retries_exceeded() {
        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient");
        let response = client.get("0.0.0.0");

        assert!(
            matches!(response, Err(HttpClientError::MaxHttpRetriesReached(_))),
            "Expected error due to MaxHttpRetriesReached: {response:?}"
        );
    }

    #[test]
    fn errors_on_http_error_status() {
        let mut mock_server = Server::new();

        let mock_endpoint = mock_server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(500)
            .expect(1)
            .create();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");

        let response = client.get(&format!(
            "{}/.well-known/openid-configuration",
            mock_server.url()
        ));

        assert!(
            matches!(response, Err(HttpClientError::HttpStatus(_))),
            "Expected error due to receiving an http error code: {response:?}"
        );

        mock_endpoint.assert();
    }
}
