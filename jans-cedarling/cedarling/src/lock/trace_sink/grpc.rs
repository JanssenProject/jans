// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE bulk requests over gRPC. Lock hands `records` to the same pipeline as
//! `POST /audit/trace/bulk` and answers with the gRPC status matching the HTTP
//! one, the REST error body being the status description.

use std::fmt;

use bytes::Bytes;
use tonic::codegen::http::uri::InvalidUri;
use tonic::metadata::MetadataValue;
use tonic::transport::{Channel, ClientTlsConfig};
use tonic::{Code, Request, Status};

use super::{Attempt, BulkItemResult, ErrorBody, PROBLEM_FLAGS, PostBulkError, RejectedStatus};
use crate::HttpClientConfig;
use crate::lock::trace_proto::trace_service_client::TraceServiceClient;
use crate::lock::trace_proto::{
    BulkAcceptance, BulkResultEntry, SubmitBulkRecordsRequest, TraceError, TraceFlags,
};

/// Lock's `TraceService`, reached on the origin of the TRACE endpoint.
pub(in crate::lock) struct GrpcTraceClient {
    client: TraceServiceClient<Channel>,
    /// Scheme, host and port: no path, query or credentials, so it can be logged.
    origin: String,
}

impl fmt::Debug for GrpcTraceClient {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.origin)
    }
}

/// Why no gRPC client could be built for the TRACE endpoint.
#[derive(Debug, thiserror::Error)]
pub(in crate::lock) enum GrpcTraceClientError {
    #[error("the TRACE endpoint origin {origin} is not a valid gRPC address")]
    InvalidUri {
        origin: String,
        #[source]
        source: InvalidUri,
    },
    #[error("the gRPC TLS configuration for {origin} could not be built")]
    Tls {
        origin: String,
        #[source]
        source: tonic::transport::Error,
    },
}

impl GrpcTraceClient {
    /// TLS uses the native roots only: `CEDARLING_LOCK_ACCEPT_INVALID_CERTS` is not
    /// honoured, as for the audit gRPC transport. The response cap and timeout are
    /// the ones REST requests get.
    pub(in crate::lock) fn new(
        endpoint: &url::Url,
        http_conf: &HttpClientConfig,
    ) -> Result<Self, GrpcTraceClientError> {
        let origin = endpoint.origin().ascii_serialization();
        let channel = Channel::from_shared(origin.clone())
            .map_err(|source| GrpcTraceClientError::InvalidUri {
                origin: origin.clone(),
                source,
            })?
            .tls_config(ClientTlsConfig::new().with_native_roots())
            .map_err(|source| GrpcTraceClientError::Tls {
                origin: origin.clone(),
                source,
            })?
            .connect_timeout(http_conf.request_timeout)
            .timeout(http_conf.request_timeout)
            .connect_lazy();
        let max_response = http_conf
            .max_response_size_bytes
            .map_or(usize::MAX, |cap| usize::try_from(cap).unwrap_or(usize::MAX));
        Ok(Self {
            client: TraceServiceClient::new(channel).max_decoding_message_size(max_response),
            origin,
        })
    }

    pub(super) async fn submit(&self, token: &str, body: &Bytes) -> Result<Attempt, PostBulkError> {
        let request = bulk_request(token, body)?;
        match self.client.clone().submit_bulk_records(request).await {
            Ok(response) => results(response.into_inner()).map(Attempt::Results),
            Err(status) if status.code() == Code::Unauthenticated => Ok(Attempt::Unauthorized),
            Err(status) => Err(failure(&status)),
        }
    }
}

/// `records` shares the batch's buffer, so a retry or a resend after
/// `UNAUTHENTICATED` neither copies nor re-encodes the bytes Lock verifies.
fn bulk_request(
    token: &str,
    body: &Bytes,
) -> Result<Request<SubmitBulkRecordsRequest>, PostBulkError> {
    let mut bearer = MetadataValue::try_from(format!("Bearer {token}")).map_err(|err| {
        super::retryable(format!(
            "the Lock access token is not a valid gRPC metadata value: {err}"
        ))
    })?;
    bearer.set_sensitive(true);
    let mut request = Request::new(SubmitBulkRecordsRequest {
        records: body.clone(),
    });
    request.metadata_mut().insert("authorization", bearer);
    Ok(request)
}

