// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! HTTP requests with retry logic that is both native and WASM compatible.

mod backoff;

use std::error::Error as StdError;
use std::fmt::{self, Display};

pub(crate) use backoff::Backoff;
#[cfg(not(target_arch = "wasm32"))]
pub(crate) use backoff::MAX_EXPONENTIAL_DELAY;

use reqwest::{RequestBuilder, StatusCode, Url};
use thiserror::Error;

/// Shows an error with its causes: `reqwest`'s own message leaves them out, so an
/// invalid URL would read only "builder error".
struct ErrorChain<'a>(&'a (dyn StdError + 'static));

impl Display for ErrorChain<'_> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)?;
        let mut source = self.0.source();
        while let Some(err) = source {
            write!(f, ": {err}")?;
            source = err.source();
        }
        Ok(())
    }
}

/// One attempt that got no usable response, sorted once by whether repeating can help.
#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum AttemptFailure {
    #[error(transparent)]
    Retryable(RetryableFailure),
    #[error(transparent)]
    Final(FinalFailure),
}

/// A failure the same request may not repeat: the network or the server may recover.
#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum RetryableFailure {
    /// Connect, timeout or dropped connection.
    #[error("{}", ErrorChain(.0))]
    Transport(reqwest::Error),
    /// `408`, `429` or `5xx`.
    #[error("HTTP {status} from {url}")]
    Status { status: StatusCode, url: Box<Url> },
}

/// A failure the same request repeats on every attempt.
#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum FinalFailure {
    /// A request that cannot be built (invalid URL) or a redirect that is refused.
    #[error("invalid request: {}", ErrorChain(.0))]
    InvalidRequest(reqwest::Error),
    /// Any `4xx` other than `408` and `429`.
    #[error("HTTP {status} from {url}")]
    Status { status: StatusCode, url: Box<Url> },
}

impl AttemptFailure {
    /// Sorts the result of one `send()`: `1xx` to `3xx` are responses, the rest failures.
    pub(crate) fn check(
        result: Result<reqwest::Response, reqwest::Error>,
    ) -> Result<reqwest::Response, Self> {
        let response = result.map_err(Self::from_send_error)?;
        match Self::from_response(&response) {
            Some(failure) => Err(failure),
            None => Ok(response),
        }
    }

    fn from_send_error(err: reqwest::Error) -> Self {
        if err.is_builder() || err.is_redirect() {
            Self::Final(FinalFailure::InvalidRequest(err))
        } else {
            Self::Retryable(RetryableFailure::Transport(err))
        }
    }

    // `408` and `429` come from a proxy as often as from the server: both ask to repeat later.
    fn from_response(response: &reqwest::Response) -> Option<Self> {
        let status = response.status();
        let url = Box::new(response.url().clone());
        if status.is_server_error()
            || status == StatusCode::TOO_MANY_REQUESTS
            || status == StatusCode::REQUEST_TIMEOUT
        {
            Some(Self::Retryable(RetryableFailure::Status { status, url }))
        } else if status.is_client_error() {
            Some(Self::Final(FinalFailure::Status { status, url }))
        } else {
            None
        }
    }

    fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Retryable(failure) => failure.status_code(),
            Self::Final(failure) => failure.status_code(),
        }
    }
}

impl RetryableFailure {
    fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Transport(_) => None,
            Self::Status { status, .. } => Some(*status),
        }
    }
}

impl FinalFailure {
    fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::InvalidRequest(_) => None,
            Self::Status { status, .. } => Some(*status),
        }
    }
}

/// Error of [`Sender::send_with_retry`]: a `RetriesExhausted` holding a `404` cannot be built.
#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum SendError {
    #[error(transparent)]
    Final(FinalFailure),
    #[error("{last} (gave up after {retries} retries)")]
    RetriesExhausted {
        retries: u32,
        last: RetryableFailure,
    },
}

impl SendError {
    fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Final(failure) => failure.status_code(),
            Self::RetriesExhausted { last, .. } => last.status_code(),
        }
    }
}

#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum HttpRequestError {
    /// A request that is retried.
    #[error(transparent)]
    Send(#[from] SendError),
    /// A request sent once: a `POST`, or a probe that falls back on failure.
    #[error(transparent)]
    Attempt(#[from] AttemptFailure),
    #[error("failed to read the HTTP {status} response body: {error}")]
    ReadBody {
        status: StatusCode,
        error: ReadBodyError,
    },
    #[error("failed to deserialize the HTTP {status} response body as JSON: {error}")]
    DeserializeJson {
        status: StatusCode,
        error: serde_json::Error,
    },
}

impl HttpRequestError {
    /// The status of the last response, `None` when no response arrived.
    #[must_use]
    pub fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Send(err) => err.status_code(),
            Self::Attempt(failure) => failure.status_code(),
            Self::ReadBody { status, .. } | Self::DeserializeJson { status, .. } => Some(*status),
        }
    }
}

