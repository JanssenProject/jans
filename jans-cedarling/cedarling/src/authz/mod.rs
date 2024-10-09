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

use std::sync::Arc;

use crate::jwt::DecodeJwtError;
use crate::jwt::JwtService;
use crate::log::{LogWriter, Logger};
use crate::models::app_types;
use crate::models::log_entry::{LogEntry, LogType};
use crate::models::policy_store::PolicyStore;
use crate::models::request::Request;

mod entities;

use di::DependencySupplier;
use entities::{create_access_token_entities, AccessTokenEntitiesError};

/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
#[allow(dead_code)]
pub struct Authz {
    log_service: Logger,
    pdp_id: app_types::PdpID,
    application_name: app_types::ApplicationName,
    policy_store: PolicyStore,
    jwt_service: Arc<JwtService>,
}

impl Authz {
    /// Create a new Authorization Service
    pub(crate) fn new_with_container(dep_map: &di::DependencyMap) -> Self {
        let application_name: Arc<app_types::ApplicationName> = dep_map.get();
        let pdp_id = *dep_map.get();
        let log: Logger = dep_map.get();
        let policy_store: Arc<PolicyStore> = dep_map.get();
        let jwt_service: Arc<JwtService> = dep_map.get();

        log.log(
            LogEntry::new_with_data(pdp_id, application_name.as_ref().clone(), LogType::System)
                .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            log_service: log,
            pdp_id,
            application_name: application_name.as_ref().clone(),
            policy_store: policy_store.as_ref().clone(),
            jwt_service,
        }
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *client*
    //
    // this function will be finished in next issue
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
