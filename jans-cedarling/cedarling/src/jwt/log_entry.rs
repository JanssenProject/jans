// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::interface::{Indexed, Loggable};
use crate::log::{BaseLogEntry, LogType};
use serde::Serialize;

#[derive(Serialize)]
pub struct JwtLogEntry {
    #[serde(flatten)]
    base: BaseLogEntry,
    msg: String,
}

impl JwtLogEntry {
    /// Shorthand for creating a [`LogType::System`] log.
    pub fn system(msg: String) -> Self {
        let base = BaseLogEntry::new_opt_request_id(LogType::System, None);
        Self { base, msg }
    }
}

impl Loggable for JwtLogEntry {
    fn get_log_level(&self) -> Option<crate::LogLevel> {
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
