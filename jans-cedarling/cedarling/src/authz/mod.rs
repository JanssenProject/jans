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

use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use std::sync::Arc;

use crate::bootstrap_config::AuthorizationConfig;
use crate::common::app_types;
use crate::common::policy_store::PolicyStoreWithID;
use crate::jwt;
use crate::log::interface::LogWriter;
use crate::log::{
    AuthorizationLogInfo, BaseLogEntry, DecisionLogEntry, Diagnostics, LogEntry, LogTokensInfo, LogType, Logger, PersonAuthorizeInfo, PrincipalLogEntry, WorkloadAuthorizeInfo
};
use std::io::Cursor;

mod authorize_result;

pub(crate) mod entities;
pub(crate) mod request;
mod token_data;

pub use authorize_result::AuthorizeResult;
use cedar_policy::{Entities, Entity, EntityUid, Response};
use entities::create_resource_entity;
use entities::CedarPolicyCreateTypeError;
use entities::ProcessTokensResult;
use entities::ResourceEntityError;
use entities::{
    create_access_token, create_id_token_entity, create_role_entities, create_user_entity,
    create_userinfo_token_entity, create_workload, RoleEntityError,
};
use request::Request;
use std::time::Instant;
use token_data::{AccessTokenData, IdTokenData, UserInfoTokenData};

/// Configuration to Authz to initialize service without errors
pub(crate) struct AuthzConfig {
    pub log_service: Logger,
    pub pdp_id: app_types::PdpID,
    pub application_name: app_types::ApplicationName,
    pub policy_store: PolicyStoreWithID,
    pub jwt_service: Arc<jwt::JwtService>,
    pub authorization: AuthorizationConfig,
}



