// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The [`LogWorker`] runs in the background and is responsible for collecting and sending
//! logs to the lock server's `/audit/log` endpoint.

use crate::LogWriter;
use crate::log::Logger;
use crate::log::lock_logger::log_entry::LockLogEntry;

use super::WORKER_HTTP_RETRY_DUR;
use futures::StreamExt;
use futures::channel::mpsc;
use reqwest::Client;
use serde_json::Value;
use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
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
    fallback_logger: Option<Logger>,
}

impl LogWorker {
    pub fn new(
        log_interval: Duration,
        http_client: Arc<Client>,
        log_endpoint: Url,
        fallback_logger: Option<Logger>,
    ) -> Self {
        Self {
            log_interval,
            log_buffer: VecDeque::new(),
            http_client,
            log_endpoint,
            fallback_logger,
        }
    }

    pub async fn run(&mut self, mut log_rx: mpsc::Receiver<SerializedLogEntry>) {
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
                _ = sleep(self.log_interval) => self.post_logs().await,
            }
        }
    }

    async fn post_logs(&mut self) {
        // save the length at the time the function is called
        let batch_size = self.log_buffer.len();

        if batch_size == 0 {
            return;
        }

        let mut failed_serializations = 0;
        let logs = self
            .log_buffer
            .iter()
            .map(|entry| {
                serde_json::from_str::<Value>(entry)
                    .map_err(|_| failed_serializations += 1)
                    .ok()
            })
            .collect::<Value>();

        // log errors to stdout since there's nowhere else to log
        if failed_serializations > 1 {
            if let Some(fallback_logger) = self.fallback_logger.as_ref() {
                fallback_logger.log_any(LockLogEntry::error_fmt(format!(
                    // This probably wouldn't happen as we define the log entries
                    // internally and they should always be serializable
                    "skipping {} log entries that couldn't be serialized",
                    failed_serializations
                )));
            }
        }

        loop {
            let resp = self
                .http_client
                .post(self.log_endpoint.as_ref())
                .body(logs.to_string())
                .send()
                .await
                .and_then(|resp| resp.error_for_status());

            match resp {
                Ok(_) => {
                    break;
                },
                Err(err) => {
                    if let Some(fallback_logger) = self.fallback_logger.as_ref() {
                        fallback_logger.log_any(LockLogEntry::error_fmt(format!(
                            "failed to POST logs to '{}': {}",
                            self.log_endpoint.as_ref(),
                            err
                        )));
                        sleep(WORKER_HTTP_RETRY_DUR).await;
                    }
                },
            }
        }

        // drain the POSTed logs from the buffer
        self.log_buffer.drain(0..batch_size);
    }
}
