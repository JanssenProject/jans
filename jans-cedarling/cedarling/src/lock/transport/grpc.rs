// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! gRPC transport implementation for Lock Server communication.

#[cfg(not(target_arch = "wasm32"))]
use std::time::Duration;

use async_trait::async_trait;
use prost_types::Timestamp;
use tonic::Request;
use tonic::metadata::{Ascii, MetadataValue};
#[cfg(not(target_arch = "wasm32"))]
use tonic::transport::{Channel, ClientTlsConfig};
#[cfg(target_arch = "wasm32")]
use tonic_web_wasm_client::Client;

use crate::{
    lock::{
        LockLogEntry,
        grpc_failure::GrpcFailure,
        proto::{
            BulkHealthRequest, BulkLogRequest, BulkTelemetryRequest, HealthEntry, LogEntry,
            TelemetryEntry, audit_service_client::AuditServiceClient,
        },
        transport::{
            AuditItem, AuditKind, AuditTransport, FinalSend, SendFailure, TransportError,
            mapping::{
                self, LockServerHealthEntry, LockServerLogEntry, LockServerMetricsEntry,
                NothingToSend,
            },
        },
    },
    log::{LogWriter, LoggerWeak},
};

#[cfg(not(target_arch = "wasm32"))]
/// Duration to wait for each gRPC request
const GRPC_REQUEST_TIMEOUT: Duration = Duration::from_secs(10);

pub(crate) struct GrpcTransport {
    #[cfg(not(target_arch = "wasm32"))]
    client: AuditServiceClient<Channel>,
    #[cfg(target_arch = "wasm32")]
    client: AuditServiceClient<Client>,
    /// Parsed once: an access token that is not a valid header value fails here,
    /// not on every attempt.
    authorization: MetadataValue<Ascii>,
    logger: Option<LoggerWeak>,
}

impl GrpcTransport {
    #[cfg_attr(target_arch = "wasm32", allow(clippy::unnecessary_wraps))]
    /// Constructs a new [`GrpcTransport`]
    pub(crate) fn new(
        endpoint: impl Into<String>,
        access_token: &str,
        logger: Option<LoggerWeak>,
    ) -> Result<Self, TransportError> {
        #[cfg(target_arch = "wasm32")]
        let client = AuditServiceClient::new(Client::new(endpoint.into()));

        #[cfg(not(target_arch = "wasm32"))]
        let client = AuditServiceClient::new(
            Channel::from_shared(endpoint.into())
                .map_err(|e| TransportError::InvalidUri(format!("failed to construct URI: {e:?}")))?
                .tls_config(ClientTlsConfig::new().with_native_roots())?
                .timeout(GRPC_REQUEST_TIMEOUT)
                .connect_lazy(),
        );

        Ok(Self {
            client,
            authorization: format!("Bearer {access_token}").parse()?,
            logger,
        })
    }
}

