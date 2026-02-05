// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::bindings::cedar_policy;
use cedarling::{
    AuthorizeMultiIssuerRequest, BootstrapConfig, BootstrapConfigRaw, DataApi,
    DataEntry as CedarDataEntry, DataStoreStats as CedarDataStoreStats, LogStorage, Request,
    RequestUnsigned,
};
use serde::ser::{Serialize, SerializeStruct, Serializer};
use serde_json::json;
use serde_wasm_bindgen::Error;
use std::collections::HashMap;
use std::rc::Rc;
use std::time::Duration;
use wasm_bindgen::prelude::*;
use wasm_bindgen_futures::js_sys::{self, Array, Map, Object, Reflect};

#[cfg(test)]
mod tests;

/// The instance of the Cedarling application.
#[wasm_bindgen]
#[derive(Clone)]
pub struct Cedarling {
    instance: cedarling::Cedarling,
}

/// A WASM wrapper for the Rust `cedarling::MultiIssuerAuthorizeResult` struct.
/// Represents the result of a multi-issuer authorization request.
#[wasm_bindgen]
#[derive(serde::Serialize)]
pub struct MultiIssuerAuthorizeResult {
    /// Result of Cedar policy authorization
    #[wasm_bindgen(getter_with_clone)]
    pub response: AuthorizeResultResponse,

    /// Result of authorization
    /// true means `ALLOW`
    /// false means `Deny`
    pub decision: bool,

    /// Request ID of the authorization request
    #[wasm_bindgen(getter_with_clone)]
    pub request_id: String,
}

#[wasm_bindgen]
impl MultiIssuerAuthorizeResult {
    /// Convert `MultiIssuerAuthorizeResult` to json string value
    pub fn json_string(&self) -> String {
        json!(self).to_string()
    }
}

