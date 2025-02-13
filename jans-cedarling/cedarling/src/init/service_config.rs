/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_store::{PolicyStoreLoadError, load_policy_store};
use crate::bootstrap_config;
use crate::common::policy_store::PolicyStoreWithID;
use bootstrap_config::BootstrapConfig;

/// Configuration that hold validated infomation from bootstrap config
#[derive(Clone)]
pub(crate) struct ServiceConfig {
    pub policy_store: PolicyStoreWithID,
    pub lock_client_config: Option<LockClientConfig>,
}

/// Config from `/.well-known/lock-configuration` and info from the
/// IDP's DCR.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct LockClientConfig {
    pub client_id: String,
    pub access_token: String,
    pub audit_uri: String,
    pub sse_uri: String,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] PolicyStoreLoadError),
}

impl ServiceConfig {
    pub async fn new(bootstrap: &BootstrapConfig) -> Result<Self, ServiceConfigError> {
        let (policy_store, lock_client_creds) =
            load_policy_store(&bootstrap.policy_store_config).await?;

        Ok(Self {
            policy_store,
            lock_client_config: lock_client_creds,
        })
    }
}
