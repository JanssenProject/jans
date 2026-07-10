// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use async_trait::async_trait;

use crate::{
    http::HttpClient,
    lock::{
        LockLogEntry,
        transport::{
            AuditItem, AuditKind, AuditTransport, TransportResult,
            mapping::{self, LockServerHealthEntry, LockServerLogEntry, LockServerMetricsEntry},
        },
    },
    log::{LogWriter, Logger},
};

pub(crate) struct RestTransport {
    client: HttpClient,
    logger: Option<Logger>,
}

impl RestTransport {
    /// Construct a new [`RestTransport`]
    pub(crate) fn new(client: HttpClient, logger: Option<Logger>) -> Self {
        Self { client, logger }
    }
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for RestTransport {
    async fn send(&self, entries: &[AuditItem], audit_kind: &AuditKind) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let warn = |msg| self.logger.log_any(LockLogEntry::warn(msg));
        match audit_kind {
            AuditKind::Log(url) => {
                let payloads: Vec<LockServerLogEntry> = mapping::map_entries(entries, "log", warn)?;
                self.client
                    .raw_client
                    .post(url.as_str())
                    .json(&payloads)
                    .send()
                    .await?
                    .error_for_status()?;
            },
            AuditKind::Telemetry(url) => {
                let payloads: Vec<LockServerMetricsEntry> =
                    mapping::map_entries(entries, "telemetry", warn)?;
                self.client
                    .raw_client
                    .post(url.as_str())
                    .json(&payloads)
                    .send()
                    .await?
                    .error_for_status()?;
            },
            AuditKind::Health(url) => {
                let payloads: Vec<LockServerHealthEntry> =
                    mapping::map_entries(entries, "health", warn)?;
                self.client
                    .raw_client
                    .post(url.as_str())
                    .json(&payloads)
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
    use crate::{HttpClientConfig, lock::transport::TransportError};

    use super::*;

    use mockito::{Server, ServerGuard};
    use url::Url;

    use crate::lock::transport::test_utils::{
        sample_health_item, sample_log_item, sample_metric_item,
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

        let entries = vec![sample_log_item()];

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

        let entries = vec![sample_log_item()];

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

        let entries = vec![sample_log_item()];

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
    async fn test_send_logs_all_invalid_dropped() {
        use crate::common::app_types::{ApplicationName, PdpID};
        use crate::lock::transport::AuditPayload;
        use crate::log::{
            BaseLogEntry, Decision, DecisionLogEntry, DiagnosticsSummary, LogTokensInfo,
        };

        let endpoint: Url = "http://localhost:8080/audit/log/bulk".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        // Build a log item whose mapping fails validation
        let mut base = BaseLogEntry::new_decision(crate::log::gen_uuid7());
        base.timestamp = Some("2026-03-23T11:50:37.504Z".to_string());
        let entry = DecisionLogEntry {
            base,
            policystore_id: "store".into(),
            policystore_version: "1.0".into(),
            principal: vec!["Jans::User".into()],
            lock_client_id: None,
            action: String::new(),
            resource: "Jans::Issue".to_string(),
            decision: Decision::Allow,
            tokens: LogTokensInfo::empty(),
            decision_time_micro_sec: 1,
            diagnostics: DiagnosticsSummary {
                reason: std::collections::HashSet::default(),
                errors: Vec::new(),
            },
            pushed_data: None,
        };
        let entries = vec![AuditItem {
            payload: AuditPayload::Decision(Box::new(entry)),
            pdp_id: PdpID::new(),
            app_name: Some(ApplicationName::from("test_app".to_string())),
        }];

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
        let entries = (0..1000).map(|_| sample_log_item()).collect::<Vec<_>>();

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

        let entries = vec![sample_metric_item()];

        transport
            .send(&entries, &AuditKind::Telemetry(endpoint))
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

        transport
            .send(&entries, &AuditKind::Health(endpoint))
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

        transport
            .send(&[], &AuditKind::Health(endpoint))
            .await
            .expect("health check should be sent successfully");
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

        let error = transport
            .send(&entries, &AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a server error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
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

        let error = transport
            .send(&entries, &AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a client error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
        );
        mock.assert();
    }

    #[tokio::test]
    async fn test_send_health_network_failure() {
        let endpoint: Url = "http://localhost:1/invalid".parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![sample_health_item()];

        let error = transport
            .send(&entries, &AuditKind::Health(endpoint))
            .await
            .expect_err("this should cause a network error");
        assert!(
            matches!(error, TransportError::Rest(_)),
            "expected reqwest error, got {error:?}"
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

        transport
            .send(&entries, &AuditKind::Health(endpoint))
            .await
            .expect("health checks should be sent successfully");
        mock.assert();
    }
}
