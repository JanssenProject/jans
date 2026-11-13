// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use async_trait::async_trait;
use bytes::Bytes;
use reqwest::header::CONTENT_TYPE;
use serde::Serialize;
use url::Url;

use crate::{
    http::HttpClient,
    http_utils::AttemptFailure,
    lock::{
        LockLogEntry,
        transport::{
            AuditItem, AuditKind, AuditTransport, SendFailure,
            mapping::{
                self, LockServerHealthEntry, LockServerLogEntry, LockServerMetricsEntry,
                NothingToSend,
            },
        },
    },
    log::{LogWriter, LoggerWeak},
};

pub(crate) struct RestTransport {
    client: HttpClient,
    logger: Option<LoggerWeak>,
}

impl RestTransport {
    /// Construct a new [`RestTransport`]
    pub(crate) fn new(client: HttpClient, logger: Option<LoggerWeak>) -> Self {
        Self { client, logger }
    }
}

/// The JSON body for one audit endpoint. `Bytes` clones share the buffer, so a
/// retry does not serialize or copy the batch again.
#[derive(Debug)]
pub(in crate::lock) struct RestBatch {
    url: Url,
    body: Bytes,
}

/// Why a REST batch has no body to send.
#[derive(Debug, thiserror::Error)]
pub(in crate::lock) enum RestPrepareError {
    #[error(transparent)]
    NothingToSend(#[from] NothingToSend),
    #[error("failed to serialize the batch as JSON: {0}")]
    Serialize(serde_json::Error),
}

fn json_body<T: Serialize>(payloads: &[T]) -> Result<Bytes, RestPrepareError> {
    serde_json::to_vec(payloads)
        .map(Bytes::from)
        .map_err(RestPrepareError::Serialize)
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for RestTransport {
    type Batch = RestBatch;
    type PrepareError = RestPrepareError;

    fn prepare(
        &self,
        entries: &[AuditItem],
        audit_kind: &AuditKind,
    ) -> Result<RestBatch, RestPrepareError> {
        let warn = |msg| self.logger.log_any(LockLogEntry::warn(msg));
        let body = match audit_kind {
            AuditKind::Log(_) => {
                json_body(&mapping::map_entries::<LockServerLogEntry>(entries, warn)?)
            },
            AuditKind::Telemetry(_) => json_body(&mapping::map_entries::<LockServerMetricsEntry>(
                entries, warn,
            )?),
            AuditKind::Health(_) => json_body(&mapping::map_entries::<LockServerHealthEntry>(
                entries, warn,
            )?),
        }?;
        Ok(RestBatch {
            url: audit_kind.url().clone(),
            body,
        })
    }

    async fn send(&self, batch: &RestBatch) -> Result<(), SendFailure> {
        let result = self
            .client
            .raw_client
            .post(batch.url.as_str())
            .header(CONTENT_TYPE, "application/json")
            .body(batch.body.clone())
            .send()
            .await;
        AttemptFailure::check(result)?;
        Ok(())
    }
}

#[cfg(test)]
mod test {
    use reqwest::StatusCode;

    use crate::HttpClientConfig;
    use crate::http_utils::{FinalFailure, RetryableFailure};
    use crate::lock::transport::{FinalSend, RetryableSend};

    use super::*;

    /// Prepares and sends `entries`, as the audit worker does.
    async fn deliver(
        transport: &RestTransport,
        entries: &[AuditItem],
        kind: AuditKind,
    ) -> Result<(), SendFailure> {
        let batch = transport
            .prepare(entries, &kind)
            .expect("the test batch should map");
        transport.send(&batch).await
    }

    use mockito::{Server, ServerGuard};
    use url::Url;

    use crate::lock::transport::test_utils::{
        malformed_log_item, sample_health_item, sample_log_item, sample_metric_item,
    };

    fn create_test_client() -> HttpClient {
        HttpClient::new(HttpClientConfig::default()).expect("Http client should be initialized")
    }

    fn mock_log_endpoint(server: &mut ServerGuard) -> mockito::Mock {
        server
            .mock("POST", "/audit/log/bulk")
            .match_header("content-type", "application/json")
            .with_status(201)
            .create()
    }

    fn mock_telemetry_endpoint(server: &mut ServerGuard) -> mockito::Mock {
        server
            .mock("POST", "/audit/telemetry/bulk")
            .match_header("content-type", "application/json")
            .with_status(201)
            .create()
    }

    #[tokio::test]
    async fn test_send_logs_success() {
        let mut server = Server::new_async().await;
        let mock = mock_log_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_log_item()];

        deliver(&transport, &entries, AuditKind::Log(endpoint))
            .await
            .expect("logs should be sent successfully");
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let server = Server::new_async().await;
        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let err = transport
            .prepare(&[], &AuditKind::Log(endpoint))
            .expect_err("an empty batch has nothing to send");
        assert!(
            matches!(
                err,
                RestPrepareError::NothingToSend(NothingToSend { count: 0 })
            ),
            "expected NothingToSend {{ count: 0 }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_server_error_500() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("POST", "/audit/log/bulk")
            .with_status(500)
            .with_body("Internal Server Error")
            .create();

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_log_item()];

        let error = deliver(&transport, &entries, AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(
                error,
                SendFailure::Retryable(RetryableSend::Http(RetryableFailure::Status {
                    status: StatusCode::INTERNAL_SERVER_ERROR,
                    ..
                }))
            ),
            "a 500 may clear, so it is retryable, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_logs_client_error_400() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("POST", "/audit/log/bulk")
            .with_status(400)
            .with_body("Bad Request")
            .create();

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_log_item()];

        let error = deliver(&transport, &entries, AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(
                error,
                SendFailure::Final(FinalSend::Http(FinalFailure::Status {
                    status: StatusCode::BAD_REQUEST,
                    ..
                }))
            ),
            "a 400 is answered the same way again, so it is final, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_logs_network_failure() {
        let endpoint: Url = "http://localhost:1/invalid".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_log_item()];

        let error = deliver(&transport, &entries, AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a network error");
        assert!(
            matches!(
                error,
                SendFailure::Retryable(RetryableSend::Http(RetryableFailure::Transport(_)))
            ),
            "a refused connection may clear, so it is retryable, got {error:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_all_invalid_dropped() {
        let endpoint: Url = "http://localhost:8080/audit/log/bulk".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![malformed_log_item()];

        let err = transport
            .prepare(&entries, &AuditKind::Log(endpoint))
            .expect_err("a batch with no mappable entry has nothing to send");
        assert!(
            matches!(
                err,
                RestPrepareError::NothingToSend(NothingToSend { count: 1 })
            ),
            "expected NothingToSend {{ count: 1 }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let mut server = Server::new_async().await;
        let mock = mock_log_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        // Send 1000 entries
        let entries = (0..1000).map(|_| sample_log_item()).collect::<Vec<_>>();

        deliver(&transport, &entries, AuditKind::Log(endpoint))
            .await
            .expect("logs should be sent successfully");
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_telemetry_success() {
        let mut server = Server::new_async().await;
        let mock = mock_telemetry_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/telemetry/bulk", server.url())
            .parse()
            .expect("valid telemetry endpoint URL");
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_metric_item()];

        deliver(&transport, &entries, AuditKind::Telemetry(endpoint))
            .await
            .expect("telemetry should be sent successfully");

        mock.assert();
    }

    fn mock_health_endpoint(server: &mut ServerGuard) -> mockito::Mock {
        server
            .mock("POST", "/audit/health/bulk")
            .match_header("content-type", "application/json")
            .with_status(200)
            .create()
    }

    #[tokio::test]
    async fn test_send_health_success() {
        let mut server = Server::new_async().await;
        let mock = mock_health_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/health/bulk", server.url())
            .parse()
            .expect("valid health endpoint URL");
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_health_item()];

        deliver(&transport, &entries, AuditKind::Health(endpoint))
            .await
            .expect("health check should be sent successfully");

        mock.assert();
    }

    #[tokio::test]
    async fn test_send_health_empty() {
        let server = Server::new_async().await;
        let endpoint: Url = format!("{}/audit/health/bulk", server.url())
            .parse()
            .unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let err = transport
            .prepare(&[], &AuditKind::Health(endpoint))
            .expect_err("an empty batch has nothing to send");
        assert!(
            matches!(
                err,
                RestPrepareError::NothingToSend(NothingToSend { count: 0 })
            ),
            "expected NothingToSend {{ count: 0 }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn test_send_health_server_error_500() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("POST", "/audit/health/bulk")
            .with_status(500)
            .with_body("Internal Server Error")
            .create();

        let endpoint: Url = format!("{}/audit/health/bulk", server.url())
            .parse()
            .unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_health_item()];

        let error = deliver(&transport, &entries, AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(
                error,
                SendFailure::Retryable(RetryableSend::Http(RetryableFailure::Status {
                    status: StatusCode::INTERNAL_SERVER_ERROR,
                    ..
                }))
            ),
            "a 500 may clear, so it is retryable, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_health_client_error_400() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("POST", "/audit/health/bulk")
            .with_status(400)
            .with_body("Bad Request")
            .create();

        let endpoint: Url = format!("{}/audit/health/bulk", server.url())
            .parse()
            .unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_health_item()];

        let error = deliver(&transport, &entries, AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a client error");
        assert!(
            matches!(
                error,
                SendFailure::Final(FinalSend::Http(FinalFailure::Status {
                    status: StatusCode::BAD_REQUEST,
                    ..
                }))
            ),
            "a 400 is answered the same way again, so it is final, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_health_network_failure() {
        let endpoint: Url = "http://localhost:1/invalid".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_health_item()];

        let error = deliver(&transport, &entries, AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a network error");
        assert!(
            matches!(
                error,
                SendFailure::Retryable(RetryableSend::Http(RetryableFailure::Transport(_)))
            ),
            "a refused connection may clear, so it is retryable, got {error:?}"
        );
    }

    #[tokio::test]
    async fn test_send_health_large_batch() {
        let mut server = Server::new_async().await;
        let mock = mock_health_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/health/bulk", server.url())
            .parse()
            .unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = (0..1000).map(|_| sample_health_item()).collect::<Vec<_>>();

        deliver(&transport, &entries, AuditKind::Health(endpoint))
            .await
            .expect("health checks should be sent successfully");
        mock.assert();
    }
}
