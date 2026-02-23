// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The [`LogWorker`] runs in the background and is responsible for collecting and sending
//! logs to the lock server's `/audit/log` endpoint.

use super::log_entry::LockLogEntry;
use crate::LogWriter;
use crate::lock::transport::{AuditTransport, SerializedLogEntry};
use crate::log::LoggerWeak;

use super::WORKER_HTTP_RETRY_DUR;
use futures::StreamExt;
use futures::channel::mpsc;
use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tokio_util::sync::CancellationToken;

/// Responsible for sending logs to the lock server
pub(super) struct LogWorker<T: AuditTransport> {
    // it would be nice to store a struct here but we can't really store
    // `VecDeque<dyn Loggable>` so we just serialize the logs before storing them in the
    // buffer.
    //
    // TODO: should we cap the capacity? what to do with the excess logs if the buffer
    // becomes full?
    log_buffer: VecDeque<SerializedLogEntry>,
    log_interval: Duration,
    transport: Arc<T>,
    logger: Option<LoggerWeak>,
}

impl<T> LogWorker<T>
where
    T: AuditTransport,
{
    pub(super) fn new(
        log_interval: Duration,
        transport: Arc<T>,
        logger: Option<LoggerWeak>,
    ) -> Self {
        Self {
            log_interval,
            log_buffer: VecDeque::new(),
            transport,
            logger,
        }
    }

    pub(super) async fn run(
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
                () = sleep(self.log_interval) => {
                    self.flush_logs(&cancel_tkn).await;
                },

                () = cancel_tkn.cancelled() => {
                    let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);
                    self.flush_logs(&cancel_tkn).await;
                    logger.log_any(LockLogEntry::info(
                        "gracefully shutting down lock log worker",
                    ));
                    break;
                }
            }
        }
    }

    async fn flush_logs(&mut self, cancel_tkn: &CancellationToken) {
        // save the length at the time the function is called
        let batch_size = self.log_buffer.len();
        if batch_size == 0 {
            return;
        }

        // TODO: logs can be lost if the process crashes duiring retry
        let entries: Vec<SerializedLogEntry> = self.log_buffer.drain(0..batch_size).collect();

        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);
        loop {
            match self.transport.send_logs(&entries).await {
                Ok(()) => {
                    logger.log_any(LockLogEntry::info(format!(
                        "sent {batch_size} log entries to lock server",
                    )));
                    break;
                },
                Err(err) => {
                    logger.log_any(LockLogEntry::error(format!(
                        "failed to send logs to lock server: {err}"
                    )));
                    tokio::select! {
                        () = sleep(WORKER_HTTP_RETRY_DUR) => {},
                        () = cancel_tkn.cancelled() => {
                            logger.log_any(LockLogEntry::warn(
                                "cancellation requested during retry; dropping batch"
                            ));
                            break;
                        }
                    }
                },
            }
        }
    }
}

impl<T: AuditTransport> Drop for LogWorker<T> {
    fn drop(&mut self) {
        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);

        if !self.log_buffer.is_empty() {
            logger.log_any(LockLogEntry::warn(
                "log worker still has some log entries that were not sent to the lock server. did you forget to call Cedarling.shut_down()?",
            ));
        }
    }
}
