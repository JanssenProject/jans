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
use crate::trace::TraceRecorder;
use std::collections::HashMap;
use std::sync::Arc;
#[cfg(not(target_arch = "wasm32"))]
use std::{fs, io, path::PathBuf};

#[cfg(not(target_arch = "wasm32"))]
use jans_trace_core::signature::{PrivateKeyError, TraceSigner as _};

#[cfg(not(target_arch = "wasm32"))]
use crate::app_types::PdpID;
#[cfg(not(target_arch = "wasm32"))]
use crate::bootstrap_config::TraceSigningKey;
#[cfg(not(target_arch = "wasm32"))]
use crate::lock::TraceSink;
#[cfg(not(target_arch = "wasm32"))]
use crate::trace::{EmitterHandle, EmitterInitError, EmitterParams, ProducerKey, spawn_emitter};

#[derive(Clone)]
pub(crate) struct ServiceFactory<'a> {
    bootstrap_config: &'a BootstrapConfig,
    service_config: ServiceConfig,
    log_service: log::Logger,
    data_store: Arc<DataStore>,
    metrics: Arc<MetricsCollector>,
    trace: Option<TraceRecorder>,
    /// The Lock TRACE endpoint client; `None` when Lock has no TRACE endpoint.
    #[cfg(not(target_arch = "wasm32"))]
    trace_sink: Option<Arc<TraceSink>>,
    #[cfg(not(target_arch = "wasm32"))]
    pdp_id: PdpID,
    container: SingletonContainer,
}

/// What [`ServiceFactory::new`] builds the services from.
pub(crate) struct ServiceFactoryParams<'a> {
    pub(crate) bootstrap_config: &'a BootstrapConfig,
    pub(crate) service_config: ServiceConfig,
    pub(crate) log_service: log::Logger,
    pub(crate) data_store: Arc<DataStore>,
    pub(crate) metrics: Arc<MetricsCollector>,
    /// The Lock TRACE endpoint client; `None` when Lock has no TRACE endpoint.
    #[cfg(not(target_arch = "wasm32"))]
    pub(crate) trace_sink: Option<Arc<TraceSink>>,
    #[cfg(not(target_arch = "wasm32"))]
    pub(crate) pdp_id: PdpID,
}

/// Caches the lazily-built `Authz` singleton so repeated calls to
/// `authz_service` are cheap.
#[derive(Clone, Default)]
struct SingletonContainer {
    authz_service: Option<Arc<Authz>>,
}

impl<'a> ServiceFactory<'a> {
    /// Create new instance of [`ServiceFactory`].
    pub(crate) fn new(params: ServiceFactoryParams<'a>) -> Self {
        let ServiceFactoryParams {
            bootstrap_config,
            service_config,
            log_service,
            data_store,
            metrics,
            #[cfg(not(target_arch = "wasm32"))]
            trace_sink,
            #[cfg(not(target_arch = "wasm32"))]
            pdp_id,
        } = params;
        Self {
            bootstrap_config,
            service_config,
            log_service,
            data_store,
            metrics,
            trace: None,
            #[cfg(not(target_arch = "wasm32"))]
            trace_sink,
            #[cfg(not(target_arch = "wasm32"))]
            pdp_id,
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
            self.trace.clone(),
        )
        .await?;

        let service = Arc::new(authz);
        self.container.authz_service = Some(service.clone());
        Ok(service)
    }
}

#[cfg(not(target_arch = "wasm32"))]
impl ServiceFactory<'_> {
    /// Starts TRACE: loads the signing key, starts the emitter on the Lock TRACE
    /// endpoint and gives the `Authz` built next its recorder. Returns the
    /// emitter, which `Cedarling` stops on shut down; `None` when TRACE is
    /// disabled or Lock has no TRACE endpoint, so decisions only report
    /// `not_emitted_no_endpoint`.
    ///
    /// The queue between the authorize path and the emitter holds as many
    /// records as the Lock log channel holds log entries, and a request is
    /// retried as often as a log batch.
    pub(crate) async fn start_trace(
        &mut self,
    ) -> Result<Option<Arc<EmitterHandle>>, StartTraceError> {
        let Some(key) = self.trace_key().await? else {
            return Ok(None);
        };
        let config = self.bootstrap_config;
        // `trace_key` loads a key only when TRACE is configured, and
        // `Cedarling::new` checks first that TRACE has Lock.
        let Some(trace) = &config.trace_config else {
            return Ok(None);
        };
        let Some(lock) = &config.lock_config else {
            return Ok(None);
        };
        let log = self.log_service.clone();
        let metrics = self.metrics.clone();
        let Some(sink) = self.trace_sink.clone() else {
            self.trace = Some(TraceRecorder::new(None, log, metrics));
            return Ok(None);
        };
        let (tx, rx) = tokio::sync::mpsc::channel(lock.log_channel_capacity.get());
        let emitter = spawn_emitter(
            EmitterParams {
                producer_id: trace.producer_id.clone(),
                pdp_id: self.pdp_id,
                signer: key,
                sink,
                bulk_max_records: trace.bulk_max_records,
                bulk_max_bytes: trace.bulk_max_bytes,
                max_retries: lock.log_max_retries,
                log: log.clone(),
                metrics: metrics.clone(),
            },
            rx,
        )?;
        self.trace = Some(TraceRecorder::new(Some(tx), log, metrics));
        Ok(Some(Arc::new(emitter)))
    }

    /// Loads the TRACE signing key; `None` when TRACE is disabled. Runs at
    /// startup so a bad key stops `Cedarling::new` instead of failing every
    /// record later, and logs the `kid` and thumbprint the admin registers in Lock.
    async fn trace_key(&self) -> Result<Option<Arc<ProducerKey>>, LoadProducerKeyError> {
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

/// Why TRACE did not start.
#[cfg(not(target_arch = "wasm32"))]
#[derive(Debug, thiserror::Error)]
pub(crate) enum StartTraceError {
    #[error(transparent)]
    Key(#[from] LoadProducerKeyError),
    #[error(transparent)]
    Emitter(#[from] EmitterInitError),
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
