// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Auth Engine
//! Part of Cedarling that main purpose is:
//! - evaluate if authorization is granted for *user*
//! - evaluate if authorization is granted for *client* / *workload *

use crate::authorization_config::IdTokenTrustMode;
use crate::bootstrap_config::AuthorizationConfig;
use crate::common::policy_store::PolicyStoreWithID;
use crate::entity_builder::*;
use crate::jwt::{self, Token};
use crate::log::interface::LogWriter;
use crate::log::{
    AuthorizationLogInfo, BaseLogEntry, DecisionLogEntry, Diagnostics, DiagnosticsRefs, LogEntry,
    LogLevel, LogTokensInfo, LogType, Logger, PrincipalLogEntry, UserAuthorizeInfo,
    WorkloadAuthorizeInfo, gen_uuid7,
};
use build_ctx::*;
use cedar_policy::{Entities, Entity, EntityUid};
use chrono::Utc;
use request::Request;
use smol_str::{SmolStr, ToSmolStr};
use std::collections::{HashMap, HashSet};
use std::io::Cursor;
use std::str::FromStr;
use std::sync::Arc;
use trust_mode::*;

mod authorize_result;
mod build_ctx;
mod trust_mode;

pub(crate) mod request;

pub use authorize_result::AuthorizeResult;

/// Configuration to Authz to initialize service without errors
pub(crate) struct AuthzConfig {
    pub log_service: Logger,
    pub policy_store: PolicyStoreWithID,
    pub jwt_service: Arc<jwt::JwtService>,
    pub entity_builder: Arc<EntityBuilder>,
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
    pub(crate) fn new(config: AuthzConfig) -> Result<Self, AuthzServiceInitError> {
        config.log_service.log_any(
            LogEntry::new_with_data(LogType::System, None)
                .set_cedar_version()
                .set_level(LogLevel::INFO)
                .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Ok(Self {
            config,
            authorizer: cedar_policy::Authorizer::new(),
        })
    }

    // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
    pub(crate) async fn decode_tokens<'a>(
        &'a self,
        request: &'a Request,
    ) -> Result<HashMap<String, Token<'a>>, AuthorizeError> {
        let tokens = self
            .config
            .jwt_service
            .validate_tokens(&request.tokens)
            .await?;
        Ok(tokens)
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *person*
    /// - evaluate if authorization is granted for *workload*
    pub async fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        let start_time = Utc::now();
        // We use uuid v7 because it is generated based on the time and sortable.
        // and we need sortable ids to use it in the sparkv database.
        // Sparkv store data in BTree. So we need have correct order of ids.
        //
        // Request ID should be passed to each log entry for tracing in logs and to get log entities from memory logger
        let request_id = gen_uuid7();

        let schema = &self.config.policy_store.schema;

        let tokens = self.decode_tokens(&request).await?;

        if let IdTokenTrustMode::Strict = self.config.authorization.id_token_trust_mode {
            validate_id_tkn_trust_mode(&tokens)?;
        }

        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(AuthorizeError::Action)?;

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data = self
            .config
            .entity_builder
            .build_entities(&tokens, &request.resource)?;

        // Get entity UIDs what we will be used on authorize check
        let resource_uid = entities_data.resource.uid();

        let context = build_context(
            &self.config,
            request.context.clone(),
            &entities_data,
            &schema.schema,
            &action,
        )?;

        let workload_principal = entities_data.workload.as_ref().map(|e| e.uid()).to_owned();
        let user_principal = entities_data.user.as_ref().map(|e| e.uid()).to_owned();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities = entities_data.entities(Some(&schema.schema))?;

