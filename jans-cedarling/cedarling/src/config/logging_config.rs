// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use derive_more::derive::Deref;
use serde::{Deserialize, Serialize};

/// Config specific to logging
#[allow(missing_docs)]
#[derive(Debug, Deserialize, Serialize, PartialEq)]
#[serde(rename_all = "snake_case")]
pub struct LoggingConfig {
    #[serde(rename = "log_type", alias = "CEDARLING_LOG_TYPE", default)]
    pub logger_kind: LoggerKind,

    #[serde(alias = "CEDARLING_LOG_LEVEL", default)]
    pub log_level: LogLevel,

    /// List of claims used by the `User` entity to include in the log entry.
    #[serde(alias = "CEDARLING_DECISION_LOG_USER_CLAIMS", default)]
    pub decision_log_user_claims: Vec<String>,

    /// List of claims used by the `Workload` entity to include in the log entry.
    #[serde(alias = "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS", default)]
    pub decision_log_workload_claims: Vec<String>,

    #[serde(alias = "CEDARLING_DECISION_LOG_DEFAULT_JWT_ID", default)]
    pub decision_log_default_jwt_id: DecisionLogDefaultJwtId,

    #[serde(alias = "CEDARLING_LOG_TTL", default)]
    pub log_ttl: LogTtl,

    #[serde(alias = "CEDARLING_LOG_MAX_ITEMS", default)]
    pub log_max_items: LogMaxItems,

    #[serde(alias = "CEDARLING_LOG_MAX_ITEM_SIZE", default)]
    pub log_max_item_size: LogMaxItemSize,
}

// Used to add a link in the docstring.
#[allow(unused)]
use crate::LogStorage;

/// Sets the logging strategy
#[derive(Debug, Default, Deserialize, Serialize, PartialEq, Clone, Copy)]
#[serde(rename_all = "snake_case")]
#[allow(missing_docs)]
pub enum LoggerKind {
    #[default]
    Off,
    /// Logger that collect messages in memory which will be available for retrieval
    /// using [`pop_logs`].
    ///
    /// [`pop_logs`]: crate::Cedarling::pop_logs
    Memory,
    /// Logger that print logs to stdout
    StdOut,
}

#[derive(Debug, Default, Deserialize, Serialize, PartialEq, Clone, Copy)]
#[serde(rename_all = "UPPERCASE")]
#[allow(missing_docs)]
pub enum LogLevel {
    Fatal = 5,
    Error = 4,
    #[default]
    Warn = 3,
    Info = 2,
    Debug = 1,
    Trace = 0,
}

#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
#[allow(missing_docs)]
pub struct DecisionLogDefaultJwtId(String);

impl Default for DecisionLogDefaultJwtId {
    /// `"jwt"`
    fn default() -> Self {
        Self("jwt".to_string())
    }
}

/// The TTL (time to live) of log entries in seconds if using [`LoggerKind::Memory`].
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct LogTtl(u64);

impl Default for LogTtl {
    /// 60 seconds
    fn default() -> Self {
        Self(60)
    }
}

/// Maximum number of log entries that can be stored using [`LoggerKind::Memory`].
///
/// Set this to 0 to remove the limit.
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct LogMaxItems(u64);

impl Default for LogMaxItems {
    /// No limit (`0`)
    fn default() -> Self {
        Self(0)
    }
}

/// Maximum size of a single log entity in bytes when using [`LoggerKind::Memory`].
///
/// Set this to 0 to remove the limit.
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct LogMaxItemSize(u64);

impl Default for LogMaxItemSize {
    /// No limit (`0`)
    fn default() -> Self {
        Self(0)
    }
}
