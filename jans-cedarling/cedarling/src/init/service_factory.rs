/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module to lazily initialize internal cedarling services

use super::authz_builder::{BuildAuthzError, build_authz};
use super::service_config::ServiceConfig;
use crate::LogLevel;
use crate::authz::metrics::MetricsCollector;
use crate::authz::Authz;
use crate::bootstrap_config::BootstrapConfig;
use crate::common::policy_store::PolicyStoreMetadata;
use crate::context_data_api::DataStore;
use crate::log::interface::LogWriter;
use crate::log::{self, BaseLogEntry, LogEntry};
use std::collections::HashMap;
use std::sync::Arc;

#[derive(Clone)]
pub(crate) struct ServiceFactory<'a> {
    bootstrap_config: &'a BootstrapConfig,
    service_config: ServiceConfig,
    log_service: log::Logger,
    data_store: Arc<DataStore>,
    metrics: Arc<MetricsCollector>,
    container: SingletonContainer,
}

/// Caches the lazily-built `Authz` singleton so repeated calls to
/// `authz_service` are cheap.
#[derive(Clone, Default)]
struct SingletonContainer {
    authz_service: Option<Arc<Authz>>,
}

impl<'a> ServiceFactory<'a> {
    /// Create new instance of [`ServiceFactory`].
    pub(crate) fn new(
        bootstrap_config: &'a BootstrapConfig,
        service_config: ServiceConfig,
        log_service: log::Logger,
        data_store: Arc<DataStore>,
        metrics: Arc<MetricsCollector>,
    ) -> Self {
        Self {
            bootstrap_config,
            service_config,
            log_service,
            data_store,
            metrics,
            container: SingletonContainer::default(),
        }
    }

    /// Get the policy store metadata if available.
    ///
    /// Metadata is only available when the policy store is loaded from the new
    /// directory/archive format. Legacy JSON/YAML formats do not include metadata.
    pub(crate) fn policy_store_metadata(&self) -> Option<&PolicyStoreMetadata> {
        self.service_config.policy_store.metadata.as_ref()
    }

    /// Returns a clone of the HTTP client used during service initialization.
    /// Exposed so the policy-store refresh worker can reuse the same client
    /// (and therefore the same timeout / retry configuration) for its periodic
    /// fetches.
    pub(crate) fn http_client_for_refresh(&self) -> crate::http::HttpClient {
        self.service_config.http_client.clone()
    }

    // get log service
    fn log_service(&mut self) -> log::Logger {
        self.log_service.clone()
    }

    /// Build and cache the [`Authz`] service.
    ///
    /// The construction is delegated to the shared [`build_authz`] path which
    /// also runs on every refresh-worker rebuild. Bootstrap-specific logging
    /// (configuration warnings, policy-count summary) is emitted here, before
    /// `build_authz` is called, so it only appears at startup.
    pub(crate) async fn authz_service(&mut self) -> Result<Arc<Authz>, ServiceInitError> {
        if let Some(authz) = &self.container.authz_service {
            return Ok(authz.clone());
        }

        let logger = self.log_service();
        let policy_store = &self.service_config.policy_store;

        // Bootstrap-only: warn once when strict schema validation is disabled.
        if !self.bootstrap_config.authorization_config.strict_schema_validation {
            let msg = if policy_store.schema.is_some() {
                "CEDARLING_STRICT_SCHEMA_VALIDATION is disabled — schema present but not enforced"
            } else {
                "CEDARLING_STRICT_SCHEMA_VALIDATION is disabled — no schema loaded; policies run without attribute validation"
            };
            logger.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(LogLevel::WARN, None))
                    .set_message(msg.to_string()),
            );
        }

        // Bootstrap-only: log a one-time summary of what was loaded.
        logger.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(LogLevel::INFO, None))
                .set_message(format!(
                    "Policy store loaded: {} policies, {} issuers, {} entities",
                    policy_store.policies.get_set().policies().count(),
                    policy_store
                        .trusted_issuers
                        .as_ref()
                        .map_or(0, HashMap::len),
                    policy_store.default_entities.entities().len(),
                )),
        );

        let authz = build_authz(
            self.service_config.policy_store.clone(),
            &self.bootstrap_config.jwt_config,
            &self.bootstrap_config.authorization_config,
            self.service_config.http_client.clone(),
            &logger,
            self.data_store.clone(),
            self.metrics.clone(),
        )
        .await?;

        let service = Arc::new(authz);
        self.container.authz_service = Some(service.clone());
        Ok(service)
    }
}

/// Error type for failing to initialize a service
#[derive(Debug, thiserror::Error)]
pub enum ServiceInitError {
    #[error("{0}")]
    AuthzInit(String),
}

impl From<BuildAuthzError> for ServiceInitError {
    fn from(e: BuildAuthzError) -> Self {
        ServiceInitError::AuthzInit(e.to_string())
    }
}
