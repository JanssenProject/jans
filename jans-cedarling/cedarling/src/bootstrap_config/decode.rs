// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// to avoid a lot of `cfg` macros
#![allow(unused_imports)]

use std::collections::{HashMap, HashSet};
use std::env;
use std::fmt::Display;
use std::fs;
use std::path::Path;
use std::str::FromStr;

use super::authorization_config::{AuthorizationConfig, IdTokenTrustMode};
use super::raw_config::LoggerType;
use super::{
    BootstrapConfig, BootstrapConfigLoadingError, JwtConfig, LogConfig, LogTypeConfig,
    MemoryLogConfig, PolicyStoreConfig, PolicyStoreSource,
};
use super::{BootstrapConfigRaw, LockServiceConfig};
use crate::context_data_api::DataStoreConfig;
use crate::log::{LogLevel, StdOutLoggerMode};
use jsonwebtoken::Algorithm;
use serde::{Deserialize, Deserializer, Serialize};

impl BootstrapConfig {
    /// Construct `BootstrapConfig` from environment variables and `BootstrapConfigRaw` config.
    /// Environment variables have bigger priority.
    //
    // Simple implementation that map input structure to JSON map
    // and map environment variables with prefix `CEDARLING_` to JSON map. And merge it.
    #[cfg(not(target_arch = "wasm32"))]
    pub fn from_raw_config_and_env(
        raw: Option<BootstrapConfigRaw>,
    ) -> Result<Self, BootstrapConfigLoadingError> {
        let config_raw = BootstrapConfigRaw::from_raw_config_and_env(raw)?;
        Self::from_raw_config(&config_raw)
    }

    /// Construct an instance from [`BootstrapConfigRaw`]
    pub fn from_raw_config(raw: &BootstrapConfigRaw) -> Result<Self, BootstrapConfigLoadingError> {
        if !raw.workload_authz.is_enabled() && !raw.user_authz.is_enabled() {
            return Err(BootstrapConfigLoadingError::BothPrincipalsDisabled);
        }

        let lock_config = raw.lock.is_enabled().then(|| raw.try_into()).transpose()?;

        // Decode LogCofig
        let log_config = LogConfig {
            log_type: resolve_log_type(raw)?,
            log_level: raw.log_level,
        };

        // Decode policy store
        let policy_store_config = match (
            raw.local_policy_store.clone(),
            raw.policy_store_uri.clone(),
            raw.policy_store_local_fn.clone(),
        ) {
            // Case: no policy store provided
            (None, None, None) => Err(BootstrapConfigLoadingError::MissingPolicyStore)?,
            // Case: get the policy store from a JSON string
            (Some(policy_store), None, None) => PolicyStoreConfig {
                source: PolicyStoreSource::Json(policy_store),
            },
            // Case: get the policy store from a URI (auto-detect .cjar archives)
            (None, Some(policy_store_uri), None) => {
                let source = if policy_store_uri.to_lowercase().ends_with(".cjar") {
                    PolicyStoreSource::CjarUrl(policy_store_uri)
                } else {
                    PolicyStoreSource::LockServer(policy_store_uri)
                };
                PolicyStoreConfig { source }
            },
            // Case: get the policy store from a local file or directory
            (None, None, Some(raw_path)) => {
                let path = Path::new(&raw_path);

                // Check if it's a directory first
                let source = if path.is_dir() {
                    PolicyStoreSource::Directory(path.into())
                } else {
                    let file_ext = path
                        .extension()
                        .and_then(|ext| ext.to_str())
                        .map(str::to_lowercase);

                    match file_ext.as_deref() {
                        Some("json") => PolicyStoreSource::FileJson(path.into()),
                        Some("yaml" | "yml") => PolicyStoreSource::FileYaml(path.into()),
                        Some("cjar") => PolicyStoreSource::CjarFile(path.into()),
                        _ => Err(
                            BootstrapConfigLoadingError::UnsupportedPolicyStoreFileFormat(raw_path),
                        )?,
                    }
                };
                PolicyStoreConfig { source }
            },
            // Case: multiple polict stores were set
            _ => Err(BootstrapConfigLoadingError::ConflictingPolicyStores)?,
        };

        // Load the jwks from a local file
        let jwks = raw
            .local_jwks
            .as_ref()
            .map(|path| {
                fs::read_to_string(path).map_err(|e| {
                    BootstrapConfigLoadingError::LoadLocalJwks(path.to_string(), e.to_string())
                })
            })
            .transpose()?;

        // JWT Config
        let jwt_config = JwtConfig {
            jwks,
            jwt_sig_validation: raw.jwt_sig_validation.into(),
            jwt_status_validation: raw.jwt_status_validation.into(),
            signature_algorithms_supported: raw.jwt_signature_algorithms_supported.clone(),
            token_cache_max_ttl_secs: raw.token_cache_max_ttl,
            token_cache_capacity: raw.token_cache_capacity,
            token_cache_earliest_expiration_eviction: raw.token_cache_earliest_expiration_eviction,
        };

        let authorization_config = AuthorizationConfig {
            use_user_principal: raw.user_authz.is_enabled(),
            use_workload_principal: raw.workload_authz.is_enabled(),
            principal_bool_operator: raw.principal_bool_operation.clone(),
            decision_log_user_claims: raw.decision_log_user_claims.clone(),
            decision_log_workload_claims: raw.decision_log_workload_claims.clone(),
            decision_log_default_jwt_id: raw.decision_log_default_jwt_id.clone(),
            id_token_trust_mode: raw.id_token_trust_mode.clone(),
        };

        // Build `DataStoreConfig` from raw config, using defaults if not specified
        let data_store_config = build_data_store_config(raw);

        Ok(Self {
            application_name: raw.application_name.clone(),
            log_config,
            policy_store_config,
            jwt_config,
            authorization_config,
            entity_builder_config: raw.into(),
            lock_config,
            max_default_entities: raw.max_default_entities,
            max_base64_size: raw.max_base64_size,
            data_store_config,
        })
    }
}

