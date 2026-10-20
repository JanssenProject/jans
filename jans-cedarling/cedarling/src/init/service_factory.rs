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
use crate::authz::Authz;
use crate::authz::metrics::MetricsCollector;
use crate::bootstrap_config::BootstrapConfig;
use crate::common::policy_store::PolicyStoreMetadata;
use crate::context_data_api::DataStore;
use crate::log::interface::LogWriter;
use crate::log::{self, BaseLogEntry, LogEntry};
use std::collections::HashMap;
use std::sync::Arc;
#[cfg(not(target_arch = "wasm32"))]
use std::{fs, io, path::PathBuf};

#[cfg(not(target_arch = "wasm32"))]
use jans_trace_core::signature::{PrivateKeyError, TraceSigner as _};

#[cfg(not(target_arch = "wasm32"))]
use crate::bootstrap_config::TraceSigningKey;
#[cfg(not(target_arch = "wasm32"))]
use crate::trace::ProducerKey;

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

    /// `bundle_hash` of the loaded store, when it came from a `.cjar`.
    pub(crate) fn policy_store_bundle_hash(&self) -> Option<jans_trace_core::Digest> {
        self.service_config.policy_store.bundle_hash()
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

    pub(crate) async fn authz_service(&mut self) -> Result<Arc<Authz>, ServiceInitError> {
        if let Some(authz) = &self.container.authz_service {
            return Ok(authz.clone());
        }

        let logger = self.log_service();
        let policy_store = &self.service_config.policy_store;

        // Log warns that some default entities loaded not correctly — once at startup.
        for warn in policy_store.default_entities.warns() {
            logger.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(warn.to_string()),
            );
        }

        // warn once at startup when strict schema validation is disabled
        if !self
            .bootstrap_config
            .authorization_config
            .strict_schema_validation
        {
            let msg = if policy_store.schema_source_exists {
                "CEDARLING_STRICT_SCHEMA_VALIDATION is disabled — schema present but not enforced"
            } else {
                "CEDARLING_STRICT_SCHEMA_VALIDATION is disabled — no schema loaded; policies run without attribute validation"
            };
            logger.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(msg.to_string()),
            );
        }

        // one-time startup summary
        logger.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::INFO,
                None,
            ))
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
            None,
        )
        .await?;

        let service = Arc::new(authz);
        self.container.authz_service = Some(service.clone());
        Ok(service)
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl ServiceFactory<'_> {
    /// Loads the TRACE signing key; `None` when TRACE is disabled. Runs at
    /// startup so a bad key stops `Cedarling::new` instead of failing every
    /// record later, and logs the `kid` and thumbprint the admin registers in Lock.
    pub(crate) async fn trace_key(&self) -> Result<Option<Arc<ProducerKey>>, LoadProducerKeyError> {
        let Some(trace) = &self.bootstrap_config.trace_config else {
            return Ok(None);
        };
        let (pem, path) = match &trace.signing_key {
            TraceSigningKey::File(path) => (read_key_file(path.clone()).await?, Some(path)),
            TraceSigningKey::Pem(pem) => (pem.clone(), None),
        };
        let key = ProducerKey::from_pem(&pem, trace.signing_kid.as_ref()).map_err(|source| {
            LoadProducerKeyError::InvalidKey {
                property: trace.signing_key.property(),
                path: path.cloned(),
                source: InvalidTraceKeyError(source),
            }
        })?;

        self.log_service.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::INFO,
                None,
            ))
            .set_message(format!(
                "TRACE producer key loaded: producer={} kid={} thumbprint={}",
                trace.producer_id,
                key.kid(),
                key.thumbprint()
            )),
        );
        Ok(Some(Arc::new(key)))
    }
}

/// Reads the key file on the blocking pool, so startup never blocks a runtime
/// worker on file I/O.
#[cfg(not(target_arch = "wasm32"))]
async fn read_key_file(path: PathBuf) -> Result<String, LoadProducerKeyError> {
    let task_path = path.clone();
    tokio::task::spawn_blocking(move || fs::read_to_string(task_path))
        .await
        // The task fails to join only if it panics or the runtime shuts down.
        .unwrap_or_else(|join| Err(io::Error::other(join)))
        .map_err(|source| LoadProducerKeyError::ReadFile { path, source })
}

/// Why the TRACE signing key could not be loaded. Messages name the property
/// and path, never key material.
#[cfg(not(target_arch = "wasm32"))]
#[derive(Debug, thiserror::Error)]
pub enum LoadProducerKeyError {
    /// The key file could not be read.
    #[error(
        "failed to read the TRACE signing key file `{}` (`CEDARLING_TRACE_SIGNING_KEY`): {source}",
        path.display()
    )]
    ReadFile {
        /// Path from `CEDARLING_TRACE_SIGNING_KEY`.
        path: PathBuf,
        /// I/O error from reading the file.
        #[source]
        source: io::Error,
    },
    /// The key is not an unencrypted PKCS#8 PEM Ed25519 private key.
    #[error(
        "the TRACE signing key from `{property}`{} is invalid: {source}",
        path.as_ref().map(|p| format!(" (`{}`)", p.display())).unwrap_or_default()
    )]
    InvalidKey {
        /// Property the key was configured with.
        property: &'static str,
        /// Key file, when the key came from a file.
        path: Option<PathBuf>,
        /// Why the key was rejected.
        #[source]
        source: InvalidTraceKeyError,
    },
}

/// Why a TRACE signing key is not a usable Ed25519 key. Wraps the core library
/// error so callers need no dependency on it; the text never carries key material.
#[cfg(not(target_arch = "wasm32"))]
#[derive(Debug, thiserror::Error)]
#[error(transparent)]
pub struct InvalidTraceKeyError(PrivateKeyError);

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
