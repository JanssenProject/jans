// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Log entry
//! The module contains structs for logging events.

use std::collections::{HashMap, HashSet};
use std::fmt::Display;
use std::hash::Hash;
use std::sync::Arc;

use super::LogLevel;
use super::interface::{Indexed, Loggable};
use crate::common::policy_store::PoliciesContainer;
use crate::jwt::Token;
use crate::log::loggable_fn::LoggableFn;
use cedar_policy::EntityUid;
use rand::Rng;
use rand::{SeedableRng, rngs::StdRng};
use smol_str::{SmolStr, ToSmolStr};
use std::sync::{LazyLock, Mutex};
use uuid7::Uuid;

/// ISO-8601 time format for [`chrono`]
/// example: 2024-11-27T10:10:50.654Z
pub(crate) const ISO8601: &str = "%Y-%m-%dT%H:%M:%S%.3fZ";

/// [`LogEntry`] is a struct that encapsulates all relevant data for logging events.
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct LogEntry {
    /// base information of entry
    /// it is unwrap to flatten structure
    #[serde(flatten)]
    pub base: BaseLogEntry,

    /// message of the event
    pub msg: String,
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
    pub(crate) fn new(base: BaseLogEntry) -> LogEntry {
        Self {
            base,
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

impl Indexed for LogEntry {
    fn get_id(&self) -> Uuid {
        self.base.get_id()
    }

    fn get_additional_ids(&self) -> Vec<Uuid> {
        self.base.get_additional_ids()
    }

    fn get_tags(&self) -> Vec<&str> {
        self.base.get_tags()
    }
}

impl Loggable for LogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.base.get_log_level()
    }
}

/// Type of log entry
#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    serde::Serialize,
    serde::Deserialize,
    strum::IntoStaticStr,
    derive_more::Display,
)]
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
    /// List of authorize info entries for debug
    pub authorize_info: Vec<AuthorizeInfo>,
    /// is authorized
    pub authorized: bool,
}

/// Workload authorize info
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct AuthorizeInfo {
    /// cedar-policy principal
    pub principal: String,
    /// cedar-policy diagnostics information
    pub diagnostics: Diagnostics,
    /// cedar-policy decision
    pub decision: Decision,
}

/// Cedar-policy decision of the authorization
#[derive(
    Debug, Clone, PartialEq, Eq, Copy, serde::Serialize, serde::Deserialize, strum::AsRefStr,
)]
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

impl From<bool> for Decision {
    fn from(value: bool) -> Self {
        if value { Self::Allow } else { Self::Deny }
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

#[derive(Debug, Default, Clone, PartialEq, serde::Serialize)]
pub(crate) struct DiagnosticsSummary {
    /// `PolicyId`s of the policies that contributed to the decision.
    pub reason: HashSet<PolicyInfo>,
    /// Errors that occurred during authorization.
    pub errors: Vec<PolicyEvaluationError>,
}

impl DiagnosticsSummary {
    pub(crate) fn from_diagnostics(diagnostics: &[Diagnostics]) -> Self {
        let mut reason: HashSet<PolicyInfo> = HashSet::new();
        let mut errors = Vec::new();

        for diagnostic in diagnostics {
            reason.extend(diagnostic.reason.iter().cloned());
            errors.extend(diagnostic.errors.iter().cloned());
        }

        Self { reason, errors }
    }
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
        let errors = cedar_diagnostic
            .errors()
            .map(std::convert::Into::into)
            .collect();

        let reason = cedar_diagnostic
            .reason()
            .map(|policy_id| {
                let id = policy_id.to_string();

                PolicyInfo {
                    description: policies
                        .get_policy_description(id.as_str())
                        .map(std::string::ToString::to_string),
                    id: policy_id.to_string(),
                }
            })
            .collect::<HashSet<_>>();

        Self { reason, errors }
    }
}

/// log entry for decision
#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub(crate) struct DecisionLogEntry {
    /// base information of entry
    /// it is unwrap to flatten structure
    #[serde(flatten)]
    pub base: BaseLogEntry,
    /// id of policy store
    pub policystore_id: SmolStr,
    /// version of policy store
    pub policystore_version: SmolStr,
    /// describe what principal was active on authorization request
    pub principal: Vec<SmolStr>,
    /// A list of claims, specified by the `CEDARLING_DECISION_LOG_USER_CLAIMS` property, that must be present in the Cedar User entity
    #[serde(rename = "User")]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user: Option<HashMap<String, serde_json::Value>>,
    /// A list of claims, specified by the `CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS` property, that must be present in the Cedar Workload entity
    #[serde(rename = "Workload")]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workload: Option<HashMap<String, serde_json::Value>>,
    /// If this Cedarling has registered with a Lock Server, what is the `client_id` it received
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lock_client_id: Option<String>,
    /// diagnostic info about policy and errors as result of cedarling
    pub diagnostics: DiagnosticsSummary,
    /// action UID for request
    pub action: String,
    /// resource UID for request
    pub resource: String,
    /// decision for request
    pub decision: Decision,
    /// Dictionary with the token type and claims which should be included in the log
    #[serde(skip_serializing_if = "LogTokensInfo::is_empty")]
    pub tokens: LogTokensInfo,
    /// time in micro-seconds spent for decision
    pub decision_time_micro_sec: i64,
}

