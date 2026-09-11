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
use std::num::NonZeroUsize;
use std::path::Path;
use std::str::FromStr;
use std::time::Duration;

use super::authorization_config::AuthorizationConfig;
use super::raw_config::LoggerType;
use super::{
    BootstrapConfig, BootstrapConfigLoadingError, JwtConfig, LogConfig, LogTypeConfig,
    MemoryLogConfig, PolicyStoreConfig, PolicyStoreSource,
};
use super::{BootstrapConfigRaw, LockServiceConfig};
use crate::HttpClientConfig;
use crate::context_data_api::DataStoreConfig;
use crate::jwt_config::{TrustedIssuerLoaderConfig, TrustedIssuerLoaderTypeRaw, WorkersCount};
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
        let lock_config = raw.lock.is_enabled().then(|| raw.try_into()).transpose()?;

        // Decode LogCofig
        let log_config = LogConfig {
            log_type: resolve_log_type(raw)?,
            log_level: raw.log_level,
        };

        let policy_store_config = build_policy_store_config(raw)?;

        // Load the jwks from a local file
        let jwks = raw
            .local_jwks
            .as_ref()
            .map(|path| {
                fs::read_to_string(path).map_err(|e| {
                    BootstrapConfigLoadingError::LoadLocalJwks(path.clone(), e.to_string())
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
            trusted_issuer_loader: raw
                .trusted_issuer_loader_type
                .to_config(raw.trusted_issuer_loader_workers),
            jwks_refresh_interval: raw.jwks_refresh_interval,
            jwks_refresh_min_interval: raw.jwks_refresh_min_interval,
            status_list_refresh_interval_max: raw.status_list_refresh_interval_max,
        };

        let authorization_config = AuthorizationConfig {
            decision_log_default_jwt_id: raw.decision_log_default_jwt_id.clone(),
            strict_schema_validation: raw.strict_schema_validation.into(),
            custom_token_processor_timeout_millis: raw.custom_token_processor_timeout_millis,
        };

        // Build `DataStoreConfig` from raw config, using defaults if not specified
        let data_store_config = build_data_store_config(raw);

        let http_client_config = HttpClientConfig {
            max_retries: raw.http_client_request_max_retries,
            retry_delay: Duration::from_secs(raw.http_client_request_retry_delay),
            #[cfg(not(target_arch = "wasm32"))]
            request_timeout: Duration::from_secs(raw.http_client_request_timeout),
            // Unset falls back to the policy-store entry cap, so a download is
            // never larger than the largest archive entry we would decompress.
            // `0` is the documented "no cap" sentinel for either property.
            max_response_size_bytes: match raw
                .http_client_max_response_size_bytes
                .unwrap_or(raw.policy_store_max_file_size)
            {
                0 => None,
                n => Some(n),
            },
        };

        Ok(Self {
            application_name: raw.application_name.clone(),
            log_config,
            policy_store_config,
            jwt_config,
            authorization_config,
            lock_config,
            max_default_entities: raw.max_default_entities,
            max_base64_size: raw.max_base64_size,
            data_store_config,
            http_client_config,
        })
    }
}

/// Build [`PolicyStoreConfig`] from the three mutually-exclusive raw source
/// fields (`CEDARLING_POLICY_STORE_LOCAL`, `_URI`, `_LOCAL_FN`).
/// Returns an error if none or more than one are set
fn build_policy_store_config(
    raw: &BootstrapConfigRaw,
) -> Result<PolicyStoreConfig, BootstrapConfigLoadingError> {
    match (
        raw.local_policy_store.clone(),
        raw.policy_store_uri.clone(),
        raw.policy_store_local_fn.clone(),
        raw.policy_store_cjar_url.clone(),
    ) {
        // Case: no policy store provided
        (None, None, None, None) => Err(BootstrapConfigLoadingError::MissingPolicyStore),
        // Case: get the policy store from a JSON string
        (Some(policy_store), None, None, None) => Ok(PolicyStoreConfig {
            source: PolicyStoreSource::Json(policy_store),
            refresh_interval_secs: raw.policy_store_refresh_interval_secs,
            max_file_size: raw.policy_store_max_file_size,
        }),
        // Case: get the policy store from a URI
        (None, Some(policy_store_uri), None, None) => Ok(PolicyStoreConfig {
            source: PolicyStoreSource::Uri(policy_store_uri),
            refresh_interval_secs: raw.policy_store_refresh_interval_secs,
            max_file_size: raw.policy_store_max_file_size,
        }),
        // Case: get the policy store from a CjarUrl
        (None, None, None, Some(policy_store_cjar_url)) => Ok(PolicyStoreConfig {
            source: PolicyStoreSource::CjarUrl(policy_store_cjar_url),
            refresh_interval_secs: raw.policy_store_refresh_interval_secs,
            max_file_size: raw.policy_store_max_file_size,
        }),
        // Case: get the policy store from a local file or directory
        (None, None, Some(raw_path), None) => {
            let path = Path::new(&raw_path);
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
                    _ => {
                        return Err(
                            BootstrapConfigLoadingError::UnsupportedPolicyStoreFileFormat(raw_path),
                        );
                    },
                }
            };
            Ok(PolicyStoreConfig {
                source,
                refresh_interval_secs: raw.policy_store_refresh_interval_secs,
                max_file_size: raw.policy_store_max_file_size,
            })
        },
        // Case: multiple policy stores were set
        _ => Err(BootstrapConfigLoadingError::ConflictingPolicyStores),
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
            .map(std::time::Duration::from_secs)
            .or(defaults.default_ttl),
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::archive_handler::ArchiveLimits;

    /// Minimal config body; a policy store source is required, and the JSON
    /// string source keeps these tests off the filesystem and network.
    fn raw_config_json(extra: &str) -> String {
        format!(
            r#"{{
                "CEDARLING_APPLICATION_NAME": "test",
                "CEDARLING_POLICY_STORE_LOCAL": "{{\"cedar_version\":\"v4.0.0\",\"policy_stores\":{{}}}}"
                {extra}
            }}"#
        )
    }

    fn decode(extra: &str) -> BootstrapConfig {
        let raw: BootstrapConfigRaw = serde_json::from_str(&raw_config_json(extra))
            .expect("raw bootstrap config should deserialize");
        BootstrapConfig::try_from(raw).expect("raw config should decode")
    }

    #[test]
    fn unset_http_cap_falls_back_to_policy_store_max_file_size() {
        let config = decode(r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": 4096"#);

        assert_eq!(
            config.http_client_config.max_response_size_bytes,
            Some(4096),
            "An unset HTTP cap must inherit the policy store cap, so a download \
             is never larger than the largest entry we would decompress"
        );
        assert_eq!(config.policy_store_config.max_file_size, 4096);
    }

    #[test]
    fn unset_http_cap_falls_back_to_the_default_when_neither_is_set() {
        let config = decode("");

        assert_eq!(
            config.http_client_config.max_response_size_bytes,
            Some(ArchiveLimits::DEFAULT_MAX_ENTRY_SIZE),
            "With neither property set both should land on the 10 MB default"
        );
        assert_eq!(
            config.policy_store_config.max_file_size,
            ArchiveLimits::DEFAULT_MAX_ENTRY_SIZE
        );
    }

    #[test]
    fn explicit_http_cap_wins_over_policy_store_max_file_size() {
        let config = decode(
            r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": 4096,
                "CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES": 8192"#,
        );

        assert_eq!(
            config.http_client_config.max_response_size_bytes,
            Some(8192),
            "An explicitly set HTTP cap must not be overridden by the fallback"
        );
        assert_eq!(config.policy_store_config.max_file_size, 4096);
    }

    #[test]
    fn zero_disables_each_cap_independently() {
        let explicit_zero = decode(
            r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": 4096,
                "CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES": 0"#,
        );
        assert_eq!(
            explicit_zero.http_client_config.max_response_size_bytes, None,
            "An explicit 0 must disable the HTTP cap, not inherit 4096"
        );
        assert_eq!(explicit_zero.policy_store_config.max_file_size, 4096);

        let inherited_zero = decode(r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": 0"#);
        assert_eq!(
            inherited_zero.http_client_config.max_response_size_bytes, None,
            "A 0 policy store cap must carry through the fallback as no cap"
        );
    }

    #[test]
    fn max_file_size_is_honored_from_json_and_yaml() {
        // Env is covered by the `raw_config` tests; these two are the remaining
        // documented input formats.
        let from_json = BootstrapConfig::load_from_json(&raw_config_json(
            r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": 512"#,
        ))
        .expect("JSON bootstrap config should load");
        assert_eq!(from_json.policy_store_config.max_file_size, 512);
        assert_eq!(
            from_json.http_client_config.max_response_size_bytes,
            Some(512)
        );

        let yaml = concat!(
            "CEDARLING_APPLICATION_NAME: test\n",
            "CEDARLING_POLICY_STORE_LOCAL: '{\"cedar_version\":\"v4.0.0\",\"policy_stores\":{}}'\n",
            "CEDARLING_POLICY_STORE_MAX_FILE_SIZE: 512\n",
        );
        let raw: BootstrapConfigRaw =
            serde_yaml_ng::from_str(yaml).expect("YAML bootstrap config should deserialize");
        let from_yaml = BootstrapConfig::try_from(raw).expect("YAML config should decode");
        assert_eq!(from_yaml.policy_store_config.max_file_size, 512);
        assert_eq!(
            from_yaml.http_client_config.max_response_size_bytes,
            Some(512)
        );
    }

    #[test]
    fn max_file_size_is_honored_from_a_string_valued_env_var() {
        // Env vars always arrive as strings, so the numeric properties go
        // through `deserialize_or_parse_string_as_json`.
        let config = decode(r#", "CEDARLING_POLICY_STORE_MAX_FILE_SIZE": "4096""#);

        assert_eq!(config.policy_store_config.max_file_size, 4096);
        assert_eq!(
            config.http_client_config.max_response_size_bytes,
            Some(4096)
        );
    }
}
