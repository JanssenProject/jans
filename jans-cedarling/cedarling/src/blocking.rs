/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Blocking client of Cedarling

use crate::{
    AuthorizeError, AuthorizeResult, BootstrapConfig, DataApi, DataEntry, DataError,
    DataStoreStats, InitCedarlingError, LogStorage, MultiIssuerAuthorizeResult, Request,
    RequestUnsigned,
};
use crate::{BootstrapConfigRaw, Cedarling as AsyncCedarling};
use std::sync::Arc;
use std::time::Duration;
use tokio::runtime::Runtime;

/// The blocking instance of the Cedarling application.
/// It is safe to share between threads.
#[derive(Clone)]
pub struct Cedarling {
    runtime: Arc<Runtime>,
    instance: AsyncCedarling,
}

impl Cedarling {
    /// Create a new instance of the Cedarling application.
    /// Initialize instance from enviroment variables and from config.
    /// Configuration structure has lower priority.
    pub fn new_with_env(
        raw_config: Option<BootstrapConfigRaw>,
    ) -> Result<Cedarling, InitCedarlingError> {
        let config = BootstrapConfig::from_raw_config_and_env(raw_config)?;
        Self::new(&config)
    }

    /// Create a new instance of the Cedarling application.
    pub fn new(config: &BootstrapConfig) -> Result<Cedarling, InitCedarlingError> {
        let rt = Runtime::new().map_err(InitCedarlingError::RuntimeInit)?;

        rt.block_on(AsyncCedarling::new(config))
            .map(|async_instance| Cedarling {
                instance: async_instance,
                runtime: Arc::new(rt),
            })
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, Box<AuthorizeError>> {
        self.runtime
            .block_on(self.instance.authorize(request))
            .map_err(Box::new)
    }

    /// Authorize request with unsigned data.
    /// makes authorization decision based on the [`RequestUnverified`]
    pub fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, Box<AuthorizeError>> {
        self.runtime
            .block_on(self.instance.authorize_unsigned(request))
            .map_err(Box::new)
    }

    /// Authorize multi-issuer request.
    /// makes authorization decision based on multiple JWT tokens from different issuers
    pub fn authorize_multi_issuer(
        &self,
        request: crate::authz::request::AuthorizeMultiIssuerRequest,
    ) -> Result<MultiIssuerAuthorizeResult, Box<AuthorizeError>> {
        self.runtime
            .block_on(self.instance.authorize_multi_issuer(request))
            .map_err(Box::new)
    }

    /// Closes the connections to the Lock Server and pushes all available logs.
    pub fn shut_down(&self) {
        self.runtime.block_on(self.instance.shut_down());
    }
}

impl LogStorage for Cedarling {
    fn pop_logs(&self) -> Vec<serde_json::Value> {
        self.instance.pop_logs()
    }

    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value> {
        self.instance.get_log_by_id(id)
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.instance.get_log_ids()
    }

    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value> {
        self.instance.get_logs_by_tag(tag)
    }

    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value> {
        self.instance.get_logs_by_request_id(request_id)
    }

    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> Vec<serde_json::Value> {
        self.instance.get_logs_by_request_id_and_tag(id, tag)
    }
}

impl DataApi for Cedarling {
    fn push_data_ctx(
        &self,
        key: &str,
        value: serde_json::Value,
        ttl: Option<Duration>,
    ) -> Result<(), DataError> {
        self.instance.push_data_ctx(key, value, ttl)
    }

    fn get_data_ctx(&self, key: &str) -> Result<Option<serde_json::Value>, DataError> {
        self.instance.get_data_ctx(key)
    }

    fn get_data_entry_ctx(&self, key: &str) -> Result<Option<DataEntry>, DataError> {
        self.instance.get_data_entry_ctx(key)
    }

    fn remove_data_ctx(&self, key: &str) -> Result<bool, DataError> {
        self.instance.remove_data_ctx(key)
    }

    fn clear_data_ctx(&self) -> Result<(), DataError> {
        self.instance.clear_data_ctx()
    }

    fn list_data_ctx(&self) -> Result<Vec<DataEntry>, DataError> {
        self.instance.list_data_ctx()
    }