/// The request message for one audit endpoint, built once and cloned per attempt.
#[derive(Debug)]
pub(in crate::lock) enum GrpcBatch {
    Log(BulkLogRequest),
    Telemetry(BulkTelemetryRequest),
    Health(BulkHealthRequest),
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for GrpcTransport {
    type Batch = GrpcBatch;
    type PrepareError = NothingToSend;

    fn prepare(
        &self,
        entries: &[AuditItem],
        audit_kind: &AuditKind,
    ) -> Result<GrpcBatch, NothingToSend> {
        let warn = |msg| self.logger.log_any(LockLogEntry::warn(msg));
        Ok(match audit_kind {
            AuditKind::Log(_) => GrpcBatch::Log(BulkLogRequest {
                entries: mapping::map_entries::<LockServerLogEntry>(entries, warn)?
                    .into_iter()
                    .map(log_json_to_proto)
                    .collect(),
            }),
            AuditKind::Telemetry(_) => GrpcBatch::Telemetry(BulkTelemetryRequest {
                entries: mapping::map_entries::<LockServerMetricsEntry>(entries, warn)?
                    .into_iter()
                    .map(telemetry_json_to_proto)
                    .collect(),
            }),
            AuditKind::Health(_) => GrpcBatch::Health(BulkHealthRequest {
                entries: mapping::map_entries::<LockServerHealthEntry>(entries, warn)?
                    .into_iter()
                    .map(health_json_to_proto)
                    .collect(),
            }),
        })
    }

    async fn send(&self, batch: &GrpcBatch) -> Result<(), SendFailure> {
        let token = self.authorization.clone();
        let mut client = self.client.clone();

        let result = match batch {
            GrpcBatch::Log(message) => {
                client
                    .process_bulk_log(authorized(message.clone(), token))
                    .await
            },
            GrpcBatch::Telemetry(message) => {
                client
                    .process_bulk_telemetry(authorized(message.clone(), token))
                    .await
            },
            GrpcBatch::Health(message) => {
                client
                    .process_bulk_health(authorized(message.clone(), token))
                    .await
            },
        };

        let inner = result.map_err(GrpcFailure::sort)?.into_inner();
        if !inner.success {
            return Err(SendFailure::Final(FinalSend::Unsuccessful {
                message: inner.message,
            }));
        }

        Ok(())
    }
}

fn authorized<T>(message: T, token: MetadataValue<Ascii>) -> Request<T> {
    let mut request = Request::new(message);
    request.metadata_mut().insert("authorization", token);
    request
}

/// Converts a [`LockServerLogEntry`] into a [`LogEntry`]
fn log_json_to_proto(entry: LockServerLogEntry) -> LogEntry {
    LogEntry {
        creation_date: parse_timestamp(&entry.creation_date),
        event_time: parse_timestamp(&entry.event_time),
        service: entry.service.unwrap_or_default(),
        node_name: entry.node_name,
        event_type: entry.event_type,
        severity_level: entry.severity_level.unwrap_or_default(),
        action: entry.action,
        decision_result: entry.decision_result,
        requested_resource: entry.requested_resource,
        principal_id: entry.principal_id.unwrap_or_default(),
        client_id: entry.client_id.unwrap_or_default(),
        jti: String::new(),
        context_information: entry
            .context_information
            .and_then(|v| v.as_object().cloned())
            .unwrap_or_default()
            .into_iter()
            .map(|(k, v)| (k, v.as_str().map_or_else(|| v.to_string(), str::to_owned)))
            .collect(),
    }
}

/// Converts a [`LockServerMetricsEntry`] into a [`TelemetryEntry`]
fn telemetry_json_to_proto(entry: LockServerMetricsEntry) -> TelemetryEntry {
    TelemetryEntry {
        creation_date: parse_timestamp(&entry.creation_date),
        service: entry.service.unwrap_or_default(),
        node_name: entry.node_name,
        status: entry.status,
        policy_stats: entry.policy_stats,
        error_counters: entry.error_counters,
        operational_stats: entry.operational_stats,
        interval_secs: entry.interval_secs,
    }
}

/// Converts a [`LockServerHealthEntry`] into a [`HealthEntry`]
fn health_json_to_proto(entry: LockServerHealthEntry) -> HealthEntry {
    HealthEntry {
        creation_date: parse_timestamp(&entry.creation_date),
        event_time: parse_timestamp(&entry.event_time),
        service: entry.service,
        node_name: entry.node_name,
        status: entry.status,
        engine_status: entry
            .engine_status
            .into_iter()
            .map(|(k, v)| (k, v.to_string()))
            .collect(),
    }
}

// Parse a RFC3339 timestamp string into a protobuf Timestamp
fn parse_timestamp(s: &str) -> Option<Timestamp> {
    chrono::DateTime::parse_from_rfc3339(s)
        .ok()
        .map(|dt| Timestamp {
            seconds: dt.timestamp(),
            nanos: dt.timestamp_subsec_nanos().cast_signed(),
        })
}

#[cfg(test)]
mod test {
    use std::net::SocketAddr;

    use super::*;

    /// Prepares and sends `entries`, as the audit worker does.
    async fn deliver(
        transport: &GrpcTransport,
        entries: &[AuditItem],
        kind: AuditKind,
    ) -> Result<(), SendFailure> {
        let batch = transport
            .prepare(entries, &kind)
            .expect("the test batch should map");
        transport.send(&batch).await
    }

    use tokio::{net::TcpListener, sync::mpsc};
    use tokio_stream::wrappers::TcpListenerStream;
    use tonic::{Response, Status, transport::Server};

    use crate::lock::transport::AuditItem;
    use crate::lock::{
        health_registry::HealthStatus,
        proto::{
            self, AuditResponse,
            audit_service_server::{AuditService, AuditServiceServer},
        },
    };

