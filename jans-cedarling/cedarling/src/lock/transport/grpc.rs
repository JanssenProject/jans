// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! gRPC transport implementation for Lock Server communication.

use std::collections::HashMap;

use prost_types::Timestamp;
use serde::Deserialize;
use tonic::{Request, metadata::MetadataValue, transport::Channel};

use crate::lock::{
    proto::{BulkLogRequest, LogEntry, audit_service_client::AuditServiceClient},
    transport::{AuditTransport, SerializedLogEntry, TransportError, TransportResult},
};

/// JSON structure matching the serialized log entries
#[derive(Debug, Deserialize)]
struct JsonLogEntry {
    #[serde(default)]
    creation_date: Option<i64>,
    #[serde(default)]
    event_time: Option<i64>,
    #[serde(default)]
    service: String,
    #[serde(default)]
    node_name: String,
    #[serde(default)]
    event_type: String,
    #[serde(default)]
    severity_level: String,
    #[serde(default)]
    action: String,
    #[serde(default)]
    decision_result: String,
    #[serde(default)]
    requested_resource: String,
    #[serde(default)]
    principal_id: String,
    #[serde(default)]
    client_id: String,
    #[serde(default)]
    jti: String,
    #[serde(default)]
    context_information: HashMap<String, String>,
}

#[derive(Debug, Clone)]
pub(crate) struct GrpcTransport {
    client: AuditServiceClient<Channel>,
    access_token: String,
}

impl GrpcTransport {
    pub(crate) fn new(
        endpoint: impl Into<String>,
        access_token: &str,
    ) -> Result<Self, TransportError> {
        let channel = Channel::from_shared(endpoint.into())
            .map_err(|_| TransportError::InvalidUri)?
            .connect_lazy();

        Ok(Self {
            client: AuditServiceClient::new(channel),
            access_token: access_token.into(),
        })
    }
}

impl AuditTransport for GrpcTransport {
    async fn send_logs(&self, entries: &[SerializedLogEntry]) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let mut skipped = 0usize;
        let proto_entries: Vec<_> = entries
            .iter()
            .filter_map(|v| {
                if let Ok(entry) = serde_json::from_str::<JsonLogEntry>(v) {
                    Some(json_to_proto(entry))
                } else {
                    skipped += 1;
                    None
                }
            })
            .collect();

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

/// Convert a [`JsonLogEntry`] to a protobuf [`LogEntry`]
fn json_to_proto(json_entry: JsonLogEntry) -> LogEntry {
    LogEntry {
        creation_date: json_entry.creation_date.map(|secs| Timestamp {
            seconds: secs,
            nanos: 0,
        }),
        event_time: json_entry.event_time.map(|secs| Timestamp {
            seconds: secs,
            nanos: 0,
        }),
        service: json_entry.service,
        node_name: json_entry.node_name,
        event_type: json_entry.event_type,
        severity_level: json_entry.severity_level,
        action: json_entry.action,
        decision_result: json_entry.decision_result,
        requested_resource: json_entry.requested_resource,
        principal_id: json_entry.principal_id,
        client_id: json_entry.client_id,
        jti: json_entry.jti,
        context_information: json_entry.context_information,
    }
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
        let json_str = r#"{
            "creation_date": 1771092901,
            "event_time": 1771092951,
            "service": "test-service",
            "node_name": "node-1",
            "event_type": "access",
            "severity_level": "INFO",
            "action": "read",
            "decision_result": "ALLOW",
            "requested_resource": "/api/resource",
            "principal_id": "user-123",
            "client_id": "client-456",
            "jti": "jti-789",
            "context_information": {
                "ip": "127.0.0.1",
                "user_agent": "test-agent"
            }
        }"#;

        let json_entry: JsonLogEntry = serde_json::from_str(json_str).unwrap();
        let proto_entry = json_to_proto(json_entry);

