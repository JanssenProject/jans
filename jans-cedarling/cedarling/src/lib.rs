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

mod authz;
mod init;
mod jwt;
mod lock;
mod log;
mod models;

#[doc(hidden)]
#[cfg(test)]
mod tests;

use std::sync::Arc;

pub use authz::AuthorizeError;
use authz::Authz;
use di::{DependencyMap, DependencySupplier};
use init::policy_store::{load_policy_store, LoadPolicyStoreError};
pub use jwt::CreateJwtServiceError;
pub use log::LogStorage;
use log::{init_logger, LogWriter};
use models::app_types;
pub use models::authorize_result::AuthorizeResult;
pub use models::config::*;
pub use models::log_entry::LogEntry;
use models::log_entry::LogType;
pub use models::request::{Request, ResourceData};

#[doc(hidden)]
pub mod bindings {
    pub use super::models::log_entry::{
        AuthorizationLogInfo, Decision, Diagnostics, PolicyEvaluationError,
    };
    pub use cedar_policy;
}

/// Errors that can occur during initialization Cedarling.
#[derive(Debug, thiserror::Error)]
pub enum InitCedarlingError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] LoadPolicyStoreError),
    /// Error that may occur during loading the JWT service.
    #[error("Could not load JWT service: {0}")]
    JWT(#[from] CreateJwtServiceError),
}

/// The instance of the Cedarling application.
/// It is safe to share between threads.
#[derive(Clone)]
pub struct Cedarling {
    log: log::Logger,
    #[allow(dead_code)]
    authz: Arc<Authz>,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub fn new(config: BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let mut container: DependencyMap = DependencyMap::new();

        container.insert(init_logger(config.log_config));
        let log: log::Logger = container.get();

        // we use uuid v4 because it is generated based on random numbers.

        container.insert(app_types::PdpID::new());

        container.insert(app_types::ApplicationName(config.application_name));

        let policy_store = load_policy_store(config.policy_store_config)
           // Log success when loading the policy store
            .inspect(|_| {
                log.log(
                    LogEntry::new_with_container(&container, LogType::System)
                        .set_message("PolicyStore loaded successfully".to_string()),
                );
            })
            // Log failure when loading the policy store
            .inspect_err(|err| {
                log.log(
                    LogEntry::new_with_container(&container, LogType::System)
                        .set_message(format!("Could not load PolicyStore: {}", err)),
                )
            })?;
        container.insert(policy_store);

        let jwt_service = jwt::JwtService::new_with_container(&container, config.jwt_config)?;
        log.log(
            LogEntry::new_with_container(&container, LogType::System)
                .set_message("JWT service loaded successfully".to_string()),
        );
        container.insert(jwt_service);

        let authz = Authz::new_with_container(&container);

        Ok(Cedarling {
            log,
            authz: Arc::new(authz),
        })
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        self.authz.authorize(request)
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
