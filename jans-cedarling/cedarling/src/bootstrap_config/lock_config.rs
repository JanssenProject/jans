// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

/// Bootstrap configuration properties related to the Lock Server
#[derive(Debug, PartialEq, Default)]
pub struct LockConfig {
    /// Connect to the Lock Master for policies, and subscribe for SSE events
    pub enabled: bool,
    /// URI where Cedarling can get JSON file with all required metadata about
    /// Lock Master, i.e. `.well-known/lock-master-configuration.`
    pub lock_master_config_uri: Option<String>,
    /// Controls whether cedarling should listen for SSE config updates
    pub dynamic_config: bool,
    /// Software Statement Assertion (SSA) for Dynamic Client Registration
    /// (DCR) in a Lock Master deployment. The Cedarling will validate this
    /// SSA JWT prior to DCR.
    pub ssa_jwt: Option<String>,
    /// How often to send log messages to Lock Master in seconds. Set this to
    /// `0` to turn off trasmission.
    pub log_interval: u64,
    /// How often to send health messages to Lock Master in seconds. Set this to
    /// `0` to turn off trasmission.
    pub health_interval: u64,
    /// How often to send telemetry messages to Lock Master in seconds. Set this to
    /// `0` to turn off trasmission.
    pub telemetry_interval: u64,
    /// Controls whether Cedarling should listen for updates from the Lock Server.
    pub listen_sse: bool,
}
