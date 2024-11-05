/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
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
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    let policy_store = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            load_policy_store_from_json(policy_json).map_err(PolicyStoreLoadError::ParseJson)?
        },
        PolicyStoreSource::Yaml(policy_yaml) => {
            serde_yml::from_str(policy_yaml).map_err(PolicyStoreLoadError::ParseYaml)?
        },
        PolicyStoreSource::LockMaster(policy_store_id) => {
            load_policy_store_from_lock_master(policy_store_id)?
        },
    };

    Ok(policy_store)
}

/// Loads the policy store from a JSON string.
fn load_policy_store_from_json(policies_json: &str) -> Result<PolicyStore, serde_json::Error> {
    let policy_store = match serde_json::from_str::<PolicyStore>(policies_json) {
        Ok(policy_store) => policy_store,
        Err(err) => {
            // try to decode compatible to agama-lab
            let Ok(result_map) =
                serde_json::from_str::<HashMap<String, PolicyStore>>(policies_json)
            else {
                // return previous error to be unit test compatible
                return Err(err);
            };
            if result_map.len() != 1 {
                return Err(serde::de::Error::custom(
                    "currently we support only one policy store",
                ));
            }
            result_map
                .into_values()
                .next()
                .expect("value should be present in the iterator")
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
