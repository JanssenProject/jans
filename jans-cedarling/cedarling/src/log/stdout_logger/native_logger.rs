// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::io::Write;
use std::sync::{Arc, Mutex};

use crate::log::LogLevel;
use crate::log::interface::{LogWriter, Loggable};

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    // we use `dyn Write`` trait to make it testable and mockable.
    writer: Mutex<Box<dyn Write + Send + Sync>>,
    log_level: LogLevel,
}

impl StdOutLogger {
    pub(crate) fn new(log_level: LogLevel) -> Self {
        Self {
            writer: Mutex::new(Box::new(std::io::stdout())),
            log_level,
        }
    }

    // Create a new StdOutLogger with custom writer.
    // is used in tests.
    #[allow(dead_code)]
    pub(crate) fn new_with(writer: Box<dyn Write + Send + Sync>, log_level: LogLevel) -> Self {
        Self {
            writer: Mutex::new(Box::new(writer)),
            log_level,
        }
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
    use crate::common::app_types::PdpID;
    use crate::log::{LogEntry, LogType};

    #[test]
    fn write_log_ok() {
        // Create a log entry
        let log_entry = LogEntry {
            base: crate::log::BaseLogEntry::new(PdpID::new(), LogType::Decision),
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
        let logger = StdOutLogger::new_with(buffer, LogLevel::TRACE);

        // Log the entry
        logger.log(log_entry);

        // call flush just to get great coverage
        _ = test_writer.flush();

        // Check logged content
        let logged_content = test_writer.into_inner_buf();

        // Verify that the log entry was logged correctly
        assert_eq!(logged_content, json_str + "\n");
    }
}