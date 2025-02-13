// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod handle_log;
mod log_store;

use crate::LockLogConfig;
use crate::init::service_config::LockClientConfig;
use crate::log::fallback;
use reqwest::Client;
use serde::Serialize;
use serde_json::Value;
use std::time::Duration;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use tokio_util::task::TaskTracker;

use super::{LockLogger, LockLoggerError};

#[derive(Serialize)]
#[allow(dead_code)]
pub enum AuditMsg {
    Log(Value),
    Health(Value),
    Telemetry(Value),
}

impl AuditMsg {
    const LOG_ENDPOINT: &str = "/log";
    const HEALTH_ENDPOINT: &str = "/health";
    const TELEMETRY_ENDPOINT: &str = "/telemetry";

    pub fn endpoint(&self) -> &str {
        match self {
            AuditMsg::Log(_) => Self::LOG_ENDPOINT,
            AuditMsg::Health(_) => Self::HEALTH_ENDPOINT,
            AuditMsg::Telemetry(_) => Self::TELEMETRY_ENDPOINT,
        }
    }
}

impl LockLogger {
    pub fn init_audit(
        lock_config: LockLogConfig,
        client_config: LockClientConfig,
        cancellation_tkn: CancellationToken,
    ) -> (TaskTracker, mpsc::Sender<Value>) {
        let tracker = TaskTracker::new();
        let (http_client_tx, http_client_rx) = mpsc::channel::<AuditMsg>(100);
        let (log_tx, log_rx) = mpsc::channel::<Value>(100);

        // Spawn task for handling logs
        if lock_config.log_interval > 0 {
            tracker.spawn(Self::handle_log(
                http_client_tx.clone(),
                Duration::from_secs(lock_config.log_interval),
                log_rx,
                cancellation_tkn.clone(),
            ));
        }

        // Task for handling sending http requests
        tracker.spawn(Self::handle_http_requests(
            client_config,
            http_client_rx,
            cancellation_tkn,
        ));

        tracker.close();

        (tracker, log_tx)
    }

    /// `POST`s messages send to the `/audit` endpoints
    async fn handle_http_requests(
        client_creds: LockClientConfig,
        mut audit_rx: mpsc::Receiver<AuditMsg>,
        cancellation_tkn: CancellationToken,
    ) -> Result<(), LockLoggerError> {
        let client = Client::new();

        while let Some(msg) = audit_rx.recv().await {
            let audit_endpoint = msg.endpoint();
            let uri = client_creds.audit_uri.clone() + audit_endpoint;
            let msg_json = serde_json::ser::to_string(&msg)?;

            let result = client
                .post(&uri)
                .bearer_auth(&client_creds.access_token)
                .body(msg_json)
                .send()
                .await;

            if let Err(err) = &result {
                fallback::log(&format!("failed to send http request to \"{uri}\": {err}"));
            }

            if let Err(err) = result.unwrap().error_for_status() {
                fallback::log(&format!("failed to send http request to \"{uri}\": {err}"));
            }
        }

        loop {
            tokio::select! {
                Some(msg) = audit_rx.recv() => {
                    let msg_json = serde_json::ser::to_string(&msg)?;
                    client
                        .post(&client_creds.audit_uri)
                        .bearer_auth(&client_creds.access_token)
                        .body(msg_json)
                        .send()
                        .await
                        .map_err(LockLoggerError::HttpRequestFailed)?
                        .error_for_status()
                        .map_err(LockLoggerError::HttpErrResponse)?;
                }
                _ = cancellation_tkn.cancelled() => break,
                else => break,
            }
        }

        Ok(())
    }
}
