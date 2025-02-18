// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Log entry
//! The module contains structs for logging events.

use super::interface::{Indexed, Loggable};
use super::{BaseLogEntry, ISO8601, LogLevel};
use uuid7::Uuid;

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct ErrorLogEntry {
    #[serde(flatten)]
    pub base: BaseLogEntry,
    pub msg: String,
}

impl ErrorLogEntry {
    pub fn from_loggable<L: Loggable>(value: &L, msg: String) -> Self {
        let timestamp = Some(chrono::Local::now().format(ISO8601).to_string());
        Self {
            base: BaseLogEntry {
                id: value.get_id(),
                request_id: value.get_additional_ids().pop(),
                timestamp,
                log_kind: super::LogType::System,
                level: Some(LogLevel::ERROR),
            },
            msg,
        }
    }
}

impl Indexed for ErrorLogEntry {
    fn get_id(&self) -> Uuid {
        self.base.get_id()
    }

    fn get_additional_ids(&self) -> Vec<Uuid> {
        self.base.get_additional_ids()
    }

    fn get_tags(&self) -> Vec<&str> {
        self.base.get_tags()
    }
}

impl Loggable for ErrorLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.base.get_log_level()
    }
}
