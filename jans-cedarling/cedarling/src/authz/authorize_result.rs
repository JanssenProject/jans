/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedar_policy::{Decision, EntityUid, Response};
use serde::ser::{SerializeMap, SerializeStruct};
use serde::{Serialize, Serializer};
use smol_str::{SmolStr, ToSmolStr};
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

    /// Result of authorization for all principals.
    /// This field is useful when you want to get the authorization results for unsigned authorization requests.
    /// Because it can be more than workload and person principals.
    #[serde(serialize_with = "serialize_hashmap_response")]
    pub principals: HashMap<SmolStr, cedar_policy::Response>,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,

    /// Request ID, generated per each request call, is used to get logs from memory logger
    pub request_id: String,
}

/// Custom serializer for an Option<cedar_policy::Response> which converts `None` to an empty string and vice versa.
pub fn serialize_opt_response<S>(
    value: &Option<cedar_policy::Response>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    match value {
        None => serializer.serialize_none(),
        Some(response) => CedarResponse(response).serialize(serializer),
    }
}

/// Custom serializer for an Option<cedar_policy::Response> which converts `None` to an empty string and vice versa.
pub fn serialize_hashmap_response<S>(
    value: &HashMap<SmolStr, cedar_policy::Response>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    let mut map = serializer.serialize_map(Some(value.len()))?;
    for (key, value) in value.iter() {
        map.serialize_entry(key, &CedarResponse(value))?;
    }

    map.end()
}

struct CedarResponse<'a>(&'a cedar_policy::Response);

impl Serialize for CedarResponse<'_> {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let response = &self.0;
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
    }
}

impl AuthorizeResult {
    /// Builder function for AuthorizeResult
    pub(crate) fn new(
        principal_bool_operator: &JsonRule,

        workload_uid: Option<EntityUid>,
        person_uid: Option<EntityUid>,

        workload: Option<cedar_policy::Response>,
        person: Option<cedar_policy::Response>,
        request_id: Uuid,
    ) -> Result<Self, ApplyRuleError> {
        let mut principal_responses: HashMap<EntityUid, cedar_policy::Response> = HashMap::new();

        workload_uid
            .clone()
            .into_iter()
            .zip(workload)
            .for_each(|(typename, response)| {
                principal_responses.insert(typename.to_owned(), response.to_owned());
            });

        person_uid
            .clone()
            .into_iter()
            .zip(person)
            .for_each(|(typename, response)| {
                principal_responses.insert(typename.to_owned(), response.to_owned());
            });

        Self::new_for_many_principals(
            principal_bool_operator,
            principal_responses,
            workload_uid,
            person_uid,
            request_id,
        )
    }

    /// Builder function for AuthorizeResult
    pub(crate) fn new_for_many_principals(
        principal_bool_operator: &JsonRule,
        principal_responses: HashMap<EntityUid, cedar_policy::Response>,
        workload_uid: Option<EntityUid>,
        person_uid: Option<EntityUid>,
        request_id: Uuid,
    ) -> Result<Self, ApplyRuleError> {
        let mut principals_decision_info = HashMap::new();
        let mut principals_response: HashMap<SmolStr, cedar_policy::Response> = HashMap::new();

        let mut workload_result: Option<Response> = None;
        let mut person_result: Option<Response> = None;

        for (principal_uid, response) in principal_responses.into_iter() {
            if let Some(uid) = &workload_uid {
                if uid == &principal_uid {
                    workload_result = Some(response.to_owned());
                }
            }
            if let Some(uid) = &person_uid {
                if uid == &principal_uid {
                    person_result = Some(response.to_owned());
                }
            }

            let principal_typename = principal_uid.type_name().to_smolstr();
            let principal_id = principal_uid.to_smolstr();
            // Insert typename and principal id into hashmap.
            // This allows us to easily access the decision for a given principal by type name and and uid.
            principals_decision_info.insert(principal_typename.clone(), response.decision().into());
            principals_decision_info.insert(principal_id.clone(), response.decision().into());

            principals_response.insert(principal_typename.clone(), response.clone());
            principals_response.insert(principal_id, response);
        }

        let decision =
            RuleApplier::new(principal_bool_operator, principals_decision_info).apply()?;

        Ok(Self {
            decision,
            workload: workload_result,
            person: person_result,
            principals: principals_response,
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
