// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::io::Write;
use std::sync::{Arc, Mutex};

use serde_json::Value;

use crate::log::LogLevel;
use crate::log::interface::{LogBuffer, LogWriter, Loggable};

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    // we use `dyn Write`` trait to make it testable and mockable.
    writer: Mutex<Box<dyn Write + Send + Sync>>,
    log_level: LogLevel,
    lock_queued_logs: Mutex<Vec<Value>>,
    // will be used for lock integration
    #[allow(dead_code)]
    current_lock_buf_size: Mutex<usize>,
    queue_logs_for_lock: bool,
}

impl StdOutLogger {
    pub(crate) fn new(log_level: LogLevel, queue_logs_for_lock: bool) -> Self {
        Self {
            writer: Mutex::new(Box::new(std::io::stdout())),
            log_level,
            lock_queued_logs: vec![].into(),
            current_lock_buf_size: 0.into(),
            queue_logs_for_lock,
        }
    }

    // Create a new StdOutLogger with custom writer.
    // is used in tests.
    #[allow(dead_code)]
    pub(crate) fn new_with(
        writer: Box<dyn Write + Send + Sync>,
        log_level: LogLevel,
        queue_logs_for_lock: bool,
    ) -> Self {
        Self {
            writer: Mutex::new(Box::new(writer)),
            log_level,
            lock_queued_logs: vec![].into(),
            current_lock_buf_size: 0.into(),
            queue_logs_for_lock,
        }
    }

    // will be used for lock server integration
    #[allow(dead_code)]
    fn queued_logs_len(&self) -> usize {
        self.lock_queued_logs
            .lock()
            .expect("should acquire lock queud logs lock")
            .len()
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    #[cfg(not(target_arch = "wasm32"))]
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let json_str = serde_json::json!(&entry).to_string();

        if self.queue_logs_for_lock {
            let mut lock_queued_logs = self
                .lock_queued_logs
                .lock()
                .expect("should obtain lock queued logs lock");
            lock_queued_logs.push(serde_json::to_value(entry).expect("should serialize log entry"));
        }

        // we can't handle error here or test it so we just panic if it happens.
        // we should have specific platform to get error
        writeln!(
            self.writer
                .lock()
                .expect("In StdOutLogger writer mutex should unlock"),
            "{}",
            &json_str
        )
        .unwrap();
    }

    #[cfg(target_arch = "wasm32")]
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }
    }
}

impl LogBuffer for StdOutLogger {
    fn batch_logs(&self) -> Vec<serde_json::Value> {
        let mut buf_size_lock = self
            .current_lock_buf_size
            .lock()
            .expect("should acquire lock for current log buf size");
        let buf_logs_lock = self
            .lock_queued_logs
            .lock()
            .expect("should acquire lock for buffered logs");
        *buf_size_lock = buf_logs_lock.len();
        let logs = buf_logs_lock
            .get(0..*buf_size_lock)
            .map(|x| x.iter().cloned().collect::<Vec<serde_json::Value>>())
            .unwrap_or_else(|| vec![]);
        logs
    }

    fn flush_batch(&mut self) {
        let mut buf_size_lock = self
            .current_lock_buf_size
            .lock()
            .expect("should acquire lock for current log buf size");
        let mut buf_logs_lock = self
            .lock_queued_logs
            .lock()
            .expect("should acquire lock for buffered logs");
        buf_logs_lock.drain(0..*buf_size_lock);
        *buf_size_lock = 0;
    }

    fn clear_batch(&mut self) {
        let mut buf_size_lock = self
            .current_lock_buf_size
            .lock()
            .expect("should acquire lock for current log buf size");
        *buf_size_lock = 0;
    }
}

// Test writer created for mocking LogWriter
#[allow(dead_code)]
#[derive(Clone)]
pub(crate) struct TestWriter {
    buf: Arc<Mutex<Vec<u8>>>,
}

#[allow(dead_code)]
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
}

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

    use super::*;
    use crate::common::app_types::{self, PdpID};
    use crate::log::{LogEntry, LogType, gen_uuid7};

    #[test]
    fn write_log_ok() {
        // Create a log entry
        let log_entry = LogEntry {
            base: crate::log::BaseLogEntry::new(PdpID::new(), LogType::Decision, gen_uuid7()),
            application_id: Some("test_app".to_string().into()),
            auth_info: None,
            msg: "Test message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
        };

        // Serialize the log entry to JSON
        let json_str = serde_json::json!(&log_entry).to_string();

        // Create a test writer
        let mut test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;

        // Create logger with test writer
        let logger = StdOutLogger::new_with(buffer, LogLevel::TRACE, false);

        // Log the entry
        logger.log_any(log_entry);

        // call flush just to get great coverage
        _ = test_writer.flush();

        // Check logged content
        let logged_content = test_writer.into_inner_buf();

        // Verify that the log entry was logged correctly
        assert_eq!(logged_content, json_str + "\n");
    }

    #[test]
    fn test_buffering_logs() {
        let mut logger = StdOutLogger::new(LogLevel::DEBUG, true);

        let first_entry =
            LogEntry::new_with_data(app_types::PdpID::new(), None, LogType::Metric, None);
        logger.log_any(first_entry.clone());
        let batch = logger.batch_logs();
        assert_eq!(batch.len(), 1, "batch should have 1 entry");

        logger.clear_batch();
        assert_eq!(
            logger.queued_logs_len(),
            1,
            "clearing the batch should not dispose of the logs"
        );

        let batch = logger.batch_logs();
        let second_entry =
            LogEntry::new_with_data(app_types::PdpID::new(), None, LogType::Metric, None);
        logger.log_any(second_entry.clone());
        assert_eq!(logger.queued_logs_len(), 2, "logger should have 2 logs");
        assert_eq!(batch.len(), 1, "batch should have 1 entry");
        assert_eq!(
            batch[0],
            serde_json::to_value(first_entry).expect("should serialize first log entry")
        );

        logger.flush_batch();
        let batch = logger.batch_logs();

        assert_eq!(batch.len(), 1, "batch should have 1 entry");
        assert_eq!(
            batch[0],
            serde_json::to_value(second_entry).expect("should serialize second log entry")
        );
    }
}