/// The same split as REST: what the network or a failing Lock may answer
/// differently next time goes again, a refusal does not. `INTERNAL` is Lock's
/// `500` and `UNKNOWN` its answer for an unmapped HTTP status.
fn failure(status: &Status) -> PostBulkError {
    match status.code() {
        Code::Unavailable
        | Code::Internal
        | Code::Unknown
        | Code::DeadlineExceeded
        | Code::Cancelled
        | Code::Aborted => {
            super::retryable(format!("gRPC {:?}: {}", status.code(), status.message()))
        },
        code => {
            let body = serde_json::from_str::<ErrorBody>(status.message()).ok();
            PostBulkError::Rejected {
                status: RejectedStatus::Grpc(code),
                error: body.as_ref().map(|body| body.error.clone()),
                reason: body.and_then(|body| body.reason),
            }
        },
    }
}

fn results(acceptance: BulkAcceptance) -> Result<Vec<BulkItemResult>, PostBulkError> {
    acceptance.results.into_iter().map(item).collect()
}

fn item(entry: BulkResultEntry) -> Result<BulkItemResult, PostBulkError> {
    let index = usize::try_from(entry.index).map_err(|_| PostBulkError::UnreadableResponse {
        reason: format!("record index {} in BulkAcceptance", entry.index),
    })?;
    // Lock sends `""` for what it leaves out, a hidden reason included.
    let TraceError { error, reason, .. } = entry.error.unwrap_or_default();
    Ok(BulkItemResult {
        index,
        accepted: entry.accepted,
        error: Some(error).filter(|error| !error.is_empty()),
        reason: Some(reason).filter(|reason| !reason.is_empty()),
        flags: entry.flags.as_ref().map(problem_flags).unwrap_or_default(),
    })
}

/// The [`PROBLEM_FLAGS`] Lock set, in that order.
fn problem_flags(flags: &TraceFlags) -> Vec<&'static str> {
    let set = [
        flags.chain_link_failure_flag,
        flags.equivocation_flag,
        flags.policy_mismatch_flag,
        flags.reserved_member_flag,
    ];
    PROBLEM_FLAGS
        .into_iter()
        .zip(set)
        .filter_map(|(name, set)| set.then_some(name))
        .collect()
}

#[cfg(test)]
mod tests {
    use mockito::{Mock, Server as HttpServer, ServerGuard};
    use serde_json::json;
    use tokio::net::TcpListener;
    use tokio::sync::{RwLock, mpsc};
    use tokio_stream::wrappers::TcpListenerStream;
    use tonic::Response;
    use tonic::transport::Server;

    use super::*;
    use crate::http::HttpClient;
    use crate::lock::LockAccessToken;
    use crate::lock::register_client::TokenSource;
    use crate::lock::trace_proto::trace_service_server::{TraceService, TraceServiceServer};
    use crate::lock::trace_proto::{
        ExecutionResult, GetExecutionRequest, GetRecordRequest, RecordAcceptance, RecordEnvelope,
        RecordSearchResult, SearchRecordsRequest, SubmitRecordRequest,
    };
    use crate::lock::trace_sink::{BulkSink, RequestRefusal, TraceChannel, TraceSink};

    /// Member order, spacing, escapes and non-ASCII text Lock signs over as sent.
    const BODY: &str = "[ {\"producer\":\"c\u{e9}darling\",\"n\":1.50} ,\n{\"s\":\"\\u00e9\"}]";

    type Reply = dyn Fn(&str) -> Result<BulkAcceptance, Status> + Send + Sync;