/// Error of [`read_response_capped`].
#[derive(Debug, Error)]
#[allow(unreachable_pub)]
pub enum ReadBodyError {
    #[error("{}", ErrorChain(.0))]
    Read(reqwest::Error),
    // Names the property so an operator can tell which knob to raise, whichever
    // endpoint (JWKS, status list, policy store, ...) tripped it.
    // `read_so_far` is the declared `Content-Length` on the fast path and the
    // bytes received so far when streaming, so the message covers both.
    #[error(
        "response body exceeds the configured limit of {limit} bytes \
         ({read_so_far} bytes declared or received); raise \
         CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES to allow larger responses"
    )]
    TooLarge { limit: u64, read_so_far: u64 },
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

    /// Sends a request, retrying with backoff only network failures, `408`, `429` and
    /// `5xx`. Any other `4xx`, an invalid URL or a refused redirect would fail the same
    /// way again, so it returns at once instead of holding startup through the backoff.
    ///
    /// The `RequestBuilder` is re-created for each attempt because it cannot be reused.
    pub(crate) async fn send_with_retry<F>(
        &mut self,
        mut request: F,
    ) -> Result<reqwest::Response, SendError>
    where
        F: FnMut() -> RequestBuilder,
    {
        self.backoff.reset();
        let mut retries = 0u32;

        loop {
            // TODO: add optional debug-level logging hook here once a logger can be
            //       passed in without pulling logging into this low-level crate.
            let last = match AttemptFailure::check(request().send().await) {
                Ok(response) => return Ok(response),
                Err(AttemptFailure::Final(failure)) => return Err(SendError::Final(failure)),
                Err(AttemptFailure::Retryable(failure)) => failure,
            };
            if self.backoff.snooze().await.is_err() {
                return Err(SendError::RetriesExhausted { retries, last });
            }
            retries += 1;
        }
    }

    /// Sends an HTTP request a single time and deserializes the JSON response.
    ///
    /// Unlike [`Self::send`], this method performs **no retries** — useful for
    /// non-idempotent requests (POST, PUT, PATCH, DELETE) where replaying the request
    /// is unsafe.
    pub(crate) async fn send_once<T, F>(&mut self, request: F) -> Result<T, HttpRequestError>
    where
        F: FnOnce() -> RequestBuilder,
        T: serde::de::DeserializeOwned,
    {
        let response = AttemptFailure::check(request().send().await)?;
        self.read_json(response).await
    }

    /// Sends an HTTP request with the retry rule of [`Self::send_with_retry`], then
    /// deserializes the JSON response.
    pub(crate) async fn send<T, F>(&mut self, request: F) -> Result<T, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
        T: serde::de::DeserializeOwned,
    {
        let response = self.send_with_retry(request).await?;
        self.read_json(response).await
    }

    /// Sends an HTTP request with retry logic and returns the response body as raw bytes.
    #[cfg(test)]
    pub(crate) async fn send_bytes<F>(&mut self, request: F) -> Result<Vec<u8>, HttpRequestError>
    where
        F: FnMut() -> RequestBuilder,
    {
        let response = self.send_with_retry(request).await?;
        read_body(response, self.max_response_size).await
    }

    async fn read_json<T>(&self, response: reqwest::Response) -> Result<T, HttpRequestError>
    where
        T: serde::de::DeserializeOwned,
    {
        let status = response.status();
        let bytes = read_body(response, self.max_response_size).await?;
        serde_json::from_slice::<T>(&bytes)
            .map_err(|error| HttpRequestError::DeserializeJson { status, error })
    }
}

/// [`read_response_capped`], keeping the status of the response in the error.
pub(crate) async fn read_body(
    response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<Vec<u8>, HttpRequestError> {
    let status = response.status();
    read_response_capped(response, max_response_size)
        .await
        .map_err(|error| HttpRequestError::ReadBody { status, error })
}

/// Reads a response body into memory, aborting with `TooLarge` when
/// `max_response_size` is exceeded. Rejects upfront on `Content-Length` and
/// also enforces the cap chunk-by-chunk so chunked or compressed responses
/// can't slip past. `None` disables the cap.
pub(crate) async fn read_response_capped(
    #[cfg_attr(target_arch = "wasm32", allow(unused_mut))] mut response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<Vec<u8>, ReadBodyError> {
    if let Some(limit) = max_response_size
        && let Some(declared) = response.content_length()
        && declared > limit
    {
        return Err(ReadBodyError::TooLarge {
            limit,
            read_so_far: declared,
        });
    }

    // Content-Length is the encoded size; the browser decompresses transparently,
    // so the decoded stream must be capped as it is read.
    #[cfg(target_arch = "wasm32")]
    {
        use futures::StreamExt;

        let mut stream = response.bytes_stream();
        let mut buf: Vec<u8> = Vec::new();
        while let Some(chunk) = stream.next().await {
            let chunk = chunk.map_err(ReadBodyError::Read)?;
            if let Some(limit) = max_response_size {
                let new_len = (buf.len() as u64).saturating_add(chunk.len() as u64);
                if new_len > limit {
                    return Err(ReadBodyError::TooLarge {
                        limit,
                        read_so_far: new_len,
                    });
                }
            }
            buf.extend_from_slice(&chunk);
        }

        Ok(buf)
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
                            return Err(ReadBodyError::TooLarge {
                                limit,
                                read_so_far: new_len,
                            });
                        }
                    }
                    buf.extend_from_slice(&chunk);
                },
                Ok(None) => break,
                Err(e) => return Err(ReadBodyError::Read(e)),
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
            matches!(err, ReadBodyError::TooLarge { limit: 1024, .. }),
            "body over the 1024-byte cap must surface as TooLarge {{ limit: 1024, .. }}, got {err:?}",
        );
        assert!(
            err.to_string()
                .contains("CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES"),
            "the error must name the property that controls the cap, got: {err}"
        );
    }

    #[tokio::test]
    async fn truncated_body_is_a_read_error_with_the_status() {
        // We want a real `ReadBodyError::Read`: response status + headers
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
        let err = read_body(resp, None)
            .await
            .expect_err("a body cut short must fail to read");
        server.join().ok();
        assert!(
            matches!(
                err,
                HttpRequestError::ReadBody {
                    status: StatusCode::OK,
                    error: ReadBodyError::Read(_),
                }
            ),
            "a truncated body must be a ReadBody read error keeping the 200 status, got {err:?}",
        );
    }
}
