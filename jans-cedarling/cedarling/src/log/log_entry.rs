/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # Log entry
//! The module contains structs for logging events.

use chrono::prelude::*;
use std::collections::HashSet;
use std::fmt::Display;

use uuid7::uuid7;
use uuid7::Uuid;

use crate::common::app_types::{self, ApplicationName};

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
    #[serde(skip_serializing_if = "Option::is_none")]
    pub application_id: Option<ApplicationName>,
    /// authorization information of the event
    #[serde(flatten)]
    pub auth_info: Option<AuthorizationLogInfo>,
    /// error message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_msg: Option<String>,
    /// cedar-policy language  version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cedar_lang_version: Option<semver::Version>,
    /// cedar-policy sdk  version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cedar_sdk_version: Option<semver::Version>,
}

impl LogEntry {
    pub(crate) fn new_with_data(
        pdp_id: app_types::PdpID,
        application_id: Option<app_types::ApplicationName>,
        log_kind: LogType,
    ) -> LogEntry {
        let unix_time_sec = Utc::now()
            .timestamp()
            .try_into()
            .expect("Failed to convert timestamp: value might be negative");

        Self {
            // We use uuid v7 because it is generated based on the time and sortable.
            // and we need sortable ids to use it in the sparkv database.
            // Sparkv store data in BTree. So we need have correct order of ids.
            id: uuid7(),
            time: unix_time_sec,
            log_kind,
            pdp_id: pdp_id.0,
            application_id,
            auth_info: None,
            msg: String::new(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
        }
    }

    pub(crate) fn set_message(mut self, message: String) -> Self {
        self.msg = message;
        self
    }

    pub(crate) fn set_error(mut self, error: String) -> Self {
        self.error_msg = Some(error);
        self
    }

    pub(crate) fn set_auth_info(mut self, auth_info: AuthorizationLogInfo) -> Self {
        self.auth_info = Some(auth_info);
        self
    }

    pub(crate) fn set_cedar_version(mut self) -> Self {
        self.cedar_lang_version = Some(cedar_policy::get_lang_version());
        self.cedar_sdk_version = Some(cedar_policy::get_sdk_version());
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
    /// cedar-policy action
    pub action: String,
    /// cedar-policy resource
    pub resource: String,
    /// cedar-policy context
    pub context: serde_json::Value,

    // We use actually same structures but with different unique field names.
    // It allow deserialize json to flatten structure.
    /// Person authorize info
    #[serde(flatten)]
    pub person_authorize_info: Option<PersonAuthorizeInfo>,
    /// Workload authorize info
    #[serde(flatten)]
    pub workload_authorize_info: Option<WorkloadAuthorizeInfo>,

    /// is authorized
    pub authorized: bool,
}

/// Person authorize info
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct PersonAuthorizeInfo {
    /// cedar-policy user/person principal
    pub person_principal: String,
    /// cedar-policy user/person diagnostics information
    pub person_diagnostics: Diagnostics,
    /// cedar-policy user/person decision
    pub person_decision: Decision,
}

/// Workload authorize info
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct WorkloadAuthorizeInfo {
    /// cedar-policy workload principal
    pub workload_principal: String,
    /// cedar-policy workload diagnostics information
    pub workload_diagnostics: Diagnostics,
    /// cedar-policy workload decision
    pub workload_decision: Decision,
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

impl Display for Decision {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Decision::Allow => f.write_str("ALLOW"),
            Decision::Deny => f.write_str("DENY"),
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
            reason: HashSet::from_iter(value.reason().map(|policy_id| policy_id.to_string())),
            errors: value.errors().map(|err| err.into()).collect(),
        }
    }
}