    use crate::lock::transport::test_utils::{
        malformed_log_item, sample_health_item, sample_log_item, sample_metric_item,
    };

    // Mock gRPC server for testing
    #[derive(Debug)]
    struct MockAuditService {
        log_sender: mpsc::UnboundedSender<Vec<LogEntry>>,
        telemetry_sender: mpsc::UnboundedSender<Vec<TelemetryEntry>>,
        health_sender: mpsc::UnboundedSender<Vec<HealthEntry>>,
        should_fail: bool,
    }

    #[tonic::async_trait]
    impl AuditService for MockAuditService {
        async fn process_health(
            &self,
            _: Request<proto::HealthRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            unimplemented!()
        }

        async fn process_bulk_health(
            &self,
            request: Request<proto::BulkHealthRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            if self.should_fail {
                return Ok(Response::new(AuditResponse {
                    success: false,
                    message: "Server error".to_string(),
                }));
            }

            let auth = request
                .metadata()
                .get("authorization")
                .and_then(|v| v.to_str().ok());

            assert!(
                matches!(auth, Some(token) if token.starts_with("Bearer ")),
                "expected Bearer token in authorization header, got {auth:?}"
            );

            let entries = request.into_inner().entries;
            self.health_sender.send(entries).unwrap();

            Ok(Response::new(AuditResponse {
                success: true,
                message: "OK".to_string(),
            }))
        }

        async fn process_log(
            &self,
            _: Request<proto::LogRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            unimplemented!()
        }

        async fn process_bulk_log(
            &self,
            request: Request<BulkLogRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            if self.should_fail {
                return Ok(Response::new(AuditResponse {
                    success: false,
                    message: "Server error".to_string(),
                }));
            }

            let auth = request
                .metadata()
                .get("authorization")
                .and_then(|v| v.to_str().ok());

            assert!(
                matches!(auth, Some(token) if token.starts_with("Bearer ")),
                "expected Bearer token in authorization header, got {auth:?}"
            );

            let entries = request.into_inner().entries;
            self.log_sender.send(entries).unwrap();

            Ok(Response::new(AuditResponse {
                success: true,
                message: "OK".to_string(),
            }))
        }

        async fn process_telemetry(
            &self,
            _: Request<proto::TelemetryRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            unimplemented!()
        }

        async fn process_bulk_telemetry(
            &self,
            request: Request<proto::BulkTelemetryRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            if self.should_fail {
                return Ok(Response::new(AuditResponse {
                    success: false,
                    message: "Server error".to_string(),
                }));
            }

            let auth = request
                .metadata()
                .get("authorization")
                .and_then(|v| v.to_str().ok());

            assert!(
                matches!(auth, Some(token) if token.starts_with("Bearer ")),
                "expected Bearer token in authorization header, got {auth:?}"
            );

            let entries = request.into_inner().entries;
            self.telemetry_sender.send(entries).unwrap();

            Ok(Response::new(AuditResponse {
                success: true,
                message: "OK".to_string(),
            }))
        }
    }

    async fn start_mock_server(
        should_fail: bool,
    ) -> (
        SocketAddr,
        mpsc::UnboundedReceiver<Vec<LogEntry>>,
        mpsc::UnboundedReceiver<Vec<TelemetryEntry>>,
        mpsc::UnboundedReceiver<Vec<HealthEntry>>,
    ) {
        let (tx, rx) = mpsc::unbounded_channel();
        let (telemetry_tx, telemetry_rx) = mpsc::unbounded_channel();
        let (health_tx, health_rx) = mpsc::unbounded_channel();
        let service = MockAuditService {
            log_sender: tx,
            telemetry_sender: telemetry_tx,
            health_sender: health_tx,
            should_fail,
        };

        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();

        tokio::spawn(async move {
            Server::builder()
                .add_service(AuditServiceServer::new(service))
                .serve_with_incoming(TcpListenerStream::new(listener))
                .await
                .unwrap();
        });

        (addr, rx, telemetry_rx, health_rx)
    }

