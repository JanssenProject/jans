// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! How a failed HTTP attempt is sorted, and the errors of the HTTP client.

use std::error::Error as StdError;
use std::fmt::{self, Display};

use reqwest::{StatusCode, Url};
use thiserror::Error;

use crate::common::retry::{Attempt, RetryError};

/// Shows an error with its causes: `reqwest`'s own message leaves them out, so an
/// invalid URL would read only "builder error".
pub(super) struct ErrorChain<'a>(&'a (dyn StdError + 'static));

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

/// A URL as an error message shows it: without userinfo, query or fragment, which
/// can carry credentials (a token, a presigned URL's signature).
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct RedactedUrl(Url);

impl RedactedUrl {
    pub(crate) fn new(url: &Url) -> Self {
        let mut url = url.clone();
        redact(&mut url);
        Self(url)
    }
}

impl Display for RedactedUrl {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        Display::fmt(&self.0, f)
    }
}

fn redact(url: &mut Url) {
    // Both fail only for a URL without a host, which has no userinfo to remove.
    url.set_username("").ok();
    url.set_password(None).ok();
    url.set_query(None);
    url.set_fragment(None);
}

/// `err` with its URL redacted, since `reqwest` prints the URL in its message.
pub(super) fn redacted(mut err: reqwest::Error) -> reqwest::Error {
    if let Some(url) = err.url_mut() {
        redact(url);
    }
    err
}

/// One attempt that got no usable response, sorted once by whether repeating can help.
#[derive(Debug, Error)]
pub(crate) enum AttemptFailure {
    #[error(transparent)]
    Retryable(RetryableFailure),
    #[error(transparent)]
    Final(FinalFailure),
}

/// A failure the same request may not repeat: the network or the server may recover.
#[derive(Debug, Error)]
pub(crate) enum RetryableFailure {
    /// Connect, timeout or dropped connection.
    #[error("{}", ErrorChain(.0))]
    Transport(reqwest::Error),
    /// `408`, `429` or `5xx`.
    #[error("HTTP {status} from {url}")]
    Status {
        status: StatusCode,
        url: Box<RedactedUrl>,
    },
}

/// A failure the same request repeats on every attempt.
#[derive(Debug, Error)]
pub(crate) enum FinalFailure {
    /// A request that cannot be built (invalid URL) or a redirect that is refused.
    #[error("invalid request: {}", ErrorChain(.0))]
    InvalidRequest(reqwest::Error),
    /// Any `4xx` other than `408` and `429`.
    #[error("HTTP {status} from {url}")]
    Status {
        status: StatusCode,
        url: Box<RedactedUrl>,
    },
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

    pub(crate) fn from_send_error(err: reqwest::Error) -> Self {
        let err = redacted(err);
        if err.is_builder() || err.is_redirect() {
            Self::Final(FinalFailure::InvalidRequest(err))
        } else {
            Self::Retryable(RetryableFailure::Transport(err))
        }
    }

    /// `None` for a response that is not a failure. `408` and `429` come from a proxy
    /// as often as from the server: both ask to repeat the request later.
    pub(crate) fn from_response(response: &reqwest::Response) -> Option<Self> {
        let status = response.status();
        // Built only for a failure: a successful response needs no URL.
        let url = || Box::new(RedactedUrl::new(response.url()));
        if status.is_server_error()
            || status == StatusCode::TOO_MANY_REQUESTS
            || status == StatusCode::REQUEST_TIMEOUT
        {
            Some(Self::Retryable(RetryableFailure::Status {
                status,
                url: url(),
            }))
        } else if status.is_client_error() {
            Some(Self::Final(FinalFailure::Status { status, url: url() }))
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

/// Error of [`crate::http::HttpClient`]'s retried requests: a `RetriesExhausted` holding a
/// `404` cannot be built.
pub(crate) type SendError = RetryError<RetryableFailure, FinalFailure>;

impl SendError {
    fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Final(failure) => failure.status_code(),
            Self::RetriesExhausted { last, .. } => last.status_code(),
        }
    }
}

#[derive(Debug, Error)]
pub(crate) enum HttpClientError {
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

/// Why an HTTP request failed, as Cedarling's public errors carry it. Only its
/// message and cause are public, so the cases it holds can change without
/// changing the public API.
#[derive(Debug, Error)]
#[error(transparent)]
pub struct HttpRequestError(HttpClientError);

impl HttpRequestError {
    /// The failure, for code inside the crate that sorts it.
    pub(crate) fn kind(&self) -> &HttpClientError {
        &self.0
    }
}

impl From<HttpClientError> for HttpRequestError {
    fn from(error: HttpClientError) -> Self {
        Self(error)
    }
}

impl HttpClientError {
    /// The status of the last response, `None` when no response arrived.
    #[must_use]
    pub(crate) fn status_code(&self) -> Option<StatusCode> {
        match self {
            Self::Send(err) => err.status_code(),
            Self::Attempt(failure) => failure.status_code(),
            Self::ReadBody { status, .. } | Self::DeserializeJson { status, .. } => Some(*status),
        }
    }
}

/// Error of [`read_response_capped`].
#[derive(Debug, Error)]
pub(crate) enum ReadBodyError {
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

/// One attempt of a retried request, sorted for [`crate::common::retry::retry`].
pub(crate) fn attempt(
    result: Result<reqwest::Response, reqwest::Error>,
) -> Attempt<reqwest::Response, RetryableFailure, FinalFailure> {
    // TODO: add optional debug-level logging hook here once a logger can be
    //       passed in without pulling logging into this low-level crate.
    match AttemptFailure::check(result) {
        Ok(response) => Attempt::Done(response),
        Err(AttemptFailure::Retryable(error)) => Attempt::Retryable {
            error,
            wait_at_least: None,
        },
        Err(AttemptFailure::Final(failure)) => Attempt::Final(failure),
    }
}
