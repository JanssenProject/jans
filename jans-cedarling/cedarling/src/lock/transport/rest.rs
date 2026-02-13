// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! REST transport implementation for Lock Server communication.

use std::sync::Arc;

use reqwest::Client;
use serde_json::Value;
use url::Url;

use crate::lock::transport::{AuditTransport, SerializedLogEntry, TransportError, TransportResult};

#[derive(Debug)]
pub(crate) struct RestTransport {
    client: Arc<Client>,
    log_endpoint: Url,
}

impl RestTransport {
    /// Construct a new [`RestTransport`]
    pub(crate) fn new(client: Arc<Client>, log_endpoint: Url) -> Self {
        Self {
            client,
            log_endpoint,
        }
    }
}

impl AuditTransport for RestTransport {
    async fn send_logs(&self, entries: &[SerializedLogEntry]) -> TransportResult<()> {
        if entries.is_empty() {
            return Ok(());
        }

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
        let transport = RestTransport::new(create_test_client(), endpoint);

        let entries = vec![
            json!({"level": "INFO", "message": "test1"})
                .to_string()
                .into_boxed_str(),
            json!({"level": "DEBUG", "message": "test2"})
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
    async fn test_send_logs_empty() {
        let server = Server::new_async().await;
        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), endpoint);

        transport
            .send_logs(&[])
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
        let transport = RestTransport::new(create_test_client(), endpoint);

        let entries = vec![
            json!({"level": "INFO", "message": "test"})
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
    async fn test_send_logs_client_error_400() {
        let mut server = Server::new_async().await;
        let mock = server
            .mock("POST", "/audit/log/bulk")
            .with_status(400)
            .with_body("Bad Request")
            .create();

        let endpoint: Url = format!("{}/audit/log/bulk", server.url()).parse().unwrap();
        let transport = RestTransport::new(create_test_client(), endpoint);

        let entries = vec![
            json!({"level": "INFO", "message": "test"})
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
        let transport = RestTransport::new(create_test_client(), endpoint);

        let entries = vec![
            json!({"level": "INFO", "message": "test"})
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
        let transport = RestTransport::new(create_test_client(), endpoint);

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
        let transport = RestTransport::new(create_test_client(), endpoint);

        // Send 1000 entries
        let entries: Vec<_> = (0..1000)
            .map(|i| {
                json!({"level": "INFO", "message": format!("test{}", i), "index": i})
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
