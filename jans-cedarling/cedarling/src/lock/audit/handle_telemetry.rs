// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;

use super::{AuditMsg, LockService};
use crate::lock::LockError;
use tokio::sync::mpsc;

impl LockService {
    pub async fn handle_telemetry(
        _client_tx: mpsc::Sender<AuditMsg>,
        _interval: Duration,
    ) -> Result<(), LockError> {
        todo!("implement sending telemetry to the lock server")

        // sample implementation
        //
        // loop {
        //     tokio::time::sleep(interval).await;
        //     let msg = AuditMsgContent {
        //         client_id: client_id.clone(),
        //         message: todo!("get telemetry"),
        //         result_code: todo!("figure out codes here"),
        //     };
        //
        //     if client_tx.send(AuditMsg::Telemetry(msg)).await.is_err() {
        //         break;
        //     }
        // }
        //
        // return Ok(());
    }
}
