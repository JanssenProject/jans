// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

pub(crate) mod cache_headers;
mod spawn_task;

pub use spawn_task::*;

use cache_headers::CacheHeadersState;
use http_utils::{Backoff, HttpRequestError, HttpRequestReasonError, Sender};
pub(crate) use reqwest::RequestBuilder;
use reqwest::{Client, ClientBuilder};
#[cfg(test)]
use serde::Deserialize;
use std::time::Duration;
use thiserror::Error;

/// A wrapper around `reqwest::Client` providing HTTP request functionality
/// with retry logic using exponential backoff.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism
/// that attempts to fetch the requested resource up to a maximum number of times
/// if an error occurs, using the `Sender` and `Backoff` utilities from `http_utils`.
///
/// Structure is thread safe, feel free to clone.
#[derive(Debug, Clone)]
pub(crate) struct HttpClient {
    pub(crate) raw_client: Client,
    // used in non-test builds via create_sender()
    #[allow(unused)]
    retry_delay: Duration,
    #[allow(unused)]
    max_retries: u32,
}

/// Configuration for [`HttpClient`] — controls retry behavior and per-request timeout.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct HttpClientConfig {
    /// Maximum number of times a failed request is retried before giving up.
    pub max_retries: u32,
    /// Base delay between retries; actual delay grows exponentially with each attempt.
    pub retry_delay: Duration,
    // WASM's reqwest backend (browser fetch) doesn't expose `.timeout(...)`;
    // request timing is handled by the browser. `request_timeout` is
    // intentionally consumed as a no-op on that target.
    /// Maximum time to wait for a single HTTP request to complete.
    #[cfg(not(target_arch = "wasm32"))]
    pub request_timeout: Duration,
}

#[cfg(not(target_arch = "wasm32"))]
impl HttpClientConfig {
    /// Default per-request timeout for production callers of [`HttpClient`].
    /// Without this, a slow or unresponsive upstream can stall a Cedarling task
    /// indefinitely.
    pub const DEFAULT_REQUEST_TIMEOUT: Duration = Duration::from_secs(10);

    /// Time budget for the TCP connect step alone. Most healthy issuers respond well
    /// inside this window; a longer wait usually means the host is unreachable.
    const DEFAULT_HTTP_CONNECT_TIMEOUT: Duration = Duration::from_secs(5);
}

impl HttpClientConfig {
    /// Default maximum retry count used when building an [`HttpClient`] without explicit config.
    pub const DEFAULT_MAX_RETRIES: u32 = 3;

    /// Default base retry delay used when building an [`HttpClient`] without explicit config.
    pub const DEFAULT_RETRY_DELAY: Duration = Duration::from_secs(3);
}

impl Default for HttpClientConfig {
    fn default() -> Self {
        Self {
            max_retries: 3,
            retry_delay: Self::DEFAULT_RETRY_DELAY,
            #[cfg(not(target_arch = "wasm32"))]
            request_timeout: Self::DEFAULT_REQUEST_TIMEOUT,
        }
    }
}

