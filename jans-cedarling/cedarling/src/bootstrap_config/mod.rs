/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! Module for bootstrap configuration types
//! to configure [`Cedarling`](crate::Cedarling)

pub(crate) mod authorization_config;
pub(crate) mod jwt_config;
pub(crate) mod log_config;
pub(crate) mod policy_store_config;

use std::{fs, io, path::Path};

pub use authorization_config::AuthorizationConfig;
// reimport to useful import values in root module
pub use jwt_config::*;
pub use log_config::*;
pub use policy_store_config::*;
mod decode;
pub use decode::{BootstrapConfigRaw, FeatureToggle, LoggerType, WorkloadBoolOp};

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
    /// A set of properties used to configure authorization workflow in the `Cedarling` application.
    pub authorization_config: AuthorizationConfig,
}

impl BootstrapConfig {
    /// Loads a `BootstrapConfig` from a file.
    ///
    /// The file format is determined based on its extension:
    /// - `.json`: Parses the file as JSON.
    /// - `.yaml` or `.yml`: Parses the file as YAML.
    ///
    /// # Example
    ///
    /// ```rust
    /// use cedarling::BootstrapConfig;
    ///
    /// let config =
    ///     BootstrapConfig::load_from_file("../test_files/bootstrap_props.json")
    ///         .unwrap();
    /// ```
    pub fn load_from_file(path: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let file_ext = Path::new(path)
            .extension()
            .and_then(|ext| ext.to_str())
            .map(|x| x.to_lowercase());
        let config = match file_ext.as_deref() {
            Some("json") => {
                let config_json = fs::read_to_string(path)
                    .map_err(|e| BootstrapConfigLoadingError::ReadFile(path.to_string(), e))?;
                let raw = serde_json::from_str::<decode::BootstrapConfigRaw>(&config_json)?;
                BootstrapConfig::from_raw_config(&raw)?
            },
            Some("yaml") | Some("yml") => {
                let config_json = fs::read_to_string(path)
                    .map_err(|e| BootstrapConfigLoadingError::ReadFile(path.to_string(), e))?;
                let raw = serde_yml::from_str::<decode::BootstrapConfigRaw>(&config_json)?;
                BootstrapConfig::from_raw_config(&raw)?
            },
            _ => Err(BootstrapConfigLoadingError::InvalidFileFormat(
                path.to_string(),
            ))?,
        };

        Ok(config)
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
    #[error(
        "Unsupported bootstrap config file format for: {0}. Supported formats include: JSON, YAML"
    )]
    InvalidFileFormat(String),

    /// Error returned when the file cannot be read.
    #[error("Failed to read {0}: {1}")]
    ReadFile(String, io::Error),

    /// Error returned when parsing the file as JSON fails.
    #[error("Failed to decode JSON string into BootstrapConfig: {0}")]
    DecodingJSON(#[from] serde_json::Error),

    /// Error returned when parsing the file as YAML fails.
    #[error("Failed to decode YAML string into BootstrapConfig: {0}")]
    DecodingYAML(#[from] serde_yml::Error),
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::bootstrap_config::decode::BootstrapConfigRaw;
    use std::{collections::HashSet, path::Path};

    use crate::{BootstrapConfig, LogConfig, LogTypeConfig, MemoryLogConfig, PolicyStoreConfig};
    use jsonwebtoken::Algorithm;
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize_from_json() {
        let config_json = include_str!("../../../test_files/bootstrap_props.json");
        let raw = serde_json::from_str::<BootstrapConfigRaw>(config_json).unwrap();
        let deserialized = BootstrapConfig::from_raw_config(&raw)
            .expect("Should deserialize bootstrap config from JSON");

        let expected = BootstrapConfig {
            application_name: "My App".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::StdOut,
            },
            policy_store_config: PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileJson(
                    Path::new("../test_files/policy-store_blobby.json").into(),
                ),
            },
            jwt_config: JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                id_token_trust_mode: IdTokenTrustMode::Strict,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256, Algorithm::RS256]),
                access_token_config: TokenValidationConfig {
                    exp_validation: true,
                    ..Default::default()
                },
                id_token_config: TokenValidationConfig {
                    iss_validation: true,
                    sub_validation: true,
                    exp_validation: true,
                    iat_validation: true,
                    aud_validation: true,
                    ..Default::default()
                },
                userinfo_token_config: TokenValidationConfig {
                    iss_validation: true,
                    sub_validation: true,
                    aud_validation: true,
                    exp_validation: true,
                    ..Default::default()
                },
            },
            authorization_config: AuthorizationConfig {
                use_user_principal: true,
                use_workload_principal: true,
                user_workload_operator: WorkloadBoolOp::And,
            },
        };

        assert_eq!(deserialized, expected);
    }

    #[test]
    fn can_deserialize_from_yaml() {
        let config_yaml = include_str!("../../../test_files/bootstrap_props.yaml");
        let raw = serde_yml::from_str::<BootstrapConfigRaw>(config_yaml).unwrap();
        let deserialized = BootstrapConfig::from_raw_config(&raw)
            .expect("Should deserialize bootstrap config from YAML");

        let expected = BootstrapConfig {
            application_name: "My App".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig { log_ttl: 60 }),
            },
            policy_store_config: PolicyStoreConfig {
                source: crate::PolicyStoreSource::FileJson(
                    Path::new("../test_files/policy-store_blobby.json").into(),
                ),
            },
            jwt_config: JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                id_token_trust_mode: IdTokenTrustMode::Strict,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256, Algorithm::RS256]),
                access_token_config: TokenValidationConfig {
                    exp_validation: true,
                    ..Default::default()
                },
                id_token_config: TokenValidationConfig {
                    iss_validation: true,
                    sub_validation: true,
                    exp_validation: true,
                    iat_validation: true,
                    aud_validation: true,
                    ..Default::default()
                },
                userinfo_token_config: TokenValidationConfig {
                    iss_validation: true,
                    sub_validation: true,
                    aud_validation: true,
                    exp_validation: true,
                    ..Default::default()
                },
            },
            authorization_config: AuthorizationConfig {
                use_user_principal: true,
                use_workload_principal: true,
                user_workload_operator: WorkloadBoolOp::And,
            },
        };

        assert_eq!(deserialized, expected);
    }
}
