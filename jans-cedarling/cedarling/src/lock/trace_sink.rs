// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Where TRACE records go and the token they go with. TRACE always uses REST:
//! Lock has no TRACE endpoint over gRPC.

use std::fmt;
use std::time::Duration;

use async_trait::async_trait;
use bytes::Bytes;
use chrono::{DateTime, Utc};
use reqwest::StatusCode;
use reqwest::header::{CONTENT_TYPE, HeaderMap, RETRY_AFTER};
use serde::Deserialize;
use tokio::sync::RwLock;

use super::LockAccessToken;
use super::register_client::{TokenSource, request_access_token};
use crate::TraceEndpoint;
use crate::http::HttpClient;
use crate::http_utils::MAX_EXPONENTIAL_DELAY;

/// Acceptance flags that point at a problem on the producer's side. Coverage
/// gaps and lateness are expected after a loss or a retry, so they are not kept.
const PROBLEM_FLAGS: [&str; 4] = [
    "chain_link_failure_flag",
    "equivocation_flag",
    "policy_mismatch_flag",
    "reserved_member_flag",
];

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
    /// Names of the [`PROBLEM_FLAGS`] Lock set on an accepted record.
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
        "Lock Server rejected the TRACE bulk request with HTTP {status} (error: {}, reason: {})",
        error.as_deref().unwrap_or("none"),
        reason.as_deref().unwrap_or("none")
    )]
    Rejected {
        status: u16,
        /// Error id from the Lock error catalog.
        error: Option<String>,
        reason: Option<String>,
    },
}

/// The Lock TRACE bulk endpoint and the client that posts to it. Holds its own
/// copy of the token, so a `401` can swap in a reissued one.
pub(crate) struct TraceSink {
    pub(super) endpoint: url::Url,
    pub(super) http: HttpClient,
    /// Shared by every bulk request until Lock answers `401`; then replaced by
    /// the reissued token, which the following requests use in turn.
    pub(super) token: RwLock<LockAccessToken>,
    /// `None` when the token was given as is and cannot be reissued.
    pub(super) source: Option<TokenSource>,
}

impl fmt::Debug for TraceSink {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("TraceSink")
            .field("endpoint", &self.endpoint.as_str())
            .field("source", &self.source)
            .finish_non_exhaustive()
    }
}

/// Answer to one attempt, before a `401` is handled.
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
            return Err(unauthorized());
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
            Attempt::Unauthorized => Err(unauthorized()),
        }
    }
}

impl TraceSink {
    /// The bulk request carrying `body`. `Bytes` clones share the buffer, so a
    /// retry or a resend after `401` does not copy the batch.
    fn bulk_request(&self, token: String, body: &Bytes) -> reqwest::RequestBuilder {
        self.http
            .raw_client
            .post(self.endpoint.clone())
            .bearer_auth(token)
            .header(CONTENT_TYPE, "application/json")
            .body(body.clone())
    }

    async fn post_once(&self, body: &Bytes) -> Result<Attempt, PostBulkError> {
        let token = self.token.read().await.0.clone();
        let response = self
            .bulk_request(token, body)
            .send()
            .await
            .map_err(|err| retryable(err.to_string()))?;
        let status = response.status();
        if status == StatusCode::UNAUTHORIZED {
            return Ok(Attempt::Unauthorized);
        }
        // `429` and `408` come from a proxy before Lock as often as from Lock:
        // both ask to repeat the request later, not to drop it.
        if status.is_server_error()
            || status == StatusCode::TOO_MANY_REQUESTS
            || status == StatusCode::REQUEST_TIMEOUT
        {
            return Err(PostBulkError::Retryable {
                reason: format!("HTTP {status}"),
                retry_after: retry_after(response.headers(), Utc::now()),
            });
        }
        let bytes = self
            .http
            .read_response_capped(response)
            .await
            .map_err(|err| retryable(err.to_string()))?;
        if status == StatusCode::MULTI_STATUS {
            return parse_results(&bytes).map(Attempt::Results);
        }
        let error = serde_json::from_slice::<ErrorBody>(&bytes).ok();
        Err(PostBulkError::Rejected {
            status: status.as_u16(),
            error: error.as_ref().map(|body| body.error.clone()),
            reason: error.and_then(|body| body.reason),
        })
    }
}

