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
use log::init_logger;
pub use log::LogStorage;
pub use models::config::*;

/// The instance of the Cedarling application.
#[derive(Clone)]
pub struct Cedarling {
    log: log::Logger,
    #[allow(dead_code)]
    authz: Rc<Authz>,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub fn new(config: BootstrapConfig) -> Cedarling {
        let log = init_logger(config.log_config);
        let authz = Authz::new(config.authz_config, log.clone());

        Cedarling {
            log,
            authz: Rc::new(authz),
        }
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
