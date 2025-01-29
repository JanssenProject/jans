// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![deny(missing_docs)]
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

use std::sync::Arc;

#[cfg(test)]
use authz::AuthorizeEntitiesData;
use authz::Authz;
pub use authz::request::{Request, ResourceData, Tokens};
pub use authz::{AuthorizeError, AuthorizeResult};
pub use bootstrap_config::*;
use common::app_types;
use init::ServiceFactory;
use init::service_config::{ServiceConfig, ServiceConfigError};
use init::service_factory::ServiceInitError;
use log::interface::LogWriter;
use log::{LogEntry, LogType};
pub use log::{LogLevel, LogStorage};

#[doc(hidden)]
pub mod bindings {
    pub use cedar_policy;

    pub use super::log::{
        AuthorizationLogInfo, Decision, Diagnostics, LogEntry, PolicyEvaluationError,
    };
    pub use crate::common::policy_store::PolicyStore;
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
}

/// The instance of the Cedarling application.
/// It is safe to share between threads.
#[derive(Clone)]
pub struct Cedarling {
    log: log::Logger,
    authz: Arc<Authz>,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub async fn new(config: &BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let log = log::init_logger(&config.log_config);
        let pdp_id = app_types::PdpID::new();

        let service_config = ServiceConfig::new(config)
            .await
            .inspect(|_| {
                log.log_any(
                    LogEntry::new_with_data(pdp_id, None, LogType::System, None)
                        .set_level(LogLevel::DEBUG)
                        .set_message("configuration parsed successfully".to_string()),
                )
            })
            .inspect_err(|err| {
                log.log_any(
                    LogEntry::new_with_data(pdp_id, None, LogType::System, None)
                        .set_error(err.to_string())
                        .set_level(LogLevel::ERROR)
                        .set_message("configuration parsed with error".to_string()),
                )
            })?;

        let mut service_factory = ServiceFactory::new(config, service_config, log.clone(), pdp_id);

        Ok(Cedarling {
            log,
            authz: service_factory.authz_service().await?,
        })
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub async fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.authorize(request).await
    }

    /// Get entites derived from `cedar-policy` schema and tokens for `authorize` request.
    #[doc(hidden)]
    #[cfg(test)]
    pub async fn build_entities(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        let tokens = self.authz.decode_tokens(request).await?;
        self.authz.build_entities(request, &tokens)
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
