// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

use super::AuditMsg;
use super::log_store::LogStore;
use crate::log::lock_logger::{LockLogger, LockLoggerError};
use serde_json::Value;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

impl LockLogger {
    pub async fn handle_log(
        http_client_tx: mpsc::Sender<AuditMsg>,
        period: Duration,
        mut log_rx: mpsc::Receiver<Value>,
        cancellation_tkn: CancellationToken,
    ) -> Result<(), LockLoggerError> {
        println!("log handle started");
        let mut log_store = LogStore::new();
        let mut log_interval = tokio::time::interval(period);

        loop {
            tokio::select! {
                _ = cancellation_tkn.cancelled() => break,
                _ = log_interval.tick() => {
                    let batch = log_store.batch();
                    if batch.is_empty() {
                        println!("batch is empty");
                        continue;
                    } else {
                        println!("sending batch");
                    }
                    let logs_json = batch.json();
                    if http_client_tx.send(AuditMsg::Log(logs_json)).await.is_err() {
                        log_store.clear_batch();
                        break;
                    }
                    log_store.flush_batch();
                },
                Some(log) = log_rx.recv() => {
                    log_store.store(log);
                }
            }
        }

        Ok(())
    }
}
