// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Where TRACE records go and the token they go with. Bulk requests go over REST
//! (`POST /audit/trace/bulk`) or, with `CEDARLING_TRACE_TRANSPORT=grpc`, over
//! gRPC (`TraceService/SubmitBulkRecords`), which runs the same Lock pipeline.

use std::fmt;
use std::time::Duration;

use async_trait::async_trait;
use bytes::Bytes;
use reqwest::StatusCode;
use serde::{Deserialize, Deserializer};
use tokio::sync::RwLock;

use super::LockAccessToken;
use super::register_client::{TokenSource, request_access_token};
use crate::TraceEndpoint;
use crate::http::HttpClient;

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod rest;

/// Acceptance flags that point at a problem on the producer's side. Coverage
/// gaps and lateness are expected after a loss or a retry, so they are not kept.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ProblemFlag {
    ChainLinkFailure,
    Equivocation,
    PolicyMismatch,
    ReservedMember,
}

impl ProblemFlag {
    const ALL: [Self; 4] = [
        Self::ChainLinkFailure,
        Self::Equivocation,
        Self::PolicyMismatch,
        Self::ReservedMember,
    ];

    /// The member of Lock's REST answer, which is also the name logged.
    fn name(self) -> &'static str {
        match self {
            Self::ChainLinkFailure => "chain_link_failure_flag",
            Self::Equivocation => "equivocation_flag",
            Self::PolicyMismatch => "policy_mismatch_flag",
            Self::ReservedMember => "reserved_member_flag",
        }
    }
}

/// Sends one bulk body to Lock. A trait so the emitter is tested without HTTP.
#[async_trait]
pub(crate) trait BulkSink: Send + Sync {
    /// Results of a processed request, in request order.
    async fn post_bulk(&self, body: &Bytes) -> Result<Vec<BulkItemResult>, PostBulkError>;
}

/// What Lock did with one record of a processed bulk request.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct BulkItemResult {
    pub(crate) index: usize,
    pub(crate) accepted: bool,
    /// Error id from the Lock error catalog when the record was rejected.
    pub(crate) error: Option<String>,
    pub(crate) reason: Option<String>,
    /// Names of the [`ProblemFlag`]s Lock set on an accepted record.
    pub(crate) flags: Vec<&'static str>,
}

/// Why a bulk request has no per-record results.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub(crate) enum PostBulkError {
    /// The request did not reach Lock, Lock failed, or Lock or a proxy before
    /// it asks to repeat it: the same bytes may go again.
    #[error("TRACE bulk request failed: {reason}")]
    Retryable {
        reason: String,
        /// How long Lock or the proxy asked to wait, if it did.
        retry_after: Option<Duration>,
    },
    /// Lock refused the whole request; the same bytes get the same answer.
    #[error(
        "Lock Server rejected the TRACE bulk request with {status} (error: {}, reason: {})",
        error.as_deref().unwrap_or("none"),
        reason.as_deref().unwrap_or("none")
    )]
    Rejected {
        status: RejectedStatus,
        /// Error id from the Lock error catalog.
        error: Option<String>,
        reason: Option<String>,
    },
    /// Lock answered that it processed the request, but the per-record results
    /// cannot be read. Lock may have kept the records, so they are not sent again.
    #[error("Lock Server's answer to the TRACE bulk request cannot be read: {reason}")]
    UnreadableResponse { reason: String },
}

/// The status a whole bulk request was refused with, in the terms of the
/// transport that carried it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RejectedStatus {
    Http(StatusCode),
    /// Lock sends the gRPC status matching the HTTP status of the REST answer.
    #[cfg(feature = "grpc")]
    Grpc(tonic::Code),
}

impl RejectedStatus {
    /// Lock's answer to a request it could not accept as sent (`invalid_request`).
    fn is_bad_request(self) -> bool {
        match self {
            Self::Http(status) => status == StatusCode::BAD_REQUEST,
            #[cfg(feature = "grpc")]
            Self::Grpc(code) => code == tonic::Code::InvalidArgument,
        }
    }

