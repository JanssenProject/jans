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
use crate::common::default_entities::DefaultEntities;
use crate::common::policy_store::PolicyStoreWithID;
use crate::data::DataStore;
use crate::entity_builder::BuiltEntities;
use crate::entity_builder::{BuiltEntitiesUnsigned, EntityBuilder};
use crate::jwt::{self, Token};
use crate::log::interface::LogWriter;
use crate::log::{
    AuthorizationLogInfo, AuthorizeInfo, BaseLogEntry, DecisionLogEntry, Diagnostics,
    DiagnosticsSummary, LogEntry, LogLevel, LogTokensInfo, Logger, PushedDataInfo, gen_uuid7,
};
use build_ctx::{build_context, build_multi_issuer_context};
use cedar_policy::{Context, Entities, Entity, EntityUid};
use chrono::Utc;
use request::{AuthorizeMultiIssuerRequest, Request, RequestUnsigned};
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::io::Cursor;
use std::str::FromStr;
use std::sync::Arc;
use trust_mode::validate_id_tkn_trust_mode;
use uuid7::Uuid;

mod authorize_result;
mod build_ctx;
mod errors;
mod trust_mode;

pub(crate) mod request;

pub use authorize_result::{AuthorizeResult, MultiIssuerAuthorizeResult};
pub use errors::*;

/// Configuration to Authz to initialize service without errors
pub(crate) struct AuthzConfig {
    pub log_service: Logger,
    pub policy_store: PolicyStoreWithID,
    pub jwt_service: Arc<jwt::JwtService>,
    pub entity_builder: Arc<EntityBuilder>,
    pub authorization: AuthorizationConfig,
    /// Data store for pushed data that gets injected into context
    pub data_store: Arc<DataStore>,
}

/// Authorization Service
/// The primary service of the Cedarling application responsible for evaluating authorization requests.
/// It leverages other services as needed to complete its evaluations.
pub(super) struct Authz {
    config: AuthzConfig,
    authorizer: cedar_policy::Authorizer,
}

