// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

use super::{AuditMsg, LockService, Logger};
use crate::lock::LockError;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

impl LockService {
    pub async fn handle_log(
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
                    let logs = logger.batch().json();
                    if client_tx.send(AuditMsg::Log(logs)).await.is_err() {
                        break;
                    }
                    logger.flush_batch();
                }
            }
        }

        Ok(())
    }
}
