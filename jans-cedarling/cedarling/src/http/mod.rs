// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#[cfg(not(target_family = "wasm"))]
mod blocking;
#[cfg(target_family = "wasm")]
mod wasm;

use std::time::Duration;

use serde::Deserialize;

trait HttpGet {
    /// Sends a GET request to the specified URI
    fn get(&self, uri: &str) -> Result<Response, HttpClientError>;
}

pub struct HttpClient {
    client: Box<dyn HttpGet>,
}

impl HttpClient {
    pub fn new(max_retries: u32, retry_delay: Duration) -> Result<Self, HttpClientError> {
        #[cfg(not(target_family = "wasm"))]
        let client = blocking::BlockingHttpClient::new(max_retries, retry_delay)?;
        #[cfg(target_family = "wasm")]
        let client = wasm::WasmHttpClient::new(max_retries, retry_delay)?;

        Ok(Self {
            client: Box::new(client),
        })
    }

    pub fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        self.client.get(uri)
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
    use std::time::Duration;

    use mockito::Server;
    use serde_json::json;
    use test_utils::assert_eq;

    use crate::http::{HttpClient, HttpClientError};

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
