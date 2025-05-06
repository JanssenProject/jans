// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use derive_more::derive::Deref;
use serde::{Deserialize, Serialize, de};
use url::Url;

/// Config specific to the lock server integration
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct LockConfig {
    /// Enables Lock server integration features.
    #[serde(rename = "lock", alias = "CEDARLING_LOCK", default)]
    pub enabled: FeatureToggle,

    /// URI where Cedarling can fetch required metadata about Lock server, e.g. the `.well-known/lock-master-configuration` endpoint.
    ///
    /// This is ***REQUIRED*** if [`lock`] is set to [`Enabled`].
    ///
    /// [`lock`]: Self::lock
    /// [`Enabled`]: FeatureToggle::Enabled
    #[serde(
        rename = "lock_server_configuration_uri",
        alias = "CEDARLING_LOCK_SERVER_CONFIGURATION_URI",
        default
    )]
    pub server_config_uri: Option<UrlWrapper>,

    /// Enables listening for SSE config updates from the Lock server.
    #[serde(
        rename = "lock_dynamic_configuration",
        alias = "CEDARLING_LOCK_DYNAMIC_CONFIGURATION",
        default
    )]
    pub dynamic_configuration: FeatureToggle,

    /// The Software Statement Assertion (SSA) JSON Web Token (JWT) that will be used
    /// for Dynamic Client Registration (DCR) for the Lock server.
    #[serde(rename = "lock_ssa_jwt", alias = "CEDARLING_LOCK_SSA_JWT", default)]
    pub ssa_jwt: Option<String>,

    /// Interval, in seconds, at which log messages are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(
        rename = "lock_log_interval",
        alias = "CEDARLING_LOCK_LOG_INTERVAL",
        default
    )]
    pub log_interval: u64,

    /// Interval, in seconds, at which health messages are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(
        rename = "lock_health_interval",
        alias = "CEDARLING_LOCK_HEALTH_INTERVAL",
        default
    )]
    pub health_interval: u64,

    /// Interval, in seconds, at which telemetry are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(
        rename = "lock_telemetry_interval",
        alias = "CEDARLING_LOCK_TELEMETRY_INTERVAL ",
        default
    )]
    pub telemetry_interval: u64,

    /// Enables listening for updates from the Lock Server.
    #[serde(
        rename = "lock_listen_sse",
        alias = "CEDARLING_LOCK_LISTEN_SSE",
        default
    )]
    pub listen_sse: FeatureToggle,
}

impl Default for LockConfig {
    fn default() -> Self {
        Self {
            enabled: FeatureToggle::Disabled,
            server_config_uri: None,
            dynamic_configuration: FeatureToggle::Enabled,
            ssa_jwt: None,
            log_interval: 60,
            health_interval: 60,
            telemetry_interval: 60,
            listen_sse: FeatureToggle::Enabled,
        }
    }
}

impl LockConfig {
    /// Checks if the [`LockConfig`] is a valid configuration
    pub fn validate(&self) -> Result<(), ConfigError> {
        if matches!(self.enabled, FeatureToggle::Disabled) {
            return Ok(());
        }

        if self.server_config_uri.is_none() {
            return Err(ConfigError::MissingLockConfigUri);
        }

        Ok(())
    }
}

/// A wrapper over [`url::Url`] that implements [`Deserialize`]
#[derive(Debug, Deref, PartialEq)]
pub struct UrlWrapper(pub Url);

impl<'de> Deserialize<'de> for UrlWrapper {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let url = String::deserialize(deserializer)?;
        let url = url
            .parse()
            .map_err(|e| de::Error::custom(format!("invalid url: {e}")))?;
        Ok(Self(url))
    }
}

impl Serialize for UrlWrapper {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_str(&self.0.to_string())
    }
}