impl DecisionLogEntry {
    pub(crate) fn principal(user: bool, workload: bool) -> Vec<SmolStr> {
        let mut tags = Vec::with_capacity(2);
        if user {
            tags.push("User".into());
        }
        if workload {
            tags.push("Workload".into());
        }
        tags
    }

    pub(crate) fn all_principals(principals: &[EntityUid]) -> Vec<SmolStr> {
        principals
            .iter()
            .map(|uid| uid.type_name().to_smolstr())
            .collect()
    }
}

impl Indexed for DecisionLogEntry {
    fn get_id(&self) -> Uuid {
        self.base.get_id()
    }

    fn get_additional_ids(&self) -> Vec<Uuid> {
        self.base.get_additional_ids()
    }

    fn get_tags(&self) -> Vec<&str> {
        self.base.get_tags()
    }
}

impl Loggable for DecisionLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.base.get_log_level()
    }
}

fn get_std_rng() -> StdRng {
    StdRng::try_from_rng(&mut rand::rngs::SysRng).expect("failed to seed StdRng from OS RNG")
}

/// Custom uuid generation function to avoid using [`std::time`] because it makes panic in WASM
//
// TODO: maybe using wasm we can use `js_sys::Date::now()`
// Static variable initialize only once at start of program and available during all program live cycle.
// Import inside function guarantee that it is used only inside function.
pub(crate) fn gen_uuid7() -> Uuid {
    use std::sync::{LazyLock, Mutex};
    use uuid7::V7Generator;

    // from docs uuid7 crate
    // The rollback_allowance parameter specifies the amount of unix_ts_ms rollback that is considered significant.
    // A suggested value is 10_000 (milliseconds).
    const ROLLBACK_ALLOWANCE: u64 = 10_000;

    static GLOBAL_V7_GENERATOR: LazyLock<
        Mutex<V7Generator<uuid7::generator::with_rand010::Adapter<StdRng>>>,
    > = LazyLock::new(|| {
        let mut g = V7Generator::with_rand010(get_std_rng());
        g.set_rollback_allowance(ROLLBACK_ALLOWANCE);
        Mutex::new(g)
    });

    let mut g = GLOBAL_V7_GENERATOR
        .lock()
        .expect("GLOBAL_V7_GENERATOR should be locked");

    let custom_unix_ts_ms = chrono::Utc::now().timestamp_millis();

    #[allow(clippy::cast_sign_loss)]
    g.generate_or_reset_with_ts(custom_unix_ts_ms as u64)
}

