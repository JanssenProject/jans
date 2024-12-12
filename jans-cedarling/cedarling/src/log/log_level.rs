/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::{de, Deserialize, Deserializer, Serialize, Serializer};
use std::fmt;
use std::str::FromStr;

/// Log levels
/// Fatal level is the highest, trace is lowest
#[derive(Debug, PartialEq, PartialOrd, Eq, Ord, Copy, Clone, Default)]
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

impl LogLevel {
    fn as_str(&self) -> &'static str {
        match self {
            LogLevel::FATAL => "FATAL",
            LogLevel::ERROR => "ERROR",
            LogLevel::WARN => "WARN",
            LogLevel::INFO => "INFO",
            LogLevel::DEBUG => "DEBUG",
            LogLevel::TRACE => "TRACE",
        }
    }
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

impl Serialize for LogLevel {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(self.as_str())
    }
}

impl<'de> Deserialize<'de> for LogLevel {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_str(LogLevelVisitor)
    }
}

// Custom Visitor for LogLevel deserialization
//
// we use custom visitor to avoid error: `invalid type: string "DEBUG", expected a borrowed string`
// so we implement deserialization for `&str` and for `String`
struct LogLevelVisitor;

impl de::Visitor<'_> for LogLevelVisitor {
    type Value = LogLevel;

    fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        formatter.write_str("a log level string (FATAL, ERROR, WARN, INFO, DEBUG, or TRACE)")
    }

    fn visit_str<E>(self, v: &str) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        LogLevel::from_str(v).map_err(de::Error::custom)
    }

    fn visit_string<E>(self, v: String) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        LogLevel::from_str(&v).map_err(de::Error::custom)
    }
}