fn retryable(reason: String) -> PostBulkError {
    PostBulkError::Retryable {
        reason,
        retry_after: None,
    }
}

/// How long `Retry-After` (RFC 9110, section 10.2.3) asks to wait: delay
/// seconds or an IMF-fixdate. The value comes from Lock or from any proxy
/// before it, so a missing, unreadable or past one leaves the wait to the
/// backoff, and a longer one is cut to the longest backoff delay: a
/// misconfigured proxy cannot stop TRACE for hours.
fn retry_after(headers: &HeaderMap, now: DateTime<Utc>) -> Option<Duration> {
    let value = headers.get(RETRY_AFTER)?.to_str().ok()?.trim();
    let delay = match value.parse::<u64>() {
        Ok(seconds) => Duration::from_secs(seconds),
        Err(_) => DateTime::parse_from_rfc2822(value)
            .ok()?
            .with_timezone(&Utc)
            .signed_duration_since(now)
            .to_std()
            .ok()?,
    };
    Some(delay.min(MAX_EXPONENTIAL_DELAY))
}

fn unauthorized() -> PostBulkError {
    PostBulkError::Rejected {
        status: StatusCode::UNAUTHORIZED.as_u16(),
        error: None,
        reason: None,
    }
}

fn parse_results(bytes: &[u8]) -> Result<Vec<BulkItemResult>, PostBulkError> {
    let response: BulkResponse =
        serde_json::from_slice(bytes).map_err(|err| PostBulkError::Rejected {
            status: StatusCode::MULTI_STATUS.as_u16(),
            error: None,
            reason: Some(format!("unreadable bulk response: {err}")),
        })?;
    Ok(response
        .results
        .into_iter()
        .map(BulkItemResult::from)
        .collect())
}

/// Lock's `207` body.
#[derive(Deserialize)]
struct BulkResponse {
    results: Vec<RawItem>,
}

#[derive(Deserialize)]
struct RawItem {
    index: usize,
    accepted: bool,
    #[serde(default)]
    error: Option<ErrorBody>,
    #[serde(flatten)]
    members: serde_json::Map<String, serde_json::Value>,
}

/// Lock's structured error, for a rejected record or a rejected request.
#[derive(Deserialize)]
struct ErrorBody {
    error: String,
    #[serde(default)]
    reason: Option<String>,
}

