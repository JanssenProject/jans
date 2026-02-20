// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log entries for policy store operations.

use serde::Serialize;

use crate::log::interface::{Indexed, Loggable};
use crate::log::{BaseLogEntry, LogLevel};

/// Log entry for policy store operations.
#[derive(Serialize, Clone)]
pub(super) struct PolicyStoreLogEntry {
    #[serde(flatten)]
    base: BaseLogEntry,
    msg: String,
}

impl PolicyStoreLogEntry {
    /// Create a new policy store log entry with an explicit or default log level.
    ///
    /// Use this constructor when you need fine-grained control over the log level,
    /// such as DEBUG or ERROR levels, or when the level is determined dynamically.
    /// If no level is provided, defaults to TRACE. This is the most flexible option
    /// for system-level policy store logs where the severity needs to be explicitly
    /// controlled based on the operation context.
    fn new(msg: impl Into<String>, level: Option<LogLevel>) -> Self {
        let base = BaseLogEntry::new_system_opt_request_id(level.unwrap_or(LogLevel::TRACE), None);
        Self {
            base,
            msg: msg.into(),
        }
    }

    /// Create an info-level log entry for general informational messages.
    ///
    /// Use this convenience method for standard informational logs about policy store
    /// operations, such as successful loads, completed validations, or routine status
    /// updates. This is the recommended choice for most non-error, non-warning policy
    /// store events that should be visible in production logs.
    pub(super) fn info(msg: impl Into<String>) -> Self {
        Self::new(msg, Some(LogLevel::INFO))
    }

    /// Create a warning-level log entry for non-critical issues.
    ///
    /// Use this convenience method for warnings that don't prevent operation but should
    /// be noted, such as missing optional files, deprecated feature usage, or
    /// recoverable validation issues. These logs help identify potential problems
    /// without disrupting normal policy store functionality.
    pub(super) fn warn(msg: impl Into<String>) -> Self {
        Self::new(msg, Some(LogLevel::WARN))
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
