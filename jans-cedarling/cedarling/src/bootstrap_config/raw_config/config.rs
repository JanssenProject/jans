// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#[cfg(not(target_arch = "wasm32"))]
use super::super::BootstrapConfigLoadingError;
use super::super::authorization_config::IdTokenTrustMode;
use super::default_values::*;
use super::feature_types::*;
use super::json_util::*;
use crate::common::json_rules::JsonRule;
use crate::log::LogLevel;
use jsonwebtoken::Algorithm;
use serde::{Deserialize, Serialize};
#[cfg(not(target_arch = "wasm32"))]
use std::collections::HashMap;
use std::collections::HashSet;
#[cfg(not(target_arch = "wasm32"))]
use std::env;

#[derive(Deserialize, Serialize, PartialEq, Debug, Default)]
/// Struct that represent mapping mapping `Bootstrap properties` to be JSON and YAML compatible
/// from [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties)
///
/// This structure is used to deserialize values from ENV VARS so json keys is same as keys in environment variables
//
//  All fields should be available to parse from string, because env vars always string.
pub struct BootstrapConfigRaw {
    ///  Human friendly identifier for the application
    #[serde(rename = "CEDARLING_APPLICATION_NAME")]
    pub application_name: String,

    /// Location of policy store JSON, used if policy store is not local, or retreived from Lock Master.
    #[serde(
        rename = "CEDARLING_POLICY_STORE_URI",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub policy_store_uri: Option<String>,

    /// An identifier for the policy store.
    #[serde(rename = "CEDARLING_POLICY_STORE_ID", default)]
    pub policy_store_id: String,

    /// How the Logs will be presented.
    #[serde(rename = "CEDARLING_LOG_TYPE", default)]
    pub log_type: LoggerType,

    /// Log level filter for logging. TRACE is lowest. FATAL is highest.
    #[serde(rename = "CEDARLING_LOG_LEVEL", default)]
    pub log_level: LogLevel,

    /// If `log_type` is set to [`LogType::Memory`], this is the TTL (time to live) of
    /// log entities in seconds.
    #[serde(rename = "CEDARLING_LOG_TTL", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub log_ttl: Option<u64>,

    /// Maximum number of log entities that can be stored using [`LogType::Memory`].
    /// If value is 0, there is no limit. But if None, default value is applied.
    #[serde(rename = "CEDARLING_LOG_MAX_ITEMS", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub log_max_items: Option<usize>,

    /// Maximum size of a single log entity in bytes using [`LogType::Memory`].
    /// If value is 0, there is no limit. But if None, default value is applied.
    #[serde(rename = "CEDARLING_LOG_MAX_ITEM_SIZE", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub log_max_item_size: Option<usize>,

    /// List of claims to map from user entity, such as ["sub", "email", "username", ...]
    #[serde(rename = "CEDARLING_DECISION_LOG_USER_CLAIMS", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub decision_log_user_claims: Vec<String>,

    /// List of claims to map from user entity, such as ["client_id", "rp_id", ...]
    #[serde(rename = "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub decision_log_workload_claims: Vec<String>,

