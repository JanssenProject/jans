// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The [`LogWorker`] runs in the background and is responsible for collecting and sending
//! logs to the lock server's `/audit/log` endpoint.

use super::log_entry::LockLogEntry;
use crate::LogWriter;
use crate::lock::transport::{AuditKind, AuditTransport, SerializedAuditEntry};
use crate::log::LoggerWeak;

use super::WORKER_HTTP_RETRY_DUR;
use futures::StreamExt;
use futures::channel::mpsc;
use http_utils::Backoff;
use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tokio_util::sync::CancellationToken;

/// Responsible for sending logs to the lock server
pub(super) struct AuditWorker<T: AuditTransport> {
    buffer: VecDeque<SerializedAuditEntry>,
    audit_interval: Duration,
    transport: Arc<T>,
    kind: AuditKind,
    logger: Option<LoggerWeak>,
    max_retries: u32,
}

impl<T> AuditWorker<T>
where
    T: AuditTransport + 'static,
{
    pub(super) fn new(
        audit_interval: Duration,
        transport: Arc<T>,
        kind: AuditKind,
        logger: Option<LoggerWeak>,
        max_retries: u32,
    ) -> Self {
        Self {
            audit_interval,
            buffer: VecDeque::new(),
            transport,
            kind,
            logger,
            max_retries,
        }
    }

    pub(super) async fn run(
        &mut self,
        mut rx: mpsc::Receiver<SerializedAuditEntry>,
        cancel_tkn: CancellationToken,
    ) {
        loop {
            tokio::select! {
                entry = rx.next() => {
                    let Some(entry) = entry else { break; };
                    self.buffer.push_back(entry);
                },

                // Send logs to the server
                () = sleep(self.audit_interval) => {
                    self.flush(&cancel_tkn).await;
                },

                () = cancel_tkn.cancelled() => {
                    self.flush(&cancel_tkn).await;
                    self.logger
                        .as_ref()
                        .and_then(std::sync::Weak::upgrade)
                        .log_any(LockLogEntry::info(format!(
                            "audit worker ({}) shut down cleanly, flushed {} entries",
                            self.kind.url(),
                            self.buffer.len(), // will be 0 if flush succeeded
                        )));
                    break;
                }
            }
        }
    }

    async fn flush(&mut self, cancel_tkn: &CancellationToken) {
        // save the length at the time the function is called
        let batch_size = self.buffer.len();
        if batch_size == 0 {
            return;
        }

        let entries: Vec<SerializedAuditEntry> = self.buffer.drain(..).collect();

        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);
        let mut backoff = Backoff::new_exponential(WORKER_HTTP_RETRY_DUR, Some(self.max_retries));

        loop {
            match self.transport.send(&entries, &self.kind).await {
                Ok(()) => {
                    logger.log_any(LockLogEntry::info(format!(
                        "sent {batch_size} entries to {} audit endpoint",
                        self.kind.url(),
                    )));
                    return;
                },
                Err(err) => {
                    logger.log_any(LockLogEntry::error(format!(
                        "failed to send to {} audit endpoint: {err}",
                        self.kind.url(),
                    )));

                    tokio::select! {
                        _ = backoff.snooze() => {},
                        () = cancel_tkn.cancelled() => {
                            logger.log_any(LockLogEntry::warn(format!(
                                "cancellation requested during retry; dropping {batch_size} entries"
                            )));
                            break;
                        },
                    }
                },
            }
        }
    }
}

impl<T: AuditTransport> Drop for AuditWorker<T> {
    fn drop(&mut self) {
        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);

        if !self.buffer.is_empty() {
            logger.log_any(LockLogEntry::warn(format!(
                "{} entries dropped on worker teardown — did you call shut_down()?",
                self.buffer.len(),
            )));
        }
    }
}
