// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! HTTP requests with retry logic that is both native and WASM compatible.

mod backoff;

use std::fmt::Display;

pub(crate) use backoff::Backoff;

use reqwest::{RequestBuilder, StatusCode};
use thiserror::Error;

#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub struct HttpRequestError {
    #[source]
    reason: HttpRequestReasonError,
    status_code: Option<StatusCode>,
    retry_count: u32,
    last_error: Option<String>,
}

impl HttpRequestError {
    #[must_use]
    pub fn new(reason: HttpRequestReasonError, status_code: Option<StatusCode>) -> Self {
        Self {
            reason,
            status_code,
            retry_count: 0,
            last_error: None,
        }
    }

    #[must_use]
    pub fn is_max_retries_exceeded(&self) -> bool {
        matches!(self.reason, HttpRequestReasonError::MaxRetriesExceeded)
    }

    /// `true` when the error indicates the response body could not be read
    /// (TCP drop mid-stream, content-decoding failure, length mismatch, etc.)
    /// — the HTTP transaction reached a status but the body fetch failed.
    /// Lets callers route such failures into a distinct outcome bucket
    /// rather than conflating them with status-code errors.
    #[must_use]
    pub fn is_decode_error(&self) -> bool {
        matches!(self.reason, HttpRequestReasonError::DecodeResponseBytes(_))
    }

    /// `true` when the error indicates an upstream rejected the request with
    /// a non-success HTTP status (4xx / 5xx) outside of any retry path.
    /// Distinguishes "we got a response but it was an error status" from
    /// transport failures (`MaxRetriesExceeded`) so callers can classify
    /// metrics correctly.
    #[must_use]
    pub fn is_http_status_error(&self) -> bool {
        matches!(self.reason, HttpRequestReasonError::HttpStatusError)
    }

    /// The HTTP status code captured at the failure point, if any. `Some(...)`
    /// means the HTTP transaction reached a status (whether 2xx, 3xx, 4xx, or
    /// 5xx); `None` means the failure happened before a response arrived
    /// (DNS, TCP connect, TLS, etc.). Callers can use this to disambiguate
    /// "couldn't reach upstream" from "got a bad status" — particularly
    /// useful when the retry layer collapses 4xx/5xx exhaustion into the
    /// generic `MaxRetriesExceeded` variant: a `Some(status)` there still
    /// means we received responses, just unacceptable ones.
    #[must_use]
    pub fn status_code(&self) -> Option<StatusCode> {
        self.status_code
    }

    #[must_use]
    pub fn with_retry_count(mut self, count: u32) -> Self {
        self.retry_count = count;
        self
    }

    #[must_use]
    pub fn with_last_error(mut self, error: impl Into<String>) -> Self {
        self.last_error = Some(error.into());
        self
    }
}

impl Display for HttpRequestError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.reason)?;
        if let Some(code) = self.status_code {
            write!(f, " (HTTP {code})")?;
        }
        if self.retry_count > 0 {
            write!(f, " (retries: {})", self.retry_count)?;
        }
        if let Some(err) = &self.last_error {
            write!(f, " [last error: {err}]")?;
        }
        Ok(())
    }
}

