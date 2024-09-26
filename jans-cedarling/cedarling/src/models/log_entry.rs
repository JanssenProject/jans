/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # Log entry
//! The module contains structs for logging events.

use std::time::{SystemTime, UNIX_EPOCH};

use uuid7::uuid7;
use uuid7::Uuid;

/// LogEntry is a struct that encapsulates all relevant data for logging events.
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct LogEntry {
    /// unique identifier for this event
    pub id: Uuid,
    /// Time of decision, in unix time
    pub time: u64,
    /// kind of log entry
    pub log_kind: LogType,
    /// unique id of cedarling
    pub pdp_id: Uuid,
    /// message of the event
    pub msg: String,
    /// name of application from [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties)
    pub application_id: String,
    /// authorization information of the event
    #[serde(flatten)]
    pub auth_info: Option<AuthorizationLogInfo>,
}

impl LogEntry {
    pub fn new_with_data(pdp_id: Uuid, application_id: String, log_kind: LogType) -> LogEntry {
        let unix_time_sec = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();

        Self {
            // We use uuid v7 because it is generated based on the time and sortable.
            // and we need sortable ids to use it in the sparkv database.
            // Sparkv store data in BTree. So we need have correct order of ids.
            id: uuid7(),
            time: unix_time_sec,
            log_kind,
            pdp_id,
            application_id,
            auth_info: None,
            msg: String::new(),
        }
    }

    pub fn set_message(mut self, message: String) -> Self {
        self.msg = message;
        self
    }

    pub fn set_auth_info(mut self, auth_info: AuthorizationLogInfo) -> Self {
        self.auth_info = Some(auth_info);
        self
    }
}

/// Type of log entry
#[derive(Debug, Clone, Copy, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum LogType {
    Decision,
    System,
    Metric,
}

/// Log information about authorization request
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct AuthorizationLogInfo {
    /// cedar-policy principal
    pub principal: String,
    /// cedar-policy action
    pub action: String,
    /// cedar-policy resource
    pub resource: String,
    /// cedar-policy context
    pub context: String,
    /// cedar-policy decision
    pub decision: Decision,
    /// cedar-policy diagnostics information
    pub diagnostics: String,
}

/// Cedar-policy decision of the authorization
#[derive(Debug, Clone, PartialEq, Copy, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum Decision {
    Allow,
    Deny,
}