#[derive(Debug, Error)]
#[error(transparent)]
pub struct InitializeHttpClientError(#[from] reqwest::Error);

impl HttpClient {
    pub(crate) fn new(conf: HttpClientConfig) -> Result<Self, InitializeHttpClientError> {
        Self::new_with_builder(Client::builder(), conf)
    }

    pub(crate) fn new_with_builder(
        builder: ClientBuilder,
        conf: HttpClientConfig,
    ) -> Result<Self, InitializeHttpClientError> {
        #[cfg(not(target_arch = "wasm32"))]
        let builder = builder
            .timeout(conf.request_timeout)
            .connect_timeout(HttpClientConfig::DEFAULT_HTTP_CONNECT_TIMEOUT);

        let raw_client = builder.build().map_err(InitializeHttpClientError)?;

        Ok(Self {
            raw_client,
            retry_delay: conf.retry_delay,
            max_retries: conf.max_retries,
        })
    }

    /// Creates a new Sender with the configured backoff strategy.
    pub(crate) fn create_sender(&self) -> Sender {
        #[cfg(not(test))]
        {
            Sender::new(Backoff::new_exponential(
                self.retry_delay,
                Some(self.max_retries),
            ))
        }

        // We implement a faster retry for the tests (faster backoff, but still
        // respects the configured max_retries count).
        #[cfg(test)]
        {
            Sender::new(Backoff::new_exponential(
                Duration::from_millis(10),
                Some(self.max_retries),
            ))
        }
    }

    /// Sends a GET request to the specified URI with retry logic. Returns the
    /// response body as a string wrapped in [`Response`], which the caller can
    /// then deserialize. Test-only: production callers use [`Self::get_bytes`]
    /// or [`Self::get_json`].
    #[cfg(test)]
    pub(crate) async fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        let mut sender = self.create_sender();
        let client = &self.raw_client;
        let text = sender.send_text(|| client.get(uri)).await?;
        Ok(Response { text })
    }

    pub(crate) async fn get_json<T>(&self, uri: &str) -> Result<T, HttpClientError>
    where
        T: serde::de::DeserializeOwned,
    {
        let mut sender = self.create_sender();
        let client = &self.raw_client;
        sender.send(|| client.get(uri)).await
    }

    /// Sends a GET request to the specified URI with retry logic, returning raw bytes.
    ///
    /// This method will attempt to fetch the resource up to the configured `max_retries`,
    /// with exponential backoff between each attempt. Useful for fetching binary content
    /// like archive files.
    pub(crate) async fn get_bytes(&self, uri: &str) -> Result<Vec<u8>, HttpClientError> {
        let mut sender = self.create_sender();
        let client = &self.raw_client;
        sender.send_bytes(|| client.get(uri)).await
    }

    // Execute POST request
    // prepare request build in `builder_fn` and return value as parsed json
    // NOTE: no retries — POST is non-idempotent; replay can cause duplicate side effects.
    pub(crate) async fn post_json<T, F>(&self, builder_fn: F) -> Result<T, HttpClientError>
    where
        T: serde::de::DeserializeOwned,
        F: FnOnce(&Client) -> RequestBuilder,
    {
        let mut sender = self.create_sender();
        let client = &self.raw_client;
        sender.send_once(move || builder_fn(client)).await
    }

    /// Sends a GET request with retry logic and returns the raw [`reqwest::Response`].
    ///
    /// Unlike `get`, `get_json`, or `get_bytes`, this method gives the caller access to
    /// response headers and lets them decide how to consume the body. Use it when you
    /// need to inspect headers (e.g. `Cache-Control`, `Content-Type`) before reading
    /// the payload.
    pub(crate) async fn get_with_retry(
        &self,
        uri: &str,
    ) -> Result<reqwest::Response, HttpClientError> {
        let mut sender = self.create_sender();
        sender.send_with_retry(|| self.raw_client.get(uri)).await
    }

    /// Sends a GET request with retry logic, allowing the caller to modify the
    /// [`RequestBuilder`] before dispatch (e.g. to add custom headers).
    pub(crate) async fn get_with_retry_with<F>(
        &self,
        uri: &str,
        f: F,
    ) -> Result<reqwest::Response, HttpClientError>
    where
        F: Fn(RequestBuilder) -> RequestBuilder,
    {
        let mut sender = self.create_sender();
        sender.send_with_retry(|| f(self.raw_client.get(uri))).await
    }

    /// Fetches `uri` as raw bytes, honoring any previously captured cache
    /// validators (`If-None-Match`, `If-Modified-Since`). Used by the policy
    /// store refresh worker.
    ///
    /// Returns:
    /// - [`ConditionalFetch::NotModified`] when the server replied `304`.
    /// - [`ConditionalFetch::Modified`] with the new body and freshly parsed
    ///   validators otherwise.
    pub(crate) async fn get_bytes_conditional(
        &self,
        uri: &str,
        validators: &CacheHeadersState,
    ) -> Result<ConditionalFetch, HttpClientError> {
        // Clone the small validator strings so the closure can be `Fn` (callable
        // on each retry attempt).
        let etag = validators.etag.clone();
        let last_modified = validators.last_modified.clone();

        let response = self
            .get_with_retry_with(uri, |b| {
                let mut b = b;
                if let Some(e) = &etag {
                    b = b.header(reqwest::header::IF_NONE_MATCH, e.as_str());
                }
                if let Some(lm) = &last_modified {
                    b = b.header(reqwest::header::IF_MODIFIED_SINCE, lm.as_str());
                }
                b
            })
            .await?;

        let status = response.status();
        if status == reqwest::StatusCode::NOT_MODIFIED {
            return Ok(ConditionalFetch::NotModified);
        }

        let headers = response.headers().clone();
        let new_validators = CacheHeadersState::from_headers(&headers, chrono::Utc::now());
        let bytes = response.bytes().await.map(|b| b.to_vec()).map_err(|e| {
            HttpRequestError::new(
                HttpRequestReasonError::DecodeResponseBytes(e),
                Some(status),
            )
        })?;

        Ok(ConditionalFetch::Modified {
            bytes,
            validators: new_validators,
        })
    }

    /// Sends a HEAD request and parses the response headers into a
    /// [`CacheHeadersState`]. Used by the HEAD-then-GET fallback strategy when an
    /// upstream emits `ETag` / `Last-Modified` but ignores `If-None-Match` /
    /// `If-Modified-Since`. Servers that reject HEAD (`405` / `501`) return
    /// [`HeadOutcome::NotSupported`] so the caller can downgrade to plain GET.
    ///
    /// Intentionally does **not** retry on failure: HEAD here is a probe, and
    /// the surrounding refresh worker either downgrades to a plain GET on the
    /// same tick or simply runs again on the next interval. Burning the retry
    /// budget on a probe would only delay the downgrade.
    pub(crate) async fn head_validators(&self, uri: &str) -> Result<HeadOutcome, HttpClientError> {
        let response = self.raw_client.head(uri).send().await.map_err(|e| {
            // No retry path — this is a single probe by design. We reuse
            // `MaxRetriesExceeded` because the surrounding refresh-worker
            // classifier (`HttpRequestError::is_max_retries_exceeded()`) uses
            // it to route transport failures into the `NetworkError` metric
            // bucket. `retry_count(0)` keeps `Display` from claiming we
            // retried.
            let msg = format!("HEAD probe send() failed: {e}");
            HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, None)
                .with_retry_count(0)
                .with_last_error(msg)
        })?;
        let status = response.status();
        if status == reqwest::StatusCode::METHOD_NOT_ALLOWED
            || status == reqwest::StatusCode::NOT_IMPLEMENTED
        {
            return Ok(HeadOutcome::NotSupported);
        }
        let validators = CacheHeadersState::from_headers(response.headers(), chrono::Utc::now());
        Ok(HeadOutcome::Headers(validators))
    }
}

