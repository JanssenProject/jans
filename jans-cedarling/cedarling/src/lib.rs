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

use std::rc::Rc;

use authz::Authz;
use init::policy_store::{load_policy_store, LoadPolicyStoreError};
pub use log::LogStorage;
use log::{init_logger, LogWriter};
pub use models::config::*;
use models::log_entry::{LogEntry, LogType};
use uuid7::uuid4;

/// Errors that can occur during initialization Cedarling.
#[derive(Debug, thiserror::Error)]
pub enum InitCedarlingError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy :{0}")]
    PolicyStore(#[from] LoadPolicyStoreError),
}

/// The instance of the Cedarling application.
#[derive(Clone)]
pub struct Cedarling {
    log: log::Logger,
    #[allow(dead_code)]
    authz: Rc<Authz>,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub fn new(config: BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let log: Rc<log::LogStrategy> = init_logger(config.log_config);
        // we use uuid v4 because it is generated based on random numbers.
        let pdp_id = uuid4();
        let application_id = config.authz_config.application_name.clone();

        let policy_store = load_policy_store(config.policy_store_config)
           // Log success when loading the policy store
            .inspect(|_| {
                log.log(
                    LogEntry::new_with_data(pdp_id, application_id.clone(), LogType::System)
                        .set_message("PolicyStore loaded successfully".to_string()),
                );
            })
            // Log failure when loading the policy store
            .inspect_err(|err| {
                log.log(
                    LogEntry::new_with_data(pdp_id, application_id, LogType::System)
                        .set_message(format!("Could not load PolicyStore: {}", err.to_string())),
                )
            })?;

        let authz = Authz::new(config.authz_config, pdp_id, log.clone(), policy_store);

        Ok(Cedarling {
            log,
            authz: Rc::new(authz),
        })
    }
}

// implements LogStorage for Cedarling
// we can use this methods outside crate only when import trait
impl LogStorage for Cedarling {
    fn pop_logs(&self) -> Vec<models::log_entry::LogEntry> {
        self.log.pop_logs()
    }

    fn get_log_by_id(&self, id: &str) -> Option<models::log_entry::LogEntry> {
        self.log.get_log_by_id(id)
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.log.get_log_ids()
    }
}
