// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::LogLevel;
use crate::log::LogType;
use crate::log::gen_uuid7;
use crate::log::interface::Indexed;
use crate::log::interface::Loggable;
use serde::Serialize;
use uuid7::Uuid;

use super::AuditMsg;

#[derive(Serialize)]
pub struct AuditLogEntry {
    id: Uuid,
    msg: String,
    tags: Vec<String>,
    level: LogLevel,
    #[serde(rename = "log_type")]
    kind: LogType,
}

impl AuditLogEntry {
    fn new(msg: &str, kind: LogType, level: LogLevel, tags: Vec<&str>) -> Self {
        let id = gen_uuid7();
        let mut tags = tags
            .into_iter()
            .map(|x| x.to_string())
            .collect::<Vec<String>>();
        tags.push(kind.to_string());
        tags.push(level.to_string());
        Self {
            id,
            msg: msg.to_string(),
            tags,
            level,
            kind,
        }
    }

    /// Helper function for creating log entries for sending HTTP requests
    pub fn new_http_debug(audit_msg: &AuditMsg) -> Self {
        let mut tags = vec!["audit"];

        let msg = match audit_msg {
            AuditMsg::Log(_) => {
                tags.push("log");
                "sending logs to \"/audit/log\""
            },
            AuditMsg::Health(_) => {
                tags.push("health");
                "sending health check to \"/audit/health\""
            },
            AuditMsg::Telemetry(_) => {
                tags.push("telemetry");
                "sending telemetry to \"/audit/telemetry\""
            },
        };

        Self::new(msg, LogType::System, LogLevel::DEBUG, tags)
    }

    /// Helper function for creating log entries for failed HTTP requests
    pub fn new_http_err(audit_msg: &AuditMsg, err_msg: &str) -> Self {
        let mut tags = vec!["audit"];

        let msg = match audit_msg {
            AuditMsg::Log(_) => {
                tags.push("log");
                format!("failed sending logs to \"/audit/log\": {}", err_msg)
            },
            AuditMsg::Health(_) => {
                tags.push("health");
                format!("failed sending logs to \"/audit/health\": {}", err_msg)
            },
            AuditMsg::Telemetry(_) => {
                tags.push("telemetry");
                format!("failed sending logs to \"/audit/telemetry\": {}", err_msg)
            },
        };

        Self::new(&msg, LogType::System, LogLevel::ERROR, tags)
    }
}

impl Indexed for AuditLogEntry {
    fn get_id(&self) -> Uuid {
        self.id
    }

    fn get_additional_ids(&self) -> Vec<Uuid> {
        vec![]
    }

    fn get_tags(&self) -> Vec<&str> {
        self.tags.iter().map(|s| s.as_str()).collect()
    }
}

impl Loggable for AuditLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        Some(self.level)
    }
}
