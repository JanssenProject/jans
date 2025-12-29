// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::io::Write;
use std::sync::mpsc;
use std::sync::{Arc, Mutex};
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};

use crate::log::LogLevel;
use crate::log::err_log_entry::ErrorLogEntry;
use crate::log::interface::{LogWriter, Loggable};

/// Mode for stdout logger in native builds.
/// Supports both immediate and asynchronous logging.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StdOutLoggerMode {
    /// Immediate synchronous logging (no buffering).
    Immediate,
    /// Asynchronous logging with configurable timeout and buffer.
    Async {
        /// Timeout in milliseconds to flush the log buffer.
        timeout_millis: u64,
        /// Maximum buffer size in bytes before an automatic flush.
        buffer_limit: usize,
    },
}

impl StdOutLoggerMode {
    /// 100 ms default timeout
    pub const DEFAULT_FLUSH_TIMEOUT_MILLIS: u64 = 100;
    /// 1 MB limit
    pub const DEFAULT_BUFFER_LIMIT: usize = 1 << 20;
}

enum LogMessage {
    Log(Box<dyn FnOnce() -> String + Send + 'static>),
    Shutdown,
}

const ERR_TO_WRITE_STD_LOGGER: &str = "failed to write logs to stdout";

fn spawn_writer_thread(
    receiver: mpsc::Receiver<LogMessage>,
    writer: Box<dyn Write + Send + Sync>,
    flush_timeout: Duration,
    buffer_limit: usize,
) -> JoinHandle<()> {
    thread::spawn(move || {
        let mut writer = writer;
        let mut buffer = String::new();
        let mut next_flush_deadline = Instant::now() + flush_timeout;
        loop {
            // Check if flush deadline has passed
            let now = Instant::now();
            if now >= next_flush_deadline && !buffer.is_empty() {
                writer
                    .write_all(buffer.as_bytes())
                    .expect(ERR_TO_WRITE_STD_LOGGER);
                buffer.clear();
                next_flush_deadline = now + flush_timeout;
            }

            // Compute remaining time until next flush deadline
            let remaining = next_flush_deadline.saturating_duration_since(now);
            match receiver.recv_timeout(remaining) {
                Ok(LogMessage::Log(serializer)) => {
                    let json_string = serializer();
                    buffer.push_str(&json_string);
                    buffer.push('\n');
                    if buffer.len() >= buffer_limit {
                        writer
                            .write_all(buffer.as_bytes())
                            .expect(ERR_TO_WRITE_STD_LOGGER);
                        buffer.clear();
                        // Reset flush deadline since we just flushed
                        next_flush_deadline = Instant::now() + flush_timeout;
                    }
                },
                Ok(LogMessage::Shutdown) => {
                    break;
                },
                Err(mpsc::RecvTimeoutError::Timeout) => {
                    // Timeout, flush buffer if not empty
                    if !buffer.is_empty() {
                        writer
                            .write_all(buffer.as_bytes())
                            .expect(ERR_TO_WRITE_STD_LOGGER);
                        buffer.clear();
                        next_flush_deadline = Instant::now() + flush_timeout;
                    }
                },
                Err(mpsc::RecvTimeoutError::Disconnected) => {
                    // Sender dropped, flush and exit
                    if !buffer.is_empty() {
                        writer
                            .write_all(buffer.as_bytes())
                            .expect(ERR_TO_WRITE_STD_LOGGER);
                        buffer.clear();
                    }
                    break;
                },
            }
        }
        // Final flush
        if !buffer.is_empty() {
            writer
                .write_all(buffer.as_bytes())
                .expect(ERR_TO_WRITE_STD_LOGGER);
        }
    })
}

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    mode: StdOutLoggerMode,
    sender: Option<mpsc::Sender<LogMessage>>,
    thread_handle: Option<JoinHandle<()>>,
    writer: Option<Arc<Mutex<Box<dyn Write + Send + Sync>>>>,
    log_level: LogLevel,
}

impl StdOutLogger {
    // is used as fallback in memory logger, so default is immediate mode
    pub(crate) fn new(log_level: LogLevel, mode: StdOutLoggerMode) -> Self {
        Self::new_inner(Box::new(std::io::stdout()), log_level, mode)
    }

