/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::AuthorizeError;
use crate::bootstrap_config::WorkloadBoolOp;
use cedar_policy::{Decision, PolicyId};
use serde::{Serialize, Serializer, ser::SerializeStruct};
use std::collections::HashSet;

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
#[derive(Debug, Serialize)]
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    #[serde(serialize_with = "serialize_opt_response")]
    pub workload: Option<cedar_policy::Response>,
    /// Result of authorization where principal is `Jans::User`
    #[serde(serialize_with = "serialize_opt_response")]
    pub user: Option<cedar_policy::Response>,

    /// Reasons why the authorization failed due to malformed inputs
    #[serde(
        serialize_with = "serialize_reason_input",
        skip_serializing_if = "Option::is_none"
    )]
    pub reason_input: Option<AuthorizeError>,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,
}

/// Custom serializer for an Option<String> which converts `None` to an empty string and vice versa.
pub fn serialize_opt_response<S>(
    value: &Option<cedar_policy::Response>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    match value {
        None => serializer.serialize_none(),
        Some(response) => {
            let decision = match response.decision() {
                Decision::Allow => "allow",
                Decision::Deny => "deny",
            };

            let diagnostics = response.diagnostics();
            let reason = diagnostics
                .reason()
                .map(|r| r.to_string())
                .collect::<HashSet<String>>();
            let errors = diagnostics
                .errors()
                .map(|e| e.to_string())
                .collect::<HashSet<String>>();

            let mut state = serializer.serialize_struct("Response", 3)?;
            state.serialize_field("decision", decision)?;
            state.serialize_field("reason", &reason)?;
            state.serialize_field("errors", &errors)?;
            state.end()
        },
    }
}

impl<E> From<E> for AuthorizeResult
where
    E: Into<AuthorizeError>,
{
    fn from(value: E) -> Self {
        Self {
            reason_input: Some(value.into()),
            decision: false,
            workload: None,
            user: None,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct PrincipalResult {
    pub decision: Option<Decision>,
    pub reason_input: Option<String>,
    pub reason_policy: Option<HashSet<PolicyId>>,
    #[serde(
        serialize_with = "serialize_errors_policy",
        skip_serializing_if = "Vec::is_empty"
    )]
    pub errors_policy: Vec<cedar_policy::AuthorizationError>,
}

impl From<cedar_policy::Response> for PrincipalResult {
    fn from(response: cedar_policy::Response) -> Self {
        let reason_policy = response.diagnostics().reason().cloned().collect();
        let errors_policy = response.diagnostics().errors().cloned().collect();

        Self {
            decision: Some(response.decision()),
            reason_input: None,
            reason_policy: Some(reason_policy),
            errors_policy,
        }
    }
}

fn serialize_reason_input<S>(
    error: &Option<AuthorizeError>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    error.as_ref().map(|e| e.to_string()).serialize(serializer)
}

fn serialize_errors_policy<S>(
    errors: &[cedar_policy::AuthorizationError],
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    let str_errors = errors.iter().map(|e| e.to_string()).collect::<String>();
    str_errors.serialize(serializer)
}

impl AuthorizeResult {
    /// Builder function for AuthorizeResult
    pub(crate) fn new(
        user_workload_operator: WorkloadBoolOp,
        workload: Option<cedar_policy::Response>,
        user: Option<cedar_policy::Response>,
    ) -> Self {
        let decision = calc_decision(user_workload_operator, &workload, &user);
        Self {
            decision,
            reason_input: None,
            user,
            workload,
        }
    }

    /// Decision of result
    /// works based on [`AuthorizeResult::is_allowed`]
    pub fn cedar_decision(&self) -> Decision {
        if self.decision {
            Decision::Allow
        } else {
            Decision::Deny
        }
    }
}

/// Evaluates the authorization result to determine if the request is allowed.  
///  
/// If present only workload result return true if decision is `ALLOW`.
/// If present only person result  return true if decision is `ALLOW`.
/// If person and workload is present will be used operator (AND or OR) based on `CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION` bootstrap property.
/// If none present return false.
fn calc_decision(
    user_workload_operator: WorkloadBoolOp,
    workload: &Option<cedar_policy::Response>,
    person: &Option<cedar_policy::Response>,
) -> bool {
    let workload_allowed = workload
        .as_ref()
        .map(|response| response.decision() == Decision::Allow);

    let person_allowed = person
        .as_ref()
        .map(|response| response.decision() == Decision::Allow);

    // cover each possible case when any of value is Some or None
    match (workload_allowed, person_allowed) {
        (None, None) => false,
        (None, Some(person)) => person,
        (Some(workload), None) => workload,
        (Some(workload), Some(person)) => user_workload_operator.calc(workload, person),
    }
}