    /// What one `SubmitBulkRecords` call carried.
    struct Seen {
        authorization: Option<String>,
        records: Bytes,
    }

    /// Answers `SubmitBulkRecords` by the bearer token it carries.
    struct MockTraceService {
        reply: Box<Reply>,
        seen: mpsc::UnboundedSender<Seen>,
    }

    #[tonic::async_trait]
    impl TraceService for MockTraceService {
        async fn submit_bulk_records(
            &self,
            request: Request<SubmitBulkRecordsRequest>,
        ) -> Result<Response<BulkAcceptance>, Status> {
            let authorization = request
                .metadata()
                .get("authorization")
                .and_then(|value| value.to_str().ok())
                .map(str::to_string);
            let token = authorization
                .as_deref()
                .and_then(|value| value.strip_prefix("Bearer "))
                .unwrap_or_default()
                .to_string();
            let records = request.into_inner().records;
            self.seen
                .send(Seen {
                    authorization,
                    records,
                })
                .expect("the test keeps the receiver");
            (self.reply)(&token).map(Response::new)
        }

        async fn submit_record(
            &self,
            _: Request<SubmitRecordRequest>,
        ) -> Result<Response<RecordAcceptance>, Status> {
            Err(Status::unimplemented("Cedarling sends bulk requests only"))
        }

        async fn get_record(
            &self,
            _: Request<GetRecordRequest>,
        ) -> Result<Response<RecordEnvelope>, Status> {
            Err(Status::unimplemented("Cedarling does not read records"))
        }

        async fn search_records(
            &self,
            _: Request<SearchRecordsRequest>,
        ) -> Result<Response<RecordSearchResult>, Status> {
            Err(Status::unimplemented("Cedarling does not read records"))
        }

        async fn get_execution(
            &self,
            _: Request<GetExecutionRequest>,
        ) -> Result<Response<ExecutionResult>, Status> {
            Err(Status::unimplemented("Cedarling does not read records"))
        }
    }

    struct MockLock {
        /// A REST bulk URL on the mock's origin: gRPC must use only the origin.
        endpoint: url::Url,
        seen: mpsc::UnboundedReceiver<Seen>,
    }

    async fn mock_lock(
        reply: impl Fn(&str) -> Result<BulkAcceptance, Status> + Send + Sync + 'static,
    ) -> MockLock {
        let (tx, seen) = mpsc::unbounded_channel();
        let service = MockTraceService {
            reply: Box::new(reply),
            seen: tx,
        };
        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("a local port is free");
        let addr = listener.local_addr().expect("the listener has an address");
        tokio::spawn(async move {
            Server::builder()
                .add_service(TraceServiceServer::new(service))
                .serve_with_incoming(TcpListenerStream::new(listener))
                .await
                .expect("the mock Lock serves");
        });
        MockLock {
            endpoint: format!("http://{addr}/jans-lock/api/v1/audit/trace/bulk")
                .parse()
                .expect("valid URL"),
            seen,
        }
    }

    fn sink(endpoint: &url::Url, source: Option<TokenSource>) -> TraceSink {
        let http_conf = HttpClientConfig::default();
        TraceSink {
            channel: TraceChannel::Grpc(
                GrpcTraceClient::new(endpoint, &http_conf).expect("gRPC client builds"),
            ),
            http: HttpClient::new(http_conf).expect("HTTP client"),
            token: RwLock::new(LockAccessToken("old-token".to_string())),
            source,
        }
    }

    fn bulk_body() -> Bytes {
        Bytes::from_static(BODY.as_bytes())
    }

    fn accepted(index: i32) -> BulkResultEntry {
        BulkResultEntry {
            index,
            accepted: true,
            flags: Some(TraceFlags::default()),
            ..Default::default()
        }
    }

    fn lock_error(code: Code, error: &str, reason: &str) -> Status {
        Status::new(
            code,
            json!({"error": error, "error_description": "described", "reason": reason}).to_string(),
        )
    }