    /// gRPC refused the message before Lock read it, so no Lock error comes with
    /// it: over the direct gRPC port's limit, or a server or proxy slowing the
    /// client down (`ENHANCE_YOUR_CALM`).
    #[cfg(feature = "grpc")]
    fn is_over_grpc_limit(self) -> bool {
        self == Self::Grpc(tonic::Code::ResourceExhausted)
    }

    /// No `TraceService` behind the address: Lock answers `UNIMPLEMENTED`, and a
    /// proxy's `404` reaches tonic as `UNIMPLEMENTED` too.
    #[cfg(feature = "grpc")]
    fn is_missing_route(self) -> bool {
        self == Self::Grpc(tonic::Code::Unimplemented)
    }
}

impl fmt::Display for RejectedStatus {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Http(status) => write!(f, "HTTP {}", status.as_u16()),
            #[cfg(feature = "grpc")]
            Self::Grpc(code) => write!(f, "gRPC {code:?}"),
        }
    }
}

/// Why Lock refused a whole bulk request Cedarling built, when the fix is in
/// the configuration of Lock or Cedarling.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RequestRefusal {
    /// More records than Lock's `maxBulkRecords`.
    TooManyRecords,
    /// A body over Lock's `maxBulkRequestBytes`.
    BodyTooLarge,
    /// `invalid_request` without a reason: Lock hides it unless its
    /// `errorReasonEnabled` is on, the default being off.
    Unexplained,
    /// No TRACE gRPC route: a Lock Server older than its TRACE gRPC services,
    /// or a proxy without the `io.jans.lock.trace.TraceService` route.
    #[cfg(feature = "grpc")]
    NoGrpcRoute,
    /// gRPC `RESOURCE_EXHAUSTED`: the body may be over a gRPC message limit,
    /// or a server or proxy is slowing Cedarling down.
    #[cfg(feature = "grpc")]
    GrpcLimit,
}

impl PostBulkError {
    /// `None` for a refusal whose error and reason already say what is wrong.
    pub(crate) fn refusal(&self) -> Option<RequestRefusal> {
        let Self::Rejected {
            status,
            error,
            reason,
        } = self
        else {
            return None;
        };
        #[cfg(feature = "grpc")]
        if status.is_over_grpc_limit() {
            return Some(RequestRefusal::GrpcLimit);
        }
        #[cfg(feature = "grpc")]
        if status.is_missing_route() {
            return Some(RequestRefusal::NoGrpcRoute);
        }
        if !status.is_bad_request() || error.as_deref() != Some("invalid_request") {
            return None;
        }
        match reason.as_deref() {
            Some("max_bulk_records_exceeded") => Some(RequestRefusal::TooManyRecords),
            Some("bulk_body_too_large") => Some(RequestRefusal::BodyTooLarge),
            None => Some(RequestRefusal::Unexplained),
            Some(_) => None,
        }
    }
}

/// How bulk requests reach Lock: one adapter per transport, each turning its
/// transport's answer into an [`Attempt`] or a [`PostBulkError`].
pub(super) enum TraceChannel {
    /// `POST` of the body to the TRACE bulk endpoint.
    Rest(rest::RestTraceClient),
    /// `TraceService/SubmitBulkRecords` on the origin of the TRACE endpoint,
    /// where the Lock proxy routes it, as for the audit gRPC transport.
    #[cfg(feature = "grpc")]
    Grpc(grpc::GrpcTraceClient),
}

/// Test failures name the transport only, so no endpoint is printed.
#[cfg(test)]
impl fmt::Debug for TraceChannel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Rest(_) => f.write_str("Rest"),
            #[cfg(feature = "grpc")]
            Self::Grpc(_) => f.write_str("Grpc"),
        }
    }
}

impl TraceChannel {
    /// A token Lock refused again after it was reissued, or that cannot be reissued.
    fn unauthorized(&self) -> PostBulkError {
        let status = match self {
            Self::Rest(_) => RejectedStatus::Http(StatusCode::UNAUTHORIZED),
            #[cfg(feature = "grpc")]
            Self::Grpc(_) => RejectedStatus::Grpc(tonic::Code::Unauthenticated),
        };
        PostBulkError::Rejected {
            status,
            error: None,
            reason: None,
        }
    }
}