#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum HttpRequestReasonError {
    #[error("max retries exceeded")]
    MaxRetriesExceeded,
    #[error("response status indicates failure")]
    HttpStatusError,
    #[error("failed to deserialize response to JSON: {0}")]
    DeserializeToJson(#[source] reqwest::Error),
    #[error("failed to deserialize response body bytes to JSON: {0}")]
    DeserializeBytesToJson(#[source] serde_json::Error),
    #[error("failed to decode response body as text: {0}")]
    DecodeResponseText(#[source] reqwest::Error),
    #[error("response body is not valid UTF-8: {0}")]
    InvalidUtf8(#[source] std::string::FromUtf8Error),
    #[error("failed to read response body bytes: {0}")]
    DecodeResponseBytes(#[source] reqwest::Error),
    #[error(
        "response body exceeds the configured limit of {limit} bytes \
         (read {read_so_far} bytes before stopping)"
    )]
    ResponseTooLarge { limit: u64, read_so_far: u64 },
}

/// Sends an HTTP request with backoff retry logic.
pub(crate) struct Sender {
    backoff: Backoff,
    /// When set, response bodies larger than this many bytes are rejected
    /// before they're fully read into memory.
    max_response_size: Option<u64>,
}

impl Sender {
    #[must_use]
    pub(crate) fn new(backoff: Backoff) -> Self {
        Self {
            backoff,
            max_response_size: None,
        }
    }

    /// Builder-style setter for the response body size cap. `None` (the default)
    /// disables the cap. Returns `Self` for chaining.
    #[must_use]
    pub(crate) fn with_max_response_size(mut self, max_response_size: Option<u64>) -> Self {
        self.max_response_size = max_response_size;
        self
    }

    /// Internal helper that sends an HTTP request with retry logic and returns the response.
    ///
    /// This is the core retry loop used by all public send methods.
    pub(crate) async fn send_with_retry<F>(
        &mut self,
        mut request: F,
    ) -> Result<reqwest::Response, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
    {
        let backoff = &mut self.backoff;
        backoff.reset();
        let mut attempt = 0u32;

        loop {
            attempt += 1;
            let response = match request().send().await {
                Ok(resp) => resp,
                Err(err) => {
                    // Retry silently - callers receive the final error if all retries fail.
                    // TODO: add optional debug-level logging hook here once a logger can be
                    //       passed in without pulling logging into this low-level crate.
                    let err_msg = err
                        .to_string()
                        .lines()
                        .next()
                        .unwrap_or("unknown error")
                        .to_string();
                    backoff.snooze().await.map_err(|()| {
                        HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, None)
                            .with_retry_count(attempt)
                            .with_last_error(err_msg)
                    })?;
                    continue;
                },
            };

            let response = match response.error_for_status() {
                Ok(resp) => resp,
                Err(err) => {
                    // Retry silently - callers receive the final error if all retries fail.
                    // TODO: add optional debug-level logging hook here once a logger can be
                    //       passed in without pulling logging into this low-level crate.
                    let status = err.status();
                    let err_msg = err
                        .to_string()
                        .lines()
                        .next()
                        .unwrap_or("unknown error")
                        .to_string();
                    backoff.snooze().await.map_err(|()| {
                        HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, status)
                            .with_retry_count(attempt)
                            .with_last_error(err_msg)
                    })?;
                    continue;
                },
            };

            return Ok(response);
        }
    }

    /// Sends an HTTP request a single time and deserializes the JSON response.
    ///
    /// Unlike [`send`], this method performs **no retries** — useful for non-idempotent
    /// requests (POST, PUT, PATCH, DELETE) where replaying the request is unsafe.
    pub(crate) async fn send_once<T, F>(&mut self, request: F) -> Result<T, HttpRequestError>
    where
        F: FnOnce() -> RequestBuilder,
        T: serde::de::DeserializeOwned,
    {
        let response = request().send().await.map_err(|e| {
            let err_msg = e.to_string();
            HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, None)
                .with_retry_count(1)
                .with_last_error(err_msg)
        })?;
        let response = response.error_for_status().map_err(|e| {
            let status = e.status();
            let err_msg = e.to_string();
            HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, status)
                .with_retry_count(1)
                .with_last_error(err_msg)
        })?;
        let status = response.status();
        let bytes = read_response_capped(response, self.max_response_size)
            .await
            .map_err(|reason| HttpRequestError::new(reason, Some(status)))?;
        serde_json::from_slice::<T>(&bytes).map_err(|e| {
            HttpRequestError::new(
                HttpRequestReasonError::DeserializeBytesToJson(e),
                Some(status),
            )
        })
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
    pub(crate) async fn send<T, F>(&mut self, request: F) -> Result<T, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
        T: serde::de::DeserializeOwned,
    {
        let response = self.send_with_retry(request).await?;
        let status = response.status();
        let bytes = read_response_capped(response, self.max_response_size)
            .await
            .map_err(|reason| HttpRequestError::new(reason, Some(status)))?;
        serde_json::from_slice::<T>(&bytes).map_err(|e| {
            HttpRequestError::new(
                HttpRequestReasonError::DeserializeBytesToJson(e),
                Some(status),
            )
        })
    }

    /// Sends an HTTP request with retry logic and returns the response body as text.
    #[cfg(test)]
    pub(crate) async fn send_text<F>(&mut self, request: F) -> Result<String, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
    {
        let response = self.send_with_retry(request).await?;
        let status = response.status();
        let bytes = read_response_capped(response, self.max_response_size)
            .await
            .map_err(|reason| HttpRequestError::new(reason, Some(status)))?;
        String::from_utf8(bytes).map_err(|e| {
            HttpRequestError::new(HttpRequestReasonError::InvalidUtf8(e), Some(status))
        })
    }

    /// Sends an HTTP request with retry logic and returns the response body as raw bytes.
    #[cfg(test)]
    pub(crate) async fn send_bytes<F>(&mut self, request: F) -> Result<Vec<u8>, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
    {
        let response = self.send_with_retry(request).await?;
        let status = response.status();
        read_response_capped(response, self.max_response_size)
            .await
            .map_err(|reason| HttpRequestError::new(reason, Some(status)))
    }
}

