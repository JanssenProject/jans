// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE bulk requests over REST: a `POST` of the body to the TRACE bulk
//! endpoint, which Lock answers with `207` and a result per record.

use std::time::Duration;

use bytes::Bytes;
use chrono::{DateTime, Utc};
use reqwest::StatusCode;
use reqwest::header::{CONTENT_TYPE, HeaderMap, RETRY_AFTER};
use serde::Deserialize;

use super::{
    Attempt, BulkItemResult, ErrorBody, PostBulkError, ProblemFlag, RejectedStatus, retryable,
};
use crate::http::HttpClient;
use crate::http_utils::{HttpRequestError, MAX_EXPONENTIAL_DELAY, ReadBodyError};

/// Lock's TRACE bulk endpoint and the client that posts to it.
pub(in crate::lock) struct RestTraceClient {
    pub(in crate::lock) endpoint: url::Url,
    /// A clone of the sink's client: the two share one connection pool.
    http: HttpClient,
}

impl RestTraceClient {
    pub(in crate::lock) fn new(endpoint: url::Url, http: HttpClient) -> Self {
        Self { endpoint, http }
    }

    /// The bulk request carrying `body`. `Bytes` clones share the buffer, so a
    /// retry or a resend after `401` does not copy the batch.
    fn bulk_request(&self, token: &str, body: &Bytes) -> reqwest::RequestBuilder {
        self.http
            .raw_client
            .post(self.endpoint.clone())
            .bearer_auth(token)
            .header(CONTENT_TYPE, "application/json")
            .body(body.clone())
    }

