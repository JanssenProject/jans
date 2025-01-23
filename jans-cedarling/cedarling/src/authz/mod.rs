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
use crate::common::app_types;
use crate::common::policy_store::PolicyStoreWithID;
use crate::jwt::{self, JwtProcessingError, TokenStr};
use crate::log::interface::LogWriter;
use crate::log::{
    Diagnostics, LogEntry, LogLevel, LogType, Logger, UserAuthorizeInfo, WorkloadAuthorizeInfo,
};
use build_ctx::*;
use cedar_policy::{Entities, Entity, EntityUid};
use chrono::Utc;
use entity_builder::*;
use log_authz::{LogAuthzArgs, LoggingError};
use request::Request;
use std::collections::HashMap;
use std::str::FromStr;
use std::sync::Arc;
use trust_mode::*;

mod authorize_result;
mod build_ctx;
mod log_authz;
mod trust_mode;

pub(crate) mod entity_builder;
pub(crate) mod request;

pub use authorize_result::AuthorizeResult;

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
    entity_builder: EntityBuilder,
}

impl Authz {
    /// Create a new Authorization Service
    pub(crate) fn new(config: AuthzConfig) -> Self {
        let json_schema = config.policy_store.schema.json.clone();
        let entity_names = EntityNames::from(&config.authorization);
        let build_workload = config.authorization.use_workload_principal;
        let build_user = config.authorization.use_user_principal;
        let entity_builder = entity_builder::EntityBuilder::new(
            json_schema,
            entity_names,
            build_workload,
            build_user,
        );

        config.log_service.log(
            LogEntry::new_with_data(
                config.pdp_id,
                Some(config.application_name.clone()),
                LogType::System,
            )
            .set_cedar_version()
            .set_level(LogLevel::INFO)
            .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            config,
            authorizer: cedar_policy::Authorizer::new(),
            entity_builder,
        }
    }

    // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
    pub(crate) async fn decode_tokens<'a>(
        &'a self,
        request: &'a Request,
    ) -> Result<DecodedTokens<'a>, JwtProcessingError> {
        let access = if let Some(tkn) = request.tokens.access_token.as_ref() {
            Some(
                self.config
                    .jwt_service
                    .process_token(TokenStr::Access(tkn))
                    .await?,
            )
        } else {
            None
        };
        let id = if let Some(tkn) = request.tokens.id_token.as_ref() {
            Some(
                self.config
                    .jwt_service
                    .process_token(TokenStr::Id(tkn))
                    .await?,
            )
        } else {
            None
        };
        let userinfo = if let Some(tkn) = request.tokens.userinfo_token.as_ref() {
            Some(
                self.config
                    .jwt_service
                    .process_token(TokenStr::Userinfo(tkn))
                    .await?,
            )
        } else {
            None
        };

        Ok(DecodedTokens {
            access,
            id,
            userinfo,
        })
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *person*
    /// - evaluate if authorization is granted for *workload*
    pub async fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        let start_time = Utc::now();

        let schema = &self.config.policy_store.schema;

        let tokens = match self.decode_tokens(&request).await {
            Ok(tokens) => tokens,
            Err(e) => return Ok(AuthorizeResult::from(e)),
        };

        if let IdTokenTrustMode::Strict = self.config.authorization.id_token_trust_mode {
            if let Err(e) = validate_id_tkn_trust_mode(&tokens) {
                return Ok(AuthorizeResult::from(e));
            };
        }

        // Parse action UID.
        let action = match cedar_policy::EntityUid::from_str(request.action.as_str()) {
            Ok(action) => action,
            Err(e) => return Ok(AuthorizeResult::from(BadInputError::Action(e))),
        };

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data = match self
            .entity_builder
            .build_entities(&tokens, &request.resource)
        {
            Ok(entities) => entities,
            Err(e) => return Ok(AuthorizeResult::from(e)),
        };

        // Get entity UIDs what we will be used on authorize check
        let resource_uid = entities_data.resource.uid();

        let context = match build_context(
            &self.config,
            request.context.clone(),
            &entities_data,
            &schema.schema,
            &action,
        ) {
            Ok(ctx) => ctx,
            Err(e) => return Ok(AuthorizeResult::from(e)),
        };

        let workload_principal = entities_data.workload.as_ref().map(|e| e.uid()).to_owned();
        let user_principal = entities_data.user.as_ref().map(|e| e.uid()).to_owned();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities = match entities_data.entities(Some(&schema.schema)) {
            Ok(entities) => entities,
            Err(e) => return Ok(AuthorizeResult::from(BadInputError::from(e))),
        };

        let (workload_authz_result, workload_authz_info, workload_entity_claims) =
            if let Some(workload) = workload_principal {
                let principal = workload;

                let authz_result = match self.execute_authorize(ExecuteAuthorizeParameters {
                    entities: &entities,
                    principal: principal.clone(),
                    action: action.clone(),
                    resource: resource_uid.clone(),
                    context: context.clone(),
                }) {
                    Ok(result) => result,
                    Err(e) => {
                        return Ok(AuthorizeResult::from(
                            BadInputError::WorkloadRequestValidation(e),
                        ));
                    },
                };

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

                let authz_result = match self.execute_authorize(ExecuteAuthorizeParameters {
                    entities: &entities,
                    principal: principal.clone(),
                    action: action.clone(),
                    resource: resource_uid.clone(),
                    context: context.clone(),
                }) {
                    Ok(result) => result,
                    Err(e) => {
                        return Ok(AuthorizeResult::from(BadInputError::UserRequestValidation(
                            e,
                        )));
                    },
                };

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
        );

        // measure time how long request executes
        let elapsed_ms = Utc::now()
            .signed_duration_since(start_time)
            .num_milliseconds();

        self.log_authz(LogAuthzArgs {
            tokens,
            entities: &entities,
            user_authz_info,
            workload_authz_info,
            user_entity_claims,
            workload_entity_claims,
            request: &request,
            resource: resource_uid,
            result: &result,
            elapsed_ms,
        })?;

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
        tokens: &DecodedTokens<'_>,
    ) -> Result<AuthorizeEntitiesData, BadInputError> {
        Ok(self
            .entity_builder
            .build_entities(tokens, &request.resource)?)
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
#[derive(Clone)]
pub struct AuthorizeEntitiesData {
    pub workload: Option<Entity>,
    pub user: Option<Entity>,
    pub access_token: Option<Entity>,
    pub id_token: Option<Entity>,
    pub userinfo_token: Option<Entity>,
    pub resource: Entity,
    pub roles: Vec<Entity>,
}

impl AuthorizeEntitiesData {
    // NOTE: the type ids created from these does not include the namespace
    fn type_ids(&self) -> HashMap<String, String> {
        self.iter()
            .map(|entity| {
                let type_name = entity.uid().type_name().basename().to_string();
                let type_id = entity.uid().id().escaped().to_string();
                (type_name, type_id)
            })
            .collect::<HashMap<String, String>>()
    }

    /// Create iterator to get all entities
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        vec![self.resource].into_iter().chain(self.roles).chain(
            vec![
                self.user,
                self.workload,
                self.access_token,
                self.userinfo_token,
                self.id_token,
            ]
            .into_iter()
            .flatten(),
        )
    }

    /// Create iterator to get all entities
    fn iter(&self) -> impl Iterator<Item = &Entity> {
        vec![&self.resource]
            .into_iter()
            .chain(self.roles.iter())
            .chain(
                vec![
                    self.user.as_ref(),
                    self.workload.as_ref(),
                    self.access_token.as_ref(),
                    self.userinfo_token.as_ref(),
                    self.id_token.as_ref(),
                ]
                .into_iter()
                .flatten(),
            )
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
    /// Error encountered while logging the request
    #[error("failed to log authorization request: {0}")]
    Logging(#[from] LoggingError),
}

/// Error returned because of invalid or malformed inputs to [`Authz::authorize`]
#[derive(thiserror::Error, Debug)]
pub enum BadInputError {
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
    #[error("the request for `Workload` does not conform to the schema: {0}")]
    WorkloadRequestValidation(cedar_policy::RequestValidationError),
    /// Error encountered while creating [`cedar_policy::Request`] for user entity principal
    #[error("the request for `User` does not conform to the schema: {0}")]
    UserRequestValidation(cedar_policy::RequestValidationError),
    /// Error encountered while collecting all entities
    #[error("could not collect all entities: {0}")]
    Entities(#[from] cedar_policy::entities_errors::EntitiesError),
    /// Error encountered while parsing all entities to json for logging
    #[error("could convert entities to json: {0}")]
    EntitiesToJson(serde_json::Error),
    /// Error encountered while building the context for the request
    #[error("failed to build context: {0}")]
    BuildContext(#[from] BuildContextError),
    /// Error encountered while building the context for the request
    #[error("error while running on strict id token trust mode: {0}")]
    IdTokenTrustMode(#[from] IdTokenTrustModeError),
    /// Error encountered while building Cedar Entities
    #[error(transparent)]
    BuildEntity(#[from] BuildCedarlingEntityError),
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
