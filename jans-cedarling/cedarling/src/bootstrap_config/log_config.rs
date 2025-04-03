// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{BootstrapConfigLoadingError, BootstrapConfigRaw};
use crate::log::LogLevel;
use std::time::Duration;
use url::Url;

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
    /// The logger sends log data to the server.
    Lock(LockLogConfig),
}

/// Configuration for memory log.
#[derive(Debug, Clone, Copy, PartialEq)]
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
pub struct LockLogConfig {
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
    pub ssa_jwt: Box<str>,
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
}

impl TryFrom<&BootstrapConfigRaw> for LockLogConfig {
    type Error = BootstrapConfigLoadingError;

    fn try_from(raw: &BootstrapConfigRaw) -> Result<Self, Self::Error> {
        let config_uri = raw
            .lock_server_configuration_uri
            .clone()
            .ok_or(BootstrapConfigLoadingError::MissingLockServerConfigUri)?
            .parse()?;

        // TODO: validate this JWT
        let ssa_jwt = raw
            .lock_ssa_jwt
            .clone()
            .ok_or(BootstrapConfigLoadingError::MissingSsaJwt)?
            .into_boxed_str();

        let log_interval =
            (raw.audit_log_interval > 0).then(|| Duration::from_secs(raw.audit_log_interval));
        let health_interval =
            (raw.audit_health_interval > 0).then(|| Duration::from_secs(raw.audit_health_interval));
        let telemetry_interval = (raw.audit_telemetry_interval > 0)
            .then(|| Duration::from_secs(raw.audit_telemetry_interval));

        let listen_sse = raw.listen_sse.into();

        Ok(LockLogConfig {
            config_uri,
            dynamic_config: raw.dynamic_configuration.into(),
            ssa_jwt,
            log_interval,
            health_interval,
            telemetry_interval,
            listen_sse,
        })
    }
}