impl From<RawItem> for BulkItemResult {
    fn from(item: RawItem) -> Self {
        let flags = PROBLEM_FLAGS
            .into_iter()
            .filter(|flag| item.members.get(*flag) == Some(&serde_json::Value::Bool(true)))
            .collect();
        let (error, reason) = match item.error {
            Some(body) => (Some(body.error), body.reason),
            None => (None, None),
        };
        Self {
            index: item.index,
            accepted: item.accepted,
            error,
            reason,
            flags,
        }
    }
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
    let secure = match url.scheme() {
        "https" => true,
        "http" => super::is_loopback_host(url),
        _ => false,
    };
    if secure {
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

#[cfg(test)]
mod tests {
    use mockito::{Matcher, Mock, Server, ServerGuard};
    use serde_json::json;

    use super::*;
    use crate::HttpClientConfig;

    const BODY: &str = r#"[{"producer":"cedarling-fleet-1"},{"producer":"cedarling-fleet-1"}]"#;
    const BULK_PATH: &str = "/jans-lock/api/v1/audit/trace/bulk";

    fn bulk_body() -> Bytes {
        Bytes::from_static(BODY.as_bytes())
    }

    fn sink(server: &ServerGuard, source: Option<TokenSource>) -> TraceSink {
        TraceSink {
            endpoint: format!("{}{BULK_PATH}", server.url())
                .parse()
                .expect("valid URL"),
            http: HttpClient::new(HttpClientConfig::default()).expect("HTTP client"),
            token: RwLock::new(LockAccessToken("old-token".to_string())),
            source,
        }
    }

    fn source(server: &ServerGuard) -> TokenSource {
        TokenSource {
            token_endpoint: format!("{}/token", server.url()),
            client_id: "cedarling-client".to_string(),
            client_secret: "secret".to_string(),
            scope: "https://jans.io/oauth/lock/trace.write".to_string(),
        }
    }

    fn bulk(server: &mut ServerGuard, token: &str, status: usize, body: &str) -> Mock {
        server
            .mock("POST", BULK_PATH)
            .match_header("authorization", format!("Bearer {token}").as_str())
            .match_body(Matcher::Exact(BODY.to_string()))
            .with_status(status)
            .with_body(body)
            .create()
    }

    fn token_endpoint(server: &mut ServerGuard, expected_calls: usize) -> Mock {
        server
            .mock("POST", "/token")
            .with_body(json!({"access_token": "new-token"}).to_string())
            .expect(expected_calls)
            .create()
    }

    #[tokio::test]
    async fn every_attempt_sends_the_body_without_copying_it() {
        let server = Server::new_async().await;
        let sink = sink(&server, None);
        let body = bulk_body();

        for attempt in ["first", "repeated"] {
            let request = sink
                .bulk_request("token".to_string(), &body)
                .build()
                .expect("the bulk request builds");
            let sent = request
                .body()
                .and_then(reqwest::Body::as_bytes)
                .expect("the bulk body is in memory");
            assert_eq!(
                sent.as_ptr(),
                body.as_ptr(),
                "the {attempt} attempt should send the batch's bytes, not a copy"
            );
        }
    }

    #[tokio::test]
    async fn multi_status_maps_results_in_order() {
        let mut server = Server::new_async().await;
        let results = json!({"results": [
            {"index": 0, "accepted": true, "policy_mismatch_flag": true,
             "chain_link_failure_flag": false, "coverage_gap_flag": true},
            {"index": 1, "accepted": false,
             "error": {"error": "invalid_signature", "reason": "signature_mismatch"}},
        ]});
        let _bulk = bulk(&mut server, "old-token", 207, &results.to_string());

        let items = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect("a 207 response should be read per record");

        assert_eq!(
            items,
            vec![
                BulkItemResult {
                    index: 0,
                    accepted: true,
                    error: None,
                    reason: None,
                    flags: vec!["policy_mismatch_flag"],
                },
                BulkItemResult {
                    index: 1,
                    accepted: false,
                    error: Some("invalid_signature".to_string()),
                    reason: Some("signature_mismatch".to_string()),
                    flags: vec![],
                },
            ],
            "results keep request order; only flags that point at a problem are kept"
        );
    }

    #[tokio::test]
    async fn multi_status_with_bad_body_is_rejected() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(&mut server, "old-token", 207, "not json");

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("an unreadable 207 body should be an error");
        assert!(
            matches!(err, PostBulkError::Rejected { status: 207, .. }),
            "expected Rejected {{ status: 207 }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn server_error_is_retryable() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(&mut server, "old-token", 503, "");

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a 5xx response should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::Retryable {
                    retry_after: None,
                    ..
                }
            ),
            "expected Retryable without Retry-After, got {err:?}"
        );
    }

    #[tokio::test]
    async fn too_many_requests_and_timeout_are_retryable_with_retry_after() {
        for (status, header, expected) in [
            (429, Some("7"), Some(Duration::from_secs(7))),
            (408, None, None),
            (503, Some("120"), Some(Duration::from_secs(120))),
        ] {
            let mut server = Server::new_async().await;
            let mut mock = server.mock("POST", BULK_PATH).with_status(status);
            if let Some(header) = header {
                mock = mock.with_header("retry-after", header);
            }
            let _bulk = mock.create();

            let err = sink(&server, None)
                .post_bulk(&bulk_body())
                .await
                .expect_err("a request Lock or a proxy asks to repeat should be an error");
            assert!(
                matches!(
                    err,
                    PostBulkError::Retryable { retry_after, .. } if retry_after == expected
                ),
                "HTTP {status} with Retry-After {header:?} should be Retryable after {expected:?}, \
                 got {err:?}"
            );
        }
    }

