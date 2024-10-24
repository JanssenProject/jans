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

use std::str::FromStr;
use std::sync::Arc;

use crate::jwt;
use crate::log::{AuthorizationLogInfo, LogEntry, LogType, Logger};
use crate::common::app_types;
use crate::common::policy_store::PolicyStore;

mod authorize_result;
mod entities;
pub(crate) mod request;
mod token_data;

pub use authorize_result::AuthorizeResult;
use entities::create_resource_entity;
use entities::ResourceEntityError;
use entities::{create_access_token_entities, AccessTokenEntitiesError};
use request::Request;
use token_data::TokenPayload;

/// Configuration to Authz to initialize service without errors
pub(crate) struct AuthzConfig {
    pub log_service: Logger,
    pub pdp_id: app_types::PdpID,
    pub application_name: app_types::ApplicationName,
    pub policy_store: PolicyStore,
    pub jwt_service: Arc<jwt::JwtService>,
}

/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
#[allow(dead_code)]
pub struct Authz {
    config: AuthzConfig,
}

impl Authz {
    /// Create a new Authorization Service
    pub(crate) fn new(config: AuthzConfig) -> Self {
        config.log_service.log(
            LogEntry::new_with_data(
                config.pdp_id,
                Some(config.application_name.clone()),
                LogType::System,
            )
            .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self { config }
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *client*
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        let (access_token, _id_token) = self
            .config
            .jwt_service
            .decode_tokens::<TokenPayload, TokenPayload>(
                &request.access_token,
                &request.id_token,
            )?;

        let access_token_entities =
            create_access_token_entities(&self.config.policy_store.schema.json, &access_token)?;

        // TODO: check if `request.userinfo_token.sub` == `id_token.sub`
        // Note that "Userinfo Token" isn't a JWT which is why we're not
        // passing it to JWT Service. It's usually a JSON you can GET from the
        // userinfo endpoint. For example:
        // `curl -X GET 'https://openidconnect.googleapis.com/v1/userinfo' -H 'Authorization: Bearer ACCESS_TOKEN'`

        let resource_entity =
            create_resource_entity(request.resource, &self.config.policy_store.schema.json)?;

        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        let context: cedar_policy::Context = cedar_policy::Context::from_json_value(
            request.context.clone(),
            Some((&self.config.policy_store.schema.schema, &action)),
        )?;

        let principal_workload_uid = access_token_entities.workload_entity.uid();
        let resource_uid = resource_entity.uid();

        let cedar_request = cedar_policy::Request::new(
            principal_workload_uid.clone(),
            action,
            resource_uid.clone(),
            context,
            Some(&self.config.policy_store.schema.schema),
        )?;

        // collect all entities
        let entities_iterator = access_token_entities
            .entities()
            .into_iter()
            .chain(vec![resource_entity]);

        let entities = cedar_policy::Entities::from_entities(
            entities_iterator,
            Some(&self.config.policy_store.schema.schema),
        )?;

        let authorizer = cedar_policy::Authorizer::new();
        let decision = authorizer.is_authorized(
            &cedar_request,
            &self.config.policy_store.policies,
            &entities,
        );

        self.config.log_service.as_ref().log(
            LogEntry::new_with_data(
                self.config.pdp_id,
                Some(self.config.application_name.clone()),
                LogType::Decision,
            )
            .set_auth_info(AuthorizationLogInfo {
                action: request.action.clone(),
                context: request.context,
                decision: decision.decision().into(),
                principal: principal_workload_uid.to_string(),
                diagnostics: decision.diagnostics().into(),
                resource: resource_uid.to_string(),
            })
            .set_message("Result of authorize with resource as workload entity".to_string()),
        );

        Ok(AuthorizeResult { workload: decision })
    }
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while parsing JWT data
    #[error("Error while parsing JWT: {0}")]
    JWT(#[from] jwt::Error),
    /// Error encountered while creating access token entities
    #[error("{0}")]
    AccessTokenEntities(#[from] AccessTokenEntitiesError),
    /// Error encountered while creating resource entity
    #[error("{0}")]
    ResourceEntity(#[from] ResourceEntityError),
    /// Error encountered while parsing Action to EntityUid
    #[error("could not parse action: {0}")]
    Action(cedar_policy::ParseErrors),
    /// Error encountered while validating context according to the schema
    #[error("could not create context: {0}")]
    CreateContext(#[from] cedar_policy::ContextJsonError),
    /// Error encountered while creating [`cedar_policy::Request`]
    #[error("could not create request type: {0}")]
    CreateRequest(#[from] cedar_policy::RequestValidationError),
    /// Error encountered while collecting all entities
    #[error("could not collect all entities: {0}")]
    Entities(#[from] cedar_policy::entities_errors::EntitiesError),
}