/// Generates a new `UUIDv4` object utilizing the random number generator inside.
///
/// The implementation is based on the `uuid7::uuid4` function.
pub(crate) fn gen_uuid4() -> Uuid {
    static RND_UUID4: LazyLock<Mutex<StdRng>> = LazyLock::new(|| Mutex::new(get_std_rng()));

    let mut bytes = [0u8; 16];
    RND_UUID4
        .lock()
        .expect("RND_UUID4 should be locked")
        .fill_bytes(&mut bytes);

    bytes[6] = 0x40 | (bytes[6] >> 4);
    bytes[8] = 0x80 | (bytes[8] >> 2);
    Uuid::from(bytes)
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct BaseLogEntry {
    /// Unique identifier for this event.
    /// It should be uuid7
    pub id: Uuid,
    /// identifier for bunch of events (whole request)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_id: Option<Uuid>,
    /// Time of decision, in ISO-8601 time format
    /// This field is optional. Can be none if we can't have access to clock (WASM)
    /// or it is not specified in context
    pub timestamp: Option<String>,
    /// kind of log entry
    pub log_kind: LogType,
    /// log level of entry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<LogLevel>,
}

impl BaseLogEntry {
    /// Create new [`BaseLogEntry`] for System log with required `request_id`
    pub(crate) fn new_system(log_level: LogLevel, request_id: Uuid) -> Self {
        Self::new_system_opt_request_id(log_level, Some(request_id))
    }

    /// Create new [`BaseLogEntry`] for Decision log with required `request_id`
    pub(crate) fn new_decision(request_id: Uuid) -> Self {
        Self::new_decision_opt_request_id(Some(request_id))
    }

    #[allow(dead_code)]
    /// Create new [`BaseLogEntry`] for Metric log with required `request_id`
    pub(crate) fn new_metric(request_id: Uuid) -> Self {
        Self::new_metric_opt_request_id(Some(request_id))
    }

    /// Create new [`BaseLogEntry`] for System log with optional `request_id`
    /// Only System log can have log level
    pub(crate) fn new_system_opt_request_id(log_level: LogLevel, request_id: Option<Uuid>) -> Self {
        Self::new_opt_request_id(LogType::System, Some(log_level), request_id)
    }

    /// Create new [`BaseLogEntry`] for Decision log with optional `request_id`
    pub(crate) fn new_decision_opt_request_id(request_id: Option<Uuid>) -> Self {
        Self::new_opt_request_id(LogType::Decision, None, request_id)
    }

    /// Create new [`BaseLogEntry`] for Metric log with optional `request_id`
    pub(crate) fn new_metric_opt_request_id(request_id: Option<Uuid>) -> Self {
        Self::new_opt_request_id(LogType::Metric, None, request_id)
    }

    fn new_opt_request_id(
        log_type: LogType,
        log_level: Option<LogLevel>,
        request_id: Option<Uuid>,
    ) -> Self {
        let local_time_string = chrono::Local::now().format(ISO8601).to_string();

        let default_log_level = if log_type == LogType::System {
            Some(if let Some(log_level_val) = log_level {
                log_level_val
            } else {
                LogLevel::TRACE
            })
        } else {
            None
        };

        Self {
            id: gen_uuid7(),
            request_id,
            timestamp: Some(local_time_string),
            log_kind: log_type,
            level: default_log_level,
        }
    }

    /// Create [`LoggableFn`] from [`BaseLogEntry`]
    pub(crate) fn with_fn<F, R>(self, builder: F) -> LoggableFn<F>
    where
        R: Loggable + Indexed,
        for<'a> F: Fn(BaseLogEntry) -> R,
    {
        LoggableFn::new(self, builder)
    }
}

impl Indexed for BaseLogEntry {
    fn get_id(&self) -> Uuid {
        self.id
    }

    fn get_additional_ids(&self) -> Vec<Uuid> {
        // return empty vec if value is None
        self.request_id.into_iter().collect()
    }

    fn get_tags(&self) -> Vec<&'static str> {
        let mut tags = Vec::with_capacity(2);
        tags.push(self.log_kind.into());

        if let Some(level) = self.level {
            tags.push(level.into());
        }

        tags
    }
}

impl Loggable for BaseLogEntry {
    fn get_log_level(&self) -> Option<LogLevel> {
        self.level
    }
}

#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub(crate) struct LogTokensInfo(pub HashMap<String, HashMap<String, serde_json::Value>>);

impl LogTokensInfo {
    pub(crate) fn new(tokens: &HashMap<String, Arc<Token>>, decision_log_jwt_id: &str) -> Self {
        let tokens_logging_info = tokens
            .iter()
            .map(|(tkn_name, tkn)| (tkn_name.clone(), tkn.logging_info(decision_log_jwt_id)))
            .collect::<HashMap<String, HashMap<String, serde_json::Value>>>();

        Self(tokens_logging_info)
    }

    pub(crate) fn empty() -> Self {
        Self(HashMap::new())
    }

    pub(crate) fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}