    #[test]
    fn retry_after_reads_seconds_and_dates_and_ignores_the_rest() {
        let now = DateTime::parse_from_rfc2822("Mon, 26 Oct 2026 12:00:00 GMT")
            .expect("test date parses")
            .with_timezone(&Utc);
        for (value, expected) in [
            ("120", Some(120)),
            (" 5 ", Some(5)),
            ("0", Some(0)),
            ("Mon, 26 Oct 2026 12:01:30 GMT", Some(90)),
            ("Mon, 26 Oct 2026 11:59:00 GMT", None),
            ("999999", Some(MAX_EXPONENTIAL_DELAY.as_secs())),
            ("-5", None),
            ("1.5", None),
            ("soon", None),
            ("", None),
        ] {
            let mut headers = HeaderMap::new();
            headers.insert(
                RETRY_AFTER,
                value.parse().expect("test header value is valid"),
            );
            assert_eq!(
                retry_after(&headers, now),
                expected.map(Duration::from_secs),
                "Retry-After {value:?}"
            );
        }
        assert_eq!(
            retry_after(&HeaderMap::new(), now),
            None,
            "no Retry-After leaves the wait to the backoff"
        );
    }

    #[tokio::test]
    async fn unreachable_endpoint_is_retryable() {
        let server = Server::new_async().await;
        let mut sink = sink(&server, None);
        sink.endpoint = "http://127.0.0.1:1/bulk".parse().expect("valid URL");

        let err = sink
            .post_bulk(&bulk_body())
            .await
            .expect_err("a connection failure should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::Retryable {
                    retry_after: None,
                    ..
                }
            ),
            "expected Retryable without Retry-After, got {err:?}"
        );
    }

    #[tokio::test]
    async fn bad_request_is_rejected_with_error_id() {
        let mut server = Server::new_async().await;
        let body = json!({"error": "invalid_request", "reason": "bulk_body_too_large"});
        let _bulk = bulk(&mut server, "old-token", 400, &body.to_string());

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a 4xx response should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::Rejected { status: 400, ref error, ref reason }
                    if error.as_deref() == Some("invalid_request")
                        && reason.as_deref() == Some("bulk_body_too_large")
            ),
            "expected Rejected {{ 400, invalid_request, bulk_body_too_large }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn unauthorized_reissues_token_and_resends_same_body() {
        let mut server = Server::new_async().await;
        let old = bulk(&mut server, "old-token", 401, "").expect(1);
        let new = bulk(&mut server, "new-token", 207, r#"{"results": []}"#).expect(1);
        let token = token_endpoint(&mut server, 1);
        let source = source(&server);

        sink(&server, Some(source))
            .post_bulk(&bulk_body())
            .await
            .expect("a reissued token should get the same body through");

        old.assert_async().await;
        new.assert_async().await;
        token.assert_async().await;
    }

    #[tokio::test]
    async fn unauthorized_twice_is_rejected() {
        let mut server = Server::new_async().await;
        let old = bulk(&mut server, "old-token", 401, "").expect(1);
        let new = bulk(&mut server, "new-token", 401, "").expect(1);
        let token = token_endpoint(&mut server, 1);
        let source = source(&server);

        let err = sink(&server, Some(source))
            .post_bulk(&bulk_body())
            .await
            .expect_err("a 401 after reissue should be an error");

        assert!(
            matches!(err, PostBulkError::Rejected { status: 401, .. }),
            "expected Rejected {{ status: 401 }}, got {err:?}"
        );
        old.assert_async().await;
        new.assert_async().await;
        token.assert_async().await;
    }

    #[tokio::test]
    async fn unauthorized_without_token_source_is_rejected() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(&mut server, "old-token", 401, "").expect(1);
        let token = token_endpoint(&mut server, 0);

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a pre-issued token cannot be reissued");

        assert!(
            matches!(err, PostBulkError::Rejected { status: 401, .. }),
            "expected Rejected {{ status: 401 }}, got {err:?}"
        );
        token.assert_async().await;
    }

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
}
