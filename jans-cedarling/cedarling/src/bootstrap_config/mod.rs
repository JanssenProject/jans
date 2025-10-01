// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Module for bootstrap configuration types
//! to configure [`Cedarling`](crate::Cedarling)

mod decode;

/// Authorization config module
pub mod authorization_config;
/// Entity builder config module
pub mod entity_builder_config;
/// JWT config module
pub mod jwt_config;
/// Lock config module
pub mod lock_config;
/// Log config module
pub mod log_config;
/// Policy store config module
pub mod policy_store_config;
/// Raw config module
pub mod raw_config;

#[cfg(not(target_arch = "wasm32"))]
use std::{io, path::Path};

use config::{Config, File};

// Re-export types that need to be public
pub use authorization_config::{AuthorizationConfig, AuthorizationConfigRaw, IdTokenTrustMode};
pub use entity_builder_config::{
    EntityBuilderConfig, EntityBuilderConfigRaw, EntityNames, UnsignedRoleIdSrc,
};
pub use jwt_config::{JwtConfig, JwtConfigRaw};
pub use lock_config::{LockServiceConfig, LockServiceConfigRaw};
pub use log_config::{LogConfig, LogConfigRaw, LogTypeConfig, MemoryLogConfig};
pub use policy_store_config::{PolicyStoreConfig, PolicyStoreConfigRaw, PolicyStoreSource};
pub use raw_config::{BootstrapConfigRaw, FeatureToggle};

/// Bootstrap configuration
/// properties for configuration [`Cedarling`](crate::Cedarling) application.
/// [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) to the documentation.
#[derive(Debug, Clone, PartialEq)]
pub struct BootstrapConfig {
    /// `CEDARLING_APPLICATION_NAME` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    pub application_name: String,
    /// A set of properties used to configure logging in the `Cedarling` application.
    pub log_config: LogConfig,
    /// A set of properties used to load `PolicyStore` in the `Cedarling` application.
    pub policy_store_config: PolicyStoreConfig,
    /// A set of properties used to configure JWT in the `Cedarling` application.
    pub jwt_config: JwtConfig,
    /// A set of properties used to configure authorization workflow in the `Cedarling` application.
    pub authorization_config: AuthorizationConfig,
    /// A set of properties used to configure the JWTs to Cedar Entity mappings
    pub entity_builder_config: EntityBuilderConfig,
    /// Lock service configuration.
    /// If `None` then lock service is disabled.
    pub lock_config: Option<LockServiceConfig>,
    /// Maximum number of default entities allowed in a policy store.
    /// This prevents DoS attacks by limiting the number of entities that can be loaded.
    pub max_default_entities: Option<usize>,
    /// Maximum size of base64-encoded default entity strings in bytes.
    /// This prevents memory exhaustion attacks from extremely large base64 strings.
    pub max_base64_size: Option<usize>,
    /// Allows to limit maximum token cache TTL in seconds.
    /// Zero means no token cache TTL limit.
    pub token_cache_max_ttl_secs: usize,
}

impl BootstrapConfig {
    /// Loads a `BootstrapConfig` from a file.
    ///
    /// The file format is determined based on its extension:
    /// - `.json`: Parses the file as JSON.
    /// - `.yaml` or `.yml`: Parses the file as YAML.
    /// - `.toml`: Parses the file as TOML.
    ///
    /// # Example
    ///
    /// ```rust
    /// use cedarling::BootstrapConfig;
    ///
    /// let config = BootstrapConfig::load_from_file("../test_files/bootstrap_props.json").unwrap();
    /// ```
    #[cfg(not(target_arch = "wasm32"))]
    pub fn load_from_file<P: AsRef<Path>>(path: P) -> Result<Self, BootstrapConfigLoadingError> {
        let config = Config::builder()
            .add_source(File::from(path.as_ref()))
            .build()
            .map_err(|e| BootstrapConfigLoadingError::DecodingJSON(e.to_string()))?;

        let raw: BootstrapConfigRaw = config
            .try_deserialize()
            .map_err(|e| BootstrapConfigLoadingError::DecodingJSON(e.to_string()))?;
        raw.try_into()
    }

