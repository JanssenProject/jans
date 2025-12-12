// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod spawn_task;

pub use spawn_task::*;

use reqwest::Client;
use serde::Deserialize;
use std::time::Duration;

/// A wrapper around `reqwest::blocking::Client` providing HTTP request functionality
/// with retry logic.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs.
#[derive(Debug)]
pub struct HttpClient {
    client: reqwest::Client,
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
}

impl HttpClient {
    /// Private helper for GET requests with retry logic.
    ///
    /// Retries are performed silently - the final error is returned if all attempts fail.
    /// This keeps HttpClient as a simple, low-level utility without logging dependencies.
    async fn get_with_retry(&self, uri: &str) -> Result<reqwest::Response, HttpClientError> {
        let mut attempts = 0;
        loop {
            match self.client.get(uri).send().await {
                Ok(response) => return Ok(response),
                Err(e) if attempts < self.max_retries => {
                    attempts += 1;
                    // Retry silently - callers can log the final error if needed
                    tokio::time::sleep(self.retry_delay * attempts).await;
                },
                Err(e) => return Err(HttpClientError::MaxHttpRetriesReached(e)),
            }
        }
    }

    /// Sends a GET request to the specified URI with retry logic.
    pub async fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        let response = self.get_with_retry(uri).await?;
        let response = response
            .error_for_status()
            .map_err(HttpClientError::HttpStatus)?;
        Ok(Response {
            text: response
                .text()
                .await
                .map_err(HttpClientError::DecodeResponseUtf8)?,
        })
    }

    /// Sends a GET request to the specified URI with retry logic, returning raw bytes.  
    ///  
    /// This method will attempt to fetch the resource up to the configured max_retries,  
    /// with an increasing delay between each attempt. Useful for fetching binary content  
    /// like archive files.  
    pub async fn get_bytes(&self, uri: &str) -> Result<Vec<u8>, HttpClientError> {
        let response = self.get_with_retry(uri).await?;
        let response = response
            .error_for_status()
            .map_err(HttpClientError::HttpStatus)?;
        response
            .bytes()
            .await
            .map(|b| b.to_vec())
            .map_err(HttpClientError::DecodeResponseBody)
    }
}

#[derive(Debug)]
pub struct Response {
    text: String,
}

impl Response {
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
    #[error("Failed to read the server's response body: {0}")]
    DecodeResponseBody(#[source] reqwest::Error),
}

#[cfg(test)]
mod test {
    use crate::http::{HttpClient, HttpClientError};

    use mockito::Server;
    use serde_json::json;
    use std::time::Duration;
    use test_utils::assert_eq;
    use tokio::join;

    #[tokio::test]
    async fn can_fetch() {
        let mut mock_server = Server::new_async().await;

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
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");

        let link = &format!("{}/.well-known/openid-configuration", mock_server.url());
        let req_fut = client.get(link);
        let (req_result, mock_result) = join!(req_fut, mock_endpoint);

        let response = req_result
            .expect("Should get response")
            .json::<serde_json::Value>()
            .expect("Should deserialize JSON response.");

        assert_eq!(
            response, expected,
            "Expected: {expected:?}\nBut got: {response:?}"
        );

        mock_result.assert();
    }

    #[tokio::test]
    async fn errors_when_max_http_retries_exceeded() {
        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient");
        let response = client.get("0.0.0.0").await;

        assert!(
            matches!(response, Err(HttpClientError::MaxHttpRetriesReached(_))),
            "Expected error due to MaxHttpRetriesReached: {response:?}"
        );
    }

    #[tokio::test]
    async fn errors_on_http_error_status() {
        let mut mock_server = Server::new_async().await;

        let mock_endpoint_fut = mock_server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(500)
            .expect(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");

        let link = &format!("{}/.well-known/openid-configuration", mock_server.url());
        let client_fut = client.get(link);

        let (mock_endpoint, response) = join!(mock_endpoint_fut, client_fut);

        assert!(
            matches!(response, Err(HttpClientError::HttpStatus(_))),
            "Expected error due to receiving an http error code: {response:?}"
        );

        mock_endpoint.assert();
    }

    #[tokio::test]
    async fn get_bytes_successful_fetch() {
        let mut mock_server = Server::new_async().await;
        let payload: Vec<u8> = vec![1, 2, 3, 4, 5, 6, 7, 8];
        let mock_endpoint = mock_server
            .mock("GET", "/binary")
            .with_status(200)
            .with_header("content-type", "application/octet-stream")
            .with_body(payload.clone())
            .expect(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");
        let link = &format!("{}/binary", mock_server.url());
        let req_fut = client.get_bytes(link);
        let (req_result, mock_result) = join!(req_fut, mock_endpoint);

        let bytes = req_result.expect("Should get bytes");
        assert_eq!(bytes, payload, "Expected bytes to match payload");
        mock_result.assert();
    }

    #[tokio::test]
    async fn get_bytes_retries_then_succeeds() {
        let mut mock_server = Server::new_async().await;
        let payload: Vec<u8> = vec![42, 99, 100];
        let mock_endpoint = mock_server
            .mock("GET", "/retry-binary")
            .with_status(500)
            .expect(2)
            .create_async();
        let mock_success = mock_server
            .mock("GET", "/retry-binary")
            .with_status(200)
            .with_header("content-type", "application/octet-stream")
            .with_body(payload.clone())
            .expect(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");
        let link = &format!("{}/retry-binary", mock_server.url());
        let req_fut = client.get_bytes(link);
        let (req_result, mock_failures, mock_success) = join!(req_fut, mock_endpoint, mock_success);

        let bytes = req_result.expect("Should get bytes after retries");
        assert_eq!(
            bytes, payload,
            "Expected bytes to match payload after retries"
        );
        mock_failures.assert();
        mock_success.assert();
    }

    #[tokio::test]
    async fn get_bytes_http_error_status() {
        let mut mock_server = Server::new_async().await;
        let mock_endpoint = mock_server
            .mock("GET", "/error-binary")
            .with_status(500)
            .expect(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");
        let link = &format!("{}/error-binary", mock_server.url());
        let req_fut = client.get_bytes(link);
        let (req_result, mock_result) = join!(req_fut, mock_endpoint);

        assert!(
            matches!(req_result, Err(HttpClientError::HttpStatus(_))),
            "Expected error due to receiving an http error code: {req_result:?}"
        );
        mock_result.assert();
    }

    #[tokio::test]
    async fn get_bytes_max_retries_exceeded() {
        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient");
        let response = client.get_bytes("0.0.0.0").await;
        assert!(
            matches!(response, Err(HttpClientError::MaxHttpRetriesReached(_))),
            "Expected error due to MaxHttpRetriesReached: {response:?}"
        );
    }
}