    // Create a new StdOutLogger with custom writer.
    // is used in tests.
    #[cfg(test)]
    pub(crate) fn new_with(
        writer: Box<dyn Write + Send + Sync>,
        log_level: LogLevel,
        mode: StdOutLoggerMode,
    ) -> Self {
        Self::new_inner(writer, log_level, mode)
    }

    fn new_inner(
        writer: Box<dyn Write + Send + Sync>,
        log_level: LogLevel,
        mode: StdOutLoggerMode,
    ) -> Self {
        match mode {
            StdOutLoggerMode::Immediate => Self {
                mode,
                sender: None,
                thread_handle: None,
                writer: Some(Arc::new(Mutex::new(writer))),
                log_level,
            },
            StdOutLoggerMode::Async {
                timeout_millis: timeout,
                buffer_limit,
            } => {
                let (sender, receiver) = mpsc::channel();
                let thread_handle = Some(spawn_writer_thread(
                    receiver,
                    writer,
                    Duration::from_millis(timeout),
                    buffer_limit,
                ));
                Self {
                    mode,
                    sender: Some(sender),
                    thread_handle,
                    writer: None,
                    log_level,
                }
            },
        }
    }
}

impl Drop for StdOutLogger {
    fn drop(&mut self) {
        match self.mode {
            StdOutLoggerMode::Immediate => {
                // Nothing to do for immediate mode
            },
            StdOutLoggerMode::Async { .. } => {
                // Send shutdown signal
                if let Some(sender) = &self.sender {
                    let _ = sender.send(LogMessage::Shutdown);
                }
                // Wait for thread to finish
                if let Some(handle) = self.thread_handle.take() {
                    let _ = handle.join();
                }
            },
        }
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let serializer = move || match serde_json::to_value(&entry) {
            Ok(json) => json.to_string(),
            Err(err) => {
                let err_msg = format!("failed to serialize log entry to JSON: {err}");
                serde_json::to_value(ErrorLogEntry::from_loggable(&entry, err_msg.clone()))
                    .expect(&err_msg)
                    .to_string()
            },
        };
        match self.mode {
            StdOutLoggerMode::Immediate => {
                let json_string = serializer();
                if let Some(writer) = &self.writer {
                    let mut guard = writer
                        .lock()
                        .expect("failed to acquire lock on stdout writer");
                    guard
                        .write_all(json_string.as_bytes())
                        .expect(ERR_TO_WRITE_STD_LOGGER);
                    guard.write_all(b"\n").expect(ERR_TO_WRITE_STD_LOGGER);
                }
            },
            StdOutLoggerMode::Async { .. } => {
                if let Some(sender) = &self.sender {
                    sender.send(LogMessage::Log(Box::new(serializer))).expect(
                        "could not send log entry to queue StdOutLogger, receiver was deallocated",
                    );
                }
            },
        }
    }

    fn log_fn<F, R>(&self, log_fn: crate::log::loggable_fn::LoggableFn<F>)
    where
        R: Loggable,
        F: Fn(crate::log::BaseLogEntry) -> R,
    {
        if !log_fn.can_log(self.log_level) {
            // do nothing
            return;
        }

        self.log_any(log_fn.build());
    }
}

// Test writer created for mocking LogWriter
#[cfg(test)]
#[derive(Clone)]
pub(crate) struct TestWriter {
    buf: Arc<Mutex<Vec<u8>>>,
}

#[cfg(test)]
impl TestWriter {
    pub(crate) fn new() -> Self {
        Self {
            buf: Arc::new(Mutex::new(Vec::new())),
        }
    }

    pub(crate) fn into_inner_buf(self) -> String {
        let buf = self.buf.lock().unwrap();
        String::from_utf8_lossy(buf.as_slice()).into_owned()
    }

    pub(crate) fn get_buf_contents(&self) -> String {
        let buf = self.buf.lock().unwrap();
        String::from_utf8_lossy(buf.as_slice()).into_owned()
    }
}

