// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

use super::{AuditMsg, LockService, Logger};
use crate::LogStorage;
use crate::lock::LockError;
use serde_json::json;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

impl LockService {
    pub async fn handle_log(
        client_id: String,
        client_tx: mpsc::Sender<AuditMsg>,
        interval: Duration,
        logger: Logger,
        cancellation_tkn: CancellationToken,
    ) -> Result<(), LockError> {
        loop {
            tokio::select! {
                _ = cancellation_tkn.cancelled() => break,
                else => {
                    tokio::time::sleep(interval).await;
                    // TODO: we need to implement a way to just get the logs
                    // without removing them from the storage since they will
                    // just be lost if the POST fails
                    let logs = logger.pop_logs();
                    for mut log in logs.into_iter() {
                        // we add in the client_id here since the usual logs
                        // will not have this if they do not use the lock
                        // server
                        log["client_id"] = json!(client_id);
                        if client_tx.send(AuditMsg::Log(log)).await.is_err() {
                            break;
                        }
                    }
                }
            }
        }

        return Ok(());
    }
}
