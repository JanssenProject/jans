// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{BootstrapConfigLoadingError, BootstrapConfigRaw};
use crate::log::LogLevel;
use std::time::Duration;
use url::Url;
use serde::{Deserialize, Serialize};

/// A set of properties used to configure logging in the `Cedarling` application.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
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
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum LogTypeConfig {
    /// Logger do nothing. It means that all logs will be ignored.
    Off,
    /// Logger holds all logs in database (in memory) with eviction policy.
    Memory(MemoryLogConfig),
    /// Logger writes log information to std output stream.
    StdOut,
}

/// Configuration for memory log.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct MemoryLogConfig {
    /// `CEDARLING_LOG_TTL` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// The maximum time to live (in seconds) of the log entries.
    pub log_ttl: u64,

    /// `CEDARLING_LOG_MAX_ITEMS` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// The maximum number of log entries to keep in memory.
    pub max_items: Option<usize>,

    /// `CEDARLING_LOG_MAX_ITEM_SIZE` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// The maximum size of a log entry in bytes.
    pub max_item_size: Option<usize>,
}

/// Config for the lock logger that are set using the bootstrap configs.
#[derive(Debug, Clone, PartialEq)]
pub struct LockServiceConfig {
    /// The logging level
    pub log_level: LogLevel,
    /// URI where Cedarling can get metadata about the lock server.
    /// i.e. `.well-known/lock-server-configuration`
    ///  
    /// This is set using the `CEDARLING_LOCK_SERVER_CONFIGURATION_URI` bootstrap property.
    pub config_uri: Url,
    /// Toggles whether Cedarling should listen for SSE config updates.
    ///
    /// This is set using the `CEDARLING_LOCK_DYNAMIC_CONFIGURATION ` bootstrap property.
    //  TODO: This feature is not yet implemented
    pub dynamic_config: bool,
    /// Software Statement Assertion Json Web Token that Cedarling will use for Dynamic
    /// Client Registration.
    ///
    /// This is set using the `CEDARLING_LOCK_SSA_JWT` bootstrap property.
    pub ssa_jwt: Option<String>,
    /// Intervals to send log messges to the lock server.
    /// Set this to [`None`] to disable transmission.
    ///
    /// This is set using the `CEDARLING_LOCK_LOG_INTERVAL` bootstrap property.
    pub log_interval: Option<Duration>,
    /// Intervals to send health messges to the lock server.
    /// Set this to [`None`] to disable transmission.
    ///
    /// This is set using the `CEDARLING_LOCK_HEALTH_INTERVAL` bootstrap property.
    //  TODO: This feature is not yet implemented
    pub health_interval: Option<Duration>,
    /// Intervals to send telemetry messges to the lock server.
    /// Set this to [`None`] to disable transmission.
    ///
    /// This is set using the `CEDARLING_LOCK_TELEMETRY_INTERVAL` bootstrap property.
    //  TODO: This feature is not yet implemented
    pub telemetry_interval: Option<Duration>,
    /// Controls whether Cedarling should listen for updates from the Lock Server.
    ///
    /// This is set using the `CEDARLING_LOCK_LISTEN_SSE` bootstrap property.
    //  TODO: This feature is not yet implemented
    pub listen_sse: bool,
    /// Allow interaction with a Lock server with invalid certificates. Used for testing.
    pub accept_invalid_certs: bool,
}

impl TryFrom<&BootstrapConfigRaw> for LockServiceConfig {
    type Error = BootstrapConfigLoadingError;

    fn try_from(raw: &BootstrapConfigRaw) -> Result<Self, Self::Error> {
        let config_uri = raw
            .lock_server_configuration_uri
            .clone()
            .ok_or(BootstrapConfigLoadingError::MissingLockServerConfigUri)?
            .parse()?;

        let ssa_jwt = raw.lock_ssa_jwt.clone();

        let log_interval =
            (raw.audit_log_interval > 0).then(|| Duration::from_secs(raw.audit_log_interval));
        let health_interval =
            (raw.audit_health_interval > 0).then(|| Duration::from_secs(raw.audit_health_interval));
        let telemetry_interval = (raw.audit_telemetry_interval > 0)
            .then(|| Duration::from_secs(raw.audit_telemetry_interval));

        let listen_sse = raw.listen_sse.into();

        Ok(LockServiceConfig {
            config_uri,
            dynamic_config: raw.dynamic_configuration.into(),
            ssa_jwt,
            log_interval,
            health_interval,
            telemetry_interval,
            listen_sse,
            log_level: raw.log_level,
            accept_invalid_certs: raw.accept_invalid_certs.into(),
        })
    }
}

/// Raw log config
pub struct LogConfigRaw {
    /// Log type
    pub log_type: String,
    /// Log level
    pub log_level: String,
}

/// Raw memory log config
pub struct MemoryLogConfigRaw {
    /// Log TTL
    pub log_ttl: u64,
    /// Max item size
    pub max_item_size: Option<usize>,
    /// Max items
    pub max_items: Option<usize>,
}

impl From<LogConfigRaw> for LogConfig {
    fn from(raw: LogConfigRaw) -> Self {
        Self {
            log_type: match raw.log_type.as_str() {
                "off" => LogTypeConfig::Off,
                "memory" => LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_item_size: None,
                    max_items: None,
                }),
                "stdout" => LogTypeConfig::StdOut,
                _ => LogTypeConfig::StdOut,
            },
            log_level: match raw.log_level.as_str() {
                "TRACE" => LogLevel::TRACE,
                "DEBUG" => LogLevel::DEBUG,
                "INFO" => LogLevel::INFO,
                "WARN" => LogLevel::WARN,
                "ERROR" => LogLevel::ERROR,
                _ => LogLevel::INFO,
            },
        }
    }
}