/// Outcome of [`HttpClient::get_bytes_conditional`].
#[derive(Debug)]
pub(crate) enum ConditionalFetch {
    /// Server responded `304 Not Modified` — body intentionally omitted.
    NotModified,
    /// Server returned a fresh body and (possibly) new cache validators.
    Modified {
        bytes: Vec<u8>,
        validators: CacheHeadersState,
    },
}

/// Outcome of [`HttpClient::head_validators`].
#[derive(Debug)]
pub(crate) enum HeadOutcome {
    /// HEAD succeeded — captured headers (validators may be empty if the server
    /// didn't set `ETag` / `Last-Modified`).
    Headers(CacheHeadersState),
    /// Server explicitly rejected HEAD (`405` / `501`).
    NotSupported,
}

/// Wrapper around a text response body — lets callers defer JSON deserialization
/// to a stage where they hold the response. Returned by [`HttpClient::get`].
/// Test-only.
#[cfg(test)]
#[derive(Debug)]
pub(crate) struct Response {
    text: String,
}

#[cfg(test)]
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

#[cfg(all(test, not(target_arch = "wasm32")))]
mod test {
    use crate::http::cache_headers::CacheHeadersState;
    use crate::http::{HttpClient, HttpClientConfig};

    use mockito::Server;
    use serde_json::json;
    use std::time::Duration;
    use test_utils::assert_eq;
    use tokio::join;
    const HTTP_CONF: HttpClientConfig = HttpClientConfig {
        max_retries: 3,
        request_timeout: Duration::from_millis(500),
        retry_delay: Duration::from_secs(5),
    };

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

        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient.");

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
        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient");
        let response = client.get("0.0.0.0").await;

        assert!(
            matches!(response, Err(ref e) if e.is_max_retries_exceeded()),
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

        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient.");

        let link = &format!("{}/.well-known/openid-configuration", mock_server.url());
        let client_fut = client.get(link);

        let (mock_endpoint, response) = join!(mock_endpoint_fut, client_fut);

        assert!(
            matches!(response, Err(ref e) if e.is_max_retries_exceeded()),
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

        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient.");
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

        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient.");
        let link = &format!("{}/error-binary", mock_server.url());
        let req_fut = client.get_bytes(link);
        let (req_result, mock_result) = join!(req_fut, mock_endpoint);

        assert!(
            matches!(req_result, Err(ref e) if e.is_max_retries_exceeded()),
            "Expected MaxRetriesExceeded after retrying on HTTP error status: {req_result:?}"
        );
        mock_result.assert();
    }

    #[tokio::test]
    async fn get_bytes_max_retries_exceeded() {
        let client = HttpClient::new(HTTP_CONF).expect("Should create HttpClient");
        let response = client.get_bytes("0.0.0.0").await;
        assert!(
            matches!(response, Err(ref e) if e.is_max_retries_exceeded()),
            "Expected error due to MaxRetriesExceeded: {response:?}"
        );
    }

    /// A server that accepts TCP connections but never sends a response would
    /// previously stall an `HttpClient::get` call indefinitely. With a request
    /// timeout configured the call must error promptly.
    #[tokio::test]
    async fn get_times_out_when_server_never_responds() {
        use tokio::net::{TcpListener, TcpStream};

        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("Should bind a local TCP listener");
        let addr = listener.local_addr().expect("Should read local addr");

        // Accept connections and hold them open without writing any response.
        let _accept_task = tokio::spawn(async move {
            let mut held: Vec<TcpStream> = Vec::new();
            while let Ok((sock, _)) = listener.accept().await {
                held.push(sock);
            }
        });

        // 0 retries so the timeout fires once and we don't measure backoff.
        let request_timeout = Duration::from_millis(200);
        let client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(1),
            request_timeout,
        })
        .expect("Should create HttpClient");

