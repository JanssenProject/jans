/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::interface::LogWriter;
use super::LogEntry;

/// A logger that do nothing.
pub(crate) struct NopLogger;

// Implementation of LogWriter
impl LogWriter for NopLogger {
    fn log(&self, _entry: LogEntry) {
        // Do nothing
    }
}
