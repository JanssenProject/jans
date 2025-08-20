// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The [`LogWorker`] runs in the background and is responsible for collecting and sending
//! logs to the lock server's `/audit/log` endpoint.

use super::log_entry::LockLogEntry;
use crate::log::{LogStrategy, LoggerWeak};
use crate::LogWriter;

use super::WORKER_HTTP_RETRY_DUR;
use futures::StreamExt;
use futures::channel::mpsc;
use reqwest::Client;
use serde_json::Value;
use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tokio_util::sync::CancellationToken;
use url::Url;

pub type SerializedLogEntry = Box<str>;

/// Responsible for sending logs to the lock server
pub struct LogWorker {
    // it would be nice to store a struct here but we can't really store
    // `VecDeque<dyn Loggable>` so we just serialize the logs before storing them in the
    // buffer. We use `Box<str>`s to save some memory.
    //
    // TODO: should we cap the capacity? what to do with the excess logs if the buffer
    // becomes full?
    log_buffer: VecDeque<Box<str>>,
    log_interval: Duration,
    http_client: Arc<Client>,
    log_endpoint: Url,
    logger: Option<LoggerWeak>,
}

impl LogWorker {
    pub fn new(
        log_interval: Duration,
        http_client: Arc<Client>,
        log_endpoint: Url,
        logger: Option<LoggerWeak>,
    ) -> Self {
        Self {
            log_interval,
            log_buffer: VecDeque::new(),
            http_client,
            log_endpoint,
            logger,
        }
    }

    pub async fn run(
        &mut self,
        mut log_rx: mpsc::Receiver<SerializedLogEntry>,
        cancel_tkn: CancellationToken,
    ) {
        loop {
            tokio::select! {
                // Append log to the buffer
                log_entry = log_rx.next() => {
                    let Some(log_entry) = log_entry else {
                        break;
                    };
                    self.log_buffer.push_back(log_entry);
                },

                // Send logs to the server
                _ = sleep(self.log_interval) => {
                    let logger = self.logger.as_ref().and_then(|logger| logger.upgrade());
                    post_logs(&mut self.log_buffer, &logger, self.http_client.clone(), &self.log_endpoint).await;
                },

                _ = cancel_tkn.cancelled() => {
                    let logger = self.logger.as_ref().and_then(|logger| logger.upgrade());
                    post_logs(&mut self.log_buffer, &logger, self.http_client.clone(), &self.log_endpoint).await;
                    logger.log_any(LockLogEntry::info(
                        "gracefully shutting down lock log worker",
                    ));
                    break;
                }
            }
        }
    }
}

async fn post_logs(
    log_buf: &mut VecDeque<Box<str>>,
    logger: &Option<Arc<LogStrategy>>,
    http_client: Arc<Client>,
    log_endpoint: &Url,
) {
    // save the length at the time the function is called
    let batch_size = log_buf.len();

    if batch_size == 0 {
        return;
    }

    let mut failed_serializations = 0;
    let logs = log_buf
        .iter()
        .map(|entry| {
            serde_json::from_str::<Value>(entry)
                .map_err(|_| failed_serializations += 1)
                .ok()
        })
        .collect::<Value>();

    // log errors to stdout since there's nowhere else to log
    if failed_serializations > 1 {
        logger.log_any(LockLogEntry::error(format!(
            // This probably wouldn't happen as we define the log entries
            // internally and they should always be serializable
            "skipping {} log entries that couldn't be serialized",
            failed_serializations
        )));
    }

    loop {
        let resp = http_client
            .post(log_endpoint.as_ref())
            .body(logs.to_string())
            .send()
            .await
            .and_then(|resp| resp.error_for_status());

        match resp {
            Ok(_) => {
                logger.log_any(LockLogEntry::info(format!(
                    "sent logs to '{}'",
                    log_endpoint.as_ref(),
                )));
                break;
            },
            Err(err) => {
                logger.log_any(LockLogEntry::error(format!(
                    "failed to POST logs to '{}': {}",
                    log_endpoint.as_ref(),
                    err
                )));
                sleep(WORKER_HTTP_RETRY_DUR).await;
            },
        }
    }

    // drain the POSTed logs from the buffer
    log_buf.drain(0..batch_size);
}

impl Drop for LogWorker {
    fn drop(&mut self) {
        let logger = self.logger.as_ref().and_then(|logger| logger.upgrade());

        if !self.log_buffer.is_empty() {
            logger.log_any(LockLogEntry::warn(
                "log worker still has some log entries that were not sent to the lock server. did you forget to call Cedarling.shut_down()?",
            ));
        }
    }
}
