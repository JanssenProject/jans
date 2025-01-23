/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::AuthorizeError;
use crate::bootstrap_config::WorkloadBoolOp;
use cedar_policy::{Decision, PolicyId};
use serde::{Serialize, Serializer};
use std::collections::{HashMap, HashSet};

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
#[derive(Debug, Serialize)]
pub struct AuthorizeResult {
    /// Authorization results for each principal
    #[serde(skip_serializing_if = "HashMap::is_empty")]
    pub reason_principals: HashMap<String, PrincipalResult>,

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

impl<E> From<E> for AuthorizeResult
where
    E: Into<AuthorizeError>,
{
    fn from(value: E) -> Self {
        Self {
            reason_principals: HashMap::new(),
            reason_input: Some(value.into()),
            decision: false,
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
        workload_entity_type_name: Option<&str>,
        user_entity_type_name: Option<&str>,
        user_workload_operator: WorkloadBoolOp,
        reason_principals: HashMap<String, PrincipalResult>,
    ) -> Self {
        let decision = calc_decision(
            workload_entity_type_name,
            user_entity_type_name,
            &reason_principals,
            &user_workload_operator,
        );
        Self {
            decision,
            reason_input: None,
            reason_principals,
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
    workload_entity_type_name: Option<&str>,
    user_entity_type_name: Option<&str>,
    reason_principals: &HashMap<String, PrincipalResult>,
    user_workload_operator: &WorkloadBoolOp,
) -> bool {
    let workload_allowed = workload_entity_type_name.as_ref().and_then(|type_name| {
        reason_principals.get(*type_name).and_then(|res| {
            res.decision.map(|decision| match decision {
                Decision::Allow => true,
                Decision::Deny => false,
            })
        })
    });

    let user_allowed = user_entity_type_name.as_ref().and_then(|type_name| {
        reason_principals.get(*type_name).and_then(|res| {
            res.decision.map(|decision| match decision {
                Decision::Allow => true,
                Decision::Deny => false,
            })
        })
    });

    // cover each possible case when any of value is Some or None
    match (workload_allowed, user_allowed) {
        (None, None) => false,
        (None, Some(person)) => person,
        (Some(workload), None) => workload,
        (Some(workload), Some(person)) => user_workload_operator.calc(workload, person),
    }
}