/// What the emitter sends TRACE bulk requests through. It owns the token and
/// reissues it after a `401`, whatever the transport; the channel only sends.
pub(crate) struct TraceSink {
    pub(super) channel: TraceChannel,
    /// Reissues the token.
    pub(super) http: HttpClient,
    /// Shared by every bulk request until Lock answers `401`; then replaced by
    /// the reissued token, which the following requests use in turn.
    pub(super) token: RwLock<LockAccessToken>,
    /// `None` when the token was given as is and cannot be reissued.
    pub(super) source: Option<TokenSource>,
}

#[cfg(test)]
impl fmt::Debug for TraceSink {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("TraceSink")
            .field("channel", &self.channel)
            .field("source", &self.source)
            .finish_non_exhaustive()
    }
}

/// Answer to one attempt, before a `401` (gRPC `UNAUTHENTICATED`) is handled.
enum Attempt {
    Results(Vec<BulkItemResult>),
    Unauthorized,
}

#[async_trait]
impl BulkSink for TraceSink {
    /// The token is not requested per call: the stored one is used, and a new one
    /// is requested only when Lock answers `401`, i.e. about once per token
    /// lifetime. Then the same bytes go again, once; a second `401` is final, so
    /// a token Lock never accepts (e.g. without `trace.write`) costs one extra
    /// token request per call, not a loop. A pre-issued token cannot be
    /// reissued, so its `401` is final at once.
    async fn post_bulk(&self, body: &Bytes) -> Result<Vec<BulkItemResult>, PostBulkError> {
        if let Attempt::Results(results) = self.post_once(body).await? {
            return Ok(results);
        }
        let Some(source) = &self.source else {
            return Err(self.channel.unauthorized());
        };
        // Stored for the following calls, so they do not hit `401` again.
        let token = request_access_token(&self.http, source)
            .await
            .map_err(|err| PostBulkError::Retryable {
                reason: format!("reissuing the Lock Server token failed: {err}"),
                retry_after: None,
            })?;
        *self.token.write().await = LockAccessToken(token);
        match self.post_once(body).await? {
            Attempt::Results(results) => Ok(results),
            Attempt::Unauthorized => Err(self.channel.unauthorized()),
        }
    }
}

impl TraceSink {
    async fn post_once(&self, body: &Bytes) -> Result<Attempt, PostBulkError> {
        let token = self.token.read().await.0.clone();
        match &self.channel {
            TraceChannel::Rest(client) => client.submit(&token, body).await,
            #[cfg(feature = "grpc")]
            TraceChannel::Grpc(client) => client.submit(&token, body).await,
        }
    }
}

fn retryable(reason: String) -> PostBulkError {
    PostBulkError::Retryable {
        reason,
        retry_after: None,
    }
}

/// Lock's structured error, for a rejected record or a rejected request.
#[derive(Deserialize)]
struct ErrorBody {
    error: String,
    #[serde(default, deserialize_with = "non_empty")]
    reason: Option<String>,
}

/// Lock sends `""` for a reason it hides (`errorReasonEnabled` off), which is
/// no reason.
fn non_empty<'de, D: Deserializer<'de>>(deserializer: D) -> Result<Option<String>, D::Error> {
    Ok(Option::<String>::deserialize(deserializer)?.filter(|reason| !reason.is_empty()))
}

/// Why Cedarling has no endpoint to send TRACE records to. TRACE is not needed
/// to decide, so each is a startup warning rather than a failure.
#[derive(Debug, thiserror::Error)]
pub(super) enum NoTraceEndpoint {
    #[error(
        "Lock Server does not publish trace_bulk_endpoint and CEDARLING_TRACE_ENDPOINT is not \
         set: TRACE records will not be sent"
    )]
    NotPublished,
    #[error(
        "CEDARLING_TRACE_ENDPOINT on {origin} (path, query and credentials are not logged) \
         must use https, or http only to a loopback host, because the Lock access token goes \
         with every request: TRACE records will not be sent"
    )]
    InsecureExplicit { origin: String },
    #[error(
        "Lock Server publishes trace_bulk_endpoint on {origin} (path, query and credentials \
         are not logged), plain http to a non-loopback host, and the Lock access token goes \
         with every request: TRACE records will not be sent. Set the Lock Server baseEndpoint \
         to https, or set CEDARLING_TRACE_ENDPOINT to an https endpoint"
    )]
    InsecureDiscovered { origin: String },
}