/// Reads a response body into memory, aborting with `ResponseTooLarge` when
/// `max_response_size` is exceeded. Rejects upfront on `Content-Length`; on
/// native targets also enforces the cap chunk-by-chunk so chunked responses
/// without `Content-Length` can't slip past. `None` disables the cap.
pub(crate) async fn read_response_capped(
    #[cfg_attr(target_arch = "wasm32", allow(unused_mut))] mut response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<Vec<u8>, HttpRequestReasonError> {
    if let Some(limit) = max_response_size
        && let Some(declared) = response.content_length()
        && declared > limit
    {
        return Err(HttpRequestReasonError::ResponseTooLarge {
            limit,
            read_so_far: declared,
        });
    }

    // WASM's reqwest backend (browser fetch) does not expose chunked reads, so
    // we fall back to a full `bytes()` read with a post-check. The
    // `Content-Length` fast path above is the primary defence on this target.
    #[cfg(target_arch = "wasm32")]
    {
        let bytes = response
            .bytes()
            .await
            .map_err(HttpRequestReasonError::DecodeResponseBytes)?;
        if let Some(limit) = max_response_size
            && (bytes.len() as u64) > limit
        {
            return Err(HttpRequestReasonError::ResponseTooLarge {
                limit,
                read_so_far: bytes.len() as u64,
            });
        }

        Ok(bytes.to_vec())
    }

    #[cfg(not(target_arch = "wasm32"))]
    {
        let mut buf: Vec<u8> = Vec::new();
        loop {
            match response.chunk().await {
                Ok(Some(chunk)) => {
                    if let Some(limit) = max_response_size {
                        let new_len = (buf.len() as u64).saturating_add(chunk.len() as u64);
                        if new_len > limit {
                            return Err(HttpRequestReasonError::ResponseTooLarge {
                                limit,
                                read_so_far: new_len,
                            });
                        }
                    }
                    buf.extend_from_slice(&chunk);
                },
                Ok(None) => break,
                Err(e) => return Err(HttpRequestReasonError::DecodeResponseBytes(e)),
            }
        }
        Ok(buf)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn rejects_body_larger_than_cap() {
        let mut server = mockito::Server::new_async().await;
        server
            .mock("GET", "/")
            .with_status(200)
            .with_body(vec![0u8; 4096])
            .create_async()
            .await;
        let resp = reqwest::get(server.url()).await.expect("send");
        let err = read_response_capped(resp, Some(1024))
            .await
            .expect_err("body exceeds cap");
        assert!(
            matches!(
                err,
                HttpRequestReasonError::ResponseTooLarge { limit: 1024, .. }
            ),
            "body over the 1024-byte cap must surface as ResponseTooLarge {{ limit: 1024, .. }}, got {err:?}",
        );
    }

    #[tokio::test]
    async fn is_decode_error_true_only_for_response_bytes_failure() {
        // We want a real `DecodeResponseBytes`: response status + headers
        // arrive, but the body stream is truncated before all of the
        // advertised `Content-Length` bytes show up.
        //
        // Mockito's `with_chunked_body(|w| Err(...))` *can* produce that
        // state, but in practice mockito tears the connection down before
        // the response line reaches the client, so reqwest reports
        // `IncompleteMessage` at send-time — a flaky race we hit on CI.
        // A one-shot raw TCP listener gives us deterministic control: we
        // send full headers (with `Content-Length: 1000`), flush, write 7
        // bytes of body, then close — the client always sees the response,
        // then always sees the truncated body.
        use std::io::{Read, Write};
        let listener = std::net::TcpListener::bind("127.0.0.1:0").expect("bind");
        let addr = listener.local_addr().expect("local_addr");
        let server = std::thread::spawn(move || {
            let (mut sock, _) = listener.accept().expect("accept");
            // Drain the request headers so the kernel doesn't RST the
            // connection while we're writing the response.
            let mut buf = [0u8; 1024];
            let mut total = 0usize;
            while total < buf.len() {
                match sock.read(&mut buf[total..]) {
                    Ok(0) | Err(_) => break,
                    Ok(n) => {
                        total += n;
                        if buf[..total].windows(4).any(|w| w == b"\r\n\r\n") {
                            break;
                        }
                    },
                }
            }
            sock.write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 1000\r\nConnection: close\r\n\r\n")
                .expect("write headers");
            sock.flush().expect("flush headers");
            // Far fewer than the advertised 1000 bytes, then drop = close.
            let _ = sock.write_all(b"partial");
        });
        let resp = reqwest::get(format!("http://{addr}/")).await.expect("send");
        let e = resp
            .bytes()
            .await
            .expect_err("expected truncated body to produce DecodeResponseBytes");
        server.join().ok();
        let err = HttpRequestError::new(
            HttpRequestReasonError::DecodeResponseBytes(e),
            Some(reqwest::StatusCode::OK),
        );
        assert!(
            err.is_decode_error(),
            "DecodeResponseBytes must satisfy is_decode_error()",
        );
        assert!(
            !err.is_max_retries_exceeded(),
            "DecodeResponseBytes must NOT satisfy is_max_retries_exceeded()",
        );
        assert!(
            !err.is_http_status_error(),
            "DecodeResponseBytes must NOT satisfy is_http_status_error()",
        );

        // Cross-check the negative directions on stable error variants.
        let max_retries = HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, None);
        assert!(
            !max_retries.is_decode_error(),
            "MaxRetriesExceeded must not satisfy is_decode_error() — the three classifiers are mutually exclusive",
        );
        assert!(
            max_retries.is_max_retries_exceeded(),
            "MaxRetriesExceeded variant must satisfy is_max_retries_exceeded()",
        );

        let status = HttpRequestError::new(HttpRequestReasonError::HttpStatusError, None);
        assert!(
            !status.is_decode_error(),
            "HttpStatusError must not satisfy is_decode_error() — they classify different failure modes",
        );
        assert!(
            !status.is_max_retries_exceeded(),
            "HttpStatusError must not satisfy is_max_retries_exceeded() — that variant is for transport-exhausted retries, not status errors",
        );
        assert!(
            status.is_http_status_error(),
            "HttpStatusError variant must satisfy is_http_status_error()",
        );
    }
}
