/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::{Deserialize, Serialize};
use std::str::FromStr;

/// Log levels
/// Fatal level is the highest, trace is lowest
#[derive(
    Debug,
    PartialEq,
    PartialOrd,
    Eq,
    Ord,
    Copy,
    Clone,
    Default,
    derive_more::Display,
    Deserialize,
    Serialize,
)]
pub enum LogLevel {
    /// Fatal level
    FATAL = 5,
    /// Error level
    ERROR = 4,
    /// Warn level
    #[default]
    WARN = 3,
    /// Info level
    INFO = 2,
    /// Debug level
    DEBUG = 1,
    /// Trace level
    TRACE = 0,
}

impl FromStr for LogLevel {
    type Err = String;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "FATAL" => Ok(LogLevel::FATAL),
            "ERROR" => Ok(LogLevel::ERROR),
            "WARN" => Ok(LogLevel::WARN),
            "INFO" => Ok(LogLevel::INFO),
            "DEBUG" => Ok(LogLevel::DEBUG),
            "TRACE" => Ok(LogLevel::TRACE),
            _ => Err(format!("Invalid log level: {}", s)),
        }
    }
}
