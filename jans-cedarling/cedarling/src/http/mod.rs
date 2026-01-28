// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod spawn_task;

pub(crate) use spawn_task::*;

use http_utils::{Backoff, HttpRequestError, Sender};
use reqwest::Client;
use serde::Deserialize;
use std::time::Duration;

/// A wrapper around `reqwest::Client` providing HTTP request functionality
/// with retry logic using exponential backoff.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs, using the `Sender` and `Backoff` utilities from `http_utils`.
#[derive(Debug)]
pub(crate) struct HttpClient {
    client: Client,
    base_delay: Duration,
    max_retries: u32,
}

impl HttpClient {
    pub(crate) fn new(max_retries: u32, retry_delay: Duration) -> Result<Self, HttpClientError> {
        let client = Client::builder()
            .build()
            .map_err(HttpRequestError::InitializeHttpClient)?;

        Ok(Self {
            client,
            base_delay: retry_delay,
            max_retries,
        })
    }

    /// Creates a new Sender with the configured backoff strategy.
    fn create_sender(&self) -> Sender {
        Sender::new(Backoff::new_exponential(
            self.base_delay,
            Some(self.max_retries),
        ))
    }

    /// Sends a GET request to the specified URI with retry logic.
    pub(crate) async fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        let mut sender = self.create_sender();
        let client = &self.client;
        let text = sender.send_text(|| client.get(uri)).await?;
        Ok(Response { text })
    }

    /// Sends a GET request to the specified URI with retry logic, returning raw bytes.
    ///
    /// This method will attempt to fetch the resource up to the configured `max_retries`,
    /// with exponential backoff between each attempt. Useful for fetching binary content
    /// like archive files.
    pub(crate) async fn get_bytes(&self, uri: &str) -> Result<Vec<u8>, HttpClientError> {
        let mut sender = self.create_sender();
        let client = &self.client;
        sender.send_bytes(|| client.get(uri)).await
    }
}

#[derive(Debug)]
pub(crate) struct Response {
    text: String,
}

impl Response {
    pub(crate) fn json<'a, T>(&'a self) -> Result<T, serde_json::Error>
    where
        T: Deserialize<'a>,
    {
        serde_json::from_str::<'a, T>(&self.text)
    }
}

/// Error type for the [`HttpClient`] - re-export from [`http_utils`] for compatibility
pub(crate) type HttpClientError = HttpRequestError;

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
            matches!(response, Err(HttpClientError::MaxRetriesExceeded)),
            "Expected error due to MaxRetriesExceeded: {response:?}"
        );
    }

    #[tokio::test]
    async fn retries_on_http_error_status_then_fails() {
        let mut mock_server = Server::new_async().await;

        // The new implementation retries on HTTP error status codes too,
        // so we expect multiple attempts before MaxRetriesExceeded
        let mock_endpoint_fut = mock_server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(500)
            .expect_at_least(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");

        let link = &format!("{}/.well-known/openid-configuration", mock_server.url());
        let client_fut = client.get(link);

        let (mock_endpoint, response) = join!(mock_endpoint_fut, client_fut);

        assert!(
            matches!(response, Err(HttpClientError::MaxRetriesExceeded)),
            "Expected error due to MaxRetriesExceeded after retrying on HTTP errors: {response:?}"
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
    async fn get_bytes_retries_on_http_error_status() {
        let mut mock_server = Server::new_async().await;

        // The new implementation retries on HTTP error status codes too
        let mock_endpoint = mock_server
            .mock("GET", "/error-binary")
            .with_status(500)
            .expect_at_least(1)
            .create_async();

        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient.");
        let link = &format!("{}/error-binary", mock_server.url());
        let req_fut = client.get_bytes(link);
        let (req_result, mock_result) = join!(req_fut, mock_endpoint);

        assert!(
            matches!(req_result, Err(HttpClientError::MaxRetriesExceeded)),
            "Expected MaxRetriesExceeded after retrying on HTTP error status: {req_result:?}"
        );
        mock_result.assert();
    }

    #[tokio::test]
    async fn get_bytes_max_retries_exceeded() {
        let client =
            HttpClient::new(3, Duration::from_millis(1)).expect("Should create HttpClient");
        let response = client.get_bytes("0.0.0.0").await;
        assert!(
            matches!(response, Err(HttpClientError::MaxRetriesExceeded)),
            "Expected error due to MaxRetriesExceeded: {response:?}"
        );
    }
}
