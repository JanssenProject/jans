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

use crate::jwt::JwtService;
use crate::log::{LogWriter, Logger};
use crate::models::app_types;
use crate::models::log_entry::AuthorizationLogInfo;
use crate::models::log_entry::{LogEntry, LogType};
use crate::models::policy_store::PolicyStore;
use crate::models::request::Request;
use crate::AuthorizeResult;

mod decode_tokens;
mod entities;

use cedar_policy::Entity;
use decode_tokens::{decode_tokens, DecodeTokensError};
use di::DependencySupplier;
use entities::CedarPolicyCreateTypeError;
use entities::ResourceEntityError;
use entities::{
    create_access_token_entities, id_token_entity, user_entity, AccessTokenEntitiesError,
};
use entities::{create_resource_entity, AccessTokenEntities};

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
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        let context: cedar_policy::Context = cedar_policy::Context::from_json_value(
            request.context.clone(),
            Some((&self.policy_store.schema.schema, &action)),
        )?;

        let entities_data = self.authorize_entities_data(&request)?;

        let principal_workload_uid = entities_data.access_token_entities.workload_entity.uid();
        let resource_uid = entities_data.resource_entity.uid();
        let principal_user_entity_uid = entities_data.user_entity.uid();

        let entities = entities_data.entities(Some(&self.policy_store.schema.schema))?;

        let authorizer = cedar_policy::Authorizer::new();

        let decision_principal_response = {
            // _____ check policy where principal is workload _____
            let request_principal_workload = cedar_policy::Request::new(
                principal_workload_uid.clone(),
                action.clone(),
                resource_uid.clone(),
                context.clone(),
                Some(&self.policy_store.schema.schema),
            )?;

            let decision_principal_workload = authorizer.is_authorized(
                &request_principal_workload,
                &self.policy_store.policies,
                &entities,
            );

            self.log_service.as_ref().log(
                LogEntry::new_with_data(
                    self.pdp_id,
                    self.application_name.clone(),
                    LogType::Decision,
                )
                .set_auth_info(AuthorizationLogInfo {
                    action: request.action.clone(),
                    context: request.context.clone(),
                    decision: decision_principal_workload.decision().into(),
                    principal: principal_workload_uid.to_string(),
                    diagnostics: decision_principal_workload.diagnostics().clone().into(),
                    resource: resource_uid.to_string(),
                })
                .set_message("Result of authorize with resource as workload entity".to_string()),
            );
            decision_principal_workload
        };

        let user_principal_response = {
            // _____ check policy where principal is user _____
            let request_user_workload = cedar_policy::Request::new(
                principal_user_entity_uid.clone(),
                action,
                resource_uid.clone(),
                context,
                Some(&self.policy_store.schema.schema),
            )?;

            let user_principal_workload = authorizer.is_authorized(
                &request_user_workload,
                &self.policy_store.policies,
                &entities,
            );

            self.log_service.as_ref().log(
                LogEntry::new_with_data(
                    self.pdp_id,
                    self.application_name.clone(),
                    LogType::Decision,
                )
                .set_auth_info(AuthorizationLogInfo {
                    action: request.action,
                    context: request.context,
                    decision: user_principal_workload.decision().into(),
                    principal: principal_workload_uid.to_string(),
                    diagnostics: user_principal_workload.diagnostics().clone().into(),
                    resource: resource_uid.to_string(),
                })
                .set_message("Result of authorize with resource as user entity".to_string()),
            );
            user_principal_workload
        };

        Ok(AuthorizeResult {
            workload: decision_principal_response,
            person: user_principal_response,
        })
    }

    /// Create all [`cedar_policy::Entity`]-s from [`Request`]
    fn authorize_entities_data(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        let decoded_tokens = decode_tokens(&request, self.jwt_service.as_ref())?;

        // Using the builder at compile time ensures all entities are added correctly
        let data = AuthorizeEntitiesData::builder()
            .access_token_entities(create_access_token_entities(
                &self.policy_store.schema.json,
                &decoded_tokens.access_token,
            )?)
            .id_token_entity(
                id_token_entity(&self.policy_store.schema.json, &decoded_tokens.id_token)
                    .map_err(AuthorizeError::CreateIdTokenEntity)?,
            )
            .user_entity(
                user_entity(
                    &self.policy_store.schema.json,
                    &decoded_tokens.id_token,
                    &decoded_tokens.userinfo,
                )
                .map_err(AuthorizeError::CreateUserEntity)?,
            )
            .resource_entity(create_resource_entity(
                request.resource.clone(),
                &self.policy_store.schema.json,
            )?);

        Ok(data.build())
    }
}

/// Structure to hold entites created from tokens
//
// we can't use simple vector because we need use uid-s
// from some entities to check authorizations
#[derive(typed_builder::TypedBuilder)]
struct AuthorizeEntitiesData {
    access_token_entities: AccessTokenEntities,
    id_token_entity: Entity,
    user_entity: Entity,
    resource_entity: Entity,
}

impl AuthorizeEntitiesData {
    /// Create iterator to get all entities
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        let iter = vec![self.id_token_entity, self.user_entity, self.resource_entity].into_iter();

        self.access_token_entities.into_iter().chain(iter)
    }

    /// Collect all entities to [`cedar_policy::Entities`]
    fn entities(
        self,
        schema: Option<&cedar_policy::Schema>,
    ) -> Result<cedar_policy::Entities, cedar_policy::entities_errors::EntitiesError> {
        cedar_policy::Entities::from_entities(self.into_iter(), schema)
    }
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while decoding JWT token data
    #[error(transparent)]
    JWT(#[from] DecodeTokensError),
    /// Error encountered while creating access token entities
    #[error("{0}")]
    AccessTokenEntities(#[from] AccessTokenEntitiesError),
    /// Error encountered while creating access token entities
    #[error("could not create Jans::id_token: {0}")]
    CreateIdTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating access token entities
    #[error("could not create Jans::User: {0}")]
    CreateUserEntity(CedarPolicyCreateTypeError),
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
