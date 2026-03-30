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
        proto::{BulkLogRequest, LogEntry, audit_service_client::AuditServiceClient},
        transport::{
            AuditTransport, SerializedLogEntry, TransportError, TransportResult,
            mapping::{CedarlingLogEntry, LockServerLogEntry},
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
    async fn send_logs(&self, entries: &[SerializedLogEntry]) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let proto_entries: Vec<_> = entries
            .iter()
            .filter_map(|v| {
                serde_json::from_str::<CedarlingLogEntry>(v)
                    .ok()
                    .and_then(|entry| LockServerLogEntry::try_from(entry).ok())
                    .map(json_to_proto)
            })
            .collect();

        let skipped = entries.len() - proto_entries.len();
        if skipped > 0 {
            self.logger.log_any(LockLogEntry::warn(format!(
                "skipped {skipped} entries because they were malformed"
            )));
        }

        if proto_entries.is_empty() {
            return Err(TransportError::Serialization(format!(
                "all {skipped} entries were malformed, nothing to send"
            )));
        }

        let mut request = Request::new(BulkLogRequest {
            entries: proto_entries,
        });
        let token: MetadataValue<_> = format!("Bearer {}", self.access_token).parse()?;
        request.metadata_mut().insert("authorization", token);

        let mut client = self.client.clone();
        let response = client.process_bulk_log(request).await?;

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
fn json_to_proto(entry: LockServerLogEntry) -> LogEntry {
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
    use serde_json::json;
    use tokio::{net::TcpListener, sync::mpsc};
    use tokio_stream::wrappers::TcpListenerStream;
    use tonic::{Response, Status, transport::Server};

    use crate::lock::proto::{
        self, AuditResponse,
        audit_service_server::{AuditService, AuditServiceServer},
    };

    // Mock gRPC server for testing
    #[derive(Debug)]
    struct MockAuditService {
        log_sender: mpsc::UnboundedSender<Vec<LogEntry>>,
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
            _: Request<proto::BulkHealthRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            unimplemented!()
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
            _: Request<proto::BulkTelemetryRequest>,
        ) -> Result<Response<AuditResponse>, Status> {
            unimplemented!()
        }
    }

    async fn start_mock_server(
        should_fail: bool,
    ) -> (SocketAddr, mpsc::UnboundedReceiver<Vec<LogEntry>>) {
        let (tx, rx) = mpsc::unbounded_channel();
        let service = MockAuditService {
            log_sender: tx,
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

        (addr, rx)
    }

    #[test]
    fn test_json_to_proto_conversion() {
        use test_utils::assert_eq;

        let json_str = r#"{
            "timestamp": "2026-03-23T11:50:37.504Z",
            "log_kind": "Decision",
            "level": "INFO",
            "action": "Jans::Action::\"Read\"",
            "decision": "ALLOW",
            "principal": ["Jans::User::\"some_user\""],
            "resource": "Jans::Issue::\"random_id\"",
            "application_id": "test_app",
            "pdp_id": "12a8a3be-6593-4215-a42b-bbf5c4f5defa",
            "lock_client_id": "client-456"
        }"#;

        let json_entry: CedarlingLogEntry = serde_json::from_str(json_str).unwrap();
        let lock_entry = LockServerLogEntry::try_from(json_entry).unwrap();
        let proto_entry = json_to_proto(lock_entry);

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
        let json_str = r#"{
            "timestamp": "2026-03-23T11:50:37.504Z",
            "application_id": "minimal-service",
            "pdp_id": "node-1",
            "log_kind": "Decision",
            "decision": "ALLOW",
            "action": "Test::Action",
            "resource": "Test::Resource"
        }"#;

        let json_entry: CedarlingLogEntry = serde_json::from_str(json_str).unwrap();
        let lock_entry = LockServerLogEntry::try_from(json_entry).unwrap();
        let proto_entry = json_to_proto(lock_entry);

        assert_eq!(proto_entry.service, "minimal-service");
        assert_eq!(proto_entry.event_type, "Decision");
        assert_eq!(proto_entry.node_name, "node-1");
        assert!(proto_entry.creation_date.is_some());
    }

    #[tokio::test]
    async fn test_send_logs_success() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "INFO",
                "action": "Jans::Action::\"Read\"",
                "decision": "ALLOW",
                "principal": ["Jans::User::\"some_user\""],
                "resource": "Jans::Issue::\"random_id\"",
                "application_id": "test_app",
                "pdp_id": "12a8a3be-6593-4215-a42b-bbf5c4f5defa"
            }"#
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        // Verify the server received the logs
        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "test_app");
        assert_eq!(
            received[0].node_name,
            "12a8a3be-6593-4215-a42b-bbf5c4f5defa"
        );
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        transport
            .send_logs(&[])
            .await
            .expect("logs should be sent successfully");

        // Should not have sent anything
        assert!(rx.try_recv().is_err());
    }

    #[tokio::test]
    async fn test_send_logs_malformed_json() {
        let (addr, _rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec!["not valid json".to_string().into_boxed_str()];

        let error = transport
            .send_logs(&entries)
            .await
            .expect_err("this should cause a serialization error");
        assert!(
            matches!(error, TransportError::Serialization(_)),
            "expected serialization error, got {error:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_server_error() {
        let (addr, _) = start_mock_server(true).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
                "timestamp": "2026-03-23T11:50:37.504Z",
                "application_id": "test",
                "pdp_id": "node",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
        ];

        let error = transport
            .send_logs(&entries)
            .await
            .expect_err("this should cause a server error");

        assert!(
            matches!(error, TransportError::GrpcServer(_)),
            "expected server error, got {error:?}",
        );
    }

    #[tokio::test]
    async fn test_send_logs_partial_malformed() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
                "timestamp": "2026-03-23T11:50:37.504Z",
                "application_id": "valid-service-1",
                "pdp_id": "node-1",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
            "not valid json".to_string().into_boxed_str(),
            r#"{
                "timestamp": "2026-03-23T11:50:37.506Z",
                "application_id": "valid-service-2",
                "pdp_id": "node-2",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully despite malformed entries");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 2, "only valid entries should be forwarded");
        assert_eq!(received[0].service, "valid-service-1");
        assert_eq!(received[1].service, "valid-service-2");
    }

    #[tokio::test]
    async fn test_send_logs_multiple_entries() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
                "timestamp": "2026-03-23T11:50:37.504Z",
                "application_id": "service-1",
                "pdp_id": "node-1",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
            r#"{
                "timestamp": "2026-03-23T11:50:37.505Z",
                "application_id": "service-2",
                "pdp_id": "node-2",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
            r#"{
                "timestamp": "2026-03-23T11:50:37.506Z",
                "application_id": "service-3",
                "pdp_id": "node-3",
                "log_kind": "Decision",
                "decision": "ALLOW",
                "action": "Test::Action",
                "resource": "Test::Resource"
            }"#
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 3);
        assert_eq!(received[0].service, "service-1");
        assert_eq!(received[1].service, "service-2");
        assert_eq!(received[2].service, "service-3");
    }

    #[tokio::test]
    async fn test_send_logs_with_all_fields() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "ERROR",
                "action": "Jans::Action::\"Deny\"",
                "decision": "DENY",
                "principal": ["Jans::User::\"admin\""],
                "resource": "Jans::Issue::\"secret\"",
                "application_id": "full-service",
                "pdp_id": "full-node",
                "lock_client_id": "admin-client",
                "extra_field": "extra_value"
            }"#
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        let entry = &received[0];

        assert_eq!(entry.service, "full-service");
        assert_eq!(entry.node_name, "full-node");
        assert_eq!(entry.event_type, "Decision");
        assert_eq!(entry.severity_level, "ERROR");
        assert_eq!(entry.action, "Jans::Action::\"Deny\"");
        assert_eq!(entry.decision_result, "DENY");
        assert_eq!(entry.requested_resource, "Jans::Issue::\"secret\"");
        assert_eq!(entry.principal_id, "Jans::User::\"admin\"");
        assert_eq!(entry.client_id, "admin-client");
        assert_eq!(entry.jti, "");
    }

    #[tokio::test]
    async fn test_send_logs_with_missing_optional_fields() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries = vec![
            r#"{
            "timestamp": "2026-03-23T11:50:37.504Z",
            "application_id": "minimal",
            "pdp_id": "test-pdp",
            "log_kind": "Decision",
            "decision": "ALLOW",
            "action": "Test::Action",
            "resource": "Test::Resource"
        }"#
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "minimal");
        assert_eq!(received[0].node_name, "test-pdp");
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token", None).unwrap();

        let entries: Vec<_> = (0..100)
            .map(|i| {
                json!({
                    "timestamp": "2026-03-23T11:50:37.504Z",
                    "log_kind": "System",
                    "level": "INFO",
                    "action": "Test",
                    "decision": "ALLOW",
                    "principal": ["Jans::User"],
                    "resource": "Jans::Issue",
                    "application_id": format!("service-{i}"),
                    "pdp_id": "node"
                })
                .to_string()
                .into_boxed_str()
            })
            .collect();

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 100);
    }

    #[test]
    fn test_negative_timestamp_handling() {
        let json_str = r#"{
            "timestamp": "1969-12-31T00:00:00Z",
            "log_kind": "System",
            "level": "INFO",
            "action": "Test",
            "decision": "ALLOW",
            "principal": [],
            "resource": "Jans::Issue",
            "application_id": "test-service",
            "pdp_id": "node-1"
        }"#;

        let json_entry: CedarlingLogEntry = serde_json::from_str(json_str).unwrap();
        let lock_entry = LockServerLogEntry::try_from(json_entry).unwrap();
        let proto_entry = json_to_proto(lock_entry);

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
        let json_str = r#"{
            "timestamp": "1900-01-01T00:00:00Z",
            "log_kind": "Decision",
            "action": "Test",
            "decision": "ALLOW",
            "resource": "Test::Resource",
            "application_id": "historical-service",
            "pdp_id": "node-1"
        }"#;

        let json_entry: CedarlingLogEntry = serde_json::from_str(json_str).unwrap();
        let lock_entry = LockServerLogEntry::try_from(json_entry).unwrap();
        let proto_entry = json_to_proto(lock_entry);

        assert_eq!(
            proto_entry.creation_date.as_ref().unwrap().seconds,
            -2_208_988_800
        );
        assert_eq!(
            proto_entry.event_time.as_ref().unwrap().seconds,
            -2_208_988_800
        );
    }
}