impl Authz {
    /// Create a new Authorization Service
    pub(crate) fn new(config: AuthzConfig) -> Self {
        config.log_service.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::INFO,
                None,
            ))
            .set_cedar_version()
            .set_message("Cedarling Authz initialized successfully".to_string()),
        );

        Self {
            config,
            authorizer: cedar_policy::Authorizer::new(),
        }
    }

    // decode JWT tokens to structs AccessTokenData, IdTokenData, UserInfoTokenData using jwt service
    pub(super) fn decode_tokens<'a>(
        &'a self,
        request: &'a Request,
    ) -> Result<HashMap<String, Arc<Token>>, AuthorizeError> {
        let tokens = self.config.jwt_service.validate_tokens(&request.tokens)?;
        Ok(tokens)
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *person*
    /// - evaluate if authorization is granted for *workload*
    pub(super) fn authorize(&self, request: &Request) -> Result<AuthorizeResult, AuthorizeError> {
        let start_time = Utc::now();
        // We use uuid v7 because it is generated based on the time and sortable.
        // and we need sortable ids to use it in the sparkv database.
        // Sparkv store data in BTree. So we need have correct order of ids.
        //
        // Request ID should be passed to each log entry for tracing in logs and to get log entities from memory logger
        let request_id = gen_uuid7();

        let schema = &self.config.policy_store.schema;

        let tokens = self.decode_tokens(request)?;

        if let IdTokenTrustMode::Strict = self.config.authorization.id_token_trust_mode {
            validate_id_tkn_trust_mode(&tokens)?;
        }

        // Parse action UID.
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())?;

        // Parse [`cedar_policy::Entity`]-s to [`AuthorizeEntitiesData`] that hold all entities (for usability).
        let entities_data = self
            .config
            .entity_builder
            .build_entities(&tokens, &request.resource)?;

        // Get entity UIDs what we will be used on authorize check
        let resource_uid = entities_data.resource.uid();

        // Capture pushed data info for logging before context is built
        let pushed_data = self.config.data_store.get_all();
        let pushed_data_info = if pushed_data.is_empty() {
            None
        } else {
            Some(PushedDataInfo {
                count: pushed_data.len(),
                keys: pushed_data.keys().cloned().collect(),
            })
        };

        let context = build_context(
            &self.config,
            request.context.clone(),
            &entities_data.built_entities(),
            &schema.schema,
            &action,
            &self.config.data_store,
        )?;

        let workload_principal = entities_data.workload.as_ref().map(Entity::uid);
        let person_principal = entities_data.user.as_ref().map(Entity::uid);

        // Convert [`AuthorizeEntitiesData`] to  [`cedar_policy::Entities`] structure,
        // hold all entities that will be used on authorize check.
        let entities: Entities = entities_data.entities(Some(&schema.schema))?;

        let authorize_and_extract = |principal: Option<&EntityUid>, claims: &[String]| {
            Ok::<_, AuthorizeError>(
                self.authorize_principal(
                    &entities,
                    &action,
                    &resource_uid,
                    &context,
                    principal,
                    claims,
                )?
                .map(|(result, auth_info, claims)| (Some(result), Some(auth_info), Some(claims)))
                .unwrap_or_default(),
            )
        };

        let (workload_authz_result, workload_authz_info, workload_entity_claims) =
            authorize_and_extract(
                workload_principal.as_ref(),
                &self.config.authorization.decision_log_workload_claims,
            )?;
        let (user_authz_result, user_authz_info, user_entity_claims) = authorize_and_extract(
            person_principal.as_ref(),
            &self.config.authorization.decision_log_user_claims,
        )?;

        let result = AuthorizeResult::new(
            &self.config.authorization.principal_bool_operator,
            workload_principal.as_ref(),
            person_principal.as_ref(),
            workload_authz_result,
            user_authz_result,
            request_id,
        )?;

        // measure time how long request executes
        let decision_time_micro_sec = calculate_elapsed_time(start_time);

        // FROM THIS POINT WE ONLY MAKE LOGS

        let decision_diagnostics =
            collect_diagnostics(&[user_authz_info.as_ref(), workload_authz_info.as_ref()]);

        // Log policy evaluation errors per principal if any exist
        if let Some(info) = user_authz_info.as_ref() {
            self.log_policy_evaluation_errors(&info.diagnostics, "user principal", request_id);
        }

        if let Some(info) = workload_authz_info.as_ref() {
            self.log_policy_evaluation_errors(&info.diagnostics, "workload principal", request_id);
        }

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        let decision_log_metadata = DecisionLogMetadata {
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            user_claims: user_entity_claims,
            workload_claims: workload_entity_claims,
            decision_diagnostics: &decision_diagnostics,
            decision_time: decision_time_micro_sec,
            decision: result.decision,
            principal: DecisionLogEntry::principal(
                result.person.is_some(),
                result.workload.is_some(),
            ),
            tokens_logging_info: LogTokensInfo::new(
                &tokens,
                self.config
                    .authorization
                    .decision_log_default_jwt_id
                    .as_str(),
            ),
        };
        self.log_decision(request_id, &decision_log_metadata);

        // DEBUG LOG
        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        let debug_log_metadata = DebugLogMetadata {
            action: request.action.clone(),
            resource: resource_uid.to_string(),
            context: request.context.clone(),
            entities: &entities,
            debug_authz_info: [user_authz_info, workload_authz_info]
                .into_iter()
                .flatten()
                .collect(),
            decision: result.decision,
        };
        self.log_debug(request_id, &debug_log_metadata);

        if !result.decision {
            self.log_failed_diagnostics(&decision_diagnostics, request_id);
        }

        Ok(result)
    }

    /// Evaluate Multi-Issuer Authorization Request
    ///
    /// This implementation processes multiple JWT tokens from different issuers.
    /// It validates the request format and JWT tokens, builds entities, and performs authorization evaluation.
    ///
    /// Unlike traditional authorization which uses workload/user principals, multi-issuer authorization
    /// evaluates policies based solely on the context (tokens) without requiring a principal.
    pub(super) fn authorize_multi_issuer(
        &self,
        request: &AuthorizeMultiIssuerRequest,
    ) -> Result<MultiIssuerAuthorizeResult, AuthorizeError> {
        let start_time = Utc::now();
        let request_id = gen_uuid7();

        // Validate the request structure
        request.validate()?;

        let schema = &self.config.policy_store.schema;

        let validated_tokens = self
            .config
            .jwt_service
            .validate_multi_issuer_tokens(&request.tokens)?;

        let entities_data = self
            .config
            .entity_builder
            .build_multi_issuer_entities(
                &validated_tokens,
                &request.resource,
                self.config.log_service.as_ref(),
            )
            .map_err(AuthorizeError::MultiIssuerEntity)?;

        let action = cedar_policy::EntityUid::from_str(request.action.as_str())?;

        // Capture pushed data info for logging before context is built
        let pushed_data = self.config.data_store.get_all();
        let pushed_data_info = if pushed_data.is_empty() {
            None
        } else {
            Some(PushedDataInfo {
                count: pushed_data.len(),
                keys: pushed_data.keys().cloned().collect(),
            })
        };

        let context = build_multi_issuer_context(
            request.context.clone().unwrap_or(json!({})),
            &entities_data.tokens,
            &schema.schema,
            &action,
            &self.config.data_store,
        )?;

        let resource_uid = entities_data.resource.uid();

        let entities = entities_data.entities(Some(&schema.schema))?;

        // Multi-issuer authorization does not use a principal
        // Authorization is based solely on the context (tokens)
        let authz_result = self.execute_authorize(ExecuteAuthorizeParameters {
            entities: &entities,
            principal: None,
            action: action.clone(),
            resource: resource_uid.clone(),
            context,
        })?;

        let authz_info = AuthorizeInfo {
            principal: "None (multi-issuer)".to_string(),
            diagnostics: Diagnostics::new(
                authz_result.diagnostics(),
                &self.config.policy_store.policies,
            ),
            decision: authz_result.decision().into(),
        };

        let result = MultiIssuerAuthorizeResult::new(authz_result.clone(), request_id);

        // measure time how long request executes
        let decision_time_micro_sec = calculate_elapsed_time(start_time);

        // FROM THIS POINT WE ONLY MAKE LOGS

        // Log policy evaluation errors if any exist
        self.log_policy_evaluation_errors(
            &authz_info.diagnostics,
            "multi-issuer (no principal)",
            request_id,
        );

        let tokens_logging_info = LogTokensInfo::new(
            &validated_tokens,
            self.config
                .authorization
                .decision_log_default_jwt_id
                .as_str(),
        );

        let multi_diagnostics = vec![authz_info.diagnostics.clone()];

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        self.log_decision(
            request_id,
            &DecisionLogMetadata {
                action: request.action.clone(),
                resource: resource_uid.to_string(),
                user_claims: None,     // No user claims for multi-issuer
                workload_claims: None, // No workload claims for multi-issuer
                decision_diagnostics: &multi_diagnostics,
                decision_time: decision_time_micro_sec,
                principal: DecisionLogEntry::principal(
                    false, // No person principal for multi-issuer
                    false, // No workload principal for multi-issuer
                ),
                tokens_logging_info,
                decision: result.decision,
            },
        );

        // DEBUG LOG
        // Log all result information about multi-issuer authorization
        let debug_log_fn = BaseLogEntry::new_system(LogLevel::DEBUG, request_id).with_fn(|base| {
            // usually debug log is disabled, so we build entities_json only when needed
            // error should newer happen here, because entities were built successfully before
            let entities_json: serde_json::Value = {
                // getting entities as json
                serialize_entities(&entities)
            };

            LogEntry::new(base)
                .set_auth_info(AuthorizationLogInfo {
                    action: request.action.clone(),
                    context: request.context.clone().unwrap_or(json!({})),
                    resource: resource_uid.to_string(),
                    entities: entities_json,
                    authorize_info: vec![authz_info.clone()],
                    authorized: result.decision,
                })
                .set_message("Result of multi-issuer authorize.".to_string())
        });
        self.config.log_service.log_fn(debug_log_fn);

        Ok(result)
    }

    /// Evaluate Authorization Request with unsigned data.
    pub(super) fn authorize_unsigned(
        &self,
        request: &RequestUnsigned,
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
        let action = cedar_policy::EntityUid::from_str(request.action.as_str())?;

        let BuiltEntitiesUnsigned {
            principals,
            roles,
            resource,
            built_entities,
        } = self
            .config
            .entity_builder
            .build_entities_unsigned(request)?;
        let principal_uids = principals
            .iter()
            .map(cedar_policy::Entity::uid)
            .collect::<Vec<EntityUid>>();
        let resource_uid = resource.uid();

        // Capture pushed data info for logging before context is built
        let pushed_data = self.config.data_store.get_all();
        let pushed_data_info = if pushed_data.is_empty() {
            None
        } else {
            Some(PushedDataInfo {
                count: pushed_data.len(),
                keys: pushed_data.keys().cloned().collect(),
            })
        };

        let context = build_context(
            &self.config,
            request.context.clone(),
            &built_entities,
            &schema.schema,
            &action,
            &self.config.data_store,
        )?;

        let entities = Entities::from_entities(
            principals.into_iter().chain(roles).chain([resource]),
            Some(&schema.schema),
        )
        .map_err(Box::new)?;

        let mut principal_responses = HashMap::new();

        for principal_uid in &principal_uids {
            let auth_result = self.execute_authorize(ExecuteAuthorizeParameters {
                entities: &entities,
                principal: Some(principal_uid.clone()),
                action: action.clone(),
                resource: resource_uid.clone(),
                context: context.clone(),
            })?;

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
        let decision_time_micro_sec = calculate_elapsed_time(start_time);

        // FROM THIS POINT WE ONLY MAKE LOGS

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
            .map(|info| info.diagnostics.clone())
            .collect::<Vec<_>>();

        // Log policy evaluation errors if any exist
        for info in &debug_authorize_info {
            self.log_policy_evaluation_errors(&info.diagnostics, &info.principal, request_id);
        }

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        self.log_decision(
            request_id,
            &DecisionLogMetadata {
                action: request.action.clone(),
                resource: resource_uid.to_string(),
                decision: result.decision.into(),
                tokens: LogTokensInfo::empty(),
                decision_time_micro_sec,
                diagnostics: DiagnosticsSummary::from_diagnostics(&diagnostics),
            },
        );
        self.config.log_service.log_fn(unsigned_decision_log_fn);

        // DEBUG LOG
        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        self.log_debug(
            request_id,
            &DebugLogMetadata {
                action: request.action.clone(),
                resource: resource_uid.to_string(),
                context: request.context.clone(),
                entities: &entities,
                debug_authz_info: debug_authorize_info,
                decision: result.decision,
            },
        );

        if !result.decision {
            self.log_failed_diagnostics(&diagnostics, request_id);
        }

        Ok(result)
    }

    /// Execute cedar policy `is_authorized` method to check
    /// if allowed make request with given parameters
    fn execute_authorize(
        &self,
        parameters: ExecuteAuthorizeParameters,
    ) -> Result<cedar_policy::Response, Box<cedar_policy::RequestValidationError>> {
        let mut request_builder = cedar_policy::Request::builder()
            .action(parameters.action)
            .resource(parameters.resource)
            .context(parameters.context)
            .schema(&self.config.policy_store.schema.schema);

        if let Some(principal) = parameters.principal {
            request_builder = request_builder.principal(principal);
        }

        let request = request_builder.build().map_err(Box::new)?;

        let response = self.authorizer.is_authorized(
            &request,
            self.config.policy_store.policies.get_set(),
            parameters.entities,
        );

        Ok(response)
    }

    #[cfg(test)]
    pub(super) fn build_entities(
        &self,
        request: &Request,
        tokens: &HashMap<String, Arc<Token>>,
    ) -> Result<AuthorizeEntitiesData, Box<AuthorizeError>> {
        self.config
            .entity_builder
            .build_entities(tokens, &request.resource)
            .map_err(|e| Box::new(AuthorizeError::from(e)))
    }

    /// Log policy evaluation errors for diagnostics
    fn log_policy_evaluation_errors(
        &self,
        diagnostics: &Diagnostics,
        principal_name: &str,
        request_id: Uuid,
    ) {
        if !diagnostics.errors.is_empty() {
            let log_entry = LogEntry::new(BaseLogEntry::new_decision(request_id))
                .set_message(format!("Policy evaluation errors for {principal_name}"))
                .set_error(format!("{:?}", diagnostics.errors));
            self.config.log_service.log_any(log_entry);
        }
    }

    /// Logs a summary of all diagnostics errors when authorization is denied.
    ///
    /// This provides a consolidated view of all policy evaluation errors across all principals,
    /// complementing the per-principal error logs. Only logs when there are actual errors
    /// to avoid noise.
    fn log_failed_diagnostics(&self, diagnostics: &[Diagnostics], request_id: Uuid) {
        let all_errors: Vec<_> = diagnostics.iter().flat_map(|d| &d.errors).collect();

        if all_errors.is_empty() {
            return;
        }

        let serialized_errors = serde_json::to_string(&all_errors)
            .unwrap_or_else(|_| "failed to serialize diagnostics errors".to_string());

        let log_entry = LogEntry::new(BaseLogEntry::new_decision(request_id))
            .set_message(
                "Authorization denied: summary of all policy evaluation errors".to_string(),
            )
            .set_error(serialized_errors);

        self.config.log_service.log_any(log_entry);
    }

    /// Logs a decision log entry.
    fn log_decision(&self, request_id: Uuid, metadata: &DecisionLogMetadata) {
        let entry = BaseLogEntry::new_decision(request_id).with_fn(|base| DecisionLogEntry {
            base,
            policystore_id: self.config.policy_store.id.as_str().into(),
            policystore_version: self.config.policy_store.get_store_version().into(),
            principal: metadata.principal.clone(),
            user: metadata.user_claims.clone(),
            workload: metadata.workload_claims.clone(),
            lock_client_id: None,
            action: metadata.action.to_string(),
            resource: metadata.resource.clone(),
            decision: metadata.decision.into(),
            tokens: metadata.tokens_logging_info.clone(),
            decision_time_micro_sec: metadata.decision_time,
            diagnostics: DiagnosticsSummary::from_diagnostics(metadata.decision_diagnostics),
        });
        self.config.log_service.log_fn(entry);
    }

    /// Logs a debug log entry.
    fn log_debug(&self, request_id: Uuid, metadata: &DebugLogMetadata) {
        let debug_log_fn = BaseLogEntry::new_system(LogLevel::DEBUG, request_id).with_fn(|base| {
            // usually debug log is disabled, so we build entities_json only when needed
            // error should newer happen here, because entities were built successfully before
            let entities_json: serde_json::Value = {
                // getting entities as json
                serialize_entities(metadata.entities)
            };

            LogEntry::new(base)
                .set_auth_info(AuthorizationLogInfo {
                    action: metadata.action.to_string(),
                    context: metadata.context.clone(),
                    resource: metadata.resource.clone(),
                    entities: entities_json,
                    authorize_info: metadata.debug_authz_info.clone(),
                    authorized: metadata.decision,
                })
                .set_message("Result of authorize.".to_string())
        });
        self.config.log_service.log_fn(debug_log_fn);
    }

    /// Helper function to authorize a principal against a given resource.
    fn authorize_principal(
        &self,
        entities: &Entities,
        action: &EntityUid,
        resource: &EntityUid,
        context: &Context,
        principal: Option<&EntityUid>,
        claim_fields: &[String],
    ) -> Result<Option<PrincipalResult>, AuthorizeError> {
        let Some(principal) = principal else {
            return Ok(None);
        };

        let authz_result = self
            .execute_authorize(ExecuteAuthorizeParameters {
                entities,
                principal: Some(principal.clone()),
                action: action.clone(),
                resource: resource.clone(),
                context: context.clone(),
            })
            .map_err(|err| InvalidPrincipalError::new(principal, *err))?;

        let authz_info = AuthorizeInfo {
            principal: principal.to_string(),
            diagnostics: Diagnostics::new(
                authz_result.diagnostics(),
                &self.config.policy_store.policies,
            ),
            decision: authz_result.decision().into(),
        };

        let claims = get_entity_claims(claim_fields, entities, principal);

        Ok(Some((authz_result, authz_info, claims)))
    }
}

