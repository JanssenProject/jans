// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log interface
//! Contains the interface for logging. And getting log information from storage.

use uuid7::Uuid;

use super::LogLevel;

/// Log Writer
/// interface for logging events
pub(crate) trait LogWriter {
    /// log any serializable entry that not suitable for [`LogEntry`]
    fn log_any<T: Loggable>(&self, entry: T);
}

const SEPARATOR: &str = "__";

pub(crate) fn composite_key(id: &str, tag: &str) -> String {
    [id, tag].join(SEPARATOR)
}

pub(crate) trait Indexed {
    /// Get unique ID of entity
    //  Is used in memory logger
    fn get_id(&self) -> Uuid;

    /// List of additional ids that entity can be related
    //  Is used in memory logger
    fn get_additional_ids(&self) -> Vec<Uuid>;

    /// List of `tags` that entity can be related
    //  Is used in memory logger
    fn get_tags(&self) -> Vec<&str>;

    fn get_index_keys(&self) -> Vec<String> {
        let tags = self.get_tags();

        let additional_ids = self
            .get_additional_ids()
            .into_iter()
            .map(|v| v.to_string())
            .collect::<Vec<String>>();

        let additional_id_and_tag = additional_ids
            .iter()
            .flat_map(|id| tags.iter().map(move |tag| composite_key(id, tag)))
            .collect::<Vec<String>>();

        let tags_iter = tags
            .into_iter()
            .map(Into::<String>::into)
            .collect::<Vec<String>>();

        let mut result = Vec::with_capacity(
            additional_ids.len() + additional_id_and_tag.len() + tags_iter.len(),
        );

        result.extend(additional_ids);
        result.extend(additional_id_and_tag);
        result.extend(tags_iter);

        result
    }
}

pub(crate) trait Loggable: serde::Serialize + Indexed {
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
    /// Return logs and remove them from the storage
    fn pop_logs(&self) -> Vec<serde_json::Value>;

    /// Get specific log entry
    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value>;

    /// Returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String>;

    /// Get logs by tag, like `log_kind` or `log level`.
    /// Tag can be `log_kind`, `log_level`.
    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value>;

    /// Get logs by request_id.
    /// Return log entries that match the given request_id.
    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value>;

    /// Get log by request_id and tag, like composite key `request_id` + `log_kind`.
    /// Tag can be `log_kind`, `log_level`.
    /// Return log entries that match the given request_id and tag.
    fn get_logs_by_request_id_and_tag(&self, request_id: &str, tag: &str)
    -> Vec<serde_json::Value>;
}

/// Implementation for buffering logs.
///
/// # Usage
///
/// 1.  Call [`LogBuffer::batch_logs`] then try to send it to the lock server
/// 2a. If sending the logs is successful, call [`LogBuffer::flush_batch`]
/// 2b. If sending the logs failed, and you want to clear the batch but not
///     lose the logs, call [`LogBuffer::clear_batch`]
// will be used for lock server integration
#[allow(dead_code)]
pub(crate) trait LogBuffer {
    /// Batches some logs to send to lock master
    fn batch_logs(&self) -> Vec<serde_json::Value>;

    /// Clear the current batch then remove the logs from log storage if
    /// they are being stored.
    fn flush_batch(&mut self);

    /// Clear the current batch but don't get rid of the logs if they are
    /// being stored.
    fn clear_batch(&mut self);
}
