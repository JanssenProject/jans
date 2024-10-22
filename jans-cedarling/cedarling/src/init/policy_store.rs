/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{PolicyStore, PolicyStoreMap};

/// Error cases for loading policy
#[derive(Debug, thiserror::Error)]
pub enum LoadPolicyStoreError {
    #[error("{0}")]
    Parse(#[from] serde_json::Error),
    #[error("store policy is empty")]
    PolicyEmpty,
    #[error("the `store_key` is not specified and the count on policies more than 1")]
    MoreThanOnePolicy,
    #[error("could not found policy by id: {0}")]
    FindPolicy(String),
}

/// Load policy store from source
fn load_policy_store_map(
    source: &PolicyStoreSource,
) -> Result<PolicyStoreMap, LoadPolicyStoreError> {
    let policy_store_map: PolicyStoreMap = match source {
        PolicyStoreSource::Json(json_raw) => serde_json::from_str(json_raw.as_str())?,
    };
    Ok(policy_store_map)
}

/// Load policy store based on config
//
// Unit tests will be added when will be implemented other types of sources
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, LoadPolicyStoreError> {
    let mut policy_store_map = load_policy_store_map(&config.source)?;

    let policy: PolicyStore = match (&config.store_id, policy_store_map.policy_stores.len()) {
        (Some(store_id), _) => policy_store_map
            .policy_stores
            .remove(store_id.as_str())
            .ok_or(LoadPolicyStoreError::FindPolicy(store_id.to_string()))?,
        (None, 0) => {
            return Err(LoadPolicyStoreError::PolicyEmpty);
        },
        (None, 1) => {
            // getting first element and we know it is save to use unwrap here,
            // because we know that there is only one element in the map
            policy_store_map
                .policy_stores
                .into_values()
                .next()
                .expect("In policy store map field policy_stores should be one element")
        },
        (None, 2..) => {
            return Err(LoadPolicyStoreError::MoreThanOnePolicy);
        },
    };

    Ok(policy)
}
