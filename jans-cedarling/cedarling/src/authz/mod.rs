/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! # Auth Engine
//! Part of Cedarling that main purpose is:
//! - evaluate if authorization is granted for *user*
//! - evaluate if authorization is granted for *client*

use crate::log::{LogWriter, Logger};
use crate::models::authz_config::AuthzConfig;
use crate::models::log_entry::{LogEntry, LogType};
use uuid7::{uuid4, Uuid};

/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
#[allow(dead_code)]
pub struct Authz {
    log_service: Logger,
    pdp_id: Uuid,
    application_name: String,
}

impl Authz {
    /// Create a new Authorization Service
    pub fn new(config: AuthzConfig, log: Logger) -> Self {
        // we use uuid v4 because it is generated based on random numbers.
        let pdp_id = uuid4();
        let application_name = config.application_name;

        log.log(
            LogEntry::new_with_data(pdp_id, application_name.clone(), LogType::System)
                .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            log_service: log,
            pdp_id,
            application_name,
        }
    }
}
