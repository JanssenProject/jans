// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log entries for policy store operations.

use crate::log::interface::{Indexed, Loggable};
use crate::log::{BaseLogEntry, LogLevel, LogType};
use serde::Serialize;

/// Log entry for policy store operations.
#[derive(Serialize, Clone)]
pub struct PolicyStoreLogEntry {
    #[serde(flatten)]
    base: BaseLogEntry,
    msg: String,
}

impl PolicyStoreLogEntry {
    /// Create a new policy store log entry.
    pub fn new(msg: impl Into<String>, level: Option<LogLevel>) -> Self {
        let mut base = BaseLogEntry::new_opt_request_id(LogType::System, None);
        base.level = level;
        Self {
            base,
            msg: msg.into(),
        }
    }

    /// Create an info-level log entry.
    pub fn info(msg: impl Into<String>) -> Self {
        Self::new(msg, Some(LogLevel::INFO))
    }

    /// Create a warning-level log entry.
    pub fn warn(msg: impl Into<String>) -> Self {
        Self::new(msg, Some(LogLevel::WARN))
    }

    /// Create a debug-level log entry.
    pub fn debug(msg: impl Into<String>) -> Self {
        Self::new(msg, Some(LogLevel::DEBUG))
    }
}

impl Loggable for PolicyStoreLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.base.get_log_level()
    }
}

impl Indexed for PolicyStoreLogEntry {
    fn get_id(&self) -> uuid7::Uuid {
        self.base.get_id()
    }

    fn get_additional_ids(&self) -> Vec<uuid7::Uuid> {
        self.base.get_additional_ids()
    }

    fn get_tags(&self) -> Vec<&str> {
        self.base.get_tags()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new_with_level() {
        let entry = PolicyStoreLogEntry::new("Test message", Some(LogLevel::INFO));
        assert_eq!(entry.msg, "Test message");
        assert_eq!(entry.get_log_level(), Some(LogLevel::INFO));
    }

    #[test]
    fn test_info_helper() {
        let entry = PolicyStoreLogEntry::info("Info message");
        assert_eq!(entry.msg, "Info message");
        assert_eq!(entry.get_log_level(), Some(LogLevel::INFO));
    }

    #[test]
    fn test_warn_helper() {
        let entry = PolicyStoreLogEntry::warn("Warning message");
        assert_eq!(entry.msg, "Warning message");
        assert_eq!(entry.get_log_level(), Some(LogLevel::WARN));
    }
}
