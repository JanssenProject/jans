/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{LoadPolicyStoreError, PolicyStore};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("Failed to parse the policy store: {0}")]
    Parsing(#[from] LoadPolicyStoreError),
    #[error("failed to fetch the policy store from the lock server")]
    FetchFromLockServer,
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    let policy_store = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            PolicyStore::load_from_json(policy_json).map_err(PolicyStoreLoadError::Parsing)?
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            PolicyStore::load_from_yaml(policy_yaml).map_err(PolicyStoreLoadError::Parsing)?
        },
        PolicyStoreSource::LockMaster(policy_store_id) => {
            load_policy_store_from_lock_master(policy_store_id)?
        },
    };

    Ok(policy_store)
}

/// Loads the policy store from the Lock Master service.
///
/// TODO: implement this function once integration with the lock
/// service has been established
fn load_policy_store_from_lock_master(
    _policy_store_id: &str,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    todo!()
}