    fn get_stats_ctx(&self) -> Result<DataStoreStats, DataError> {
        self.instance.get_stats_ctx()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn create_test_cedarling() -> Cedarling {
        use crate::{
            AuthorizationConfig, DataStoreConfig, EntityBuilderConfig, JwtConfig, LogConfig,
            LogTypeConfig, MemoryLogConfig, PolicyStoreConfig, PolicyStoreSource,
        };
        let config = BootstrapConfig {
            application_name: "test_app".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_items: None,
                    max_item_size: None,
                }),
                log_level: crate::LogLevel::DEBUG,
            },
            policy_store_config: PolicyStoreConfig {
                source: PolicyStoreSource::Yaml(
                    "cedar_version: v4.0.0\npolicy_stores: {}\n".to_string(),
                ),
            },
            jwt_config: JwtConfig::new_without_validation(),
            authorization_config: AuthorizationConfig::default(),
            entity_builder_config: EntityBuilderConfig::default(),
            lock_config: None,
            max_default_entities: None,
            max_base64_size: None,
            data_store_config: DataStoreConfig::default(),
        };
        Cedarling::new(&config).expect("Failed to create Cedarling instance")
    }

    #[test]
    fn test_push_data_ctx() {
        let cedarling = create_test_cedarling();
        let result = cedarling.push_data_ctx("test_key", json!("test_value"), None);
        assert!(result.is_ok(), "push_data_ctx should succeed");
    }

    #[test]
    fn test_push_data_ctx_with_ttl() {
        let cedarling = create_test_cedarling();
        let result = cedarling.push_data_ctx(
            "test_key_ttl",
            json!("value"),
            Some(Duration::from_secs(60)),
        );
        assert!(result.is_ok(), "push_data_ctx with TTL should succeed");
    }

    #[test]
    fn test_get_data_ctx() {
        let cedarling = create_test_cedarling();
        cedarling
            .push_data_ctx("get_key", json!("get_value"), None)
            .unwrap();

        let result = cedarling.get_data_ctx("get_key");
        assert!(result.is_ok(), "get_data_ctx should succeed");
        let value = result.unwrap();
        assert_eq!(
            value,
            Some(json!("get_value")),
            "retrieved value should match"
        );
    }

    #[test]
    fn test_get_data_ctx_nonexistent() {
        let cedarling = create_test_cedarling();
        let result = cedarling.get_data_ctx("nonexistent");
        assert!(result.is_ok(), "get_data_ctx should succeed");
        assert_eq!(
            result.unwrap(),
            None,
            "should return None for nonexistent key"
        );
    }

    #[test]
    fn test_get_data_entry_ctx() {
        let cedarling = create_test_cedarling();
        cedarling
            .push_data_ctx("entry_key", json!({"foo": "bar"}), None)
            .unwrap();

        let result = cedarling.get_data_entry_ctx("entry_key");
        assert!(result.is_ok(), "get_data_entry_ctx should succeed");
        let entry = result.unwrap();
        assert!(entry.is_some(), "entry should exist");
        assert_eq!(entry.unwrap().key, "entry_key");
    }

    #[test]
    fn test_remove_data_ctx() {
        let cedarling = create_test_cedarling();
        cedarling
            .push_data_ctx("remove_key", json!("value"), None)
            .unwrap();

        let result = cedarling.remove_data_ctx("remove_key");
        assert!(result.is_ok(), "remove_data_ctx should succeed");
        assert_eq!(result.unwrap(), true, "should return true for existing key");

        let result2 = cedarling.remove_data_ctx("remove_key");
        assert_eq!(
            result2.unwrap(),
            false,
            "should return false for removed key"
        );
    }

    #[test]
    fn test_clear_data_ctx() {
        let cedarling = create_test_cedarling();
        cedarling
            .push_data_ctx("key1", json!("value1"), None)
            .unwrap();
        cedarling
            .push_data_ctx("key2", json!("value2"), None)
            .unwrap();

        let result = cedarling.clear_data_ctx();
        assert!(result.is_ok(), "clear_data_ctx should succeed");

        let result2 = cedarling.get_data_ctx("key1");
        assert_eq!(result2.unwrap(), None, "key1 should be cleared");
    }

    #[test]
    fn test_list_data_ctx() {
        let cedarling = create_test_cedarling();
        cedarling
            .push_data_ctx("list_key1", json!("value1"), None)
            .unwrap();
        cedarling
            .push_data_ctx("list_key2", json!("value2"), None)
            .unwrap();

        let result = cedarling.list_data_ctx();
        assert!(result.is_ok(), "list_data_ctx should succeed");
        let entries = result.unwrap();
        assert!(entries.len() >= 2, "should have at least 2 entries");
    }

    #[test]
    fn test_get_stats_ctx() {
        let cedarling = create_test_cedarling();
        let result = cedarling.get_stats_ctx();
        assert!(result.is_ok(), "get_stats_ctx should succeed");
        let stats = result.unwrap();
        assert!(
            stats.entry_count == stats.entry_count,
            "entry_count should be valid"
        );
    }

    #[test]
    fn test_push_data_ctx_invalid_key() {
        let cedarling = create_test_cedarling();
        let result = cedarling.push_data_ctx("", json!("value"), None);
        assert!(result.is_err(), "push_data_ctx with empty key should fail");
        assert!(matches!(result.unwrap_err(), DataError::InvalidKey));
    }
}
