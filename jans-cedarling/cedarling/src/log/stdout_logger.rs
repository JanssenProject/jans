/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::interface::LogWriter;
use crate::models::log_entry::LogEntry;
use std::{cell::RefCell, io::Write, rc::Rc};

/// A logger that do nothing.
pub(crate) struct StdOutLogger {
    // we use `dyn Write`` trait to make it testable and mockable.
    writer: RefCell<Box<dyn Write>>,
}

impl StdOutLogger {
    pub(crate) fn new() -> Self {
        Self {
            writer: RefCell::new(Box::new(std::io::stdout())),
        }
    }

    // Create a new StdOutLogger with custom writer.
    // is used in tests.
    #[allow(dead_code)]
    pub(crate) fn new_with(writer: Box<dyn Write>) -> Self {
        Self {
            writer: RefCell::new(writer),
        }
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    fn log(&self, entry: LogEntry) {
        let json_str = serde_json::json!(&entry).to_string();
        // we can't handle error here or test it so we just panic if it happens.
        // we should have specific platform to get error
        writeln!(self.writer.borrow_mut(), "{}", &json_str).unwrap();
    }
}

// Test writer created for mocking LogWriter
#[allow(dead_code)]
#[derive(Clone)]
pub(crate) struct TestWriter {
    buf: Rc<RefCell<Vec<u8>>>,
}

#[allow(dead_code)]
impl TestWriter {
    pub(crate) fn new() -> Self {
        Self {
            buf: Rc::new(RefCell::new(Vec::new())),
        }
    }

    pub(crate) fn into_inner_buf(self) -> String {
        let buf = self.buf.take();
        String::from_utf8_lossy(buf.as_slice()).into_owned()
    }
}

impl Write for TestWriter {
    fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
        self.buf.borrow_mut().extend_from_slice(buf);
        Ok(buf.len())
    }

    fn flush(&mut self) -> std::io::Result<()> {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use crate::models::log_entry::LogType;

    use super::*;
    use std::{
        io::Write,
        time::{SystemTime, UNIX_EPOCH},
    };

    use uuid7::uuid7;

    #[test]
    fn write_log_ok() {
        // Create a log entry
        let log_entry = LogEntry {
            id: uuid7(),
            time: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .expect("Time went backwards")
                .as_secs(),
            log_kind: LogType::Decision,
            pdp_id: uuid7(),
            application_id: "test_app".to_string(),
            auth_info: None,
            msg: "Test message".to_string(),
        };

        // Serialize the log entry to JSON
        let json_str = serde_json::json!(&log_entry).to_string();

        // Create a test writer
        let mut test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + 'static>;

        // Create logger with test writer
        let logger = StdOutLogger::new_with(buffer);

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
