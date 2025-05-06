// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This is the bootstrap config module

mod authz_config;
mod config_builder;
mod entity_mapping_config;
mod jwt_validation_config;
mod lock_config;
mod logging_config;
mod policy_store_config;

pub use authz_config::*;
pub use config_builder::*;
pub use entity_mapping_config::*;
pub use jwt_validation_config::*;
pub use lock_config::*;
pub use logging_config::*;
pub use policy_store_config::*;

use config::{self, FileFormat};
use derive_more::Display;
use serde::de;
use serde::{Deserialize, Deserializer, Serialize};
use thiserror::Error;

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

const DEFAULT_CONFIG: &str = include_str!("./default_config.json");

impl Config {
    /// Loads the configuration using default values and environment variables.
    ///
    /// This method initializes the configuration by first loading a built-in default
    /// JSON config and then layering environment variables on top.
    ///
    /// Environment variables must be prefixed with `CEDARLING_`, and nested fields
    /// should use underscores (`_`) as separators.
    ///
    /// A list of available environment varibles can be found in [the docs](https://docs.jans.io/v1.5.0/cedarling/cedarling-properties/).
    pub fn load_with_defaults() -> Result<Self, config::ConfigError> {
        const ENV_PREFIX: &str = "CEDARLING";

        let config = config::Config::builder()
            .add_source(config::File::from_str(DEFAULT_CONFIG, FileFormat::Json))
            .add_source(config::Environment::with_prefix(ENV_PREFIX).prefix_separator("_"))
            .build()?;

        config.try_deserialize::<Config>()
    }

    /// Loads the configuration with default values and overrides from a test-specific
    /// config.
    ///
    /// Intended for use in tests, this method loads the built-in default JSON config
    /// and overlays it with an additional configuration provided via the `extra_config`
    /// parameter.
    #[cfg(test)]
    pub fn load_with_test_overrides(
        overrides: serde_json::Value,
    ) -> Result<Self, config::ConfigError> {
        let overrides = overrides.to_string();

        let config = config::Config::builder()
            .add_source(config::File::from_str(DEFAULT_CONFIG, FileFormat::Json))
            .add_source(config::File::from_str(&overrides, FileFormat::Json))
            .build()?;

        config.try_deserialize::<Config>()
    }
}

#[derive(Debug, Error)]
#[allow(missing_docs)]
pub enum ConfigError {
    #[error("the config is missing the required application name")]
    MissingApplicationName,
    #[error("the config is missing the required policy source")]
    MissingPolicySource,
    #[error("the config is missing the required lock server configuration uri")]
    MissingLockConfigUri,
}

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;
    use test_utils::assert_eq;

    static CONFIG_STR: &str = include_str!("../../../test_files/bootstrap_props_new.json");
    static CONFIG_STR_OLD: &str = include_str!("../../../test_files/bootstrap_props_old.json");

    #[test]
    fn test_builder_has_same_defaults() {
        let default_config = Config::load_with_test_overrides(json!({
            "application_name": "my_cedarling_app",
            "policy_store_uri": "https://test.com/policy_store.json"
        }))
        .expect("load default config");

        let config_from_builder = Config::builder()
            .application_name("my_cedarling_app".to_string())
            .policy_store_src(PolicyStoreSource::Url(
                "https://test.com/policy_store.json".parse().unwrap(),
            ))
            .build()
            .expect("build config");

        assert_eq!(default_config, config_from_builder);
    }

    #[test]
    fn test_backward_compatibility() {
        let old = serde_json::from_str::<Config>(CONFIG_STR_OLD).expect("deserialize old config");
        let new = serde_json::from_str::<Config>(CONFIG_STR).expect("deserialize new config");

        assert_eq!(old, new);
    }
}
