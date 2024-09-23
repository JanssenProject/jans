use crate::models::policy_store::{PolicyStore, PolicyStoreMap};
use crate::models::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};

/// Error cases for loading policy
#[derive(Debug, thiserror::Error)]
pub enum ErrorLoadPolicy {
    #[error("{0}")]
    JsonParce(#[from] serde_json::Error),
    #[error("store policy is empty")]
    PolicyEmpty,
    #[error("could not get policy, the `store_key` is not specified and the count on policies more than 1")]
    MoreThanOnePolicy,
    #[error("could not found policy by id: {0}")]
    FindPolicy(String),
}

/// Load policy store based on config
//
// Unit tests will be added when will be implemented other types of sources
pub(crate) fn load_policy_store(config: PolicyStoreConfig) -> Result<PolicyStore, ErrorLoadPolicy> {
    let mut policy_store_map: PolicyStoreMap = match config.source {
        PolicyStoreSource::Json(json_raw) => serde_json::from_str(json_raw.as_str())?,
    };

    if policy_store_map.policy_stores.is_empty() {
        return Err(ErrorLoadPolicy::PolicyEmpty);
    }

    let policy: PolicyStore = match (config.store_id, policy_store_map.policy_stores.len()) {
        (Some(store_id), _) => policy_store_map
            .policy_stores
            .remove(store_id.as_str())
            .ok_or(ErrorLoadPolicy::FindPolicy(store_id))?,
        (None, 0) => {
            return Err(ErrorLoadPolicy::PolicyEmpty);
        },
        (None, 1) => {
            // getting first element and we know it is save to use unwrap here,
            // because we know that there is only one element in the map
            policy_store_map
                .policy_stores
                .into_values()
                .into_iter()
                .next()
                .unwrap()
        },
        (None, 2..) => {
            return Err(ErrorLoadPolicy::MoreThanOnePolicy);
        },
    };

    Ok(policy)
}
