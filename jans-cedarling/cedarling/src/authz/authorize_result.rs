/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedar_policy::Decision;
use serde::ser::SerializeStruct;
use serde::{Serialize, Serializer};
use std::collections::HashSet;

use crate::bootstrap_config::WorkloadBoolOp;

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
#[derive(Debug, Clone, Serialize)]
pub struct AuthorizeResult {
    user_workload_operator: WorkloadBoolOp,
    /// Result of authorization where principal is `Jans::Workload`
    #[serde(serialize_with = "serialize_opt_response")]
    pub workload: Option<cedar_policy::Response>,
    /// Result of authorization where principal is `Jans::User`
    #[serde(serialize_with = "serialize_opt_response")]
    pub person: Option<cedar_policy::Response>,
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

impl AuthorizeResult {
    /// Builder function for AuthorizeResult
    pub(crate) fn new(
        user_workload_operator: WorkloadBoolOp,
        workload: Option<cedar_policy::Response>,
        person: Option<cedar_policy::Response>,
    ) -> Self {
        Self {
            user_workload_operator,
            workload,
            person,
        }
    }

    /// Evaluates the authorization result to determine if the request is allowed.  
    ///  
    /// This function checks the decision based on the following rule:  
    /// - The `workload` must allow the request (PRINCIPAL).  
    /// - Either the `person` or `role` must also allow the request (USER OR ROLE).  
    ///  
    /// This approach represents a hierarchical decision-making model, where the  
    /// `workload` (i.e., primary principal) needs to permit the request and
    /// additional conditions (either `person` or `role`) must also indicate allowance.
    pub fn is_allowed(&self) -> bool {
        let workload_allowed = self
            .workload
            .as_ref()
            .map(|response| response.decision() == Decision::Allow);

        let person_allowed = self
            .person
            .as_ref()
            .map(|response| response.decision() == Decision::Allow);

        // cover each possible case when any of value is Some or None
        match (workload_allowed, person_allowed) {
            (None, None) => false,
            (None, Some(person)) => person,
            (Some(workload), None) => workload,
            (Some(workload), Some(person)) => self.user_workload_operator.calc(workload, person),
        }
    }
}
