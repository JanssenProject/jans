// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This is the bootstrap config module

mod authz_config;
mod entity_mapping_config;
mod jwt_validation_config;
mod lock_config;
mod logging_config;
mod policy_store_config;

pub use authz_config::*;
pub use entity_mapping_config::*;
pub use jwt_validation_config::*;
pub use lock_config::*;
pub use logging_config::*;
pub use policy_store_config::*;

use config::{self, FileFormat};
use derive_more::Display;
use serde::de;
use serde::{Deserialize, Deserializer, Serialize};

#[derive(Debug, Deserialize, Serialize, PartialEq)]
#[serde(rename_all = "lowercase")]
#[allow(missing_docs)]
pub struct Config {
    /// Human friendly identifier for the app. Used for logging.
    #[serde(alias = "CEDARLING_APPLICATION_NAME")]
    pub application_name: AppName,
    #[serde(flatten)]
    pub policy_store: PolicyStoreConfig,
    #[serde(flatten)]
    pub authz: AuthzConfig,
    #[serde(flatten)]
    pub entity_mapping: EntityMappingConfig,
    #[serde(flatten)]
    pub logging: LoggingConfig,
    #[serde(flatten)]
    pub jwt_validation: JwtValidationConfig,
    #[serde(flatten)]
    pub lock: LockConfig,
}

/// The name of the application that will be used for log entries
#[derive(Debug, Serialize, PartialEq, Display)]
pub struct AppName(pub String);

impl<'de> Deserialize<'de> for AppName {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let value = Option::<String>::deserialize(deserializer)?;
        let app_name = value.ok_or_else(|| {
            de::Error::custom(
                "an application name was not provided. please set the `CEDARLING_APPLICATION_NAME` environment variable",
            )
        })?;
        Ok(Self(app_name))
    }
}

#[derive(Debug, PartialEq, Deserialize, Serialize, Default)]
#[serde(rename_all = "lowercase")]
#[allow(missing_docs)]
pub enum FeatureToggle {
    #[default]
    Disabled,
    Enabled,
}

impl Config {
    /// Loads the config using default values and environment variables.
    pub fn load_with_defaults() -> Result<Self, config::ConfigError> {
        const ENV_PREFIX: &str = "CEDARLING";
        const DEFAULT_CONFIG: &str = include_str!("./default_config.json");

        let config = config::Config::builder()
            .add_source(config::File::from_str(DEFAULT_CONFIG, FileFormat::Json))
            .add_source(config::Environment::with_prefix(ENV_PREFIX).prefix_separator("_"))
            .build()?;

        config.try_deserialize::<Config>()
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use test_utils::assert_eq;

    #[test]
    fn test_backward_compatibility() {
        let config_str_old = include_str!("../../../test_files/bootstrap_props_old.json");
        let config_str_new = include_str!("../../../test_files/bootstrap_props_new.json");

        let old = serde_json::from_str::<Config>(config_str_old).unwrap();
        let new = serde_json::from_str::<Config>(config_str_new).unwrap();

        assert_eq!(old, new);
    }
}