        let (workload_authz_result, workload_authz_info, workload_entity_claims) =
            if let Some(workload) = workload_principal {
                let principal = workload;

                let authz_result = self
                    .execute_authorize(ExecuteAuthorizeParameters {
                        entities: &entities,
                        principal: principal.clone(),
                        action: action.clone(),
                        resource: resource_uid.clone(),
                        context: context.clone(),
                    })
                    .map_err(AuthorizeError::WorkloadRequestValidation)?;

                let authz_info = WorkloadAuthorizeInfo {
                    principal: principal.to_string(),
                    diagnostics: Diagnostics::new(
                        authz_result.diagnostics(),
                        &self.config.policy_store.policies,
                    ),
                    decision: authz_result.decision().into(),
                };

                let workload_entity_claims = get_entity_claims(
                    self.config
                        .authorization
                        .decision_log_workload_claims
                        .as_slice(),
                    &entities,
                    principal,
                );

                (
                    Some(authz_result),
                    Some(authz_info),
                    Some(workload_entity_claims),
                )
            } else {
                (None, None, None)
            };

        // Check authorize where principal is `"Jans::User"` from cedar-policy schema.
        let (user_authz_result, user_authz_info, user_entity_claims) =
            if let Some(user) = user_principal {
                let principal = user;

                let authz_result = self
                    .execute_authorize(ExecuteAuthorizeParameters {
                        entities: &entities,
                        principal: principal.clone(),
                        action: action.clone(),
                        resource: resource_uid.clone(),
                        context: context.clone(),
                    })
                    .map_err(AuthorizeError::UserRequestValidation)?;

                let authz_info = UserAuthorizeInfo {
                    principal: principal.to_string(),
                    diagnostics: Diagnostics::new(
                        authz_result.diagnostics(),
                        &self.config.policy_store.policies,
                    ),
                    decision: authz_result.decision().into(),
                };

                let user_entity_claims = get_entity_claims(
                    self.config
                        .authorization
                        .decision_log_user_claims
                        .as_slice(),
                    &entities,
                    principal,
                );

                (
                    Some(authz_result),
                    Some(authz_info),
                    Some(user_entity_claims),
                )
            } else {
                (None, None, None)
            };

        let result = AuthorizeResult::new(
            self.config.authorization.user_workload_operator,
            workload_authz_result,
            user_authz_result,
            request_id,
        );

        // measure time how long request executes
        let since_start = Utc::now().signed_duration_since(start_time);
        let decision_time_micro_sec = since_start.num_microseconds().unwrap_or({
            //overflow (exceeding 2^63 microseconds in either direction)
            i64::MAX
        });

        // FROM THIS POINT WE ONLY MAKE LOGS

        // getting entities as json
        let mut entities_raw_json = Vec::new();
        let cursor = Cursor::new(&mut entities_raw_json);

        entities.write_to_json(cursor)?;
        let entities_json: serde_json::Value = serde_json::from_slice(entities_raw_json.as_slice())
            .map_err(AuthorizeError::EntitiesToJson)?;

        let user_authz_diagnostic = user_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        let workload_authz_diagnostic = workload_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        let tokens_logging_info = LogTokensInfo::new(
            &tokens,
            self.config
                .authorization
                .decision_log_default_jwt_id
                .as_str(),
        );

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        self.config.log_service.as_ref().log_any(&DecisionLogEntry {
            base: BaseLogEntry::new(LogType::Decision, request_id),
            policystore_id: self.config.policy_store.id.as_str(),
            policystore_version: self.config.policy_store.get_store_version(),
            principal: PrincipalLogEntry::new(&self.config.authorization),
            user: user_entity_claims,
            workload: workload_entity_claims,
            lock_client_id: None,
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            decision: result.decision.into(),
            tokens: tokens_logging_info,
            decision_time_micro_sec,
            diagnostics: DiagnosticsRefs::new(&[
                &user_authz_diagnostic,
                &workload_authz_diagnostic,
            ]),
        });

