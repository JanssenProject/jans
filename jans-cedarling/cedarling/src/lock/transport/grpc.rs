// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! gRPC transport implementation for Lock Server communication.

#[cfg(not(target_arch = "wasm32"))]
use std::time::Duration;

use async_trait::async_trait;
use prost_types::Timestamp;
#[cfg(not(target_arch = "wasm32"))]
use tonic::transport::{Channel, ClientTlsConfig};
use tonic::{Request, metadata::MetadataValue};
#[cfg(target_arch = "wasm32")]
use tonic_web_wasm_client::Client;

use crate::{
    lock::{
        LockLogEntry,
        proto::{
            BulkHealthRequest, BulkLogRequest, BulkTelemetryRequest, HealthEntry, LogEntry,
            TelemetryEntry, audit_service_client::AuditServiceClient,
        },
        transport::{
            AuditItem, AuditKind, AuditTransport, TransportError, TransportResult,
            mapping::{self, LockServerHealthEntry, LockServerLogEntry, LockServerMetricsEntry},
        },
    },
    log::{LogWriter, Logger},
};

#[cfg(not(target_arch = "wasm32"))]
/// Duration to wait for each gRPC request
const GRPC_REQUEST_TIMEOUT: Duration = Duration::from_secs(10);

pub(crate) struct GrpcTransport {
    #[cfg(not(target_arch = "wasm32"))]
    client: AuditServiceClient<Channel>,
    #[cfg(target_arch = "wasm32")]
    client: AuditServiceClient<Client>,
    access_token: String,
    logger: Option<Logger>,
}

impl GrpcTransport {
    #[cfg_attr(target_arch = "wasm32", allow(clippy::unnecessary_wraps))]
    /// Constructs a new [`GrpcTransport`]
    pub(crate) fn new(
        endpoint: impl Into<String>,
        access_token: &str,
        logger: Option<Logger>,
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
            access_token: access_token.into(),
            logger,
        })
    }
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for GrpcTransport {
    async fn send(&self, entries: &[AuditItem], audit_kind: &AuditKind) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let token: MetadataValue<_> = format!("Bearer {}", self.access_token).parse()?;
        let mut client = self.client.clone();
        let warn = |msg| self.logger.log_any(LockLogEntry::warn(msg));

        let response = match audit_kind {
            AuditKind::Log(_) => {
                let entries: Vec<LogEntry> =
                    mapping::map_entries::<LockServerLogEntry>(entries, warn)?
                        .into_iter()
                        .map(log_json_to_proto)
                        .collect();

                let mut request = Request::new(BulkLogRequest { entries });
                request
                    .metadata_mut()
                    .insert("authorization", token.clone());

                client.process_bulk_log(request).await?
            },
            AuditKind::Telemetry(_) => {
                let entries: Vec<TelemetryEntry> =
                    mapping::map_entries::<LockServerMetricsEntry>(entries, warn)?
                        .into_iter()
                        .map(telemetry_json_to_proto)
                        .collect();

                let mut request = Request::new(BulkTelemetryRequest { entries });
                request
                    .metadata_mut()
                    .insert("authorization", token.clone());

                client.process_bulk_telemetry(request).await?
            },
            AuditKind::Health(_) => {
                let proto_entries: Vec<HealthEntry> =
                    mapping::map_entries::<LockServerHealthEntry>(entries, warn)?
                        .into_iter()
                        .map(health_json_to_proto)
                        .collect();

                let mut request = Request::new(BulkHealthRequest {
                    entries: proto_entries,
                });
                request.metadata_mut().insert("authorization", token);

                client.process_bulk_health(request).await?
            },
        };

        let inner = response.into_inner();
        if !inner.success {
            return Err(TransportError::GrpcServer(format!(
                "Server reported failure: {}",
                inner.message
            )));
        }

        Ok(())
    }
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

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("logs should be sent successfully");

        // Verify the server received the logs
        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert!(!received[0].node_name.is_empty());
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        transport
            .send(
                &Vec::<AuditItem>::new(),
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("logs should be sent successfully");

        // Should not have sent anything
        assert!(rx.try_recv().is_err());
    }

    #[tokio::test]
    async fn test_send_logs_malformed_json() {
        let (addr, _, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![malformed_log_item()];

        let error = transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect_err("this should cause a serialization error");
        assert!(
            matches!(error, TransportError::Serialization(_)),
            "expected serialization error, got {error:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_server_error() {
        let (addr, _, _, _) = start_mock_server(true).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item()];

        let error = transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect_err("this should cause a server error");

        assert!(
            matches!(error, TransportError::GrpcServer(_)),
            "expected server error, got {error:?}",
        );
    }

    #[tokio::test]
    async fn test_send_logs_partial_malformed() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_log_item(), malformed_log_item(), sample_log_item()];

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
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

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
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

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        let entry = &received[0];

        assert_eq!(entry.service, "test_app");
        assert!(!entry.node_name.is_empty());
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

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert!(!received[0].node_name.is_empty());
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let (addr, mut rx, _, _) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries: Vec<_> = (0..100).map(|_| sample_log_item()).collect();

        transport
            .send(
                &entries,
                &AuditKind::Log(format!("http://{addr}").parse().unwrap()),
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

        transport
            .send(
                &entries,
                &AuditKind::Telemetry(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("telemetry should be sent successfully");

        let received = telemetry_rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert!(!received[0].node_name.is_empty());
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

        transport
            .send(
                &entries,
                &AuditKind::Health(format!("http://{addr}").parse().unwrap()),
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

        transport
            .send(
                &Vec::<AuditItem>::new(),
                &AuditKind::Health(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect("empty health check should succeed");

        assert!(health_rx.try_recv().is_err());
    }

    #[tokio::test]
    async fn test_send_health_server_error() {
        let (addr, _, _, _) = start_mock_server(true).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![sample_health_item()];

        let error = transport
            .send(
                &entries,
                &AuditKind::Health(format!("http://{addr}").parse().unwrap()),
            )
            .await
            .expect_err("this should cause a server error");

        assert!(
            matches!(error, TransportError::GrpcServer(_)),
            "expected server error, got {error:?}",
        );
    }

    #[tokio::test]
    async fn test_send_health_large_batch() {
        let (addr, _, _, mut health_rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries: Vec<_> = (0..100).map(|_| sample_health_item()).collect();

        transport
            .send(
                &entries,
                &AuditKind::Health(format!("http://{addr}").parse().unwrap()),
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
}
