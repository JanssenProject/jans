// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::io::Write;
use std::sync::{Arc, Mutex};

use crate::log::LogLevel;
use crate::log::interface::{LogWriter, Loggable};

use serde_wasm_bindgen::to_value;
use web_sys::js_sys::{Array, Object};
use web_sys::wasm_bindgen::JsValue;
use web_sys::{ConsoleLevel, console};

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    log_level: LogLevel,
}

impl StdOutLogger {
    pub(crate) fn new(log_level: LogLevel) -> Self {
        Self { log_level }
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let json_string = serde_json::json!(entry).to_string();
        let js_string = JsValue::from(json_string);

        let mut js_array = Array::new();
        js_array.push(&js_string);

        match entry.get_log_level() {
            Some(LogLevel::FATAL) => {
                // error is highest level of logging
                console::error(&js_array);
            },
            Some(LogLevel::ERROR) => {
                console::error(&js_array);
            },
            Some(LogLevel::WARN) => {
                console::warn(&js_array);
            },
            Some(LogLevel::INFO) => {
                console::info(&js_array);
            },
            Some(LogLevel::DEBUG) => {
                console::debug(&js_array);
            },
            Some(LogLevel::TRACE) => {
                console::trace(&js_array);
            },
            None => console::log(&js_array),
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