    /// Loads a `BootstrapConfig` from a JSON string
    pub fn load_from_json(json: &str) -> Result<Self, BootstrapConfigLoadingError> {
        let raw: BootstrapConfigRaw = serde_json::from_str(json)
            .map_err(|e| BootstrapConfigLoadingError::DecodingJSON(e.to_string()))?;
        raw.try_into()
    }

    /// Load config from environment variables.
    /// If you need with fallback to applied config use [`Self::from_raw_config_and_env`].
    #[cfg(not(target_arch = "wasm32"))]
    pub fn from_env() -> Result<Self, BootstrapConfigLoadingError> {
        let config = Config::builder()
            .add_source(config::Environment::with_prefix("CEDARLING"))
            .build()
            .map_err(|e| BootstrapConfigLoadingError::DecodingJSON(e.to_string()))?;

        let raw: BootstrapConfigRaw = config
            .try_deserialize()
            .map_err(|e| BootstrapConfigLoadingError::DecodingJSON(e.to_string()))?;

        raw.try_into()
    }

    /// Loads the default configuration bundled with the library.
    /// This configuration provides sensible defaults for all components.
    ///
    /// # Example
    ///
    /// ```rust
    /// use cedarling::BootstrapConfig;
    ///
    /// let config = BootstrapConfig::load_default().unwrap();
    /// ```
    pub fn load_default() -> Result<Self, BootstrapConfigLoadingError> {
        const DEFAULT_CONFIG: &str = include_str!("../../config/default_config.yaml");

        let config = Config::builder()
            .add_source(File::from_str(DEFAULT_CONFIG, config::FileFormat::Yaml))
            .build()
            .map_err(|e| BootstrapConfigLoadingError::DecodingYAML(e.to_string()))?;

        let raw: BootstrapConfigRaw = config
            .try_deserialize()
            .map_err(|e| BootstrapConfigLoadingError::DecodingYAML(e.to_string()))?;

        raw.try_into()
    }
}

impl TryFrom<BootstrapConfigRaw> for BootstrapConfig {
    type Error = BootstrapConfigLoadingError;

    fn try_from(raw: BootstrapConfigRaw) -> Result<Self, Self::Error> {
        Self::from_raw_config(&raw)
    }
}

