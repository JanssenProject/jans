// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The [`LogWorker`] runs in the background and is responsible for collecting and sending
//! logs to the lock server's `/audit/log` endpoint.

use super::log_entry::LockLogEntry;
use crate::LogWriter;
use crate::async_sleep::Ticker;
use crate::lock::transport::{AuditItem, AuditKind, AuditTransport, SendFailure};
use crate::log::LoggerWeak;

use crate::common::retry::{
    Attempt, Backoff, Cancelled, RetryError, RetryPolicy, retry, retry_until_cancelled,
};
use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

/// Responsible for sending logs to the lock server
pub(super) struct AuditWorker<T: AuditTransport> {
    buffer: VecDeque<AuditItem>,
    audit_interval: Duration,
    transport: Arc<T>,
    kind: AuditKind,
    logger: Option<LoggerWeak>,
    max_retries: u32,
    /// Delay before the first retry, doubling after each.
    retry_delay: Duration,
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
        retry_delay: Duration,
    ) -> Self {
        Self {
            audit_interval,
            buffer: VecDeque::new(),
            transport,
            kind,
            logger,
            max_retries,
            retry_delay,
        }
    }

    pub(super) async fn run(
        &mut self,
        mut rx: mpsc::Receiver<AuditItem>,
        cancel_tkn: CancellationToken,
    ) {
        let mut flush_tick = Ticker::new(self.audit_interval);

        loop {
            tokio::select! {
                entry = rx.recv() => {
                    let Some(entry) = entry else {
                        // This branch can win the select race against
                        // `cancelled()` during Drop-based teardown, so flush
                        // here too instead of silently discarding the buffer.
                        self.flush(None).await;
                        break;
                    };
                    self.buffer.push_back(entry);
                    if matches!(self.kind, AuditKind::Telemetry(_)) {
                        self.flush(Some(&cancel_tkn)).await;
                    }
                },

                // Send logs to the server
                () = flush_tick.tick() => {
                    self.flush(Some(&cancel_tkn)).await;
                },

                () = cancel_tkn.cancelled() => {
                    while let Ok(entry) = rx.try_recv() {
                        self.buffer.push_back(entry);
                    }
                    self.flush(None).await;
                    self.logger
                        .as_ref()
                        .and_then(std::sync::Weak::upgrade)
                        .log_any(LockLogEntry::info(format!(
                            "shutting down {} audit worker ({})",
                            self.kind, self.kind.url()
                        )));
                    break;
                }
            }
        }
    }

    async fn flush(&mut self, cancel_tkn: Option<&CancellationToken>) {
        // save the length at the time the function is called
        let batch_size = self.buffer.len();
        if batch_size == 0 {
            return;
        }

        let entries: Vec<AuditItem> = self.buffer.drain(0..batch_size).collect();

        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);
        // Mapped once: entries that do not map would fail the same way on every attempt.
        let batch = match self.transport.prepare(&entries, &self.kind) {
            Ok(batch) => batch,
            Err(err) => {
                logger.log_any(LockLogEntry::error(format!(
                    "dropping {batch_size} {} entries: {err}",
                    self.kind,
                )));
                return;
            },
        };
        let policy = RetryPolicy {
            backoff: Backoff::exponential(self.retry_delay),
            max_retries: self.max_retries,
        };
        let attempt = || async {
            match self.transport.send(&batch).await {
                Ok(()) => Attempt::Done(()),
                Err(SendFailure::Final(err)) => Attempt::Final(err),
                Err(SendFailure::Retryable(err)) => {
                    logger.log_any(LockLogEntry::error(format!(
                        "failed to send to {} audit endpoint: {err}",
                        self.kind,
                    )));
                    Attempt::Retryable {
                        error: err,
                        wait_at_least: None,
                    }
                },
            }
        };
        let result = match cancel_tkn {
            Some(cancel) => retry_until_cancelled(policy, cancel, attempt).await,
            None => retry(policy, attempt)
                .await
                .map_err(RetryError::cancellable),
        };

        match result {
            Ok(()) => logger.log_any(LockLogEntry::info(format!(
                "sent {batch_size} {} entries to lock server",
                self.kind,
            ))),
            // Repeating would get the same answer, and hold the channel meanwhile.
            Err(RetryError::Final(err)) => logger.log_any(LockLogEntry::error(format!(
                "dropping {batch_size} {} entries: {err}",
                self.kind
            ))),
            Err(RetryError::RetriesExhausted { .. }) => {
                logger.log_any(LockLogEntry::warn(format!(
                    "retries exhausted; dropping {batch_size} {} entries",
                    self.kind,
                )));
            },
            Err(RetryError::Cancelled(Cancelled)) => {
                logger.log_any(LockLogEntry::warn(format!(
                    "cancellation requested during retry; dropping {batch_size} {} entries",
                    self.kind,
                )));
            },
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

#[cfg(test)]
mod tests {
    use mockito::Server;

    use super::*;
    use crate::common::app_types::PdpID;
    use crate::http::{HttpClient, HttpClientConfig};
    use crate::lock::transport::rest::RestTransport;
    use crate::lock::transport::test_utils::{malformed_log_item, sample_log_item};
    use crate::log::{LogLevel, LogStrategy, Logger};
    use crate::{LogConfig, LogStorage, LogTypeConfig, MemoryLogConfig};

    fn memory_log() -> Logger {
        Arc::new(LogStrategy::new(
            &LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_item_size: None,
                    max_items: None,
                }),
                log_level: LogLevel::DEBUG,
            },
            PdpID::new(),
            None,
        ))
    }

    fn messages(logs: &[serde_json::Value], level: &str) -> Vec<String> {
        logs.iter()
            .filter(|entry| entry["level"] == level)
            .filter_map(|entry| entry["message"].as_str().map(str::to_string))
            .collect()
    }

    #[tokio::test]
    async fn unmappable_batch_makes_no_request_and_warns_once() {
        let mut server = Server::new_async().await;
        let bulk = server
            .mock("POST", "/audit/log/bulk")
            .expect(0)
            .create_async()
            .await;
        let url = format!("{}/audit/log/bulk", server.url())
            .parse()
            .expect("valid URL");
        let log = memory_log();
        let http = HttpClient::new(HttpClientConfig::default()).expect("HTTP client");
        let transport = Arc::new(RestTransport::new(http, Some(Arc::downgrade(&log))));
        let mut worker = AuditWorker::new(
            Duration::from_secs(60),
            transport,
            AuditKind::Log(url),
            Some(Arc::downgrade(&log)),
            3,
            Duration::from_millis(1),
        );
        worker.buffer.push_back(malformed_log_item());
        worker.buffer.push_back(malformed_log_item());

        worker.flush(None).await;

        bulk.assert_async().await;
        let logs = log.pop_logs();
        let warnings = messages(&logs, "WARN");
        assert_eq!(
            warnings.len(),
            2,
            "one WARN per bad entry, not one per attempt: {warnings:?}"
        );
        let errors = messages(&logs, "ERROR");
        assert!(
            errors.len() == 1 && errors[0].contains("dropping 2 log entries"),
            "the batch must be dropped with one ERROR, got {errors:?}"
        );
        assert!(
            worker.buffer.is_empty(),
            "the dropped batch leaves the buffer"
        );
    }

    const MAX_RETRIES: u32 = 2;

    /// A REST log worker for `url` that retries after 1 ms, logging into `log`.
    fn rest_worker(url: &str, log: &Logger) -> AuditWorker<RestTransport> {
        let http = HttpClient::new(HttpClientConfig::default()).expect("HTTP client");
        AuditWorker::new(
            Duration::from_secs(60),
            Arc::new(RestTransport::new(http, Some(Arc::downgrade(log)))),
            AuditKind::Log(url.parse().expect("valid URL")),
            Some(Arc::downgrade(log)),
            MAX_RETRIES,
            Duration::from_millis(1),
        )
    }

    #[tokio::test]
    async fn refused_batch_is_dropped_after_one_request() {
        for status in [400, 403, 404, 413] {
            let mut server = Server::new_async().await;
            let bulk = server
                .mock("POST", "/audit/log/bulk")
                .with_status(status)
                .expect(1)
                .create_async()
                .await;
            let log = memory_log();
            let mut worker = rest_worker(&format!("{}/audit/log/bulk", server.url()), &log);
            worker.buffer.push_back(sample_log_item());

            worker.flush(None).await;

            bulk.assert_async().await;
            let logs = log.pop_logs();
            let errors = messages(&logs, "ERROR");
            assert!(
                errors.len() == 1
                    && errors[0].contains("dropping 1 log entries")
                    && errors[0].contains(&format!("HTTP {status}")),
                "HTTP {status} must drop the batch with one ERROR, got {errors:?}"
            );
            assert!(
                messages(&logs, "WARN").is_empty(),
                "a refused batch is not retried until exhausted"
            );
        }
    }

    #[tokio::test]
    async fn server_error_is_retried_until_exhausted() {
        let mut server = Server::new_async().await;
        let bulk = server
            .mock("POST", "/audit/log/bulk")
            .with_status(503)
            .expect(usize::try_from(MAX_RETRIES).expect("small") + 1)
            .create_async()
            .await;
        let log = memory_log();
        let mut worker = rest_worker(&format!("{}/audit/log/bulk", server.url()), &log);
        worker.buffer.push_back(sample_log_item());

        worker.flush(None).await;

        bulk.assert_async().await;
        assert_exhausted(&log.pop_logs());
    }

    /// Lock refuses a client for a few seconds after registering it.
    #[tokio::test]
    async fn unauthorized_batch_is_sent_again() {
        let mut server = Server::new_async().await;
        let refused = server
            .mock("POST", "/audit/log/bulk")
            .with_status(401)
            .expect(1)
            .create_async()
            .await;
        let accepted = server
            .mock("POST", "/audit/log/bulk")
            .with_status(200)
            .expect(1)
            .create_async()
            .await;
        let log = memory_log();
        let mut worker = rest_worker(&format!("{}/audit/log/bulk", server.url()), &log);
        worker.buffer.push_back(sample_log_item());

        worker.flush(None).await;

        refused.assert_async().await;
        accepted.assert_async().await;
        let logs = log.pop_logs();
        assert!(
            messages(&logs, "ERROR")
                .iter()
                .all(|error| !error.contains("dropping")),
            "a 401 must not drop the batch: {:?}",
            messages(&logs, "ERROR")
        );
    }

    #[tokio::test]
    async fn connection_failure_is_retried_until_exhausted() {
        let closed = crate::http::test_support::ClosedPort::new();
        let log = memory_log();
        let mut worker = rest_worker(&closed.url("/audit/log/bulk"), &log);
        worker.buffer.push_back(sample_log_item());

        worker.flush(None).await;

        let logs = log.pop_logs();
        assert_exhausted(&logs);
        assert!(
            messages(&logs, "ERROR")
                .iter()
                .all(|error| error.contains("error sending request")),
            "every attempt must fail on the connection, not on an answer: {:?}",
            messages(&logs, "ERROR")
        );
    }

    /// One ERROR per attempt, then the batch is dropped with a WARN.
    fn assert_exhausted(logs: &[serde_json::Value]) {
        let attempts = messages(logs, "ERROR");
        assert_eq!(
            attempts.len(),
            usize::try_from(MAX_RETRIES).expect("small") + 1,
            "the first attempt and {MAX_RETRIES} retries: {attempts:?}"
        );
        let warnings = messages(logs, "WARN");
        assert!(
            warnings.len() == 1 && warnings[0].contains("retries exhausted"),
            "the batch is dropped once retries are exhausted, got {warnings:?}"
        );
    }
}