fn calculate_elapsed_time(start_time: chrono::DateTime<Utc>) -> i64 {
    let since_start = Utc::now().signed_duration_since(start_time);
    since_start.num_microseconds().unwrap_or(
        //overflow (exceeding 2^63 microseconds in either direction)
        i64::MAX,
    )
}

fn serialize_entities(entities: &Entities) -> serde_json::Value {
    let mut buf = Vec::new();
    let cursor = Cursor::new(&mut buf);
    entities
        .write_to_json(cursor)
        .ok()
        .and_then(|()| serde_json::from_slice(buf.as_slice()).ok())
        .unwrap_or(serde_json::Value::Null)
}

fn collect_diagnostics(infos: &[Option<&AuthorizeInfo>]) -> Vec<Diagnostics> {
    infos
        .iter()
        .flatten()
        .map(|info| info.diagnostics.clone())
        .collect()
}

type PrincipalResult = (
    cedar_policy::Response,
    AuthorizeInfo,
    HashMap<String, serde_json::Value>,
);

/// Helper struct to hold named parameters for [`Authz::log_decision`] method.
struct DecisionLogMetadata<'a> {
    action: String,
    resource: String,
    principal: Vec<smol_str::SmolStr>,
    tokens_logging_info: LogTokensInfo,
    user_claims: Option<HashMap<String, serde_json::Value>>,
    workload_claims: Option<HashMap<String, serde_json::Value>>,
    decision_diagnostics: &'a [Diagnostics],
    decision_time: i64,
    decision: bool,
}

