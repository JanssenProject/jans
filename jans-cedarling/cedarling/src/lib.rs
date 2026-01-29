// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![deny(missing_docs)]
#![warn(unreachable_pub)]
#![allow(clippy::missing_errors_doc)]
//! # Cedarling
//! The Cedarling is a performant local authorization service that runs the Rust Cedar Engine.
//! Cedar policies and schema are loaded at startup from a locally cached "Policy Store".
//! In simple terms, the Cedarling returns the answer: should the application allow this action on this resource given these JWT tokens.
//! "Fit for purpose" policies help developers build a better user experience.
//! For example, why display form fields that a user is not authorized to see?
//! The Cedarling is a more productive and flexible way to handle authorization.

mod authz;
mod bootstrap_config;
mod common;
mod data;
mod entity_builder;
mod http;
mod init;
mod jwt;
mod lock;
mod log;

#[cfg(not(target_arch = "wasm32"))]
#[cfg(feature = "blocking")]
pub mod blocking;

#[doc(hidden)]
#[cfg(test)]
mod tests;

use std::{fmt::Write, sync::Arc};

pub use crate::common::json_rules::JsonRule;
use crate::data::DataStore;
pub use crate::data::{
    CedarType, CedarValueMapper, ConfigValidationError, DataApi, DataEntry, DataError,
    DataStoreConfig, DataStoreStats, DataValidator, ExtensionValue, ValidationConfig,
    ValidationError, ValidationResult, ValueMappingError,
};
pub use crate::init::policy_store::{PolicyStoreLoadError, load_policy_store};
#[cfg(test)]
use authz::AuthorizeEntitiesData;
use authz::Authz;
pub use authz::request::{
    AuthorizeMultiIssuerRequest, CedarEntityMapping, EntityData, Request, RequestUnsigned,
    TokenInput,
};
pub use authz::{AuthorizeError, AuthorizeResult, MultiIssuerAuthorizeResult};
pub use bootstrap_config::*;
use common::app_types::{self, ApplicationName};
use init::ServiceFactory;
use init::service_config::{ServiceConfig, ServiceConfigError};
use init::service_factory::ServiceInitError;
use lock::InitLockServiceError;
use log::interface::LogWriter;
use log::{BaseLogEntry, LogEntry};
pub use log::{LogLevel, LogStorage};

use semver::Version;

#[doc(hidden)]
pub mod bindings {
    pub use cedar_policy;

    pub use super::log::{
        AuthorizationLogInfo, Decision, Diagnostics, LogEntry, PolicyEvaluationError,
    };
    pub use crate::common::policy_store::PolicyStore;

    pub use serde_json;
    pub use serde_yml;
}

/// Errors that can occur during initialization Cedarling.
#[derive(Debug, thiserror::Error)]
pub enum InitCedarlingError {
    /// Error while preparing config for internal services
    #[error(transparent)]
    ServiceConfig(#[from] ServiceConfigError),
    /// Error while initializing a Service
    #[error(transparent)]
    ServiceInit(#[from] ServiceInitError),
    /// Error while parse [`BootstrapConfigRaw`]
    #[error(transparent)]
    BootstrapConfigLoading(#[from] BootstrapConfigLoadingError),
    #[cfg(feature = "blocking")]
    /// Error while init tokio runtime
    #[error(transparent)]
    RuntimeInit(std::io::Error),
    /// Error returned when Cedarling fails to obtain client credentials for sending
    /// logs to the Lock Server.
    #[error("failed to initialize the Lock Service: {0}")]
    InitLockService(#[from] InitLockServiceError),
}

/// The instance of the Cedarling application.
/// It is safe to share between threads.
#[derive(Clone)]
pub struct Cedarling {
    log: log::Logger,
    authz: Arc<Authz>,
    data: Arc<DataStore>,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    /// Initialize instance from enviroment variables and from config.
    /// Configuration structure has lower priority.
    #[cfg(not(target_arch = "wasm32"))]
    pub async fn new_with_env(
        raw_config: Option<BootstrapConfigRaw>,
    ) -> Result<Cedarling, InitCedarlingError> {
        let config = BootstrapConfig::from_raw_config_and_env(raw_config)?;
        Self::new(&config).await
    }

    /// Create a new instance of the Cedarling application.
    ///
    /// # Panics
    ///
    /// Panics if the `data_store_config` is invalid. This should not happen
    /// in practice because the config is validated during bootstrap parsing.
    pub async fn new(config: &BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let pdp_id = app_types::PdpID::new();
        let app_name = (!config.application_name.is_empty())
            .then(|| ApplicationName(config.application_name.clone()));

        let log = log::init_logger(
            &config.log_config,
            pdp_id,
            app_name,
            config.lock_config.as_ref(),
        )
        .await?;

        let service_config = ServiceConfig::new(config)
            .await
            .inspect(|_| {
                log.log_any(
                    LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                        LogLevel::DEBUG,
                        None,
                    ))
                    .set_message("configuration parsed successfully".to_string()),
                );
            })
            .inspect_err(|err| {
                log.log_any(
                    LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                        LogLevel::ERROR,
                        None,
                    ))
                    .set_error(err.to_string())
                    .set_message("configuration parsed with error".to_string()),
                );
            })?;

        // Initialize data store first so it can be passed to authz service
        let data_store_config = config.data_store_config.clone().unwrap_or_default();
        let data = Arc::new(
            DataStore::new(data_store_config)
                .expect("DataStoreConfig should be valid after bootstrap config parsing"),
        );

        let mut service_factory =
            ServiceFactory::new(config, service_config, log.clone(), data.clone());

        // Log policy store metadata if available (new format only)
        if let Some(metadata) = service_factory.policy_store_metadata() {
            log_policy_store_metadata(&log, metadata);
        }

        Ok(Cedarling {
            log,
            authz: service_factory.authz_service().await?,
            data,
        })
    }