    /// Token claims that will be used for decision logging.
    /// Default is jti, but perhaps some other claim is needed.
    #[serde(
        rename = "CEDARLING_DECISION_LOG_DEFAULT_JWT_ID",
        default = "default_jti"
    )]
    pub decision_log_default_jwt_id: String,

    /// When `enabled`, Cedar engine authorization is queried for a User principal.
    #[serde(rename = "CEDARLING_USER_AUTHZ", default)]
    pub user_authz: FeatureToggle,

    /// When `enabled`, Cedar engine authorization is queried for a Workload principal.
    #[serde(rename = "CEDARLING_WORKLOAD_AUTHZ", default)]
    pub workload_authz: FeatureToggle,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD` when
    /// making authz (authorization) decisions.
    ///
    /// Use [JsonLogic](https://jsonlogic.com/).
    ///
    /// Default value:
    /// ```json
    /// {
    ///     "and" : [
    ///         {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
    ///         {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ///     ]
    /// }
    /// ```
    #[serde(rename = "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub principal_bool_operation: JsonRule,

    /// Mapping name of cedar schema TrustedIssuer entity
    #[serde(rename = "CEDARLING_MAPPING_TRUSTED_ISSUER", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub mapping_iss: Option<String>,

    /// Mapping name of cedar schema User entity
    #[serde(rename = "CEDARLING_MAPPING_USER", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub mapping_user: Option<String>,

    /// Mapping name of cedar schema Workload entity.
    #[serde(rename = "CEDARLING_MAPPING_WORKLOAD", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub mapping_workload: Option<String>,

    /// Mapping name of cedar schema Workload entity.
    #[serde(rename = "CEDARLING_MAPPING_ROLE", default)]
    pub mapping_role: Option<String>,

    /// Path to a local file pointing containing a JWKS.
    #[serde(
        rename = "CEDARLING_LOCAL_JWKS",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub local_jwks: Option<String>,

    /// JSON object with policy store
    #[serde(rename = "CEDARLING_POLICY_STORE_LOCAL", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub local_policy_store: Option<String>,

    /// Path to a Policy Store JSON file
    #[serde(
        rename = "CEDARLING_POLICY_STORE_LOCAL_FN",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub policy_store_local_fn: Option<String>,

    /// Whether to check the signature of all JWT tokens.
    ///
    /// This requires that an `iss` (Issuer) claim is present on each token.
    #[serde(rename = "CEDARLING_JWT_SIG_VALIDATION", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub jwt_sig_validation: FeatureToggle,

    /// Whether to check the status of the JWT. On startup.
    ///
    /// Cedarling will fetch and retreive the latest Status List JWT from the
    /// `.well-known/openid-configuration` via the `status_list_endpoint` claim and
    /// cache it. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    #[serde(rename = "CEDARLING_JWT_STATUS_VALIDATION", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub jwt_status_validation: FeatureToggle,

    /// Cedarling will only accept tokens signed with these algorithms.
    #[serde(rename = "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub jwt_signature_algorithms_supported: HashSet<Algorithm>,

    /// Varying levels of validations based on the preference of the developer.
    ///
    /// # Strict Mode
    ///
    /// Strict mode requires:
    ///     1. id_token aud matches the access_token client_id;
    ///     2. if a Userinfo token is present, the sub matches the id_token, and that
    ///         the aud matches the access token client_id.
    #[serde(rename = "CEDARLING_ID_TOKEN_TRUST_MODE", default)]
    pub id_token_trust_mode: IdTokenTrustMode,

    /// If Enabled, the Cedarling will connect to the Lock Master for policies,
    /// and subscribe for SSE events.
    #[serde(rename = "CEDARLING_LOCK", default)]
    pub lock: FeatureToggle,

    /// URI where Cedarling can get JSON file with all required metadata about
    /// Lock Master, i.e. .well-known/lock-master-configuration.
    ///
    /// ***Required*** if `LOCK == enabled`.
    #[serde(rename = "CEDARLING_LOCK_SERVER_CONFIGURATION_URI", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub lock_server_configuration_uri: Option<String>,

    /// Controls whether Cedarling should listen for SSE config updates.
    #[serde(rename = "CEDARLING_LOCK_DYNAMIC_CONFIGURATION", default)]
    pub dynamic_configuration: FeatureToggle,

    /// SSA for DCR in a Lock Master deployment. The Cedarling will validate this
    /// SSA JWT prior to DCR.
    #[serde(
        rename = "CEDARLING_LOCK_SSA_JWT",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub lock_ssa_jwt: Option<String>,

    /// How often to send log messages to Lock Master (0 to turn off trasmission).
    #[serde(rename = "CEDARLING_LOCK_LOG_INTERVAL", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub audit_log_interval: u64,

    /// How often to send health messages to Lock Master (0 to turn off transmission).
    #[serde(rename = "CEDARLING_LOCK_HEALTH_INTERVAL", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub audit_health_interval: u64,

    /// How often to send telemetry messages to Lock Master (0 to turn off transmission).
    #[serde(rename = "CEDARLING_LOCK_TELEMETRY_INTERVAL", default)]
    #[serde(deserialize_with = "deserialize_or_parse_string_as_json")]
    pub audit_telemetry_interval: u64,

    /// Controls whether Cedarling should listen for updates from the Lock Server.
    #[serde(rename = "CEDARLING_LOCK_LISTEN_SSE", default)]
    pub listen_sse: FeatureToggle,
}

