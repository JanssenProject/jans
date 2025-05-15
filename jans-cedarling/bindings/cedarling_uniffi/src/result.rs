// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use cedarling::{self as core, bindings::cedar_policy};

#[derive(Debug, uniffi::Record)]
pub struct AuthorizeResult {
    pub workload: Option<Response>,
    pub person: Option<Response>,
    pub principals: HashMap<String, Response>,
    pub decision: bool,
    pub request_id: String,
}

#[derive(Debug, uniffi::Record)]
pub struct Response {
    pub decision: Decision,
    pub diagnostics: Diagnostics,
}

#[derive(Debug, uniffi::Enum)]
pub enum Decision {
    Allow,
    Deny,
}

#[derive(Debug, uniffi::Record)]
pub struct Diagnostics {
    pub reasons: Vec<String>,
    pub errors: Vec<String>,
}

// Conversion implementations
impl From<cedar_policy::Decision> for Decision {
    fn from(decision: cedar_policy::Decision) -> Self {
        match decision {
            cedar_policy::Decision::Allow => Decision::Allow,
            cedar_policy::Decision::Deny => Decision::Deny,
        }
    }
}

impl From<&cedar_policy::Diagnostics> for Diagnostics {
    fn from(diag: &cedar_policy::Diagnostics) -> Self {
        Diagnostics {
            reasons: diag.reason().map(|id| id.to_string()).collect(),
            errors: diag.errors().map(|e| e.to_string()).collect(),
        }
    }
}

impl From<cedar_policy::Response> for Response {
    fn from(response: cedar_policy::Response) -> Self {
        Response {
            decision: response.decision().into(),
            diagnostics: response.diagnostics().into(),
        }
    }
}

impl From<core::AuthorizeResult> for AuthorizeResult {
    fn from(result: core::AuthorizeResult) -> Self {
        AuthorizeResult {
            workload: result.workload.map(Into::into),
            person: result.person.map(Into::into),
            principals: result
                .principals
                .into_iter()
                .map(|(k, v)| (k.into(), v.into()))
                .collect(),
            decision: result.decision,
            request_id: result.request_id,
        }
    }
}
