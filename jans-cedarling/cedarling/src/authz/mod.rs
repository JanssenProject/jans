// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Auth Engine
//! Part of Cedarling that main purpose is:
//! - evaluate if authorization is granted for *user*
//! - evaluate if authorization is granted for *client* / *workload *

use crate::TrustedIssuerLoadingInfo;
use crate::bootstrap_config::AuthorizationConfig;
use crate::common::default_entities::DefaultEntities;
use crate::common::policy_store::PolicyStoreWithID;
use crate::context_data_api::DataStore;
use crate::entity_builder::{BuiltEntitiesUnsigned, EntityBuilder};
use crate::jwt;
use crate::log::interface::LogWriter;
use crate::log::{
    AuthorizationLogInfo, AuthorizeInfo, BaseLogEntry, DecisionLogEntry, Diagnostics,
    DiagnosticsSummary, LogEntry, LogLevel, LogTokensInfo, Logger, PushedDataInfo, gen_uuid7,
};
use build_ctx::{build_context, build_multi_issuer_context};
use cedar_policy::{Entities, Entity, EntityUid};
use chrono::Utc;
use request::{AuthorizeMultiIssuerRequest, RequestUnsigned};
use serde_json::json;
use smol_str::SmolStr;
use std::collections::{HashMap, HashSet};
use std::io::Cursor;
use std::str::FromStr;
use std::sync::Arc;
use uuid7::Uuid;

mod authorize_result;
mod build_ctx;
mod errors;

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

    /// Get pushed data and build `PushedDataInfo` for logging.
    fn get_pushed_data(&self) -> (HashMap<String, serde_json::Value>, Option<PushedDataInfo>) {
        let pushed_data = self.config.data_store.get_all();
        let pushed_data_info = if pushed_data.is_empty() {
            None
        } else {
            // Use iterator chain - compiler optimizes this well
            Some(PushedDataInfo {
                keys: pushed_data
                    .keys()
                    .map(|k| SmolStr::from(k.as_str()))
                    .collect(),
            })
        };
        (pushed_data, pushed_data_info)
    }

    /// Evaluate Multi-Issuer Authorization Request
    ///
    /// This implementation processes multiple JWT tokens from different issuers.
    /// It validates the request format and JWT tokens, builds entities, and performs authorization evaluation.
    ///
    /// Unlike traditional authorization which uses workload/user principals, multi-issuer authorization
    /// evaluates policies based solely on the context (tokens) without requiring a principal.
    // This function orchestrates the full multi-issuer authorization flow. The complexity
    // is inherent to handling multiple token sources and splitting it would reduce readability.
    #[allow(clippy::too_many_lines)]
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
        let (pushed_data, pushed_data_info) = self.get_pushed_data();

        let context = build_multi_issuer_context(
            request.context.clone().unwrap_or(json!({})),
            &entities_data.tokens,
            &schema.schema,
            &action,
            &pushed_data,
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
                decision_diagnostics: &multi_diagnostics,
                decision_time: decision_time_micro_sec,
                principal: DecisionLogEntry::principal(
                    false, // No person principal for multi-issuer
                    false, // No workload principal for multi-issuer
                ),
                tokens_logging_info,
                decision: result.decision,
                pushed_data: pushed_data_info,
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
    // This function handles unsigned authorization flow with entity building,
    // authorization checks, and logging. The complexity is inherent to the workflow.
    #[allow(clippy::too_many_lines)]
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
        let (pushed_data, pushed_data_info) = self.get_pushed_data();

        let context = build_context(
            &self.config,
            request.context.clone(),
            &built_entities,
            &schema.schema,
            &action,
            &pushed_data,
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
                decision: result.decision,
                tokens_logging_info: LogTokensInfo::empty(),
                decision_time: decision_time_micro_sec,
                decision_diagnostics: &diagnostics,
                principal: DecisionLogEntry::all_principals(&principal_uids),
                pushed_data: pushed_data_info,
            },
        );

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
            lock_client_id: None,
            action: metadata.action.clone(),
            resource: metadata.resource.clone(),
            decision: metadata.decision.into(),
            tokens: metadata.tokens_logging_info.clone(),
            decision_time_micro_sec: metadata.decision_time,
            diagnostics: DiagnosticsSummary::from_diagnostics(metadata.decision_diagnostics),
            pushed_data: metadata.pushed_data.clone(),
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
                    action: metadata.action.clone(),
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

    /// Returns metadata for policies matching the given unsigned request parameters.
    ///
    /// Builds entity type names from `EntityData` principals/resources and parses
    /// action strings, then delegates to `PoliciesContainer::get_matching_policies`.
    pub(super) fn get_matching_policies_unsigned(
        &self,
        principals: &[crate::EntityData],
        actions: &[String],
        resources: &[crate::EntityData],
    ) -> Result<Vec<crate::PolicyMetadata>, AuthorizeError> {
        let principal_types = entity_data_to_type_names(principals)?;
        let action_uids = parse_action_uids(actions)?;
        let resource_types = entity_data_to_type_names(resources)?;

        Ok(self.config.policy_store.policies.get_matching_policies(
            &principal_types,
            &action_uids,
            &resource_types,
        ))
    }

    /// Returns metadata for policies matching the given multi-issuer request parameters.
    ///
    /// Validates tokens and extracts principal entity types from them, then
    /// delegates to `PoliciesContainer::get_matching_policies`.
    pub(super) fn get_matching_policies_multi_issuer(
        &self,
        tokens: &[crate::TokenInput],
        actions: &[String],
        resources: &[crate::EntityData],
    ) -> Result<Vec<crate::PolicyMetadata>, AuthorizeError> {
        let validated_tokens = self
            .config
            .jwt_service
            .validate_multi_issuer_tokens(tokens)?;

        let principal_types: HashSet<cedar_policy::EntityTypeName> = validated_tokens
            .keys()
            .map(|mapping| {
                cedar_policy::EntityTypeName::from_str(mapping)
                    .map_err(|e| AuthorizeError::ActionParsing(e.into()))
            })
            .collect::<Result<_, _>>()?;

        let action_uids = parse_action_uids(actions)?;
        let resource_types = entity_data_to_type_names(resources)?;

        Ok(self.config.policy_store.policies.get_matching_policies(
            &principal_types,
            &action_uids,
            &resource_types,
        ))
    }
}

