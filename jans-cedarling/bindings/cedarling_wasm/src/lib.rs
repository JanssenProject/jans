// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::bindings::cedar_policy;
use cedarling::{BootstrapConfig, BootstrapConfigRaw, LogStorage, Request};
use serde::ser::{Serialize, SerializeStruct, Serializer};
use serde_json::json;
use serde_wasm_bindgen::Error;
use std::rc::Rc;
use wasm_bindgen::prelude::*;
use wasm_bindgen_futures::js_sys::{Array, Map, Object, Reflect};

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
    /// Assume that config is `Map`
    pub async fn new_from_map(config: Map) -> Result<Cedarling, Error> {
        let conf_js_val = config.unchecked_into();

        let conf_object = Object::from_entries(&conf_js_val)?;
        Self::new(&conf_object).await
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub async fn authorize(&self, request: JsValue) -> Result<AuthorizeResult, Error> {
        // if `request` is map convert to object
        let request_object: JsValue = if request.is_instance_of::<Map>() {
            Object::from_entries(&request)?.into()
        } else {
            request
        };

        let cedar_request: Request = serde_wasm_bindgen::from_value(request_object)?;

        let result = self
            .instance
            .authorize(cedar_request)
            .await
            .map_err(Error::new)?;
        Ok(result.into())
    }

    /// Get logs and remove them from the storage.
    /// Returns `Array` of `Map`
    pub fn pop_logs(&self) -> Result<Array, Error> {
        let result = Array::new();
        for log in self.instance.pop_logs() {
            let js_log = convert_json_to_object(&log)?;
            result.push(&js_log);
        }
        Ok(result)
    }

    /// Get specific log entry.
    /// Returns `Map` with values or `null`.
    pub fn get_log_by_id(&self, id: &str) -> Result<JsValue, Error> {
        let result = if let Some(log_json_value) = self.instance.get_log_by_id(id) {
            convert_json_to_object(&log_json_value)?
        } else {
            JsValue::NULL
        };
        Ok(result)
    }

    /// Returns a list of all log ids.
    /// Returns `Array` of `String`
    pub fn get_log_ids(&self) -> Array {
        let result = Array::new();
        for log_id in self.instance.get_log_ids() {
            let js_id = log_id.into();
            result.push(&js_id);
        }
        result
    }

    /// Get logs by tag, like `log_kind` or `log level`.
    /// Tag can be `log_kind`, `log_level`.
    pub fn get_logs_by_tag(&self, tag: &str) -> Result<Vec<JsValue>, Error> {
        self.instance
            .get_logs_by_tag(tag)
            .iter()
            .map(convert_json_to_object)
            .collect()
    }

    /// Get logs by request_id.
    /// Return log entries that match the given request_id.
    pub fn get_logs_by_request_id(&self, request_id: &str) -> Result<Vec<JsValue>, Error> {
        self.instance
            .get_logs_by_request_id(request_id)
            .iter()
            .map(convert_json_to_object)
            .collect()
    }

    /// Get log by request_id and tag, like composite key `request_id` + `log_kind`.
    /// Tag can be `log_kind`, `log_level`.
    /// Return log entries that match the given request_id and tag.
    pub fn get_logs_by_request_id_and_tag(
        &self,
        request_id: &str,
        tag: &str,
    ) -> Result<Vec<JsValue>, Error> {
        self.instance
            .get_logs_by_request_id_and_tag(request_id, tag)
            .iter()
            .map(convert_json_to_object)
            .collect()
    }
}

/// convert json to js object
fn convert_json_to_object(json_value: &serde_json::Value) -> Result<JsValue, Error> {
    let js_map_value = serde_wasm_bindgen::to_value(json_value)?;
    to_object_recursive(js_map_value)
}

/// recurcive convert [`Map`] to object
fn to_object_recursive(value: JsValue) -> Result<JsValue, Error> {
    if value.is_instance_of::<Map>() {
        // Convert the Map into an Object where keys and values are recursively processed
        let map = Map::unchecked_from_js(value);
        let obj = Object::new();
        for entry in map.entries().into_iter() {
            let entry = Array::unchecked_from_js(entry?);
            let key = entry.get(0);
            let val = to_object_recursive(entry.get(1))?;
            Reflect::set(&obj, &key, &val)?;
        }
        Ok(obj.into())
    } else if value.is_instance_of::<Array>() {
        // Recursively process arrays
        let array = Array::unchecked_from_js(value);
        let serialized_array = Array::new();
        for item in array.iter() {
            serialized_array.push(&to_object_recursive(item)?);
        }
        Ok(serialized_array.into())
    } else if value.is_object() {
        // Recursively process plain objects
        let obj = Object::unchecked_from_js(value);
        let keys = Object::keys(&obj);
        let serialized_obj = Object::new();
        for key in keys.iter() {
            let val = Reflect::get(&obj, &key)?;
            Reflect::set(&serialized_obj, &key, &to_object_recursive(val)?)?;
        }
        Ok(serialized_obj.into())
    } else {
        // Return primitive values as-is
        Ok(value)
    }
}

/// A WASM wrapper for the Rust `cedarling::AuthorizeResult` struct.
/// Represents the result of an authorization request.
#[wasm_bindgen]
#[derive(serde::Serialize)]
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

    /// Request ID of the authorization request
    #[wasm_bindgen(getter_with_clone)]
    pub request_id: String,
}

#[wasm_bindgen]
impl AuthorizeResult {
    /// Convert `AuthorizeResult` to json string value
    pub fn json_string(&self) -> String {
        json!(self).to_string()
    }
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
            request_id: value.request_id,
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

impl Serialize for AuthorizeResultResponse {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let mut state = serializer.serialize_struct("Diagnostics", 2)?;
        state.serialize_field("decision", &self.decision())?;
        state.serialize_field("diagnostics", &self.diagnostics())?;
        state.end()
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

impl Serialize for Diagnostics {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let mut state = serializer.serialize_struct("Diagnostics", 2)?;
        state.serialize_field("reason", &self.reason())?;
        state.serialize_field("errors", &self.errors())?;
        state.end()
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

impl Serialize for PolicyEvaluationError {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let mut state = serializer.serialize_struct("PolicyEvaluationError", 2)?;
        state.serialize_field("id", &self.id())?;
        state.serialize_field("error", &self.error())?;
        state.end()
    }
}
