/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # Log entry
//! The module contains structs for logging events.

use std::collections::HashSet;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};

use di::{DependencyMap, DependencySupplier};
use uuid7::uuid7;
use uuid7::Uuid;

use super::app_types;

/// LogEntry is a struct that encapsulates all relevant data for logging events.
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct LogEntry {
    /// unique identifier for this event
    pub id: Uuid,
    /// Time of decision, in unix time
    pub time: u64,
    /// kind of log entry
    pub log_kind: LogType,
    /// unique id of cedarling
    pub pdp_id: Uuid,
    /// message of the event
    pub msg: String,
    /// name of application from [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties)
    pub application_id: String,
    /// authorization information of the event
    #[serde(flatten)]
    pub auth_info: Option<AuthorizationLogInfo>,
}

impl LogEntry {
    pub(crate) fn new_with_container(dep_map: &DependencyMap, log_kind: LogType) -> LogEntry {
        let app_id: Arc<app_types::ApplicationName> = dep_map.get();
        Self::new_with_data(*dep_map.get(), app_id.as_ref().clone(), log_kind)
    }

    pub(crate) fn new_with_data(
        pdp_id: app_types::PdpID,
        application_id: app_types::ApplicationName,
        log_kind: LogType,
    ) -> LogEntry {
        let unix_time_sec = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();

        Self {
            // We use uuid v7 because it is generated based on the time and sortable.
            // and we need sortable ids to use it in the sparkv database.
            // Sparkv store data in BTree. So we need have correct order of ids.
            id: uuid7(),
            time: unix_time_sec,
            log_kind,
            pdp_id: pdp_id.0,
            application_id: application_id.0,
            auth_info: None,
            msg: String::new(),
        }
    }

    pub(crate) fn set_message(mut self, message: String) -> Self {
        self.msg = message;
        self
    }

    #[allow(dead_code)]
    pub(crate) fn set_auth_info(mut self, auth_info: AuthorizationLogInfo) -> Self {
        self.auth_info = Some(auth_info);
        self
    }
}

/// Type of log entry
#[derive(Debug, Clone, Copy, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum LogType {
    Decision,
    System,
    Metric,
}

/// Log information about authorization request
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct AuthorizationLogInfo {
    /// cedar-policy principal
    pub principal: String,
    /// cedar-policy action
    pub action: String,
    /// cedar-policy resource
    pub resource: String,
    /// cedar-policy context
    pub context: serde_json::Value,
    /// cedar-policy decision
    pub decision: Decision,
    /// cedar-policy diagnostics information
    pub diagnostics: Diagnostics,
}

/// Cedar-policy decision of the authorization
#[derive(Debug, Clone, PartialEq, Eq, Copy, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum Decision {
    /// Determined that the request should be allowed
    Allow,
    /// Determined that the request should be denied.
    Deny,
}

impl ToString for Decision {
    fn to_string(&self) -> String {
        match self {
            Decision::Allow => "ALLOW".to_string(),
            Decision::Deny => "DENY".to_string(),
        }
    }
}

#[doc(hidden)]
impl From<cedar_policy::Decision> for Decision {
    fn from(value: cedar_policy::Decision) -> Self {
        match value {
            cedar_policy::Decision::Allow => Decision::Allow,
            cedar_policy::Decision::Deny => Decision::Deny,
        }
    }
}

/// An error occurred when evaluating a policy
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct PolicyEvaluationError {
    /// Id of the policy with an error
    pub id: String,
    /// Underlying evaluation error string representation
    pub error: String,
}

#[doc(hidden)]
impl From<&cedar_policy::AuthorizationError> for PolicyEvaluationError {
    fn from(value: &cedar_policy::AuthorizationError) -> Self {
        match value {
            cedar_policy::AuthorizationError::PolicyEvaluationError(policy_evaluation_error) => {
                Self {
                    id: policy_evaluation_error.policy_id().to_string(),
                    error: policy_evaluation_error.inner().to_string(),
                }
            },
        }
    }
}

/// Diagnostics providing more information on how a `Decision` was reached
#[derive(Debug, Default, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Diagnostics {
    /// `PolicyId`s of the policies that contributed to the decision.
    /// If no policies applied to the request, this set will be empty.
    pub reason: HashSet<String>,
    /// Errors that occurred during authorization. The errors should be
    /// treated as unordered, since policies may be evaluated in any order.
    pub errors: Vec<PolicyEvaluationError>,
}

#[doc(hidden)]
impl From<&cedar_policy::Diagnostics> for Diagnostics {
    fn from(value: &cedar_policy::Diagnostics) -> Self {
        Self {
            reason: HashSet::from_iter(
                value
                    .reason()
                    .into_iter()
                    .map(|policy_id| policy_id.to_string()),
            ),
            errors: value.errors().into_iter().map(|err| err.into()).collect(),
        }
    }
}
