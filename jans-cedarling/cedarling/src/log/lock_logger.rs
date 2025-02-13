// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod audit;

use super::LogLevel;
use super::interface::Loggable;
use crate::init::service_config::LockClientConfig;
use crate::log::fallback;
use crate::{LockLogConfig, LogWriter};
use serde_json::Value;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use tokio_util::task::TaskTracker;

pub(crate) struct LockLogger {
    log_level: LogLevel,
    audit_tasks_tracker: TaskTracker,
    cancellation_tkn: CancellationToken,
    log_tx: mpsc::Sender<Value>,
}

impl LockLogger {
    pub fn new(
        log_level: LogLevel,
        lock_config: LockLogConfig,
        client_config: LockClientConfig,
    ) -> Self {
        let cancellation_tkn = CancellationToken::new();

        let (audit_tasks_tracker, log_tx) =
            Self::init_audit(lock_config, client_config, cancellation_tkn.child_token());

        Self {
            log_level,
            audit_tasks_tracker,
            cancellation_tkn,
            log_tx,
        }
    }

    /// Closes connections to the lock server
    pub async fn close(&self) {
        self.cancellation_tkn.cancel();
        self.audit_tasks_tracker.wait().await;
    }
}

impl LogWriter for LockLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            return;
        }

        let entry = match serde_json::to_value(&entry) {
            Ok(entry) => entry,
            Err(e) => {
                fallback::log(&format!("failed to serialize log entry: {e:?}"));
                return;
            },
        };

        let tx = self.log_tx.clone();
        tokio::spawn(async move {
            if let Err(err) = tx.send(entry).await {
                fallback::log(&format!("failed to send logs to the lock server: {err}"));
            }
        });
    }
}

#[derive(Debug, thiserror::Error)]
pub enum LockLoggerError {
    #[error("failed to send HTTP request to lock server: {0}")]
    HttpRequestFailed(#[source] reqwest::Error),
    #[error("received an HTTP error response from the lock server: {0}")]
    HttpErrResponse(#[source] reqwest::Error),
    #[error("failed to serialize audit message to a JSON string: {0}")]
    SerializeAuditMsg(#[from] serde_json::Error),
}

#[cfg(test)]
mod test {
    use std::time::Duration;

    use super::LockLogger;
    use crate::app_types::PdpID;
    use crate::init::service_config::LockClientConfig;
    use crate::log::LogEntry;
    use crate::{LockLogConfig, LogLevel, LogWriter};
    use mockito::Server;
    use tokio::test;

    #[test]
    async fn can_push_logs_to_the_lock_server() {
        let mut server = Server::new_async().await;
        let audit_endpoint = "/audit";
        let sse_endpoint = "/sse";
        let audit_uri = format!("{}{}", server.url(), audit_endpoint);
        let sse_uri = format!("{}{}", server.url(), sse_endpoint);

        let audit_endpoint = server
            .mock("POST", format!("{audit_endpoint}/log").as_str())
            .with_status(200)
            .match_request(|req| {
                let header = req.header("Authorization");
                let header_val = header[0].to_str().expect("header value should be a string");
                header.len() == 1 && header_val == "Bearer some.access.tkn"
            })
            .expect(1)
            .create();

        let logger = LockLogger::new(
            LogLevel::TRACE,
            LockLogConfig {
                log_interval: 1,
                health_interval: 0,
                telemetry_interval: 0,
            },
            LockClientConfig {
                client_id: "someclientid".to_string(),
                access_token: "some.access.tkn".to_string(),
                audit_uri,
                sse_uri,
            },
        );

        logger.log_any(LogEntry::new_with_data(
            PdpID::new(),
            None,
            crate::log::LogType::Decision,
            None,
        ));

        // sleep for 1.1 secs so the log can send
        tokio::time::sleep(Duration::new(1, 1000000000)).await;

        audit_endpoint.assert_async().await;
    }
}
