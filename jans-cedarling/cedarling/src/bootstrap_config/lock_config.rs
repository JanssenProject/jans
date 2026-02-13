use crate::log::LogLevel;
use crate::{BootstrapConfigLoadingError, BootstrapConfigRaw};
use serde::{Deserialize, Serialize};
use std::time::Duration;
use url::Url;

/// Transport protocol for Lock Server communication
#[derive(Debug, Default, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
pub enum LockTransport {
    /// REST/HTTP transport
    #[default]
    Rest,
    /// gRPC transport (default when `grpc` feature is enabled)
    #[cfg(feature = "grpc")]
    Grpc,
}

/// Lock service config
#[derive(Debug, Clone, PartialEq)]
pub struct LockServiceConfig {
    /// The logging level
    pub log_level: LogLevel,
    /// URI where Cedarling can get metadata about the lock server.
    /// i.e. `.well-known/lock-server-configuration`
    pub config_uri: Url,
    /// Toggles whether Cedarling should listen for SSE config updates.
    pub dynamic_config: bool,
    /// Software Statement Assertion Json Web Token that Cedarling will use for Dynamic
    /// Client Registration.
    pub ssa_jwt: Option<String>,
    /// Intervals to send log messages to the lock server.
    /// Set this to [`None`] to disable transmission.
    pub log_interval: Option<Duration>,
    /// Intervals to send health messages to the lock server.
    /// Set this to [`None`] to disable transmission.
    pub health_interval: Option<Duration>,
    /// Intervals to send telemetry messages to the lock server.
    /// Set this to [`None`] to disable transmission.
    pub telemetry_interval: Option<Duration>,
    /// Controls whether Cedarling should listen for updates from the Lock Server.
    pub listen_sse: bool,
    /// Allow interaction with a Lock server with invalid certificates. Used for testing.
    pub accept_invalid_certs: bool,
    /// Transport protocol to use for Lock Server communication
    pub transport: LockTransport,
    /// gRPC endpoint to use for Lock Server communication
    pub grpc_endpoint: Option<String>,
}

/// Raw lock service config
#[derive(Debug, Clone, PartialEq)]
pub struct LockServiceConfigRaw {
    /// The logging level
    pub log_level: LogLevel,
    /// Config URI
    pub config_uri: String,
    /// Dynamic config
    pub dynamic_config: bool,
    /// SSA JWT
    pub ssa_jwt: Option<String>,
    /// Log interval
    pub log_interval: Option<Duration>,
    /// Health interval
    pub health_interval: Option<Duration>,
    /// Telemetry interval
    pub telemetry_interval: Option<Duration>,
    /// Listen SSE
    pub listen_sse: bool,
    /// Accept invalid certs
    pub accept_invalid_certs: bool,
    /// Transport protocol
    pub transport: LockTransport,
    /// gRPC endpoint
    pub grpc_endpoint: Option<String>,
}

impl Default for LockServiceConfig {
    fn default() -> Self {
        Self {
            log_level: LogLevel::INFO,
            config_uri: "http://localhost:8080/.well-known/lock-server-configuration"
                .parse()
                .expect("Failed to parse default lock server configuration URI"),
            dynamic_config: false,
            ssa_jwt: None,
            log_interval: None,
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            accept_invalid_certs: false,
            transport: LockTransport::default(),
            grpc_endpoint: None,
        }
    }
}

impl From<LockServiceConfigRaw> for LockServiceConfig {
    fn from(raw: LockServiceConfigRaw) -> Self {
        Self {
            log_level: raw.log_level,
            config_uri: raw
                .config_uri
                .parse()
                .expect("Failed to parse lock server configuration URI from raw config"),
            dynamic_config: raw.dynamic_config,
            ssa_jwt: raw.ssa_jwt,
            log_interval: raw.log_interval,
            health_interval: raw.health_interval,
            telemetry_interval: raw.telemetry_interval,
            listen_sse: raw.listen_sse,
            accept_invalid_certs: raw.accept_invalid_certs,
            transport: raw.transport,
            grpc_endpoint: raw.grpc_endpoint,
        }
    }
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
        let grpc_endpoint = raw.grpc_endpoint.clone();

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
            transport: raw.lock_transport,
            grpc_endpoint,
        })
    }
}
