// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::LogLevel;
use crate::log::gen_uuid7;
use crate::log::interface::{Indexed, Loggable};
use serde::Serialize;

#[derive(Serialize)]
pub struct LockLogEntry {
    message: String,
    level: LogLevel,
    id: uuid7::Uuid,
}

impl LockLogEntry {
    #[allow(dead_code)]
    pub fn error(msg: &'static str) -> Self {
        Self {
            message: msg.to_string(),
            level: LogLevel::ERROR,
            id: gen_uuid7(),
        }
    }

    /// If you're not using [`format!`] to build the message, it's better to use [`Self::error`].
    pub fn error_fmt(msg: String) -> Self {
        Self {
            message: msg,
            level: LogLevel::ERROR,
            id: gen_uuid7(),
        }
    }
}

impl Loggable for LockLogEntry {
    fn get_log_level(&self) -> Option<crate::LogLevel> {
        Some(self.level)
    }
}

impl Indexed for LockLogEntry {
    fn get_id(&self) -> uuid7::Uuid {
        self.id
    }

    fn get_additional_ids(&self) -> Vec<uuid7::Uuid> {
        Vec::new()
    }

    fn get_tags(&self) -> Vec<&str> {
        Vec::new()
    }
}
