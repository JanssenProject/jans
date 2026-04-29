// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use std::sync::Arc;

use async_trait::async_trait;
use reqwest::Client;

use crate::{
    lock::{
        LockLogEntry,
        transport::{
            self, AuditKind, AuditTransport, SerializedAuditEntry, TransportResult,
            mapping::{
                CedarlingLogEntry, CedarlingMetricsEntry, LockServerLogEntry,
                LockServerMetricsEntry,
            },
        },
    },
    log::{LogWriter, Logger},
};

pub(crate) struct RestTransport {
    client: Arc<Client>,
    logger: Option<Logger>,
}

impl RestTransport {
    /// Construct a new [`RestTransport`]
    pub(crate) fn new(client: Arc<Client>, logger: Option<Logger>) -> Self {
        Self { client, logger }
    }
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for RestTransport {
    async fn send(
        &self,
        entries: &[SerializedAuditEntry],
        audit_kind: &AuditKind,
    ) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let warn = |msg| self.logger.log_any(LockLogEntry::warn(msg));
        match audit_kind {
            AuditKind::Log(url) => {
                let entries = transport::deserialize_entries::<
                    LockServerLogEntry,
                    CedarlingLogEntry,
                >(entries, "log", warn)?;

                self.client
                    .post(url.as_str())
                    .json(&entries)
                    .send()
                    .await?
                    .error_for_status()?;
            },
            AuditKind::Telemetry(url) => {
                let entries = transport::deserialize_entries::<
                    LockServerMetricsEntry,
                    CedarlingMetricsEntry,
                >(entries, "telemetry", warn)?;

                self.client
                    .post(url.as_str())
                    .json(&entries)
                    .send()
                    .await?
                    .error_for_status()?;
            },
        }

        Ok(())
    }
}

#[cfg(test)]
mod test {
    use crate::lock::transport::TransportError;

    use super::*;

    use mockito::{Server, ServerGuard};
    use serde_json::json;
    use url::Url;

    fn create_test_client() -> Arc<Client> {
        Arc::new(Client::builder().build().unwrap())
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

        let entries = vec![
            json!({
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "INFO",
                "action": "Test",
                "decision": "ALLOW",
                "principal": ["Jans::User"],
                "resource": "Jans::Issue",
                "application_id": "test_app",
                "pdp_id": "test-pdp"
            })
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send(&entries, &AuditKind::Log(endpoint))
            .await
            .expect("logs should be sent successfully");
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_logs_empty() {
        let server = Server::new_async().await;
        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        transport
            .send(&[], &AuditKind::Log(endpoint))
            .await
            .expect("logs should be sent successfully");
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

        let entries = vec![
            json!({
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "INFO",
                "action": "Test",
                "decision": "ALLOW",
                "principal": ["Jans::User"],
                "resource": "Jans::Issue",
                "application_id": "test_app",
                "pdp_id": "test-pdp"
            })
            .to_string()
            .into_boxed_str(),
        ];

        let error = transport
            .send(&entries, &AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
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

        let entries = vec![
            json!({
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "INFO",
                "action": "Test",
                "decision": "ALLOW",
                "principal": ["Jans::User"],
                "resource": "Jans::Issue",
                "application_id": "test_app",
                "pdp_id": "test-pdp"
            })
            .to_string()
            .into_boxed_str(),
        ];

        let error = transport
            .send(&entries, &AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_logs_network_failure() {
        let endpoint: Url = "http://localhost:1/invalid".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![
            json!({
                "timestamp": "2026-03-23T11:50:37.504Z",
                "log_kind": "Decision",
                "level": "INFO",
                "action": "Test",
                "decision": "ALLOW",
                "principal": ["Jans::User"],
                "resource": "Jans::Issue",
                "application_id": "test_app",
                "pdp_id": "test-pdp"
            })
            .to_string()
            .into_boxed_str(),
        ];

        let error = transport
            .send(&entries, &AuditKind::Log(endpoint))
            .await
            .expect_err("this should cause a network error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
        );
    }

    #[tokio::test]
    async fn test_send_logs_malformed_json() {
        let endpoint: Url = "http://localhost:8080/audit/log/bulk".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![
            "not valid json".to_string().into_boxed_str(),
            "{ invalid json }".to_string().into_boxed_str(),
        ];

        let result = transport.send(&entries, &AuditKind::Log(endpoint)).await;
        assert!(matches!(
            result.unwrap_err(),
            TransportError::Serialization(_)
        ));
    }

    #[tokio::test]
    async fn test_send_logs_large_batch() {
        let mut server = Server::new_async().await;
        let mock = mock_log_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        // Send 1000 entries
        let entries: Vec<_> = (0..1000)
            .map(|_| {
                json!({
                    "timestamp": "2026-03-23T11:50:37.504Z",
                    "log_kind": "Decision",
                    "level": "INFO",
                    "action": "Test",
                    "decision": "ALLOW",
                    "principal": ["Jans::User"],
                    "resource": "Jans::Issue",
                    "application_id": "test_app",
                    "pdp_id": "test-pdp"
                })
                .to_string()
                .into_boxed_str()
            })
            .collect();

        transport
            .send(&entries, &AuditKind::Log(endpoint))
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

        let entries = vec![
            json!({
                "id": "f3c80a24-4608-45b8-adc3-a74f2841e156",
                "request_id": "019d6842-7577-7e43-adfd-46e2bb275405",
                "timestamp": "2026-04-07T17:04:39.162Z",
                "log_kind": "Metric",
                "policy_stats": {
                    "555da5d85403f35ea76519ed1a18a33989f855bf1cf8_allow": 6,
                    "555da5d85403f35ea76519ed1a18a33989f855bf1cf8": 7,
                    "555da5d85403f35ea76519ed1a18a33989f855bf1cf8_deny": 1
                },
                "error_counters": {
                    "parse_error": 0,
                    "validation_error": 0
                },
                "operational_stats": {
                    "evaluation_requests": 100,
                    "memory_usage": 240
                },
                "interval_secs": 60,
                "pdp_id": "test-pdp",
                "application_id": "test_app"
            })
            .to_string()
            .into_boxed_str(),
        ];

        transport
            .send(&entries, &AuditKind::Telemetry(endpoint))
            .await
            .expect("telemetry should be sent successfully");

        mock.assert();
    }
}
