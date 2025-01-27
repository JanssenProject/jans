// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log interface
//! Contains the interface for logging. And getting log information from storage.

use std::iter::once;

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

        let id: String = self.get_id().to_string();

        let ids = self
            .get_additional_ids()
            .into_iter()
            .chain(once(self.get_id()))
            .map(|v| v.to_string())
            .collect::<Vec<String>>();

        let id_and_tags = tags
            .iter()
            .map(|tag| composite_key(&id, *tag))
            .collect::<Vec<String>>();

        let additional_id_and_tag = self
            .get_additional_ids()
            .into_iter()
            .map(|id| id.to_string())
            .flat_map(|id| tags.iter().map(move |tag| composite_key(&id, *tag)))
            .collect::<Vec<String>>();

        let tags_iter = tags
            .into_iter()
            .map(Into::<String>::into)
            .collect::<Vec<String>>();

        let mut result = Vec::with_capacity(
            ids.len() + id_and_tags.len() + additional_id_and_tag.len() + tags_iter.len(),
        );

        result.extend(ids);
        result.extend(id_and_tags);
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
    /// return logs and remove them from the storage
    fn pop_logs(&self) -> Vec<serde_json::Value>;

    /// get specific log entry
    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value>;

    /// returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String>;

    /// get logs by tag, like `log_kind` or `log level`
    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value>;

    /// get log by id and tag, like `request_id` + `log_kind`
    fn get_logs_by_id_and_tag(&self, id: &str, tag: &str) -> Vec<serde_json::Value>;
}