impl From<cedarling::MultiIssuerAuthorizeResult> for MultiIssuerAuthorizeResult {
    fn from(value: cedarling::MultiIssuerAuthorizeResult) -> Self {
        Self {
            response: AuthorizeResultResponse {
                inner: Rc::new(value.response),
            },
            decision: value.decision,
            request_id: value.request_id,
        }
    }
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

/// Create a new instance of the Cedarling application from archive bytes.
///
/// This function allows loading a policy store from a Cedar Archive (.cjar)
/// that was fetched with custom logic (e.g., with authentication headers).
///
/// # Arguments
/// * `config` - Bootstrap configuration (Map or Object). Policy store config is ignored.
/// * `archive_bytes` - The .cjar archive bytes (Uint8Array)
///
/// # Example
/// ```javascript
/// const response = await fetch(url, { headers: { Authorization: 'Bearer ...' } });
/// const bytes = new Uint8Array(await response.arrayBuffer());
/// const cedarling = await init_from_archive_bytes(config, bytes);
/// ```
#[wasm_bindgen]
pub async fn init_from_archive_bytes(
    config: JsValue,
    archive_bytes: js_sys::Uint8Array,
) -> Result<Cedarling, Error> {
    use cedarling::PolicyStoreSource;

    // Convert Uint8Array to Vec<u8>
    let bytes: Vec<u8> = archive_bytes.to_vec();

    // Parse the config
    let config_object = if config.is_instance_of::<Map>() {
        let config_map: Map = config.unchecked_into();
        Object::from_entries(&config_map.unchecked_into())?
    } else if let Some(obj) = Object::try_from(&config) {
        obj.clone()
    } else {
        return Err(Error::new("config should be Map or Object"));
    };

    let mut raw_config: BootstrapConfigRaw = serde_wasm_bindgen::from_value(config_object.into())?;

    // Clear any existing policy store sources to avoid conflicts
    // We'll set a dummy source temporarily to satisfy validation, then override with ArchiveBytes
    raw_config.local_policy_store = None;
    raw_config.policy_store_uri = None;
    // Set a dummy .cjar file path to satisfy validation (will be overridden below)
    raw_config.policy_store_local_fn = Some("dummy.cjar".to_string());

    let mut bootstrap_config = BootstrapConfig::from_raw_config(&raw_config).map_err(Error::new)?;

    // Override the policy store source with the archive bytes
    bootstrap_config.policy_store_config.source = PolicyStoreSource::ArchiveBytes(bytes);

    cedarling::Cedarling::new(&bootstrap_config)
        .await
        .map(|instance| Cedarling { instance })
        .map_err(Error::new)
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

    /// Authorize request for unsigned principals.
    /// makes authorization decision based on the [`RequestUnsigned`]
    pub async fn authorize_unsigned(&self, request: JsValue) -> Result<AuthorizeResult, Error> {
        // if `request` is map convert to object
        let request_object: JsValue = if request.is_instance_of::<Map>() {
            Object::from_entries(&request)?.into()
        } else {
            request
        };
        let cedar_request: RequestUnsigned = serde_wasm_bindgen::from_value(request_object)?;
        let result = self
            .instance
            .authorize_unsigned(cedar_request)
            .await
            .map_err(Error::new)?;
        Ok(result.into())
    }

    /// Authorize multi-issuer request.
    /// Makes authorization decision based on multiple JWT tokens from different issuers
    pub async fn authorize_multi_issuer(
        &self,
        request: JsValue,
    ) -> Result<MultiIssuerAuthorizeResult, Error> {
        // if `request` is map convert to object
        let request_object: JsValue = if request.is_instance_of::<Map>() {
            Object::from_entries(&request)?.into()
        } else {
            request
        };
        let cedar_request: AuthorizeMultiIssuerRequest =
            serde_wasm_bindgen::from_value(request_object)?;
        let result = self
            .instance
            .authorize_multi_issuer(cedar_request)
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

    /// Closes the connections to the Lock Server and pushes all available logs.
    pub async fn shut_down(&self) {
        self.instance.shut_down().await;
    }

    /// Push a value into the data store with an optional TTL.
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from configuration is used.
    ///
    /// # Arguments
    ///
    /// * `key` - A string key for the data entry (must not be empty)
    /// * `value` - The value to store (any JSON-serializable JavaScript value: object, array, string, number, boolean)
    /// * `ttl_secs` - Optional TTL in seconds (undefined/null uses default from config)
    ///
    /// # Example
    ///
    /// ```javascript
    /// await cedarling.push_data("user:123", { name: "John", age: 30 }, 3600);
    /// await cedarling.push_data("config", { setting: "value" }); // Uses default TTL
    /// ```
    pub fn push_data(&self, key: &str, value: JsValue, ttl_secs: Option<u64>) -> Result<(), Error> {
        let json_value: serde_json::Value = serde_wasm_bindgen::from_value(value)?;
        let ttl = ttl_secs.map(Duration::from_secs);
        self.instance
            .push_data(key, json_value, ttl)
            .map_err(Error::new)
    }

    /// Get a value from the data store by key.
    /// Returns null if the key doesn't exist or the entry has expired.
    ///
    /// # Arguments
    ///
    /// * `key` - A string key for the data entry to retrieve
    ///
    /// # Example
    ///
    /// ```javascript
    /// const value = await cedarling.get_data("user:123");
    /// if (value !== null) {
    ///     console.log(value.name); // "John"
    /// }
    /// ```
    pub fn get_data(&self, key: &str) -> Result<JsValue, Error> {
        match self.instance.get_data(key).map_err(Error::new)? {
            Some(value) => {
                let js_value = serde_wasm_bindgen::to_value(&value)?;
                Ok(to_object_recursive(js_value)?)
            },
            None => Ok(JsValue::NULL),
        }
    }

    /// Get a data entry with full metadata by key.
    /// Returns null if the key doesn't exist or the entry has expired.
    ///
    /// # Arguments
    ///
    /// * `key` - A string key for the data entry to retrieve
    ///
    /// # Example
    ///
    /// ```javascript
    /// const entry = await cedarling.get_data_entry("user:123");
    /// if (entry !== null) {
    ///     console.log(entry.key); // "user:123"
    ///     console.log(entry.value); // { name: "John", age: 30 }
    ///     console.log(entry.data_type); // "Record"
    ///     console.log(entry.created_at); // "2024-01-01T12:00:00Z"
    ///     console.log(entry.access_count); // 5
    /// }
    /// ```
    pub fn get_data_entry(&self, key: &str) -> Result<JsValue, Error> {
        match self.instance.get_data_entry(key).map_err(Error::new)? {
            Some(entry) => {
                let wasm_entry = DataEntry::from(entry);
                let js_value = serde_wasm_bindgen::to_value(&wasm_entry)?;
                Ok(to_object_recursive(js_value)?)
            },
            None => Ok(JsValue::NULL),
        }
    }

    /// Remove a value from the data store by key.
    /// Returns true if the key existed and was removed, false otherwise.
    ///
    /// # Arguments
    ///
    /// * `key` - A string key for the data entry to remove
    ///
    /// # Example
    ///
    /// ```javascript
    /// const removed = await cedarling.remove_data("user:123");
    /// if (removed) {
    ///     console.log("Entry was successfully removed");
    /// }
    /// ```
    pub fn remove_data(&self, key: &str) -> Result<bool, Error> {
        self.instance.remove_data(key).map_err(Error::new)
    }

    /// Clear all entries from the data store.
    ///
    /// # Example
    ///
    /// ```javascript
    /// await cedarling.clear_data();
    /// console.log("All data entries cleared");
    /// ```
    pub fn clear_data(&self) -> Result<(), Error> {
        self.instance.clear_data().map_err(Error::new)
    }

    /// List all entries with their metadata.
    /// Returns an array of DataEntry objects.
    ///
    /// # Example
    ///
    /// ```javascript
    /// const entries = await cedarling.list_data();
    /// entries.forEach(entry => {
    ///     console.log(`${entry.key}: ${entry.data_type} (accessed ${entry.access_count} times)`);
    /// });
    /// ```
    pub fn list_data(&self) -> Result<Array, Error> {
        let entries = self.instance.list_data().map_err(Error::new)?;
        let result = Array::new();
        for entry in entries {
            let wasm_entry = DataEntry::from(entry);
            let js_value = serde_wasm_bindgen::to_value(&wasm_entry)?;
            result.push(&to_object_recursive(js_value)?);
        }
        Ok(result)
    }

    /// Get statistics about the data store.
    ///
    /// # Example
    ///
    /// ```javascript
    /// const stats = await cedarling.get_stats();
    /// console.log(`Entries: ${stats.entry_count}/${stats.max_entries || 'unlimited'}`);
    /// console.log(`Capacity: ${stats.capacity_usage_percent.toFixed(2)}%`);
    /// console.log(`Total size: ${stats.total_size_bytes} bytes`);
    /// ```
    pub fn get_stats(&self) -> Result<DataStoreStats, Error> {
        self.instance
            .get_stats()
            .map(|stats| stats.into())
            .map_err(Error::new)
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

    #[wasm_bindgen(skip)]
    pub principals: HashMap<String, AuthorizeResultResponse>,

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

    pub fn principal(&self, principal: &str) -> Option<AuthorizeResultResponse> {
        self.principals.get(principal).cloned()
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
            principals: value
                .principals
                .into_iter()
                .map(|(k, v)| (k.to_string(), AuthorizeResultResponse { inner: Rc::new(v) }))
                .collect(),
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

/// A WASM wrapper for the Rust `cedarling::DataEntry` struct.
/// Represents a data entry in the DataStore with value and metadata.
#[wasm_bindgen]
#[derive(serde::Serialize)]
pub struct DataEntry {
    /// The key for this entry
    #[wasm_bindgen(getter_with_clone)]
    pub key: String,
    /// The actual value stored (as JSON)
    #[wasm_bindgen(skip)]
    pub value: serde_json::Value,
    /// The inferred Cedar type of the value
    #[wasm_bindgen(getter_with_clone)]
    pub data_type: String,
    /// Timestamp when this entry was created (RFC 3339 format)
    #[wasm_bindgen(getter_with_clone)]
    pub created_at: String,
    /// Timestamp when this entry expires (RFC 3339 format), or null if no TTL
    #[wasm_bindgen(getter_with_clone)]
    pub expires_at: Option<String>,
    /// Number of times this entry has been accessed
    pub access_count: u64,
}

#[wasm_bindgen]
impl DataEntry {
    /// Get the value stored in this entry as a JavaScript object
    pub fn value(&self) -> Result<JsValue, Error> {
        let js_value = serde_wasm_bindgen::to_value(&self.value)?;
        to_object_recursive(js_value)
    }

    /// Convert `DataEntry` to json string value
    pub fn json_string(&self) -> String {
        json!(self).to_string()
    }
}

impl From<CedarDataEntry> for DataEntry {
    fn from(value: CedarDataEntry) -> Self {
        Self {
            key: value.key,
            value: value.value,
            data_type: format!("{:?}", value.data_type).to_lowercase(),
            created_at: value.created_at.to_rfc3339(),
            expires_at: value.expires_at.map(|dt| dt.to_rfc3339()),
            access_count: value.access_count,
        }
    }
}

/// A WASM wrapper for the Rust `cedarling::DataStoreStats` struct.
/// Statistics about the DataStore.
#[wasm_bindgen]
#[derive(serde::Serialize)]
pub struct DataStoreStats {
    /// Number of entries currently stored
    pub entry_count: usize,
    /// Maximum number of entries allowed (0 = unlimited)
    pub max_entries: usize,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: usize,
    /// Whether metrics tracking is enabled
    pub metrics_enabled: bool,
    /// Total size of all entries in bytes (approximate, based on JSON serialization)
    pub total_size_bytes: usize,
    /// Average size per entry in bytes (0 if no entries)
    pub avg_entry_size_bytes: usize,
    /// Percentage of capacity used (0.0-100.0, based on entry count)
    pub capacity_usage_percent: f64,
    /// Memory usage threshold percentage (from config)
    pub memory_alert_threshold: f64,
    /// Whether memory usage exceeds the alert threshold
    pub memory_alert_triggered: bool,
}

#[wasm_bindgen]
impl DataStoreStats {
    /// Convert `DataStoreStats` to json string value
    pub fn json_string(&self) -> String {
        json!(self).to_string()
    }
}

impl From<CedarDataStoreStats> for DataStoreStats {
    fn from(value: CedarDataStoreStats) -> Self {
        Self {
            entry_count: value.entry_count,
            max_entries: value.max_entries,
            max_entry_size: value.max_entry_size,
            metrics_enabled: value.metrics_enabled,
            total_size_bytes: value.total_size_bytes,
            avg_entry_size_bytes: value.avg_entry_size_bytes,
            capacity_usage_percent: value.capacity_usage_percent,
            memory_alert_threshold: value.memory_alert_threshold,
            memory_alert_triggered: value.memory_alert_triggered,
        }
    }
}
