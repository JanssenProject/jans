/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! # Auth Engine
//! Part of Cedarling that main purpose is:
//! - evaluate if authorization is granted for *user*
//! - evaluate if authorization is granted for *client* / *workload *

use std::str::FromStr;
use std::sync::Arc;

use crate::common::app_types;
use crate::common::policy_store::PolicyStore;
use crate::jwt;
use crate::log::{AuthorizationLogInfo, LogEntry, LogType, Logger};

mod authorize_result;

mod entities;
pub(crate) mod request;
mod token_data;

pub use authorize_result::AuthorizeResult;
use cedar_policy::{Entities, Entity, EntityUid};
use entities::CedarPolicyCreateTypeError;
use entities::ResourceEntityError;
use entities::{
    create_access_token_entities, create_id_token_entity, create_user_entity,
    AccessTokenEntitiesError,
};
use entities::{create_resource_entity, AccessTokenEntities};
use request::Request;
pub(crate) use token_data::{AccessTokenData, IdTokenData, TokenPayload, UserInfoTokenData};

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
    authorizer: cedar_policy::Authorizer,
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

        Self {
            config,
            authorizer: cedar_policy::Authorizer::new(),
        }
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *person*
    /// - evaluate if authorization is granted for *workload*
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        // Parse context.
        let context: cedar_policy::Context = cedar_policy::Context::from_json_value(
            request.context.clone(),
            Some((&self.config.policy_store.cedar_schema.schema, &action)),
        )?;

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data: AuthorizeEntitiesData = self.authorize_entities_data(&request)?;

        // Get entity UIDs what we will be used on authorize check
        let principal_workload_uid = entities_data.access_token_entities.workload_entity.uid();
        let resource_uid = entities_data.resource_entity.uid();
        let principal_user_entity_uid = entities_data.user_entity.uid();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities =
            entities_data.entities(Some(&self.config.policy_store.cedar_schema.schema))?;

        // Check authorize where principal is `"Jans::Workload"` from cedar-policy schema.
        let workload_result = self
            .execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_workload_uid.clone(),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            })
            .map_err(AuthorizeError::CreateRequestWorkloadEntity)?;

        // Check authorize where principal is `"Jans::User"` from cedar-policy schema.
        let person_result = self
            .execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_user_entity_uid.clone(),
                action,
                resource: resource_uid.clone(),
                context,
            })
            .map_err(AuthorizeError::CreateRequestUserEntity)?;

        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        self.config.log_service.as_ref().log(
            LogEntry::new_with_data(
                self.config.pdp_id,
                Some(self.config.application_name.clone()),
                LogType::Decision,
            )
            .set_auth_info(AuthorizationLogInfo {
                action: request.action,
                context: request.context,
                resource: resource_uid.to_string(),

                person_principal: principal_user_entity_uid.to_string(),
                workload_principal: principal_workload_uid.to_string(),

                person_diagnostics: person_result.diagnostics().into(),
                workload_diagnostics: workload_result.diagnostics().into(),

                person_decision: person_result.decision().into(),
                workload_decision: workload_result.decision().into(),
            })
            .set_message("Result of authorize.".to_string()),
        );

        Ok(AuthorizeResult {
            workload: workload_result,
            person: person_result,
        })
    }

    /// Execute cedar policy is_authorized method to check
    /// if allowed make request with given parameters
    fn execute_authorize(
        &self,
        parameters: ExecuteAuthorizeParameters,
    ) -> Result<cedar_policy::Response, cedar_policy::RequestValidationError> {
        let request_principal_workload = cedar_policy::Request::new(
            parameters.principal,
            parameters.action,
            parameters.resource,
            parameters.context,
            Some(&self.config.policy_store.cedar_schema.schema),
        )?;

        let response = self.authorizer.is_authorized(
            &request_principal_workload,
            &self.config.policy_store.cedar_policies,
            parameters.entities,
        );

        Ok(response)
    }

    /// Create all [`Entity`]-s from [`Request`]
    fn authorize_entities_data(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
        let (access_token, id_token, userinfo_token) = self.config.jwt_service.decode_tokens(
            &request.access_token,
            &request.id_token,
            &request.userinfo_token,
        )?;
        let access_token: AccessTokenData = access_token.into();
        let id_token: IdTokenData = id_token.into();
        let userinfo_token: UserInfoTokenData = userinfo_token.into();

        // Populate the `AuthorizeEntitiesData` structure using the builder pattern
        let data = AuthorizeEntitiesData::builder()
            // Populate the structure with entities derived from the access token
            .access_token_entities(create_access_token_entities(
                &self.config.policy_store.cedar_schema.json,
                &access_token,
            )?)
            // Add an entity created from the ID token
            .id_token_entity(
                create_id_token_entity(&self.config.policy_store.cedar_schema.json, &id_token)
                    .map_err(AuthorizeError::CreateIdTokenEntity)?,
            )
            // Add an entity created from the userinfo token
            .user_entity(
                create_user_entity(
                    &self.config.policy_store.cedar_schema.json,
                    &id_token,
                    &userinfo_token,
                )
                .map_err(AuthorizeError::CreateUserEntity)?,
            )
            // Add an entity created from the resource in the request
            .resource_entity(create_resource_entity(
                request.resource.clone(),
                &self.config.policy_store.cedar_schema.json,
            )?);

        Ok(data.build())
    }
}

/// Helper struct to hold named parameters for [`Authz::execute_authorize`] method.
struct ExecuteAuthorizeParameters<'a> {
    entities: &'a Entities,
    principal: EntityUid,
    action: EntityUid,
    resource: EntityUid,
    context: cedar_policy::Context,
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
        Entities::from_entities(self.into_iter(), schema)
    }
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while decoding JWT token data
    #[error(transparent)]
    DecodeTokens(#[from] jwt::JwtDecodingError),
    /// Error encountered while creating access token entities
    #[error("{0}")]
    AccessTokenEntities(#[from] AccessTokenEntitiesError),
    /// Error encountered while creating id token entities
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
    /// Error encountered while creating [`cedar_policy::Request`] for workload entity principal
    #[error("could not create request workload entity principal: {0}")]
    CreateRequestWorkloadEntity(cedar_policy::RequestValidationError),
    /// Error encountered while creating [`cedar_policy::Request`] for user entity principal
    #[error("could not create request user entity principal: {0}")]
    CreateRequestUserEntity(cedar_policy::RequestValidationError),
    /// Error encountered while collecting all entities
    #[error("could not collect all entities: {0}")]
    Entities(#[from] cedar_policy::entities_errors::EntitiesError),
}
