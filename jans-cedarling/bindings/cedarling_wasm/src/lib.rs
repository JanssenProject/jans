// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
#![cfg(all(
    target_arch = "wasm32",
    target_vendor = "unknown",
    target_os = "unknown"
))]

use cedarling::bindings::cedar_policy;
use cedarling::{BootstrapConfig, BootstrapConfigRaw, Request};
use serde_wasm_bindgen::Error;
use std::rc::Rc;
use wasm_bindgen::prelude::*;

/// The instance of the Cedarling application.
#[wasm_bindgen]
#[derive(Clone)]
pub struct Cedarling {
    instance: cedarling::Cedarling,
}

#[wasm_bindgen]
pub async fn init(config: JsValue) -> Result<Cedarling, Error> {
    console_error_panic_hook::set_once();
    Cedarling::new(config).await
}

#[wasm_bindgen]
impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub async fn new(config: JsValue) -> Result<Cedarling, Error> {
        let config: BootstrapConfigRaw = serde_wasm_bindgen::from_value(config)?;

        let config = BootstrapConfig::from_raw_config(&config).map_err(|err| Error::new(err))?;

        cedarling::Cedarling::new(&config)
            .await
            .map(|instance| Cedarling { instance })
            .map_err(|err| Error::new(err))
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    //
    // TODO: return typed value
    pub async fn authorize(&self, request: JsValue) -> Result<AuthorizeResult, Error> {
        let request: Request = serde_wasm_bindgen::from_value(request)?;

        let result = self
            .instance
            .authorize(request)
            .await
            .map_err(|err| Error::new(err))?;
        Ok(result.into())
        // let json_result = serde_json::json!(result);
        // serde_wasm_bindgen::to_value(&json_result)
    }
}

/// A WASM wrapper for the Rust `cedarling::AuthorizeResult` struct.
/// Represents the result of an authorization request.
#[wasm_bindgen]
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    #[wasm_bindgen(getter_with_clone)]
    pub workload: Option<AuthorizeResultResponse>,
    /// Result of authorization where principal is `Jans::User`
    #[wasm_bindgen(getter_with_clone)]
    pub person: Option<AuthorizeResultResponse>,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    ///
    /// this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
    pub decision: bool,
}

impl From<cedarling::AuthorizeResult> for AuthorizeResult {
    fn from(value: cedarling::AuthorizeResult) -> Self {
        Self {
            workload: value
                .workload
                .map(|v| AuthorizeResultResponse { inner: Rc::new(v) }),
            person: value
                .person
                .map(|v| AuthorizeResultResponse { inner: Rc::new(v) }),
            decision: value.decision,
        }
    }
}

/// A WASM wrapper for the Rust `cedar_policy::Response` struct.
/// Represents the result of an authorization request.
#[wasm_bindgen]
#[derive(Clone)]
pub struct AuthorizeResultResponse {
    inner: Rc<cedar_policy::Response>,
}

#[wasm_bindgen]
impl AuthorizeResultResponse {
    /// Authorization decision
    #[wasm_bindgen(getter)]
    pub fn decision(&self) -> bool {
        self.inner.decision() == cedar_policy::Decision::Allow
    }

    /// Diagnostics providing more information on how this decision was reached
    #[wasm_bindgen(getter)]
    pub fn diagnostics(&self) -> Diagnostics {
        Diagnostics {
            inner: self.inner.diagnostics().clone(),
        }
    }
}

/// Diagnostics
/// ===========
///
/// Provides detailed information about how a policy decision was made, including policies that contributed to the decision and any errors encountered during evaluation.
#[wasm_bindgen]
pub struct Diagnostics {
    inner: cedar_policy::Diagnostics,
}

#[wasm_bindgen]
impl Diagnostics {
    /// `PolicyId`s of the policies that contributed to the decision.
    /// If no policies applied to the request, this set will be empty.
    ///
    /// The ids should be treated as unordered,
    #[wasm_bindgen(getter)]
    pub fn reason(&self) -> Vec<String> {
        self.inner.reason().map(|v| v.to_string()).collect()
    }

    /// Errors that occurred during authorization. The errors should be
    /// treated as unordered, since policies may be evaluated in any order.
    #[wasm_bindgen(getter)]
    pub fn errors(&self) -> Vec<PolicyEvaluationError> {
        self.inner
            .errors()
            .map(|err| {
                let mapped_error: cedarling::bindings::PolicyEvaluationError = err.into();
                PolicyEvaluationError {
                    inner: mapped_error,
                }
            })
            .collect()
    }
}

/// PolicyEvaluationError
/// =====================
///
/// Represents an error that occurred when evaluating a Cedar policy.
#[wasm_bindgen]
pub struct PolicyEvaluationError {
    inner: cedarling::bindings::PolicyEvaluationError,
}

#[wasm_bindgen]
impl PolicyEvaluationError {
    /// Id of the policy with an error
    #[wasm_bindgen(getter)]
    pub fn id(&self) -> String {
        self.inner.id.clone()
    }

    /// Underlying evaluation error string representation
    #[wasm_bindgen(getter)]
    pub fn error(&self) -> String {
        self.inner.error.clone()
    }
}
