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

use super::BootstrapConfigRaw;
use super::authorization_config::{AuthorizationConfig, IdTokenTrustMode};
use super::raw_config::LoggerType;
use super::{
    BootstrapConfig, BootstrapConfigLoadingError, JwtConfig, LogConfig, LogTypeConfig,
    MemoryLogConfig, PolicyStoreConfig, PolicyStoreSource,
};
use crate::log::LogLevel;
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

    /// Construct an instance from BootstrapConfigRaw
    pub fn from_raw_config(raw: &BootstrapConfigRaw) -> Result<Self, BootstrapConfigLoadingError> {
        if !raw.workload_authz.is_enabled() && !raw.user_authz.is_enabled() {
            return Err(BootstrapConfigLoadingError::BothPrincipalsDisabled);
        }

        // Decode LogCofig
        let log_type = match raw.log_type {
            LoggerType::Off => LogTypeConfig::Off,
            LoggerType::Memory => LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: raw
                    .log_ttl
                    .ok_or(BootstrapConfigLoadingError::MissingLogTTL)?,
                max_item_size: raw.log_max_item_size,
                max_items: raw.log_max_items,
            }),
            LoggerType::StdOut => LogTypeConfig::StdOut,
            LoggerType::Lock => LogTypeConfig::Lock,
        };
        let log_config = LogConfig {
            log_type,
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
            // Case: get the policy store from the lock server
            (None, Some(policy_store_uri), None) => PolicyStoreConfig {
                source: PolicyStoreSource::LockServer(policy_store_uri),
            },
            // Case: get the policy store from a local JSON file
            (None, None, Some(raw_path)) => {
                let path = Path::new(&raw_path);
                let file_ext = Path::new(&path)
                    .extension()
                    .and_then(|ext| ext.to_str())
                    .map(|x| x.to_lowercase());

                let source = match file_ext.as_deref() {
                    Some("json") => PolicyStoreSource::FileJson(path.into()),
                    Some("yaml") | Some("yml") => PolicyStoreSource::FileYaml(path.into()),
                    _ => Err(
                        BootstrapConfigLoadingError::UnsupportedPolicyStoreFileFormat(raw_path),
                    )?,
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
        };

        let authorization_config = AuthorizationConfig {
            use_user_principal: raw.user_authz.is_enabled(),
            use_workload_principal: raw.workload_authz.is_enabled(),
            user_workload_operator: raw.usr_workload_bool_op,
            decision_log_user_claims: raw.decision_log_user_claims.clone(),
            decision_log_workload_claims: raw.decision_log_workload_claims.clone(),
            decision_log_default_jwt_id: raw.decision_log_default_jwt_id.clone(),
            mapping_iss: raw.mapping_iss.clone(),
            mapping_user: raw.mapping_user.clone(),
            mapping_workload: raw.mapping_workload.clone(),
            mapping_role: raw.mapping_role.clone(),
            id_token_trust_mode: raw.id_token_trust_mode,
        };

        Ok(Self {
            application_name: raw.application_name.clone(),
            log_config,
            policy_store_config,
            jwt_config,
            authorization_config,
        })
    }
}
