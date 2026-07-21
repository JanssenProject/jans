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

mod async_sleep;
mod authz;
mod bootstrap_config;
mod common;
mod context_data_api;
mod entity_builder;
mod http;
mod http_utils;
mod init;
mod jwt;
mod lock;
mod log;
// is reexported in hidden bindings module
#[doc(hidden)]
pub mod sparkv;

#[cfg(not(target_arch = "wasm32"))]
#[cfg(feature = "blocking")]
pub mod blocking;

#[doc(hidden)]
#[cfg(test)]
mod tests;

use std::collections::HashSet;
use std::{fmt::Write, sync::Arc};

use crate::authz::metrics::MetricsCollector;
use crate::context_data_api::DataStore;
pub use crate::context_data_api::{
    CedarType, CedarValueMapper, ConfigValidationError, DataApi, DataEntry, DataError,
    DataStoreConfig, DataStoreStats, DataValidator, ExtensionValue, ValidationConfig,
    ValidationError, ValidationResult, ValueMappingError,
};
pub use crate::jwt::TrustedIssuerLoadingInfo;
use authz::Authz;
pub use authz::request::{
    AuthorizeMultiIssuerRequest, BatchAuthorizeMultiIssuerRequest, BatchAuthorizeResponse,
    BatchAuthorizeUnsignedRequest, BatchItem, CedarEntityMapping, EntityData, RequestUnsigned,
    TokenInput,
};
pub use authz::{
    AuthorizeError, AuthorizeResult, BatchItemError, MultiIssuerAuthorizeResult,
};
pub use bootstrap_config::*;
use common::app_types::{self, ApplicationName};
pub use common::policy_store::{PolicyEffect, PolicyMetadata};
pub use http::HttpClientConfig;
use init::ServiceFactory;
use init::policy_store::{LoadedPolicyStore, load_policy_store};
use init::policy_store_refresh::{
    AuthzRebuilder, PolicyStoreRefreshHandle, RefreshSource, RefreshWorkerSeed, WorkerContext,
    spawn_refresh_worker,
};
use init::service_config::{ServiceConfig, ServiceConfigError};
use init::service_factory::ServiceInitError;
use lock::InitLockServiceError;
use lock::health_registry::HealthStatus;
use log::interface::LogWriter;
use log::{BaseLogEntry, LogEntry};
pub use log::{LogLevel, LogStorage};

use semver::Version;

/// Git commit hash at build time (`None` if git is unavailable or
/// `CEDARLING_BUILD_COMMIT` was not set at compile time).
pub const BUILD_COMMIT: Option<&str> = option_env!("CEDARLING_BUILD_COMMIT");
/// Build timestamp in RFC 3339 format (`None` if
/// `CEDARLING_BUILD_TIMESTAMP` was not set at compile time).
pub const BUILD_TIMESTAMP: Option<&str> = option_env!("CEDARLING_BUILD_TIMESTAMP");

#[doc(hidden)]
pub mod bindings {
    pub use cedar_policy;