    // The following public methods retain async signatures for API compatibility
    // to avoid breaking changes. They use #[allow(clippy::unused_async)] since
    // they no longer await internally. Future maintainers can safely remove
    // or refactor these methods when compatibility constraints allow.

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    #[allow(clippy::unused_async)]
    pub async fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.authorize(&request)
    }

    /// Authorize request with unsigned data.
    /// makes authorization decision based on the [`RequestUnverified`]
    #[allow(clippy::unused_async)]
    pub async fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.authorize_unsigned(&request)
    }

    /// Authorize multi-issuer request.
    /// makes authorization decision based on multiple JWT tokens from different issuers
    #[allow(clippy::unused_async)]
    pub async fn authorize_multi_issuer(
        &self,
        request: AuthorizeMultiIssuerRequest,
    ) -> Result<MultiIssuerAuthorizeResult, AuthorizeError> {
        self.authz.authorize_multi_issuer(&request)
    }

    /// Get entites derived from `cedar-policy` schema and tokens for `authorize` request.
    #[doc(hidden)]
    #[cfg(test)]
    pub(crate) fn build_entities(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, Box<AuthorizeError>> {
        let tokens = self.authz.decode_tokens(request)?;
        self.authz.build_entities(request, &tokens)
    }

    /// Closes the connections to the Lock Server and pushes all available logs.
    pub async fn shut_down(&self) {
        self.log.shut_down().await;
    }
}

/// Log detailed information about the loaded policy store metadata, including
/// ID, version, description, Cedar version, timestamps, and compatibility with
/// the runtime Cedar version.
fn log_policy_store_metadata(
    log: &log::Logger,
    metadata: &crate::common::policy_store::PolicyStoreMetadata,
) {
    // Build detailed log message using accessor methods
    let mut details = format!(
        "Policy store '{}' (ID: {}) v{} loaded",
        metadata.name(),
        if metadata.id().is_empty() {
            "<auto>"
        } else {
            metadata.id()
        },
        metadata.version()
    );

    // Add description if available
    if let Some(desc) = metadata.description() {
        let _ = write!(details, " - {desc}");
    }

    // Add Cedar version info
    let _ = write!(details, " [Cedar {}]", metadata.cedar_version());

    // Add timestamp info if available
    if let Some(created) = metadata.created_date() {
        let _ = write!(details, " (created: {})", created.format("%Y-%m-%d"));
    }
    if let Some(updated) = metadata.updated_date() {
        let _ = write!(details, " (updated: {})", updated.format("%Y-%m-%d"));
    }

    log.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::DEBUG,
            None,
        ))
        .set_message(details),
    );

    // Log version compatibility check with current Cedar
    let current_cedar_version: Version = cedar_policy::get_lang_version();
    match metadata.is_compatible_with_cedar(&current_cedar_version) {
        Ok(true) => {
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::DEBUG,
                    None,
                ))
                .set_message(format!(
                    "Policy store Cedar version {} is compatible with runtime version {}",
                    metadata.cedar_version(),
                    current_cedar_version
                )),
            );
        },
        Ok(false) => {
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "Policy store Cedar version {} may not be compatible with runtime version {}",
                    metadata.cedar_version(),
                    current_cedar_version
                )),
            );
        },
        Err(e) => {
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!("Could not check Cedar version compatibility: {e}")),
            );
        },
    }

    // Log parsed version for debugging if available
    if let Some(parsed_version) = metadata.version_parsed() {
        log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::TRACE,
                None,
            ))
            .set_message(format!(
                "Policy store semantic version: {}.{}.{}",
                parsed_version.major, parsed_version.minor, parsed_version.patch
            )),
        );
    }
}

