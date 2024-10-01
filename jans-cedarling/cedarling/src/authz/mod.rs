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

use crate::jwt::DecodeJwtError;
use crate::jwt::JwtService;
use crate::log::{LogWriter, Logger};
use crate::models::authz_config::AuthzConfig;
use crate::models::log_entry::{LogEntry, LogType};
use crate::models::policy_store::PolicyStore;
use crate::models::request::Request;
use uuid7::Uuid;

mod entities;

use entities::{create_access_token_entities, AccessTokenEntitiesError};

/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
#[allow(dead_code)]
pub struct Authz {
    log_service: Logger,
    pdp_id: Uuid,
    application_name: String,
    policy_store: PolicyStore,
    jwt_service: JwtService,
}

impl Authz {
    /// Create a new Authorization Service
    pub fn new(
        config: AuthzConfig,
        pdp_id: Uuid,
        log: Logger,
        policy_store: PolicyStore,
        jwt_service: JwtService,
    ) -> Self {
        let application_name = config.application_name;

        log.log(
            LogEntry::new_with_data(pdp_id, application_name.clone(), LogType::System)
                .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            log_service: log,
            pdp_id,
            application_name,
            policy_store,
            jwt_service,
        }
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *client*
    pub fn authorize(&self, request: &Request) -> Result<(), AuthorizeError> {
        #[allow(unused_variables)]
        let access_token_entities = create_access_token_entities(
            &self.jwt_service.decode_token_data(request.access_token)?,
        );

        Ok(())
    }
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while decoding JWT token data
    #[error("Malformed JWT provided")]
    JWT(#[from] DecodeJwtError),
    /// Error encountered while creating access token entities
    #[error("{0}")]
    AccessTokenEntities(#[from] AccessTokenEntitiesError),
}
