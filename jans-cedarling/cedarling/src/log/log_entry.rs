/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # Log entry
//! The module contains structs for logging events.

use std::collections::HashMap;
use std::collections::HashSet;
use std::fmt::Display;

use std::hash::Hash;

use uuid7::uuid7;
use uuid7::Uuid;

use crate::bootstrap_config::AuthorizationConfig;
use crate::common::app_types::{self, ApplicationName};
use crate::common::policy_store::PoliciesContainer;

use super::interface::Loggable;

/// ISO-8601 time format for [`chrono`]
/// example: 2024-11-27T10:10:50.654Z
const ISO8601: &str = "%Y-%m-%dT%H:%M:%S%.3fZ";

/// LogEntry is a struct that encapsulates all relevant data for logging events.
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct LogEntry {
    /// base information of entry
    /// it is unwrap to flatten structure
    #[serde(flatten)]
    pub base: BaseLogEntry,

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
        log_type: LogType,
    ) -> LogEntry {
        Self {
            base: BaseLogEntry::new(pdp_id, log_type),
            // We use uuid v7 because it is generated based on the time and sortable.
            // and we need sortable ids to use it in the sparkv database.
            // Sparkv store data in BTree. So we need have correct order of ids.
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

impl Loggable for LogEntry {
    fn get_request_id(&self) -> Uuid {
        self.base.get_request_id()
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
    /// cedar-policy entities json presentation for forensic analysis
    pub entities: serde_json::Value,

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
    pub reason: HashSet<PolicyInfo>,
    /// Errors that occurred during authorization. The errors should be
    /// treated as unordered, since policies may be evaluated in any order.
    pub errors: Vec<PolicyEvaluationError>,
}

/// Policy diagnostic info
#[derive(Debug, Default, Clone, Eq, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct PolicyInfo {
    pub id: String,
    pub description: Option<String>,
}

impl Hash for PolicyInfo {
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        self.id.hash(state);
    }
}

impl Diagnostics {
    /// Create new [`Diagnostics`] info structure for logging based on [`cedar_policy::Diagnostics`]
    pub(crate) fn new(
        cedar_diagnostic: &cedar_policy::Diagnostics,
        policies: &PoliciesContainer,
    ) -> Self {
        let errors = cedar_diagnostic.errors().map(|err| err.into()).collect();

        let reason = HashSet::from_iter(cedar_diagnostic.reason().map(|policy_id| {
            let id = policy_id.to_string();

            PolicyInfo {
                description: policies
                    .get_policy_description(id.as_str())
                    .map(|v| v.to_string()),
                id: policy_id.to_string(),
            }
        }));

        Self { reason, errors }
    }
}

/// log entry for decision
#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub struct DecisionLogEntry<'a> {
    /// base information of entry
    /// it is unwrap to flatten structure
    #[serde(flatten)]
    pub base: BaseLogEntry,
    /// id of policy store
    pub policystore_id: &'a str,
    /// version of policy store
    pub policystore_version: &'a str,
    /// describe what principal was active on authorization request
    pub principal: PrincipalLogEntry,
    /// A list of claims, specified by the CEDARLING_DECISION_LOG_USER_CLAIMS property, that must be present in the Cedar User entity
    #[serde(rename = "User")]
    pub user: HashMap<String, serde_json::Value>,
    /// A list of claims, specified by the CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS property, that must be present in the Cedar Workload entity
    #[serde(rename = "Workload")]
    pub workload: HashMap<String, serde_json::Value>,
    /// If this Cedarling has registered with a Lock Server, what is the client_id it received
    pub lock_client_id: Option<String>,
    /// action UID for request
    pub action: String,
    /// resource UID for request
    pub resource: String,
    /// decision for request
    pub decision: Decision,
    /// Dictionary with the token type and claims which should be included in the log
    pub tokens: LogTokensInfo<'a>,
    /// time in milliseconds spent for decision
    pub decision_time_ms: u128,
}

impl Loggable for &DecisionLogEntry<'_> {
    fn get_request_id(&self) -> Uuid {
        self.base.get_request_id()
    }
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct BaseLogEntry {
    /// unique identifier for this event
    pub request_id: Uuid,
    /// Time of decision, in ISO-8601 time format
    /// This field is optional. Can be none if we can't have access to clock (WASM)
    /// or it is not specified in context
    pub timestamp: Option<String>,
    /// kind of log entry
    pub log_type: LogType,
    /// unique id of cedarling
    pub pdp_id: Uuid,
}

impl BaseLogEntry {
    pub(crate) fn new(pdp_id: app_types::PdpID, log_type: LogType) -> Self {
        let local_time_string = chrono::Local::now().format(ISO8601).to_string();

        Self {
            // We use uuid v7 because it is generated based on the time and sortable.
            // and we need sortable ids to use it in the sparkv database.
            // Sparkv store data in BTree. So we need have correct order of ids.
            request_id: uuid7(),
            timestamp: Some(local_time_string),
            log_type,
            pdp_id: pdp_id.0,
        }
    }
}

impl Loggable for BaseLogEntry {
    fn get_request_id(&self) -> Uuid {
        self.request_id
    }
}

/// Describes what principal is was executed
//
// is used only for logging
#[derive(Debug, Clone, PartialEq)]
pub enum PrincipalLogEntry {
    User,
    Workload,
    UserAndWorkload,
    UserORWorkload,
    // corner case, should never happen
    None,
}

impl PrincipalLogEntry {
    pub(crate) fn new(conf: &AuthorizationConfig) -> Self {
        match (
            conf.use_user_principal,
            conf.use_workload_principal,
            conf.user_workload_operator,
        ) {
            (true, true, crate::WorkloadBoolOp::And) => Self::UserAndWorkload,
            (true, true, crate::WorkloadBoolOp::Or) => Self::UserORWorkload,
            (true, false, _) => Self::User,
            (false, true, _) => Self::Workload,
            (false, false, _) => Self::None,
        }
    }
}

impl Display for PrincipalLogEntry {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let str_val = match self {
            Self::User => "User",
            Self::Workload => "Workload",
            Self::UserAndWorkload => "User & Workload",
            Self::UserORWorkload => "User | Workload",
            Self::None => "none",
        };

        f.write_str(str_val)
    }
}

// implement Serialize for PrincipalLogEntry to use Display trait
impl serde::Serialize for PrincipalLogEntry {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_str(self.to_string().as_str())
    }
}

#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub struct LogTokensInfo<'a> {
    pub id_token: Option<HashMap<&'a str, &'a serde_json::Value>>,
    #[serde(rename = "Userinfo")]
    pub userinfo: Option<HashMap<&'a str, &'a serde_json::Value>>,
    pub access: Option<HashMap<&'a str, &'a serde_json::Value>>,
}