        assert_eq!(
            proto_entry.creation_date,
            Some(Timestamp {
                seconds: 1_771_092_901,
                nanos: 0
            })
        );
        assert_eq!(
            proto_entry.event_time,
            Some(Timestamp {
                seconds: 1_771_092_951,
                nanos: 0,
            })
        );
        assert_eq!(proto_entry.service, "test-service");
        assert_eq!(proto_entry.node_name, "node-1");
        assert_eq!(proto_entry.event_type, "access");
        assert_eq!(proto_entry.decision_result, "ALLOW");
        assert_eq!(proto_entry.severity_level, "INFO");
        assert_eq!(proto_entry.action, "read");
        assert_eq!(proto_entry.requested_resource, "/api/resource");
        assert_eq!(proto_entry.principal_id, "user-123");
        assert_eq!(proto_entry.client_id, "client-456");
        assert_eq!(proto_entry.jti, "jti-789");
        assert_eq!(proto_entry.context_information.len(), 2);
        assert_eq!(
            proto_entry.context_information.get("ip"),
            Some(&"127.0.0.1".to_string())
        );
    }

    #[test]
    fn test_partial_json_entry() {
        let json_str = r#"{
            "service": "minimal-service",
            "event_type": "test"
        }"#;

        let json_entry: JsonLogEntry = serde_json::from_str(json_str).unwrap();
        let proto_entry = json_to_proto(json_entry);

        assert_eq!(proto_entry.service, "minimal-service");
        assert_eq!(proto_entry.event_type, "test");
        assert_eq!(proto_entry.node_name, "");
        assert!(proto_entry.creation_date.is_none());
    }

    #[tokio::test]
    async fn test_send_logs_success() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries = vec![
            r#"{
                "creation_date": 1771092901,
                "event_time": 1771092951,
                "service": "test-service",
                "node_name": "node-1",
                "event_type": "authorization",
                "severity_level": "INFO",
                "action": "permit",
                "decision_result": "allowed",
                "requested_resource": "/api/resource",
                "principal_id": "user-123",
                "client_id": "client-456",
                "jti": "token-789",
                "context_information": {
                    "ip": "127.0.0.1",
                    "user_agent": "test"
                }
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
        assert_eq!(received[0].service, "test-service");
        assert_eq!(received[0].node_name, "node-1");
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

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
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

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
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries = vec![
            r#"{
                "service": "test",
                "node_name": "node",
                "event_type": "test"
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
    async fn test_send_logs_multiple_entries() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries = vec![
            r#"{
                "service": "service-1",
                "node_name": "node-1",
                "event_type": "event-1"
            }"#
            .to_string()
            .into_boxed_str(),
            r#"{
                "service": "service-2",
                "node_name": "node-2",
                "event_type": "event-2"
            }"#
            .to_string()
            .into_boxed_str(),
            r#"{
                "service": "service-3",
                "node_name": "node-3",
                "event_type": "event-3"
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
    }

    #[tokio::test]
    async fn test_send_logs_with_all_fields() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries = vec![
            r#"{
                "creation_date": 1000,
                "event_time": 2000,
                "service": "full-service",
                "node_name": "full-node",
                "event_type": "full-event",
                "severity_level": "ERROR",
                "action": "deny",
                "decision_result": "forbidden",
                "requested_resource": "/secret",
                "principal_id": "admin",
                "client_id": "admin-client",
                "jti": "jwt-id",
                "context_information": {
                    "key1": "value1",
                    "key2": "value2"
                }
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

        assert_eq!(entry.creation_date.as_ref().unwrap().seconds, 1000);
        assert_eq!(entry.event_time.as_ref().unwrap().seconds, 2000);
        assert_eq!(entry.service, "full-service");
        assert_eq!(entry.node_name, "full-node");
        assert_eq!(entry.event_type, "full-event");
        assert_eq!(entry.severity_level, "ERROR");
        assert_eq!(entry.action, "deny");
        assert_eq!(entry.decision_result, "forbidden");
        assert_eq!(entry.requested_resource, "/secret");
        assert_eq!(entry.principal_id, "admin");
        assert_eq!(entry.client_id, "admin-client");
        assert_eq!(entry.jti, "jwt-id");
        assert_eq!(entry.context_information.len(), 2);
        assert_eq!(entry.context_information.get("key1").unwrap(), "value1");
    }

    #[tokio::test]
    async fn test_send_logs_with_missing_optional_fields() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries = vec![r#"{ "service": "minimal" }"#.to_string().into_boxed_str()];

        transport
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");

        let received = rx.try_recv().unwrap();
        assert_eq!(received.len(), 1);
        assert_eq!(received[0].service, "minimal");
        assert_eq!(received[0].node_name, "");
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let (addr, mut rx) = start_mock_server(false).await;
        let transport = GrpcTransport::new(format!("http://{addr}"), "test-token").unwrap();

        let entries: Vec<_> = (0..100)
            .map(|i| {
                json!({
                    "service": format!("service-{i}"),
                    "node_name": "node",
                    "event_type": "test"
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
}
