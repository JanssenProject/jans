// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use std::sync::Arc;

use async_trait::async_trait;
use reqwest::Client;
use url::Url;

use crate::{
    lock::{
        LockLogEntry,
        transport::{
            AuditTransport, SerializedLogEntry, TransportError, TransportResult,
            mapping::{CedarlingLogEntry, LockServerLogEntry},
        },
    },
    log::{LogWriter, Logger},
};

pub(crate) struct RestTransport {
    client: Arc<Client>,
    log_endpoint: Url,
    logger: Option<Logger>,
}

impl RestTransport {
    /// Construct a new [`RestTransport`]
    pub(crate) fn new(client: Arc<Client>, log_endpoint: Url, logger: Option<Logger>) -> Self {
        Self {
            client,
            log_endpoint,
            logger,
        }
    }
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl AuditTransport for RestTransport {
    async fn send_logs(&self, entries: &[SerializedLogEntry]) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

        let logs: Vec<LockServerLogEntry> = entries
            .iter()
            .filter_map(|v| {
                serde_json::from_str::<CedarlingLogEntry>(v)
                    .ok()
                    .and_then(|entry| LockServerLogEntry::try_from(entry).ok())
            })
            .collect();

        let skipped = entries.len() - logs.len();
        if skipped > 0 {
            self.logger.log_any(LockLogEntry::warn(format!(
                "skipped {skipped} entries because they were malformed"
            )));
        }

        if logs.is_empty() {
            return Err(TransportError::Serialization(format!(
                "all {skipped} entries were malformed, nothing to send"
            )));
        }

        self.client
            .post(self.log_endpoint.as_ref())
            .json(&logs)
            .send()
            .await?
            .error_for_status()?;

        Ok(())
    }
}

#[cfg(test)]
mod test {
    use super::*;

    use mockito::{Server, ServerGuard};
    use serde_json::json;

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

    #[tokio::test]
    async fn test_send_logs_success() {
        let mut server = Server::new_async().await;
        let mock = mock_log_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), endpoint, None);

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
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");
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
        let transport = RestTransport::new(create_test_client(), endpoint, None);

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
            .send_logs(&entries)
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
        let transport = RestTransport::new(create_test_client(), endpoint, None);

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
            .send_logs(&entries)
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
        let transport = RestTransport::new(create_test_client(), endpoint, None);

        let entries = vec![
            "not valid json".to_string().into_boxed_str(),
            "{ invalid json }".to_string().into_boxed_str(),
        ];

        let result = transport.send_logs(&entries).await;
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
        let transport = RestTransport::new(create_test_client(), endpoint, None);

        // Send 1000 entries
        let entries: Vec<_> = (0..1000)
            .map(|_i| {
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
            .send_logs(&entries)
            .await
            .expect("logs should be sent successfully");
        mock.assert();
    }
}
