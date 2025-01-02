// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// #![cfg(target_arch = "wasm32")]

use cedarling::bindings::cedar_policy;
use cedarling::{BootstrapConfig, BootstrapConfigRaw, Request};
use serde_wasm_bindgen::Error;
use std::rc::Rc;
use wasm_bindgen::prelude::*;
use wasm_bindgen_futures::js_sys::{Map, Object};
use wasm_bindgen_test::console_log;

#[cfg(test)]
mod tests;

/// The instance of the Cedarling application.
#[wasm_bindgen]
#[derive(Clone)]
pub struct Cedarling {
    instance: cedarling::Cedarling,
}

/// Create a new instance of the Cedarling application.
/// This function can take as config parameter the eather `Map` other `Object`
#[wasm_bindgen]
pub async fn init(config: JsValue) -> Result<Cedarling, Error> {
    if config.is_instance_of::<Map>() {
        // convert to map
        let config_map: Map = config.unchecked_into();
        Cedarling::new_from_map(config_map).await
    } else if let Some(config_object) = Object::try_from(&config) {
        console_log!("init  as object");
        Cedarling::new(config_object).await
    } else {
        Err(Error::new("config should be Map or Object"))
    }
}

#[wasm_bindgen]
impl Cedarling {
    /// Create a new instance of the Cedarling application.
    /// Assume that config is `Object`
    pub async fn new(config: &Object) -> Result<Cedarling, Error> {
        let config: BootstrapConfigRaw = serde_wasm_bindgen::from_value(config.into())?;

        let config = BootstrapConfig::from_raw_config(&config).map_err(Error::new)?;

        cedarling::Cedarling::new(&config)
            .await
            .map(|instance| Cedarling { instance })
            .map_err(Error::new)
    }

    /// Create a new instance of the Cedarling application.
    /// Assume that config is `Object`
    pub async fn new_from_map(config: Map) -> Result<Cedarling, Error> {
        let conf_js_val = config.unchecked_into();

        let conf_object = Object::from_entries(&conf_js_val)?;
        Self::new(&conf_object).await
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub async fn authorize(&self, request: JsValue) -> Result<AuthorizeResult, Error> {
        let request: Request = serde_wasm_bindgen::from_value(request)?;

        let result = self.instance.authorize(request).await.map_err(Error::new)?;
        Ok(result.into())
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
    // It can be premature optimization, but RC allows avoiding clone actual structure
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
