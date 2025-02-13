// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::LogLevel;

/// A set of properties used to configure logging in the `Cedarling` application.
#[derive(Debug, Clone, PartialEq)]
pub struct LogConfig {
    /// `CEDARLING_LOG_TYPE` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub log_type: LogTypeConfig,

    /// Log level filter for logging. TRACE is lowest. FATAL is highest.
    /// `CEDARLING_LOG_LEVEL` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub log_level: LogLevel,
}

///  Log type configuration.
///  `CEDARLING_LOG_TYPE` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
///   Current type represent this value.
#[derive(Debug, Clone, PartialEq)]
pub enum LogTypeConfig {
    /// Logger do nothing. It means that all logs will be ignored.
    Off,
    /// Logger holds all logs in database (in memory) with eviction policy.
    Memory(MemoryLogConfig),
    /// Logger writes log information to std output stream.
    StdOut,
    /// The logger sends log data to the server (corporate feature).
    Lock(LockLogConfig),
}

/// Configuration for memory log.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct MemoryLogConfig {
    /// `CEDARLING_LOG_TTL` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// The maximum time to live (in seconds) of the log entries.
    pub log_ttl: u64,
}

/// Configuration specific for lock server integration
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct LockLogConfig {
    /// How often to send log messages to Lock Master. Setting this to `0` will
    /// turn off transmissions.
    pub log_interval: u64,
    /// How often to send health messages to Lock Master. Setting this to `0`
    /// will turn off transmissions.
    pub health_interval: u64,
    /// How often to send telemetry messages to Lock Master. Setting this to
    /// `0` will turn off transmission.
    pub telemetry_interval: u64,
}
