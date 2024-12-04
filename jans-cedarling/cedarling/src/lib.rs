/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
#![deny(missing_docs)]
//! # Cedarling
//! The Cedarling is a performant local authorization service that runs the Rust Cedar Engine.
//! Cedar policies and schema are loaded at startup from a locally cached "Policy Store".
//! In simple terms, the Cedarling returns the answer: should the application allow this action on this resource given these JWT tokens.
//! "Fit for purpose" policies help developers build a better user experience.
//! For example, why display form fields that a user is not authorized to see?
//! The Cedarling is a more productive and flexible way to handle authorization.

mod http;
mod authz;
mod bootstrap_config;
mod common;
mod init;
mod jwt;
mod lock;
mod log;

#[doc(hidden)]
#[cfg(test)]
mod tests;

use std::sync::Arc;

pub use authz::request::{Request, ResourceData};
pub use authz::{AuthorizeError, AuthorizeResult};
use authz::{Authz, AuthzInitError};
pub use bootstrap_config::*;
use init::service_config::{ServiceConfig, ServiceConfigError};
use init::ServiceFactory;

use common::app_types;
use log::LogEntry;
pub use log::LogStorage;
use log::LogType;

#[cfg(test)]
use authz::AuthorizeEntitiesData;

#[doc(hidden)]
pub mod bindings {
    pub use super::log::{
        AuthorizationLogInfo, Decision, Diagnostics, LogEntry, PolicyEvaluationError,
    };
    pub use crate::common::policy_store::PolicyStore;
    pub use cedar_policy;
}

/// Errors that can occur during initialization Cedarling.
#[derive(Debug, thiserror::Error)]
pub enum InitCedarlingError {
    /// Error while preparing config for internal services
    #[error(transparent)]
    ServiceConfig(#[from] ServiceConfigError),
    /// Error while initializeing AuthZ module
    #[error(transparent)]
    AuthzInit(#[from] AuthzInitError),
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
    pub fn new(config: BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let log = log::init_logger(&config.log_config);
        let pdp_id = app_types::PdpID::new();

        let service_config = ServiceConfig::new(&config)
            .inspect(|_| {
                log.log(
                    LogEntry::new_with_data(pdp_id, None, LogType::System)
                        .set_message("configuration parsed successfully".to_string()),
                )
            })
            .inspect_err(|err| {
                log.log(
                    LogEntry::new_with_data(pdp_id, None, LogType::System)
                        .set_error(err.to_string())
                        .set_message("configuration parsed with error".to_string()),
                )
            })?;

        let mut service_factory = ServiceFactory::new(&config, service_config, log.clone(), pdp_id);

        Ok(Cedarling {
            log,
            authz: service_factory.authz_service()?,
        })
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.authorize(request)
    }

    /// Get entites derived from `cedar-policy` schema and tokens for `authorize` request.
    #[doc(hidden)]
    #[cfg(test)]
    pub fn authorize_entities_data(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        self.authz.authorize_entities_data(request)
    }
}

// implements LogStorage for Cedarling
// we can use this methods outside crate only when import trait
impl LogStorage for Cedarling {
    fn pop_logs(&self) -> Vec<LogEntry> {
        self.log.pop_logs()
    }

    fn get_log_by_id(&self, id: &str) -> Option<LogEntry> {
        self.log.get_log_by_id(id)
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.log.get_log_ids()
    }
}
