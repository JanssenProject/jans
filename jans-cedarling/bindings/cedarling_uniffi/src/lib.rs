// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
#![cfg(not(target_arch = "wasm32"))]

use cedarling::{
    self as core, BootstrapConfig, BootstrapConfigRaw, DataApi, DataEntry as CoreDataEntry,
    DataStoreStats as CoreDataStoreStats, LogStorage,
};
use std::sync::Arc;
mod result;
use result::{AuthorizeResult, MultiIssuerAuthorizeResult};
use serde_json::Value;
use std::collections::HashMap;
use std::time::Duration;
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

// Enum representing data store errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum DataError {
    #[error("Data Error: {error_msg}")]
    DataOperationFailed { error_msg: String },
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

/// TokenInput for multi-issuer authorization
#[derive(Debug, Clone, uniffi::Record)]
pub struct TokenInput {
    /// Token mapping type (e.g., "Jans::Access_Token", "Acme::DolphinToken")
    pub mapping: String,
    /// JWT token string
    pub payload: String,
}

/// Data entry with value and metadata
#[derive(Debug, Clone, uniffi::Record)]
pub struct DataEntry {
    /// The key for this entry
    pub key: String,
    /// The actual value stored (as JSON string)
    pub value: JsonValue,
    /// The inferred Cedar type of the value
    pub data_type: String,
    /// Timestamp when this entry was created (RFC 3339 format)
    pub created_at: String,
    /// Timestamp when this entry expires (RFC 3339 format), or empty string if no TTL
    pub expires_at: String,
    /// Number of times this entry has been accessed
    pub access_count: u64,
}

/// Statistics about the DataStore
#[derive(Debug, Clone, uniffi::Record)]
pub struct DataStoreStats {
    /// Number of entries currently stored
    pub entry_count: u64,
    /// Maximum number of entries allowed (0 = unlimited)
    pub max_entries: u64,
    /// Maximum size per entry in bytes (0 = unlimited)
    pub max_entry_size: u64,
    /// Whether metrics tracking is enabled
    pub metrics_enabled: bool,
    /// Total size of all entries in bytes (approximate, based on JSON serialization)
    pub total_size_bytes: u64,
    /// Average size per entry in bytes (0 if no entries)
    pub avg_entry_size_bytes: u64,
    /// Percentage of capacity used (0.0-100.0, based on entry count)
    pub capacity_usage_percent: f64,
    /// Memory usage threshold percentage (from config)
    pub memory_alert_threshold: f64,
    /// Whether memory usage exceeds the alert threshold
    pub memory_alert_triggered: bool,
}

impl From<CoreDataEntry> for DataEntry {
    fn from(entry: CoreDataEntry) -> Self {
        Self {
            key: entry.key,
            value: JsonValue(
                serde_json::to_string(&entry.value)
                    .expect("DataEntry value should be serializable to JSON"),
            ),
            data_type: format!("{:?}", entry.data_type).to_lowercase(),
            created_at: entry.created_at.to_rfc3339(),
            expires_at: entry
                .expires_at
                .map(|dt| dt.to_rfc3339())
                .unwrap_or_default(),
            access_count: entry.access_count,
        }
    }
}

impl From<CoreDataStoreStats> for DataStoreStats {
    fn from(stats: CoreDataStoreStats) -> Self {
        Self {
            entry_count: stats.entry_count as u64,
            max_entries: stats.max_entries as u64,
            max_entry_size: stats.max_entry_size as u64,
            metrics_enabled: stats.metrics_enabled,
            total_size_bytes: stats.total_size_bytes as u64,
            avg_entry_size_bytes: stats.avg_entry_size_bytes as u64,
            capacity_usage_percent: stats.capacity_usage_percent,
            memory_alert_threshold: stats.memory_alert_threshold,
            memory_alert_triggered: stats.memory_alert_triggered,
        }
    }
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
            cedar_mapping: core::CedarEntityMapping { entity_type, id },
            attributes: converted_payload,
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

