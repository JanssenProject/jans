// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::interface::{Indexed, Loggable};
use crate::log::{BaseLogEntry, LogLevel, LogType};
use serde::Serialize;

#[derive(Serialize, Clone)]
pub struct JwtLogEntry {
    #[serde(flatten)]
    base: BaseLogEntry,
    msg: String,
}

impl JwtLogEntry {
    /// Helper function for creating log messages
    pub fn new(msg: String, level: Option<LogLevel>) -> Self {
        let mut base = BaseLogEntry::new_opt_request_id(LogType::System, None);
        base.level = level;
        Self {
            base,
            msg,
        }
    }
}

impl Loggable for JwtLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.base.get_log_level()
    }
}

impl Indexed for JwtLogEntry {
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
mod test {
    use super::*;
    use crate::log::LogLevel;

    #[test]
    fn test_new_with_level() {
        let msg = "Test message".to_string();
        let level = Some(LogLevel::ERROR);
        let entry = JwtLogEntry::new(msg.clone(), level);

        assert_eq!(entry.msg, msg);
        assert_eq!(entry.get_log_level(), level);
    }

    #[test]
    fn test_new_without_level() {
        let msg = "Test message".to_string();
        let entry = JwtLogEntry::new(msg.clone(), None);

        assert_eq!(entry.msg, msg);
        assert_eq!(entry.get_log_level(), None);
    }

    #[test]
    fn test_new_with_different_levels() {
        let msg = "Test message".to_string();
        
        let entry = JwtLogEntry::new(msg.clone(), Some(LogLevel::INFO));
        assert_eq!(entry.get_log_level(), Some(LogLevel::INFO));

        let entry = JwtLogEntry::new(msg.clone(), Some(LogLevel::WARN));
        assert_eq!(entry.get_log_level(), Some(LogLevel::WARN));

        let entry = JwtLogEntry::new(msg.clone(), Some(LogLevel::ERROR));
        assert_eq!(entry.get_log_level(), Some(LogLevel::ERROR));
    }
}
