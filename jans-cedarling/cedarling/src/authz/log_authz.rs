// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{AuthorizeResult, Authz, DecodedTokens, LogWriter};
use crate::log::{
    AuthorizationLogInfo, BaseLogEntry, DecisionLogEntry, DiagnosticsRefs, LogTokensInfo, LogType,
    PrincipalLogEntry, UserAuthorizeInfo, WorkloadAuthorizeInfo,
};
use crate::{LogLevel, Request};
use cedar_policy::EntityUid;
use cedar_policy::{Entities, entities_errors::EntitiesError};
use serde_json::Value;
use std::{collections::HashMap, io::Cursor};

pub struct LogAuthzArgs<'a> {
    pub tokens: DecodedTokens<'a>,
    pub entities: &'a Entities,
    pub user_authz_info: Option<UserAuthorizeInfo>,
    pub workload_authz_info: Option<WorkloadAuthorizeInfo>,
    pub user_entity_claims: Option<HashMap<String, Value>>,
    pub workload_entity_claims: Option<HashMap<String, Value>>,
    pub request: &'a Request,
    pub resource: EntityUid,
    pub result: &'a AuthorizeResult,
    pub elapsed_ms: i64,
}

impl Authz {
    pub fn log_authz(&self, args: LogAuthzArgs) -> Result<(), LoggingError> {
        // getting entities as json
        let mut entities_raw_json = Vec::new();
        let cursor = Cursor::new(&mut entities_raw_json);

        args.entities.write_to_json(cursor)?;
        let entities_json: serde_json::Value =
            serde_json::from_slice(entities_raw_json.as_slice())?;

        let user_authz_diagnostic = args
            .user_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        let workload_authz_diagnostic = args
            .user_authz_info
            .as_ref()
            .map(|auth_info| &auth_info.diagnostics);

        let tokens_logging_info = LogTokensInfo {
            access: args.tokens.access.as_ref().map(|tkn| {
                tkn.logging_info(
                    self.config
                        .authorization
                        .decision_log_default_jwt_id
                        .as_str(),
                )
            }),
            id_token: args.tokens.id.as_ref().map(|tkn| {
                tkn.logging_info(
                    self.config
                        .authorization
                        .decision_log_default_jwt_id
                        .as_str(),
                )
            }),
            userinfo: args.tokens.userinfo.as_ref().map(|tkn| {
                tkn.logging_info(
                    self.config
                        .authorization
                        .decision_log_default_jwt_id
                        .as_str(),
                )
            }),
        };

        // Decision log
        // we log decision log before debug log, to avoid cloning diagnostic info
        self.config.log_service.as_ref().log_any(&DecisionLogEntry {
            base: BaseLogEntry::new(self.config.pdp_id, LogType::Decision),
            policystore_id: self.config.policy_store.id.as_str(),
            policystore_version: self.config.policy_store.get_store_version(),
            principal: PrincipalLogEntry::new(&self.config.authorization),
            user: args.user_entity_claims,
            workload: args.workload_entity_claims,
            lock_client_id: None,
            action: args.request.action.clone(),
            resource: args.resource.to_string(),
            decision: args.result.decision.into(),
            tokens: tokens_logging_info,
            decision_time_ms: args.elapsed_ms,
            diagnostics: DiagnosticsRefs::new(&[
                &user_authz_diagnostic,
                &workload_authz_diagnostic,
            ]),
        });

        // DEBUG LOG
        // Log all result information about both authorize checks.
        // Where principal is `"Jans::Workload"` and where principal is `"Jans::User"`.
        self.config.log_service.as_ref().log(
            crate::log::LogEntry::new_with_data(
                self.config.pdp_id,
                Some(self.config.application_name.clone()),
                LogType::System,
            )
            .set_level(LogLevel::DEBUG)
            .set_auth_info(AuthorizationLogInfo {
                action: args.request.action.clone(),
                context: args.request.context.clone(),
                resource: args.resource.to_string(),
                entities: entities_json,
                person_authorize_info: args.user_authz_info,
                workload_authorize_info: args.workload_authz_info,
                authorized: args.result.decision,
            })
            .set_message("Result of authorize.".to_string()),
        );

        Ok(())
    }
}

#[derive(Debug, thiserror::Error)]
pub enum LoggingError {
    #[error("failed to write entities to json file: {0}")]
    WriteEntitiesToJsonFile(#[from] EntitiesError),
    #[error("failed to serialize entities into json: {0}")]
    SerializeEntitiesToJson(#[from] serde_json::error::Error),
}
