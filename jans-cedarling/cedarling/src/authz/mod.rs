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

use std::collections::HashSet;
use std::str::FromStr;
use std::sync::Arc;

use crate::common::app_types;
use crate::common::policy_store::PolicyStore;
use crate::jwt;
use crate::log::{
    AuthorizationLogInfo, Diagnostics, LogEntry, LogType, Logger, PersonAuthorizeInfo,
    WorkloadAuthorizeInfo,
};
use std::io::Cursor;

mod authorize_result;

mod entities;
pub(crate) mod request;
mod token_data;

pub use authorize_result::AuthorizeResult;
use cedar_policy::{Entities, Entity, EntityUid, RequestValidationError, Response};
use entities::CedarPolicyCreateTypeError;
use entities::DecodeTokensResult;
use entities::ResourceEntityError;
use entities::{
    create_access_token_entities, create_id_token_entity, create_role_entities, create_user_entity,
    create_userinfo_token_entity, AccessTokenEntitiesError, RoleEntityError,
};
use entities::{create_resource_entity, AccessTokenEntities};
use request::Request;
use token_data::{AccessTokenData, IdTokenData, UserInfoTokenData};

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
        let schema = &self.config.policy_store.schema;

        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        // Parse context.
        let context: cedar_policy::Context = cedar_policy::Context::from_json_value(
            request.context.clone(),
            Some((&schema.schema, &action)),
        )?;

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data: AuthorizeEntitiesData = self.authorize_entities_data(&request)?;

        // Get entity UIDs what we will be used on authorize check
        let principal_workload_uid = entities_data.access_token_entities.workload_entity.uid();
        let resource_uid = entities_data.resource_entity.uid();
        let principal_user_entity_uid = entities_data.user_entity.uid();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities = entities_data.entities(Some(&schema.schema))?;

        // Check authorize where principal is `"Jans::Workload"` from cedar-policy schema.
        let workload_result: Option<Response> =
            match self.execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_workload_uid.clone(),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            }) {
                Ok(resp) => Some(resp),
                // if invalid principal than we no need result
                Err(RequestValidationError::InvalidPrincipalType(_)) => None,
                Err(err) => return Err(AuthorizeError::CreateRequestWorkloadEntity(err)),
            };

        // Check authorize where principal is `"Jans::User"` from cedar-policy schema.
        let person_result: Option<Response> =
            match self.execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_user_entity_uid.clone(),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            }) {
                Ok(resp) => Some(resp),
                // if invalid principal than we no need result
                Err(RequestValidationError::InvalidPrincipalType(_)) => None,
                Err(err) => return Err(AuthorizeError::CreateRequestUserEntity(err)),
            };

        let result = AuthorizeResult {
            workload: workload_result,
            person: person_result,
        };

        // getting entities as json
        let mut entities_raw_json = Vec::new();
        let cursor = Cursor::new(&mut entities_raw_json);

        entities.write_to_json(cursor)?;
        let entities_json: serde_json::Value = serde_json::from_slice(entities_raw_json.as_slice())
            .map_err(AuthorizeError::EntitiesToJson)?;

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
                entities: entities_json,

                person_authorize_info: result.person.as_ref().map(|response| PersonAuthorizeInfo {
                    person_principal: principal_user_entity_uid.to_string(),
                    person_diagnostics: Diagnostics::new(
                        response.diagnostics(),
                        &self.config.policy_store.policies,
                    ),
                    person_decision: response.decision().into(),
                }),

                workload_authorize_info: result.workload.as_ref().map(|response| {
                    WorkloadAuthorizeInfo {
                        workload_principal: principal_workload_uid.to_string(),
                        workload_diagnostics: Diagnostics::new(
                            response.diagnostics(),
                            &self.config.policy_store.policies,
                        ),
                        workload_decision: response.decision().into(),
                    }
                }),

                authorized: result.is_allowed(),
            })
            .set_message("Result of authorize.".to_string()),
        );

        Ok(result)
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
            Some(&self.config.policy_store.schema.schema),
        )?;

        let response = self.authorizer.is_authorized(
            &request_principal_workload,
            self.config.policy_store.policies.get_set(),
            parameters.entities,
        );

        Ok(response)
    }

    /// Create all [`Entity`]-s from [`Request`]
    pub fn authorize_entities_data(
        &self,
        request: &Request,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        let policy_store = &self.config.policy_store;

        // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
        let decode_result: DecodeTokensResult = self
            .config
            .jwt_service
            .decode_tokens::<AccessTokenData, IdTokenData, UserInfoTokenData>(
                &request.access_token,
                &request.id_token,
                &request.userinfo_token,
            )?;

        let trusted_issuer = decode_result.trusted_issuer.unwrap_or_default();
        let tokens_metadata = trusted_issuer.tokens_metadata();

        let role_entities = create_role_entities(policy_store, &decode_result, trusted_issuer)?;

        // Populate the `AuthorizeEntitiesData` structure using the builder pattern
        let data = AuthorizeEntitiesData::builder()
            // Populate the structure with entities derived from the access token
            .access_token_entities(create_access_token_entities(
                policy_store,
                &decode_result.access_token,
                tokens_metadata.access_tokens
            )?)
            // Add an entity created from the ID token
            .id_token_entity(
                create_id_token_entity(policy_store, &decode_result.id_token, &tokens_metadata.id_tokens.claim_mapping)
                    .map_err(AuthorizeError::CreateIdTokenEntity)?,
            )
            // Add an entity created from the userinfo token
            .userinfo_token(
                create_userinfo_token_entity(policy_store, &decode_result.userinfo_token, &tokens_metadata.userinfo_tokens.claim_mapping)
                .map_err(AuthorizeError::CreateUserinfoTokenEntity)?
            )
            // Add an entity created from the userinfo token
            .user_entity(
                create_user_entity(
                    policy_store,
                    &decode_result,
                    // parents for Jans::User entity
                    HashSet::from_iter(role_entities.iter().map(|e|e.uid())),
                    trusted_issuer
                )
                .map_err(AuthorizeError::CreateUserEntity)?,
            )
            // Add an entity created from the resource in the request
            .resource_entity(create_resource_entity(
                request.resource.clone(),
                &self.config.policy_store.schema.json,
            )?)
            // Add Jans::Role entities
            .role_entities(role_entities);

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
pub struct AuthorizeEntitiesData {
    pub access_token_entities: AccessTokenEntities,
    pub id_token_entity: Entity,
    pub userinfo_token: Entity,
    pub user_entity: Entity,
    pub resource_entity: Entity,
    pub role_entities: Vec<Entity>,
}

impl AuthorizeEntitiesData {
    /// Create iterator to get all entities
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        vec![
            self.id_token_entity,
            self.userinfo_token,
            self.user_entity,
            self.resource_entity,
        ]
        .into_iter()
        .chain(self.access_token_entities.into_iter())
        .chain(self.role_entities)
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
    DecodeTokens(#[from] jwt::JwtServiceError),
    /// Error encountered while creating access token entities
    #[error("{0}")]
    AccessTokenEntities(#[from] AccessTokenEntitiesError),
    /// Error encountered while creating id token entities
    #[error("could not create id_token entity: {0}")]
    CreateIdTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating userinfo token entities
    #[error("could not create userinfo entity: {0}")]
    CreateUserinfoTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating access token entities
    #[error("could not create User entity: {0}")]
    CreateUserEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating resource entity
    #[error("{0}")]
    ResourceEntity(#[from] ResourceEntityError),
    /// Error encountered while creating role entity
    #[error(transparent)]
    RoleEntity(#[from] RoleEntityError),
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
    /// Error encountered while creating [`cedar_policy::Request`] for role entity principal
    //
    // Additional error was created to use only one placeholder.
    // It allows to use macro for error mapping in python binding
    #[error(transparent)]
    CreateRequestRoleEntity(CreateRequestRoleError),
    /// Error encountered while collecting all entities
    #[error("could not collect all entities: {0}")]
    Entities(#[from] cedar_policy::entities_errors::EntitiesError),
    /// Error encountered while parsing all entities to json for logging
    #[error("could convert entities to json: {0}")]
    EntitiesToJson(serde_json::Error),
}

#[derive(Debug, derive_more::Error, derive_more::Display)]
#[display("could not create request user entity principal for {uid}: {err}")]
pub struct CreateRequestRoleError {
    /// Error value
    err: cedar_policy::RequestValidationError,
    /// Role ID [`EntityUid`] value used for authorization request
    uid: EntityUid,
}