impl BootstrapConfigRaw {
    /// Construct `BootstrapConfig` from environment variables and `BootstrapConfigRaw` config.
    /// Environment variables have bigger priority.
    //
    // Simple implementation that map input structure to JSON map
    // and map environment variables with prefix `CEDARLING_` to JSON map. And merge it.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn from_raw_config_and_env(
        raw: Option<BootstrapConfigRaw>,
    ) -> Result<Self, BootstrapConfigLoadingError> {
        let mut json_config_params = serde_json::json!(raw.unwrap_or_default())
            .as_object()
            .map(|v| v.to_owned())
            .unwrap_or_default();

        get_cedarling_env_vars().into_iter().for_each(|(k, v)| {
            // update map with values from env variables
            json_config_params.insert(k, v);
        });

        Ok(BootstrapConfigRaw::deserialize(json_config_params)?)
    }
}

/// Get environment variables related to `Cedarling`
#[cfg(not(target_arch = "wasm32"))]
fn get_cedarling_env_vars() -> HashMap<String, serde_json::Value> {
    env::vars()
        .filter_map(|(k, v)| {
            k.starts_with("CEDARLING_")
                .then_some((k, serde_json::json!(v)))
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::{
        env,
        sync::{LazyLock, Mutex},
    };
    use test_utils::assert_eq;

    static ENV_MUTEX: LazyLock<Mutex<()>> = LazyLock::new(|| Mutex::new(()));

    fn with_env_vars<F>(vars: Vec<(&str, &str)>, test: F)
    where
        F: FnOnce(),
    {
        // Ensure only one test modifies env vars at a time
        let _lock = ENV_MUTEX.lock().unwrap();

        // Create a fresh environment by clearing all CEDARLING_* vars
        let mut all_vars = Vec::new();
        for (key, value) in env::vars() {
            let key_clone = key.clone();
            all_vars.push((key, value));
            env::remove_var(&key_clone);
        }

        // Set new env vars
        for (key, value) in &vars {
            env::set_var(key, value);
        }

        test();

        // Clean up
        for (key, _) in &vars {
            env::remove_var(key);
        }

        // Restore original environment
        for (key, value) in all_vars {
            env::set_var(&key, value);
        }
    }

    /// Tests that the default configuration values are correctly set when no environment variables
    /// or raw config is provided.
    #[test]
    fn test_from_raw_config_and_env_defaults() {
        with_env_vars(vec![], || {
            let config = BootstrapConfigRaw::from_raw_config_and_env(None).unwrap();
            assert_eq!(
                config.application_name, "",
                "Application name should be empty by default"
            );
            assert_eq!(
                config.policy_store_uri, None,
                "Policy store URI should be None by default"
            );
            assert_eq!(
                config.policy_store_id, "",
                "Policy store ID should be empty by default"
            );
            assert_eq!(
                config.log_type,
                LoggerType::Off,
                "Logger should be off by default"
            );
            assert_eq!(
                config.log_level,
                LogLevel::WARN,
                "Default log level should be WARN"
            );
            assert_eq!(config.log_ttl, None, "Log TTL should be None by default");
            assert!(
                config.decision_log_user_claims.is_empty(),
                "Decision log user claims should be empty by default"
            );
            assert!(
                config.decision_log_workload_claims.is_empty(),
                "Decision log workload claims should be empty by default"
            );
            assert_eq!(
                config.decision_log_default_jwt_id, "",
                "Default JWT ID for decision logging should be ''"
            );
            assert_eq!(
                config.user_authz,
                FeatureToggle::Disabled,
                "User authorization should be disabled by default"
            );
            assert_eq!(
                config.workload_authz,
                FeatureToggle::Disabled,
                "Workload authorization should be disabled by default"
            );
            assert_eq!(
                config.principal_bool_operation,
                JsonRule::default(),
                "Default user-workload boolean operator should default"
            );
        });
    }

    /// Tests that configuration values are correctly set when only a raw config is provided.
    #[test]
    fn test_from_raw_config_and_env() {
        with_env_vars(vec![], || {
            let raw = BootstrapConfigRaw {
                application_name: "test-app".to_string(),
                log_type: LoggerType::Memory,
                log_level: LogLevel::DEBUG,
                ..Default::default()
            };

            let config = BootstrapConfigRaw::from_raw_config_and_env(Some(raw)).unwrap();

            assert_eq!(config.application_name, "test-app");
            assert_eq!(config.log_type, LoggerType::Memory);
            assert_eq!(config.log_level, LogLevel::DEBUG);
        });
    }

    /// Tests that configuration values are correctly set when only environment variables are provided.
    #[test]
    fn test_from_raw_config_and_env_with_env_vars() {
        with_env_vars(
            vec![
                ("CEDARLING_APPLICATION_NAME", "env-app"),
                ("CEDARLING_LOG_TYPE", "memory"),
                ("CEDARLING_LOG_LEVEL", "DEBUG"),
            ],
            || {
                let config = BootstrapConfigRaw::from_raw_config_and_env(None).unwrap();

                assert_eq!(config.application_name, "env-app");
                assert_eq!(config.log_type, LoggerType::Memory);
                assert_eq!(config.log_level, LogLevel::DEBUG);
            },
        );
    }

    /// Tests that environment variables override raw config values when both are provided.
    #[test]
    fn test_from_raw_config_and_env_vars_override() {
        with_env_vars(
            vec![
                ("CEDARLING_APPLICATION_NAME", "env-app"),
                ("CEDARLING_LOG_TYPE", "memory"),
            ],
            || {
                let raw = BootstrapConfigRaw {
                    application_name: "test-app".to_string(),
                    log_type: LoggerType::StdOut,
                    log_level: LogLevel::INFO,
                    ..Default::default()
                };

                let config = BootstrapConfigRaw::from_raw_config_and_env(Some(raw)).unwrap();

                // Env vars should override raw config
                assert_eq!(config.application_name, "env-app");
                assert_eq!(config.log_type, LoggerType::Memory);

                // Fields not set in env should use raw config values
                assert_eq!(config.log_level, LogLevel::INFO);
            },
        );
    }

    /// Tests that an error is returned when an invalid environment variable value is provided.
    #[test]
    fn test_from_raw_config_and_env_invalid_env_var() {
        with_env_vars(vec![("CEDARLING_LOG_TYPE", "invalid")], || {
            let result = BootstrapConfigRaw::from_raw_config_and_env(None);
            assert!(result.is_err());
        });
    }

    /// Tests that empty string values in environment variables are handled correctly.
    #[test]
    fn test_from_raw_config_and_env_empty_strings() {
        with_env_vars(
            vec![
                ("CEDARLING_APPLICATION_NAME", ""),
                ("CEDARLING_POLICY_STORE_URI", ""),
            ],
            || {
                let config = BootstrapConfigRaw::from_raw_config_and_env(None).unwrap();

                assert_eq!(config.application_name, "");
                assert_eq!(config.policy_store_uri, None);
            },
        );
    }
}
