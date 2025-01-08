// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log interface
//! Contains the interface for logging. And getting log information from storage.

use uuid7::Uuid;

use super::{LogEntry, LogLevel};

/// Log Writer
/// interface for logging events
pub(crate) trait LogWriter {
    /// log any serializable entry that not suitable for [`LogEntry`]
    fn log_any<T: Loggable>(&self, entry: T);

    /// log logging entry
    fn log(&self, entry: LogEntry) {
        self.log_any(entry);
    }
}

pub(crate) trait Loggable: serde::Serialize {
    /// get unique request ID
    fn get_request_id(&self) -> Uuid;

    /// get log level for entity
    /// not all log entities have log level, only when `log_kind` == `System`
    fn get_log_level(&self) -> Option<LogLevel>;

    /// check if entry can log to logger
    // default implementation of method
    // is used to avoid boilerplate code
    fn can_log(&self, logger_level: LogLevel) -> bool {
        if let Some(entry_log_level) = self.get_log_level() {
            // higher level is more important, ie closer to fatal
            logger_level <= entry_log_level
        } else {
            // if `.get_log_level` return None
            // it means that `log_kind` != `System` and we should log it
            true
        }
    }
}

/// Log Storage
/// interface for getting log entries from the storage
pub trait LogStorage {
    /// return logs and remove them from the storage
    fn pop_logs(&self) -> Vec<serde_json::Value>;

    /// get specific log entry
    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value>;

    /// returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String>;
}