/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
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
            .set_cedar_version()
            .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            config,
            authorizer: cedar_policy::Authorizer::new(),
        }
    }

    // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
    pub (crate) fn decode_tokens<'a>(&'a self, request: &'a Request) -> Result<ProcessTokensResult<'a>, AuthorizeError> {
            // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
        Ok(self
            .config
            .jwt_service
            .process_tokens::<AccessTokenData, IdTokenData, UserInfoTokenData>(
                &request.access_token,
                &request.id_token,
                Some(&request.userinfo_token),
        )?)
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *person*
    /// - evaluate if authorization is granted for *workload*
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        let start_time = Instant::now();

        let schema = &self.config.policy_store.schema;

        let tokens = self.decode_tokens(&request)?;


        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        // Parse context.
        let context: cedar_policy::Context = cedar_policy::Context::from_json_value(
            request.context.clone(),
            Some((&schema.schema, &action)),
        )?;

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data: AuthorizeEntitiesData = self.authorize_entities_data(&request,&tokens)?;

        // Get entity UIDs what we will be used on authorize check
        let principal_workload_uid = entities_data.workload_entity.uid();
        let resource_uid = entities_data.resource_entity.uid();
        let principal_user_entity_uid = entities_data.user_entity.uid();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities = entities_data.entities(Some(&schema.schema))?;

        // Check authorize where principal is `"Jans::Workload"` from cedar-policy schema.
        let workload_result: Option<Response> = if self.config.authorization.use_workload_principal
        {
            match self.execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_workload_uid.clone(),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            }) {
                Ok(resp) => Some(resp),
                Err(err) => return Err(AuthorizeError::CreateRequestWorkloadEntity(err)),
            }
        } else {
            None
        };

        // Check authorize where principal is `"Jans::User"` from cedar-policy schema.
        let person_result: Option<Response> = if self.config.authorization.use_user_principal {
            match self.execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: principal_user_entity_uid.clone(),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            }) {
                Ok(resp) => Some(resp),
                Err(err) => return Err(AuthorizeError::CreateRequestUserEntity(err)),
            }
        } else {
            None
        };

        let result = AuthorizeResult::new(
            self.config.authorization.user_workload_operator,
            workload_result,
            person_result,
        );

        // measure time how long request executes
        let elapsed_ms = start_time.elapsed().as_millis();

        // FROM THIS POINT WE ONLY MAKE LOGS

        // getting entities as json
        let mut entities_raw_json = Vec::new();
        let cursor = Cursor::new(&mut entities_raw_json);

        entities.write_to_json(cursor)?;
        let entities_json: serde_json::Value = serde_json::from_slice(entities_raw_json.as_slice())
            .map_err(AuthorizeError::EntitiesToJson)?;

        // DEBUG LOG
        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        self.config.log_service.as_ref().log(
            LogEntry::new_with_data(
                self.config.pdp_id,
                Some(self.config.application_name.clone()),
                LogType::Decision,
            )
            .set_auth_info(AuthorizationLogInfo {
                action: request.action.clone(),
                context: request.context.clone(),
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

        // Decision log
        self.config.log_service.as_ref().log_any(&DecisionLogEntry {
            base: BaseLogEntry::new(self.config.pdp_id, LogType::Decision),
            policystore_id: self.config.policy_store.id.as_str(),
            policystore_version: self.config.policy_store.get_store_version(),
            principal: PrincipalLogEntry::new(&self.config.authorization),
            user: get_entity_claims( self.config.authorization.decision_log_user_claims.as_slice(),&entities,principal_user_entity_uid),
            workload: get_entity_claims( self.config.authorization.decision_log_workload_claims.as_slice(),&entities,principal_workload_uid),
            lock_client_id: None,
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            decision: result.decision().into(),
            tokens: LogTokensInfo{
                access: tokens.access_token.get_log_tokens_info(self.config.authorization.decision_log_default_jwt_id.as_str()),
                id_token: tokens.id_token.get_log_tokens_info(self.config.authorization.decision_log_default_jwt_id.as_str()),
                userinfo: tokens.userinfo_token.get_log_tokens_info(self.config.authorization.decision_log_default_jwt_id.as_str()),
            },
            decision_time_ms: elapsed_ms,
        });

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
        tokens: &ProcessTokensResult,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        let policy_store = &self.config.policy_store;
        let auth_conf = &self.config.authorization;

        // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
        let decode_result: ProcessTokensResult = self
        .config
        .jwt_service
        .process_tokens::<AccessTokenData, IdTokenData, UserInfoTokenData>(
            &request.access_token,
            &request.id_token,
            Some(&request.userinfo_token),
        )?;

        let trusted_issuer = tokens.trusted_issuer.unwrap_or_default();
        let tokens_metadata = trusted_issuer.tokens_metadata();

        let role_entities = create_role_entities(policy_store, tokens, trusted_issuer)?;

        // Populate the `AuthorizeEntitiesData` structure using the builder pattern
        let data = AuthorizeEntitiesData::builder()
            // Add workload entity
            .workload_entity(create_workload(auth_conf.mapping_workload.as_deref(), policy_store,
                &decode_result.access_token,
                tokens_metadata.access_tokens).map_err(AuthorizeError::CreateWorkloadEntity)?)
            // Add access token entity
            .access_token(create_access_token(auth_conf.mapping_access_token.as_deref(), policy_store,
                &tokens.access_token,
                tokens_metadata.access_tokens).map_err(AuthorizeError::CreateAccessTokenEntity)?)
            // add id_token entity
            .id_token_entity(
                create_id_token_entity(auth_conf.mapping_id_token.as_deref(), 
                    policy_store, 
                    &tokens.id_token, 
                    &tokens_metadata.id_tokens.claim_mapping)
                    .map_err(AuthorizeError::CreateIdTokenEntity)?,
            )
            // Add userinfo_token entity
            .userinfo_token(
                create_userinfo_token_entity(auth_conf.mapping_userinfo_token.as_deref(), 
                    policy_store, 
                    &tokens.userinfo_token, 
                    &tokens_metadata.userinfo_tokens.claim_mapping)
                .map_err(AuthorizeError::CreateUserinfoTokenEntity)?
            )
            // Add User entity
            .user_entity(
                create_user_entity(auth_conf.mapping_user.as_deref(),
                    policy_store,
                    tokens,
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
            // Add Role entities
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
    pub workload_entity: Entity,
    pub access_token: Entity,
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
            self.workload_entity,
            self.access_token,
            self.id_token_entity,
            self.userinfo_token,
            self.user_entity,
            self.resource_entity,
        ]
        .into_iter()
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
    /// Error encountered while processing JWT token data
    #[error(transparent)]
    ProcessTokens(#[from] jwt::JwtProcessingError),
    /// Error encountered while creating id token entity
    #[error("could not create id_token entity: {0}")]
    CreateIdTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating userinfo entity
    #[error("could not create userinfo entity: {0}")]
    CreateUserinfoTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating access_token entity
    #[error("could not create access_token entity: {0}")]
    CreateAccessTokenEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating user entity
    #[error("could not create User entity: {0}")]
    CreateUserEntity(CedarPolicyCreateTypeError),
    /// Error encountered while creating workload
    #[error("could not create Workload entity: {0}")]
    CreateWorkloadEntity(CedarPolicyCreateTypeError),
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



/// Get entity claims from list in config
// 
// To get claims we convert entity to json, because no other way to get introspection
fn get_entity_claims(decision_log_claims: &[String], entities: &Entities,principal_user_entity_uid: EntityUid) -> HashMap<String, serde_json::Value> {
    HashMap::from_iter(  decision_log_claims
        .iter()
        .filter_map(|claim_key| {
            entities
                .get(&principal_user_entity_uid)
                // convert entity to json and result to option
                .and_then(|entity| entity.to_json_value().ok())
                // JSON structure of entity:
                // {
                //     "uid": {
                //         "type": "Jans::User",
                //         "id": "..."
                //     },
                //     "attrs": {
                //         ...
                //     },
                //     "parents": [
                //         {
                //             "type": "Jans::Role",
                //             "id": "SomeID"
                //         }
                //     ]
                // },
                .and_then(|json_value| 
                    // get `attrs` attribute
                    json_value.get("attrs")
                    .map(|attrs_value| 
                        // get claim key value
                        attrs_value.get(claim_key)
                        .map(|claim_value| claim_value.to_owned())
                    )
                )
                .flatten()
                // convert to (String, Value) tuple
                .map(|attr_json| (claim_key.clone(),attr_json.clone()))
        }))
}

