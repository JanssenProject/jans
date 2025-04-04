// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
#![cfg(not(target_arch = "wasm32"))]

use cedarling::{self as core, BootstrapConfig, BootstrapConfigRaw, LogStorage};
use std::sync::Arc;
mod result;
use result::*;
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
    #[error("Invalid context json data provided")]
    InvalidContext,
}

// Enum representing logging errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum LogError {
    #[error("Logging Error: {error_msg}")]
    LoggingFailed { error_msg: String },
}

#[derive(Debug, Clone, uniffi::Object)]
pub struct EntityData {
    inner: core::EntityData,
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum EntityError {
    #[error("JSON conversion failed: {0}")]
    JsonConversion(String),
    #[error("JSON conversion failed for payload key: {0}, value: {1}")]
    JsonConversionPayload(String, String),
}

#[uniffi::export]
impl EntityData {
    // A custom method that returns a Result for cases where you want to handle errors.
    #[uniffi::constructor]
    pub fn new(
        entity_type: String,
        id: String,
        payload: HashMap<String, JsonValue>,
    ) -> Result<Self, EntityError> {
        let mut converted_payload = HashMap::new();
        for (k, v) in payload {
            let json_value = Value::try_from(v)
                .map_err(|err| EntityError::JsonConversionPayload(k.clone(), err.to_string()))?;

            converted_payload.insert(k, json_value);
        }
        let inner = core::EntityData {
            entity_type,
            id,
            payload: converted_payload,
        };
        Ok(Self { inner })
    }

    #[uniffi::constructor]
    pub fn from_json(json_string: String) -> Result<Self, EntityError> {
        let parsed: core::EntityData = serde_json::from_str(&json_string)
            .map_err(|err| EntityError::JsonConversion(err.to_string()))?;

        Ok(Self { inner: parsed })
    }
}

/// Wrapper struct for JSON values, holding a string representation of the JSON value.
#[derive(Debug, Clone)]
pub struct JsonValue(String);

uniffi::custom_newtype!(JsonValue, String);

#[uniffi::export]
impl JsonValue {
    #[uniffi::constructor]
    pub fn new(value: String) -> Self {
        Self(value)
    }
}

impl TryFrom<JsonValue> for Value {
    type Error = serde_json::Error;
    fn try_from(value: JsonValue) -> Result<Self, Self::Error> {
        serde_json::from_str(&value.0)
    }
}

// Wrapper struct for core Cedarling instance
#[derive(uniffi::Object)]
pub struct Cedarling {
    inner: cedarling::blocking::Cedarling,
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
                    error_msg: format!("Failed to read the file: {}", e),
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
        resource: Arc<EntityData>,
        context: JsonValue,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        let core_resource = resource.inner.clone();

        let core_request = core::Request {
            tokens,
            action,
            resource: core_resource,
            context: context
                .try_into()
                .map_err(|_| AuthorizeError::InvalidContext)?,
        };

        let result: cedarling::AuthorizeResult =
            self.inner.authorize(core_request).map_err(|e| {
                AuthorizeError::AuthorizationFailed {
                    error_msg: e.to_string(),
                }
            })?;
        Ok(result.into())
    }

    // Handles authorization for unsigned requests
    #[uniffi::method]
    pub fn authorize_unsigned(
        &self,
        principals: Vec<Arc<EntityData>>,
        action: String,
        resource: Arc<EntityData>,
        context: JsonValue,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        let core_principals: Vec<core::EntityData> =
            principals.into_iter().map(|v| v.inner.clone()).collect();

        let core_resource = resource.inner.clone();

        let core_request = core::RequestUnsigned {
            principals: core_principals,
            action,
            resource: core_resource,
            context: context
                .try_into()
                .map_err(|_| AuthorizeError::InvalidContext)?,
        };

        let result = self.inner.authorize_unsigned(core_request).map_err(|e| {
            AuthorizeError::AuthorizationFailed {
                error_msg: e.to_string(),
            }
        })?;
        Ok(result.into())
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
            serde_json::to_string(&log_json_value).map_err(|e| LogError::LoggingFailed {
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
