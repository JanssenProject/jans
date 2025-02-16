// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
#![cfg(not(target_arch = "wasm32"))]

use cedarling::{self as core, BootstrapConfig, BootstrapConfigRaw, LogStorage};
mod request_wrapper;
use crate::request_wrapper::RequestWrapper;
use serde_json::Value;
use std::collections::HashMap;
#[cfg(test)]
mod tests;

uniffi::setup_scaffolding!();

// Enum representing initialization errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum CedarlingError {
    #[error("Initialization Error: {error_msg}")]
    InitializationFailed { error_msg: String },
}

// Enum representing authorization errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum AuthorizeError {
    #[error("Authorization Error: {error_msg}")]
    AuthorizationFailed { error_msg: String },
}

// Enum representing logging errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum LogError {
    #[error("Logging Error: {error_msg}")]
    LoggingFailed { error_msg: String },
}

// Wrapper struct for core Cedarling instance
#[derive(uniffi::Object)]
pub struct Cedarling {
    inner: cedarling::blocking::Cedarling,
}

// Struct to hold authorization result, compatible with iOS serialization
#[derive(Debug, serde::Serialize, uniffi::Record)]
pub struct AuthorizeResult {
    json_workload: String,
    json_person: String,
    decision: bool,
    request_id: String,
}

impl AuthorizeResult {
    // Constructor to create a new AuthorizeResult instance
    pub fn new(json_workload: &Value, json_person: &Value, decision: bool, request_id: String) -> Self {
        let json_workload_string =
            serde_json::to_string(&json_workload).unwrap_or_else(|_| "null".to_string());
        let json_person_string =
            serde_json::to_string(&json_person).unwrap_or_else(|_| "null".to_string());
        Self {
            json_workload: json_workload_string,
            json_person: json_person_string,
            decision,
            request_id
        }
    }
    // Convert workload string back to JSON Value
    pub fn to_workload_json_value(&self) -> Value {
        serde_json::from_str(&self.json_workload).unwrap_or(Value::Null)
    }
    // Convert person string back to JSON Value
    pub fn to_person_json_value(&self) -> Value {
        serde_json::from_str(&self.json_person).unwrap_or(Value::Null)
    }
}

#[uniffi::export]
impl Cedarling {
    // Loads Cedarling instance from a JSON configuration string
    #[uniffi::constructor]
    pub fn load_from_json(config: String) -> Result<Self, CedarlingError> {
        // Parse the JSON string into `BootstrapConfigRaw`
        let config: BootstrapConfigRaw =
            serde_json::from_str(&config).map_err(|e| CedarlingError::InitializationFailed {
                error_msg: e.to_string(),
            })?;

        // Convert to `BootstrapConfig`
        let config = BootstrapConfig::from_raw_config(&config).map_err(|e| {
            CedarlingError::InitializationFailed {
                error_msg: e.to_string(),
            }
        })?;

        // Create a new `Cedarling` instance
        let cedarling = core::blocking::Cedarling::new(&config).map_err(|e| {
            CedarlingError::InitializationFailed {
                error_msg: e.to_string(),
            }
        })?;

        Ok(Self { inner: cedarling })
    }

    // Loads Cedarling instance from a configuration file
    #[uniffi::constructor]
    pub fn load_from_file(path: String) -> Result<Self, CedarlingError> {
        let config: BootstrapConfig =
            cedarling::BootstrapConfig::load_from_file(&path).map_err(|e| {
                CedarlingError::InitializationFailed {
                    error_msg: format!("Failed to read the file: {}", e)
                }
            })?;

        let cedarling = core::blocking::Cedarling::new(&config).map_err(|e| {
            CedarlingError::InitializationFailed {
                error_msg: e.to_string(),
            }
        })?;

        Ok(Self { inner: cedarling })
    }