    // Handles multi-issuer authorization request
    #[uniffi::method]
    pub fn authorize_multi_issuer(
        &self,
        tokens: Vec<TokenInput>,
        action: String,
        resource: Arc<EntityData>,
        context: Option<JsonValue>,
    ) -> Result<MultiIssuerAuthorizeResult, AuthorizeError> {
        let core_tokens: Vec<core::TokenInput> = tokens
            .into_iter()
            .map(|t| core::TokenInput {
                mapping: t.mapping,
                payload: t.payload,
            })
            .collect();

        let core_resource = resource.inner.clone();

        let core_context = if let Some(ctx) = context {
            Some(ctx.try_into().map_err(|_| AuthorizeError::InvalidContext)?)
        } else {
            None
        };

        let core_request = core::AuthorizeMultiIssuerRequest {
            tokens: core_tokens,
            resource: core_resource,
            action,
            context: core_context,
        };

        let result = self
            .inner
            .authorize_multi_issuer(core_request)
            .map_err(|e| AuthorizeError::AuthorizationFailed {
                error_msg: e.to_string(),
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

    /// Closes the connections to the Lock Server and pushes all available logs.
    #[uniffi::method]
    pub fn shut_down(&self) {
        self.inner.shut_down();
    }

    /// Push a value into the data store with an optional TTL.
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from configuration is used.
    #[uniffi::method]
    pub fn push_data_ctx(
        &self,
        key: String,
        value: JsonValue,
        ttl_secs: Option<u64>,
    ) -> Result<(), DataError> {
        let json_value: Value = value
            .try_into()
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: format!("Failed to parse JSON value: {}", e),
            })?;

        let ttl = ttl_secs.map(Duration::from_secs);
        self.inner
            .push_data_ctx(&key, json_value, ttl)
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })
    }

    /// Get a value from the data store by key.
    /// Returns None if the key doesn't exist or the entry has expired.
    #[uniffi::method]
    pub fn get_data_ctx(&self, key: String) -> Result<Option<JsonValue>, DataError> {
        match self
            .inner
            .get_data_ctx(&key)
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })? {
            Some(value) => Ok(Some(JsonValue(serde_json::to_string(&value).map_err(
                |e| DataError::DataOperationFailed {
                    error_msg: format!("Failed to serialize value: {}", e),
                },
            )?))),
            None => Ok(None),
        }
    }

    /// Get a data entry with full metadata by key.
    /// Returns None if the key doesn't exist or the entry has expired.
    #[uniffi::method]
    pub fn get_data_entry_ctx(&self, key: String) -> Result<Option<DataEntry>, DataError> {
        match self
            .inner
            .get_data_entry_ctx(&key)
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })? {
            Some(entry) => Ok(Some(entry.into())),
            None => Ok(None),
        }
    }

    /// Remove a value from the data store by key.
    /// Returns true if the key existed and was removed, false otherwise.
    #[uniffi::method]
    pub fn remove_data_ctx(&self, key: String) -> Result<bool, DataError> {
        self.inner
            .remove_data_ctx(&key)
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })
    }

    /// Clear all entries from the data store.
    #[uniffi::method]
    pub fn clear_data_ctx(&self) -> Result<(), DataError> {
        self.inner
            .clear_data_ctx()
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })
    }

    /// List all entries with their metadata.
    /// Returns a list of DataEntry objects.
    #[uniffi::method]
    pub fn list_data_ctx(&self) -> Result<Vec<DataEntry>, DataError> {
        self.inner
            .list_data_ctx()
            .map(|entries| entries.into_iter().map(Into::into).collect())
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })
    }

    /// Get statistics about the data store.
    #[uniffi::method]
    pub fn get_stats_ctx(&self) -> Result<DataStoreStats, DataError> {
        self.inner
            .get_stats_ctx()
            .map(Into::into)
            .map_err(|e| DataError::DataOperationFailed {
                error_msg: e.to_string(),
            })
    }
}
