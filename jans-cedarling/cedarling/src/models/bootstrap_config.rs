/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::jwt_config::JwtConfig;
use super::log_config::LogConfig;
use super::policy_store_config::PolicyStoreConfig;

/// Bootstrap configuration
/// properties for configuration `Cedarling` application.
/// [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) to the documentation.
pub struct BootstrapConfig {
    /// `CEDARLING_APPLICATION_NAME` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub application_name: String,
    /// A set of properties used to configure logging in the `Cedarling` application.
    pub log_config: LogConfig,
    /// A set of properties used to load `PolicyStore` in the `Cedarling` application.
    pub policy_store_config: PolicyStoreConfig,
    /// A set of properties used to configure JWT in the `Cedarling` application.
    pub jwt_config: JwtConfig,
}
