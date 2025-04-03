/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Blocking client of Cedarling

use crate::{
    AuthorizeError, AuthorizeResult, BootstrapConfig, InitCedarlingError, LogStorage, Request,
    RequestUnsigned,
};
use crate::{BootstrapConfigRaw, Cedarling as AsyncCedarling};
use std::sync::Arc;
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
    pub fn authorize(&self, request: Request) -> Result<AuthorizeResult, AuthorizeError> {
        self.runtime.block_on(self.instance.authorize(request))
    }

    /// Authorize request with unsigned data.
    /// makes authorization decision based on the [`RequestUnverified`]
    pub fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        self.runtime
            .block_on(self.instance.authorize_unsigned(request))
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