// implements LogStorage for Cedarling
// we can use this methods outside crate only when import trait
impl LogStorage for Cedarling {
    fn pop_logs(&self) -> Vec<serde_json::Value> {
        self.log.pop_logs()
    }

    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value> {
        self.log.get_log_by_id(id)
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.log.get_log_ids()
    }

    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value> {
        self.log.get_logs_by_tag(tag)
    }

    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value> {
        self.log.get_logs_by_request_id(request_id)
    }

    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> Vec<serde_json::Value> {
        self.log.get_logs_by_request_id_and_tag(id, tag)
    }
}

// implements DataApi for Cedarling
// provides public interface for pushing and retrieving data
impl DataApi for Cedarling {
    fn push_data(
        &self,
        key: &str,
        value: serde_json::Value,
        ttl: Option<std::time::Duration>,
    ) -> Result<(), DataError> {
        self.data.push(key, value, ttl)?;

        // Check memory usage and log warning if threshold is exceeded
        let config = self.data.config();
        if config.max_entries > 0 {
            let entry_count = self.data.count();
            // Precision loss is acceptable for percentage calculation
            #[allow(clippy::cast_precision_loss)]
            let usage_percent = (entry_count as f64 / config.max_entries as f64) * 100.0;
            if usage_percent >= config.memory_alert_threshold {
                let log_entry = LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "DataStore memory usage alert: {:.1}% capacity used ({}/{} entries), threshold: {:.1}%",
                    usage_percent,
                    entry_count,
                    config.max_entries,
                    config.memory_alert_threshold
                ));
                self.log.log_any(log_entry);
            }
        }

        Ok(())
    }

    fn get_data(&self, key: &str) -> Result<Option<serde_json::Value>, DataError> {
        Ok(self.data.get(key))
    }

    fn get_data_entry(&self, key: &str) -> Result<Option<DataEntry>, DataError> {
        Ok(self.data.get_entry(key))
    }

    fn remove_data(&self, key: &str) -> Result<bool, DataError> {
        Ok(self.data.remove(key))
    }

    fn clear_data(&self) -> Result<(), DataError> {
        self.data.clear();
        Ok(())
    }

    fn list_data(&self) -> Result<Vec<DataEntry>, DataError> {
        Ok(self.data.list_entries())
    }

    fn get_stats(&self) -> Result<DataStoreStats, DataError> {
        let config = self.data.config();
        let entry_count = self.data.count();
        let total_size_bytes = self.data.total_size();
        let avg_entry_size_bytes = if entry_count > 0 {
            total_size_bytes / entry_count
        } else {
            0
        };

        // Calculate capacity usage percentage
        // Precision loss is acceptable for percentage calculation
        #[allow(clippy::cast_precision_loss)]
        let capacity_usage_percent = if config.max_entries > 0 {
            (entry_count as f64 / config.max_entries as f64) * 100.0
        } else {
            0.0 // Unlimited capacity, no percentage
        };

        let memory_alert_triggered = capacity_usage_percent >= config.memory_alert_threshold;

        Ok(DataStoreStats {
            entry_count,
            max_entries: config.max_entries,
            max_entry_size: config.max_entry_size,
            metrics_enabled: config.enable_metrics,
            total_size_bytes,
            avg_entry_size_bytes,
            capacity_usage_percent,
            memory_alert_threshold: config.memory_alert_threshold,
            memory_alert_triggered,
        })
    }
}
