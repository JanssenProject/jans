/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! Module for bootstrap configuration types
//! to configure [`Cedarling`](crate::Cedarling)

pub(crate) mod jwt_config;
pub(crate) mod log_config;
pub(crate) mod policy_store_config;

// reimport to useful import values in root module
pub use jwt_config::*;
pub use log_config::*;
pub use policy_store_config::*;
mod decode;

/// Bootstrap configuration
/// properties for configuration [`Cedarling`](crate::Cedarling) application.
/// [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) to the documentation.
#[derive(Debug, PartialEq)]
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

#[cfg(test)]
mod test {
    use crate::{BootstrapConfig, LogConfig, LogTypeConfig, MemoryLogConfig, PolicyStoreConfig};
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize_from_json() {
        let config_json = include_str!("../../../test_files/bootstrap_jwt_disabled.json");
        let deserialized = serde_json::from_str::<BootstrapConfig>(config_json)
            .expect("Should deserialize bootstrap config from JSON");

        let expected = BootstrapConfig {
            application_name: "My App".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig { log_ttl: 604800 }),
            },
            policy_store_config: PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileJson(
                    "../test_files/policy-store_blobby.json".to_string(),
                ),
            },
            jwt_config: crate::JwtConfig::Enabled {
                signature_algorithms: vec!["HS256".to_string(), "RS256".to_string()],
            },
        };

        assert_eq!(deserialized, expected);
    }

    #[test]
    fn can_deserialize_from_yaml() {
        let config_json = include_str!("../../../test_files/bootstrap_jwt_disabled.yaml");
        let deserialized = serde_yml::from_str::<BootstrapConfig>(config_json)
            .expect("Should deserialize bootstrap config from YAML");

        let expected = BootstrapConfig {
            application_name: "My App".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig { log_ttl: 604800 }),
            },
            policy_store_config: PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileJson(
                    "../test_files/policy-store_blobby.json".to_string(),
                ),
            },
            jwt_config: crate::JwtConfig::Enabled {
                signature_algorithms: vec!["HS256".to_string(), "RS256".to_string()],
            },
        };

        assert_eq!(deserialized, expected);
    }
}
