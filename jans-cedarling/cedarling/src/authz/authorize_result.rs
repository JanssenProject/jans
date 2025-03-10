/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedar_policy::Decision;
use serde::ser::SerializeStruct;
use serde::{Serialize, Serializer};
use smol_str::SmolStr;
use std::collections::{HashMap, HashSet};
use uuid7::Uuid;

use crate::common::json_rules::{ApplyRuleError, JsonRule, RuleApplier};

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
#[derive(Debug, Clone, Serialize)]
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    #[serde(serialize_with = "serialize_opt_response")]
    pub workload: Option<cedar_policy::Response>,
    /// Result of authorization where principal is `Jans::User`
    #[serde(serialize_with = "serialize_opt_response")]
    pub person: Option<cedar_policy::Response>,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,

    /// Request ID, generated per each request call, is used to get logs from memory logger
    pub request_id: String,
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
        principal_bool_operator: &JsonRule,

        workload_typename: Option<SmolStr>,
        person_typename: Option<SmolStr>,

        workload: Option<cedar_policy::Response>,
        person: Option<cedar_policy::Response>,
        request_id: Uuid,
    ) -> Result<Self, ApplyRuleError> {
        let mut principal_info = HashMap::new();

        workload_typename
            .into_iter()
            .zip(workload.iter())
            .for_each(|(typename, response)| {
                principal_info.insert(typename, response.decision().into());
            });

        person_typename
            .into_iter()
            .zip(person.iter())
            .for_each(|(typename, response)| {
                principal_info.insert(typename, response.decision().into());
            });

        let decision = RuleApplier::new(principal_bool_operator, principal_info).apply()?;

        Ok(Self {
            decision,
            workload,
            person,
            request_id: request_id.to_string(),
        })
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
