/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Blocking client of Cedarling

use crate::{
    AuthorizeError, AuthorizeResult, BootstrapConfig, DataApi, DataEntry, DataError,
    DataStoreStats, EntityData, InitCedarlingError, LogStorage, MultiIssuerAuthorizeResult,
    PolicyId, PolicyMetadata, RequestUnsigned, TokenInput, TrustedIssuerLoadingInfo,
};
use crate::{BootstrapConfigRaw, Cedarling as AsyncCedarling};
use std::collections::HashMap;
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

    /// Authorize request with unsigned data.
    /// makes authorization decision based on the [`RequestUnverified`]
    #[allow(clippy::needless_pass_by_value)] // to respect the ownership of the request in the async version
    pub fn authorize_unsigned(
        &self,
        request: RequestUnsigned,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        self.instance.authz.load().authorize_unsigned(&request)
    }

    /// Authorize multi-issuer request.
    /// makes authorization decision based on multiple JWT tokens from different issuers
    #[allow(clippy::needless_pass_by_value)] // to respect the ownership of the request in the async version
    pub fn authorize_multi_issuer(
        &self,
        request: crate::authz::request::AuthorizeMultiIssuerRequest,
    ) -> Result<MultiIssuerAuthorizeResult, AuthorizeError> {
        self.instance.authz.load().authorize_multi_issuer(&request)
    }

    /// Returns metadata for all policies whose scope constraints are compatible
    /// with the given principals, actions, and resources.
    pub fn get_matching_policies_unsigned(
        &self,
        principal: Option<&EntityData>,
        actions: &[String],
        resources: &[EntityData],
    ) -> Result<Vec<PolicyMetadata>, AuthorizeError> {
        self.instance
            .authz
            .load()
            .get_matching_policies_unsigned(principal, actions, resources)
    }

    /// Returns metadata for all policies whose scope constraints are compatible
    /// with the given token-derived principals, actions, and resources.
    pub fn get_matching_policies_multi_issuer(
        &self,
        tokens: &[TokenInput],
        actions: &[String],
        resources: &[EntityData],
    ) -> Result<Vec<PolicyMetadata>, AuthorizeError> {
        self.instance
            .authz
            .load()
            .get_matching_policies_multi_issuer(tokens, actions, resources)
    }

    /// Merge the annotations (`@key("value")`) of the given policies into a single map.
    ///
    /// Lossy on duplicate keys across policies; see [`AsyncCedarling::annotations_map`]
    /// for details and the policy-store refresh caveat.
    pub fn annotations_map<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
    ) -> HashMap<String, String> {
        self.instance.authz.load().annotations_map(ids)
    }

    /// Collect every value of the annotation `key` across the given policies,
    /// preserving duplicates; see [`AsyncCedarling::annotation_values`].
    pub fn annotation_values<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
        key: &str,
    ) -> Vec<String> {
        self.instance.authz.load().annotation_values(ids, key)
    }

    /// Return the annotations of each given policy, grouped by policy ID;
    /// see [`AsyncCedarling::annotations_by_policy`].
    pub fn annotations_by_policy<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
    ) -> HashMap<String, HashMap<String, String>> {
        self.instance.authz.load().annotations_by_policy(ids)
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

impl TrustedIssuerLoadingInfo for Cedarling {
    fn is_trusted_issuer_loaded_by_name(&self, issuer_id: &str) -> bool {
        self.instance.is_trusted_issuer_loaded_by_name(issuer_id)
    }

    fn is_trusted_issuer_loaded_by_iss(&self, iss_claim: &str) -> bool {
        self.instance.is_trusted_issuer_loaded_by_iss(iss_claim)
    }

    fn total_issuers(&self) -> usize {
        self.instance.total_issuers()
    }

    fn loaded_trusted_issuers_count(&self) -> usize {
        self.instance.loaded_trusted_issuers_count()
    }

    fn loaded_trusted_issuer_ids(&self) -> std::collections::HashSet<String> {
        self.instance.loaded_trusted_issuer_ids()
    }

    fn failed_trusted_issuer_ids(&self) -> std::collections::HashSet<String> {
        self.instance.failed_trusted_issuer_ids()
    }
}