#[cfg(test)]
impl Write for TestWriter {
    fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
        self.buf.lock().unwrap().extend_from_slice(buf);
        Ok(buf.len())
    }

    fn flush(&mut self) -> std::io::Result<()> {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use std::io::Write;

    use serde_json::json;

    use super::*;
    use crate::common::app_types::PdpID;
    use crate::log::log_strategy::LogEntryWithClientInfo;
    use crate::log::{BaseLogEntry, LogEntry, gen_uuid7};

    #[test]
    fn write_log_ok() {
        let pdp_id = PdpID::new();
        let app_name = None;
        // Create a log entry
        let log_entry = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "Test message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
        };
        let log_entry =
            LogEntryWithClientInfo::from_loggable(log_entry.clone(), pdp_id, app_name.clone());

        // Serialize the log entry to JSON
        let json_str = json!(log_entry).to_string();

        // Create a test writer
        let test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;

        // Create logger with test writer
        let logger = StdOutLogger::new_with(buffer, LogLevel::TRACE, StdOutLoggerMode::Immediate);

        // Log the entry
        logger.log_any(log_entry);

        // Drop logger to ensure writer thread finishes
        drop(logger);

        // Check logged content
        let logged_content = test_writer.into_inner_buf();

        // Verify that the log entry was logged correctly
        assert_eq!(logged_content, json_str + "\n");
    }

    #[test]
    fn flush_guaranteed_within_timeout_even_with_continuous_messages() {
        use std::thread;

        let pdp_id = PdpID::new();
        let app_name = None;

        // Create a test writer
        let test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;

        // Use a short timeout for testing (10ms)
        let flush_timeout_u64 = 10;
        let flush_timeout = Duration::from_millis(flush_timeout_u64);
        let logger = StdOutLogger::new_with(
            buffer,
            LogLevel::TRACE,
            StdOutLoggerMode::Async {
                timeout_millis: flush_timeout_u64,
                buffer_limit: StdOutLoggerMode::DEFAULT_BUFFER_LIMIT,
            },
        );

        // Create first log entry
        let log_entry1 = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "First message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
        };

        // Create second log entry
        let log_entry2 = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "Second message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
        };

        // Log first entry
        logger.log_any(log_entry1.clone());

        // Wait for half the timeout - buffer should still be empty
        thread::sleep(flush_timeout / 2);
        assert_eq!(
            test_writer.get_buf_contents(),
            "",
            "Buffer should be empty before flush timeout"
        );

        logger.log_any(log_entry2);

        // Wait for the full timeout plus small margin
        thread::sleep(flush_timeout / 2 + Duration::from_millis(1));

        // Buffer should now contain the first message (may also contain second message)
        let expected_json = json!(log_entry1).to_string() + "\n";
        let buf_contents = test_writer.get_buf_contents();
        assert!(
            buf_contents.starts_with(&expected_json),
            "First message should appear after flush timeout, got: {}",
            buf_contents
        );

        // Now continuously send messages at intervals shorter than timeout
        for i in 0..5 {
            let log_entry = LogEntry {
                base: BaseLogEntry::new_decision(gen_uuid7()),
                auth_info: None,
                msg: format!("Continuous message {}", i),
                error_msg: None,
                cedar_lang_version: None,
                cedar_sdk_version: None,
            };
            let log_entry =
                LogEntryWithClientInfo::from_loggable(log_entry.clone(), pdp_id, app_name.clone());

            logger.log_any(log_entry);

            // Wait shorter than timeout
            thread::sleep(flush_timeout / 4);

            // Verify that previous continuous messages might not be flushed yet,
            // but they shouldn't block the flush of the first message (already verified)
        }

        // Wait for final flush
        thread::sleep(flush_timeout * 2);

        // Clean up logger
        drop(logger);

        // Verify that all messages eventually arrived
        let final_content = test_writer.into_inner_buf();
        assert!(
            final_content.contains("First message"),
            "First message should still be in final output"
        );
        for i in 0..5 {
            assert!(
                final_content.contains(&format!("Continuous message {}", i)),
                "Continuous message {} should be in final output",
                i
            );
        }
    }
}