    #[test]
    fn test_json_to_proto_conversion() {
        use test_utils::assert_eq;

        let lock_entry = LockServerLogEntry {
            creation_date: "2026-03-23T11:50:37.504Z".to_string(),
            event_time: "2026-03-23T11:50:37.504Z".to_string(),
            service: Some("test_app".to_string()),
            node_name: "12a8a3be-6593-4215-a42b-bbf5c4f5defa".to_string(),
            event_type: "Decision".to_string(),
            severity_level: Some("INFO".to_string()),
            action: "Jans::Action::\"Read\"".to_string(),
            decision_result: "ALLOW".to_string(),
            requested_resource: "Jans::Issue::\"random_id\"".to_string(),
            principal_id: Some("Jans::User::\"some_user\"".to_string()),
            client_id: Some("client-456".to_string()),
            context_information: None,
        };
        let proto_entry = log_json_to_proto(lock_entry);

        assert_eq!(
            proto_entry.creation_date,
            Some(Timestamp {
                seconds: 1_774_266_637,
                nanos: 504_000_000
            })
        );
        assert_eq!(
            proto_entry.event_time,
            Some(Timestamp {
                seconds: 1_774_266_637,
                nanos: 504_000_000,
            })
        );
        assert_eq!(proto_entry.service, "test_app");
        assert_eq!(
            proto_entry.node_name,
            "12a8a3be-6593-4215-a42b-bbf5c4f5defa"
        );
        assert_eq!(proto_entry.event_type, "Decision");
        assert_eq!(proto_entry.decision_result, "ALLOW");
        assert_eq!(proto_entry.severity_level, "INFO");
        assert_eq!(proto_entry.action, "Jans::Action::\"Read\"");
        assert_eq!(proto_entry.requested_resource, "Jans::Issue::\"random_id\"");
        assert_eq!(proto_entry.principal_id, "Jans::User::\"some_user\"");
        assert_eq!(proto_entry.client_id, "client-456");
        assert_eq!(proto_entry.jti, "");
    }

    #[test]
    fn test_partial_json_entry() {
        let lock_entry = LockServerLogEntry {
            creation_date: "2026-03-23T11:50:37.504Z".to_string(),
            event_time: "2026-03-23T11:50:37.504Z".to_string(),
            service: Some("minimal-service".to_string()),
            node_name: "node-1".to_string(),
            event_type: "Decision".to_string(),
            severity_level: None,
            action: "Test::Action".to_string(),
            decision_result: "ALLOW".to_string(),
            requested_resource: "Test::Resource".to_string(),
            principal_id: None,
            client_id: None,
            context_information: None,
        };
        let proto_entry = log_json_to_proto(lock_entry);

        assert_eq!(proto_entry.service, "minimal-service");
        assert_eq!(proto_entry.event_type, "Decision");
        assert_eq!(proto_entry.node_name, "node-1");
        assert!(proto_entry.creation_date.is_some());
    }

    #[tokio::test]
    async fn test_send_logs_success() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully");

        // Verify the server received the logs
        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert_ne!(received[0].node_name, "");
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let err = transport
            .prepare(
                &[],
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .expect_err("an empty batch has nothing to send");
        assert!(
            matches!(err, NothingToSend { count: 0 }),
            "expected NothingToSend {{ count: 0 }}, got {err:?}"
        );
        assert!(rx.try_recv().is_err(), "nothing should have been sent");
    }

    #[tokio::test]
    async fn test_send_logs_malformed_json() {
        let (addr, _, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![malformed_log_item()];

        let err = transport
            .prepare(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .expect_err("a batch with no mappable entry has nothing to send");
        assert!(
            matches!(err, NothingToSend { count: 1 }),
            "expected NothingToSend {{ count: 1 }}, got {err:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_server_error() {
        let (addr, _, _, _) = start_mock_server(true).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item()];

        let error = deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect_err("this should cause a server error");

        assert!(
            matches!(
                error,
                SendFailure::Final(FinalSend::Unsuccessful { ref message }) if message == "Server error"
            ),
            "success = false is Lock's answer to this batch, so it is final, got {error:?}",
        );
    }

    #[tokio::test]
    async fn test_send_logs_partial_malformed() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item(), malformed_log_item(), sample_log_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully despite malformed entries");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 2, "only valid entries should be forwarded");
        assert_eq!(received[0].service, "test_app");
        assert_eq!(received[1].service, "test_app");
    }

    #[tokio::test]
    async fn test_send_logs_multiple_entries() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item(), sample_log_item(), sample_log_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 3);
        assert_eq!(received[0].service, "test_app");
        assert_eq!(received[1].service, "test_app");
        assert_eq!(received[2].service, "test_app");
    }

