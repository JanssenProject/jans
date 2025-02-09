// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod handle_health;
mod handle_log;
mod handle_telemetry;
mod log_entry;

use super::LockError;
use super::LockService;
use crate::init::service_config::LockConfig;
use crate::log::Logger;
use crate::log::interface::LogWriter;
use log_entry::AuditLogEntry;
use reqwest::Client;
use serde::Serialize;
use serde_json::Value;
use std::time::Duration;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use tokio_util::task::TaskTracker;

pub struct AuditIntervals {
    pub log: u64,
    pub health: u64,
    pub telemetry: u64,
}

#[derive(Serialize)]
pub enum AuditMsg {
    Log(Value),
    Health(AuditMsgContent),
    Telemetry(AuditMsgContent),
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

#[derive(Serialize)]
pub struct AuditMsgContent {
    client_id: String,
    message: String,
    result_code: i32,
}

impl LockService {
    pub fn init_audit(
        client_creds: LockConfig,
        audit_intervals: AuditIntervals,
        logger: Logger,
        cancellation_tkn: CancellationToken,
    ) -> TaskTracker {
        let tracker = TaskTracker::new();
        let (client_tx, client_rx) = mpsc::channel::<AuditMsg>(100);

        // Task for handling logs
        tracker.spawn(Self::handle_log(
            client_creds.client_id.clone(),
            client_tx.clone(),
            Duration::from_secs(audit_intervals.log),
            logger.clone(),
            cancellation_tkn.clone(),
        ));

        // Task for handling health checks
        tracker.spawn(Self::handle_health(
            client_creds.client_id.clone(),
            client_tx.clone(),
            Duration::from_secs(audit_intervals.health),
        ));

        // Task for handling telemetry
        tracker.spawn(Self::handle_telemetry(
            client_creds.client_id.clone(),
            client_tx.clone(),
            Duration::from_secs(audit_intervals.telemetry),
        ));

        // Task for handling sending http requests
        tracker.spawn(Self::handle_http_requests(
            client_creds,
            client_rx,
            cancellation_tkn,
            logger,
        ));

        tracker.close();

        tracker
    }

    /// `POST`s messages send to the `/audit` endpoints
    async fn handle_http_requests(
        client_creds: LockConfig,
        mut audit_rx: mpsc::Receiver<AuditMsg>,
        cancellation_tkn: CancellationToken,
        logger: Logger,
    ) -> Result<(), LockError> {
        let client = Client::new();

        while let Some(msg) = audit_rx.recv().await {
            let audit_endpoint = msg.endpoint();
            let uri = client_creds.audit_uri.clone() + audit_endpoint;
            let msg_json = serde_json::ser::to_string(&msg)?;

            logger.log_any(AuditLogEntry::new_http_debug(&msg));

            let result = client
                .post(uri)
                .bearer_auth(&client_creds.access_token)
                .body(msg_json)
                .send()
                .await;

            if let Err(err) = &result {
                logger.log_any(AuditLogEntry::new_http_err(&msg, &err.to_string()));
            }

            if let Err(err) = result.unwrap().error_for_status() {
                logger.log_any(AuditLogEntry::new_http_err(&msg, &err.to_string()));
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
                        .map_err(LockError::HttpRequestFailed)?
                        .error_for_status()
                        .map_err(LockError::HttpErrResponse)?;
                }
                _ = cancellation_tkn.cancelled() => break,
                else => break,
            }
        }

        Ok(())
    }
}