/// Helper struct to hold named parameters for [`Authz::log_debug`] method.
struct DebugLogMetadata<'a> {
    action: String,
    resource: String,
    context: serde_json::Value,
    entities: &'a Entities,
    debug_authz_info: Vec<AuthorizeInfo>,
    decision: bool,
}

/// Helper struct to hold named parameters for [`Authz::execute_authorize`] method.
struct ExecuteAuthorizeParameters<'a> {
    entities: &'a Entities,
    principal: Option<EntityUid>,
    action: EntityUid,
    resource: EntityUid,
    context: cedar_policy::Context,
}

/// Structure to hold entites created from tokens
#[derive(Debug)]
pub(super) struct AuthorizeEntitiesData {
    pub issuers: HashSet<Entity>,
    pub tokens: HashMap<String, Entity>,
    pub workload: Option<Entity>,
    pub user: Option<Entity>,
    pub roles: Vec<Entity>,
    pub resource: Entity,
    pub default_entities: DefaultEntities,
}

impl AuthorizeEntitiesData {
    /// Create iterator to get all entities
    ///
    /// This method merges request entities with default entities, where request entities
    /// take precedence over default entities in case of UID conflicts.
    fn into_iter(self) -> impl Iterator<Item = Entity> {
        let mut merged_entities: HashMap<EntityUid, Entity> = HashMap::new();

        // Add default entities first
        merged_entities.extend(
            self.default_entities
                .inner
                .into_values()
                .map(|e| (e.uid(), e)),
        );

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
        merged_entities.extend(
            self.default_entities
                .inner
                .values()
                .map(|e| (e.uid(), e.clone())),
        );

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
        merged_entities
            .values()
            .map(cedar_policy::Entity::uid)
            .collect::<BuiltEntities>()
    }
}

/// Get entity claims from list in config
// To get claims we convert entity to json, because no other way to get introspection
fn get_entity_claims(
    decision_log_claims: &[String],
    entities: &Entities,
    entity_uid: &EntityUid,
) -> HashMap<String, serde_json::Value> {
    decision_log_claims
        .iter()
        .filter_map(|claim_key| {
            entities
                .get(entity_uid)
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
                        .map(std::borrow::ToOwned::to_owned)
                    )
                )
                .flatten()
                // convert to (String, Value) tuple
                .map(|attr_json| (claim_key.clone(),attr_json.clone()))
        })
        .collect::<HashMap<_, _>>()
}