/// Build `DataStoreConfig` from raw config fields.
/// Uses default values for any fields that are not specified.
fn build_data_store_config(raw: &BootstrapConfigRaw) -> DataStoreConfig {
    let defaults = DataStoreConfig::default();

    DataStoreConfig {
        max_entries: raw.data_store_max_entries.unwrap_or(defaults.max_entries),
        max_entry_size: raw
            .data_store_max_entry_size
            .unwrap_or(defaults.max_entry_size),
        default_ttl: raw
            .data_store_default_ttl
            .map(std::time::Duration::from_secs),
        max_ttl: raw
            .data_store_max_ttl
            .map(std::time::Duration::from_secs)
            .or(defaults.max_ttl),
        enable_metrics: raw
            .data_store_enable_metrics
            .unwrap_or(defaults.enable_metrics),
        memory_alert_threshold: raw
            .data_store_memory_alert_threshold
            .unwrap_or(defaults.memory_alert_threshold),
    }
}

/// Helper function to resolve log type from raw config
fn resolve_log_type(
    raw_config: &BootstrapConfigRaw,
) -> Result<LogTypeConfig, BootstrapConfigLoadingError> {
    let log_type_config = match raw_config.log_type {
        LoggerType::Off => LogTypeConfig::Off,
        LoggerType::Memory => LogTypeConfig::Memory(MemoryLogConfig {
            log_ttl: raw_config
                .log_ttl
                .ok_or(BootstrapConfigLoadingError::MissingLogTTL)?,
            max_item_size: raw_config.log_max_item_size,
            max_items: raw_config.log_max_items,
        }),
        LoggerType::StdOut => {
            let std_out_logger_conf = match raw_config.stdout_mode {
                // WASM does not support async
                #[cfg(not(target_arch = "wasm32"))]
                super::log_config::StdOutMode::Async => StdOutLoggerMode::Async {
                    timeout_millis: raw_config.stdout_timeout_millis,
                    buffer_limit: raw_config.stdout_buffer_limit,
                },
                super::log_config::StdOutMode::Immediate => StdOutLoggerMode::Immediate,
            };
            LogTypeConfig::StdOut(std_out_logger_conf)
        },
    };
    Ok(log_type_config)
}
