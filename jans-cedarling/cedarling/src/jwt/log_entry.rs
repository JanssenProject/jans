// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::interface::{Indexed, Loggable};
use crate::log::{BaseLogEntry, LogLevel, LogType};
use serde::Serialize;

#[derive(Serialize)]
pub struct JwtLogEntry {
    #[serde(flatten)]
    base: BaseLogEntry,
    msg: String,
}

impl JwtLogEntry {
    /// Helper function for creating log messages
    pub fn new(msg: impl Into<String>, level: LogLevel) -> Self {
        let mut base = BaseLogEntry::new_opt_request_id(LogType::System, None);
        base.level = Some(level);
        Self {
            base,
            msg: msg.into(),
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