    pub use super::log::{
        AuthorizationLogInfo, Decision, Diagnostics, LogEntry, PolicyEvaluationError,
    };
    pub use crate::common::policy_store::PolicyStore;
    pub use crate::http::spawn_task;
    pub use crate::sparkv;
    pub use serde_json;
    pub use serde_yaml_ng;
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
    /// Error while initializing the `DataStore` (invalid configuration)
    #[error(transparent)]
    DataStoreInit(#[from] ConfigValidationError),
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
    /// Wrapped in [`ArcSwap`] so the policy-store refresh worker can publish a
    /// freshly built [`Authz`] (with new policy store, rebuilt JWT service and
    /// entity builder) atomically. Every public method snapshots via
    /// [`ArcSwap::load`] so an in-flight authorization keeps using the
    /// pre-swap instance.
    authz: Arc<arc_swap::ArcSwap<Authz>>,
    data: Arc<DataStore>,
    /// Held purely for its `Drop` side effect: dropping the last `Arc` closes
    /// the worker's `oneshot` shutdown channel so the background refresh loop
    /// exits when [`Cedarling`] goes away. The leading `_` tells the compiler
    /// the field is intentionally not read.
    _refresh_handle: Option<Arc<PolicyStoreRefreshHandle>>,
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
    pub async fn new(config: &BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let pdp_id = app_types::PdpID::new();
        let app_name = (!config.application_name.is_empty())
            .then(|| ApplicationName::from(config.application_name.clone()));

        let metrics = Arc::new(
            if config
                .lock_config
                .as_ref()
                .is_some_and(|c| c.telemetry_interval.is_some())
            {
                MetricsCollector::new(0)
            } else {
                MetricsCollector::disabled()
            },
        );

        let log = crate::log::init_logger(
            &config.log_config,
            pdp_id,
            app_name,
            config.lock_config.as_ref(),
            metrics.clone(),
            config.http_client_config,
        )
        .await?;

        log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::INFO,
                None,
            ))
            .set_message("Cedarling initialization started".to_string())
            .set_build_info(BUILD_COMMIT, BUILD_TIMESTAMP),
        );

        // Bootstrap-load: build HttpClient + load policy store. Returns the
        // service config plus the refresh-worker seed in one shot so the
        // seed values (initial body hash, initial cache validators) stay in
        // lexical scope right next to the refresh-worker spawn below.
        let (service_config, refresh_seed) = perform_bootstrap_load(config, &log).await?;

        let policy_count = service_config
            .policy_store
            .policies
            .get_set()
            .num_of_policies();
        metrics.set_policy_count(policy_count);

        // Initialize data store first so it can be passed to authz service
        let data = Arc::new(DataStore::new(
            config.data_store_config.clone(),
            metrics.clone(),
        )?);

        let mut service_factory = ServiceFactory::new(
            config,
            service_config,
            log.clone(),
            data.clone(),
            metrics.clone(),
        );

        // Log policy store metadata if available (new format only)
        if let Some(metadata) = service_factory.policy_store_metadata() {
            log_policy_store_metadata(&log, metadata);
        }

        if let Some(registry) = log.health_registry() {
            registry.register("core", || HealthStatus::Success);
            registry.register("policy_load", move || {
                if policy_count > 0 {
                    HealthStatus::Success
                } else {
                    HealthStatus::Failure
                }
            });
        }

        let authz = service_factory.authz_service().await?;
        let authz_swap = Arc::new(arc_swap::ArcSwap::from(authz));

        let refresh_handle = maybe_spawn_refresh_worker(
            config,
            &service_factory,
            authz_swap.clone(),
            log.clone(),
            data.clone(),
            metrics.clone(),
            refresh_seed,
        );

        Ok(Cedarling {
            log,
            authz: authz_swap,
            data,
            _refresh_handle: refresh_handle,
        })
    }

    // The following public methods retain async signatures for API compatibility
    // to avoid breaking changes. They use #[allow(clippy::unused_async)] since
    // they no longer await internally. Future maintainers can safely remove
    // or refactor these methods when compatibility constraints allow.

    /// Authorize request with unsigned data.
    /// makes authorization decision based on the [`RequestUnverified`]
    #[allow(clippy::unused_async)]
    pub async fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.load().authorize_unsigned(&request)
    }

    /// Authorize a batch of unsigned requests against one shared principal.
    ///
    /// Runs setup work (principal build + pushed-data snapshot) once and
    /// evaluates each item with its own resource and context. Results are
    /// returned in input order, wrapped in a [`BatchAuthorizeResponse`] that
    /// carries a shared `batch_id` for audit correlation.
    ///
    /// Batch-level failures (validation, principal parse) return `Err`;
    /// per-item failures synthesize a fail-closed `Deny` for that item
    /// without affecting other items.
    #[allow(clippy::unused_async)]
    pub async fn authorize_unsigned_batch(
        &self,
        request: BatchAuthorizeUnsignedRequest,
    ) -> Result<BatchAuthorizeResponse<Result<AuthorizeResult, BatchItemError>>, AuthorizeError>
    {
        self.authz.load().authorize_unsigned_batch(&request)
    }

    /// Authorize multi-issuer request.
    /// makes authorization decision based on multiple JWT tokens from different issuers
    #[allow(clippy::unused_async)]
    pub async fn authorize_multi_issuer(
        &self,
        request: AuthorizeMultiIssuerRequest,
    ) -> Result<MultiIssuerAuthorizeResult, AuthorizeError> {
        self.authz.load().authorize_multi_issuer(&request)
    }

    /// Authorize a batch of multi-issuer requests against one shared token set.
    ///
    /// Validates tokens and builds token/issuer entities once, then evaluates
    /// each item with its own resource and context. Results are returned in
    /// input order, wrapped in a [`BatchAuthorizeResponse`] carrying a shared
    /// `batch_id`. Batch-level failures (validation, JWT verification,
    /// status-list refresh) return `Err`; per-item failures synthesize a
    /// fail-closed `Deny` without affecting other items.
    #[allow(clippy::unused_async)]
    pub async fn authorize_multi_issuer_batch(
        &self,
        request: BatchAuthorizeMultiIssuerRequest,
    ) -> Result<
        BatchAuthorizeResponse<Result<MultiIssuerAuthorizeResult, BatchItemError>>,
        AuthorizeError,
    > {
        self.authz.load().authorize_multi_issuer_batch(&request)
    }

    /// Returns metadata for all policies whose scope constraints are compatible
    /// with the given principals, actions, and resources.
    ///
    /// This performs scope-level filtering only (principal/action/resource constraints).
    /// Policies with `when`/`unless` conditions may still not apply at evaluation time.
    pub fn get_matching_policies_unsigned(
        &self,
        principal: Option<&EntityData>,
        actions: &[String],
        resources: &[EntityData],
    ) -> Result<Vec<PolicyMetadata>, AuthorizeError> {
        self.authz
            .load()
            .get_matching_policies_unsigned(principal, actions, resources)
    }

    /// Returns metadata for all policies whose scope constraints are compatible
    /// with the given token-derived principals, actions, and resources.
    ///
    /// Tokens are validated and their mapping types used as principal entity types.
    pub fn get_matching_policies_multi_issuer(
        &self,
        tokens: &[TokenInput],
        actions: &[String],
        resources: &[EntityData],
    ) -> Result<Vec<PolicyMetadata>, AuthorizeError> {
        self.authz
            .load()
            .get_matching_policies_multi_issuer(tokens, actions, resources)
    }

    /// Closes the connections to the Lock Server and pushes all available logs.
    pub async fn shut_down(&self) {
        self.log.shut_down().await;
    }
}

