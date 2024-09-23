/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Log interface
//! Contains the interface for logging. And getting log information from storage.

use crate::models::log_entry::LogEntry;

/// Log Writer
/// interface for logging events
pub(crate) trait LogWriter {
    /// log logging entry
    fn log(&self, entry: LogEntry);
}

/// Log Storage
/// interface for getting log entries from the storage
pub trait LogStorage {
    /// return logs and remove them from the storage
    fn pop_logs(&self) -> Vec<LogEntry>;

    /// get specific log entry
    fn get_log_by_id(&self, id: &str) -> Option<LogEntry>;

    /// returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String>;
}
