// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use derive_more::derive::Deref;
use serde::{Deserialize, Serialize, de};

/// Config specific to the lock server integration
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct LockConfig {
    /// Enables Lock server integration features.
    #[serde(alias = "CEDARLING_LOCK", default)]
    pub lock: FeatureToggle,

    /// URI where Cedarling can fetch required metadata about Lock server, e.g. the `.well-known/lock-master-configuration` endpoint.
    ///
    /// This is ***REQUIRED*** if [`lock`] is set to [`Enabled`].
    ///
    /// [`lock`]: Self::lock
    /// [`Enabled`]: FeatureToggle::Enabled
    #[serde(alias = "CEDARLING_LOCK_SERVER_CONFIGURATION_URI", default)]
    pub lock_server_configuration_uri: Option<UrlWrapper>,

    /// Enables listening for SSE config updates from the Lock server.
    #[serde(alias = "CEDARLING_LOCK_DYNAMIC_CONFIGURATION", default)]
    pub lock_dynamic_configuration: FeatureToggle,

    /// The Software Statement Assertion (SSA) JSON Web Token (JWT) that will be used
    /// for Dynamic Client Registration (DCR) for the Lock server.
    #[serde(alias = "CEDARLING_LOCK_SSA_JWT", default)]
    pub lock_ssa_jwt: Option<String>,

    /// Interval, in seconds, at which log messages are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(alias = "CEDARLING_LOCK_LOG_INTERVAL", default)]
    pub lock_log_interval: u64,

    /// Interval, in seconds, at which health messages are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(alias = "CEDARLING_LOCK_HEALTH_INTERVAL", default)]
    pub lock_health_interval: u64,

    /// Interval, in seconds, at which telemetry are sent to the Lock Server.
    ///
    /// A value of `0` disables transmission entirely.
    #[serde(alias = "CEDARLING_LOCK_TELEMETRY_INTERVAL ", default)]
    pub lock_telemetry_interval: u64,

    /// Enables listening for updates from the Lock Server.
    #[serde(alias = "CEDARLING_LOCK_LISTEN_SSE", default)]
    pub lock_listen_sse: FeatureToggle,
}

/// A wrapper over [`url::Url`] that implements [`Deserialize`]
#[derive(Debug, Deref, PartialEq)]
pub struct UrlWrapper(url::Url);

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