    pub(super) async fn submit(&self, token: &str, body: &Bytes) -> Result<Attempt, PostBulkError> {
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
            .map_err(|err| match err {
                // Lock processed the request; its answer would be over the cap again.
                HttpRequestError::ReadBody {
                    error: ReadBodyError::TooLarge { .. },
                    ..
                } if status == StatusCode::MULTI_STATUS => PostBulkError::UnreadableResponse {
                    reason: format!("HTTP 207 body: {err}"),
                },
                HttpRequestError::ReadBody {
                    error: ReadBodyError::TooLarge { .. },
                    ..
                } => PostBulkError::Rejected {
                    status: RejectedStatus::Http(status),
                    error: None,
                    reason: None,
                },
                err => retryable(err.to_string()),
            })?;
        if status == StatusCode::MULTI_STATUS {
            return parse_results(&bytes).map(Attempt::Results);
        }
        let error = serde_json::from_slice::<ErrorBody>(&bytes).ok();
        Err(PostBulkError::Rejected {
            status: RejectedStatus::Http(status),
            error: error.as_ref().map(|body| body.error.clone()),
            reason: error.and_then(|body| body.reason),
        })
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

fn parse_results(bytes: &[u8]) -> Result<Vec<BulkItemResult>, PostBulkError> {
    let response: BulkResponse =
        serde_json::from_slice(bytes).map_err(|err| PostBulkError::UnreadableResponse {
            reason: format!("HTTP 207 body: {err}"),
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

impl From<RawItem> for BulkItemResult {
    fn from(item: RawItem) -> Self {
        let flags = ProblemFlag::ALL
            .into_iter()
            .map(ProblemFlag::name)
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

#[cfg(test)]
mod tests {
    use mockito::{Matcher, Mock, Server, ServerGuard};
    use serde_json::json;

    use super::*;
    use crate::lock::register_client::TokenSource;
    use crate::lock::trace_sink::test_support::{self, source, token_endpoint};
    use crate::lock::trace_sink::{BulkSink, TraceChannel, TraceSink};

    const BODY: &str = r#"[{"producer":"cedarling-fleet-1"},{"producer":"cedarling-fleet-1"}]"#;
    const BULK_PATH: &str = "/jans-lock/api/v1/audit/trace/bulk";

    fn bulk_body() -> Bytes {
        Bytes::from_static(BODY.as_bytes())
    }

    fn client(server: &ServerGuard) -> RestTraceClient {
        let endpoint = format!("{}{BULK_PATH}", server.url())
            .parse()
            .expect("valid URL");
        RestTraceClient::new(endpoint, test_support::http())
    }

    fn sink(server: &ServerGuard, source: Option<TokenSource>) -> TraceSink {
        test_support::sink(TraceChannel::Rest(client(server)), source)
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

    #[tokio::test]
    async fn every_attempt_sends_the_body_without_copying_it() {
        let server = Server::new_async().await;
        let client = client(&server);
        let body = bulk_body();

        for attempt in ["first", "repeated"] {
            let request = client
                .bulk_request("token", &body)
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
    async fn multi_status_with_bad_body_is_unreadable() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(&mut server, "old-token", 207, "not json");

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("an unreadable 207 body should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::UnreadableResponse { ref reason }
                    if reason.starts_with("HTTP 207 body")
            ),
            "expected UnreadableResponse for the 207 body, got {err:?}"
        );
    }

    #[tokio::test]
    async fn multi_status_over_the_response_cap_is_unreadable() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(
            &mut server,
            "old-token",
            207,
            &json!({"results": []}).to_string(),
        );
        let http = HttpClient::new(crate::HttpClientConfig {
            max_response_size_bytes: Some(4),
            ..crate::HttpClientConfig::default()
        })
        .expect("HTTP client");
        let client = RestTraceClient::new(client(&server).endpoint, http);

        let err = test_support::sink(TraceChannel::Rest(client), None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a 207 body over the cap should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::UnreadableResponse { ref reason }
                    if reason.starts_with("HTTP 207 body")
            ),
            "Lock processed the records, so they are not sent again, got {err:?}"
        );
    }

    #[tokio::test]
    async fn refusal_over_the_response_cap_is_final() {
        let mut server = Server::new_async().await;
        let _bulk = bulk(
            &mut server,
            "old-token",
            413,
            &json!({"error": "invalid_request", "reason": "bulk_body_too_large"}).to_string(),
        );
        let http = HttpClient::new(crate::HttpClientConfig {
            max_response_size_bytes: Some(4),
            ..crate::HttpClientConfig::default()
        })
        .expect("HTTP client");
        let client = RestTraceClient::new(client(&server).endpoint, http);

        let err = test_support::sink(TraceChannel::Rest(client), None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a refusal with a body over the cap should be an error");
        assert_eq!(
            err,
            PostBulkError::Rejected {
                status: RejectedStatus::Http(StatusCode::PAYLOAD_TOO_LARGE),
                error: None,
                reason: None,
            },
            "a refusal stays final when its body is over the cap; only the error body is lost"
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
        sink.channel = TraceChannel::Rest(RestTraceClient::new(
            "http://127.0.0.1:1/bulk".parse().expect("valid URL"),
            test_support::http(),
        ));

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
                PostBulkError::Rejected {
                    status: RejectedStatus::Http(StatusCode::BAD_REQUEST),
                    ref error,
                    ref reason,
                }
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
            matches!(
                err,
                PostBulkError::Rejected {
                    status: RejectedStatus::Http(StatusCode::UNAUTHORIZED),
                    ..
                }
            ),
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
            matches!(
                err,
                PostBulkError::Rejected {
                    status: RejectedStatus::Http(StatusCode::UNAUTHORIZED),
                    ..
                }
            ),
            "expected Rejected {{ status: 401 }}, got {err:?}"
        );
        token.assert_async().await;
    }

    #[tokio::test]
    async fn hidden_reason_is_no_reason() {
        let mut server = Server::new_async().await;
        let body = json!({"error": "invalid_request", "reason": ""});
        let _bulk = bulk(&mut server, "old-token", 400, &body.to_string());

        let err = sink(&server, None)
            .post_bulk(&bulk_body())
            .await
            .expect_err("a 4xx response should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::Rejected {
                    status: RejectedStatus::Http(StatusCode::BAD_REQUEST),
                    ref error,
                    reason: None,
                }
                    if error.as_deref() == Some("invalid_request")
            ),
            "expected Rejected {{ 400, invalid_request, no reason }}, got {err:?}"
        );
    }
}