        // DEBUG LOG
        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        self.config.log_service.as_ref().log_any(
            LogEntry::new_with_data(LogType::System, Some(request_id))
                .set_level(LogLevel::DEBUG)
                .set_auth_info(AuthorizationLogInfo {
                    action: request.action.clone(),
                    context: request.context.clone(),
                    resource: resource_uid.to_string(),
                    entities: entities_json,
                    person_authorize_info: user_authz_info,
                    workload_authorize_info: workload_authz_info,
                    authorized: result.decision,
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

    #[cfg(test)]
    pub fn build_entities(
        &self,
        request: &Request,
        tokens: &HashMap<String, Token>,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        Ok(self
            .config
            .entity_builder
            .build_entities(tokens, &request.resource)?)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum AuthzServiceInitError {
    #[error(transparent)]
    InitEntityBuilder(#[from] InitEntityBuilderError),
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
pub struct AuthorizeEntitiesData {
    pub issuers: HashSet<Entity>,
    pub tokens: HashMap<String, Entity>,
    pub workload: Option<Entity>,
    pub user: Option<Entity>,
    pub roles: Vec<Entity>,
    pub resource: Entity,
}

impl AuthorizeEntitiesData {
    /// Create iterator to get all entities
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        vec![self.resource]
            .into_iter()
            .chain(self.issuers)
            .chain(self.roles)
            .chain(self.tokens.into_values())
            .chain(vec![self.user, self.workload].into_iter().flatten())
    }

    /// Collect all entities to [`cedar_policy::Entities`]
    fn entities(
        self,
        schema: Option<&cedar_policy::Schema>,
    ) -> Result<cedar_policy::Entities, cedar_policy::entities_errors::EntitiesError> {
        Entities::from_entities(self.into_iter(), schema)
    }

    /// Returns the names and IDs of built entities for inclusion in the context.
    ///
    /// This includes:
    /// - **Token Entities**: e.g., `access_token`, `id_token`, etc.
    /// - **Principal Entities**: e.g., `Workload`, `User`, etc.
    /// - **Role Entities**
    ///
    /// Only entities that have been built will be included
    fn entities_for_context(&self) -> BuiltEntities {
        let token_entities = self.tokens.values().map(get_name_and_id);
        let principal_entities = [self.workload.as_ref(), self.user.as_ref()]
            .into_iter()
            .flatten()
            .map(get_name_and_id);
        let role_entities = self.roles.iter().map(get_name_and_id);

        HashMap::from_iter(
            token_entities
                .chain(principal_entities)
                .chain(role_entities),
        )
        .into()
    }
}

fn get_name_and_id(entity: &Entity) -> (SmolStr, SmolStr) {
    let name = entity.uid().type_name().to_smolstr();
    let id = entity.uid().id().escaped();
    (name, id)
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while processing JWT token data
    #[error(transparent)]
    ProcessTokens(#[from] jwt::JwtProcessingError),
    /// Error encountered while parsing Action to EntityUid
    #[error("could not parse action: {0}")]
    Action(cedar_policy::ParseErrors),
    /// Error encountered while validating context according to the schema
    #[error("could not create context: {0}")]
    CreateContext(#[from] cedar_policy::ContextJsonError),
    /// Error encountered while creating [`cedar_policy::Request`] for workload entity principal
    #[error("The request for `Workload` does not conform to the schema: {0}")]
    WorkloadRequestValidation(cedar_policy::RequestValidationError),
    /// Error encountered while creating [`cedar_policy::Request`] for user entity principal
    #[error("The request for `User` does not conform to the schema: {0}")]
    UserRequestValidation(cedar_policy::RequestValidationError),
    /// Error encountered while checking if the Entities adhere to the schema
    #[error("failed to validate Cedar entities: {0:?}")]
    ValidateEntities(#[from] cedar_policy::entities_errors::EntitiesError),
    /// Error encountered while parsing all entities to json for logging
    #[error("could convert entities to json: {0}")]
    EntitiesToJson(serde_json::Error),
    /// Error encountered while building the context for the request
    #[error("Failed to build context: {0}")]
    BuildContext(#[from] BuildContextError),
    /// Error encountered while building the context for the request
    #[error("error while running on strict id token trust mode: {0}")]
    IdTokenTrustMode(#[from] IdTokenTrustModeError),
    /// Error encountered while building Cedar Entities
    #[error(transparent)]
    BuildEntity(#[from] BuildEntityError),
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
// To get claims we convert entity to json, because no other way to get introspection
fn get_entity_claims(
    decision_log_claims: &[String],
    entities: &Entities,
    entity_uid: EntityUid,
) -> HashMap<String, serde_json::Value> {
    HashMap::from_iter(decision_log_claims.iter().filter_map(|claim_key| {
        entities
                .get(&entity_uid)
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
