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
    AuthorizationLogInfo, AuthorizeInfo, BaseLogEntry, DecisionLogEntry, Diagnostics,
    DiagnosticsRefs, LogEntry, LogLevel, LogTokensInfo, LogType, Logger, gen_uuid7,
};
use build_ctx::*;
use cedar_policy::{Entities, Entity, EntityUid};
use chrono::Utc;
use request::{Request, RequestUnsigned};
use std::collections::{HashMap, HashSet};
use std::io::Cursor;
use std::str::FromStr;
use std::sync::Arc;
use trust_mode::*;
use uuid7::Uuid;

mod authorize_result;
mod build_ctx;
mod errors;
mod trust_mode;

pub(crate) mod request;

pub use authorize_result::AuthorizeResult;
pub use errors::*;

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
    ) -> Result<HashMap<String, Arc<Token>>, AuthorizeError> {
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
            .map_err(|e| AuthorizeError::Action(Box::new(e)))?;

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
            &entities_data.built_entities(),
            &schema.schema,
            &action,
        )?;

        let workload_principal = entities_data.workload.as_ref().map(|e| e.uid()).to_owned();
        let person_principal = entities_data.user.as_ref().map(|e| e.uid()).to_owned();

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities: Entities = entities_data.entities(Some(&schema.schema))?;

        let (workload_authz_result, workload_authz_info, workload_entity_claims) =
            if let Some(workload) = workload_principal.clone() {
                let principal = workload;

                let authz_result = self
                    .execute_authorize(ExecuteAuthorizeParameters {
                        entities: &entities,
                        principal: principal.clone(),
                        action: action.clone(),
                        resource: resource_uid.clone(),
                        context: context.clone(),
                    })
                    .map_err(|err| InvalidPrincipalError::new(&principal, err))?;

                let authz_info = AuthorizeInfo {
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
            if let Some(user) = person_principal.clone() {
                let principal = user;

                let authz_result = self
                    .execute_authorize(ExecuteAuthorizeParameters {
                        entities: &entities,
                        principal: principal.clone(),
                        action: action.clone(),
                        resource: resource_uid.clone(),
                        context: context.clone(),
                    })
                    .map_err(|err| InvalidPrincipalError::new(&principal, err))?;

                let authz_info = AuthorizeInfo {
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
            &self.config.authorization.principal_bool_operator,
            workload_principal,
            person_principal,
            workload_authz_result,
            user_authz_result,
            request_id,
        )?;

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

        entities.write_to_json(cursor).map_err(Box::new)?;
        let entities_json: serde_json::Value = serde_json::from_slice(entities_raw_json.as_slice())
            .map_err(AuthorizeError::EntitiesToJson)?;

        let user_authz_diagnostic = user_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        let workload_authz_diagnostic = workload_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        // Log policy evaluation errors if any exist
        if let Some(diagnostics) = user_authz_diagnostic {
            self.log_policy_evaluation_errors(diagnostics, "user principal", request_id);
        }

        if let Some(diagnostics) = workload_authz_diagnostic {
            self.log_policy_evaluation_errors(diagnostics, "workload principal", request_id);
        }

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
            principal: DecisionLogEntry::principal(
                result.person.is_some(),
                result.workload.is_some(),
            ),
            user: user_entity_claims,
            workload: workload_entity_claims,
            lock_client_id: None,
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            decision: result.decision.into(),
            tokens: tokens_logging_info,
            decision_time_micro_sec,
            diagnostics: DiagnosticsRefs::new(&[user_authz_diagnostic, workload_authz_diagnostic]),
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
                    authorize_info: [user_authz_info, workload_authz_info]
                        .into_iter()
                        .flatten()
                        .collect(),
                    authorized: result.decision,
                })
                .set_message("Result of authorize.".to_string()),
        );

        Ok(result)
    }

    /// Evaluate Multi-Issuer Authorization Request
    ///
    /// This is a draft implementation for processing multiple JWT tokens from different issuers.
    /// It validates the request format and JWT tokens, then performs authorization evaluation.
    pub async fn authorize_multi_issuer(
        &self,
        request: request::AuthorizeMultiIssuerRequest,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        // Validate the request format
        request.validate()?;

        // Validate JWT tokens using the JWT service
        let _validated_tokens = self
            .config
            .jwt_service
            .validate_multi_issuer_tokens(&request.tokens)
            .map_err( AuthorizeError::MultiIssuerValidation)?;

        // TODO: Convert validated tokens to Token HashMap for entity building
        // TODO: Build entities from validated tokens
        // TODO: Perform authorization evaluation
        // TODO: Return authorization result

        // Placeholder return - this will be implemented in future PRs
        Err(AuthorizeError::MultiIssuerValidation(
            MultiIssuerValidationError::EmptyTokenArray,
        ))
    }

    /// Evaluate Authorization Request with unsigned data.
    pub async fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        let start_time = Utc::now();
        // We use uuid v7 because it is generated based on the time and sortable.
        // and we need sortable ids to use it in the sparkv database.
        // Sparkv store data in BTree. So we need have correct order of ids.
        //
        // Request ID should be passed to each log entry for tracing in logs and to get log entities from memory logger
        let request_id = gen_uuid7();

        let schema = &self.config.policy_store.schema;
        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())
            .map_err(|e| AuthorizeError::Action(Box::new(e)))?;

        let BuiltEntitiesUnsigned {
            principals,
            roles,
            resource,
            built_entities,
        } = self
            .config
            .entity_builder
            .build_entities_unsigned(&request)?;
        let principal_uids = principals
            .iter()
            .map(|p| p.uid())
            .collect::<Vec<EntityUid>>();
        let resource_uid = resource.uid();

        let context = build_context(
            &self.config,
            request.context.clone(),
            &built_entities,
            &schema.schema,
            &action,
        )?;

        let entities = Entities::from_entities(
            principals.into_iter().chain(roles).chain([resource]),
            Some(&schema.schema),
        )
        .map_err(Box::new)?;

        let mut principal_responses = HashMap::new();

        for principal_uid in principal_uids.iter() {
            let auth_result = self
                .execute_authorize(ExecuteAuthorizeParameters {
                    entities: &entities,
                    principal: principal_uid.clone(),
                    action: action.clone(),
                    resource: resource_uid.clone(),
                    context: context.clone(),
                })
                .map_err(|err| InvalidPrincipalError::new(principal_uid, err))?;

            principal_responses.insert(principal_uid.clone(), auth_result);
        }

        let result = AuthorizeResult::new_for_many_principals(
            &self.config.authorization.principal_bool_operator,
            principal_responses,
            None,
            None,
            request_id,
        )?;

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

        entities.write_to_json(cursor).map_err(Box::new)?;
        let entities_json: serde_json::Value = serde_json::from_slice(entities_raw_json.as_slice())
            .map_err(AuthorizeError::EntitiesToJson)?;

        let debug_authorize_info = result
            .principals
            .iter()
            .map(|(principal, response)| AuthorizeInfo {
                principal: principal.to_string(),
                diagnostics: Diagnostics::new(
                    response.diagnostics(),
                    &self.config.policy_store.policies,
                ),
                decision: response.decision().into(),
            })
            .collect::<Vec<_>>();

        let diagnostics = debug_authorize_info
            .iter()
            .map(|info| Some(&info.diagnostics))
            .collect::<Vec<_>>();

        // Log policy evaluation errors if any exist
        for info in &debug_authorize_info {
            self.log_policy_evaluation_errors(&info.diagnostics, &info.principal, request_id);
        }

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        self.config.log_service.as_ref().log_any(&DecisionLogEntry {
            base: BaseLogEntry::new(LogType::Decision, request_id),
            policystore_id: self.config.policy_store.id.as_str(),
            policystore_version: self.config.policy_store.get_store_version(),
            principal: DecisionLogEntry::all_principals(principal_uids.as_slice()),
            user: None,
            workload: None,
            lock_client_id: None,
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            decision: result.decision.into(),
            tokens: LogTokensInfo::empty(),
            decision_time_micro_sec,
            diagnostics: DiagnosticsRefs::new(diagnostics.as_slice()),
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
                    authorize_info: debug_authorize_info,
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
    ) -> Result<cedar_policy::Response, Box<cedar_policy::RequestValidationError>> {
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
        tokens: &HashMap<String, Arc<Token>>,
    ) -> Result<AuthorizeEntitiesData, AuthorizeError> {
        Ok(self
            .config
            .entity_builder
            .build_entities(tokens, &request.resource)?)
    }

    /// Log policy evaluation errors for diagnostics
    fn log_policy_evaluation_errors(
        &self,
        diagnostics: &Diagnostics,
        principal_name: &str,
        request_id: Uuid,
    ) {
        if !diagnostics.errors.is_empty() {
            self.config.log_service.log_any(
                LogEntry::new_with_data(LogType::Decision, Some(request_id))
                    .set_level(LogLevel::ERROR)
                    .set_message(format!("Policy evaluation errors for {}", principal_name))
                    .set_error(format!("{:?}", diagnostics.errors)),
            );
        }
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
pub struct AuthorizeEntitiesData {
    pub issuers: HashSet<Entity>,
    pub tokens: HashMap<String, Entity>,
    pub workload: Option<Entity>,
    pub user: Option<Entity>,
    pub roles: Vec<Entity>,
    pub resource: Entity,
    pub default_entities: HashMap<String, Entity>,
}

impl AuthorizeEntitiesData {
    /// Create iterator to get all entities
    ///
    /// This method merges request entities with default entities, where request entities
    /// take precedence over default entities in case of UID conflicts.
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        let mut merged_entities: HashMap<EntityUid, Entity> = HashMap::new();

        // Add default entities first
        merged_entities.extend(self.default_entities.into_values().map(|e| (e.uid(), e)));

        // Add request entities (these will override default entities if conflicts exist)
        merged_entities.extend(vec![self.resource].into_iter().map(|e| (e.uid(), e)));
        merged_entities.extend(self.issuers.into_iter().map(|e| (e.uid(), e)));
        merged_entities.extend(self.roles.into_iter().map(|e| (e.uid(), e)));
        merged_entities.extend(self.tokens.into_values().map(|e| (e.uid(), e)));
        merged_entities.extend(
            vec![self.user, self.workload]
                .into_iter()
                .flatten()
                .map(|e| (e.uid(), e)),
        );

        merged_entities.into_values()
    }

    /// Collect all entities to [`cedar_policy::Entities`]
    pub(crate) fn entities(
        self,
        schema: Option<&cedar_policy::Schema>,
    ) -> Result<cedar_policy::Entities, Box<cedar_policy::entities_errors::EntitiesError>> {
        Entities::from_entities(self.into_iter(), schema).map_err(Box::new)
    }

    /// Returns the names and IDs of built entities for inclusion in the context.
    ///
    /// This includes:
    /// - **Token Entities**: e.g., `access_token`, `id_token`, etc.
    /// - **Principal Entities**: e.g., `Workload`, `User`, etc.
    /// - **Role Entities**
    /// - **Default Entities**: Entities loaded from the policy store configuration
    ///
    /// Only entities that have been built will be included
    ///
    /// Note: This method uses the same merging logic as `into_iter()` to ensure consistency
    /// and avoid duplicate entity UIDs in the context.
    fn built_entities(&self) -> BuiltEntities {
        // Use the same merging logic as into_iter() to ensure consistency
        let mut merged_entities: HashMap<EntityUid, Entity> = HashMap::new();

        // Add default entities first
        merged_entities.extend(self.default_entities.values().map(|e| (e.uid(), e.clone())));

        // Add request entities, overriding any conflicting default entities
        merged_entities.extend(
            vec![&self.resource]
                .into_iter()
                .map(|e| (e.uid(), e.clone())),
        );
        merged_entities.extend(self.issuers.iter().map(|e| (e.uid(), e.clone())));
        merged_entities.extend(self.roles.iter().map(|e| (e.uid(), e.clone())));
        merged_entities.extend(self.tokens.values().map(|e| (e.uid(), e.clone())));
        merged_entities.extend(
            vec![&self.user, &self.workload]
                .into_iter()
                .flatten()
                .map(|e| (e.uid(), e.clone())),
        );

        // Return built entities from merged collection
        BuiltEntities::from_iter(merged_entities.values().map(|e| e.uid()))
    }
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