/// The explicit endpoint wins over discovery. The bulk request carries the Lock
/// access token, so it goes over `https`, or over `http` only to a loopback
/// host, as the log transport does.
pub(super) fn resolve_trace_endpoint(
    endpoint: &TraceEndpoint,
    discovered: Option<&url::Url>,
) -> Result<url::Url, NoTraceEndpoint> {
    let url = match endpoint {
        TraceEndpoint::Url(url) => url,
        TraceEndpoint::Discovered => discovered.ok_or(NoTraceEndpoint::NotPublished)?,
    };
    if crate::http::is_secure_transport(url) {
        return Ok(url.clone());
    }
    // The warning names only where the endpoint points: credentials, a path or a
    // query may carry what the log must not, and the message says they are left out.
    let host = url.host_str().unwrap_or_default();
    let origin = match url.port() {
        Some(port) => format!("{}://{host}:{port}", url.scheme()),
        None => format!("{}://{host}", url.scheme()),
    };
    Err(match endpoint {
        TraceEndpoint::Url(_) => NoTraceEndpoint::InsecureExplicit { origin },
        TraceEndpoint::Discovered => NoTraceEndpoint::InsecureDiscovered { origin },
    })
}

/// Helpers the facade's and both adapters' tests share.
#[cfg(test)]
mod test_support {
    use mockito::{Mock, ServerGuard};
    use serde_json::json;
    use tokio::sync::RwLock;

    use super::{TraceChannel, TraceSink};
    use crate::HttpClientConfig;
    use crate::http::HttpClient;
    use crate::lock::LockAccessToken;
    use crate::lock::register_client::{CredentialEndpoint, TokenSource};

    pub(super) fn http() -> HttpClient {
        HttpClient::new(HttpClientConfig::default()).expect("HTTP client")
    }

    /// A sink holding `old-token`, which a test's Lock may refuse.
    pub(super) fn sink(channel: TraceChannel, source: Option<TokenSource>) -> TraceSink {
        TraceSink {
            channel,
            http: http(),
            token: RwLock::new(LockAccessToken("old-token".to_string())),
            source,
        }
    }

    /// Reissues `new-token` from `server`'s `/token`.
    pub(super) fn source(server: &ServerGuard) -> TokenSource {
        TokenSource {
            token_endpoint: CredentialEndpoint::new(
                "token_endpoint",
                format!("{}/token", server.url())
                    .parse()
                    .expect("valid URL"),
            )
            .expect("mockito serves on a loopback host"),
            client_id: "cedarling-client".to_string(),
            client_secret: "secret".to_string(),
            scope: "https://jans.io/oauth/lock/trace.write".to_string(),
        }
    }