        let start = std::time::Instant::now();
        let response = client.get(&format!("http://{addr}/")).await;
        let elapsed = start.elapsed();

        assert!(response.is_err(), "Expected timeout error: {response:?}");
        // Timeout fires at ~200ms; allow generous slack for CI variance.
        assert!(
            elapsed < Duration::from_secs(2),
            "Expected to time out near {request_timeout:?}, took {elapsed:?}",
        );
    }

    #[tokio::test]
    async fn conditional_get_returns_not_modified_on_304() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("GET", "/store")
            .match_header("if-none-match", "\"v1\"")
            .with_status(304)
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let validators = crate::http::cache_headers::CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            ..Default::default()
        };
        let url = format!("{}/store", server.url());

        let result = client
            .get_bytes_conditional(&url, &validators)
            .await
            .expect("request");

        assert!(matches!(result, super::ConditionalFetch::NotModified));
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn conditional_get_captures_validators_on_200() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("GET", "/store")
            .with_status(200)
            .with_header("etag", "\"v2\"")
            .with_header("cache-control", "max-age=120")
            .with_body(b"new-body-bytes")
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let url = format!("{}/store", server.url());

        let result = client
            .get_bytes_conditional(&url, &CacheHeadersState::default())
            .await
            .expect("request");

        match result {
            super::ConditionalFetch::Modified { bytes, validators } => {
                assert_eq!(bytes, b"new-body-bytes");
                assert_eq!(validators.etag.as_deref(), Some("\"v2\""));
                assert_eq!(
                    validators.fresh_for,
                    Some(std::time::Duration::from_secs(120))
                );
            }
            super::ConditionalFetch::NotModified => panic!("expected Modified, got NotModified"),
        }
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn conditional_get_sends_if_modified_since() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("GET", "/store")
            .match_header(
                "if-modified-since",
                "Sun, 22 May 2026 12:00:00 GMT",
            )
            .with_status(200)
            .with_body(b"x")
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let validators = crate::http::cache_headers::CacheHeadersState {
            last_modified: Some("Sun, 22 May 2026 12:00:00 GMT".to_string()),
            ..Default::default()
        };
        let url = format!("{}/store", server.url());

        let _ = client
            .get_bytes_conditional(&url, &validators)
            .await
            .expect("request");
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn head_validators_captures_etag_and_last_modified() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("HEAD", "/store")
            .with_status(200)
            .with_header("etag", "\"v3\"")
            .with_header("last-modified", "Sun, 22 May 2026 12:00:00 GMT")
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let url = format!("{}/store", server.url());

        let outcome = client.head_validators(&url).await.expect("request");
        match outcome {
            super::HeadOutcome::Headers(v) => {
                assert_eq!(v.etag.as_deref(), Some("\"v3\""));
                assert_eq!(
                    v.last_modified.as_deref(),
                    Some("Sun, 22 May 2026 12:00:00 GMT")
                );
                assert!(v.has_validator());
            },
            super::HeadOutcome::NotSupported => panic!("expected Headers, got NotSupported"),
        }
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn head_validators_returns_empty_headers_when_no_validators() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("HEAD", "/store")
            .with_status(200)
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let url = format!("{}/store", server.url());

        let outcome = client.head_validators(&url).await.expect("request");
        match outcome {
            super::HeadOutcome::Headers(v) => {
                assert!(
                    !v.has_validator(),
                    "no ETag, no Last-Modified → has_validator() false"
                );
            },
            super::HeadOutcome::NotSupported => panic!("expected Headers, got NotSupported"),
        }
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn head_validators_405_maps_to_not_supported() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("HEAD", "/store")
            .with_status(405)
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let url = format!("{}/store", server.url());

        let outcome = client.head_validators(&url).await.expect("request");
        assert!(matches!(outcome, super::HeadOutcome::NotSupported));
        mock.assert_async().await;
    }

    #[tokio::test]
    async fn head_validators_501_maps_to_not_supported() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("HEAD", "/store")
            .with_status(501)
            .expect(1)
            .create_async()
            .await;

        let client = HttpClient::new(HTTP_CONF).expect("client");
        let url = format!("{}/store", server.url());

        let outcome = client.head_validators(&url).await.expect("request");
        assert!(matches!(outcome, super::HeadOutcome::NotSupported));
        mock.assert_async().await;
    }
}