impl TrustedIssuerLoadingInfo for Cedarling {
    fn is_trusted_issuer_loaded_by_name(&self, issuer_id: &str) -> bool {
        self.authz
            .load()
            .is_trusted_issuer_loaded_by_name(issuer_id)
    }

    fn is_trusted_issuer_loaded_by_iss(&self, iss_claim: &str) -> bool {
        self.authz.load().is_trusted_issuer_loaded_by_iss(iss_claim)
    }

    fn total_issuers(&self) -> usize {
        self.authz.load().total_issuers()
    }

    fn loaded_trusted_issuers_count(&self) -> usize {
        self.authz.load().loaded_trusted_issuers_count()
    }

    fn loaded_trusted_issuer_ids(&self) -> HashSet<String> {
        self.authz.load().loaded_trusted_issuer_ids()
    }

    fn failed_trusted_issuer_ids(&self) -> HashSet<String> {
        self.authz.load().failed_trusted_issuer_ids()
    }
}

/// Build the HTTP client and load the policy store from the bootstrap config.
/// Returns the parts the rest of `Cedarling::new` needs: the [`ServiceConfig`]
/// for service-factory construction, and the [`RefreshWorkerSeed`] for the
/// refresh worker's first-tick short-circuit. Wraps both fallible steps so
/// the existing "configuration parsed successfully" / "...with error" log
/// behavior covers the whole bootstrap unit.
async fn perform_bootstrap_load(
    config: &BootstrapConfig,
    log: &log::Logger,
) -> Result<(ServiceConfig, RefreshWorkerSeed), ServiceConfigError> {
    let raw_load: Result<(http::HttpClient, LoadedPolicyStore), ServiceConfigError> = async {
        let http_client = http::HttpClient::new(config.http_client_config)?;
        let loaded = load_policy_store(
            &config.policy_store_config,
            &http_client,
            config.authorization_config.strict_schema_validation,
        )
        .await?;
        Ok((http_client, loaded))
    }
    .await;

    let (http_client, loaded) = raw_load
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

    let LoadedPolicyStore {
        store: policy_store,
        body_hash,
        validators,
    } = loaded;
    Ok((
        ServiceConfig {
            policy_store,
            http_client,
        },
        RefreshWorkerSeed {
            initial_body_hash: body_hash,
            initial_validators: validators,
        },
    ))
}