/// Parse entity type names from `EntityData` slices.
fn entity_data_to_type_names(
    entities: &[crate::EntityData],
) -> Result<HashSet<cedar_policy::EntityTypeName>, AuthorizeError> {
    entities
        .iter()
        .map(|e| {
            cedar_policy::EntityTypeName::from_str(&e.cedar_mapping.entity_type)
                .map_err(|e| AuthorizeError::ActionParsing(e.into()))
        })
        .collect()
}

/// Parse action strings into `EntityUid` set.
fn parse_action_uids(actions: &[String]) -> Result<HashSet<EntityUid>, AuthorizeError> {
    actions
        .iter()
        .map(|a| EntityUid::from_str(a).map_err(Into::into))
        .collect()
}

impl TrustedIssuerLoadingInfo for Authz {
    fn is_trusted_issuer_loaded_by_name(&self, issuer_id: &str) -> bool {
        self.config
            .jwt_service
            .is_trusted_issuer_loaded_by_name(issuer_id)
    }

    fn is_trusted_issuer_loaded_by_iss(&self, iss_claim: &str) -> bool {
        self.config
            .jwt_service
            .is_trusted_issuer_loaded_by_iss(iss_claim)
    }

    fn total_issuers(&self) -> usize {
        self.config.jwt_service.total_issuers()
    }

    fn loaded_trusted_issuers_count(&self) -> usize {
        self.config.jwt_service.loaded_trusted_issuers_count()
    }

    fn loaded_trusted_issuer_ids(&self) -> HashSet<String> {
        self.config.jwt_service.loaded_trusted_issuer_ids()
    }

    fn failed_trusted_issuer_ids(&self) -> HashSet<String> {
        self.config.jwt_service.failed_trusted_issuer_ids()
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

/// Helper struct to hold named parameters for [`Authz::log_decision`] method.
struct DecisionLogMetadata<'a> {
    action: String,
    resource: String,
    principal: Vec<smol_str::SmolStr>,
    tokens_logging_info: LogTokensInfo,
    decision_diagnostics: &'a [Diagnostics],
    decision_time: i64,
    decision: bool,
    pushed_data: Option<PushedDataInfo>,
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
}