    async fn post(lock: &MockLock) -> Result<Vec<BulkItemResult>, PostBulkError> {
        sink(&lock.endpoint, None).post_bulk(&bulk_body()).await
    }

    #[tokio::test]
    async fn results_map_in_order_with_problem_flags_only() {
        let mut lock = mock_lock(|_| {
            Ok(BulkAcceptance {
                results: vec![
                    BulkResultEntry {
                        flags: Some(TraceFlags {
                            policy_mismatch_flag: true,
                            coverage_gap_flag: true,
                            late_flag: true,
                            ..Default::default()
                        }),
                        ..accepted(0)
                    },
                    BulkResultEntry {
                        index: 1,
                        accepted: false,
                        error: Some(TraceError {
                            error: "invalid_signature".to_string(),
                            error_description: "described".to_string(),
                            reason: "signature_mismatch".to_string(),
                        }),
                        ..Default::default()
                    },
                    BulkResultEntry {
                        index: 2,
                        accepted: false,
                        error: Some(TraceError {
                            error: "invalid_request".to_string(),
                            ..Default::default()
                        }),
                        ..Default::default()
                    },
                ],
            })
        })
        .await;

        let items = post(&lock)
            .await
            .expect("an OK answer should be read per record");

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
                BulkItemResult {
                    index: 2,
                    accepted: false,
                    error: Some("invalid_request".to_string()),
                    reason: None,
                    flags: vec![],
                },
            ],
            "results keep request order, only problem flags are kept and a hidden reason is \
             no reason"
        );
        let seen = lock.seen.recv().await.expect("Lock got the request");
        assert_eq!(
            seen.authorization.as_deref(),
            Some("Bearer old-token"),
            "the Lock token goes in the authorization metadata"
        );
    }

    #[test]
    fn each_problem_flag_maps_to_its_name() {
        for (name, flags) in [
            (
                "chain_link_failure_flag",
                TraceFlags {
                    chain_link_failure_flag: true,
                    ..Default::default()
                },
            ),
            (
                "equivocation_flag",
                TraceFlags {
                    equivocation_flag: true,
                    ..Default::default()
                },
            ),
            (
                "policy_mismatch_flag",
                TraceFlags {
                    policy_mismatch_flag: true,
                    ..Default::default()
                },
            ),
            (
                "reserved_member_flag",
                TraceFlags {
                    reserved_member_flag: true,
                    ..Default::default()
                },
            ),
        ] {
            assert_eq!(problem_flags(&flags), vec![name], "{name} alone");
        }
    }

    #[tokio::test]
    async fn records_are_the_exact_body_bytes() {
        let mut lock = mock_lock(|_| Ok(BulkAcceptance::default())).await;

        post(&lock).await.expect("an empty answer is no records");

        let seen = lock.seen.recv().await.expect("Lock got the request");
        assert_eq!(
            seen.records.as_ref(),
            BODY.as_bytes(),
            "records must be the signed body, byte for byte"
        );
    }

    #[test]
    fn every_attempt_sends_the_body_without_copying_it() {
        let body = bulk_body();
        for attempt in ["first", "repeated"] {
            let request = bulk_request("token", &body).expect("the bulk request builds");
            assert_eq!(
                request.get_ref().records.as_ptr(),
                body.as_ptr(),
                "the {attempt} attempt should send the batch's bytes, not a copy"
            );
        }
    }

    #[tokio::test]
    async fn transient_statuses_are_retryable() {
        for code in [
            Code::Unavailable,
            Code::Internal,
            Code::Unknown,
            Code::DeadlineExceeded,
            Code::Cancelled,
            Code::Aborted,
        ] {
            let lock = mock_lock(move |_| Err(Status::new(code, "try later"))).await;

            let err = post(&lock)
                .await
                .expect_err("a failed request should be an error");
            assert!(
                matches!(
                    err,
                    PostBulkError::Retryable {
                        retry_after: None,
                        ..
                    }
                ),
                "gRPC {code:?} should be Retryable without a wait, got {err:?}"
            );
        }
    }

    #[tokio::test]
    async fn refusals_carry_the_lock_error() {
        for (code, error, reason) in [
            (Code::InvalidArgument, "invalid_request", "trace_disabled"),
            (Code::PermissionDenied, "access_denied", "missing_scope"),
            (Code::NotFound, "not_found", "unknown_domain"),
            (Code::AlreadyExists, "conflict", "duplicate"),
            (
                Code::FailedPrecondition,
                "ambiguous_identifier",
                "several_producers",
            ),
        ] {
            let lock = mock_lock(move |_| Err(lock_error(code, error, reason))).await;

            let err = post(&lock)
                .await
                .expect_err("a refused request should be an error");
            assert_eq!(
                err,
                PostBulkError::Rejected {
                    status: RejectedStatus::Grpc(code),
                    error: Some(error.to_string()),
                    reason: Some(reason.to_string()),
                },
                "gRPC {code:?} should be Rejected with Lock's error and reason"
            );
        }
    }

    #[tokio::test]
    async fn bulk_limits_name_the_setting_to_change() {
        for (reason, expected) in [
            (
                "max_bulk_records_exceeded",
                Some(RequestRefusal::TooManyRecords),
            ),
            ("bulk_body_too_large", Some(RequestRefusal::BodyTooLarge)),
            ("", Some(RequestRefusal::Unexplained)),
            ("trace_disabled", None),
        ] {
            let lock = mock_lock(move |_| {
                Err(lock_error(Code::InvalidArgument, "invalid_request", reason))
            })
            .await;

            let err = post(&lock)
                .await
                .expect_err("a refused request should be an error");
            assert_eq!(
                err.refusal(),
                expected,
                "INVALID_ARGUMENT with reason {reason:?}"
            );
        }
    }

    #[tokio::test]
    async fn message_over_the_transport_limit_is_a_body_too_large() {
        let lock = mock_lock(|_| {
            Err(Status::resource_exhausted(
                "gRPC message exceeds maximum size 4259840: 5000000",
            ))
        })
        .await;

        let err = post(&lock)
            .await
            .expect_err("an oversized message should be an error");
        assert_eq!(
            err,
            PostBulkError::Rejected {
                status: RejectedStatus::Grpc(Code::ResourceExhausted),
                error: None,
                reason: None,
            },
            "the same bytes would be refused again, so they are not retried"
        );
        assert_eq!(
            err.refusal(),
            Some(RequestRefusal::BodyTooLarge),
            "the emitter should say to lower CEDARLING_TRACE_BULK_MAX_BYTES"
        );
    }

    #[tokio::test]
    async fn description_that_is_not_a_lock_error_has_no_error_id() {
        let lock = mock_lock(|_| Err(Status::permission_denied("Authorization error"))).await;

        let err = post(&lock)
            .await
            .expect_err("a refused request should be an error");
        assert_eq!(
            err,
            PostBulkError::Rejected {
                status: RejectedStatus::Grpc(Code::PermissionDenied),
                error: None,
                reason: None,
            },
            "a plain description is no Lock error"
        );
    }

    #[tokio::test]
    async fn negative_index_is_an_unreadable_response() {
        let lock = mock_lock(|_| {
            Ok(BulkAcceptance {
                results: vec![accepted(0), accepted(-1)],
            })
        })
        .await;

        let err = post(&lock)
            .await
            .expect_err("a record index that cannot exist should be an error");
        assert!(
            matches!(
                err,
                PostBulkError::UnreadableResponse { ref reason }
                    if reason.contains("record index -1")
            ),
            "expected an unreadable response naming the index, got {err:?}"
        );
    }

    #[tokio::test]
    async fn unreachable_lock_is_retryable() {
        let endpoint: url::Url = "http://127.0.0.1:1/bulk".parse().expect("valid URL");

        let err = sink(&endpoint, None)
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
            "expected Retryable, got {err:?}"
        );
    }

    #[test]
    fn rejection_message_names_the_grpc_status() {
        let err = PostBulkError::Rejected {
            status: RejectedStatus::Grpc(Code::PermissionDenied),
            error: Some("access_denied".to_string()),
            reason: None,
        };
        assert_eq!(
            err.to_string(),
            "Lock Server rejected the TRACE bulk request with gRPC PermissionDenied (error: \
             access_denied, reason: none)",
            "the logged rejection should name the transport and its status"
        );
    }

    fn source(server: &ServerGuard) -> TokenSource {
        TokenSource {
            token_endpoint: format!("{}/token", server.url()),
            client_id: "cedarling-client".to_string(),
            client_secret: "secret".to_string(),
            scope: "https://jans.io/oauth/lock/trace.write".to_string(),
        }
    }

    fn token_endpoint(server: &mut ServerGuard, expected_calls: usize) -> Mock {
        server
            .mock("POST", "/token")
            .with_body(json!({"access_token": "new-token"}).to_string())
            .expect(expected_calls)
            .create()
    }

    /// `UNAUTHENTICATED` for every token but `accepted`.
    fn only_token(
        accepted: &'static str,
    ) -> impl Fn(&str) -> Result<BulkAcceptance, Status> + Send + Sync + 'static {
        move |token| {
            if token == accepted {
                Ok(BulkAcceptance::default())
            } else {
                Err(Status::unauthenticated("invalid token"))
            }
        }
    }

    fn tokens_seen(lock: &mut MockLock) -> Vec<String> {
        let mut tokens = Vec::new();
        while let Ok(seen) = lock.seen.try_recv() {
            assert_eq!(
                seen.records.as_ref(),
                BODY.as_bytes(),
                "every attempt sends the same bytes"
            );
            tokens.push(seen.authorization.unwrap_or_default());
        }
        tokens
    }

    #[tokio::test]
    async fn unauthenticated_reissues_token_and_resends_same_body() {
        let mut lock = mock_lock(only_token("new-token")).await;
        let mut idp = HttpServer::new_async().await;
        let token = token_endpoint(&mut idp, 1);

        sink(&lock.endpoint, Some(source(&idp)))
            .post_bulk(&bulk_body())
            .await
            .expect("a reissued token should get the same body through");

        token.assert_async().await;
        assert_eq!(
            tokens_seen(&mut lock),
            vec!["Bearer old-token", "Bearer new-token"],
            "the body goes again, once, with the reissued token"
        );
    }

    #[tokio::test]
    async fn unauthenticated_twice_is_rejected() {
        let mut lock = mock_lock(only_token("never")).await;
        let mut idp = HttpServer::new_async().await;
        let token = token_endpoint(&mut idp, 1);

        let err = sink(&lock.endpoint, Some(source(&idp)))
            .post_bulk(&bulk_body())
            .await
            .expect_err("UNAUTHENTICATED after reissue should be an error");

        assert_eq!(
            err,
            PostBulkError::Rejected {
                status: RejectedStatus::Grpc(Code::Unauthenticated),
                error: None,
                reason: None,
            },
            "a second UNAUTHENTICATED is final"
        );
        token.assert_async().await;
        assert_eq!(
            tokens_seen(&mut lock).len(),
            2,
            "one attempt per token, no loop"
        );
    }

    #[tokio::test]
    async fn unauthenticated_without_token_source_is_rejected() {
        let lock = mock_lock(only_token("never")).await;
        let mut idp = HttpServer::new_async().await;
        let token = token_endpoint(&mut idp, 0);

        let err = post(&lock)
            .await
            .expect_err("a pre-issued token cannot be reissued");

        assert_eq!(
            err,
            PostBulkError::Rejected {
                status: RejectedStatus::Grpc(Code::Unauthenticated),
                error: None,
                reason: None,
            },
            "UNAUTHENTICATED is final without a token source"
        );
        token.assert_async().await;
    }
}