    #[tokio::test]
    async fn test_send_logs_with_all_fields() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        let entry = &received[0];

        assert_eq!(entry.service, "test_app");
        assert_ne!(entry.node_name, "");
        assert_eq!(entry.event_type, "Decision");
        assert_eq!(entry.action, "Test");
        assert_eq!(entry.decision_result, "ALLOW");
        assert_eq!(entry.requested_resource, "Jans::Issue");
        assert_eq!(entry.principal_id, "Jans::User");
        assert_eq!(entry.jti, "");
    }

    #[tokio::test]
    async fn test_send_logs_with_missing_optional_fields() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert_ne!(received[0].node_name, "");
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries: Vec<_> = (0..100).map(|_| sample_log_item()).collect();

        deliver(
            &transport,
            &entries,
            AuditKind::Log(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 100);
    }

    #[tokio::test]
    async fn test_send_telemetry_success() {
        let (addr, _, mut telemetry_rx, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_metric_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Telemetry(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("telemetry should be sent successfully");

        let received = telemetry_rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert_ne!(received[0].node_name, "");
        assert_eq!(received[0].interval_secs, 60);
    }

    #[test]
    fn test_negative_timestamp_handling() {
        let lock_entry = LockServerLogEntry {
            creation_date: "1969-12-31T00:00:00Z".to_string(),
            event_time: "1969-12-31T00:00:00Z".to_string(),
            service: Some("test-service".to_string()),
            node_name: "node-1".to_string(),
            event_type: "System".to_string(),
            severity_level: Some("INFO".to_string()),
            action: "Test".to_string(),
            decision_result: "ALLOW".to_string(),
            requested_resource: "Jans::Issue".to_string(),
            principal_id: None,
            client_id: None,
            context_information: None,
        };
        let proto_entry = log_json_to_proto(lock_entry);

        assert_eq!(
            proto_entry.creation_date,
            Some(Timestamp {
                seconds: -86400,
                nanos: 0
            })
        );
        assert_eq!(
            proto_entry.event_time,
            Some(Timestamp {
                seconds: -86400,
                nanos: 0,
            })
        );
    }

    #[test]
    fn test_large_negative_timestamp() {
        let lock_entry = LockServerLogEntry {
            creation_date: "1900-01-01T00:00:00Z".to_string(),
            event_time: "1900-01-01T00:00:00Z".to_string(),
            service: Some("historical-service".to_string()),
            node_name: "node-1".to_string(),
            event_type: "Decision".to_string(),
            severity_level: None,
            action: "Test".to_string(),
            decision_result: "ALLOW".to_string(),
            requested_resource: "Test::Resource".to_string(),
            principal_id: None,
            client_id: None,
            context_information: None,
        };
        let proto_entry = log_json_to_proto(lock_entry);

        assert_eq!(
            proto_entry.creation_date.as_ref().unwrap().seconds,
            -2_208_988_800
        );
        assert_eq!(
            proto_entry.event_time.as_ref().unwrap().seconds,
            -2_208_988_800
        );
    }

    #[tokio::test]
    async fn test_send_health_success() {
        let (addr, _, _, mut health_rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_health_item()];

        deliver(
            &transport,
            &entries,
            AuditKind::Health(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("health check should be sent successfully");

        let received = health_rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert_eq!(received[0].node_name, "test-pdp");
        assert_eq!(received[0].status, "running");
        assert_eq!(received[0].engine_status.get("core").unwrap(), "success");
    }

    #[tokio::test]
    async fn test_send_health_empty() {
        let (addr, _, _, mut health_rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let err = transport
            .prepare(
                &[],
                &AuditKind::Health(format!("http://{addr}").parse().unwrap()),
            )
            .expect_err("an empty batch has nothing to send");
        assert!(
            matches!(err, NothingToSend { count: 0 }),
            "expected NothingToSend {{ count: 0 }}, got {err:?}"
        );
        assert!(
            health_rx.try_recv().is_err(),
            "nothing should have been sent"
        );
    }

    #[tokio::test]
    async fn test_send_health_server_error() {
        let (addr, _, _, _) = start_mock_server(true).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_health_item()];

        let error = deliver(
            &transport,
            &entries,
            AuditKind::Health(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect_err("this should cause a server error");

        assert!(
            matches!(
                error,
                SendFailure::Final(FinalSend::Unsuccessful { ref message }) if message == "Server error"
            ),
            "success = false is Lock's answer to this batch, so it is final, got {error:?}",
        );
    }

    #[tokio::test]
    async fn test_send_health_large_batch() {
        let (addr, _, _, mut health_rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries: Vec<_> = (0..100).map(|_| sample_health_item()).collect();

        deliver(
            &transport,
            &entries,
            AuditKind::Health(format!("http://{addr}").parse().unwrap()),
        )
        .await
        .expect("health checks should be sent successfully");

        let received = health_rx.try_recv().unwrap();
        assert_eq!(received.len(), 100);
        assert_eq!(received[0].service, "test_app");
        assert_eq!(received[99].service, "test_app");
    }

    #[test]
    fn test_health_json_to_proto_conversion() {
        let entry = LockServerHealthEntry {
            creation_date: "2026-03-23T11:50:37.504Z".to_string(),
            event_time: "2026-03-23T11:50:37.504Z".to_string(),
            service: "test_app".to_string(),
            node_name: "test-pdp".to_string(),
            status: "running".to_string(),
            engine_status: [("core".to_string(), HealthStatus::Success)]
                .into_iter()
                .collect(),
        };

        let proto = health_json_to_proto(entry);

        assert_eq!(
            proto.creation_date,
            Some(Timestamp {
                seconds: 1_774_266_637,
                nanos: 504_000_000
            })
        );
        assert_eq!(
            proto.event_time,
            Some(Timestamp {
                seconds: 1_774_266_637,
                nanos: 504_000_000,
            })
        );
        assert_eq!(proto.service, "test_app");
        assert_eq!(proto.node_name, "test-pdp");
        assert_eq!(proto.status, "running");
        assert_eq!(proto.engine_status.get("core").unwrap(), "success");
    }

    mod retries {
        use std::sync::Arc;
        use std::sync::atomic::{AtomicUsize, Ordering};
        use std::time::Duration;

        use tokio::net::TcpListener;
        use tokio::sync::mpsc;
        use tokio_stream::wrappers::TcpListenerStream;
        use tokio_util::sync::CancellationToken;
        use tonic::transport::Server;
        use tonic::{Request, Response, Status};

        use crate::lock::log_worker::AuditWorker;
        use crate::lock::proto::audit_service_server::{AuditService, AuditServiceServer};
        use crate::lock::proto::{self, AuditResponse, BulkLogRequest};
        use crate::lock::transport::AuditKind;
        use crate::lock::transport::grpc::GrpcTransport;
        use crate::lock::transport::test_utils::sample_log_item;

        const MAX_RETRIES: usize = 2;

        type Reply = fn() -> Result<AuditResponse, Status>;

        /// Answers every bulk log call with `reply` and counts the calls.
        struct ReplyingLock {
            reply: Reply,
            calls: Arc<AtomicUsize>,
        }

        #[tonic::async_trait]
        impl AuditService for ReplyingLock {
            async fn process_log(
                &self,
                _: Request<proto::LogRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                unimplemented!()
            }

            async fn process_bulk_log(
                &self,
                _: Request<BulkLogRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                self.calls.fetch_add(1, Ordering::SeqCst);
                (self.reply)().map(Response::new)
            }

            async fn process_telemetry(
                &self,
                _: Request<proto::TelemetryRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                unimplemented!()
            }

            async fn process_bulk_telemetry(
                &self,
                _: Request<proto::BulkTelemetryRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                unimplemented!()
            }

            async fn process_health(
                &self,
                _: Request<proto::HealthRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                unimplemented!()
            }

            async fn process_bulk_health(
                &self,
                _: Request<proto::BulkHealthRequest>,
            ) -> Result<Response<AuditResponse>, Status> {
                unimplemented!()
            }
        }

        /// Answers every call with `status`'s HTTP status and no `grpc-status`, as a
        /// proxy before Lock does when it refuses the request itself.
        #[derive(Clone)]
        struct ProxyAnswer {
            status: u16,
            calls: Arc<AtomicUsize>,
        }

        impl tonic::server::NamedService for ProxyAnswer {
            const NAME: &'static str = "io.jans.lock.audit.AuditService";
        }

        impl tonic::codegen::Service<tonic::codegen::http::Request<tonic::body::Body>> for ProxyAnswer {
            type Response = tonic::codegen::http::Response<tonic::body::Body>;
            type Error = std::convert::Infallible;
            type Future = std::future::Ready<Result<Self::Response, Self::Error>>;

            fn poll_ready(
                &mut self,
                _: &mut std::task::Context<'_>,
            ) -> std::task::Poll<Result<(), Self::Error>> {
                std::task::Poll::Ready(Ok(()))
            }

            fn call(
                &mut self,
                _: tonic::codegen::http::Request<tonic::body::Body>,
            ) -> Self::Future {
                self.calls.fetch_add(1, Ordering::SeqCst);
                let response = tonic::codegen::http::Response::builder()
                    .status(self.status)
                    .body(tonic::body::Body::default())
                    .expect("a status-only response builds");
                std::future::ready(Ok(response))
            }
        }

        /// A bound local port: the connections to serve and the origin to call.
        struct Listening {
            incoming: TcpListenerStream,
            origin: String,
        }

        async fn listen() -> Listening {
            let listener = TcpListener::bind("127.0.0.1:0")
                .await
                .expect("a local port is free");
            let addr = listener.local_addr().expect("the listener has an address");
            Listening {
                incoming: TcpListenerStream::new(listener),
                origin: format!("http://{addr}"),
            }
        }

        async fn lock(reply: Reply, calls: Arc<AtomicUsize>) -> String {
            let Listening { incoming, origin } = listen().await;
            tokio::spawn(
                Server::builder()
                    .add_service(AuditServiceServer::new(ReplyingLock { reply, calls }))
                    .serve_with_incoming(incoming),
            );
            origin
        }

        async fn proxy(status: u16, calls: Arc<AtomicUsize>) -> String {
            let Listening { incoming, origin } = listen().await;
            tokio::spawn(
                Server::builder()
                    .add_routes(tonic::service::Routes::new(ProxyAnswer { status, calls }))
                    .serve_with_incoming(incoming),
            );
            origin
        }

        /// Runs a log worker on one entry until it is sent or dropped.
        async fn flush_one_entry(origin: &str) {
            let transport = GrpcTransport::new(origin, "test-token", None).expect("transport");
            let mut worker = AuditWorker::new(
                Duration::from_secs(60),
                Arc::new(transport),
                AuditKind::Log(format!("{origin}/audit/log").parse().expect("valid URL")),
                None,
                u32::try_from(MAX_RETRIES).expect("small"),
                Duration::from_millis(1),
            );
            let (tx, rx) = mpsc::channel(1);
            tx.send(sample_log_item())
                .await
                .expect("the entry is queued");
            let cancel = CancellationToken::new();
            cancel.cancel();
            // Shutting down flushes the queued entry, retries included.
            worker.run(rx, cancel).await;
            drop(tx);
        }

        #[tokio::test]
        async fn lock_refusal_is_sent_once() {
            let cases: [(&str, Reply); 3] = [
                ("InvalidArgument", || Err(Status::invalid_argument("bad"))),
                ("PermissionDenied", || {
                    Err(Status::permission_denied("missing scope"))
                }),
                ("success = false", || {
                    Ok(AuditResponse {
                        success: false,
                        message: "refused".to_string(),
                    })
                }),
            ];
            for (name, reply) in cases {
                let calls = Arc::new(AtomicUsize::new(0));
                flush_one_entry(&lock(reply, calls.clone()).await).await;
                assert_eq!(
                    calls.load(Ordering::SeqCst),
                    1,
                    "{name} is Lock's answer to this batch, so it is not repeated"
                );
            }
        }

        #[tokio::test]
        async fn unavailable_is_retried() {
            let calls = Arc::new(AtomicUsize::new(0));
            let origin = lock(|| Err(Status::unavailable("restarting")), calls.clone()).await;

            flush_one_entry(&origin).await;

            assert_eq!(
                calls.load(Ordering::SeqCst),
                MAX_RETRIES + 1,
                "UNAVAILABLE may clear, so it is sent once and retried {MAX_RETRIES} times"
            );
        }

        #[tokio::test]
        async fn proxy_client_error_is_sent_once() {
            let calls = Arc::new(AtomicUsize::new(0));
            let origin = proxy(400, calls.clone()).await;

            flush_one_entry(&origin).await;

            assert_eq!(
                calls.load(Ordering::SeqCst),
                1,
                "a proxy's 400 without grpc-status is final, as over REST"
            );
        }
    }
}
