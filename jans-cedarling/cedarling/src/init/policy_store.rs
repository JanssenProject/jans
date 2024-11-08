/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::AgamaPolicyStore;
use crate::common::policy_store::PolicyStore;

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("failed to parse the policy store from policy_store json: {0}")]
    ParseJson(#[from] serde_json::Error),
    #[error("failed to parse the policy store from policy_store yaml: {0}")]
    ParseYaml(#[from] serde_yml::Error),
    #[error("failed to fetch the policy store from the lock server")]
    FetchFromLockServer,
    #[error("Policy Store does not contain correct structure: {0}")]
    InvalidStore(String),
}

// AgamaPolicyStore contains the structure to accommodate several policies,
// and this code for now assumes that there is only ever one policy store,
// extract the first 'policy_stores' entry.
fn extract_first_policy_store(
    agama_policy_store: &AgamaPolicyStore,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    if agama_policy_store.policy_stores.len() != 1 {
        return Err(PolicyStoreLoadError::InvalidStore(format!(
            "expected exactly one 'policy_stores' entry, but found {:?}",
            agama_policy_store.policy_stores.len()
        )));
    }
    // extract exactly the first policy store in the struct
    let mut policy_stores = agama_policy_store
        .policy_stores
        .values()
        .take(1)
        .collect::<Vec<_>>();
    match policy_stores.pop() {
        Some(policy_store) => Ok(policy_store.clone()),
        None => Err(PolicyStoreLoadError::InvalidStore(
            "error retrieving first policy_stores element".into(),
        )),
    }
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    let policy_store = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            let agama_policy_store = serde_json::from_str::<AgamaPolicyStore>(policy_json)
                .map_err(PolicyStoreLoadError::ParseJson)?;
            extract_first_policy_store(&agama_policy_store)?
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            let agama_policy_store = serde_yml::from_str::<AgamaPolicyStore>(policy_yaml)
                .map_err(PolicyStoreLoadError::ParseYaml)?;
            extract_first_policy_store(&agama_policy_store)?
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