    // Handles authorization and returns a structured result
    #[uniffi::method]
    pub fn authorize(
        &self,
        tokens: HashMap<String, String>,
        action: String,
        resource_type: String,
        resource_id: String,
        payload: String,
        context: String,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        // Run the async operation within the Tokio runtime
        let request =
            RequestWrapper::new(tokens, action, resource_type, resource_id, payload, context);

        let core_request: core::Request = request
            .map_err(|e: request_wrapper::RequestError| AuthorizeError::AuthorizationFailed {
                error_msg: e.to_string(),
            })?
            .inner;
        let result: cedarling::AuthorizeResult =
            self.inner.authorize(core_request).map_err(|e| {
                AuthorizeError::AuthorizationFailed {
                    error_msg: e.to_string(),
                }
            })?;
        let res_val = serde_json::to_value(result.clone()).unwrap();
        Ok(AuthorizeResult::new(
            res_val.get("workload").unwrap(),
            res_val.get("person").unwrap(),
            result.decision,
            result.request_id
        ))
    }

    // Retrieves logs and serializes them as JSON strings
    #[uniffi::method]
    pub fn pop_logs(&self) -> Result<Vec<String>, LogError> {
        let mut result = Vec::new();

        for log in self.inner.pop_logs() {
            let log_str = serde_json::to_string(&log).map_err(|e| LogError::LoggingFailed {
                error_msg: e.to_string(),
            })?;
            result.push(log_str);
        }
        Ok(result)
    }
    //Get log by id
    #[uniffi::method]
    pub fn get_log_by_id(&self, id: &str) -> Result<String, LogError> {

        if let Some(log_json_value) = self.inner.get_log_by_id(id) {
            serde_json::to_string(&log_json_value)
                .map_err(|e| LogError::LoggingFailed {
                    error_msg: e.to_string(),
                })
        } else {
            Err(LogError::LoggingFailed {
                error_msg: "Log not found".to_string(),
            })
        }
    }
    //Get all log ids
    #[uniffi::method]
    pub fn get_log_ids(&self) -> Vec<String> {
        let log_ids = self.inner.get_log_ids();
        let mut result = Vec::with_capacity(log_ids.len());
        
        for log_id in log_ids {
            result.push(log_id.clone());
        }
        
        result
    }

    /// Get logs by tag, like `log_kind` or `log level`.
    /// Tag can be `log_kind`, `log_level`.
    #[uniffi::method]
    pub fn get_logs_by_tag(&self, tag: &str) -> Result<Vec<String>, LogError> {
        let logs = self.inner.get_logs_by_tag(tag);
        let mut result = Vec::with_capacity(logs.len());
    
        for log in logs {
            let log_str = serde_json::to_string(&log).map_err(|e| LogError::LoggingFailed {
                error_msg: e.to_string(),
            })?;
            result.push(log_str);
        }
    
        Ok(result)
    }

    /// Get logs by request_id.
    /// Return log entries that match the given request_id.
    #[uniffi::method]
    pub fn get_logs_by_request_id(&self, request_id: &str) -> Result<Vec<String>, LogError> {
        let logs = self.inner.get_logs_by_request_id(request_id);
        let mut result = Vec::with_capacity(logs.len());

        for log in logs {
            let log_str = serde_json::to_string(&log).map_err(|e| LogError::LoggingFailed {
                error_msg: e.to_string(),
            })?;
            result.push(log_str);
        }
        Ok(result)
    }

    /// Get log by request_id and tag, like composite key `request_id` + `log_kind`.
    /// Tag can be `log_kind`, `log_level`.
    /// Return log entries that match the given request_id and tag.
    #[uniffi::method]
    pub fn get_logs_by_request_id_and_tag(
        &self,
        request_id: &str,
        tag: &str,
    ) -> Result<Vec<String>, LogError> {
        let logs = self.inner.get_logs_by_request_id_and_tag(request_id, tag);
        let mut result = Vec::with_capacity(logs.len());
    
        for log in logs {
            let log_str = serde_json::to_string(&log).map_err(|e| LogError::LoggingFailed {
                error_msg: e.to_string(),
            })?;
            result.push(log_str);
        }
    
        Ok(result)
    }

}