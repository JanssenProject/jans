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
use uuid7::Uuid;

/// Result of authorization and evaluation cedar policy
/// based on the policy store and an optional principal from the request.
#[derive(Debug, Clone, Serialize)]
pub struct AuthorizeResult {
    /// Cedar authorization response for the request.
    #[serde(serialize_with = "serialize_response")]
    pub response: cedar_policy::Response,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,

    /// Request ID, generated per each request call, is used to get logs from memory logger
    pub request_id: String,
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
            .map(std::string::ToString::to_string)
            .collect::<HashSet<String>>();
        let errors = diagnostics
            .errors()
            .map(std::string::ToString::to_string)
            .collect::<HashSet<String>>();

        let mut state = serializer.serialize_struct("Response", 3)?;
        state.serialize_field("decision", decision)?;
        state.serialize_field("reason", &reason)?;
        state.serialize_field("errors", &errors)?;
        state.end()
    }
}

/// Result of multi-issuer authorization
/// This result structure contains a single Cedar policy evaluation response
#[derive(Debug, Clone, Serialize)]
pub struct MultiIssuerAuthorizeResult {
    /// Result of authorization from Cedar policy evaluation
    #[serde(serialize_with = "serialize_response")]
    pub response: cedar_policy::Response,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,

    /// Request ID, generated per each request call, is used to get logs from memory logger
    pub request_id: String,
}

/// Custom serializer for `cedar_policy::Response`
fn serialize_response<S>(value: &cedar_policy::Response, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    CedarResponse(value).serialize(serializer)
}

impl MultiIssuerAuthorizeResult {
    /// Create a new `MultiIssuerAuthorizeResult`
    pub(crate) fn new(response: cedar_policy::Response, request_id: Uuid) -> Self {
        let decision = response.decision() == Decision::Allow;

        Self {
            response,
            decision,
            request_id: request_id.to_string(),
        }
    }
}

impl AuthorizeResult {
    /// Create a new [`AuthorizeResult`] from a single Cedar response.
    pub(crate) fn new(response: cedar_policy::Response, request_id: Uuid) -> Self {
        let decision = response.decision() == Decision::Allow;
        Self {
            response,
            decision,
            request_id: request_id.to_string(),
        }
    }

    /// Decision of result
    /// works based on [`AuthorizeResult::is_allowed`]
    #[must_use]
    pub fn cedar_decision(&self) -> Decision {
        if self.decision {
            Decision::Allow
        } else {
            Decision::Deny
        }
    }
}