    pub(super) fn token_endpoint(server: &mut ServerGuard, expected_calls: usize) -> Mock {
        server
            .mock("POST", "/token")
            .with_body(json!({"access_token": "new-token"}).to_string())
            .expect(expected_calls)
            .create()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn url(raw: &str) -> url::Url {
        raw.parse().expect("valid URL")
    }

    #[test]
    fn https_and_loopback_http_endpoints_are_used() {
        for raw in [
            "https://lock.example.com/audit/trace/bulk",
            "http://localhost:8080/audit/trace/bulk",
            "http://127.0.0.1:8080/audit/trace/bulk",
            "http://[::1]:8080/audit/trace/bulk",
        ] {
            let explicit = resolve_trace_endpoint(&TraceEndpoint::Url(url(raw)), None)
                .expect("an explicit https or loopback endpoint should be used");
            assert_eq!(explicit, url(raw), "explicit {raw} should be used as is");
            let discovered = resolve_trace_endpoint(&TraceEndpoint::Discovered, Some(&url(raw)))
                .expect("a discovered https or loopback endpoint should be used");
            assert_eq!(
                discovered,
                url(raw),
                "discovered {raw} should be used as is"
            );
        }
    }

    #[test]
    fn explicit_endpoint_without_tls_to_remote_host_is_refused() {
        for raw in [
            "http://lock.example.com/audit/trace/bulk",
            "ftp://lock.example.com/audit/trace/bulk",
        ] {
            let err = resolve_trace_endpoint(
                &TraceEndpoint::Url(url(raw)),
                Some(&url("https://lock.example.com/audit/trace/bulk")),
            )
            .expect_err("the token must not go to a remote host without TLS");
            assert!(
                matches!(err, NoTraceEndpoint::InsecureExplicit { ref origin } if raw.starts_with(origin.as_str())),
                "expected InsecureExplicit for {raw}, got {err:?}"
            );
        }
    }

    #[test]
    fn discovered_endpoint_without_tls_to_remote_host_is_refused() {
        let err = resolve_trace_endpoint(
            &TraceEndpoint::Discovered,
            Some(&url("http://lock.example.com/audit/trace/bulk")),
        )
        .expect_err("the token must not go to a remote host without TLS");
        assert!(
            matches!(err, NoTraceEndpoint::InsecureDiscovered { ref origin } if origin == "http://lock.example.com"),
            "expected InsecureDiscovered, got {err:?}"
        );
    }

    #[test]
    fn insecure_endpoint_message_hides_path_and_credentials() {
        let err = resolve_trace_endpoint(
            &TraceEndpoint::Url(url("http://user:secret@lock.example.com:8080/bulk?key=1")),
            None,
        )
        .expect_err("the token must not go to a remote host without TLS");
        let message = err.to_string();
        assert!(
            !message.contains("secret") && !message.contains("/bulk") && !message.contains("key=1"),
            "the warning should name the scheme, host and port only, got {message}"
        );
        assert!(
            message.contains("http://lock.example.com:8080")
                && message.contains("path, query and credentials are not logged"),
            "the warning should say the shown endpoint is cut, got {message}"
        );
    }

    #[test]
    fn discovery_without_published_endpoint_has_none() {
        let err = resolve_trace_endpoint(&TraceEndpoint::Discovered, None)
            .expect_err("Lock publishes no endpoint");
        assert!(
            matches!(err, NoTraceEndpoint::NotPublished),
            "expected NotPublished, got {err:?}"
        );
    }

    fn refused(status: StatusCode, error: Option<&str>, reason: Option<&str>) -> PostBulkError {
        PostBulkError::Rejected {
            status: RejectedStatus::Http(status),
            error: error.map(str::to_string),
            reason: reason.map(str::to_string),
        }
    }

    #[test]
    fn refusal_tells_limits_and_a_hidden_reason_from_the_rest() {
        let invalid = Some("invalid_request");
        assert_eq!(
            refused(
                StatusCode::BAD_REQUEST,
                invalid,
                Some("max_bulk_records_exceeded")
            )
            .refusal(),
            Some(RequestRefusal::TooManyRecords),
            "too many records"
        );
        assert_eq!(
            refused(
                StatusCode::BAD_REQUEST,
                invalid,
                Some("bulk_body_too_large")
            )
            .refusal(),
            Some(RequestRefusal::BodyTooLarge),
            "a body too large"
        );
        assert_eq!(
            refused(StatusCode::BAD_REQUEST, invalid, None).refusal(),
            Some(RequestRefusal::Unexplained),
            "invalid_request without a reason"
        );
        assert_eq!(
            refused(StatusCode::BAD_REQUEST, invalid, Some("trace_disabled")).refusal(),
            None,
            "a reason that explains itself"
        );
        assert_eq!(
            refused(StatusCode::BAD_REQUEST, Some("invalid_signature"), None).refusal(),
            None,
            "another error id"
        );
        assert_eq!(
            refused(StatusCode::FORBIDDEN, invalid, None).refusal(),
            None,
            "another status"
        );
        assert_eq!(
            refused(StatusCode::BAD_REQUEST, None, None).refusal(),
            None,
            "a body that is not a Lock error"
        );
        assert_eq!(
            retryable("Lock is down".to_string()).refusal(),
            None,
            "a retryable failure"
        );
    }

    #[test]
    fn rejection_message_names_the_http_status() {
        let message = refused(StatusCode::FORBIDDEN, Some("access_denied"), None).to_string();
        assert_eq!(
            message,
            "Lock Server rejected the TRACE bulk request with HTTP 403 (error: access_denied, \
             reason: none)",
            "the logged rejection should name the transport and its status"
        );
    }
}