/// Spawn the background policy-store refresh worker if the source is a remote
/// URL and a non-zero refresh interval was configured. Returns `None` for
/// local sources or when refresh is disabled. The `seed` carries the
/// `body_hash` and `validators` captured during initial bootstrap so the
/// first periodic tick can short-circuit — passed in directly from the
/// bootstrap-load result rather than detoured through `ServiceFactory`.
fn maybe_spawn_refresh_worker(
    config: &BootstrapConfig,
    service_factory: &ServiceFactory<'_>,
    authz_swap: Arc<arc_swap::ArcSwap<authz::Authz>>,
    log: log::Logger,
    data: Arc<context_data_api::DataStore>,
    metrics: Arc<authz::metrics::MetricsCollector>,
    seed: RefreshWorkerSeed,
) -> Option<Arc<PolicyStoreRefreshHandle>> {
    if !config.policy_store_config.refresh_enabled() {
        return None;
    }
    let source = RefreshSource::from_policy_store_source(&config.policy_store_config.source)?;
    let (interval_secs, clamped) = config.policy_store_config.effective_refresh_interval();
    if clamped {
        log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(LogLevel::WARN, None))
                .set_message(format!(
                    "CEDARLING_POLICY_STORE_REFRESH_INTERVAL={} is below the minimum; clamped to {} seconds",
                    config.policy_store_config.refresh_interval_secs,
                    interval_secs,
                )),
        );
    }
    let rebuilder = AuthzRebuilder {
        jwt_config: config.jwt_config.clone(),
        authorization_config: config.authorization_config.clone(),
        http_client: service_factory.http_client_for_refresh(),
        log: log.clone(),
        data_store: data,
        metrics: metrics.clone(),
    };
    let ctx = WorkerContext {
        source,
        interval_secs,
        http_client: service_factory.http_client_for_refresh(),
        rebuilder,
        authz_swap,
        metrics,
        log,
        initial_body_hash: seed.initial_body_hash,
        initial_validators: seed.initial_validators,
        strict_schema_validation: config.authorization_config.strict_schema_validation,
    };
    Some(Arc::new(spawn_refresh_worker(ctx)))
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
// Helper function to calculate capacity usage and check memory alert threshold
fn calculate_capacity_usage(
    entry_count: usize,
    max_entries: usize,
    memory_alert_threshold: f64,
) -> (f64, bool) {
    // Precision loss is acceptable for percentage calculation
    #[allow(clippy::cast_precision_loss)]
    let capacity_usage_percent = if max_entries > 0 {
        (entry_count as f64 / max_entries as f64) * 100.0
    } else {
        0.0 // Unlimited capacity, no percentage
    };
    let memory_alert_triggered = capacity_usage_percent >= memory_alert_threshold;
    (capacity_usage_percent, memory_alert_triggered)
}

// provides public interface for pushing and retrieving data
impl DataApi for Cedarling {
    fn push_data_ctx(
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
            let (capacity_usage_percent, memory_alert_triggered) = calculate_capacity_usage(
                entry_count,
                config.max_entries,
                config.memory_alert_threshold,
            );
            if memory_alert_triggered {
                let log_entry = LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "DataStore memory usage alert: {:.1}% capacity used ({}/{} entries), threshold: {:.1}%",
                    capacity_usage_percent,
                    entry_count,
                    config.max_entries,
                    config.memory_alert_threshold
                ));
                self.log.log_any(log_entry);
            }
        }

        Ok(())
    }

    fn get_data_ctx(&self, key: &str) -> Result<Option<serde_json::Value>, DataError> {
        Ok(self.data.get(key))
    }

    fn get_data_entry_ctx(&self, key: &str) -> Result<Option<DataEntry>, DataError> {
        Ok(self.data.get_entry(key))
    }

    fn remove_data_ctx(&self, key: &str) -> Result<bool, DataError> {
        Ok(self.data.remove(key))
    }

    fn clear_data_ctx(&self) -> Result<(), DataError> {
        self.data.clear();
        Ok(())
    }

    fn list_data_ctx(&self) -> Result<Vec<DataEntry>, DataError> {
        Ok(self.data.list_entries())
    }

    fn get_stats_ctx(&self) -> Result<DataStoreStats, DataError> {
        let config = self.data.config();
        let entry_count = self.data.count();
        let total_size_bytes = self.data.total_size();
        let avg_entry_size_bytes = total_size_bytes.checked_div(entry_count).unwrap_or(0);

        // Calculate capacity usage percentage
        let (capacity_usage_percent, memory_alert_triggered) = calculate_capacity_usage(
            entry_count,
            config.max_entries,
            config.memory_alert_threshold,
        );

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
