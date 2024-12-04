/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_store::{load_policy_store, PolicyStoreLoadError};
use crate::bootstrap_config;
use crate::common::policy_store::PolicyStore;
use bootstrap_config::BootstrapConfig;

/// Configuration that hold validated infomation from bootstrap config
#[derive(typed_builder::TypedBuilder, Clone)]
pub(crate) struct ServiceConfig {
    pub policy_store: PolicyStore,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] PolicyStoreLoadError),
}

impl ServiceConfig {
    pub fn new(bootstrap: &BootstrapConfig) -> Result<Self, ServiceConfigError> {
        let policy_store = load_policy_store(&bootstrap.policy_store_config)?;

        let builder = ServiceConfig::builder().policy_store(policy_store);

        Ok(builder.build())
    }
}