/// Represents errors that may occur while loading a `BootstrapConfig` from a file.
#[derive(Debug, thiserror::Error)]
pub enum BootstrapConfigLoadingError {
    /// Error returned when the file format is unsupported.
    ///
    /// Supported formats include:
    /// - `.json`
    /// - `.yaml` or `.yml`
    /// - `.toml`
    #[cfg(not(target_arch = "wasm32"))]
    #[error(
        "Unsupported bootstrap config file format for: {0}. Supported formats include: JSON, YAML, TOML"
    )]
    InvalidFileFormat(String),

    /// Error returned when the file cannot be read.
    #[cfg(not(target_arch = "wasm32"))]
    #[error("Failed to read {0}: {1}")]
    ReadFile(String, io::Error),

    /// Error returned when parsing the file as JSON fails.
    #[error("Failed to decode JSON string into BootstrapConfig: {0}")]
    DecodingJSON(String),

    /// Error returned when parsing the file as YAML fails.
    #[error("Failed to decode YAML string into BootstrapConfig: {0}")]
    DecodingYAML(String),

    /// Error returned when parsing the file as TOML fails.
    #[error("Failed to decode TOML string into BootstrapConfig: {0}")]
    DecodingTOML(String),

    /// Error returned when the boostrap property `CEDARLING_LOG_TTL` is missing.
    #[error(
        "Missing bootstrap property: `CEDARLING_LOG_TTL`. This property is required if \
         `CEDARLING_LOG_TYPE` is set to Memory."
    )]
    MissingLogTTL,

    /// Error returned when multiple policy store sources were provided.
    #[error(
        "Multiple store options were provided. Make sure you only one of these properties is set: \
         `CEDARLING_POLICY_STORE_URI` or `CEDARLING_POLICY_STORE_LOCAL`"
    )]
    ConflictingPolicyStores,

    /// Error returned when no policy store source was provided.
    #[error("No Policy store was provided.")]
    MissingPolicyStore,

    /// Error returned when the policy store file is in an unsupported format.
    #[error("Unsupported policy store file format for: {0}. Supported formats include: JSON, YAML")]
    UnsupportedPolicyStoreFileFormat(String),

    /// Error returned when failing to load a local JWKS
    #[error("Failed to load local JWKS from {0}: {1}")]
    LoadLocalJwks(String, String),

    /// Error returned when both `CEDARLING_USER_AUTHZ` and `CEDARLING_WORKLOAD_AUTHZ` are disabled.
    /// These two authentication configurations cannot be disabled at the same time.
    #[error(
        "Both `CEDARLING_USER_AUTHZ` and `CEDARLING_WORKLOAD_AUTHZ` cannot be disabled \
         simultaneously."
    )]
    BothPrincipalsDisabled,

    /// Error returned when `CEDARLING_LOCK` is set to `enabled` but `CEDARLING_LOCK_SERVER_CONFIGURATION_URI` is not set.
    #[error(
        "the `CEDARLING_LOCK` is set to `enabled` but `CEDARLING_LOCK_SERVER_CONFIGURATION_URI` is not set."
    )]
    MissingLockServerConfigUri,

    /// Error returned when the lock server configuration URI is invalid.
    #[error("Invalid lock server configuration URI: {0}")]
    InvalidLockServerConfigUri(url::ParseError),
}

impl From<url::ParseError> for BootstrapConfigLoadingError {
    fn from(err: url::ParseError) -> Self {
        BootstrapConfigLoadingError::InvalidLockServerConfigUri(err)
    }
}

impl From<serde_json::Error> for BootstrapConfigLoadingError {
    fn from(err: serde_json::Error) -> Self {
        Self::DecodingJSON(err.to_string())
    }
}

impl From<std::convert::Infallible> for BootstrapConfigLoadingError {
    fn from(_: std::convert::Infallible) -> Self {
        unreachable!("Infallible cannot be instantiated")
    }
}

#[cfg(test)]
mod tests {
    use jsonwebtoken::Algorithm;

    use crate::LogLevel;

    use super::*;

    #[test]
    fn test_load_default_config() {
        let config = BootstrapConfig::load_default().unwrap();

        // Verify basic configuration
        assert_eq!(config.application_name, "My App");

        // Verify log configuration
        assert!(matches!(
            config.log_config.log_type,
            LogTypeConfig::Memory(_)
        ));
        assert_eq!(config.log_config.log_level, LogLevel::DEBUG);

        // Verify policy store configuration
        assert!(matches!(
            config.policy_store_config.source,
            PolicyStoreSource::FileJson(_)
        ));

        // Verify JWT configuration
        assert!(config.jwt_config.jwt_sig_validation);
        assert!(!config.jwt_config.jwt_status_validation);
        assert!(
            config
                .jwt_config
                .signature_algorithms_supported
                .contains(&Algorithm::HS256)
        );
        assert!(
            config
                .jwt_config
                .signature_algorithms_supported
                .contains(&Algorithm::RS256)
        );

        // Verify authorization configuration
        assert!(config.authorization_config.use_user_principal);
        assert!(config.authorization_config.use_workload_principal);
        assert_eq!(
            config.authorization_config.decision_log_default_jwt_id,
            "jti"
        );

        // Verify entity builder configuration
        assert!(config.entity_builder_config.build_user);
        assert!(config.entity_builder_config.build_workload);
    }
}
