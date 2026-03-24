// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use std::sync::Arc;

use async_trait::async_trait;
use reqwest::Client;
use serde_json::Value;

use crate::{
    lock::{
        LockLogEntry,
        transport::{
            AuditKind, AuditTransport, SerializedAuditEntry, TransportError, TransportResult,
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

    fn deserialize_entries(
        &self,
        entries: &[SerializedAuditEntry],
    ) -> Result<Vec<Value>, TransportError> {
        let mut skipped = 0usize;
        let logs: Vec<Value> = entries
            .iter()
            .filter_map(|v| {
                if let Ok(log) = serde_json::from_str::<Value>(v) {
                    Some(log)
                } else {
                    skipped += 1;
                    None
                }
            })
            .collect();

        if skipped > 0 {
            self.logger.log_any(LockLogEntry::warn(format!(
                "skipped {skipped} malformed entries"
            )));
        }

        if logs.is_empty() {
            return Err(TransportError::Serialization(format!(
                "all {skipped} entries were malformed, nothing to send"
            )));
        }

        Ok(logs)
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

        let logs = self.deserialize_entries(entries)?;

        self.client
            .post(audit_kind.url().as_str())
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

    #[tokio::test]
    async fn test_send_logs_success() {
        let mut server = Server::new_async().await;
        let mock = mock_log_endpoint(&mut server);

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), None);

        let entries = vec![
            json!({"level": "INFO", "message": "test1"})
                .to_string()
                .into_boxed_str(),
            json!({"level": "DEBUG", "message": "test2"})
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
            json!({"level": "INFO", "message": "test"})
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
            json!({"level": "INFO", "message": "test"})
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
            json!({"level": "INFO", "message": "test"})
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
            .map(|i| {
                json!({"level": "INFO", "message": format!("test{}", i), "index": i})
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
}
