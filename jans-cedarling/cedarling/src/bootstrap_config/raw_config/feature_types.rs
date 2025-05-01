// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{fmt::Display, str::FromStr};

use serde::{Deserialize, Serialize};

/// Type of logger
#[derive(Debug, PartialEq, Deserialize, Serialize, Default)]
#[serde(rename_all = "lowercase")]
pub enum LoggerType {
    /// Disabled logger
    #[default]
    Off,
    /// Logger that collect messages in memory.
    /// Log entities available using trait [`LogStorage`](crate::LogStorage)
    Memory,
    /// Logger that print logs to stdout
    #[serde(rename = "std_out")]
    StdOut,
    /// Logger send log messages to `Lock` server
    Lock,
}

impl FromStr for LoggerType {
    type Err = ParseLoggerTypeError;

    /// Parse string to `LoggerType` enum.
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_lowercase();
        match s.as_str() {
            "memory" => Ok(Self::Memory),
            "std_out" => Ok(Self::StdOut),
            "lock" => Ok(Self::Lock),
            "off" => Ok(Self::Off),
            _ => Err(Self::Err { logger_type: s }),
        }
    }
}

impl Display for LoggerType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            // we have to make the string representation is lowercase
            LoggerType::Off => write!(f, "off"),
            LoggerType::Memory => write!(f, "memory"),
            LoggerType::StdOut => write!(f, "stdout"),
            LoggerType::Lock => write!(f, "lock"),
        }
    }
}

/// Enum varians that represent if feature is enabled or disabled
#[derive(Debug, PartialEq, Deserialize, Serialize, Default, Copy, Clone)]
#[serde(rename_all = "lowercase")]
pub enum FeatureToggle {
    /// Represent as disabled.
    #[default]
    Disabled,
    /// Represent as enabled.
    Enabled,
}

impl From<FeatureToggle> for bool {
    fn from(value: FeatureToggle) -> bool {
        match value {
            FeatureToggle::Disabled => false,
            FeatureToggle::Enabled => true,
        }
    }
}

impl TryFrom<String> for FeatureToggle {
    type Error = ParseFeatureToggleError;

    fn try_from(s: String) -> Result<Self, Self::Error> {
        let s = s.to_lowercase();
        match s.as_str() {
            "enabled" => Ok(FeatureToggle::Enabled),
            "disabled" => Ok(FeatureToggle::Disabled),
            _ => Err(ParseFeatureToggleError { value: s }),
        }
    }
}

impl FromStr for FeatureToggle {
    type Err = ParseFeatureToggleError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_lowercase();
        match s.as_str() {
            "enabled" => Ok(FeatureToggle::Enabled),
            "disabled" => Ok(FeatureToggle::Disabled),
            _ => Err(ParseFeatureToggleError { value: s }),
        }
    }
}

impl FeatureToggle {
    /// Parse bool to `FeatureToggle`.
    pub fn from_bool(v: bool) -> Self {
        match v {
            true => Self::Enabled,
            false => Self::Disabled,
        }
    }

    /// Return true if is enabled.
    pub fn is_enabled(&self) -> bool {
        match self {
            Self::Enabled => true,
            Self::Disabled => false,
        }
    }
}

impl From<bool> for FeatureToggle {
    fn from(val: bool) -> Self {
        FeatureToggle::from_bool(val)
    }
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Could not parce `WorkloadBoolOp` with payload {payload}, should be `AND` or `OR`")]
pub struct ParseWorkloadBoolOpError {
    payload: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `TrustMode`: {trust_mode}. should be `strict` or `none`")]
pub struct ParseTrustModeError {
    trust_mode: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `LoggerType`: {logger_type}. should be `memory`, `std_out`, `lock`, or `off`")]
pub struct ParseLoggerTypeError {
    logger_type: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `FeatureToggle`: {value}. should be `enabled`, or `disabled`")]
pub struct ParseFeatureToggleError {
    value: String,
}
